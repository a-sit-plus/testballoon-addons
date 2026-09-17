package at.asitplus.testballoon.gradle

import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

/**
 * Owns the build's status listener: a single loopback HTTP endpoint that test processes post progress lines to.
 *
 * Lifetime matters more than it looks. The port is derived from the root directory rather than picked freshly,
 * because a test task's environment is one of its inputs -- a new port every build would re-run every test task
 * every build. Keeping that port stable requires *releasing* it when the build ends, which is why this is an
 * [AutoCloseable] build service: leave the socket bound in the daemon and the next build finds its own listener
 * still there, walks to the next free port, and invalidates every test task in the process.
 */
abstract class StatusChannelService : BuildService<StatusChannelService.Parameters>, AutoCloseable {

    interface Parameters : BuildServiceParameters {
        val rootPath: Property<String>
        val basePort: Property<Int>
        val portSearchWidth: Property<Int>
        val renderStatus: Property<Boolean>
    }

    private val listener: ServerSocket? = run {
        val loopback = InetAddress.getByName(HOST)
        val base = parameters.basePort.orNull ?: derivePort(parameters.rootPath.get())
        val width = parameters.portSearchWidth.getOrElse(DEFAULT_SEARCH_WIDTH)
        (base until base + width).firstNotNullOfOrNull { candidate ->
            runCatching { ServerSocket(candidate, BACKLOG, loopback) }.getOrNull()
        }
    }

    /** The port test processes should post to, or null when nothing could be bound. */
    val port: Int? = listener?.localPort

    private val cleanups = ConcurrentLinkedQueue<() -> Unit>()

    /**
     * Registers work to undo when the build ends, whichever way it ends.
     *
     * `doLast` is not good enough for undoing state outside the build: Gradle skips it when the task action
     * fails, which is how a failed instrumented test run leaves an `adb reverse` mapping behind on a device.
     * Build services are closed regardless of task outcome, so this runs either way.
     */
    fun onBuildFinished(action: () -> Unit) {
        cleanups.add(action)
    }

    init {
        val announce = parameters.renderStatus.getOrElse(true)
        val bound = listener
        if (bound != null) {
            thread(isDaemon = true, name = "testballoon-status-channel") { accept(bound) }
            if (announce) LOGGER.lifecycle("TestBalloon status channel listening on $HOST:${bound.localPort}")
        } else if (announce) {
            LOGGER.lifecycle(
                "TestBalloon status channel: no free port available; test progress stays on the console."
            )
        }
    }

    private fun accept(server: ServerSocket) {
        while (!server.isClosed) {
            val connection = runCatching { server.accept() }.getOrNull() ?: continue
            // Handled inline rather than per-connection: progress lines must arrive in the order they were sent.
            runCatching { handle(connection) }
        }
    }

    private fun handle(connection: Socket) = connection.use { socket ->
        val input = socket.getInputStream()
        val head = input.readHead() ?: return
        val length = head.lineSequence()
            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0

        // Decode only after reading exactly Content-Length *bytes*: progress lines carry non-ASCII (↘, Σ, …),
        // so counting characters would truncate them.
        val body = input.readExactly(length).toString(Charsets.UTF_8)

        socket.getOutputStream().apply {
            write(RESPONSE)
            flush()
        }

        if (body.isNotBlank() && parameters.renderStatus.getOrElse(true)) render(body)
    }

    /**
     * Logged at lifecycle level rather than written to `System.out`. Gradle captures background-thread output
     * at INFO, so a `println` here is invisible at the default log level: the whole point of the channel is
     * that progress shows up while tests run, without `--info`.
     */
    private fun render(message: String) = LOGGER.lifecycle("  ⟨status⟩ $message")

    override fun close() {
        while (true) {
            val cleanup = cleanups.poll() ?: break
            runCatching { cleanup() }
        }
        runCatching { listener?.close() }
    }

    private companion object {
        val LOGGER = Logging.getLogger(StatusChannelService::class.java)
        const val HOST = "127.0.0.1"
        const val BACKLOG = 64
        const val DEFAULT_SEARCH_WIDTH = 64
        val RESPONSE = (
            "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray()

        /** Keeps concurrent checkouts on one machine off each other's port without any coordination. */
        fun derivePort(rootPath: String): Int = 49152 + Math.floorMod(rootPath.hashCode(), 16_000)

        /** Reads up to and including the blank line that terminates the request head. */
        fun InputStream.readHead(): String? {
            val buffer = ByteArrayOutputStream()
            var matched = 0
            while (matched < 4) {
                val byte = read()
                if (byte < 0) return null
                buffer.write(byte)
                matched = when {
                    byte == '\r'.code && matched % 2 == 0 -> matched + 1
                    byte == '\n'.code && matched % 2 == 1 -> matched + 1
                    else -> 0
                }
            }
            return buffer.toString("UTF-8")
        }

        fun InputStream.readExactly(count: Int): ByteArray {
            val bytes = ByteArray(count)
            var read = 0
            while (read < count) {
                val n = read(bytes, read, count - read)
                if (n < 0) break
                read += n
            }
            return if (read == count) bytes else bytes.copyOf(read)
        }
    }
}

/** Host the listener binds to. Loopback deliberately: a wider bind draws a firewall prompt on macOS. */
const val STATUS_CHANNEL_HOST: String = "127.0.0.1"

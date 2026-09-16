package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.testPlatform

/**
 * Out-of-band channel for progress/status messages.
 *
 * On every non-JVM target the console is the TeamCity service-message protocol channel: native writes its
 * report to `stdout`, the JS/Wasm mocha reporter does the same, so any status line printed there rides on the
 * same stream Gradle's parser consumes. Worse, progress emitted during *registration* (see the matrix module)
 * happens while no test is open at all, which no legal service message can express.
 *
 * This channel therefore leaves the console alone and posts status lines to a loopback HTTP endpoint that the
 * Gradle build opens for the duration of the run. It is strictly best-effort: unconfigured, unreachable or
 * misbehaving endpoints silently disable it, and callers fall back to [compactProgressPrint].
 */
object StatusChannel {
    private const val HOST_VARIABLE = "TESTBALLOON_ADDONS_STATUS_HOST"
    private const val PORT_VARIABLE = "TESTBALLOON_ADDONS_STATUS_PORT"

    private var endpoint: Pair<String, Int>? = null
    private var resolved = false
    private var dead = false

    /** Overrides the endpoint the build advertised. Mainly useful for testing the channel itself. */
    fun configure(host: String, port: Int) {
        endpoint = host to port
        resolved = true
        dead = false
    }

    /**
     * Reads the endpoint the Gradle build advertised, once. [testPlatform] resolves environment variables on
     * every target: `System.getenv` on JVM, `getenv` on native, `process.env` under Node, and
     * `window.__karma__.config.env` in a browser. TestBalloon's own Gradle plugin fills that last slot from a
     * generated `karma.config.d/testBalloonParameters.js`, and prefixes `SIMCTL_CHILD_` for Apple simulators,
     * but it sources both from the daemon's own environment -- so a value the addons plugin sets on a test
     * task still needs forwarding of its own.
     */
    private fun endpoint(): Pair<String, Int>? {
        if (!resolved) {
            resolved = true
            val host = testPlatform.environment(HOST_VARIABLE)
            val port = testPlatform.environment(PORT_VARIABLE)?.toIntOrNull()
            endpoint = if (host != null && port != null) host to port else null
        }
        return endpoint
    }

    /** True once an endpoint is known and no send has failed yet. */
    val isLive: Boolean get() = !dead && endpoint() != null

    /**
     * Posts [message], returning true if it went out. The first failure retires the channel permanently -- a
     * test run must never stall or fail because nobody is listening.
     */
    fun send(message: String): Boolean {
        if (dead) return false
        val (host, port) = endpoint() ?: return false
        if (!statusPost(host, port, "HTTPMSG $message")) {
            dead = true
            return false
        }
        return true
    }
}

/** Builds the fixed request. Deliberately HTTP: it is the only wire format a browser can also speak. */
internal fun statusRequest(host: String, port: Int, body: String): String {
    val payload = body.encodeToByteArray()
    return buildString {
        append("POST /status HTTP/1.1\r\n")
        append("Host: ").append(host).append(':').append(port).append("\r\n")
        append("Content-Type: text/plain; charset=utf-8\r\n")
        append("Content-Length: ").append(payload.size).append("\r\n")
        append("Connection: close\r\n\r\n")
        append(body)
    }
}

internal expect fun statusPost(host: String, port: Int, body: String): Boolean

/** `sin_port` is big-endian; every target we build for is little-endian, so swap the two bytes. */
internal fun Int.toNetworkOrderPort(): UShort = (((this and 0xFF) shl 8) or ((this shr 8) and 0xFF)).toUShort()

/**
 * `s_addr` holds the four octets in network order. Reading them back as a host-order integer on a
 * little-endian machine means the first octet occupies the lowest byte, which is what this builds.
 * Avoids depending on `inet_addr`, whose binding differs across the Apple/Linux/Android-native platform libs.
 */
internal fun String.toLoopbackAddress(): UInt? {
    val octets = split('.')
    if (octets.size != 4) return null
    var packed = 0u
    octets.forEachIndexed { index, octet ->
        val value = octet.toUIntOrNull() ?: return null
        if (value > 255u) return null
        packed = packed or (value shl (8 * index))
    }
    return packed
}

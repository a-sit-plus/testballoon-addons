package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Shared by the JVM target and Android's *local* unit tests (Robolectric included): those run in a host JVM,
 * so the build's loopback listener is simply there.
 *
 * KNOWN GAP -- Android instrumented/device tests (`androidDeviceTest`, `connectedAndroidTest`). Two separate
 * problems, neither solved here:
 *
 *  1. Address. The test runs inside the emulator or on the device, where `127.0.0.1` is *its own* loopback,
 *     not the host's. The fix is `adb reverse tcp:<port> tcp:<port>`, which forwards the device's loopback to
 *     the host's: emulators and USB-connected devices then both reach the listener at plain `127.0.0.1`, the
 *     same as every other target. No `10.0.2.2` (or Genymotion's `10.0.3.2`) special case, and the host keeps
 *     its loopback-only bind, which is what avoids a firewall prompt -- do not widen it.
 *     Requires API 21+ (matching this project's minSdk), must be issued per device (`adb -s <serial> ...`)
 *     after each connect, and is lost when the adb server restarts. Timing is the awkward part: it has to run
 *     after boot but before instrumentation starts -- straightforward to hang off `connectedAndroidTest`,
 *     harder for AGP's Gradle Managed Devices, which boot their emulator inside the task itself.
 *  2. Delivery. `testPlatform.environment(...)` reads `System.getenv`, and a test task's environment does not
 *     reach an instrumented process. The endpoint has to arrive as an instrumentation runner argument
 *     (AGP's `testInstrumentationRunnerArguments`, read back via `InstrumentationRegistry.getArguments()`),
 *     which means hooking AGP rather than the Kotlin test tasks.
 *
 * Until both are done, instrumented tests never come up live and keep the console fallback.
 */
internal actual fun statusPost(host: String, port: Int, body: String): Boolean =
    catchingUnwrapped {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS)
            socket.soTimeout = CONNECT_TIMEOUT_MILLIS
            socket.getOutputStream().apply {
                write(statusRequest(host, port, body).encodeToByteArray())
                flush()
            }
        }
        true
    }.getOrElse { false }

private const val CONNECT_TIMEOUT_MILLIS = 250

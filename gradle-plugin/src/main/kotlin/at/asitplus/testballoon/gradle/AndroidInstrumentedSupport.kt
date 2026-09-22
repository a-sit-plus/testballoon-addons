package at.asitplus.testballoon.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File
import java.util.Collections
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Makes the status channel reachable from Android instrumented tests.
 *
 * Two problems, both of which have to be solved here because nothing else does:
 *
 *  1. Delivery. A test task's environment never reaches a process started by `am instrument`, so the endpoint
 *     travels as an instrumentation runner argument instead. Nothing is needed on the runtime side:
 *     TestBalloon's Android `TestPlatform.environment` already falls back to
 *     `InstrumentationRegistry.getArguments()`, so `testPlatform.environment(...)` finds it.
 *  2. Address. Inside the emulator or device `127.0.0.1` is its own loopback, so `adb reverse` forwards that
 *     port back to the host. This works uniformly for emulators and USB-connected devices, which is why the
 *     host listener can stay bound to loopback.
 *
 * Both steps are best-effort: with no SDK, no adb or no device, instrumented tests keep the console fallback
 * like any other unsupported target.
 *
 * AGP types are referenced directly (via a `compileOnly` dependency) rather than reflectively, which is what
 * lets a single code path serve both the classic `android { }` DSL and the Kotlin-multiplatform
 * `androidLibrary { }` one: `KotlinMultiplatformAndroidComponentsExtension` extends
 * [AndroidComponentsExtension], and every device test implements `GeneratesTestApk`. All of it sits behind a
 * `withPlugin` guard, so these classes are never loaded in a project without AGP.
 */
internal object AndroidInstrumentedSupport {

    /**
     * AGP's `DeviceProviderInstrumentTestTask` and `ManagedDeviceInstrumentationTestTask`. Matched by class
     * name because both live in `com.android.build.gradle.internal.tasks` and are not part of the public API;
     * task *names* are unusable here, since `packageDebugAndroidTest` also ends in "AndroidTest".
     */
    private val INSTRUMENTED_TEST_TASK = Regex("Instrument\\w*TestTask")

    /** Gradle Managed Devices always run on an emulator, whose serial carries this prefix. */
    private const val EMULATOR_SERIAL_PREFIX = "emulator-"

    private const val POLL_INTERVAL_MILLIS = 200L

    private val ANDROID_PLUGINS = listOf(
        "com.android.base",
        "com.android.application",
        "com.android.library",
        "com.android.kotlin.multiplatform.library"
    )

    fun configure(
        project: Project,
        endpoint: Map<String, String>,
        port: Int,
        waitSeconds: Int,
        service: Provider<StatusChannelService>
    ) {
        val configured = AtomicBoolean(false)
        ANDROID_PLUGINS.forEach { pluginId ->
            project.pluginManager.withPlugin(pluginId) {
                if (configured.compareAndSet(false, true)) {
                    declareRunnerArguments(project, endpoint)
                    forwardPortForInstrumentedTests(project, port, waitSeconds, service)
                }
            }
        }
    }

    private fun declareRunnerArguments(project: Project, endpoint: Map<String, String>) {
        val components = project.extensions.findByType(AndroidComponentsExtension::class.java) ?: return

        @Suppress("UNCHECKED_CAST")
        val typed = components as AndroidComponentsExtension<*, *, Variant>
        typed.onVariants(typed.selector().all()) { variant ->
            val deviceTests = when (variant) {
                // `deviceTests` is the general form; `androidTest` remains for variants that only expose one.
                is HasDeviceTests -> variant.deviceTests.values
                is HasAndroidTest -> listOfNotNull(variant.androidTest)
                else -> emptyList()
            }
            deviceTests.forEach { deviceTest ->
                endpoint.forEach { (name, value) -> deviceTest.instrumentationRunnerArguments.put(name, value) }
            }
        }
    }

    /**
     * Gradle Managed Devices boot their emulator *inside* the test task, so at `doFirst` there is usually
     * nothing attached yet. Rather than give up on them, forward to whatever is already there and then keep
     * watching: a daemon thread polls for new serials and forwards each as it appears. The emulator shows up
     * well before AGP has installed the APKs and started instrumentation, so the mapping is in place by the
     * time any test code connects. The watcher stops at `doLast`; the timeout only bounds a task that dies
     * without ever reaching it.
     *
     * `adb` is resolved at configuration time so the task actions never touch the [Project].
     */
    private fun forwardPortForInstrumentedTests(
        project: Project,
        port: Int,
        waitSeconds: Int,
        service: Provider<StatusChannelService>
    ) {
        val adb = adbExecutable(project) ?: return

        project.tasks.configureEach { task ->
            val type = task.javaClass.name.removeSuffix("_Decorated")
            if (!INSTRUMENTED_TEST_TASK.containsMatchIn(type)) return@configureEach

            // A managed-device run owns its emulator, so it has no business forwarding a port onto a phone the
            // developer happens to have plugged in. `connectedAndroidTest` is the opposite: the user chose the
            // attached devices, so all of them are fair game.
            val emulatorsOnly = type.contains("ManagedDevice")

            val forwarded = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
            val watching = AtomicBoolean(true)

            task.usesService(service)

            task.doFirst {
                // Removing the mappings is registered for the end of the build rather than done in `doLast`,
                // which Gradle skips when the task action fails -- precisely the case that strands an
                // `adb reverse` mapping on a device.
                service.get().onBuildFinished {
                    watching.set(false)
                    forwarded.forEach { serial -> runAdb(adb, "-s", serial, "reverse", "--remove", "tcp:$port") }
                }

                forwardToNewDevices(adb, port, forwarded, emulatorsOnly)
                thread(isDaemon = true, name = "testballoon-status-adb-reverse") {
                    val deadline = System.nanoTime() + waitSeconds.toLong() * 1_000_000_000L
                    while (watching.get() && System.nanoTime() < deadline) {
                        Thread.sleep(POLL_INTERVAL_MILLIS)
                        if (!watching.get()) break
                        forwardToNewDevices(adb, port, forwarded, emulatorsOnly)
                    }
                }
            }

            // Stops the poller promptly on the happy path; the deadline bounds it when this is skipped.
            task.doLast { watching.set(false) }
        }
    }

    /** Forwards the port on every eligible device not handled yet. Safe to call repeatedly. */
    private fun forwardToNewDevices(adb: String, port: Int, forwarded: MutableSet<String>, emulatorsOnly: Boolean) {
        attachedDevices(adb)
            .filter { !emulatorsOnly || it.startsWith(EMULATOR_SERIAL_PREFIX) }
            .forEach { serial ->
                if (forwarded.add(serial)) runAdb(adb, "-s", serial, "reverse", "tcp:$port", "tcp:$port")
            }
    }

    private fun adbExecutable(project: Project): String? {
        val sdk = sequenceOf(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            localProperties(project)?.getProperty("sdk.dir")
        ).firstOrNull { !it.isNullOrBlank() }

        val fromSdk = sdk?.let { File(it, "platform-tools/adb") }
        // Falling back to PATH: `attachedDevices` yields nothing if that is not resolvable either.
        return if (fromSdk != null && fromSdk.canExecute()) fromSdk.absolutePath else "adb"
    }

    private fun localProperties(project: Project): Properties? = runCatching {
        val file = project.rootProject.file("local.properties")
        if (!file.isFile) return@runCatching null
        Properties().apply { file.inputStream().use { load(it) } }
    }.getOrNull()

    /** Serials of devices in state `device`; anything offline or unauthorized is skipped. */
    private fun attachedDevices(adb: String): List<String> = runCatching {
        val process = ProcessBuilder(adb, "devices").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        output.lineSequence()
            .drop(1)
            .mapNotNull { line ->
                val fields = line.trim().split(Regex("\\s+"))
                if (fields.size >= 2 && fields[1] == "device") fields[0] else null
            }
            .toList()
    }.getOrDefault(emptyList())

    private fun runAdb(adb: String, vararg arguments: String) {
        runCatching {
            ProcessBuilder(listOf(adb, *arguments))
                .redirectErrorStream(true)
                .start()
                .also { process -> process.inputStream.use { it.readBytes() } }
                .waitFor()
        }
    }
}

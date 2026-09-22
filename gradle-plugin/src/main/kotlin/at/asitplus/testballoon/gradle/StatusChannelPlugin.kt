package at.asitplus.testballoon.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.testing.AbstractTestTask

/** Settings for the status channel, exposed as the `testBalloonStatusChannel` extension on the root project. */
abstract class StatusChannelExtension {
    /** Set to false to leave test processes on their console fallback. Defaults to true. */
    abstract val enabled: Property<Boolean>

    /** First port to try. Defaults to a value derived from the root directory, stable across builds. */
    abstract val basePort: Property<Int>

    /** How far to walk upwards when the base port is taken. Defaults to 64. */
    abstract val portSearchWidth: Property<Int>

    /** Set to false to collect status lines without printing them. Defaults to true. */
    abstract val renderStatus: Property<Boolean>

    /**
     * How long an Android instrumented test task keeps watching for a device to appear before giving up on
     * forwarding the port to it. Covers Gradle Managed Devices, whose emulator only boots once the task is
     * already running. Defaults to 300 seconds.
     */
    abstract val androidDeviceWaitSeconds: Property<Int>
}

/**
 * Opens one loopback listener for the whole build and tells every test process where to find it.
 *
 * Apply to the root project:
 * ```
 * plugins { id("at.asitplus.testballoon.addons") }
 * ```
 *
 * The addons pick the endpoint up through `testPlatform.environment(...)`, so nothing is needed in test code.
 * The channel is best-effort throughout: if no port can be bound, or a test process cannot reach it, progress
 * falls back to the console exactly as before.
 */
class StatusChannelPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "The TestBalloon addons status channel plugin must be applied to the root project, " +
                "as it opens a single listener for the whole build (applied to '${target.path}')."
        }

        val settings = target.extensions.create(EXTENSION, StatusChannelExtension::class.java)

        val service = target.gradle.sharedServices.registerIfAbsent(SERVICE, StatusChannelService::class.java) {
            it.parameters.rootPath.set(target.rootDir.absolutePath)
            it.parameters.basePort.set(settings.basePort)
            it.parameters.portSearchWidth.set(settings.portSearchWidth)
            it.parameters.renderStatus.set(settings.renderStatus)
        }

        target.allprojects { project ->
            if (settings.enabled.getOrElse(true)) {
                service.get().port?.let { port ->
                    AndroidInstrumentedSupport.configure(
                        project,
                        endpoint = mapOf(HOST_VARIABLE to STATUS_CHANNEL_HOST, PORT_VARIABLE to port.toString()),
                        port = port,
                        waitSeconds = settings.androidDeviceWaitSeconds.getOrElse(300),
                        service = service
                    )
                }
            }

            project.tasks.withType(AbstractTestTask::class.java).configureEach { task ->
                // Binds the listener at execution time too, so the channel still exists on a configuration
                // cache hit, where none of the configuration code above runs again.
                task.usesService(service)

                if (settings.enabled.getOrElse(true)) {
                    val port = service.get().port ?: return@configureEach
                    if (BROWSER_TASK.containsMatchIn(task.name)) {
                        task.advertiseToBrowser(project.projectDir.toPath(), port)
                    } else {
                        task.advertise(HOST_VARIABLE, STATUS_CHANNEL_HOST)
                        task.advertise(PORT_VARIABLE, port.toString())
                    }
                }
            }
        }
    }

    /**
     * `AbstractTestTask` is the common base of Gradle's `Test` and Kotlin's `KotlinNativeTest`/`KotlinJsTest`,
     * but it declares no way to set an environment variable, and the subtypes disagree on the signature:
     * `(String, Object)` on JVM and native, `(String, String)` on JS. Resolving it reflectively keeps this
     * plugin free of any dependency on a particular Kotlin Gradle plugin version; tasks that expose no such
     * method at all -- karma-driven browser tests among them -- are simply skipped.
     */
    private fun Task.advertise(name: String, value: String) {
        val setter = sequenceOf(Any::class.java, String::class.java).firstNotNullOfOrNull { valueType ->
            runCatching { javaClass.getMethod("environment", String::class.java, valueType) }.getOrNull()
        } ?: return
        runCatching { setter.invoke(this, name, value) }
        // Apple simulator tests run through `xcrun simctl spawn`, which only passes variables carrying this
        // prefix down to the spawned binary; the plain name reaches `simctl` itself and stops there.
        // TestBalloon's own plugin does exactly the same for its variables, but it sources them from the
        // daemon's environment, which a build-chosen port never appears in.
        runCatching { setter.invoke(this, "SIMCTL_CHILD_$name", value) }
    }

    /**
     * Karma-driven browser tests get no environment at all: the task's environment belongs to the karma *node*
     * process, not the page. Karma instead exposes `config.client` to the page as `window.__karma__.config`,
     * which is where TestBalloon's `testPlatform.environment(...)` looks, so the endpoint is injected through a
     * `karma.config.d` snippet -- the same mechanism TestBalloon's own Gradle plugin uses for its variables.
     *
     * The file name has to sort *after* TestBalloon's `testBalloonParameters.js`: that one assigns
     * `config.client.env` wholesale, so a snippet loaded earlier would be discarded. This one merges into
     * whatever is already there, and is removed again afterwards so nothing is left behind in the project.
     */
    private fun Task.advertiseToBrowser(projectDir: java.nio.file.Path, port: Int) {
        val directory = projectDir.resolve("karma.config.d")
        val snippet = directory.resolve("zzTestBalloonAddonsStatusChannel.js")

        doFirst {
            runCatching {
                java.nio.file.Files.createDirectories(directory)
                java.nio.file.Files.writeString(
                    snippet,
                    """
                    config.client = config.client || {};
                    config.client.env = config.client.env || {};
                    config.client.env.$HOST_VARIABLE = "$STATUS_CHANNEL_HOST";
                    config.client.env.$PORT_VARIABLE = "$port";
                    """.trimIndent() + "\n"
                )
            }
        }

        doLast {
            runCatching {
                if (java.nio.file.Files.deleteIfExists(snippet)) {
                    runCatching { java.nio.file.Files.delete(directory) }
                }
            }
        }
    }

    private companion object {
        const val EXTENSION = "testBalloonStatusChannel"
        const val SERVICE = "testBalloonStatusChannel"
        const val HOST_VARIABLE = "TESTBALLOON_ADDONS_STATUS_HOST"
        const val PORT_VARIABLE = "TESTBALLOON_ADDONS_STATUS_PORT"
        val BROWSER_TASK = Regex("[Bb]rowser")
    }
}

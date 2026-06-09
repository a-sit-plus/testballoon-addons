package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.truncated
import at.asitplus.testballoon.withCompactProgressHeartbeatSuspending
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import de.infix.testBalloon.framework.shared.TestSuitePropertyName
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import kotlin.coroutines.CoroutineContext

@DslMarker
annotation class MatrixTestDsl

typealias NameFn<T> = (index: Long, value: T) -> String

/**
 * Declares a top-level matrix test suite.
 *
 * Pass per-suite configuration with [matrixConfig], e.g.
 * `val Suite by matrixSuite(matrixConfig { execution = ExecutionMode.Concurrent(8) }) { … }`. Unset fields fall back
 * to the global [MatrixTestDefaults]. With no configuration, write `val Suite by matrixSuite { … }`.
 *
 * The config's `testConfig` is for `aroundAll` / `aroundEach` / context / timeout wrappers only. Set concurrency via
 * its `execution`, **never** `TestConfig.invocation(...)` — matrix derives invocation from `execution`, and a
 * conflicting one is overridden (it cannot be removed; TestBalloon configs are opaque). A virtual-time `testScope(...)`
 * applies only to *sequential* execution (matrix auto-disables it when concurrent); prefer enabling `TestScope` on the
 * `TestSession`, and use `aroundEach`/`aroundAll` + `withTimeout` for timeouts under concurrency.
 *
 * Note: configuration is taken as a single [MatrixSuiteConfigBuilder] argument rather than many named parameters, so
 * the call site stays a shape the TestBalloon compiler plugin reliably discovers (a plain call, not a reordered-named-
 * argument call that the plugin can drop from discovery).
 */
@TestRegistering
fun matrixSuite(
    config: MatrixSuiteConfigBuilder = matrixConfig {},
    @TestSuitePropertyName propertyName: String = "",
    body: MatrixSuiteScope.() -> Unit,
) = config.build().let { resolved ->
    testSuite(
        qualifiedPropertyName = propertyName,
        testConfig = resolved.testConfig,
    ) {
        MatrixSuiteScope(this, resolved).apply { building { body() } }
    }
}

@MatrixTestDsl
data class MatrixSuiteScope internal constructor(
    internal val target: TestSuiteScope,
    val config: MatrixSuiteConfig,
    internal val registrationPath: List<MatrixRegistrationFrame> = emptyList(),
    internal val registrationReporter: MatrixRegistrationReporter = MatrixRegistrationReporter(),
    internal val replayPath: List<MatrixReplayFrame> = emptyList(),
) : MatrixScope<MatrixSuiteScope> {
    override val matrixConfig: MatrixSuiteConfig get() = config

    // True only while this scope's build body runs. Registering when false means the call happened
    // during a test body (a forgotten `-` on an enclosing `"name"`), where it would be silently lost.
    private var registrationOpen = false

    internal fun building(block: () -> Unit) {
        registrationOpen = true
        try {
            block()
        } finally {
            registrationOpen = false
        }
    }

    internal fun requireOpen(name: String?) = check(registrationOpen) { nestedRegistrationMessage(name) }

    @TestRegistering
    fun testSuite(
        name: String,
        config: MatrixSuiteConfigBuilder,
        body: MatrixSuiteScope.() -> Unit,
    ) {
        requireOpen(name)
        val resolved = config.build(this@MatrixSuiteScope.config)
        target.apply {
            testSuite(
                name = matrixName(name),
                testConfig = resolved.testConfig.disableByMatrixName(name),
            ) {
                MatrixSuiteScope(
                    this,
                    resolved.nested(resolved.execution),
                    registrationPath,
                    registrationReporter,
                    replayPath + MatrixReplayFrame.Group(matrixName(name)),
                ).apply { building { body() } }
            }
        }
    }

    /**
     * Shorthand wrapping [matrixConfig] with only a [testConfig] (`aroundAll` / `aroundEach` / context / timeouts).
     * Set concurrency via `execution` (see [matrixConfig]), not `TestConfig.invocation(...)`; `testScope(...)` is
     * sequential-only (matrix disables it when concurrent).
     */
    @TestRegistering
    fun testSuite(
        name: String,
        testConfig: TestConfig = TestConfig,
        body: MatrixSuiteScope.() -> Unit,
    ) = testSuite(name, matrixConfig { this.testConfig = testConfig }, body)

    @TestRegistering
    fun test(
        name: String,
        config: MatrixSuiteConfigBuilder,
        body: suspend Test.ExecutionScope.() -> Unit,
    ) {
        requireOpen(name)
        val resolved = config.build(this@MatrixSuiteScope.config)
        target.apply {
            test(
                name = matrixName(name),
                testConfig = resolved.testConfig.disableByMatrixName(name),
                action = { withMatrixReplay(replayPath + MatrixReplayFrame.Group(matrixName(name)), body) },
            )
        }
    }

    /**
     * Shorthand; [testConfig] is for `aroundAll` / `aroundEach` / context / timeouts only. Set concurrency via
     * `execution` (see [matrixConfig]), not `TestConfig.invocation(...)`; `testScope(...)` is sequential-only.
     */
    @TestRegistering
    fun test(
        name: String,
        testConfig: TestConfig = TestConfig,
        body: suspend Test.ExecutionScope.() -> Unit,
    ) = test(name, matrixConfig { this.testConfig = testConfig }, body)

    @TestRegistering
    operator fun String.invoke(
        config: MatrixSuiteConfigBuilder,
        body: suspend Test.ExecutionScope.() -> Unit,
    ) {
        test(this, config, body)
    }

    /**
     * FreeSpec test. [testConfig] is for `aroundAll` / `aroundEach` / context / timeouts only — set concurrency via
     * `execution` (use the `matrixConfig` overload), not `TestConfig.invocation(...)`; `testScope(...)` is
     * sequential-only.
     */
    @TestRegistering
    operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
        body: suspend Test.ExecutionScope.() -> Unit,
    ) {
        test(this, testConfig, body)
    }

    @TestRegistering
    operator fun String.invoke(
        config: MatrixSuiteConfigBuilder,
    ): MatrixConfiguredSuite = MatrixConfiguredSuite(this@MatrixSuiteScope, this, config)

    /**
     * FreeSpec suite (`"name"(testConfig) - { … }`). [testConfig] is for `aroundAll` / `aroundEach` / context /
     * timeouts only — set concurrency via `execution` (use the `matrixConfig` overload), not
     * `TestConfig.invocation(...)`; `testScope(...)` is sequential-only.
     */
    @TestRegistering
    operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
    ): MatrixConfiguredSuite = MatrixConfiguredSuite(this@MatrixSuiteScope, this, matrixConfig { this.testConfig = testConfig })

    @TestRegistering
    infix operator fun MatrixConfiguredSuite.minus(body: MatrixSuiteScope.() -> Unit) {
        scope.testSuite(name, config, body)
    }

    @TestRegistering
    infix operator fun String.minus(body: MatrixSuiteScope.() -> Unit) {
        testSuite(this, body = body)
    }

    // One registration path for both kinds: [spec] supplies what differs (cases, replay frame, counts, default
    // name, execution); [body] supplies the container-vs-terminal wiring. Replaces the former four *Internal methods.
    internal fun registerLayer(
        name: String?,
        spec: LayerSpec,
        nameFn: NameFn<Any?>,
        body: RealLayerBody,
    ) {
        requireOpen(name)
        val layerName = name?.let(::matrixName)
        val registrationName = layerName ?: spec.defaultName
        val scopedConfig = config.nested(spec.execution)
        val caseTestConfig = scopedConfig.testConfig.boundBy(spec.execution.caseLimiter())
        val register: TestSuiteScope.() -> Unit = {
            val progress = registrationProgress(registrationName, spec.registrationProgressTotal, registrationPath, registrationReporter)
            val iterator = spec.cases()
            var registered = 0L
            progress.registered(registered)
            while (iterator.hasNext()) {
                val case = iterator.next()
                val caseName = nameFn(case.index, case.value).truncated(spec.nameMaxLength)
                val replayFrame = spec.frame(layerName, case, caseName)
                when (body) {
                    is RealLayerBody.Container -> testSuite(name = caseName, testConfig = caseTestConfig) {
                        val child = MatrixSuiteScope(
                            this,
                            scopedConfig,
                            registrationPath + MatrixRegistrationFrame(registrationName, case.index, spec.registrationFrameTotal),
                            registrationReporter,
                            replayPath + replayFrame,
                        )
                        child.building { body.build(child, case.value) }
                    }

                    is RealLayerBody.Terminal -> test(name = caseName, testConfig = caseTestConfig) {
                        withMatrixReplay(replayPath + replayFrame) { body.leaf(this, case.value) }
                    }
                }
                progress.registered(++registered)
            }
            progress.completed(registered)
        }
        target.apply {
            if (name == null) register()
            else testSuite(name = registrationName, testConfig = scopedConfig.testConfig.disableByMatrixName(name)) { register() }
        }
    }

    @TestRegistering
    fun compact(
        name: String,
        config: CompactConfigBuilder.() -> Unit = {},
    ): MatrixCompactLayer = MatrixCompactLayer(this, name, config)

    internal fun compactInternal(
        name: String,
        config: CompactConfigBuilder.() -> Unit,
        body: CompactScope.() -> Unit,
    ) {
        requireOpen(name)
        val compactConfig = CompactConfigBuilder(this.config).apply(config).build()
        val planningScope = CompactScope(this.config, compactConfig)
        planningScope.building { planningScope.body() }
        val nodes = planningScope.nodes.toList()
        target.apply {
            // Compact executes its cases on real dispatchers (its own worker pool / per-layer concurrency), so it
            // cannot run in a virtual-time TestScope — disable it here even when the surrounding session enabled it.
            test(name = matrixName(name), testConfig = this@MatrixSuiteScope.config.invocationConfig.testScope(isEnabled = false).disableByMatrixName(name)) {
                val run = CompactRun(name, compactConfig)
                when (val progress = compactConfig.progressIndicator) {
                    is Indicator.Heartbeat -> withCompactProgressHeartbeatSuspending(
                        progress.every,
                        { run.progressMessage() }
                    ) {
                        runCompactNodes(nodes, this@test, run, replayPath)
                    }

                    Indicator.None -> runCompactNodes(nodes, this@test, run, replayPath)
                }
                run.throwIfAny()
            }
        }
    }
}

class MatrixConfiguredSuite internal constructor(
    internal val scope: MatrixSuiteScope,
    internal val name: String,
    internal val config: MatrixSuiteConfigBuilder,
)

/**
 * Real-tree counterpart to the compact [LayerBody]: a container registers child suites (its body builds into a child
 * [MatrixSuiteScope]); a terminal registers a leaf test. The `Container` halves differ between trees (the compact one
 * returns nodes), so this cannot share `LayerBody`; the `Terminal` body type is the same.
 */
internal sealed interface RealLayerBody {
    class Container(val build: MatrixSuiteScope.(Any?) -> Unit) : RealLayerBody
    class Terminal(val leaf: suspend Test.ExecutionScope.(Any?) -> Unit) : RealLayerBody
}

class MatrixCompactLayer internal constructor(
    private val scope: MatrixSuiteScope,
    private val name: String,
    private val config: CompactConfigBuilder.() -> Unit,
) {
    operator fun minus(body: CompactScope.() -> Unit) {
        scope.compactInternal(name, config, body)
    }
}

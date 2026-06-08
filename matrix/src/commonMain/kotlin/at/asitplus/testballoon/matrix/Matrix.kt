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

@TestRegistering
fun matrixSuite(
    @TestSuitePropertyName propertyName: String = "",
    execution: ExecutionMode? = null,
    defaultPropertyIterations: Int? = null,
    defaultCompactConcurrency: CompactConcurrency? = null,
    defaultCompactReport: CompactReport? = null,
    defaultCompactAddSuppressedErrors: Boolean? = null,
    defaultCompactReportRows: Int? = null,
    defaultProgressIndicator: Indicator? = null,
    defaultCompactCoroutineContext: CoroutineContext? = null,
    defaultTestNameMaxLength: Int? = null,
    testConfig: TestConfig? = null,
    body: MatrixSuiteScope.() -> Unit,
) = MatrixSuiteConfigBuilder().apply {
    this.execution = execution
    this.defaultPropertyIterations = defaultPropertyIterations
    this.defaultCompactConcurrency = defaultCompactConcurrency
    this.defaultCompactReport = defaultCompactReport
    this.defaultCompactAddSuppressedErrors = defaultCompactAddSuppressedErrors
    this.defaultCompactReportRows = defaultCompactReportRows
    this.defaultProgressIndicator = defaultProgressIndicator
    this.defaultCompactCoroutineContext = defaultCompactCoroutineContext
    this.defaultTestNameMaxLength = defaultTestNameMaxLength
    this.testConfig = testConfig
}.build().let { resolved ->
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

    /** Shorthand for the most common case — wraps [matrixConfig] with only a [testConfig] (e.g. for `aroundAll`). */
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

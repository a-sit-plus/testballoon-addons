package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.truncated
import at.asitplus.testballoon.withCompactProgressHeartbeatSuspending
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
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
        MatrixSuiteScope(this, resolved).body()
    }
}

@MatrixTestDsl
data class MatrixSuiteScope internal constructor(
    internal val target: TestSuiteScope,
    val config: MatrixSuiteConfig,
    internal val registrationPath: List<MatrixRegistrationFrame> = emptyList(),
    internal val registrationReporter: MatrixRegistrationReporter = MatrixRegistrationReporter(),
    internal val propertyReplayPath: List<MatrixPropertyReplayFrame> = emptyList(),
) {
    @TestRegistering
    fun testSuite(
        name: String,
        testConfig: TestConfig = TestConfig,
        body: MatrixSuiteScope.() -> Unit,
    ) {
        target.apply {
            testSuite(
                name = matrixName(name),
                testConfig = config.testConfig.chainedWith(testConfig).disableByMatrixName(name),
            ) {
                MatrixSuiteScope(this, config, registrationPath, registrationReporter, propertyReplayPath).body()
            }
        }
    }

    @TestRegistering
    fun test(
        name: String,
        testConfig: TestConfig = TestConfig,
        body: suspend Test.ExecutionScope.() -> Unit,
    ) {
        target.apply {
            test(
                name = matrixName(name),
                testConfig = config.testConfig.chainedWith(testConfig).disableByMatrixName(name),
                action = { withMatrixPropertyReplay(propertyReplayPath, body) },
            )
        }
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
        testConfig: TestConfig = TestConfig,
    ): MatrixConfiguredSuite = MatrixConfiguredSuite(this@MatrixSuiteScope, this, testConfig)

    @TestRegistering
    infix operator fun MatrixConfiguredSuite.minus(body: MatrixSuiteScope.() -> Unit) {
        scope.testSuite(name, testConfig, body)
    }

    @TestRegistering
    infix operator fun String.minus(body: MatrixSuiteScope.() -> Unit) {
        testSuite(this, body = body)
    }

    fun <T> data(
        name: String,
        values: Iterable<T>,
        nameFn: NameFn<T> = { index, value -> defaultLayerName(index, value) },
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): MatrixDataLayer<T> = MatrixDataLayer(this, name, IterableDataSource(values), nameFn, config)

    fun <T> data(
        name: String,
        values: Sequence<T>,
        limit: Long? = null,
        nameFn: NameFn<T> = { index, value -> defaultLayerName(index, value) },
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): MatrixDataLayer<T> = MatrixDataLayer(this, name, SequenceDataSource(values, limit), nameFn, config)

    internal fun <T> dataInternal(
        name: String,
        source: MatrixDataSource<T>,
        nameFn: NameFn<T>,
        configBlock: DataLayerConfigBuilder.() -> Unit,
        body: MatrixSuiteScope.(T) -> Unit,
    ) {
        val layerConfig = DataLayerConfigBuilder(config).apply(configBlock).build()
        val strippedName = matrixName(name)
        val iterator = source.cases(layerConfig.replayIndex)
        val dataConfig = config.copy(execution = layerConfig.execution)
        val caseLimiter = layerConfig.execution.caseLimiter()
        val caseTestConfig = dataConfig.testConfig.boundBy(caseLimiter)
        target.apply {
            testSuite(name = strippedName, testConfig = dataConfig.testConfig.disableByMatrixName(name)) {
                val progress = registrationProgress(strippedName, source.knownSize, registrationPath, registrationReporter)
                var registered = 0L
                progress.registered(registered)
                while (iterator.hasNext()) {
                    val case = iterator.next()
                    val caseName = nameFn(case.index, case.value).truncated(layerConfig.nameMaxLength)
                    testSuite(
                        name = caseName,
                        testConfig = caseTestConfig
                    ) {
                        MatrixSuiteScope(
                            this,
                            dataConfig,
                            registrationPath + MatrixRegistrationFrame(strippedName, case.index, source.knownSize),
                            registrationReporter,
                            propertyReplayPath,
                        ).body(case.value)
                    }
                    progress.registered(++registered)
                }
                progress.completed(registered)
            }
        }
    }

    internal fun <T> dataTestInternal(
        name: String,
        source: MatrixDataSource<T>,
        nameFn: NameFn<T>,
        configBlock: DataLayerConfigBuilder.() -> Unit,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        val layerConfig = DataLayerConfigBuilder(config).apply(configBlock).build()
        val strippedName = matrixName(name)
        val iterator = source.cases(layerConfig.replayIndex)
        val dataConfig = config.copy(execution = layerConfig.execution)
        val caseLimiter = layerConfig.execution.caseLimiter()
        val caseTestConfig = dataConfig.testConfig.boundBy(caseLimiter)
        target.apply {
            testSuite(name = strippedName, testConfig = dataConfig.testConfig.disableByMatrixName(name)) {
                val progress = registrationProgress(strippedName, source.knownSize, registrationPath, registrationReporter)
                var registered = 0L
                progress.registered(registered)
                while (iterator.hasNext()) {
                    val case = iterator.next()
                    val caseName = nameFn(case.index, case.value).truncated(layerConfig.nameMaxLength)
                    test(
                        name = caseName,
                        testConfig = caseTestConfig,
                    ) {
                        withMatrixPropertyReplay(propertyReplayPath) { body(case.value) }
                    }
                    progress.registered(++registered)
                }
                progress.completed(registered)
            }
        }
    }

    fun <T> property(
        name: String,
        gen: Gen<T>,
        iterations: Int = this@MatrixSuiteScope.config.defaultPropertyIterations,
        nameFn: NameFn<T> = { index, value -> defaultLayerName(index, value) },
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): MatrixPropertyLayer<T> = MatrixPropertyLayer(this, name, gen, iterations, nameFn, config)

    internal fun <T> propertyInternal(
        name: String,
        gen: Gen<T>,
        iterations: Int,
        nameFn: NameFn<T>,
        config: PropertyLayerConfigBuilder.() -> Unit,
        body: MatrixSuiteScope.(T) -> Unit,
    ) {
        require(iterations >= 0) { "iterations must be >= 0" }
        val strippedName = matrixName(name)
        val layerConfig = PropertyLayerConfigBuilder(this.config).apply(config).build()
        val random = layerConfig.seed?.let { RandomSource.seeded(it) } ?: RandomSource.default()
        val seed = random.seed
        val propertyConfig = this@MatrixSuiteScope.config.copy(execution = layerConfig.execution)
        val caseLimiter = layerConfig.execution.caseLimiter()
        val caseTestConfig = propertyConfig.testConfig.boundBy(caseLimiter)
        target.apply {
            testSuite(strippedName, testConfig = propertyConfig.testConfig.disableByMatrixName(name)) {
                val progress = registrationProgress(strippedName, iterations.toLong(), registrationPath, registrationReporter)
                val iterator = propertyCases(gen, iterations, random, layerConfig.edgeConfig, layerConfig.replayIteration)
                var registered = 0L
                progress.registered(registered)
                while (iterator.hasNext()) {
                    val case = iterator.next()
                    val caseName = nameFn(case.index, case.value).truncated(layerConfig.nameMaxLength)
                    val replayFrame = MatrixPropertyReplayFrame(strippedName, seed, case.index, caseName)
                    testSuite(
                        name = caseName,
                        testConfig = caseTestConfig
                    ) {
                        MatrixSuiteScope(
                            this,
                            propertyConfig,
                            registrationPath + MatrixRegistrationFrame(strippedName, case.index, iterations.toLong()),
                            registrationReporter,
                            propertyReplayPath + replayFrame,
                        ).body(case.value)
                    }
                    progress.registered(++registered)
                }
                progress.completed(registered)
            }
        }
    }

    internal fun <T> propertyTestInternal(
        name: String,
        gen: Gen<T>,
        iterations: Int,
        nameFn: NameFn<T>,
        config: PropertyLayerConfigBuilder.() -> Unit,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        require(iterations >= 0) { "iterations must be >= 0" }
        val strippedName = matrixName(name)
        val layerConfig = PropertyLayerConfigBuilder(this.config).apply(config).build()
        val random = layerConfig.seed?.let { RandomSource.seeded(it) } ?: RandomSource.default()
        val seed = random.seed
        val propertyConfig = this@MatrixSuiteScope.config.copy(execution = layerConfig.execution)
        val caseLimiter = layerConfig.execution.caseLimiter()
        val caseTestConfig = propertyConfig.testConfig.boundBy(caseLimiter)
        target.apply {
            testSuite(strippedName, testConfig = propertyConfig.testConfig.disableByMatrixName(name)) {
                val progress = registrationProgress(strippedName, iterations.toLong(), registrationPath, registrationReporter)
                val iterator = propertyCases(gen, iterations, random, layerConfig.edgeConfig, layerConfig.replayIteration)
                var registered = 0L
                progress.registered(registered)
                while (iterator.hasNext()) {
                    val case = iterator.next()
                    val caseName = nameFn(case.index, case.value).truncated(layerConfig.nameMaxLength)
                    val replayFrame = MatrixPropertyReplayFrame(strippedName, seed, case.index, caseName)
                    test(
                        name = caseName,
                        testConfig = caseTestConfig,
                    ) {
                        withMatrixPropertyReplay(propertyReplayPath + replayFrame) { body(case.value) }
                    }
                    progress.registered(++registered)
                }
                progress.completed(registered)
            }
        }
    }

    fun compact(
        name: String = "compacted",
        config: CompactConfigBuilder.() -> Unit = {},
    ): MatrixCompactLayer = MatrixCompactLayer(this, name, config)

    internal fun compactInternal(
        name: String,
        config: CompactConfigBuilder.() -> Unit,
        body: CompactScope.() -> Unit,
    ) {
        val compactConfig = CompactConfigBuilder(this.config).apply(config).build()
        val planningScope = CompactScope(this.config, compactConfig)
        planningScope.body()
        val nodes = planningScope.nodes.toList()
        target.apply {
            test(name = matrixName(name), testConfig = this@MatrixSuiteScope.config.testConfig.disableByMatrixName(name)) {
                val run = CompactRun(name, compactConfig)
                when (val progress = compactConfig.progressIndicator) {
                    is Indicator.Heartbeat -> withCompactProgressHeartbeatSuspending(
                        progress.every,
                        { run.progressMessage() }
                    ) {
                        runCompactNodes(nodes, this@test, run, propertyReplayPath)
                    }

                    Indicator.None -> runCompactNodes(nodes, this@test, run, propertyReplayPath)
                }
                run.throwIfAny()
            }
        }
    }
}

class MatrixConfiguredSuite internal constructor(
    internal val scope: MatrixSuiteScope,
    internal val name: String,
    internal val testConfig: TestConfig,
)

class MatrixDataLayer<T> internal constructor(
    private val scope: MatrixSuiteScope,
    private val name: String,
    private val source: MatrixDataSource<T>,
    private val nameFn: NameFn<T>,
    private val config: DataLayerConfigBuilder.() -> Unit,
) {
    operator fun minus(body: MatrixSuiteScope.(T) -> Unit) {
        scope.dataInternal(name, source, nameFn, config, body)
    }

    infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) {
        scope.dataTestInternal(name, source, nameFn, config, body)
    }
}

class MatrixPropertyLayer<T> internal constructor(
    private val scope: MatrixSuiteScope,
    private val name: String,
    private val gen: Gen<T>,
    private val iterations: Int,
    private val nameFn: NameFn<T>,
    private val config: PropertyLayerConfigBuilder.() -> Unit,
) {
    operator fun minus(body: MatrixSuiteScope.(T) -> Unit) {
        scope.propertyInternal(name, gen, iterations, nameFn, config, body)
    }

    infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) {
        scope.propertyTestInternal(name, gen, iterations, nameFn, config, body)
    }
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

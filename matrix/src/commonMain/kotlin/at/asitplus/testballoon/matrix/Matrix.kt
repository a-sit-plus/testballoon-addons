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
import io.kotest.property.PropertyTesting
import io.kotest.property.RandomSource
import kotlin.coroutines.CoroutineContext

@DslMarker
annotation class MatrixTestDsl

public typealias NameFn<T> = (index: Long, value: T) -> String

@TestRegistering
public fun matrixSuite(
    @TestSuitePropertyName propertyName: String = "",
    execution: ExecutionMode? = null,
    defaultPropertyIterations: Int? = null,
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
public data class MatrixSuiteScope internal constructor(
    internal val target: TestSuiteScope,
    public val config: MatrixSuiteConfig,
    internal val registrationPath: List<MatrixRegistrationFrame> = emptyList(),
    internal val registrationReporter: MatrixRegistrationReporter = MatrixRegistrationReporter(),
    internal val propertyReplayPath: List<MatrixPropertyReplayFrame> = emptyList(),
) {
    @TestRegistering
    public fun testSuite(
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
    public fun test(
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
    public operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
        body: suspend Test.ExecutionScope.() -> Unit,
    ) {
        test(this, testConfig, body)
    }

    @TestRegistering
    public operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
    ): MatrixConfiguredSuite = MatrixConfiguredSuite(this@MatrixSuiteScope, this, testConfig)

    @TestRegistering
    public infix operator fun MatrixConfiguredSuite.minus(body: MatrixSuiteScope.() -> Unit) {
        scope.testSuite(name, testConfig, body)
    }

    @TestRegistering
    public infix operator fun String.minus(body: MatrixSuiteScope.() -> Unit) {
        testSuite(this, body = body)
    }

    public fun <T> data(
        name: String,
        values: Iterable<T>,
        nameFn: NameFn<T> = { index, value -> defaultLayerName(index, value) },
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): MatrixDataLayer<T> = MatrixDataLayer(this, name, IterableDataSource(values), nameFn, config)

    public fun <T> data(
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
        var index = 0L
        val iterator = source.open()
        val dataConfig = config.copy(execution = layerConfig.execution)
        val caseLimiter = layerConfig.execution.caseLimiter()
        val caseTestConfig = dataConfig.testConfig.boundBy(caseLimiter)
        target.apply {
            testSuite(name = strippedName, testConfig = dataConfig.testConfig.disableByMatrixName(name)) {
                val progress = registrationProgress(strippedName, source.knownSize, registrationPath, registrationReporter)
                progress.registered(index)
                while (iterator.hasNext()) {
                    val caseIndex = index
                    val value = iterator.next()
                    val caseName = nameFn(caseIndex, value).truncated(layerConfig.nameMaxLength)
                    testSuite(
                        name = caseName,
                        testConfig = caseTestConfig
                    ) {
                        MatrixSuiteScope(
                            this,
                            dataConfig,
                            registrationPath + MatrixRegistrationFrame(strippedName, caseIndex, source.knownSize),
                            registrationReporter,
                            propertyReplayPath,
                        ).body(value)
                    }
                    index++
                    progress.registered(index)
                }
                progress.completed(index)
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
        var index = 0L
        val iterator = source.open()
        val dataConfig = config.copy(execution = layerConfig.execution)
        val caseLimiter = layerConfig.execution.caseLimiter()
        val caseTestConfig = dataConfig.testConfig.boundBy(caseLimiter)
        target.apply {
            testSuite(name = strippedName, testConfig = dataConfig.testConfig.disableByMatrixName(name)) {
                val progress = registrationProgress(strippedName, source.knownSize, registrationPath, registrationReporter)
                progress.registered(index)
                while (iterator.hasNext()) {
                    val caseIndex = index
                    val value = iterator.next()
                    val caseName = nameFn(caseIndex, value).truncated(layerConfig.nameMaxLength)
                    test(
                        name = caseName,
                        testConfig = caseTestConfig,
                    ) {
                        withMatrixPropertyReplay(propertyReplayPath) { body(value) }
                    }
                    index++
                    progress.registered(index)
                }
                progress.completed(index)
            }
        }
    }

    public fun <T> property(
        name: String,
        gen: Gen<T>,
        iterations: Int = PropertyTesting.defaultIterationCount,
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
                val iterator = gen.generate(random, layerConfig.edgeConfig).take(iterations).iterator()
                var index = 0L
                progress.registered(index)
                while (iterator.hasNext()) {
                    progress.registered(index)
                    val caseIndex = index
                    val sample = iterator.next()
                    val value = sample.value
                    val caseName = nameFn(caseIndex, value).truncated(layerConfig.nameMaxLength)
                    val replayFrame = MatrixPropertyReplayFrame(strippedName, seed, caseIndex, caseName)
                    testSuite(
                        name = caseName,
                        testConfig = caseTestConfig
                    ) {
                        MatrixSuiteScope(
                            this,
                            propertyConfig,
                            registrationPath + MatrixRegistrationFrame(strippedName, caseIndex, iterations.toLong()),
                            registrationReporter,
                            propertyReplayPath + replayFrame,
                        ).body(value)
                    }
                    index++
                    progress.registered(index)
                }
                progress.completed(index)
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
                val iterator = gen.generate(random, layerConfig.edgeConfig).take(iterations).iterator()
                var index = 0L
                progress.registered(index)
                while (iterator.hasNext()) {
                    progress.registered(index)
                    val caseIndex = index
                    val sample = iterator.next()
                    val value = sample.value
                    val caseName = nameFn(caseIndex, value).truncated(layerConfig.nameMaxLength)
                    val replayFrame = MatrixPropertyReplayFrame(strippedName, seed, caseIndex, caseName)
                    test(
                        name = caseName,
                        testConfig = caseTestConfig,
                    ) {
                        withMatrixPropertyReplay(propertyReplayPath + replayFrame) { body(value) }
                    }
                    index++
                    progress.registered(index)
                }
                progress.completed(index)
            }
        }
    }

    public fun compact(
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
                        runVirtualNodes(nodes, this@test, run)
                    }

                    Indicator.None -> runVirtualNodes(nodes, this@test, run)
                }
                run.throwIfAny()
            }
        }
    }
}

public class MatrixConfiguredSuite internal constructor(
    internal val scope: MatrixSuiteScope,
    internal val name: String,
    internal val testConfig: TestConfig,
)

public class MatrixDataLayer<T> internal constructor(
    private val scope: MatrixSuiteScope,
    private val name: String,
    private val source: MatrixDataSource<T>,
    private val nameFn: NameFn<T>,
    private val config: DataLayerConfigBuilder.() -> Unit,
) {
    public operator fun minus(body: MatrixSuiteScope.(T) -> Unit) {
        scope.dataInternal(name, source, nameFn, config, body)
    }

    public infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) {
        scope.dataTestInternal(name, source, nameFn, config, body)
    }
}

public class MatrixPropertyLayer<T> internal constructor(
    private val scope: MatrixSuiteScope,
    private val name: String,
    private val gen: Gen<T>,
    private val iterations: Int,
    private val nameFn: NameFn<T>,
    private val config: PropertyLayerConfigBuilder.() -> Unit,
) {
    public operator fun minus(body: MatrixSuiteScope.(T) -> Unit) {
        scope.propertyInternal(name, gen, iterations, nameFn, config, body)
    }

    public infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) {
        scope.propertyTestInternal(name, gen, iterations, nameFn, config, body)
    }
}

public class MatrixCompactLayer internal constructor(
    private val scope: MatrixSuiteScope,
    private val name: String,
    private val config: CompactConfigBuilder.() -> Unit,
) {
    public operator fun minus(body: CompactScope.() -> Unit) {
        scope.compactInternal(name, config, body)
    }
}

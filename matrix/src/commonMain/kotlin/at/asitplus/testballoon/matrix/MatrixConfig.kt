package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.invocation
import io.kotest.property.EdgeConfig
import io.kotest.property.default
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal object MatrixTestDefaults {
    var execution: ExecutionMode = ExecutionMode.Sequential
    var defaultPropertyIterations: Int = 1000
    var defaultCompactConcurrency: CompactConcurrency = CompactConcurrency.Layered
    var defaultCompactReport: CompactReport = CompactReport.AllCases
    var defaultCompactAddSuppressedErrors: Boolean = false
    var defaultCompactReportRows: Int = 1024
    var defaultProgressIndicator: Indicator = Indicator.Heartbeat(every = 1.seconds)
    var defaultCompactCoroutineContext: CoroutineContext = Dispatchers.Default
    var defaultTestNameMaxLength: Int = 256
    var testSessionConfig: TestConfig = TestConfig
}

sealed interface ExecutionMode {
    data object Sequential : ExecutionMode
    data class Concurrent(val parallelism: Int= 128) : ExecutionMode {
        init {
            require(parallelism > 0) { "parallelism must be > 0" }
        }
    }
}

sealed interface CompactConcurrency {
    data object Layered : CompactConcurrency
    data class Shared(val parallelism: Int) : CompactConcurrency {
        init {
            require(parallelism > 0) { "parallelism must be > 0" }
        }
    }
}

sealed interface CompactReport {
    data object FailuresOnly : CompactReport
    data object AllCases : CompactReport
    data object SummaryOnly : CompactReport
}


sealed interface Indicator {
    data object None : Indicator
    data class Heartbeat(val every: Duration = 1.seconds) : Indicator
}


fun TestConfig.MatrixTestDefaults(config: MatrixSuiteConfigBuilder.() -> Unit) {
    val bld = MatrixSuiteConfigBuilder()
    config.invoke(bld)
    bld.build().let { suiteConfig ->
        MatrixTestDefaults.execution = suiteConfig.execution
        MatrixTestDefaults.defaultPropertyIterations = suiteConfig.defaultPropertyIterations
        MatrixTestDefaults.defaultCompactConcurrency = suiteConfig.defaultCompactConcurrency
        MatrixTestDefaults.defaultCompactReport = suiteConfig.defaultCompactReport
        MatrixTestDefaults.defaultCompactAddSuppressedErrors = suiteConfig.defaultCompactAddSuppressedErrors
        MatrixTestDefaults.defaultCompactReportRows = suiteConfig.defaultCompactReportRows
        MatrixTestDefaults.defaultTestNameMaxLength = suiteConfig.defaultTestNameMaxLength
        MatrixTestDefaults.defaultProgressIndicator = suiteConfig.defaultProgressIndicator
        MatrixTestDefaults.defaultCompactCoroutineContext = suiteConfig.defaultCompactCoroutineContext
        MatrixTestDefaults.testSessionConfig = this
    }
}

@MatrixTestDsl
class MatrixSuiteConfigBuilder internal constructor() {
    var execution: ExecutionMode? = null
    var defaultPropertyIterations: Int? = null
    var defaultCompactConcurrency: CompactConcurrency? = null
    var defaultCompactReport: CompactReport? = null
    var defaultCompactAddSuppressedErrors: Boolean? = null
    var defaultCompactReportRows: Int? = null
    var defaultProgressIndicator: Indicator? = null
    var defaultCompactCoroutineContext: CoroutineContext? = null
    var defaultTestNameMaxLength: Int? = null
    internal var testConfig: TestConfig? = null

    internal fun build(): MatrixSuiteConfig {
        val executionMode = execution ?: MatrixTestDefaults.execution
        return MatrixSuiteConfig(
            execution = executionMode,
            defaultPropertyIterations = defaultPropertyIterations ?: MatrixTestDefaults.defaultPropertyIterations,
            defaultCompactConcurrency = defaultCompactConcurrency ?: MatrixTestDefaults.defaultCompactConcurrency,
            defaultCompactReport = defaultCompactReport ?: MatrixTestDefaults.defaultCompactReport,
            defaultCompactAddSuppressedErrors = defaultCompactAddSuppressedErrors
                ?: MatrixTestDefaults.defaultCompactAddSuppressedErrors,
            defaultCompactReportRows = defaultCompactReportRows ?: MatrixTestDefaults.defaultCompactReportRows,
            defaultProgressIndicator = defaultProgressIndicator ?: MatrixTestDefaults.defaultProgressIndicator,
            defaultCompactCoroutineContext = defaultCompactCoroutineContext
                ?: MatrixTestDefaults.defaultCompactCoroutineContext,
            defaultTestNameMaxLength = defaultTestNameMaxLength ?: MatrixTestDefaults.defaultTestNameMaxLength,
            config = (testConfig?.let { MatrixTestDefaults.testSessionConfig.chainedWith(it) }
                ?: MatrixTestDefaults.testSessionConfig),
        )
    }
}

data class MatrixSuiteConfig internal constructor(
    val execution: ExecutionMode,
    val defaultPropertyIterations: Int,
    val defaultCompactConcurrency: CompactConcurrency,
    val defaultCompactReport: CompactReport,
    val defaultCompactAddSuppressedErrors: Boolean,
    val defaultCompactReportRows: Int,
    val defaultProgressIndicator: Indicator,
    val defaultCompactCoroutineContext: CoroutineContext,
    val defaultTestNameMaxLength: Int,
    private var config: TestConfig,
) {
    val testConfig: TestConfig by lazy {
        config.chainedWith(
            when (execution) {
                is ExecutionMode.Concurrent -> TestConfig.invocation(TestConfig.Invocation.Concurrent)
                ExecutionMode.Sequential -> TestConfig.invocation(TestConfig.Invocation.Sequential)
            }
        )
    }
}


@MatrixTestDsl
class DataLayerConfigBuilder internal constructor(private val parent: MatrixSuiteConfig) {
    var execution: ExecutionMode? = null
    var nameMaxLength: Int? = null

    internal fun build(replayIndexes: List<Long>? = null): DataLayerConfig = DataLayerConfig(
        execution = execution ?: parent.execution,
        nameMaxLength = nameMaxLength ?: parent.defaultTestNameMaxLength,
        replayIndexes = replayIndexes,
    )
}

data class DataLayerConfig internal constructor(
    val execution: ExecutionMode,
    val nameMaxLength: Int,
    val replayIndexes: List<Long>? = null,
)

/**
 * Coordinates to reproduce recorded property cases, copied from a failure's replay report.
 * [seed] and [iterations] always travel together — iteration indexes only reproduce values relative
 * to the seed that generated them, so they cannot be set independently.
 */
data class ReplayInput(val seed: Long, val iterations: List<Long>) {
    constructor(seed: Long, iteration: Long) : this(seed, listOf(iteration))
}

@MatrixTestDsl
class PropertyLayerConfigBuilder internal constructor(private val parent: MatrixSuiteConfig) {
    var execution: ExecutionMode? = null
    var seed: Long? = null
    var edgeConfig: EdgeConfig? = null
    var nameMaxLength: Int? = null

    internal fun build(replay: ReplayInput? = null): PropertyLayerConfig = PropertyLayerConfig(
        execution = execution ?: parent.execution,
        seed = replay?.seed ?: seed,
        edgeConfig = edgeConfig ?: EdgeConfig.default(),
        nameMaxLength = nameMaxLength ?: parent.defaultTestNameMaxLength,
        replayIterations = replay?.iterations,
    )
}

data class PropertyLayerConfig internal constructor(
    val execution: ExecutionMode,
    val seed: Long?,
    val edgeConfig: EdgeConfig,
    val nameMaxLength: Int,
    val replayIterations: List<Long>? = null,
)

@MatrixTestDsl
class CompactConfigBuilder internal constructor(private val parent: MatrixSuiteConfig) {
    var concurrency: CompactConcurrency? = null
    var report: CompactReport? = null
    var addSuppressedErrors: Boolean? = null
    var reportRows: Int? = null
    var progressIndicator: Indicator? = null
    var coroutineContext: CoroutineContext? = null

    internal fun build(): CompactConfig = CompactConfig(
        concurrency = concurrency ?: parent.defaultCompactConcurrency,
        report = report ?: parent.defaultCompactReport,
        addSuppressedErrors = addSuppressedErrors ?: parent.defaultCompactAddSuppressedErrors,
        reportRows = reportRows ?: parent.defaultCompactReportRows,
        progressIndicator = progressIndicator ?: parent.defaultProgressIndicator,
        coroutineContext = coroutineContext ?: parent.defaultCompactCoroutineContext,
    )
}

data class CompactConfig internal constructor(
    val concurrency: CompactConcurrency,
    val report: CompactReport,
    val addSuppressedErrors: Boolean,
    val reportRows: Int,
    val progressIndicator: Indicator,
    val coroutineContext: CoroutineContext,
)

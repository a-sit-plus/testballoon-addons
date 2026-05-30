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
    public var execution: ExecutionMode = ExecutionMode.Sequential
    public var defaultPropertyIterations: Int = 1000
    public var defaultCompactReport: CompactReport = CompactReport.FailuresOnly
    public var defaultCompactAddSuppressedErrors: Boolean = false
    public var defaultCompactReportRows: Int = 1024
    public var defaultProgressIndicator: Indicator = Indicator.Heartbeat(every = 1.seconds)
    public var defaultCompactCoroutineContext: CoroutineContext = Dispatchers.Default
    public var defaultTestNameMaxLength: Int = 256
    public var testSessionConfig: TestConfig = TestConfig
}

public sealed interface ExecutionMode {
    public data object Sequential : ExecutionMode
    public data class Concurrent(val parallelism: Int= 128) : ExecutionMode {
        init {
            require(parallelism > 0) { "parallelism must be > 0" }
        }
    }
}

public sealed interface CompactReport {
    public data object FailuresOnly : CompactReport
    public data object AllCases : CompactReport
    public data object SummaryOnly : CompactReport
}


public sealed interface Indicator {
    public data object None : Indicator
    public data class Heartbeat(val every: Duration = 1.seconds) : Indicator
}


fun TestConfig.MatrixTestDefaults(config: MatrixSuiteConfigBuilder.() -> Unit) {
    val bld = MatrixSuiteConfigBuilder()
    config.invoke(bld)
    bld.build().let { suiteConfig ->
        MatrixTestDefaults.execution = suiteConfig.execution
        MatrixTestDefaults.defaultPropertyIterations = suiteConfig.defaultPropertyIterations
        MatrixTestDefaults.defaultCompactReport = suiteConfig.defaultCompactReport
        MatrixTestDefaults.defaultCompactAddSuppressedErrors = true
        MatrixTestDefaults.defaultCompactReportRows = suiteConfig.defaultCompactReportRows
        MatrixTestDefaults.defaultTestNameMaxLength = suiteConfig.defaultTestNameMaxLength
        MatrixTestDefaults.defaultProgressIndicator = suiteConfig.defaultProgressIndicator
        MatrixTestDefaults.defaultCompactCoroutineContext = suiteConfig.defaultCompactCoroutineContext
        MatrixTestDefaults.testSessionConfig = this
    }
}

@MatrixTestDsl
public class MatrixSuiteConfigBuilder internal constructor() {
    public var execution: ExecutionMode? = null
    public var defaultPropertyIterations: Int? = null
    public var defaultCompactReport: CompactReport? = null
    public var defaultCompactAddSuppressedErrors: Boolean? = null
    public var defaultCompactReportRows: Int? = null
    public var defaultProgressIndicator: Indicator? = null
    public var defaultCompactCoroutineContext: CoroutineContext? = null
    public var defaultTestNameMaxLength: Int? = null
    internal var testConfig: TestConfig? = null

    internal fun build(): MatrixSuiteConfig {
        val executionMode = execution ?: MatrixTestDefaults.execution
        return MatrixSuiteConfig(
            execution = executionMode,
            defaultPropertyIterations = defaultPropertyIterations ?: MatrixTestDefaults.defaultPropertyIterations,
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

public data class MatrixSuiteConfig internal constructor(
    val execution: ExecutionMode,
    val defaultPropertyIterations: Int,
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
public class DataLayerConfigBuilder internal constructor(private val parent: MatrixSuiteConfig) {
    public var execution: ExecutionMode? = null
    public var nameMaxLength: Int? = null

    internal fun build(): DataLayerConfig = DataLayerConfig(
        execution = execution ?: parent.execution,
        nameMaxLength = nameMaxLength ?: parent.defaultTestNameMaxLength,
    )
}

public data class DataLayerConfig internal constructor(
    val execution: ExecutionMode,
    val nameMaxLength: Int,
)

@MatrixTestDsl
public class PropertyLayerConfigBuilder internal constructor(private val parent: MatrixSuiteConfig) {
    public var execution: ExecutionMode? = null
    public var seed: Long? = null
    public var edgeConfig: EdgeConfig? = null
    public var nameMaxLength: Int? = null

    internal fun build(): PropertyLayerConfig = PropertyLayerConfig(
        execution = execution ?: parent.execution,
        seed = seed,
        edgeConfig = edgeConfig ?: EdgeConfig.default(),
        nameMaxLength = nameMaxLength ?: parent.defaultTestNameMaxLength,
    )
}

public data class PropertyLayerConfig internal constructor(
    val execution: ExecutionMode,
    val seed: Long?,
    val edgeConfig: EdgeConfig,
    val nameMaxLength: Int,
)

@MatrixTestDsl
public class CompactConfigBuilder internal constructor(private val parent: MatrixSuiteConfig) {
    public var report: CompactReport? = null
    public var addSuppressedErrors: Boolean? = null
    public var reportRows: Int? = null
    public var progressIndicator: Indicator? = null
    public var coroutineContext: CoroutineContext? = null

    internal fun build(): CompactConfig = CompactConfig(
        report = report ?: parent.defaultCompactReport,
        addSuppressedErrors = addSuppressedErrors ?: parent.defaultCompactAddSuppressedErrors,
        reportRows = reportRows ?: parent.defaultCompactReportRows,
        progressIndicator = progressIndicator ?: parent.defaultProgressIndicator,
        coroutineContext = coroutineContext ?: parent.defaultCompactCoroutineContext,
    )
}

public data class CompactConfig internal constructor(
    val report: CompactReport,
    val addSuppressedErrors: Boolean,
    val reportRows: Int,
    val progressIndicator: Indicator,
    val coroutineContext: CoroutineContext,
)

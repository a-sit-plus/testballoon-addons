package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testScope
import io.kotest.property.EdgeConfig
import io.kotest.property.default
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline
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

    /**
     * A `TestConfig` for `aroundAll` / `aroundEach` / context / timeout wrappers.
     *
     * Set concurrency via [execution], **never** `TestConfig.invocation(...)`: matrix derives invocation from
     * [execution], and a conflicting one here can't be removed (TestBalloon configs are opaque) — it is overridden and
     * can break test discovery. A virtual-time `testScope(...)` applies only to *sequential* execution (matrix
     * auto-disables it under concurrent [execution]); prefer enabling `TestScope` on the `TestSession`, and for a
     * timeout under concurrency use `aroundEach`/`aroundAll` + `withTimeout` rather than `testScope`'s timeout.
     */
    var testConfig: TestConfig? = null

    // Unset fields fall back to [parent] (the enclosing matrix scope) when given, otherwise the global defaults.
    internal fun build(parent: MatrixSuiteConfig? = null): MatrixSuiteConfig =
        MatrixSuiteConfig(
            execution = execution ?: parent?.execution ?: MatrixTestDefaults.execution,
            defaultPropertyIterations = defaultPropertyIterations
                ?: parent?.defaultPropertyIterations ?: MatrixTestDefaults.defaultPropertyIterations,
            defaultCompactConcurrency = defaultCompactConcurrency
                ?: parent?.defaultCompactConcurrency ?: MatrixTestDefaults.defaultCompactConcurrency,
            defaultCompactReport = defaultCompactReport
                ?: parent?.defaultCompactReport ?: MatrixTestDefaults.defaultCompactReport,
            defaultCompactAddSuppressedErrors = defaultCompactAddSuppressedErrors
                ?: parent?.defaultCompactAddSuppressedErrors ?: MatrixTestDefaults.defaultCompactAddSuppressedErrors,
            defaultCompactReportRows = defaultCompactReportRows
                ?: parent?.defaultCompactReportRows ?: MatrixTestDefaults.defaultCompactReportRows,
            defaultProgressIndicator = defaultProgressIndicator
                ?: parent?.defaultProgressIndicator ?: MatrixTestDefaults.defaultProgressIndicator,
            defaultCompactCoroutineContext = defaultCompactCoroutineContext
                ?: parent?.defaultCompactCoroutineContext ?: MatrixTestDefaults.defaultCompactCoroutineContext,
            defaultTestNameMaxLength = defaultTestNameMaxLength
                ?: parent?.defaultTestNameMaxLength ?: MatrixTestDefaults.defaultTestNameMaxLength,
            // Only this scope's own testConfig; the parent's is inherited structurally by TestBalloon, not re-applied.
            config = testConfig ?: TestConfig,
        )
}

/**
 * Builds a reusable matrix configuration value to pass to `test` / `testSuite` (and their FreeSpec `"name"(…)` forms),
 * e.g. `testSuite("group", matrixConfig { execution = ExecutionMode.Concurrent(4) }) { … }`. Unset fields inherit the
 * enclosing matrix scope. `testConfig` is one of the fields, so this also carries `aroundAll` / `aroundEach` / context.
 *
 * Set concurrency via `execution`, **not** `testConfig = TestConfig.invocation(...)` (matrix derives invocation from
 * `execution`; a conflicting one is overridden and can break discovery). A `testScope(...)` (virtual time) applies only
 * to sequential execution — matrix auto-disables it when concurrent — so prefer enabling `TestScope` on the
 * `TestSession`, and use `aroundEach`/`aroundAll` + `withTimeout` for timeouts under concurrency.
 */
fun matrixConfig(block: MatrixSuiteConfigBuilder.() -> Unit): MatrixSuiteConfigBuilder =
    MatrixSuiteConfigBuilder().apply(block)

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
    /**
     * Just this scope's invocation (Sequential/Concurrent) — idempotent, so it is safe to apply at every level.
     * Concurrent execution additionally disables TestBalloon's virtual-time `TestScope`: real concurrency cannot run
     * in a `TestScope` (TestBalloon forbids the combination), so matrix turns the scope off wherever it parallelizes,
     * even when the surrounding session enabled it. Sequential execution leaves the inherited scope untouched.
     */
    internal val invocationConfig: TestConfig
        get() = when (execution) {
            is ExecutionMode.Concurrent ->
                TestConfig.invocation(TestConfig.Invocation.Concurrent).testScope(isEnabled = false)
            ExecutionMode.Sequential -> TestConfig.invocation(TestConfig.Invocation.Sequential)
        }

    /** The base [config] plus invocation — applied ONCE, at the top level of a matrix suite. */
    val testConfig: TestConfig by lazy { config.chainedWith(invocationConfig) }

    /**
     * Config for a NESTED matrix scope: drops the base [config]. TestBalloon already inherits a parent element's
     * config to its children structurally, so re-applying the base at each nested matrix level would multiply
     * stateful wrappers (`testScope`, `aroundAll`, …). Only the per-layer [execution] differs.
     */
    internal fun nested(execution: ExecutionMode): MatrixSuiteConfig = copy(execution = execution, config = TestConfig)
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
 * One recorded property case to reproduce, copied from a failure's replay report. [seed] and [iterations]
 * always travel together — iteration indexes only reproduce values relative to the seed that generated them,
 * so they cannot be set independently.
 */
data class Input(val seed: Long, val iterations: List<Long>) {
    constructor(seed: Long, vararg iter: Long) : this(seed, iter.toList())
    constructor(seed: Long, iter: LongRange) : this(seed, iter.toList())
}

/**
 * Data-layer replay selector: the case indexes to re-run, pasted from a failure report as `replay = Indexes(3L)`.
 * Construct from explicit indexes (`Indexes(0L, 2L)`) or a range (`Indexes(0L..9L)`).
 */
@JvmInline
value class Indexes private constructor(val indexes: List<Long>) {
    constructor(vararg index: Long) : this(index.toList())
    constructor(indexes: LongRange) : this(indexes.toList())
}

/**
 * Property-layer replay selector: the recorded cases to re-run, pasted as `replay = Cases(seed = 1L, iter = 2L)`.
 * The flat `(seed, iteration)` form covers the common single-case paste; use `Cases(Input(...), Input(...))` for
 * several seeds at once, or the `(seed, iterations)` vararg / range forms for several iterations of one seed.
 */
@JvmInline
value class Cases private constructor(val inputs: List<Input>) {
    constructor(vararg input: Input) : this(input.toList())
    constructor(seed: Long, iter: Long) : this(listOf(Input(seed, iter)))
    constructor(seed: Long, vararg iter: Long) : this(listOf(Input(seed, iter.toList())))
    constructor(seed: Long, iter: LongRange) : this(listOf(Input(seed, iter.toList())))
}

@MatrixTestDsl
class PropertyLayerConfigBuilder internal constructor(private val parent: MatrixSuiteConfig) {
    var execution: ExecutionMode? = null
    var seed: Long? = null
    var edgeConfig: EdgeConfig? = null
    var nameMaxLength: Int? = null

    internal fun build(replays: List<Input>? = null): PropertyLayerConfig = PropertyLayerConfig(
        execution = execution ?: parent.execution,
        seed = seed,
        edgeConfig = edgeConfig ?: EdgeConfig.default(),
        nameMaxLength = nameMaxLength ?: parent.defaultTestNameMaxLength,
        replays = replays,
    )
}

data class PropertyLayerConfig internal constructor(
    val execution: ExecutionMode,
    // Seed for a deterministic *full* run (ignored while replaying — each Input carries its own seed).
    val seed: Long?,
    val edgeConfig: EdgeConfig,
    val nameMaxLength: Int,
    val replays: List<Input>? = null,
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

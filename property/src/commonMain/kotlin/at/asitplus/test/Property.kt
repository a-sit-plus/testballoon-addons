package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.property.*


/**
 * Global knobs to tweak the behavior of PropertyTest Addon
 */
object PropertyTest {
    /**
     * If `true`, all `withData` and `checkAll` iterations will be compacted into one test (suite) instead of one each per iteration by default.
     * If `false` each iteration of `withData` and `checkAll` will create a new test (suite).
     */
    var compactByDefault = false

    /**
     * The default maximum length of test element names (not display name).
     * Defaults to [TestBalloonAddons.defaultTestNameMaxLength], but setting it here will take precedence.
     * * `-1` means no truncation.
     * * `null` means it will again fall back to [TestBalloonAddons.defaultTestNameMaxLength]
     *
     * This property's getter will never return null, but fall back to [TestBalloonAddons.defaultTestNameMaxLength].
     */
    var defaultTestNameMaxLength: Int? = null
        get() = field?:TestBalloonAddons.defaultTestNameMaxLength

    /**
     * Default number of iterations for property testing (`1000`)
     */
    var defaultIterationCount: Int = 1000

    /**
     * Whether compacted failure reports should attach every failed input as a suppressed throwable.
     *
     * This affects compacted `withData`, `withDataSuites`, `checkAll`, and `checkAllSuites` reports. The rendered
     * failure message still includes the stack trace of the first failure either way.
     *
     * `null` means it will again fall back to [TestBalloonAddons.addSuppressedErrorsToCompactedFailures]
     *
     *  This property's getter will never return null, but fall back to [TestBalloonAddons.addSuppressedErrorsToCompactedFailures].
     */
    var addSuppressedErrorsToCompactedFailures: Boolean? = null
        get() = field ?: TestBalloonAddons.addSuppressedErrorsToCompactedFailures

    /**
     * Whether compacted failure reports should omit successful input rows.
     *
     * Successes are still counted in the summary, but individual `OK` rows are not rendered when this is enabled.
     *
     * `null` means it will again fall back to [TestBalloonAddons.suppressCompactSuccesses]
     *
     *  This property's getter will never return null, but fall back to [TestBalloonAddons.suppressCompactSuccesses].
     */
    var suppressCompactSuccesses: Boolean? = null
        get() = field ?: TestBalloonAddons.suppressCompactSuccesses
}

class ConfiguredPropertyScope<Value>(
    private val compact: Boolean,
    private val maxLength: Int,
    val prefix: String,
    val testSuite: TestSuiteScope,
    val iterations: Int,
    val genA: Gen<Value>,
    val testConfig: TestConfig = TestConfig,
) {
    /**
     * @param content Test suite block receiving generated values
     */
    operator fun minus(content: context(PropertyContext) TestSuiteScope.(Value) -> Unit) {
        testSuite.checkAllSuitesInternal(
            iterations,
            genA,
            compact,
            maxLength,
            prefix,
            testConfig,
            content
        )
    }
}


/**
 * Internal function to handle series of property-based test executions.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param series Block to execute for each generated value
 */
private fun <Value> checkAllSeries(
    iterations: Int,
    genA: Gen<Value>,
    series: (Int, Value, PropertyContext) -> Unit
) {
    val constraints = Constraints.iterations(iterations)

    @Suppress("OPT_IN_USAGE")
    val config = PropTestConfig(constraints = constraints)
    val context = PropertyContext(config)
    genA.generate(RandomSource.default(), config.edgeConfig)
        .takeWhile { constraints.evaluate(context) }
        .forEachIndexed { iter, sample ->
            context.markEvaluation()
            catchingUnwrapped {
                series(iter, sample.value, context)
                context.markSuccess()
            }.getOrElse {
                context.markFailure()
                throw it
            }
        }
}

/**
 * Internal helper that produces a lazy sequence of generated values.
 *
 * @param iterations Number of elements to generate
 */
private fun <Value> Gen<Value>.generateSequence(
    iterations: Int,
): Pair<PropertyContext, Sequence<Value>> {
    val constraints = Constraints.iterations(iterations)

    @Suppress("OPT_IN_USAGE")
    val config = PropTestConfig(constraints = constraints)
    val context = PropertyContext(config)

    return context to sequence {

        generate(RandomSource.default(), config.edgeConfig)
            .takeWhile { constraints.evaluate(context) }
            .forEach { sample ->
                yield(sample.value)
            }
    }
}

private fun propertyIterationNamePrefix(
    normalizedPrefix: String,
    iter: Int,
    iterations: Int,
    value: Any?,
    suffix: String
): String =
    "$normalizedPrefix${iter + 1} of $iterations ${value.typeDisplayName()}$suffix"

private fun compactPropertyCaseName(iter: Int, iterations: Int, value: Any?, maxLength: Int): String {
    val namePrefix = propertyIterationNamePrefix("", iter, iterations, value, ": ")
    val valueStr = value.toPrettyString(maxLength, namePrefix.length)
    return "$namePrefix$valueStr"
}

internal fun generatedPropertyLeafName(
    normalizedPrefix: String,
    iter: Int,
    iterations: Int,
    value: Any?,
    maxLength: Int
): String {
    val namePrefix = propertyIterationNamePrefix(normalizedPrefix, iter, iterations, value, ": ")
    val valueStr = value.toPrettyString(maxLength, namePrefix.length)
    return "$namePrefix$valueStr"
}

private fun generatedPropertySuiteName(
    normalizedPrefix: String,
    iter: Int,
    iterations: Int,
    value: Any?,
    maxLength: Int
): String {
    val namePrefix = propertyIterationNamePrefix(normalizedPrefix, iter, iterations, value, "s (")
    val valueStr = value.toPrettyString(maxLength, namePrefix.length + 1)
    return "$namePrefix$valueStr)"
}

private inline fun <Value> PropertyContext.runCompactedPropertyResults(
    series: Sequence<Value>,
    iterations: Int,
    testName: String,
    maxLength: Int,
    suppressCompactSuccesses: Boolean?,
    content: (Value) -> Result<Unit>
) {
    val run = CollatedTestRun(
        testName,
        PropertyTest.addSuppressedErrorsToCompactedFailures!!,
        suppressCompactSuccesses ?: PropertyTest.suppressCompactSuccesses!!
    )
    series.forEachIndexed { iter, value ->
        markEvaluation()
        run.record(
            name = compactPropertyCaseName(iter, iterations, value, maxLength),
            result = content(value),
            onSuccess = { markSuccess() },
            onFailure = { markFailure() }
        )
    }
    run.throwIfAny()
}

internal fun <Value> PropertyContext.runCompactedProperty(
    series: Sequence<Value>,
    iterations: Int,
    testName: String,
    maxLength: Int,
    suppressCompactSuccesses: Boolean? = null,
    content: context(PropertyContext) (Value) -> Unit
) = runCompactedPropertyResults(series, iterations, testName, maxLength, suppressCompactSuccesses) {
    catchingUnwrapped {
        content(it)
    }
}

internal suspend fun <Value> PropertyContext.runCompactedPropertySuspend(
    series: Sequence<Value>,
    iterations: Int,
    testName: String,
    maxLength: Int,
    suppressCompactSuccesses: Boolean? = null,
    content: suspend context(PropertyContext) (Value) -> Unit
) = runCompactedPropertyResults(series, iterations, testName, maxLength, suppressCompactSuccesses) {
    catchingUnwrapped {
        content(it)
    }
}

internal fun <Value> TestSuiteScope.checkAllSuitesInternal(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean,
    maxLength: Int,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuiteScope.(Value) -> Unit
) {
    val prefix = prefix.normalizedTestPrefix()
    if (!compact) {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val name = generatedPropertySuiteName(prefix, iter, iterations, value, maxLength)
            this@checkAllSuitesInternal.testSuite(
                name = (name.truncated(maxLength)),
                testConfig = testConfig,
                content = fun TestSuiteScope.() {
                    with(context) {
                        content(value)
                    }
                })
        }
    } else {
        val (context, sequence) = genA.generateSequence(iterations)
        val (testName, series) = sequence.compactTestNameAndReplay(prefix) { it }
        this@checkAllSuitesInternal.testSuite(
            name = (testName.truncated(maxLength)),
            testConfig = testConfig
        ) {
            with(context) {
                runCompactedProperty(series, iterations, testName, maxLength) { content(it) }
            }
        }
    }
}


/**
 * Executes property-based tests with generated values.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 * @param content Test execution block receiving generated values
 */
internal fun <Value> TestSuiteScope.checkAllInternal(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean,
    suppressCompactSuccesses: Boolean?,
    maxLength: Int,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) Test.ExecutionScope.(Value) -> Unit
) {
    val prefix = prefix.normalizedTestPrefix()
    if (compact) {
        val (context, sequence) = genA.generateSequence(iterations)
        val (testName, series) = sequence.compactTestNameAndReplay(prefix) { it }
        this@checkAllInternal.test(
            name = (testName.truncated(maxLength)),
            testConfig = testConfig
        ) {
            with(context) {
                runCompactedPropertySuspend(series, iterations, testName, maxLength, suppressCompactSuccesses) { content(it) }
            }
        }
    } else {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val name = generatedPropertyLeafName(prefix, iter, iterations, value, maxLength)
            this@checkAllInternal.test(
                name = (name.truncated(maxLength)),
                testConfig = testConfig
            ) {
                with(context) {
                    content(value)
                }
            }
        }
    }
}

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
}

class ConfiguredPropertyScope<Value>(
    private val compact: Boolean,
    private val maxLength: Int,
    val prefix: String,
    val testSuite: TestSuiteScope,
    val iterations: Int,
    val genA: Gen<Value>,
    val testConfig: TestConfig = TestConfig
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

internal fun <Value> TestSuiteScope.checkAllSuitesInternal(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean,
    maxLength: Int,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuiteScope.(Value) -> Unit
) {
    val prefix = if (prefix.isNotEmpty()) "$prefix " else ""
    if (!compact) {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val type = if (value == null) "null" else value::class.simpleName
            val namePrefix = "$prefix${iter + 1} of $iterations ${type}s ("
            val valueStr = value.toPrettyString(maxLength, namePrefix.length + 1)
            val name = "$prefix${iter + 1} of $iterations ${type}s (${valueStr})"
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
        val (compactName, series) = sequence.peekTypeNameAndReplay { it }
        val testName = "${prefix}Σ$compactName"
        this@checkAllSuitesInternal.testSuite(
            name = (testName.truncated(maxLength)),
            testConfig = testConfig
        ) {
            val errors = CollatedTestFailures(testName, PropertyTest.addSuppressedErrorsToCompactedFailures!!)
            series.forEachIndexed { iter, value ->
                with(context) {
                    markEvaluation()
                    val valueStr = value.toPrettyString()
                    val name =
                        "${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
                    catchingUnwrapped {
                        content(value)
                        markSuccess()
                        errors.recordOk(name)
                    }.onFailure {
                        markFailure()
                        errors.recordError(name, it)
                    }
                }
            }
            errors.throwIfAny()
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
    maxLength: Int,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) Test.ExecutionScope.(Value) -> Unit
) {
    val prefix = if (prefix.isNotEmpty()) "$prefix " else ""
    if (compact) {
        val (context, sequence) = genA.generateSequence(iterations)
        val (compactName, series) = sequence.peekTypeNameAndReplay { it }
        val testName = "${prefix}Σ$compactName"
        this@checkAllInternal.test(
            name = (testName.truncated(maxLength)),
            testConfig = testConfig
        ) {
            val errors = CollatedTestFailures(testName, PropertyTest.addSuppressedErrorsToCompactedFailures!!)
            series.forEachIndexed { iter, value ->
                with(context) {
                    markEvaluation()
                    val valueStr = value.toPrettyString()
                    val name =
                        "${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
                    catchingUnwrapped {
                        content(value)
                        markSuccess()
                        errors.recordOk(name)
                    }.onFailure {
                        markFailure()
                        errors.recordError(name, it)
                    }
                }
            }
            errors.throwIfAny()
        }
    } else {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val type = if (value == null) "null" else value::class.simpleName
            val namePrefix = "$prefix ${iter + 1} of $iterations $type: "
            val valueStr = value.toPrettyString(maxLength, namePrefix.length)
            val name =
                "$prefix ${iter + 1} of $iterations $type: $valueStr"
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

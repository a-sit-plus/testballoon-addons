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
     * The default maximum length of test element names (not display name). Default = 64. `-1` means no truncation
     */
    var defaultTestNameMaxLength: Int = DEFAULT_TEST_NAME_MAX_LEN

    /**
     * The default maximum length of test element names (not display name). Default = -1 (no truncation)
     */
    var defaultDisplayNameMaxLength: Int = -1
}


data class ConfiguredPropertyScope<Value>(
    private val compact: Boolean,
    private val maxLength: Int,
    private val displayNameMaxLength: Int,
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
            displayNameMaxLength,
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
    displayNameMaxLength: Int,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuiteScope.(Value) -> Unit
) {
    val prefix = if (prefix.isNotEmpty()) "$prefix " else ""
    if (!compact) {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val valueStr = value.toPrettyString()
            val type = if (value == null) "null" else value::class.simpleName
            val name = "$prefix${iter + 1} of $iterations ${type}s (${valueStr})"
            this@checkAllSuitesInternal.testSuite(
                name = name.truncated(maxLength).escaped,
                displayName = name.truncated(displayNameMaxLength).escaped,
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
        val testName = "$prefix[*] $compactName"
        this@checkAllSuitesInternal.testSuite(
            name = testName.truncated(maxLength).escaped,
            displayName = testName.truncated(displayNameMaxLength).escaped,
            testConfig = testConfig
        ) {
            val errors = mutableMapOf<String, Throwable?>()
            series.forEachIndexed { iter, value ->
                with(context) {
                    markEvaluation()
                    val valueStr = value.toPrettyString()
                    val name =
                        "${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
                    catchingUnwrapped {
                        content(value)
                        markSuccess()
                        errors["OK:    $name"] = null
                    }.onFailure {
                        markFailure()
                        errors["Error: $name"] = it
                    }
                }
            }
            collateErrors(errors, testName)
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
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 * @param content Test execution block receiving generated values
 */
internal fun <Value> TestSuiteScope.checkAllInternal(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean,
    maxLength: Int,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) Test.ExecutionScope.(Value) -> Unit
) {
    val prefix = if (prefix.isNotEmpty()) "$prefix " else ""
    if (compact) {
        val (context, sequence) = genA.generateSequence(iterations)
        val (compactName, series) = sequence.peekTypeNameAndReplay { it }
        val testName = "$prefix[*] $compactName"
        this@checkAllInternal.test(
            name = testName.truncated(maxLength).escaped,
            displayName = testName.truncated(displayNameMaxLength).escaped,
            testConfig = testConfig
        ) {
            val errors = mutableMapOf<String, Throwable?>()
            series.forEachIndexed { iter, value ->
                with(context) {
                    markEvaluation()
                    val valueStr = value.toPrettyString()
                    val name =
                        "${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
                    catchingUnwrapped {
                        content(value)
                        markSuccess()
                        errors["OK:    $name"] = null
                    }.onFailure {
                        markFailure()
                        errors["Error: $name"] = it
                    }
                }
            }
            collateErrors(errors, testName)
        }
    } else {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val valueStr = value.toPrettyString()
            val name =
                "$prefix ${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
            this@checkAllInternal.test(
                name = name.truncated(maxLength).escaped,
                displayName = name.truncated(displayNameMaxLength).escaped,
                testConfig = testConfig
            ) {
                with(context) {
                    content(value)
                }
            }
        }
    }
}

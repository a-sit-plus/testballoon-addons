package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite
import io.kotest.property.*

/**
 * Executes property-based tests with generated values.
 *
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param content Test execution block receiving generated values
 */
fun <Value> TestSuite.checkAll(
    genA: Gen<Value>,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) TestExecutionScope.(Value) -> Unit
) = checkAll(PropertyTesting.defaultIterationCount, genA, testConfig, content)

/**
 * Executes property-based tests with generated values.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param content Test execution block receiving generated values
 */
fun <Value> TestSuite.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) TestExecutionScope.(Value) -> Unit
) {
    var count = 0
    checkAllSeries(iterations, genA) { value, context ->
        count++
        val name = "$count of $iterations ${if (value == null) "null" else value::class.simpleName}: $value"
        this@checkAll.test(name, testConfig = testConfig) {
            with(context) {
                content(value)
            }
        }
    }
}

data class ConfiguredPropertyScope<Value>(
    val testSuite: TestSuite,
    val iterations: Int,
    val genA: Gen<Value>,
    val testConfig: TestConfig = TestConfig
) {
    /**
     * @param content Test suite block receiving generated values
     */
    operator fun minus(content: context(PropertyContext) TestSuite.(Value) -> Unit) {
        testSuite.checkAllSuites(iterations, genA, testConfig, content)
    }
}

/**
 * Creates a configured property scope for property-based testing with specified iterations.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 */
fun <Value> TestSuite.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    testConfig: TestConfig = TestConfig
) = ConfiguredPropertyScope(this, iterations, genA, testConfig)

/**
 * Creates test suites for property-based testing with specified iterations.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
fun <Value> TestSuite.checkAllSuites(
    iterations: Int,
    genA: Gen<Value>,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuite.(Value) -> Unit
) {

    var count = 0
    checkAllSeries(iterations, genA) { value, context ->
        count++
        val prefix = if (value == null) "null" else value::class.simpleName
        this@checkAllSuites.testSuite(
            name = "$count-${iterations}_${prefix}_${value.toString()}",
            testConfig = testConfig,
            content = fun TestSuite.() {
                with(context) {
                    content(value)
                }
            })
    }
}

/**
 * Creates a configured property scope for property-based testing using default iteration count.
 *
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
fun <A> TestSuite.checkAll(
    genA: Gen<A>,
    testConfig: TestConfig = TestConfig,
) = ConfiguredPropertyScope(this, PropertyTesting.defaultIterationCount, genA, testConfig)

/**
 * Creates test suites for property-based testing using default iteration count.
 *
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
fun <A> TestSuite.checkAllSuites(
    genA: Gen<A>,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuite.(A) -> Unit
) = checkAllSuites(PropertyTesting.defaultIterationCount, genA, testConfig, content)

/**
 * Internal function to handle series of property-based test executions.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param series Block to execute for each generated value
 */
private inline fun <Value> checkAllSeries(iterations: Int, genA: Gen<Value>, series: (Value, PropertyContext) -> Unit) {
    val constraints = Constraints.iterations(iterations)

    @Suppress("OPT_IN_USAGE")
    val config = PropTestConfig(constraints = constraints)
    val context = PropertyContext(config)
    genA.generate(RandomSource.default(), config.edgeConfig)
        .takeWhile { constraints.evaluate(context) }
        .forEach { sample ->
            context.markEvaluation()
            series(sample.value, context)
            context.markSuccess()
        }
}
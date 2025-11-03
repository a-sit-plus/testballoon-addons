package at.asitplus.testballoon

import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestExecutionScope
import de.infix.testBalloon.framework.TestSuite
import io.kotest.property.*


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

fun <A> TestSuite.checkAllSuites(
    genA: Gen<A>,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuite.(A) -> Unit
) = checkAllSuites(PropertyTesting.defaultIterationCount, genA, testConfig, content)

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
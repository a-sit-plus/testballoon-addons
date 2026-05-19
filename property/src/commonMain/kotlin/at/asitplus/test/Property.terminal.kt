package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.property.Gen
import io.kotest.property.PropertyContext
import io.kotest.property.PropertyTesting


/**
 * Executes property-based tests with generated values.
 *
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 * @param suppressCompactSuccesses whether to omit successful compacted child rows for this series
 * @param compactConcurrent whether to run compacted child bodies sequentially for this series
 * @param content Test execution block receiving generated values
 */
fun <Value> TestSuiteScope.checkAll(
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
    suppressCompactSuccesses: Boolean? = null,
    compactConcurrent: Boolean? = null,
    content: suspend context(PropertyContext) Test.ExecutionScope.(Value) -> Unit
) = checkAll(
    iterations = PropertyTest.defaultIterationCount,
    genA = genA,
    compact = compact,
    maxLength = maxLength,
    prefix = prefix,
    testConfig = testConfig,
    suppressCompactSuccesses = suppressCompactSuccesses,
    compactConcurrent = compactConcurrent,
    content = content
)

/**
 * Executes property-based tests with generated values.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 * @param suppressCompactSuccesses whether to omit successful compacted child rows for this series
 * @param compactConcurrent whether to run compacted child bodies sequentially for this series
 * @param content Test execution block receiving generated values
 */
fun <Value> TestSuiteScope.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
    suppressCompactSuccesses: Boolean? = null,
    compactConcurrent: Boolean? = null,
    content: suspend context(PropertyContext) Test.ExecutionScope.(Value) -> Unit
) = checkAllInternal(
    iterations,
    genA,
    compact,
    suppressCompactSuccesses,
    compactConcurrent,
    maxLength,
    prefix,
    testConfig,
) { content(it) }

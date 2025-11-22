package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite
import io.kotest.property.Gen
import io.kotest.property.PropertyContext
import io.kotest.property.PropertyTesting


/**
 * Executes property-based tests with generated values.
 *
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param content Test execution block receiving generated values
 */
fun <Value> TestSuite.checkAll(
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) TestExecutionScope.(Value) -> Unit
) = checkAll(
    PropertyTesting.defaultIterationCount,
    genA,
    compact,
    maxLength,
    displayNameMaxLength,
    testConfig,
    content
)

/**
 * Executes property-based tests with generated values.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param content Test execution block receiving generated values
 */
fun <Value> TestSuite.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) TestExecutionScope.(Value) -> Unit
) = checkAllInternal(
    iterations,
    genA,
    compact,
    maxLength,
    displayNameMaxLength,
    testConfig,
) { content(it) }

package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope
import io.kotest.property.Gen
import io.kotest.property.PropertyContext
import io.kotest.property.PropertyTesting

/**
 * Creates a configured property scope for property-based testing using default iteration count.
 *
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 */
fun <A> TestSuiteScope.checkAll(
    genA: Gen<A>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
) = ConfiguredPropertyScope(
    compact,
    maxLength,
    prefix,
    this,
    PropertyTest.defaultIterationCount,
    genA,
    testConfig
)


/**
 * Creates a configured property scope for property-based testing with specified iterations.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 */
fun <Value> TestSuiteScope.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
) = ConfiguredPropertyScope(
    compact,
    maxLength,
    prefix,
    this,
    iterations,
    genA,
    testConfig
)

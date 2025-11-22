package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import io.kotest.property.Gen
import io.kotest.property.PropertyContext
import io.kotest.property.PropertyTesting

/**
 * Creates a configured property scope for property-based testing using default iteration count.
 *
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
fun <A> TestSuite.checkAll(
    genA: Gen<A>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredPropertyScope(
    compact,
    maxLength,
    displayNameMaxLength,
    this,
    PropertyTesting.defaultIterationCount,
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
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
fun <Value> TestSuite.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredPropertyScope(
    compact,
    maxLength,
    displayNameMaxLength,
    this,
    iterations,
    genA,
    testConfig
)


/**
 * Creates test suites for property-based testing using default iteration count.
 *
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
fun <A> TestSuite.checkAllSuites(
    genA: Gen<A>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuite.(A) -> Unit
) = checkAllSuitesInternal(
    PropertyTesting.defaultIterationCount,
    genA,
    compact,
    maxLength,
    displayNameMaxLength,
    testConfig,
    content
)



/**
 * Creates test suites for property-based testing with specified iterations.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
fun <Value> TestSuite.checkAllSuites(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuite.(Value) -> Unit
) = checkAllSuitesInternal(
    iterations,
    genA,
    compact,
    maxLength,
    displayNameMaxLength,
    testConfig
) { content(it) }

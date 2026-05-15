package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope


/**
 * Creates a configured test suite scope to generate test suites for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuiteScope.withData(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    compact,
    maxLength,
    prefix = prefix,
    this,
    data.asSequence().map { it.toPrettyString() to it },
    testConfig
)


/**
 * Creates a configured test suite scope to generate test suites for each item in the provided iterable data.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The iterable collection of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuiteScope.withData(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    compact,
    maxLength,
    prefix = prefix,
    this,
    data.asSequence().map { nameFn(it) to it },
    testConfig
)

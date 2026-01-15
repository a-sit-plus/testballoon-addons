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
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuiteScope.withData(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
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
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuiteScope.withData(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    data.asSequence().map { nameFn(it) to it },
    testConfig
)


/**
 * Creates a test suite for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each data item
 */
fun <Data> TestSuiteScope.withDataSuites(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: TestSuiteScope.(Data) -> Unit
) = withDataSuitesInternal(
    data.map { it.toPrettyString() to it }.asSequence(),
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

/**
 * Creates a test suite for each item in the provided iterable data.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each data item
 */
fun <Data> TestSuiteScope.withDataSuites(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: TestSuiteScope.(Data) -> Unit
) = withDataSuitesInternal(
    data.map { nameFn(it) to it }.asSequence(),
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

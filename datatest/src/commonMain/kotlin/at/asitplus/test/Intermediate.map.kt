package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope


/**
 * Creates a configured test suite scope to generate test suites for each entry in the provided map.
 * Uses map keys as suite names.
 *
 * @param map Map of suite names to test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuiteScope.withData(
    map: Map<String, Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    compact,
    maxLength,
    prefix = prefix,
    this,
    map.asSequence().map { (k, v) -> k to v },
    testConfig
)

/**
 * Creates a test suite for each entry in the provided map.
 * Uses map keys as suite names.
 *
 * @param map Map of suite names to test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param action Test suite configuration action for each map value
 */
@Deprecated("will be removed in 0.9.0", ReplaceWith("withData(map, compact, maxLength, prefix, testConfig) - {}"))
fun <Data> TestSuiteScope.withDataSuites(
    map: Map<String, Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
    action: TestSuiteScope.(Data) -> Unit
) = withDataSuitesInternal(
    map.map { (k, v) -> k to v }.asSequence(),
    compact,
    maxLength,
    prefix = prefix,
    testConfig,
    action
)


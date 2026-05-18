package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope


/**
 * Executes a test for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 * @param suppressCompactSuccesses whether to omit successful compacted child rows for this series
 * @param action Test action to execute for each data item
 */
fun <Data> TestSuiteScope.withData(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
    suppressCompactSuccesses: Boolean? = null,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    data.asSequence().map { generatedDataName(it, compact, maxLength, prefix) to it },
    testConfig,
    compact,
    suppressCompactSuccesses,
    maxLength,
    prefix,
    action
)


/**
 * Executes a test for each item in the provided iterable data.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param data The iterable collection of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 * @param suppressCompactSuccesses whether to omit successful compacted child rows for this series
 * @param action Test action to execute for each data item
 */
fun <Data> TestSuiteScope.withData(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
    suppressCompactSuccesses: Boolean? = null,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    data.asSequence().map { nameFn(it) to it },
    testConfig,
    compact,
    suppressCompactSuccesses,
    maxLength,
    prefix,
    action
)

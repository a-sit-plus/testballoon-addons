package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.TestSuiteScope


/**
 * Executes a test for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each data item
 */
fun <Data> TestSuiteScope.withData(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    data.asSequence().map { it.toPrettyString() to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
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
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each data item
 */
fun <Data> TestSuiteScope.withData(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    data.asSequence().map { nameFn(it) to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)


package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope


/**
 * Executes a test for each item in the provided sequence.
 *
 * @param data The sequence of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each sequence item
 */
fun <Data> TestSuiteScope.withData(
    data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    data.map { it.toPrettyString() to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    prefix,
    action
)

/**
 * Executes a test for each item in the provided sequence.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param data The sequence of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param prefix an optional prefix to add to the test name
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each sequence item
 */
fun <Data> TestSuiteScope.withData(
    nameFn: (Data) -> String,
    data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength!!,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength!!,
    prefix: String = "",
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    data.map { nameFn(it) to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    prefix,
    action
)


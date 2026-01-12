package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite

/**
 * Executes a test for each provided data parameter.
 *
 * @param parameters The data parameters to test with
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    parameters.asSequence().map { it.toPrettyString() to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)

/**
 * Executes a test for each provided data parameter.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param arguments The data parameters to test with
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String,
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) = withDataInternal(
    parameters.asSequence().map { nameFn(it) to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)
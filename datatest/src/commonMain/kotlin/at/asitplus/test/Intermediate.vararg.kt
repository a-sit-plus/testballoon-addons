package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite


/**
 * Creates a configured test suite scope to generate test suites for each provided data parameter.
 *
 * @param parameters The data parameters to create suites for
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    parameters.asSequence().map { it.toPrettyString() to it },
    testConfig
)


/**
 * Creates a configured test suite scope to generate test suites for each provided data parameter.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param parameters The data parameters to create suites for
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String,
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    parameters.asSequence().map { nameFn(it) to it },
    testConfig
)


/**
 * Creates a test suite for each provided data parameter.
 *
 * @param parameters The data parameters to create suites for
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withDataSuites(
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    parameters.map { it.toPrettyString() to it }.asSequence(),
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)


/**
 * Creates a test suite for each provided data parameter.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param parameters The data parameters to create suites for
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    parameters.map { nameFn(it) to it }.asSequence(),
    compact,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

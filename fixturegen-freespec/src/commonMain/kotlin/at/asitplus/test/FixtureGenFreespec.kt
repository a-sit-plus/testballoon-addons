package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope


context(fixture: GeneratingFixtureScope<T>)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @param maxLength maximum length of test element name (not display name)
 * @param nested The test body to execute.
 */
operator fun <T> String.invoke(
    maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    testConfig: TestConfig = TestConfig,
    nested: suspend Test.ExecutionScope.(T) -> Unit
) {
    with(fixture.testSuite) {
        val truncatedName = freeSpecName(this@invoke).truncated(maxLength)
        testSuiteInScope.checkPathLenIncluding(truncatedName)
        test(
            truncatedName,
            testConfig = testConfig.disableByName(this@invoke)
        ) {
            nested(fixture.generator())
        }
    }
}


context(fixture: NonSuspendingGeneratingFixtureScope<T>)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @param maxLength maximum length of test element name (not display name)
 * @param nested The test body to execute.
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
operator fun <T> String.invoke(
    maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    testConfig: TestConfig = TestConfig,
    nested: suspend Test.ExecutionScope.(T) -> Unit
) {
    fixture.test(
        (freeSpecName(this@invoke).truncated(maxLength)),
        testConfig = testConfig.disableByName(this@invoke)
    ) {
        nested(fixture.generator())
    }
}


context(fixture: NonSuspendingGeneratingFixtureScope<T>)
/**
 * Creates a test suite with the specified name and configuration.
 *
 * @param suiteBody The test suite body to execute.
 */
infix operator fun <T> String.minus(suiteBody: TestSuiteScope.(T) -> Unit) = fixture.testSuite(
    freeSpecName(this),
    testConfig = TestConfig.disableByName(this),
    content = suiteBody
)

context(fixture: NonSuspendingGeneratingFixtureScope<T>)
/**
 * Creates a configured suite with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @param maxLength maximum length of test element name (not display name)
 * @return A new [ConfiguredSuite] instance.
 */
operator fun <T> String.invoke(
    maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    testConfig: TestConfig = TestConfig
) = ConfiguredSuite(
    fixture.testSuite,
    maxLength,
    freeSpecName(this),
    testConfig
)

context(fixture: NonSuspendingGeneratingFixtureScope<T>)
infix operator fun <T> ConfiguredSuite.minus(suiteBody: TestSuiteScope.(T) -> Unit) = with(fixture.testSuite) {
    var truncatedName = testName.truncated(maxLength)
    testSuiteInScope.checkPathLenIncluding(truncatedName)
    testSuite(
        truncatedName,
        testConfig = config.disableByName(testName)
    ) { suiteBody(fixture.generator()) }
}

package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
//import de.infix.testBalloon.framework.core.TestSuite
//import kotlinx.coroutines.runBlocking


context(fixture: GeneratingFixtureScope<T>)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @property displayName optional display name override
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param nested The test body to execute.
 */
operator fun <T> String.invoke(
    displayName: String = this,
    maxLength: Int = FreeSpec.defaultTestNameMaxLength,
    displayNameMaxLength: Int = FreeSpec.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
     nested: suspend TestExecutionScope.(T) -> Unit
) {
    fixture.testSuite.test(
        freeSpecName(this@invoke).truncated(maxLength).escaped,
        displayName = displayName.truncated(displayNameMaxLength).escaped,
        testConfig = testConfig.disableByName(this@invoke)
    ) {
        nested(fixture.generator())
    }
}
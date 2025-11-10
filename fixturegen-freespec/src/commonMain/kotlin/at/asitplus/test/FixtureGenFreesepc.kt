package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite


context(fixture: GeneratingFixtureScope<T>)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @property displayName optional display name override
 * @param nested The test body to execute.
 */
inline operator fun <reified T> String.invoke(
    displayName: String = this,
    testConfig: TestConfig = TestConfig,
    crossinline nested: suspend TestExecutionScope.(T) -> Unit
) {
    fixture.testSuite.apply {
        this@invoke.freespec(displayName, fixture.testSuite, testConfig) {
            nested(fixture.generator())
        }
    }
}


//need to replicate freespec leaf functionality here to disambiguate
@PublishedApi
internal fun String.freespec(
    displayName: String = this,
    suite: TestSuite,
    testConfig: TestConfig = TestConfig,
    nested: suspend TestExecutionScope.() -> Unit
) {
    suite.test(freeSpecName(this).truncated(), displayName = displayName.escaped, testConfig = testConfig.disableByName(this)) { nested() }
}

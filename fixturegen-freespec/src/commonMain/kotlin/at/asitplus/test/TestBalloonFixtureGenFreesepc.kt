package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite
import de.infix.testBalloon.framework.core.disable


context(fixture: MutatingFixtureScope<T>)
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
    suite.test(testName2(this), displayName = displayName, testConfig = testConfig.disableByName2(this)) { nested() }
}

private fun TestConfig.disableByName2(name: String) =
    if (name.startsWith("!")) TestConfig.disable() else this

private inline fun testName2(name: String) = if (name.startsWith("!")) name.substring(1) else name

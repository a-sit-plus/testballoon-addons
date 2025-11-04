package at.asitplus.testballoon

import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestExecutionScope
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.disable

context(suite: TestSuite)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @param nested The test body to execute.
 */
operator fun String.invoke(testConfig: TestConfig = TestConfig, nested: suspend TestExecutionScope.() -> Unit) {
    suite.test(this, testConfig = testConfig.disableByName(this)) { nested() }
}


/**
 * Represents a configured test suite with its parent suite, name, and configuration.
 *
 * @property parent The parent test suite.
 * @property name The name of the suite.
 * @property config The configuration for the suite.
 */
data class ConfiguredSuite(val parent: TestSuite, val name: String, val config: TestConfig)

context(suite: TestSuite)
/**
 * Creates a configured suite with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @return A new [ConfiguredSuite] instance.
 */
operator fun String.invoke(testConfig: TestConfig = TestConfig) = ConfiguredSuite(suite, this, testConfig)

context(suite: TestSuite)
/**
 * Creates a test suite with the specified name and body.
 *
 * @param suiteBody The body of the test suite.
 */
infix operator fun String.minus(suiteBody: TestSuite.() -> Unit) {
    suite.testSuite(name = testName(this), testConfig = TestConfig.disableByName(this), content = fun TestSuite.() {
        suiteBody()
    })
}

/**
 * Creates a test suite from a configured suite with the specified body.
 *
 * @param suiteBody The body of the test suite.
 */
infix operator fun ConfiguredSuite.minus(suiteBody: TestSuite.() -> Unit) {
    parent.testSuite(name, testConfig = config.disableByName(name), content = fun TestSuite.() {
        suiteBody()
    })
}


private fun TestConfig.disableByName(name: String) =
    if (name.startsWith("!")) TestConfig.disable() else this

private fun testName(name: String) = if (name.startsWith("!")) name.substring(1) else name

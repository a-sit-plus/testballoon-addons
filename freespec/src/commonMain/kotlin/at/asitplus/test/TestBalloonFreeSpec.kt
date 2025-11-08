package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite


context(suite: TestSuite)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @property displayName optional display name override
 * @param nested The test body to execute.
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
operator fun String.invoke(
    displayName: String = this,
    testConfig: TestConfig = TestConfig,
    nested: suspend TestExecutionScope.() -> Unit
) {
    suite.test(freeSpecName(this).truncated(), displayName = displayName.escaped, testConfig = testConfig.disableByName(this)) { nested() }
}


/**
 * Represents a configured test suite with its parent suite, name, and configuration.
 *
 * @property parent The parent test suite
 * @property testName The name of the suite
 * @property displayName optional display name override
 * @property config The configuration for the suite
 */
data class ConfiguredSuite(
    val parent: TestSuite,
    val displayName: String,
    val testName: String,
    val config: TestConfig
) {
    /**
     * Creates a test suite from a configured suite with the specified body.
     *
     * @param suiteBody The body of the test suite.
     */
    infix operator fun minus(suiteBody: TestSuite.() -> Unit) {
        parent.testSuite(
            freeSpecName(testName).truncated(),
            displayName = displayName.escaped,
            testConfig = config.disableByName(displayName),
            content = fun TestSuite.() {
                suiteBody()
            })
    }

}

context(suite: TestSuite)

/**
 * Creates a configured suite with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @param displayName Optional display name override
 * @return A new [ConfiguredSuite] instance.
 */
operator fun String.invoke(displayName: String = this, testConfig: TestConfig = TestConfig) =
    ConfiguredSuite(suite, displayName, this, testConfig)

context(suite: TestSuite)
/**
 * Creates a test suite with the specified name and body.
 *
 * @param suiteBody The body of the test suite.
 */
infix operator fun String.minus(suiteBody: TestSuite.() -> Unit) {
    suite.testSuite(name = freeSpecName(this).truncated(), displayName = freeSpecName(this).escaped, testConfig = TestConfig.disableByName(this), content = fun TestSuite.() {
        suiteBody()
    })
}


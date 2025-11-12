package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite

/**
 * Global knobs to tweak the behavior of PropertyTest Addon
 */
object FreeSpec {

    /**
     * The default maximum length of test element names (not display name). Default = 64. `-1` means no truncation
     */
    var defaultTestNameMaxLength: Int = DEFAULT_TEST_NAME_MAX_LEN

    /**
     * The default maximum length of test element names (not display name). Default = -1 (no truncation)
     */
    var defaultDisplayNameMaxLength: Int = -1

    @Deprecated("to be removed", replaceWith = ReplaceWith("defaultTestNameMaxLength"))
    var maxLength
        get() = defaultTestNameMaxLength
        set(value) {
            defaultTestNameMaxLength = value
        }

    @Deprecated("to be removed", replaceWith = ReplaceWith("defaultTestNameMaxLength"))
    var defaultMaxLength
        get() = defaultTestNameMaxLength
        set(value) {
            defaultTestNameMaxLength = value
        }

}

context(suite: TestSuite)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @property displayName optional display name override
 * @param nested The test body to execute.
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
operator fun String.invoke(
    displayName: String = this,
    maxLength: Int = FreeSpec.defaultTestNameMaxLength,
    displayNameMaxLength: Int = FreeSpec.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    nested: suspend TestExecutionScope.() -> Unit
) {
    suite.test(
        freeSpecName(this).truncated(maxLength).escaped,
        displayName = displayName.truncated(displayNameMaxLength).escaped,
        testConfig = testConfig.disableByName(this)
    ) { nested() }
}


/**
 * Represents a configured test suite with its parent suite, name, and configuration.
 *
 * @property parent The parent test suite
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @property testName The name of the suite
 * @property displayName optional display name override
 * @property config The configuration for the suite
 */
data class ConfiguredSuite(
    val parent: TestSuite,
    private val maxLength: Int = FreeSpec.defaultTestNameMaxLength,
    private val displayNameMaxLength: Int = FreeSpec.defaultDisplayNameMaxLength,
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
            freeSpecName(testName).truncated(maxLength).escaped,
            displayName = displayName.truncated(displayNameMaxLength).escaped,
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
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param displayName Optional display name override
 * @return A new [ConfiguredSuite] instance.
 */
operator fun String.invoke(
    displayName: String = this,
    maxLength: Int = FreeSpec.defaultTestNameMaxLength,
    displayNameMaxLength: Int = FreeSpec.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig
) =
    ConfiguredSuite(suite, maxLength, displayNameMaxLength, displayName, this, testConfig)

context(suite: TestSuite)
/**
 * Creates a test suite with the specified name and body.
 *
 * @param suiteBody The body of the test suite.
 */
infix operator fun String.minus(suiteBody: TestSuite.() -> Unit) {
    suite.testSuite(
        name = freeSpecName(this).truncated(FreeSpec.defaultTestNameMaxLength).escaped,
        displayName = freeSpecName(this).truncated(FreeSpec.defaultDisplayNameMaxLength).escaped,
        testConfig = TestConfig.disableByName(this),
        content = fun TestSuite.() {
            suiteBody()
        })
}


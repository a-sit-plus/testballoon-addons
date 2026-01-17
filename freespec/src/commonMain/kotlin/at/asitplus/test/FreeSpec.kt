package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope

/**
 * Global knobs to tweak the behavior of PropertyTest Addon
 */
object FreeSpec {

    /**
     * The default maximum length of test element names (not display name).
     * Defaults to [TestBalloonAddons.defaultTestNameMaxLength], but setting it here will take precedence.
     * * `-1` means no truncation.
     * * `null` means it will again fall back to [TestBalloonAddons.defaultTestNameMaxLength]
     *
     * This property's getter will never return null, but fall back to [TestBalloonAddons.defaultTestNameMaxLength].
     */
    var defaultTestNameMaxLength: Int? = null
        get() = field?:TestBalloonAddons.defaultTestNameMaxLength

    /**
     * The default maximum length of test element display names (not test name).
     * Defaults to [TestBalloonAddons.defaultDisplayNameMaxLength], but setting it here will take precedence.
     * * `-1` means no truncation.
     * * `null` means it will again fall back to [TestBalloonAddons.defaultDisplayNameMaxLength]
     *
     * This property's getter will never return null, but fall back to [TestBalloonAddons.defaultDisplayNameMaxLength].
     */
    var defaultDisplayNameMaxLength: Int? = null
        get() = field?:TestBalloonAddons.defaultDisplayNameMaxLength

}

context(suite: TestSuiteScope)
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
    maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    displayNameMaxLength: Int = FreeSpec.defaultDisplayNameMaxLength!!,
    testConfig: TestConfig = TestConfig,
    nested: suspend Test.ExecutionScope.() -> Unit
) {
    with(suite) {
        test(
            (freeSpecName(this@invoke).truncated(maxLength)),
            displayName = (displayName.truncated(displayNameMaxLength)),
            testConfig = testConfig.disableByName(this@invoke),
            nested
        )
    }
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
    val parent: TestSuiteScope,
    val maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    val displayNameMaxLength: Int = FreeSpec.defaultDisplayNameMaxLength!!,
    val displayName: String,
    val testName: String,
    val config: TestConfig
) {
    /**
     * Creates a test suite from a configured suite with the specified body.
     *
     * @param suiteBody The body of the test suite.
     */
    infix operator fun minus(suiteBody: TestSuiteScope.() -> Unit) {
        with(parent) {
            testSuite(
                (freeSpecName(testName).truncated(maxLength)),
                displayName = (displayName.truncated(displayNameMaxLength)),
                testConfig = config.disableByName(displayName),
                content = suiteBody
            )
        }
    }

}

context(suite: TestSuiteScope)

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
    maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    displayNameMaxLength: Int = FreeSpec.defaultDisplayNameMaxLength!!,
    testConfig: TestConfig = TestConfig
) =
    ConfiguredSuite(suite, maxLength, displayNameMaxLength, displayName, this, testConfig)

context(suite: TestSuiteScope)
/**
 * Creates a test suite with the specified name and body.
 *
 * @param suiteBody The body of the test suite.
 */
infix operator fun String.minus(suiteBody: TestSuiteScope.() -> Unit) =
    with(suite) {
        testSuite(
            name = (freeSpecName(this@minus).truncated(FreeSpec.defaultTestNameMaxLength!!)),
            displayName = (freeSpecName(this@minus).truncated(FreeSpec.defaultDisplayNameMaxLength!!)),
            testConfig = TestConfig.disableByName(this@minus),
            content = suiteBody
        )
    }



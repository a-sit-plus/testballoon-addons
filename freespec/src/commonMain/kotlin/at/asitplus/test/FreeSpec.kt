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
        get() = field ?: TestBalloonAddons.defaultTestNameMaxLength

}

context(suite: TestSuiteScope)
/**
 * Creates a test case with the specified name and configuration.
 *
 * @param testConfig Optional test configuration
 * @param maxLength maximum length of test element name (not display name)
 * @param nested The test body to execute.
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
operator fun String.invoke(
    maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    testConfig: TestConfig = TestConfig,
    nested: suspend Test.ExecutionScope.() -> Unit
) {
    with(suite) {
        val truncatedName = freeSpecName(this@invoke).truncated(maxLength)
        testSuiteInScope.checkPathLenIncluding(truncatedName)
        test(
            name = truncatedName,
            testConfig = testConfig.disableByName(this@invoke),
            action = nested
        )
    }
}


/**
 * Represents a configured test suite with its parent suite, name, and configuration.
 *
 * @property parent The parent test suite
 * @param maxLength maximum length of test element name (not display name)
 * @property testName The name of the suite
 * @property config The configuration for the suite
 */
data class ConfiguredSuite(
    val parent: TestSuiteScope,
    val maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
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
            val truncatedName = freeSpecName(testName).truncated(maxLength)
            testSuiteInScope.checkPathLenIncluding(truncatedName)
            testSuite(
                truncatedName,
                testConfig = config.disableByName(testName),
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
 * @return A new [ConfiguredSuite] instance.
 */
operator fun String.invoke(
    maxLength: Int = FreeSpec.defaultTestNameMaxLength!!,
    testConfig: TestConfig = TestConfig
) =
    ConfiguredSuite(suite, maxLength, this, testConfig)

context(suite: TestSuiteScope)
/**
 * Creates a test suite with the specified name and body.
 *
 * @param suiteBody The body of the test suite.
 */
infix operator fun String.minus(suiteBody: TestSuiteScope.() -> Unit) =
    with(suite) {
        val truncatedName = freeSpecName(this@minus).truncated(FreeSpec.defaultTestNameMaxLength!!)
        testSuiteInScope.checkPathLenIncluding(truncatedName)
        testSuite(
            name = truncatedName,
            testConfig = TestConfig.disableByName(this@minus),
            content = suiteBody
        )
    }



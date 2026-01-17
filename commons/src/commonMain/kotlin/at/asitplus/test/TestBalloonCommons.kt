package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable

/**
 * The default maximum length of test names and test suite names created using:
 * * `withData`
 * * `withDataSuites`
 * * `checkAll`
 * * `checkAllSuites`
 * * FreeSoec
 *
 * On Android this is `10`. On all other platforms this is 64
 */
internal expect val defaultMaxLen: Int


/**
 * Global configuration properties for all TestBalloon Addons used to override properties of **all addons in one place**.
 * Setting properties of individual addons takes precedence over the global properties set here
 */
object TestBalloonAddons {

    /**
     * The default maximum length of test names and test suite names created using:
     * * `withData`
     * * `withDataSuites`
     * * `checkAll`
     * * `checkAllSuites`
     * * FreeSoec
     *
     * On Android this is `10`. On all other platforms this is 64.
     *
     * `-1` means no truncation.
     */
    var defaultTestNameMaxLength: Int = defaultMaxLen

    /**
     * The default maximum length of display names of tests and suites created using:
     * * `withData`
     * * `withDataSuites`
     * * `checkAll`
     * * `checkAllSuites`
     * * FreeSoec
     *
     * Defaults to `-1` (= no truncation).
     */
    var defaultDisplayNameMaxLength: Int = -1

}

@Deprecated(
    "To be removed in 0.8.", replaceWith = ReplaceWith("TestBalloonAddons.defaultTestNameMaxLength"),
    DeprecationLevel.WARNING
)
val DEFAULT_TEST_NAME_MAX_LEN: Int get() = TestBalloonAddons.defaultTestNameMaxLength
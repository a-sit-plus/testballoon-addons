package at.asitplus.testballoon

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
     * The default maximum length of test element names created using:
     * * `withData`
     * * `withDataSuites`
     * * `checkAll`
     * * `checkAllSuites`
     * * FreeSoec
     *
     * On Android this is `15`. On all other platforms this is 64.
     *
     * `-1` means no truncation.
     */
    var defaultTestNameMaxLength: Int = defaultMaxLen

    /**
     * The default maximum length of display names of test elements created using:
     * * `withData`
     * * `withDataSuites`
     * * `checkAll`
     * * `checkAllSuites`
     * * FreeSoec
     *
     * Defaults to `-1` (= no truncation).
     */
    var defaultDisplayNameMaxLength: Int = -1

    /**
     * Hard limit of the maximum path length of a test (i.e. all layers after the root suite's FQN down to the test case).
     * **This defaults to `122` on Android** due to hardcoded limits somewhere deep into the Android device test pipeline.
     * On all other platforms, no hard limit is enforced by default.
     * The intent of this hard limit is to have Android device tests fail in a controlled manner, as test names
     * exceeding an (utterly undocumented) limit will cause unintelligible failures inside UTP.
     */
    var overallMaxTestPathLength: Int
        get() = totalMaxLen
        set(value) {
            totalMaxLen = value
        }

}

@Deprecated(
    "To be removed in 0.8.", replaceWith = ReplaceWith("TestBalloonAddons.defaultTestNameMaxLength"),
    DeprecationLevel.WARNING
)
val DEFAULT_TEST_NAME_MAX_LEN: Int get() = TestBalloonAddons.defaultTestNameMaxLength
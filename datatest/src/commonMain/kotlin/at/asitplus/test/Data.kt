package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope

/**
 * Global knobs to tweak the behavior of DataTest Addon
 */
object DataTest {
    /**
     * If `true`, all `withData` and `checkAll` iterations will be compacted into one test (suite) instead of one each per iteration by default.
     * If `false` each iteration of `withData` and `checkAll` will create a new test (suite).
     */
    var compactByDefault = false

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

    /**
     * The default maximum length of test element display names (not test name).
     * Defaults to [TestBalloonAddons.defaultDisplayNameMaxLength], but setting it here will take precedence.
     * * `-1` means no truncation.
     * * `null` means it will again fall back to [TestBalloonAddons.defaultDisplayNameMaxLength]
     * 
     * This property's getter will never return null, but fall back to [TestBalloonAddons.defaultDisplayNameMaxLength].
     */
    var defaultDisplayNameMaxLength: Int? = null
        get() = field ?: TestBalloonAddons.defaultDisplayNameMaxLength

}


data class ConfiguredDataTestScope<Data>(
    private val compact: Boolean,
    private val maxLength: Int,
    private val displayNameMaxLength: Int,
    val prefix: String,
    val testSuite: TestSuiteScope, val map: Sequence<Pair<String, Data>>,
    val testConfig: TestConfig = TestConfig,
) {
    operator fun minus(action: TestSuiteScope.(Data) -> Unit) =
        testSuite.withDataSuitesInternal(map, compact, maxLength, displayNameMaxLength, prefix, testConfig, action)
}


/**
 * Executes a test for each entry in the provided map.
 * Uses map keys as test names.
 *
 * @param map Map of test names to test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param prefix an optional prefix to add to the test name
 * @param action Test action to execute for each map value
 */
internal fun <Data> TestSuiteScope.withDataInternal(
    map: Sequence<Pair<String, Data>>,
    testConfig: TestConfig = TestConfig,
    compact: Boolean,
    maxLength: Int,
    displayNameMaxLength: Int,
    prefix: String,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) {
    val prefix = if (prefix.isNotEmpty()) "$prefix " else ""
    if (compact) {
        val (compactName, map) = map.peekTypeNameAndReplay { it.second }
        val testName = "${prefix}Σ$compactName"
        val truncatedName = testName.truncated(maxLength)
        testSuiteInScope.checkPathLenIncluding(truncatedName)
        test(
            name = truncatedName,
            displayName = (testName.truncated(displayNameMaxLength)),
            testConfig = testConfig
        ) {
            val errors = mutableMapOf<String, Throwable?>()
            map.forEachIndexed { i, d ->
                val name = "${i + 1}: ${d.first}"
                catchingUnwrapped {
                    action(d.second)
                    errors["OK:    $name"] = null
                }.onFailure {
                    errors["Error: $name"] = it
                }
            }
            collateErrors(errors, testName)
        }
    } else {
        for (d in map) {
            val name = prefix + d.first
            val truncatedName = name.truncated(maxLength)
            testSuiteInScope.checkPathLenIncluding(truncatedName)
            test(
                name = truncatedName,
                displayName = (name.truncated(displayNameMaxLength)),
                testConfig = testConfig
            ) { action(d.second) }
        }
    }
}

/**
 * Creates a test suite for each item in the provided sequence.
 * Uses provided function to generate suite names.
 *
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param prefix an optional prefix to add to the test name
 * @param action Test suite configuration action for each data item
 */
internal fun <Data> TestSuiteScope.withDataSuitesInternal(
    data: Sequence<Pair<String, Data>>,
    compact: Boolean,
    maxLength: Int,
    displayNameMaxLength: Int,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    action: TestSuiteScope.(Data) -> Unit
) {
    val prefix = if (prefix.isNotEmpty()) "$prefix " else ""
    if (compact) {
        val (compactName, data) = data.peekTypeNameAndReplay { it.second }
        val testName = "${prefix}Σ$compactName"
        val truncatedName = testName.truncated(maxLength)
        testSuiteInScope.checkPathLenIncluding(truncatedName)
        testSuite(
            name = truncatedName,
            displayName = (testName.truncated(displayNameMaxLength)),
            testConfig = testConfig
        ) {
            val errors = mutableMapOf<String, Throwable?>()
            data.forEachIndexed { i, d ->
                val name = "${i + 1}: ${d.first}"
                catchingUnwrapped {
                    action(d.second)
                    errors["OK:    $name"] = null
                }.onFailure {
                    errors["Error: $name"] = it
                }
            }
            collateErrors(errors, testName)
        }
    } else {
        for (d in data) {
            val name = prefix + d.first
            val truncatedName = name.truncated(maxLength)
            testSuiteInScope.checkPathLenIncluding(truncatedName)
            testSuite(
                name = truncatedName,
                displayName = (name.truncated(displayNameMaxLength)),
                testConfig = testConfig,
                content = fun TestSuiteScope.() {
                    action(d.second)
                })
        }
    }
}
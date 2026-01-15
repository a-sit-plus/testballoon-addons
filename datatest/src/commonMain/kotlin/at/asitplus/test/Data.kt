package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
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
     * The default maximum length of test element names (not display name). Default = 64. `-1` means no truncation
     */
    var defaultTestNameMaxLength: Int = DEFAULT_TEST_NAME_MAX_LEN

    /**
     * The default maximum length of test element names (not display name). Default = -1 (no truncation)
     */
    var defaultDisplayNameMaxLength: Int = -1

}


data class ConfiguredDataTestScope<Data>(
    private val compact: Boolean,
    private val maxLength: Int = DataTest.defaultTestNameMaxLength,
    private val displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    val testSuite: TestSuiteScope, val map: Sequence<Pair<String, Data>>,
    val testConfig: TestConfig = TestConfig,
) {
    operator fun minus(action: TestSuiteScope.(Data) -> Unit) =
        testSuite.withDataSuitesInternal(map, compact, maxLength, displayNameMaxLength, testConfig, action)
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
 * @param action Test action to execute for each map value
 */
internal fun <Data> TestSuiteScope.withDataInternal(
    map: Sequence<Pair<String, Data>>,
    testConfig: TestConfig = TestConfig,
    compact: Boolean,
    maxLength: Int,
    displayNameMaxLength: Int,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) {
    if (compact) {
        val (compactName, map) = map.peekTypeNameAndReplay { it.second }
        val testName = "[compacted] $compactName"
        test(
            name = testName.truncated(maxLength).escaped,
            displayName = testName.truncated(displayNameMaxLength).escaped,
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
            test(
                name = d.first.truncated(maxLength).escaped,
                displayName = d.first.truncated(displayNameMaxLength).escaped,
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
 * @param action Test suite configuration action for each data item
 */
internal fun <Data> TestSuiteScope.withDataSuitesInternal(
    data: Sequence<Pair<String, Data>>,
    compact: Boolean,
    maxLength: Int,
    displayNameMaxLength: Int,
    testConfig: TestConfig = TestConfig,
    action: TestSuiteScope.(Data) -> Unit
) {

    if (compact) {
        val (compactName, data) = data.peekTypeNameAndReplay { it.second }
        val testName = "[compacted] $compactName"
        testSuite(
            name = testName.truncated(maxLength).escaped,
            displayName = testName.truncated(displayNameMaxLength).escaped,
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
            val name = d.first.escaped
            testSuite(
                name = name.truncated(maxLength).escaped,
                displayName = name.truncated(displayNameMaxLength).escaped,
                testConfig = testConfig,
                content = fun TestSuiteScope.() {
                    action(d.second)
                })
        }
    }
}
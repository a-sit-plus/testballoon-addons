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
     * Whether compacted failure reports should attach every failed input as a suppressed throwable.
     *
     * This affects compacted `withData`, `withDataSuites`, `checkAll`, and `checkAllSuites` reports. The rendered
     * failure message still includes the stack trace of the first failure either way.
     *
     * `null` means it will again fall back to [TestBalloonAddons.addSuppressedErrorsToCompactedFailures]
     *
     *  This property's getter will never return null, but fall back to [TestBalloonAddons.addSuppressedErrorsToCompactedFailures].
     */
    var addSuppressedErrorsToCompactedFailures: Boolean? = null
        get() = field ?: TestBalloonAddons.addSuppressedErrorsToCompactedFailures

}


class ConfiguredDataTestScope<Data>(
    private val compact: Boolean,
    private val maxLength: Int,
    val prefix: String,
    val testSuite: TestSuiteScope, val map: Sequence<Pair<String, Data>>,
    val testConfig: TestConfig = TestConfig,
) {
    operator fun minus(action: TestSuiteScope.(Data) -> Unit) =
        testSuite.withDataSuitesInternal(map, compact, maxLength, prefix, testConfig, action)
}

internal fun generatedDataName(
    data: Any?,
    compact: Boolean,
    maxLength: Int,
    prefix: String
): String = if (compact) {
    data.toPrettyString(maxLength, generatedDataNamePrefixLength(prefix))
} else {
    data.toPrettyString(maxLength, generatedDataNamePrefixLength(prefix))
}

private fun generatedDataNamePrefixLength(prefix: String): Int =
    if (prefix.isEmpty()) 0 else prefix.length + 1

private fun dataCaseName(index: Int, data: Pair<String, *>): String =
    "${index + 1}: ${data.first}"

private inline fun <Data> runCompactedDataResults(
    data: Sequence<Pair<String, Data>>,
    testName: String,
    action: (Data) -> Result<Unit>
) {
    val run = CollatedTestRun(testName, DataTest.addSuppressedErrorsToCompactedFailures!!)
    data.forEachIndexed { i, d ->
        run.record(dataCaseName(i, d), action(d.second))
    }
    run.throwIfAny()
}

internal suspend fun <Data> runCompactedDataSuspend(
    data: Sequence<Pair<String, Data>>,
    testName: String,
    action: suspend (Data) -> Unit
) = runCompactedDataResults(data, testName) {
    catchingUnwrapped {
        action(it)
    }
}

internal fun <Data> runCompactedData(
    data: Sequence<Pair<String, Data>>,
    testName: String,
    action: (Data) -> Unit
) = runCompactedDataResults(data, testName) {
    catchingUnwrapped {
        action(it)
    }
}


/**
 * Executes a test for each entry in the provided map.
 * Uses map keys as test names.
 *
 * @param map Map of test names to test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param prefix an optional prefix to add to the test name
 * @param action Test action to execute for each map value
 */
internal fun <Data> TestSuiteScope.withDataInternal(
    map: Sequence<Pair<String, Data>>,
    testConfig: TestConfig = TestConfig,
    compact: Boolean,
    maxLength: Int,
    prefix: String,
    action: suspend Test.ExecutionScope.(Data) -> Unit
) {
    val prefix = prefix.normalizedTestPrefix()
    if (compact) {
        val (testName, map) = map.compactTestNameAndReplay(prefix) { it.second }
        val truncatedName = checkedTruncatedName(testName, maxLength)
        test(
            name = truncatedName,
            testConfig = testConfig
        ) {
            runCompactedDataSuspend(map, testName) { action(it) }
        }
    } else {
        for (d in map) {
            val truncatedName = checkedTruncatedName(prefixedTestName(prefix, d.first), maxLength)
            test(
                name = truncatedName,
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
 * @param prefix an optional prefix to add to the test name
 * @param action Test suite configuration action for each data item
 */
internal fun <Data> TestSuiteScope.withDataSuitesInternal(
    data: Sequence<Pair<String, Data>>,
    compact: Boolean,
    maxLength: Int,
    prefix: String,
    testConfig: TestConfig = TestConfig,
    action: TestSuiteScope.(Data) -> Unit
) {
    val prefix = prefix.normalizedTestPrefix()
    if (compact) {
        val (testName, data) = data.compactTestNameAndReplay(prefix) { it.second }
        val truncatedName = checkedTruncatedName(testName, maxLength)
        testSuite(
            name = truncatedName,
            testConfig = testConfig
        ) {
            runCompactedData(data, testName) { action(it) }
        }
    } else {
        for (d in data) {
            val truncatedName = checkedTruncatedName(prefixedTestName(prefix, d.first), maxLength)
            testSuite(
                name = truncatedName,
                testConfig = testConfig,
                content = fun TestSuiteScope.() {
                    action(d.second)
                })
        }
    }
}

package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite

/**
 * Executes a test for each provided data parameter.
 *
 * @param parameters The data parameters to test with
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    vararg parameters: Data,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) = withDataInternal(parameters.asSequence().map { it.toString() to it }, testConfig, action)


/**
 * Executes a test for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each data item
 */
fun <Data> TestSuite.withData(
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) = withDataInternal(data.asSequence().map { it.toString() to it }, testConfig, action)


/**
 * Executes a test for each entry in the provided map.
 * Uses map keys as test names.
 *
 * @param map Map of test names to test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each map value
 */
fun <Data> TestSuite.withData(
    map: Map<String, Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) = withDataInternal(map.asSequence().map { (k, v) -> k to v }, testConfig, action)


/**
 * Executes a test for each item in the provided iterable data.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each data item
 */
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String, data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) = withDataInternal(data.asSequence().map { nameFn(it) to it }, testConfig, action)


/**
 * Executes a test for each provided data parameter.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param arguments The data parameters to test with
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String, vararg arguments: Data,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) = withDataInternal(arguments.asSequence().map { nameFn(it) to it }, testConfig, action)

/**
 * Executes a test for each item in the provided sequence.
 *
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each sequence item
 */
fun <Data> TestSuite.withData(
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) = withDataInternal(data.map { it.toString() to it }, testConfig, action)

/**
 * Executes a test for each item in the provided sequence.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each sequence item
 */
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String, data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) = withDataInternal(data.map { nameFn(it) to it }, testConfig, action)

data class ConfiguredDataTestScope<Data>(
    val testSuite: TestSuite, val map: Sequence<Pair<String, Data>>,
    val testConfig: TestConfig = TestConfig,
) {
    operator fun minus(action: TestSuite.(Data) -> Unit) = testSuite.withDataSuitesInternal(map, testConfig, action)
}

/**
 * Creates a configured test suite scope to generate test suites for each provided data parameter.
 *
 * @param parameters The data parameters to create suites for
 * @param testConfig Optional test configuration
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    vararg parameters: Data,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(this, parameters.asSequence().map { it.toString() to it }, testConfig)

/**
 * Creates a configured test suite scope to generate test suites for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuite.withData(
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(this, data.asSequence().map { it.toString() to it }, testConfig)

/**
 * Creates a configured test suite scope to generate test suites for each entry in the provided map.
 * Uses map keys as suite names.
 *
 * @param map Map of suite names to test data
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuite.withData(
    map: Map<String, Data>,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(this, map.asSequence().map { (k, v) -> k to v }, testConfig)

/**
 * Creates a configured test suite scope to generate test suites for each item in the provided sequence.
 *
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuite.withData(
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(this, data.map { it.toString() to it }, testConfig)


/**
 * Creates a configured test suite scope to generate test suites for each provided data parameter.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param arguments The data parameters to create suites for
 * @param testConfig Optional test configuration
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    vararg arguments: Data,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(this, arguments.asSequence().map { nameFn(it) to it }, testConfig)


/**
 * Creates a configured test suite scope to generate test suites for each item in the provided iterable data.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(this, data.asSequence().map { nameFn(it) to it }, testConfig)


/**
 * Creates a configured test suite scope to generate test suites  for each item in the provided sequence.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 */
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String,
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(this, data.map { nameFn(it) to it }, testConfig)

/**
 * Creates a test suite for each provided data parameter.
 *
 * @param parameters The data parameters to create suites for
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withDataSuites(
    vararg parameters: Data,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(parameters.map { it.toString() to it }.asSequence(), testConfig, action)

/**
 * Creates a test suite for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each data item
 */
fun <Data> TestSuite.withDataSuites(
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(data.map { it.toString() to it }.asSequence(), testConfig, action)

/**
 * Creates a test suite for each entry in the provided map.
 * Uses map keys as suite names.
 *
 * @param map Map of suite names to test data
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each map value
 */
fun <Data> TestSuite.withDataSuites(
    map: Map<String, Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(map.map { (k, v) -> k to v }.asSequence(), testConfig, action)

/**
 * Creates a test suite for each item in the provided sequence.
 *
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each data item
 */
fun <Data> TestSuite.withDataSuites(
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(data.map { it.toString() to it }, testConfig, action)

/**
 * Creates a test suite for each provided data parameter.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param arguments The data parameters to create suites for
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    vararg arguments: Data,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(arguments.map { nameFn(it) to it }.asSequence(), testConfig, action)

/**
 * Creates a test suite for each item in the provided iterable data.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each data item
 */
fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(data.map { nameFn(it) to it }.asSequence(), testConfig, action)

/**
 * Creates a test suite for each item in the provided sequence.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each data item
 */
fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(data.map { nameFn(it) to it }, testConfig, action)

/**
 * Creates a test suite for each item in the provided sequence.
 * Uses provided function to generate suite names.
 *
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param action Test suite configuration action for each data item
 */
@PublishedApi
internal fun <Data> TestSuite.withDataSuitesInternal(
    data: Sequence<Pair<String, Data>>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {
    for (d in data) {
        val name = d.first.escaped
        testSuite(
            name = name.truncated(),
            displayName = name.escaped,
            testConfig = testConfig,
            content = fun TestSuite.() {
                action(d.second)
            })
    }
}


/**
 * Executes a test for each entry in the provided map.
 * Uses map keys as test names.
 *
 * @param map Map of test names to test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each map value
 */
internal fun <Data> TestSuite.withDataInternal(
    map: Sequence<Pair<String, Data>>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in map) {
        test(name = d.first.truncated(), displayName = d.first.escaped, testConfig = testConfig) { action(d.second) }
    }
}

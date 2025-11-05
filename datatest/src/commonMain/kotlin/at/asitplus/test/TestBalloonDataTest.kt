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
) {
    for (data in parameters) {
        val name = "$data"
        test(name, testConfig = testConfig) {
            action(data)
        }
    }
}

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
) {
    for (d in data) {
        val name = "$d"
        test(name, testConfig = testConfig) { action(d) }
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
fun <Data> TestSuite.withData(
    map: Map<String, Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in map) {
        test(d.key, testConfig = testConfig) { action(d.value) }
    }
}

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
) {
    for (d in data) {
        val name = nameFn(d)
        test(name, testConfig = testConfig) { action(d) }
    }
}

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
) {
    for (d in arguments) {
        val name = nameFn(d)
        test(name, testConfig = testConfig) { action(d) }
    }
}

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
) {
    for (d in data) {
        val name = "$d"
        test(name, testConfig = testConfig) { action(d) }
    }
}

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
) {
    for (d in data) {
        val name = nameFn(d)
        test(name, testConfig = testConfig) { action(d) }
    }
}


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
) {
    for (d in parameters) {
        testSuite(name = d.toString(), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

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
) {
    for (d in data) {
        testSuite(name = d.toString(), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

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
) {
    for (d in map) {
        testSuite(d.key, testConfig = testConfig, content = fun TestSuite.() {
            action(d.value)
        })
    }
}

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
) {
    for (d in data) {
        testSuite(name = d.toString(), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

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
) = withDataSuites(nameFn, arguments.asIterable(), testConfig, action)

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
) {
    for (d in data) {
        testSuite(nameFn(d), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

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
) {
    for (d in data) {
        testSuite(nameFn(d), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}
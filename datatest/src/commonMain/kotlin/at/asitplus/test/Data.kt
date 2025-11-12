package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuite
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


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

    @Deprecated("to be removed", replaceWith = ReplaceWith("defaultTestNameMaxLength"))
    var maxLength get() = defaultTestNameMaxLength
        set(value) {defaultTestNameMaxLength = value}
}


/**
 * Executes a test for each provided data parameter.
 *
 * @param parameters The data parameters to test with
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
inline fun <reified Data> TestSuite.withData(
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline action: suspend (Data) -> Unit
) = withDataInternal(
    parameters.asSequence().map { it.toString() to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)


/**
 * Executes a test for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each data item
 */
inline fun <reified Data> TestSuite.withData(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline action: suspend (Data) -> Unit
) = withDataInternal(
    data.asSequence().map { it.toString() to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)


/**
 * Executes a test for each entry in the provided map.
 * Uses map keys as test names.
 *
 * @param map Map of test names to test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each map value
 */
inline fun <reified Data> TestSuite.withData(
    map: Map<String, Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline action: suspend (Data) -> Unit
) = withDataInternal(
    map.asSequence().map { (k, v) -> k to v },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)


/**
 * Executes a test for each item in the provided iterable data.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param data The iterable collection of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each data item
 */
inline fun <reified Data> TestSuite.withData(
    crossinline nameFn: (Data) -> String, data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline action: suspend (Data) -> Unit
) = withDataInternal(
    data.asSequence().map { nameFn(it) to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)


/**
 * Executes a test for each provided data parameter.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param arguments The data parameters to test with
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
inline fun <reified Data> TestSuite.withData(
    crossinline nameFn: (Data) -> String, vararg arguments: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline action: suspend (Data) -> Unit
) = withDataInternal(
    arguments.asSequence().map { nameFn(it) to it },
    testConfig,
    compact,
    maxLength,
    displayNameMaxLength,
    action
)

/**
 * Executes a test for each item in the provided sequence.
 *
 * @param data The sequence of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each sequence item
 */
inline fun <reified Data> TestSuite.withData(
    data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline action: suspend (Data) -> Unit
) = withDataInternal(data.map { it.toString() to it }, testConfig, compact, maxLength, displayNameMaxLength, action)

/**
 * Executes a test for each item in the provided sequence.
 * Uses provided function to generate test names.
 *
 * @param nameFn Function to generate test name from data
 * @param data The sequence of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param action Test action to execute for each sequence item
 */
inline fun <reified Data> TestSuite.withData(
    crossinline nameFn: (Data) -> String, data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline action: suspend (Data) -> Unit
) = withDataInternal(data.map { nameFn(it) to it }, testConfig, compact, maxLength, displayNameMaxLength, action)

data class ConfiguredDataTestScope<Data>(
    private val compactName: String?,
    private val maxLength: Int = DataTest.defaultTestNameMaxLength,
    private val displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    val testSuite: TestSuite, val map: Sequence<Pair<String, Data>>,
    val testConfig: TestConfig = TestConfig,
) {
    operator fun minus(action: TestSuite.(Data) -> Unit) =
        testSuite.withDataSuitesInternal(map, compactName, maxLength, displayNameMaxLength, testConfig, action)
}

/**
 * Creates a configured test suite scope to generate test suites for each provided data parameter.
 *
 * @param parameters The data parameters to create suites for
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
inline fun <reified Data> TestSuite.withData(
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    parameters.asSequence().map { it.toString() to it },
    testConfig
)

/**
 * Creates a configured test suite scope to generate test suites for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
inline fun <reified Data> TestSuite.withData(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    data.asSequence().map { it.toString() to it },
    testConfig
)

/**
 * Creates a configured test suite scope to generate test suites for each entry in the provided map.
 * Uses map keys as suite names.
 *
 * @param map Map of suite names to test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
inline fun <reified Data> TestSuite.withData(
    map: Map<String, Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    map.asSequence().map { (k, v) -> k to v },
    testConfig
)

/**
 * Creates a configured test suite scope to generate test suites for each item in the provided sequence.
 *
 * @param data The sequence of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
inline fun <reified Data> TestSuite.withData(
    data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    data.map { it.toString() to it },
    testConfig
)


/**
 * Creates a configured test suite scope to generate test suites for each provided data parameter.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param arguments The data parameters to create suites for
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
inline fun <reified Data> TestSuite.withDataSuites(
    crossinline nameFn: (Data) -> String,
    vararg arguments: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig
) = ConfiguredDataTestScope<Data>(
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    arguments.asSequence().map { nameFn(it) to it },
    testConfig
)


/**
 * Creates a configured test suite scope to generate test suites for each item in the provided iterable data.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The iterable collection of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
inline fun <reified Data> TestSuite.withData(
    crossinline nameFn: (Data) -> String,
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    data.asSequence().map { nameFn(it) to it },
    testConfig
)


/**
 * Creates a configured test suite scope to generate test suites for each item in the provided sequence.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The sequence of test data
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
inline fun <reified Data> TestSuite.withData(
    crossinline nameFn: (Data) -> String,
    data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredDataTestScope<Data>(
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    this,
    data.map { nameFn(it) to it },
    testConfig
)

/**
 * Creates a test suite for each provided data parameter.
 *
 * @param parameters The data parameters to create suites for
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
inline fun <reified Data> TestSuite.withDataSuites(
    vararg parameters: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    parameters.map { it.toString() to it }.asSequence(),
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

/**
 * Creates a test suite for each item in the provided iterable data.
 *
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each data item
 */
inline fun <reified Data> TestSuite.withDataSuites(
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    data.map { it.toString() to it }.asSequence(),
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

/**
 * Creates a test suite for each entry in the provided map.
 * Uses map keys as suite names.
 *
 * @param map Map of suite names to test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each map value
 */
inline fun <reified Data> TestSuite.withDataSuites(
    map: Map<String, Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    map.map { (k, v) -> k to v }.asSequence(),
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

/**
 * Creates a test suite for each item in the provided sequence.
 *
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each data item
 */
inline fun <reified Data> TestSuite.withDataSuites(
    data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    data.map { it.toString() to it },
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

/**
 * Creates a test suite for each provided data parameter.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param arguments The data parameters to create suites for
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each parameter
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
inline fun <reified Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    vararg arguments: Data,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    arguments.map { nameFn(it) to it }.asSequence(),
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

/**
 * Creates a test suite for each item in the provided iterable data.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The iterable collection of test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each data item
 */
inline fun <reified Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    data.map { nameFn(it) to it }.asSequence(),
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

/**
 * Creates a test suite for each item in the provided sequence.
 * Uses provided function to generate suite names.
 *
 * @param nameFn Function to generate suite name from data
 * @param data The sequence of test data
 * @param testConfig Optional test configuration
 * @param compact If true, only a single test element is created and the class name of the data parameter is used as test name
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test suite configuration action for each data item
 */
inline fun <reified Data> TestSuite.withDataSuites(
    crossinline nameFn: (Data) -> String,
    data: Sequence<Data>,
    compact: Boolean = DataTest.compactByDefault,
    maxLength: Int = DataTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = DataTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline action: TestSuite.(Data) -> Unit
) = withDataSuitesInternal(
    data.map { nameFn(it) to it },
    if (compact) Data::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength = displayNameMaxLength,
    testConfig,
    action
)

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
@PublishedApi
internal fun <Data> TestSuite.withDataSuitesInternal(
    data: Sequence<Pair<String, Data>>,
    compactedName: String?,
    maxLength: Int,
    displayNameMaxLength: Int,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {

    if (compactedName != null) {
        val testName = "[compacted] $compactedName"
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
            if (errors.values.filterNotNull().isNotEmpty()) {
                val messages = errors.map { (msg, err) -> msg + (err?.let { ": ${it.message}" }) }.joinToString("\n")
                throw (if (errors.count { it is AssertionError } == errors.size) AssertionError(
                    testName + "\n$messages"
                )
                else RuntimeException(
                    testName + "\n$messages"
                )).also { errors.values.filterNotNull().forEach(it::addSuppressed) }
            }
        }
    } else {

        for (d in data) {
            val name = d.first.escaped
            testSuite(
                name = name.truncated(maxLength).escaped,
                displayName = name.truncated(displayNameMaxLength).escaped,
                testConfig = testConfig,
                content = fun TestSuite.() {
                    action(d.second)
                })
        }
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
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param action Test action to execute for each map value
 */
@PublishedApi
internal inline fun <reified Data> TestSuite.withDataInternal(
    map: Sequence<Pair<String, Data>>,
    testConfig: TestConfig = TestConfig,
    compact: Boolean,
    maxLength: Int,
    displayNameMaxLength: Int,
    crossinline action: suspend (Data) -> Unit
) {

    if (compact) {
        val testName = "[compacted] " + Data::class.simpleName ?: "<Anonymous>"
        test(
            name = testName.truncated(maxLength).escaped,
            displayName = testName.truncated(displayNameMaxLength).escaped,
            testConfig = testConfig
        ) {
            val mutex = Mutex()
            val errors = mutableMapOf<String, Throwable?>()
            map.forEachIndexed { i, d ->
                val name = "${i + 1}: ${d.first}"
                catchingUnwrapped {
                    action(d.second)
                    mutex.withLock { errors["OK:    $name"] = null }
                }.onFailure {
                    mutex.withLock { errors["Error: $name"] = it }

                }
            }
            if (errors.values.filterNotNull().isNotEmpty()) {
                val messages = errors.map { (msg, err) -> msg + (err?.let { ": ${it.message}" }) }.joinToString("\n")
                throw (if (errors.count { it is AssertionError } == errors.size) AssertionError(
                    testName + "\n$messages"
                )
                else RuntimeException(
                    testName + "\n$messages"
                )).also { errors.values.filterNotNull().forEach(it::addSuppressed) }
            }
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

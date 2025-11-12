package at.asitplus.testballoon

import at.asitplus.catchingUnwrapped
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite
import io.kotest.assertions.AssertionErrorBuilder
import io.kotest.property.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Global knobs to tweak the behavior of PropertyTest Addon
 */
object PropertyTest {
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
    var maxLength
        get() = defaultTestNameMaxLength
        set(value) {
            defaultTestNameMaxLength = value
        }

    @Deprecated("to be removed", replaceWith = ReplaceWith("defaultTestNameMaxLength"))
    var defaultMaxLength
        get() = defaultTestNameMaxLength
        set(value) {
            defaultTestNameMaxLength = value
        }
}


/**
 * Executes property-based tests with generated values.
 *
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param content Test execution block receiving generated values
 */
inline fun <reified Value> TestSuite.checkAll(
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline content: suspend context(PropertyContext) TestExecutionScope.(Value) -> Unit
) = checkAll(
    PropertyTesting.defaultIterationCount,
    genA,
    compact,
    maxLength,
    displayNameMaxLength,
    testConfig,
    content
)

/**
 * Executes property-based tests with generated values.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param content Test execution block receiving generated values
 */
inline fun <reified Value> TestSuite.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline content: suspend context(PropertyContext) TestExecutionScope.(Value) -> Unit
) = checkAllInternal(
    iterations,
    genA,
    if (compact) Value::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength,
    testConfig,
) { content(it) }

/**
 * Executes property-based tests with generated values.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param testConfig Optional test configuration
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param content Test execution block receiving generated values
 */
@PublishedApi
internal fun <Value> TestSuite.checkAllInternal(
    iterations: Int,
    genA: Gen<Value>,
    compactName: String?,
    maxLength: Int,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    content: suspend context(PropertyContext) TestExecutionScope.(Value) -> Unit
) {

    if (compactName != null) {
        val testName = "$iterations ${compactName}s"
        this@checkAllInternal.test(
            name = testName.truncated(maxLength).escaped,
            displayName = testName.truncated(displayNameMaxLength).escaped,
            testConfig = testConfig
        ) {
            val mutex = Mutex()
            val errors = mutableMapOf<String, Throwable?>()
            checkAllSeries(iterations, genA) { iter, value, context ->
                with(context) {
                    val valueStr = if (value is Iterable<*>) value.joinToString() else value.toString()
                    val name =
                        "${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
                    catchingUnwrapped {
                        content(value)
                        mutex.withLock { errors["OK:    $name"] = null }
                    }.onFailure {
                        mutex.withLock { errors["Error: $name"] = it }
                    }
                }
            }
            if (errors.values.filterNotNull().isNotEmpty()) {
                val messages = errors.map { (msg, err) -> msg + (err?.let { ": ${it.message}" }) }.joinToString("\n")
                throw (if (errors.count { it is AssertionError } == errors.size) AssertionErrorBuilder(
                    testName + "\n$messages",
                    null,
                    null,
                    null
                ).build() else RuntimeException(
                    testName + "\n$messages"
                )).also { errors.values.filterNotNull().forEach(it::addSuppressed) }
            }
        }
    } else {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val valueStr = if (value is Iterable<*>) value.joinToString() else value.toString()
            val name = "${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
            this@checkAllInternal.test(
                name = name.truncated(maxLength).escaped,
                displayName = name.truncated(displayNameMaxLength).escaped,
                testConfig = testConfig
            ) {
                with(context) {
                    content(value)
                }
            }
        }
    }
}


data class ConfiguredPropertyScope<Value>(
    private val compactName: String?,
    private val maxLength: Int,
    private val displayNameMaxLength: Int,
    val testSuite: TestSuite,
    val iterations: Int,
    val genA: Gen<Value>,
    val testConfig: TestConfig = TestConfig
) {
    /**
     * @param content Test suite block receiving generated values
     */
    operator fun minus(content: context(PropertyContext) TestSuite.(Value) -> Unit) {
        testSuite.checkAllSuitesInternal(iterations, genA, compactName, maxLength, displayNameMaxLength,testConfig, content)
    }
}

/**
 * Creates a configured property scope for property-based testing with specified iterations.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 */
inline fun <reified Value> TestSuite.checkAll(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredPropertyScope(
    if (compact) Value::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength,
    this,
    iterations,
    genA,
    testConfig
)

/**
 * Creates test suites for property-based testing with specified iterations.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
inline fun <reified Value> TestSuite.checkAllSuites(
    iterations: Int,
    genA: Gen<Value>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    crossinline content: context(PropertyContext) TestSuite.(Value) -> Unit
) = checkAllSuitesInternal(
    iterations,
    genA,
    if (compact) Value::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength,
    testConfig
) { content(it) }


fun <Value> TestSuite.checkAllSuitesInternal(
    iterations: Int,
    genA: Gen<Value>,
    compactName: String?,
    maxLength: Int,
    displayNameMaxLength: Int,
    testConfig: TestConfig = TestConfig,
    content: context(PropertyContext) TestSuite.(Value) -> Unit
) {
    if (compactName == null) {
        checkAllSeries(iterations, genA) { iter, value, context ->
            val valueStr = if (value is Iterable<*>) value.joinToString() else value.toString()
            val prefix = if (value == null) "null" else value::class.simpleName
            val name = "${iter + 1} of $iterations ${prefix}s (${valueStr})"
            this@checkAllSuitesInternal.testSuite(
                name = name.truncated(maxLength).escaped,
                displayName = name.truncated(displayNameMaxLength).escaped,
                testConfig = testConfig,
                content = fun TestSuite.() {
                    with(context) {
                        content(value)
                    }
                })
        }
    } else {
        val testName = "$iterations ${compactName}s"
        this@checkAllSuitesInternal.testSuite(
            name = testName.truncated(maxLength).escaped,
            displayName = testName.truncated(displayNameMaxLength).escaped,
            testConfig = testConfig
        ) {
            val errors = mutableMapOf<String, Throwable?>()
            checkAllSeries(iterations, genA) { iter, value, context ->
                with(context) {
                    val valueStr = if (value is Iterable<*>) value.joinToString() else value.toString()
                    val name =
                        "${iter + 1} of $iterations ${if (value == null) "null" else value::class.simpleName}: $valueStr"
                    catchingUnwrapped {
                        content(value)
                        errors["OK:    $name"] = null
                    }.onFailure {
                        errors["Error: $name"] = it
                    }
                }
            }
            if (errors.values.filterNotNull().isNotEmpty()) {
                val messages = errors.map { (msg, err) -> msg + (err?.let { ": ${it.message}" }) }.joinToString("\n")
                throw (if (errors.count { it.value is AssertionError } == errors.size) AssertionErrorBuilder(
                    testName + "\n$messages",
                    null,
                    null,
                    null
                ).build() else RuntimeException(
                    testName + "\n$messages"
                )).also { errors.values.filterNotNull().forEach(it::addSuppressed) }
            }
        }
    }
}

/**
 * Creates a configured property scope for property-based testing using default iteration count.
 *
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
inline fun <reified A> TestSuite.checkAll(
    genA: Gen<A>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
) = ConfiguredPropertyScope(
    if (compact) A::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength,
    this,
    PropertyTesting.defaultIterationCount,
    genA,
    testConfig
)

/**
 * Creates test suites for property-based testing using default iteration count.
 *
 * @param genA Generator for test values
 * @param compact whether to compact all generated child test elements into one
 * @param maxLength maximum length of test element name (not display name)
 * @param displayNameMaxLength maximum length of test element **display name**
 * @param testConfig Optional test configuration
 * @param content Test suite block receiving generated values
 */
inline fun <reified A> TestSuite.checkAllSuites(
    genA: Gen<A>,
    compact: Boolean = PropertyTest.compactByDefault,
    maxLength: Int = PropertyTest.defaultTestNameMaxLength,
    displayNameMaxLength: Int = PropertyTest.defaultDisplayNameMaxLength,
    testConfig: TestConfig = TestConfig,
    noinline content: context(PropertyContext) TestSuite.(A) -> Unit
) = checkAllSuitesInternal(
    PropertyTesting.defaultIterationCount,
    genA,
    if (compact) A::class.simpleName ?: "<Anonymous>" else null,
    maxLength,
    displayNameMaxLength,
    testConfig,
    content
)

/**
 * Internal function to handle series of property-based test executions.
 *
 * @param iterations Number of test iterations to perform
 * @param genA Generator for test values
 * @param series Block to execute for each generated value
 */
@PublishedApi
internal inline fun <Value> checkAllSeries(
    iterations: Int,
    genA: Gen<Value>,
    series: (Int, Value, PropertyContext) -> Unit
) {
    val constraints = Constraints.iterations(iterations)

    @Suppress("OPT_IN_USAGE")
    val config = PropTestConfig(constraints = constraints)
    val context = PropertyContext(config)
    genA.generate(RandomSource.default(), config.edgeConfig)
        .takeWhile { constraints.evaluate(context) }
        .forEachIndexed { iter, sample ->
            context.markEvaluation()
            series(iter, sample.value, context)
            context.markSuccess()
        }
}
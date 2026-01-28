package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.shared.TestDisplayName
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlin.jvm.JvmInline

/**
 * Scope for managing mutable test fixtures that are regenerated for each test.
 * Provides a structured way to define tests that require fresh fixture instances generated before each test.
 *
 * @param T The type of fixture object being managed
 * @property testSuite The parent test suite containing this mutable fixture scope
 * @property generator Suspending function that generates new fixture instances
 */
class GeneratingFixtureScope<T> @PublishedApi internal constructor(
    val testSuite: TestSuiteScope,
    val generator: suspend () -> T
) {
    /**
     * Registers a test that uses a fresh fixture instance as a child of the current [testSuite].
     *
     * @param name The name of the test
     * @param displayName The display name of the test
     * @param maxLength maximum length of test element name (not display name)
     * @param testConfig Configuration for test execution
     * @param content Test block that receives a newly generated fixture instance
     */
    @TestRegistering
    fun test(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        maxLength: Int = TestBalloonAddons.defaultTestNameMaxLength,
        testConfig: TestConfig = TestConfig,
        content: suspend Test.ExecutionScope.(T) -> Unit
    ) {
        with(testSuite) {
            val truncatedName = name.truncated(maxLength)
            testSuiteInScope.checkPathLenIncluding(truncatedName)
            test(
                truncatedName,
                (displayName.truncated(maxLength)),
                testConfig = testConfig
            ) { content(generator()) }
        }
    }
}


/**
 * Scope for managing mutable test fixtures that are regenerated for each test.
 * Provides a structured way to define tests that require fresh fixture instances generated before each test.
 *
 * @param T The type of fixture object being managed
 * @property testSuite The parent test suite containing this mutable fixture scope
 * @property generator Function that generates new fixture instances
 */
class NonSuspendingGeneratingFixtureScope<T> @PublishedApi internal constructor(
    val testSuite: TestSuiteScope,
    val generator: () -> T
) {
    /**
     * Registers a test that uses a fresh fixture instance as a child of the current [testSuite].
     *
     * @param name The name of the test
     * @param displayName The display name of the test
     * @param maxLength maximum length of test element name (not display name)
     * @param testConfig Configuration for test execution
     * @param content Test block that receives a newly generated fixture instance
     */
    @TestRegistering
    fun test(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        maxLength: Int = TestBalloonAddons.defaultTestNameMaxLength,
        testConfig: TestConfig = TestConfig,
        content: suspend Test.ExecutionScope.(T) -> Unit
    ) {
        with(testSuite) {
            val truncatedName = name.truncated(maxLength)
            testSuiteInScope.checkPathLenIncluding(truncatedName)
            test(
                truncatedName,
                ( displayName.truncated(maxLength)),
                testConfig = testConfig
            ) { content(generator()) }
        }
    }

    /**
     * Registers a test suite that uses a fresh fixture instance as a child of the current [testSuite].
     *
     * @param name The name of the suite
     * @param displayName The display name of the test
     * @param maxLength maximum length of test element name (not display name)
     * @param testConfig Configuration for test execution
     * @param content Test suite block that receives a newly generated fixture instance
     */
    @TestRegistering
    fun testSuite(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        maxLength: Int = TestBalloonAddons.defaultTestNameMaxLength,
        testConfig: TestConfig = TestConfig,
        content: TestSuiteScope.(T) -> Unit
    ) {
        with(testSuite) {
            val truncatedName = name.truncated(maxLength)
            testSuiteInScope.checkPathLenIncluding(truncatedName)
            testSuite(
                truncatedName,
                (displayName.truncated(maxLength)),
                testConfig = testConfig
            ) { content(generator()) }
        }
    }
}


@JvmInline
value class GeneratingFixtureScopHolder<T>(val scope: NonSuspendingGeneratingFixtureScope<T>) {

    operator fun minus(block: NonSuspendingGeneratingFixtureScope<T>.() -> Unit) = scope.block()
}

@JvmInline
value class GeneratingSuspendFixtureScopHolder<T>(val scope: GeneratingFixtureScope<T>) {

    operator fun minus(block: GeneratingFixtureScope<T>.() -> Unit) = scope.block()
}


/**
 * Prepares a fixture-generating scope from a generator function.
 *
 * Use as follows:
 * ```kotlin
 * val aGeneratingSuite by testSuite {
 *     withFixtureGenerator { Random.nextBytes(32) } - {
 *
 *         test("A Test with fresh Randomness") { freshFixture ->
 *             //your test logic here
 *         }
 *
 *         repeat(100) {
 *             test("A Test with fresh Randomness") { freshFixture ->
 *                 //some more test logic; each call gets fresh randomness
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param T The type of fixture object being managed
 * @param generator The generator function invoked to provide fresh state fo each test
 */
fun <T> TestSuiteScope.withFixtureGenerator(generator: (() -> T)) = GeneratingFixtureScopHolder(
    NonSuspendingGeneratingFixtureScope(this, generator)
)

/**
 * Prepares a fixture-generating scope from a generator function.
 *
 * Use as follows:
 * ```kotlin
 * val aGeneratingSuite by testSuite {
 *     withFixtureGenerator(suspend { Random.nextBytes(32) }) - {
 *
 *         test("A Test with fresh Randomness") { freshFixture ->
 *             //your test logic here
 *         }
 *
 *         repeat(100) {
 *             test("A Test with fresh Randomness") { freshFixture ->
 *                 //some more test logic; each call gets fresh randomness
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param T The type of fixture object being managed
 * @param generator The generator function invoked to provide fresh state fo each test
 */
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <T> TestSuiteScope.withFixtureGenerator(generator: suspend (() -> T)) =
    GeneratingSuspendFixtureScopHolder(GeneratingFixtureScope(this, generator))
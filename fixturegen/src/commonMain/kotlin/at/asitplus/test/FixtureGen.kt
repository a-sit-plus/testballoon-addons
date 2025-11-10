package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestExecutionScope
import de.infix.testBalloon.framework.core.TestSuite
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
    val testSuite: TestSuite,
    val generator: suspend () -> T
) {
    /**
     * Registers a test that uses a fresh fixture instance as a child of the current [testSuite].
     *
     * @param name The name of the test
     * @param displayName The display name of the test
     * @param testConfig Configuration for test execution
     * @param content Test block that receives a newly generated fixture instance
     */
    @TestRegistering
    fun test(
        @TestElementName name: String,
        @TestDisplayName displayName: String = name,
        testConfig: TestConfig = TestConfig,
        content: suspend TestExecutionScope.(T) -> Unit
    ) {
        testSuite.test(name.truncated(), displayName.escaped, testConfig = testConfig) { content(generator()) }
    }
}


@JvmInline
value class GeneratingFixtureScopHolder<T>(val scope: GeneratingFixtureScope<T>) {

    inline operator fun minus(noinline block: GeneratingFixtureScope<T>.() -> Unit) = scope.block()
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
 *     generatingFixture { Random.nextBytes(32) } - {
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
inline fun <reified T> TestSuite.generatingFixture(noinline generator: (() -> T)) = GeneratingFixtureScopHolder(
    GeneratingFixtureScope(this, generator)
)

/**
 * Prepares a fixture-generating scope from a generator function.
 *
 * Use as follows:
 * ```kotlin
 * val aGeneratingSuite by testSuite {
 *     generatingFixture suspend { Random.nextBytes(32) } - {
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
inline fun <reified T> TestSuite.generatingFixture(noinline generator: suspend (() -> T)) =
    GeneratingSuspendFixtureScopHolder(GeneratingFixtureScope(this, generator))



/**
 * Creates a fixture-generating scope from a suspending generator function.
 *
 * Use as follows:
 * ```kotlin
 * val aGeneratingSuite by testSuite {
 *     { Random.nextBytes(32) }.generatingFixtureFor {
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
 * @param block Configuration block for defining tests using the fixture
 */
@Deprecated("To be removed in 0.4.0", replaceWith = ReplaceWith("generatingFixture(this)"), DeprecationLevel.ERROR)
context(suite: TestSuite)
inline fun <reified T> (suspend () -> T).generatingFixtureFor(
    block: GeneratingFixtureScope<T>.() -> Unit
) {
    val scope = GeneratingFixtureScope(suite, this)
    scope.block()
}

/**
 * Creates a fixture-generating scope from a non-suspending generator function.
 *
 * Use as follows:
 * ```kotlin
 * val aGeneratingSuite by testSuite {
 *     { Random.nextBytes(32) }.generatingFixtureFor {
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
 * @param block Configuration block for defining tests using the fixture
 */
@Deprecated("To be removed in 0.4.0", replaceWith = ReplaceWith("generatingFixture(this)"), DeprecationLevel.ERROR)
context(suite: TestSuite)
inline fun <reified T> (() -> T).generatingFixtureFor(
    block: GeneratingFixtureScope<T>.() -> Unit
) {
    val suspendGen: suspend () -> T = { this() }
    val scope = GeneratingFixtureScope(suite, suspendGen)
    scope.block()
}
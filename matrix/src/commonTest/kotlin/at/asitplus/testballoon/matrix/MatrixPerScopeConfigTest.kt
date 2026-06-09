package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundAll
import de.infix.testBalloon.framework.core.testScope
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.coroutines.coroutineContext

// Per-scope matrix config: `test` / `testSuite` and their FreeSpec forms accept `matrixConfig { … }` to override
// execution (etc.) for that subtree. Verified via the concurrency→testScope-disable behavior: a Concurrent override
// drops the inherited virtual-time TestScope, a Sequential sibling keeps it. Unset fields inherit the enclosing scope.

val perSuiteMatrixConfig by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    testSuite("concurrent group", matrixConfig { execution = ExecutionMode.Concurrent(2) }) {
        "the concurrent override disables the virtual-time TestScope" {
            coroutineContext[TestCoroutineScheduler].shouldBeNull()
        }
    }
    "a sequential sibling keeps the inherited TestScope" {
        coroutineContext[TestCoroutineScheduler].shouldNotBeNull()
    }
}

val perTestMatrixConfig by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    test("concurrent test", matrixConfig { execution = ExecutionMode.Concurrent(2) }) {
        coroutineContext[TestCoroutineScheduler].shouldBeNull()
    }
}

val freeSpecMatrixConfig by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    "concurrent group"(matrixConfig { execution = ExecutionMode.Concurrent(2) }) - {
        "freespec group override disables the TestScope" {
            coroutineContext[TestCoroutineScheduler].shouldBeNull()
        }
    }
    "freespec test override"(matrixConfig { execution = ExecutionMode.Concurrent(2) }) {
        coroutineContext[TestCoroutineScheduler].shouldBeNull()
    }
}

// Concurrency must win over a user-supplied testConfig: even if the user's testConfig enables a TestScope,
// the matrix concurrency override disables it (testScope(false) is chained innermost, so it wins).
val concurrencyOverridesUserTestScope by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    testSuite(
        "forced concurrent",
        matrixConfig {
            execution = ExecutionMode.Concurrent(2)
            testConfig = TestConfig.testScope(isEnabled = true)
        },
    ) {
        "concurrency disables the TestScope even when the user's testConfig enabled it" {
            coroutineContext[TestCoroutineScheduler].shouldBeNull()
        }
    }
}

// Fixture-scope test/testSuite (and freespec) take matrixConfig the same way, and still inject the fixture value.
val fixtureMatrixConfig by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    fixture { 42 } - {
        testSuite("concurrent fixture group", matrixConfig { execution = ExecutionMode.Concurrent(2) }) { value ->
            "fixture suite override disables the TestScope and still sees the fixture" {
                coroutineContext[TestCoroutineScheduler].shouldBeNull()
                value shouldBe 42
            }
        }
        test("concurrent fixture test", matrixConfig { execution = ExecutionMode.Concurrent(2) }) { value ->
            coroutineContext[TestCoroutineScheduler].shouldBeNull()
            value shouldBe 42
        }
        "freespec fixture group"(matrixConfig { execution = ExecutionMode.Concurrent(2) }) - { value ->
            "child sees the fixture under a concurrent override" { value shouldBe 42 }
        }
    }
}

// The testConfig-only shorthand still wraps matrixConfig and applies aroundAll exactly once.
private var shorthandAroundAllRuns = 0

val testConfigShorthandStillAppliesOnce by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    testSuite("g", testConfig = TestConfig.aroundAll { shorthandAroundAllRuns++; it() }) {
        data(listOf(1, 2, 3)) test { }
    }
    "the testConfig shorthand aroundAll wraps its suite once" {
        shorthandAroundAllRuns shouldBe 1
    }
}

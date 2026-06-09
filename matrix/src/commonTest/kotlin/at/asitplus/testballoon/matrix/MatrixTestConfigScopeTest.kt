package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundAll
import io.kotest.matchers.shouldBe

// Regression: a TestConfig set at a matrix level must be applied ONCE at that level (testBalloon inherits it to
// children structurally), not re-applied at every nested matrix level. The latter is what made a session's
// `testScope` multiply into a deadlock and would make a suite-level `aroundAll` run once per data case.

private var suiteAroundAllRuns = 0

val matrixSuiteTestConfigAppliesOnce by matrixSuite(matrixConfig {
        execution = ExecutionMode.Sequential
        testConfig = TestConfig.aroundAll { suiteAroundAllRuns++; it() }
    }) {
    data(listOf(1, 2, 3)) test { }
    "a suite-level aroundAll wraps the suite once, not once per data case" {
        suiteAroundAllRuns shouldBe 1
    }
}

private var nestedSuiteAroundAllRuns = 0

val matrixNestedTestSuiteTestConfigAppliesOnce by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    testSuite("group", testConfig = TestConfig.aroundAll { nestedSuiteAroundAllRuns++; it() }) {
        data(listOf(1, 2, 3)) test { }
    }
    "a nested testSuite-level aroundAll wraps that suite once, not once per data case" {
        nestedSuiteAroundAllRuns shouldBe 1
    }
}

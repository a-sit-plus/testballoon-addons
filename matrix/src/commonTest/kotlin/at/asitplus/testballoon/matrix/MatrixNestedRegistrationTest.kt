package at.asitplus.testballoon.matrix

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

// The forgotten-`-` footgun: a `"name" { … }` registered while a test body runs (i.e. on a scope whose
// build window already closed) must throw loudly instead of being silently dropped. These suites capture
// the scope and register after its build window, asserting the throw — so they pass.

val nestedRegistrationRealTreeTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    val scope = this // the root MatrixSuiteScope; open only during this build body
    "registering on a matrix scope while a test runs throws" {
        // we are now executing a test body — scope's build window is closed
        val error = shouldThrow<IllegalStateException> { scope.test("late") {} }
        error.message!! shouldContain "forgot the '-'"
    }
}

val nestedRegistrationRealTreeHappyPathTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var innerRan = false
    "outer" - {
        "inner" { innerRan = true }
    }
    "a correctly opened enclosing suite still registers and runs its child" {
        innerRan shouldBe true
    }
}

val nestedRegistrationCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var planningScope: CompactScope? = null
    compact("c") { report = CompactReport.SummaryOnly } - {
        planningScope = this // the compact planning scope; open only during planning
        "ok" { }
    }
    "registering on a compact scope after planning throws" {
        val error = shouldThrow<IllegalStateException> { planningScope!!.test("late") {} }
        error.message!! shouldContain "forgot the '-'"
    }
}

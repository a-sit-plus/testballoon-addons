package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

val CollatedTestFailuresTest by testSuite {

    test("ordinary failures keep first message line in summary") {
        val error = shouldThrow<AssertionError> {
            CollatedTestFailures("outer", false).apply {
                recordOk("ok")
                recordError("case", AssertionError("boom\nextra detail"))
                throwIfAny()
            }
        }

        error.message!!.contains("Summary: 1 OK, 1 failed").shouldBeTrue()
        error.message!!.contains("Error: case: boom").shouldBeTrue()
    }

    test("nested collated failures use child summary only") {
        val child = shouldThrow<AssertionError> {
            CollatedTestFailures("child", false).apply {
                recordError("inner", AssertionError("deep failure"))
                throwIfAny()
            }
        }

        val parent = shouldThrow<AssertionError> {
            CollatedTestFailures("parent", false).apply {
                recordError("outer", child)
                throwIfAny()
            }
        }

        val message = parent.message!!
        message.contains("Error: outer: child").shouldBeTrue()
        message.lineSequence().none { it == "Error: inner: deep failure" }.shouldBeTrue()
        message.split("Stack trace of first error").size shouldBe 2
    }

    test("suppressed errors are still attached when enabled") {
        val error = shouldThrow<AssertionError> {
            CollatedTestFailures("outer", true).apply {
                recordError("one", AssertionError("first"))
                recordError("two", AssertionError("second"))
                throwIfAny()
            }
        }

        error.suppressedExceptions.size shouldBe 2
    }

    test("suppressed errors are not retained when disabled") {
        val error = shouldThrow<AssertionError> {
            CollatedTestFailures("outer", false).apply {
                recordError("one", AssertionError("first"))
                recordError("two", AssertionError("second"))
                throwIfAny()
            }
        }

        error.suppressedExceptions.size shouldBe 0
    }

    test("success rows can be suppressed while summary keeps counts") {
        val error = shouldThrow<AssertionError> {
            CollatedTestFailures("outer", addSuppressedErrors = false, suppressSuccesses = true).apply {
                recordOk("one")
                recordError("two", AssertionError("second"))
                throwIfAny()
            }
        }

        val message = error.message!!
        message.contains("Summary: 1 OK, 1 failed").shouldBeTrue()
        message.contains("OK:    one").shouldBeFalse()
        message.contains("Error: two: second").shouldBeTrue()
    }

    test("mixed failures throw runtime wrapper") {
        val error = shouldThrow<RuntimeException> {
            CollatedTestFailures("outer", false).apply {
                recordError("one", AssertionError("first"))
                recordError("two", IllegalStateException("second"))
                throwIfAny()
            }
        }

        error.message!!.contains("Error: two: second").shouldBeTrue()
    }
}

import at.asitplus.testballoon.DataTest
import at.asitplus.testballoon.runCompactedData
import at.asitplus.testballoon.runCompactedDataSuspend
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

val dataCompactionSuite by testSuite {

    test("compacted data terminal collation omits suppressed errors by default") {
        DataTest.addSuppressedErrorsToCompactedFailures = false
        try {
            val error = shouldThrow<AssertionError> {
                runCompactedDataSuspend(sequenceOf("one" to 1, "two" to 2), "ΣInt") { value ->
                    if (value != 1) throw AssertionError("bad $value")
                }
            }

            error.suppressedExceptions.size shouldBe 0
            error.message!!.contains("OK:    1: one").shouldBeTrue()
            error.message!!.contains("Error: 2: two: bad 2").shouldBeTrue()
        } finally {
            DataTest.addSuppressedErrorsToCompactedFailures = null
        }
    }

    test("compacted data suite collation can attach suppressed errors") {
        DataTest.addSuppressedErrorsToCompactedFailures = true
        try {
            val error = shouldThrow<AssertionError> {
                runCompactedData(sequenceOf("one" to 1, "two" to 2, "three" to 3), "ΣInt") { value ->
                    if (value != 1) throw AssertionError("bad $value")
                }
            }

            error.suppressedExceptions.size shouldBe 2
            error.message!!.contains("OK:    1: one").shouldBeTrue()
            error.message!!.contains("Error: 2: two: bad 2").shouldBeTrue()
            error.message!!.contains("Error: 3: three: bad 3").shouldBeTrue()
        } finally {
            DataTest.addSuppressedErrorsToCompactedFailures = null
        }
    }
}

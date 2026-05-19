import at.asitplus.testballoon.DataTest
import at.asitplus.testballoon.generatedDataName
import at.asitplus.testballoon.runCompactedData
import at.asitplus.testballoon.runCompactedDataSuspend
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

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
            error.message!!.contains("Summary: 1 OK, 1 failed").shouldBeTrue()
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
            error.message!!.contains("Summary: 1 OK, 2 failed").shouldBeTrue()
            error.message!!.contains("OK:    1: one").shouldBeTrue()
            error.message!!.contains("Error: 2: two: bad 2").shouldBeTrue()
            error.message!!.contains("Error: 3: three: bad 3").shouldBeTrue()
        } finally {
            DataTest.addSuppressedErrorsToCompactedFailures = null
        }
    }

    test("compacted data can suppress success rows") {
        DataTest.suppressCompactSuccesses = true
        try {
            val error = shouldThrow<AssertionError> {
                runCompactedData(sequenceOf("one" to 1, "two" to 2), "ΣInt") { value ->
                    if (value != 2) throw AssertionError("bad $value")
                }
            }

            val message = error.message!!
            message.contains("Summary: 1 OK, 1 failed").shouldBeTrue()
            message.contains("OK:    2: two").shouldBeFalse()
            message.contains("Error: 1: one: bad 1").shouldBeTrue()
        } finally {
            DataTest.suppressCompactSuccesses = null
        }
    }

    test("compacted data terminal can run suspended leaves concurrently") {
        val error = shouldThrow<AssertionError> {
            runCompactedDataSuspend(
                data = sequenceOf("one" to 1, "two" to 2, "three" to 3),
                testName = "ΣInt",
                compactConcurrent = false
            ) { value ->
                delay((4 - value).toLong())
                if (value != 2) throw AssertionError("bad $value")
            }
        }

        val message = error.message!!
        message.contains("Summary: 1 OK, 2 failed").shouldBeTrue()
        message.contains("Error: 1: one: bad 1").shouldBeTrue()
        message.contains("Error: 3: three: bad 3").shouldBeTrue()
    }

    test("compacted data concurrent terminal can suppress success rows") {
        val error = shouldThrow<AssertionError> {
            runCompactedDataSuspend(
                data = sequenceOf("one" to 1, "two" to 2),
                testName = "ΣInt",
                suppressCompactSuccesses = true,
                compactConcurrent = false
            ) { value ->
                delay(value.toLong())
                if (value != 2) throw AssertionError("bad $value")
            }
        }

        val message = error.message!!
        message.contains("Summary: 1 OK, 1 failed").shouldBeTrue()
        message.contains("OK:    2: two").shouldBeFalse()
        message.contains("Error: 1: one: bad 1").shouldBeTrue()
    }

    test("compacted data generated names respect maxLength") {
        val result = generatedDataName(
            data = "abcdefghij",
            compact = true,
            maxLength = 6,
            prefix = "",
        )

        result.length shouldBeLessThanOrEqual 6
        result shouldBe "ab…hij"
    }
}

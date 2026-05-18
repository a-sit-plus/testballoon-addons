import at.asitplus.testballoon.PropertyTest
import at.asitplus.testballoon.generatedPropertyLeafName
import at.asitplus.testballoon.runCompactedProperty
import at.asitplus.testballoon.runCompactedPropertySuspend
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.property.PropertyContext

val propertyCompactionSuite by testSuite {

    test("compacted property terminal records context and collation") {
        PropertyTest.addSuppressedErrorsToCompactedFailures = false
        try {
            val context = PropertyContext()
            val error = shouldThrow<AssertionError> {
                with(context) {
                    runCompactedPropertySuspend(sequenceOf(1, 2), 2, "ΣInt", maxLength = 64) { value ->
                        if (value != 1) throw AssertionError("bad $value")
                    }
                }
            }

            context.evals() shouldBe 2
            context.successes() shouldBe 1
            context.failures() shouldBe 1
            error.suppressedExceptions.size shouldBe 0
            error.message!!.contains("Summary: 1 OK, 1 failed").shouldBeTrue()
            error.message!!.contains("OK:    1 of 2 Int: 1").shouldBeTrue()
            error.message!!.contains("Error: 2 of 2 Int: 2: bad 2").shouldBeTrue()
        } finally {
            PropertyTest.addSuppressedErrorsToCompactedFailures = null
        }
    }

    test("compacted property suite can attach suppressed errors") {
        PropertyTest.addSuppressedErrorsToCompactedFailures = true
        try {
            val context = PropertyContext()
            val error = shouldThrow<AssertionError> {
                with(context) {
                    runCompactedProperty(sequenceOf(1, 2, 3), 3, "ΣInt", maxLength = 64) { value ->
                        if (value != 1) throw AssertionError("bad $value")
                    }
                }
            }

            context.evals() shouldBe 3
            context.successes() shouldBe 1
            context.failures() shouldBe 2
            error.suppressedExceptions.size shouldBe 2
            error.message!!.contains("Summary: 1 OK, 2 failed").shouldBeTrue()
            error.message!!.contains("Error: 2 of 3 Int: 2: bad 2").shouldBeTrue()
            error.message!!.contains("Error: 3 of 3 Int: 3: bad 3").shouldBeTrue()
        } finally {
            PropertyTest.addSuppressedErrorsToCompactedFailures = null
        }
    }

    test("compacted property can suppress success rows") {
        PropertyTest.suppressCompactSuccesses = true
        try {
            val context = PropertyContext()
            val error = shouldThrow<AssertionError> {
                with(context) {
                    runCompactedProperty(sequenceOf(1, 2), 2, "ΣInt", maxLength = 64) { value ->
                        if (value != 2) throw AssertionError("bad $value")
                    }
                }
            }

            context.evals() shouldBe 2
            context.successes() shouldBe 1
            context.failures() shouldBe 1
            val message = error.message!!
            message.contains("Summary: 1 OK, 1 failed").shouldBeTrue()
            message.contains("OK:    2 of 2 Int: 2").shouldBeFalse()
            message.contains("Error: 1 of 2 Int: 1: bad 1").shouldBeTrue()
        } finally {
            PropertyTest.suppressCompactSuccesses = null
        }
    }

    test("non-compact property terminal names have no leading space without prefix") {
        val name = generatedPropertyLeafName("", iter = 0, iterations = 1, value = 7, maxLength = 64)

        name.startsWith(" ").shouldBeFalse()
        name shouldBe "1 of 1 Int: 7"
    }

    test("compacted property row values respect maxLength") {
        val context = PropertyContext()
        val error = shouldThrow<AssertionError> {
            with(context) {
                runCompactedProperty(sequenceOf("abcdefghij"), 1, "ΣString", maxLength = 18) {
                    throw AssertionError("bad")
                }
            }
        }

        error.message!!.contains("Error: 1 of 1 String: a…j: bad").shouldBeTrue()
    }
}

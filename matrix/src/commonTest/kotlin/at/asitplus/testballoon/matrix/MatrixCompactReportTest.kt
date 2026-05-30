package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import kotlin.coroutines.EmptyCoroutineContext

val MatrixCompactReportTest by testSuite {

    test("compact report frames first stack trace with dashed lines") {
        val error = shouldThrow<AssertionError> {
            CompactRun(
                name = "compact",
                config = CompactConfig(
                    report = CompactReport.FailuresOnly,
                    addSuppressedErrors = false,
                    reportRows = 1,
                    progressIndicator = Indicator.None,
                    coroutineContext = EmptyCoroutineContext,
                ),
            ).apply {
                failure(listOf("row"), AssertionError("boom"))
                throwIfAny()
            }
        }

        val message = error.message!!
        message.contains("----------------------------------------").shouldBeTrue()
        message.contains("Stack trace of first error: Failure: row").shouldBeTrue()
        message.contains("AssertionError: boom").shouldBeTrue()
    }

    test("compact report with all rows omitted has no fake summary cause") {
        val error = shouldThrow<AssertionError> {
            CompactRun(
                name = "compact",
                config = CompactConfig(
                    report = CompactReport.FailuresOnly,
                    addSuppressedErrors = false,
                    reportRows = 0,
                    progressIndicator = Indicator.None,
                    coroutineContext = EmptyCoroutineContext,
                ),
            ).apply {
                failure(listOf("row"), AssertionError("boom"))
                throwIfAny()
            }
        }

        val message = error.message!!
        message.contains("... 1 failures and 0 OKs omitted from compact report").shouldBeTrue()
        message.contains("Stack traces omitted: all compact failures were omitted from compact report").shouldBeTrue()
        message.contains("AssertionError: boom").shouldBeFalse()
        error.cause.shouldBeNull()
    }
}

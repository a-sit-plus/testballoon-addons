package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.coroutines.EmptyCoroutineContext

private fun compactRun(
    report: CompactReport,
    reportRows: Int = -1,
    addSuppressedErrors: Boolean = false,
) = CompactRun(
    name = "compact",
    config = CompactConfig(
        concurrency = CompactConcurrency.Layered,
        report = report,
        addSuppressedErrors = addSuppressedErrors,
        reportRows = reportRows,
        progressIndicator = Indicator.None,
        coroutineContext = EmptyCoroutineContext,
    ),
)

/** Behaviour of the compact run accumulator and its rendered report across report modes. */
val MatrixCompactRunTest by testSuite {

    test("a run with no failures does not throw") {
        shouldNotThrowAny {
            compactRun(CompactReport.AllCases).apply {
                success(listOf("ok"))
                throwIfAny()
            }
        }
    }

    test("all-cases report lists OK rows and failure rows") {
        val message = shouldThrow<AssertionError> {
            compactRun(CompactReport.AllCases).apply {
                success(listOf("good case"))
                failure(listOf("bad case"), AssertionError("boom"))
                throwIfAny()
            }
        }.message!!
        message.shouldContain("1 succeeded, 1 failed")
        message.shouldContain("OK     : good case")
        message.shouldContain("Failure: bad case")
    }

    test("failures-only report omits OK rows") {
        val message = shouldThrow<AssertionError> {
            compactRun(CompactReport.FailuresOnly).apply {
                success(listOf("good"))
                failure(listOf("bad"), AssertionError("boom"))
                throwIfAny()
            }
        }.message!!
        message.shouldNotContain("OK     :")
        message.shouldContain("Failure: bad")
    }

    test("summary-only report omits individual failure rows but keeps the count") {
        val message = shouldThrow<AssertionError> {
            compactRun(CompactReport.SummaryOnly).apply {
                failure(listOf("bad"), AssertionError("boom"))
                throwIfAny()
            }
        }.message!!
        message.shouldNotContain("Failure: bad")
        message.shouldContain("1 failed")
    }

    test("reportRows caps rendered failures and counts the omitted ones") {
        val message = shouldThrow<AssertionError> {
            compactRun(CompactReport.FailuresOnly, reportRows = 1).apply {
                failure(listOf("f1"), AssertionError("boom1"))
                failure(listOf("f2"), AssertionError("boom2"))
                failure(listOf("f3"), AssertionError("boom3"))
                throwIfAny()
            }
        }.message!!
        message.shouldContain("Failure: f1")
        message.shouldNotContain("Failure: f3")
        message.shouldContain("omitted")
    }

    test("addSuppressedErrors attaches the remaining failures as suppressed") {
        val error = shouldThrow<AssertionError> {
            compactRun(CompactReport.FailuresOnly, addSuppressedErrors = true).apply {
                failure(listOf("f1"), AssertionError("first"))
                failure(listOf("f2"), AssertionError("second"))
                throwIfAny()
            }
        }
        error.suppressedExceptions.size shouldBe 1
    }

    test("without addSuppressedErrors there are no suppressed exceptions") {
        val error = shouldThrow<AssertionError> {
            compactRun(CompactReport.FailuresOnly, addSuppressedErrors = false).apply {
                failure(listOf("f1"), AssertionError("first"))
                failure(listOf("f2"), AssertionError("second"))
                throwIfAny()
            }
        }
        error.suppressedExceptions.shouldBeEmpty()
    }

    test("the first recorded failure becomes the cause of the thrown assertion") {
        val error = shouldThrow<AssertionError> {
            compactRun(CompactReport.FailuresOnly).apply {
                failure(listOf("f1"), AssertionError("the-cause"))
                throwIfAny()
            }
        }
        error.cause?.message shouldBe "the-cause"
    }

    test("progress message reports completion and failure counts") {
        val run = compactRun(CompactReport.SummaryOnly)
        run.start()
        run.start()
        run.success(listOf("a"))
        run.failure(listOf("b"), AssertionError("x"))
        val message = run.progressMessage()
        message.shouldContain("2 of 2 queued completed")
        message.shouldContain("1 failed")
    }
}

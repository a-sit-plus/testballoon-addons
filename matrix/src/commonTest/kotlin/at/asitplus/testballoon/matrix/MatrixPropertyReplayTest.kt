package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlin.coroutines.EmptyCoroutineContext

val MatrixPropertyReplayTest by testSuite {

    test("property replay wraps assertion with full frame chain") {
        val original = AssertionError("boom")
        val wrapped = original.withMatrixPropertyReplay(
            listOf(
                MatrixPropertyReplayFrame("outer", seed = 111, iteration = 12, rowName = "12: alpha"),
                MatrixPropertyReplayFrame("inner", seed = 222, iteration = 417, rowName = "417: beta"),
            )
        )

        val message = wrapped.message!!
        message.startsWith("boom\n    Matrix property replay:").shouldBeTrue()
        message.contains("    Matrix property replay: outer: 12: alpha / inner: 417: beta").shouldBeTrue()
        message.contains("      - outer: seed=111, iteration=12").shouldBeTrue()
        message.contains("      - inner: seed=222, iteration=417").shouldBeTrue()
        message.contains("boom").shouldBeTrue()
        wrapped.cause shouldBe original
    }

    test("property replay wrapper ignores non-property failures") {
        val original = AssertionError("boom")

        original.withMatrixPropertyReplay(emptyList()) shouldBe original
    }

    test("compact summary report prints first omitted replay path") {
        val replayPath = listOf(
            MatrixPropertyReplayFrame("outer", seed = 111, iteration = 12, rowName = "12: alpha"),
            MatrixPropertyReplayFrame("inner", seed = 222, iteration = 417, rowName = "417: beta"),
        )

        val error = shouldThrow<AssertionError> {
            CompactRun(
                name = "compact",
                config = CompactConfig(
                    concurrency = CompactConcurrency.Layered,
                    report = CompactReport.SummaryOnly,
                    addSuppressedErrors = false,
                    reportRows = 0,
                    progressIndicator = Indicator.None,
                    coroutineContext = EmptyCoroutineContext,
                ),
            ).apply {
                failure(listOf("row"), AssertionError("boom").withMatrixPropertyReplay(replayPath), replayPath)
                throwIfAny()
            }
        }

        val message = error.message!!
        message.contains(
            "First matrix property replay omitted from row report: outer: 12: alpha / inner: 417: beta"
        ).shouldBeTrue()
        message.contains("- outer: seed=111, iteration=12").shouldBeTrue()
        message.contains("- inner: seed=222, iteration=417").shouldBeTrue()
        message.contains("Failure: row").shouldBeFalse()
    }
}

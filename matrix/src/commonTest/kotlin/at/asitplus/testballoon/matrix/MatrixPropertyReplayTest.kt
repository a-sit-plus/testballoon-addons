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
        val wrapped = original.withMatrixReplay(
            listOf(
                MatrixReplayFrame.Property("outer", seed = 111, iteration = 12, rowName = "12: alpha"),
                MatrixReplayFrame.Property("inner", seed = 222, iteration = 417, rowName = "417: beta"),
            )
        )

        val message = wrapped.message!!
        message.startsWith("boom\n    Error replay info:").shouldBeTrue()
        message.contains("    Error replay info: outer: 12: alpha / inner: 417: beta").shouldBeTrue()
        message.contains("      - outer: seed=111L, iteration=12L").shouldBeTrue()
        message.contains("      - inner: seed=222L, iteration=417L").shouldBeTrue()
        message.contains("boom").shouldBeTrue()
        wrapped.cause shouldBe original
    }

    test("property replay wrapper ignores non-property failures") {
        val original = AssertionError("boom")

        original.withMatrixReplay(emptyList()) shouldBe original
    }

    test("compact summary report prints first omitted replay path") {
        val replayPath = listOf(
            MatrixReplayFrame.Property("outer", seed = 111, iteration = 12, rowName = "12: alpha"),
            MatrixReplayFrame.Property("inner", seed = 222, iteration = 417, rowName = "417: beta"),
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
                failure(listOf("row"), AssertionError("boom").withMatrixReplay(replayPath), replayPath)
                throwIfAny()
            }
        }

        val message = error.message!!
        message.contains(
            "First error replay info omitted from row report: outer: 12: alpha / inner: 417: beta"
        ).shouldBeTrue()
        message.contains("- outer: seed=111L, iteration=12L").shouldBeTrue()
        message.contains("- inner: seed=222L, iteration=417L").shouldBeTrue()
        message.contains("Failure: row").shouldBeFalse()
    }
}

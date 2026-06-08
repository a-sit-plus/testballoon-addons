package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import kotlin.coroutines.EmptyCoroutineContext

val MatrixCompactResultTest by testSuite {

    test("compact suite planning errors are rethrown") {
        val matrixConfig = MatrixSuiteConfigBuilder().build()
        val compactConfig = CompactConfigBuilder(matrixConfig).build()
        val scope = CompactScope(matrixConfig, compactConfig)

        shouldThrow<IllegalArgumentException> {
            scope.building {
                scope.testSuite("outer") {
                    property("invalid property", Arb.int(), iterations = -1) test {}
                }
            }
        }
    }

    test("compact report frames first stack trace with dashed lines") {
        val error = shouldThrow<AssertionError> {
            CompactRun(
                name = "compact",
                config = CompactConfig(
                    concurrency = CompactConcurrency.Layered,
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
        message.shouldContain("----------------------------------------")
        message.shouldContain("Stack trace of first error: Failure: row")
        message.shouldContain("AssertionError: boom")
    }

    test("compact report with all rows omitted has no fake summary cause") {
        val error = shouldThrow<AssertionError> {
            CompactRun(
                name = "compact",
                config = CompactConfig(
                    concurrency = CompactConcurrency.Layered,
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
        message.shouldContain("... 1 failures and 0 OKs omitted from compact report")
        message.shouldContain("Stack traces omitted: all compact failures were omitted from compact report")
        message.shouldNotContain("AssertionError: boom")
        error.cause.shouldBeNull()
    }

    test("compact report separates multiline failure rows") {
        val error = shouldThrow<AssertionError> {
            CompactRun(
                name = "compact",
                config = CompactConfig(
                    concurrency = CompactConcurrency.Layered,
                    report = CompactReport.FailuresOnly,
                    addSuppressedErrors = false,
                    reportRows = 2,
                    progressIndicator = Indicator.None,
                    coroutineContext = EmptyCoroutineContext,
                ),
            ).apply {
                failure(listOf("row 1"), AssertionError("boom\nmore"))
                failure(listOf("row 2"), AssertionError("bang"))
                throwIfAny()
            }
        }

        error.message!!.contains("boom\nmore\n\nFailure: row 2").shouldBeTrue()
    }

    test("compact report indents property replay below assertion message") {
        val replayPath = listOf(
            MatrixReplayFrame.Property("first", seed = 111, iteration = 1, rowName = "1: alpha"),
            MatrixReplayFrame.Property("second", seed = 222, iteration = 2, rowName = "2: beta"),
        )
        val error = shouldThrow<AssertionError> {
            CompactRun(
                name = "compact",
                config = CompactConfig(
                    concurrency = CompactConcurrency.Layered,
                    report = CompactReport.FailuresOnly,
                    addSuppressedErrors = false,
                    reportRows = 1,
                    progressIndicator = Indicator.None,
                    coroutineContext = EmptyCoroutineContext,
                ),
            ).apply {
                failure(listOf("row"), AssertionError("boom").withMatrixReplay(replayPath), replayPath)
                throwIfAny()
            }
        }

        val message = error.message!!
        message.shouldContain("  error: AssertionError: boom\n")
        message.shouldContain("    Error replay info: (property) first: 1: alpha ↘ (property) second: 2: beta\n")
        message.shouldContain("      - (property) first: replay = Cases(seed = 111L, iter = 1L)\n")
        message.shouldContain("      - (property) second: replay = Cases(seed = 222L, iter = 2L)\n")
    }
}

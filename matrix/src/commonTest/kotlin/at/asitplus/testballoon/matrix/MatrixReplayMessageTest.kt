package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

/** Rendering of the "Error replay info" block and the assertion-wrapping helper. */
val MatrixReplayMessageTest by testSuite {

    test("an empty replay path renders just the header") {
        emptyList<MatrixReplayFrame>().message() shouldBe "Error replay info:"
    }

    test("message renders named data and property frames with their type marker") {
        val frames = listOf(
            MatrixReplayFrame.Data("d", index = 2L, rowName = "two"),
            MatrixReplayFrame.Property("p", seed = 9L, iteration = 4L, rowName = "four"),
        )
        val message = frames.message(prefix = "Repro:")
        message.shouldStartWith("Repro: (data) d: two ↘ (property) p: four")
        message.shouldContain("- (data) d: replay = Indexes(2L)")
        message.shouldContain("- (property) p: replay = Cases(seed = 9L, iter = 4L)")
    }

    test("a null layer name (nameless layer) keeps the type marker but drops the name") {
        val frames = listOf(
            MatrixReplayFrame.Data(null, index = 2L, rowName = "two"),
            MatrixReplayFrame.Property(null, seed = 9L, iteration = 4L, rowName = "four"),
        )
        val message = frames.message(prefix = "Repro:")
        message.shouldStartWith("Repro: (data) two ↘ (property) four")
        message.shouldContain("- (data): replay = Indexes(2L)")
        message.shouldContain("- (property): replay = Cases(seed = 9L, iter = 4L)")
    }

    test("group frames appear in the path but produce no replay-argument line") {
        val frames = listOf(
            MatrixReplayFrame.Group("outer group"),
            MatrixReplayFrame.Data("d", index = 2L, rowName = "two"),
            MatrixReplayFrame.Group("leaf"),
        )
        val message = frames.message(prefix = "Repro:")
        // full path includes the structural groups...
        message.shouldStartWith("Repro: outer group ↘ (data) d: two ↘ leaf")
        // ...but only the replayable layer contributes a detail line
        message.shouldContain("- (data) d: replay = Indexes(2L)")
        message.shouldNotContain("outer group:")
        message.shouldNotContain("- leaf")
    }

    test("withMatrixReplay on empty frames returns the original error unchanged") {
        val original = AssertionError("boom")
        original.withMatrixReplay(emptyList()) shouldBe original
    }

    test("withMatrixReplay on a group-only path (nothing to replay) returns the original unchanged") {
        val original = AssertionError("boom")
        original.withMatrixReplay(listOf(MatrixReplayFrame.Group("a"), MatrixReplayFrame.Group("b"))) shouldBe original
    }

    test("withMatrixReplay preserves the original as the cause and keeps its message") {
        val original = AssertionError("boom")
        val wrapped = original.withMatrixReplay(listOf(MatrixReplayFrame.Data("d", index = 0L, rowName = "0: x")))
        wrapped.cause shouldBe original
        wrapped.message!!.shouldContain("boom")
        wrapped.message!!.shouldContain("- (data) d: replay = Indexes(0L)")
    }

    test("withMatrixReplay is idempotent on an already-wrapped assertion") {
        val frames = listOf(MatrixReplayFrame.Property("p", seed = 1L, iteration = 0L, rowName = "0: x"))
        val wrapped = AssertionError("boom").withMatrixReplay(frames)
        wrapped.withMatrixReplay(frames) shouldBe wrapped
    }
}

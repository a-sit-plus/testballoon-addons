package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith

/** Rendering of the "Error replay info" block and the assertion-wrapping helper. */
val MatrixReplayMessageTest by testSuite {

    test("an empty replay path renders just the header") {
        emptyList<MatrixReplayFrame>().message() shouldBe "Error replay info:"
    }

    test("message renders data and property frames with a custom prefix") {
        val frames = listOf(
            MatrixReplayFrame.Data("d", index = 2L, rowName = "two"),
            MatrixReplayFrame.Property("p", seed = 9L, iteration = 4L, rowName = "four"),
        )
        val message = frames.message(prefix = "Repro:")
        message.shouldStartWith("Repro: d: two / p: four")
        message.shouldContain("- d: replayIndex = 2L")
        message.shouldContain("- p: replay = ReplayInput(seed=9L, iteration=4L)")
    }

    test("withMatrixReplay on empty frames returns the original error unchanged") {
        val original = AssertionError("boom")
        original.withMatrixReplay(emptyList()) shouldBe original
    }

    test("withMatrixReplay preserves the original as the cause and keeps its message") {
        val original = AssertionError("boom")
        val wrapped = original.withMatrixReplay(listOf(MatrixReplayFrame.Data("d", index = 0L, rowName = "0: x")))
        wrapped.cause shouldBe original
        wrapped.message!!.shouldContain("boom")
        wrapped.message!!.shouldContain("- d: replayIndex = 0L")
    }

    test("withMatrixReplay is idempotent on an already-wrapped assertion") {
        val frames = listOf(MatrixReplayFrame.Property("p", seed = 1L, iteration = 0L, rowName = "0: x"))
        val wrapped = AssertionError("boom").withMatrixReplay(frames)
        wrapped.withMatrixReplay(frames) shouldBe wrapped
    }
}

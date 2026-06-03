package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.EdgeConfig
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.int
import io.kotest.property.default

/** Unit-level checks of the replay selection helpers (deterministic). */
val MatrixReplayTest by testSuite {

    test("propertyCases replays exactly the recorded iteration with its original value") {
        val seed = 42L
        val full = propertyCases(Arb.int(), 50, RandomSource.seeded(seed), EdgeConfig.default(), replayIteration = null)
            .asSequence().toList()
        val replayed = propertyCases(Arb.int(), 50, RandomSource.seeded(seed), EdgeConfig.default(), replayIteration = 7L)
            .asSequence().toList()

        replayed shouldHaveSize 1
        replayed.single().index shouldBe 7L
        replayed.single().value shouldBe full[7].value
    }

    test("data cases replay exactly the recorded index") {
        val source = IterableDataSource(listOf(10, 20, 30, 40))

        source.cases(replayIndex = null).asSequence().map { it.value }.toList() shouldBe listOf(10, 20, 30, 40)

        val replayed = source.cases(replayIndex = 2L).asSequence().toList()
        replayed shouldHaveSize 1
        replayed.single().index shouldBe 2L
        replayed.single().value shouldBe 30
    }

    test("ReplayInput overrides the standalone seed and supplies the iteration") {
        val config = PropertyLayerConfigBuilder(MatrixSuiteConfigBuilder().build()).apply {
            seed = 1L
        }.build(replay = ReplayInput(seed = 99L, iteration = 3L))

        config.seed shouldBe 99L
        config.replayIteration shouldBe 3L
    }

    test("error replay info records data indexes alongside property frames, independent of nameFn") {
        val frames = listOf(
            MatrixReplayFrame.Property("prop", seed = 7L, iteration = 3L, rowName = "3: x"),
            // a custom nameFn produced a row name that omits the index entirely:
            MatrixReplayFrame.Data("data", index = 5L, rowName = "no-index-here"),
        )

        val message = frames.message()
        message.contains("Error replay info: prop: 3: x / data: no-index-here").shouldBeTrue()
        message.contains("- prop: replay = ReplayInput(seed=7L, iteration=3L)").shouldBeTrue()
        message.contains("- data: replayIndex = 5L").shouldBeTrue()
    }
}

// End-to-end: each suite pins replay to a single case out of 1000. If the selector failed to isolate,
// the other 999 iterations would run and fail `v shouldBe expected`, so a passing suite proves both
// isolation and exact value reproduction — through the real and the compact execution paths.

private fun expectedAt(seed: Long, iteration: Int): Int =
    Arb.int().generate(RandomSource.seeded(seed), EdgeConfig.default()).elementAt(iteration).value

val replayReproductionReal by matrixSuite {
    val expected = expectedAt(seed = 123L, iteration = 4)
    property("p", Arb.int(), iterations = 1000, replay = ReplayInput(seed = 123L, iteration = 4L)) test { v ->
        v shouldBe expected
    }
}

val replayReproductionCompact by matrixSuite {
    val expected = expectedAt(seed = 123L, iteration = 4)
    compact("replayed") - {
        property("p", Arb.int(), iterations = 1000, replay = ReplayInput(seed = 123L, iteration = 4L)) test { v ->
            v shouldBe expected
        }
    }
}

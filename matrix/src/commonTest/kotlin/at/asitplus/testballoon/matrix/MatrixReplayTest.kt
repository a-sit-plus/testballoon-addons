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

    test("propertyCases replays exactly the recorded iteration with its original value and seed") {
        val seed = 42L
        val full = propertyCases(Arb.int(), 50, EdgeConfig.default(), seed = seed, replays = null)
            .asSequence().toList()
        val replayed = propertyCases(Arb.int(), 50, EdgeConfig.default(), seed = null, replays = listOf(ReplayInput(seed, 7L)))
            .asSequence().toList()

        replayed shouldHaveSize 1
        replayed.single().index shouldBe 7L
        replayed.single().value shouldBe full[7].value
        replayed.single().seed shouldBe seed
    }

    test("propertyCases can replay multiple iterations of one seed") {
        val seed = 42L
        val full = propertyCases(Arb.int(), 50, EdgeConfig.default(), seed = seed, replays = null)
            .asSequence().toList()
        val replayed = propertyCases(Arb.int(), 50, EdgeConfig.default(), seed = null, replays = listOf(ReplayInput(seed, listOf(3L, 7L))))
            .asSequence().toList()

        replayed.map { it.index } shouldBe listOf(3L, 7L)
        replayed.map { it.value } shouldBe listOf(full[3].value, full[7].value)
    }

    test("propertyCases can replay multiple seed+iteration pairs at once") {
        val replayed = propertyCases(
            Arb.int(), 50, EdgeConfig.default(), seed = null,
            replays = listOf(ReplayInput(1L, listOf(2L)), ReplayInput(2L, listOf(5L))),
        ).asSequence().toList()

        replayed.map { it.index } shouldBe listOf(2L, 5L)
        replayed.map { it.seed } shouldBe listOf(1L, 2L)
        replayed[0].value shouldBe Arb.int().generate(RandomSource.seeded(1L), EdgeConfig.default()).elementAt(2).value
        replayed[1].value shouldBe Arb.int().generate(RandomSource.seeded(2L), EdgeConfig.default()).elementAt(5).value
    }

    test("data cases replay exactly the recorded index") {
        val source = IterableDataSource(listOf(10, 20, 30, 40))

        source.cases(replayIndexes = null).asSequence().map { it.value }.toList() shouldBe listOf(10, 20, 30, 40)

        val replayed = source.cases(replayIndexes = listOf(2L)).asSequence().toList()
        replayed shouldHaveSize 1
        replayed.single().index shouldBe 2L
        replayed.single().value shouldBe 30
    }

    test("data cases can replay multiple recorded indexes") {
        val source = IterableDataSource(listOf(10, 20, 30, 40))
        val replayed = source.cases(replayIndexes = listOf(1L, 3L)).asSequence().toList()

        replayed.map { it.index } shouldBe listOf(1L, 3L)
        replayed.map { it.value } shouldBe listOf(20, 40)
    }

    test("property config keeps the standalone seed and the replays independent") {
        val config = PropertyLayerConfigBuilder(MatrixSuiteConfigBuilder().build()).apply {
            seed = 1L
        }.build(replays = listOf(ReplayInput(seed = 99L, iterations = listOf(3L, 7L))))

        config.seed shouldBe 1L  // determinism seed, untouched by replay (each ReplayInput carries its own)
        config.replays shouldBe listOf(ReplayInput(99L, listOf(3L, 7L)))
    }

    test("ReplayInput keeps single iteration constructor for report copy-paste") {
        ReplayInput(seed = 99L, iteration = 3L).iterations shouldBe listOf(3L)
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

// Multiple seed+iteration pairs in one property layer: exactly two cases run (one per ReplayInput).
// A pass proves the DSL threads a List<ReplayInput> through to multi-seed reproduction.
val replayReproductionMultiSeed by matrixSuite {
    val first = expectedAt(seed = 11L, iteration = 2)
    val second = expectedAt(seed = 22L, iteration = 5)
    property(
        "p", Arb.int(), iterations = 1000,
        replays = listOf(ReplayInput(seed = 11L, iteration = 2L), ReplayInput(seed = 22L, iteration = 5L)),
    ) test { v ->
        (v == first || v == second).shouldBeTrue()
    }
}

package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.EdgeConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.default

/** Layer config builders, execution modes, property-case selection, and naming helpers. */
val MatrixLayerConfigTest by testSuite {

    // ----- ExecutionMode -----

    test("concurrent execution defaults to 128-way parallelism") {
        ExecutionMode.Concurrent().parallelism shouldBe 128
    }

    test("concurrent execution keeps a custom parallelism") {
        ExecutionMode.Concurrent(7).parallelism shouldBe 7
    }

    test("concurrent execution rejects non-positive parallelism") {
        shouldThrow<IllegalArgumentException> { ExecutionMode.Concurrent(0) }
        shouldThrow<IllegalArgumentException> { ExecutionMode.Concurrent(-3) }
    }

    test("case limiter is null for sequential and a sized semaphore for concurrent") {
        ExecutionMode.Sequential.caseLimiter().shouldBeNull()
        ExecutionMode.Concurrent(4).caseLimiter()!!.availablePermits shouldBe 4
    }

    test("shared compact concurrency rejects non-positive parallelism and keeps a valid one") {
        shouldThrow<IllegalArgumentException> { CompactConcurrency.Shared(0) }
        CompactConcurrency.Shared(3).parallelism shouldBe 3
    }

    // ----- Input -----

    test("Input single-iteration constructor wraps into a list") {
        Input(5L, 3L) shouldBe Input(5L, listOf(3L))
    }

    test("Input is a value type") {
        Input(5L, listOf(1L, 2L)) shouldBe Input(5L, listOf(1L, 2L))
    }

    // ----- Data / Property layer config inheritance & overrides -----

    test("data layer config inherits parent execution and name length") {
        val parent = MatrixSuiteConfigBuilder().apply {
            execution = ExecutionMode.Concurrent(9)
            defaultTestNameMaxLength = 42
        }.build()
        val cfg = DataLayerConfigBuilder(parent).build(null)
        cfg.execution shouldBe ExecutionMode.Concurrent(9)
        cfg.nameMaxLength shouldBe 42
        cfg.replayIndexes.shouldBeNull()
    }

    test("data layer config overrides the parent") {
        val parent = MatrixSuiteConfigBuilder().apply { execution = ExecutionMode.Concurrent(9) }.build()
        val cfg = DataLayerConfigBuilder(parent).apply {
            execution = ExecutionMode.Sequential
            nameMaxLength = 7
        }.build(listOf(2L))
        cfg.execution shouldBe ExecutionMode.Sequential
        cfg.nameMaxLength shouldBe 7
        cfg.replayIndexes shouldBe listOf(2L)
    }

    test("property layer config inherits execution, defaults edge config, keeps seed and replays separate") {
        val parent = MatrixSuiteConfigBuilder().apply { execution = ExecutionMode.Concurrent(2) }.build()
        val cfg = PropertyLayerConfigBuilder(parent).apply { seed = 123L }.build(null)
        cfg.execution shouldBe ExecutionMode.Concurrent(2)
        cfg.seed shouldBe 123L
        cfg.replays.shouldBeNull()
        cfg.edgeConfig shouldBe EdgeConfig.default()
    }

    test("suite config builder applies explicit overrides") {
        val cfg = MatrixSuiteConfigBuilder().apply {
            execution = ExecutionMode.Concurrent(3)
            defaultPropertyIterations = 17
            defaultTestNameMaxLength = 9
            defaultCompactReport = CompactReport.SummaryOnly
            defaultCompactConcurrency = CompactConcurrency.Shared(5)
        }.build()
        cfg.execution shouldBe ExecutionMode.Concurrent(3)
        cfg.defaultPropertyIterations shouldBe 17
        cfg.defaultTestNameMaxLength shouldBe 9
        cfg.defaultCompactReport shouldBe CompactReport.SummaryOnly
        cfg.defaultCompactConcurrency shouldBe CompactConcurrency.Shared(5)
    }

    // ----- caseCount -----

    test("property caseCount is the full iteration count without replay") {
        val parent = MatrixSuiteConfigBuilder().build()
        PropertyLayerConfigBuilder(parent).build(null).caseCount(50) shouldBe 50L
    }

    test("property caseCount sums the distinct replay iterations") {
        val parent = MatrixSuiteConfigBuilder().build()
        PropertyLayerConfigBuilder(parent)
            .build(listOf(Input(1L, listOf(2L, 4L, 6L)), Input(2L, listOf(8L))))
            .caseCount(50) shouldBe 4L
    }

    test("data caseCount is the known size without replay and the distinct selection with it") {
        val parent = MatrixSuiteConfigBuilder().build()
        DataLayerConfigBuilder(parent).build(null).caseCount(10L) shouldBe 10L
        DataLayerConfigBuilder(parent).build(null).caseCount(null).shouldBeNull()
        DataLayerConfigBuilder(parent).build(listOf(1L, 1L, 2L)).caseCount(10L) shouldBe 2L
    }

    // ----- propertyCases -----

    test("propertyCases with zero iterations yields nothing") {
        propertyCases(Arb.int(), 0, EdgeConfig.default(), seed = 1L, replays = null)
            .asSequence().toList().shouldBeEmpty()
    }

    test("propertyCases full run yields the requested count tagged with the source seed") {
        val cases = propertyCases(Arb.int(), 5, EdgeConfig.default(), seed = 7L, replays = null).asSequence().toList()
        cases.map { it.index } shouldBe (0L..4L).toList()
        cases.all { it.seed == 7L } shouldBe true
    }

    test("propertyCases default seed is non-null and constant within a run") {
        val cases = propertyCases(Arb.int(), 3, EdgeConfig.default(), seed = null, replays = null).asSequence().toList()
        cases.map { it.seed }.toSet().size shouldBe 1
        cases.first().seed.shouldNotBeNull()
    }

    test("propertyCases replay dedups duplicate iterations") {
        propertyCases(Arb.int(), 10, EdgeConfig.default(), seed = null, replays = listOf(Input(1L, listOf(3L, 3L))))
            .asSequence().toList().map { it.index } shouldBe listOf(3L)
    }

    test("propertyCases replay selects matching iterations in ascending order regardless of input order") {
        val full = propertyCases(Arb.int(), 20, EdgeConfig.default(), seed = 9L, replays = null).asSequence().toList()
        val replayed = propertyCases(
            Arb.int(), 20, EdgeConfig.default(), seed = null,
            replays = listOf(Input(9L, listOf(5L, 1L))),
        ).asSequence().toList()
        replayed.map { it.index } shouldBe listOf(1L, 5L)
        replayed.map { it.value } shouldBe listOf(full[1].value, full[5].value)
    }

    // ----- naming helpers -----

    test("default layer name starts with the index") {
        defaultLayerName(2L, "hi").shouldStartWith("2: ")
    }

    test("matrix disabled-name detection keys on a leading bang") {
        isMatrixDisabledName("!disabled") shouldBe true
        isMatrixDisabledName("enabled") shouldBe false
    }
}

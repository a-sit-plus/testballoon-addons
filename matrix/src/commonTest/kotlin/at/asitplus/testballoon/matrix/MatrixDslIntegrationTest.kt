package at.asitplus.testballoon.matrix

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of

// End-to-end DSL behaviour. All suites run Sequential so a trailing assertion test observes the
// fully-accumulated counters (same pattern as `terminalLayerMatrix`). Names avoid "Report" so CI gates them.

val matrixNestedDimensionsTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var leaves = 0
    data("outer", listOf(1, 2, 3)) - {
        data("inner", listOf("a", "b")) test { leaves++ }
    }
    "nested data dimensions produce the cartesian product" {
        leaves shouldBe 6
    }
}

val matrixNestedPropertyAndDataTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var leaves = 0
    property("outer", Arb.of(1, 2), iterations = 2) - {
        data("inner", listOf(10, 20, 30)) test { leaves++ }
    }
    "property over data multiplies cases" {
        leaves shouldBe 6
    }
}

val matrixSequenceLimitTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var count = 0
    data("seq", sequenceOf(1, 2, 3, 4, 5), limit = 2) test { count++ }
    "sequence limit caps the number of cases" {
        count shouldBe 2
    }
}

// NOTE / finding: an *empty* data layer used as a terminal `test` registers a childless suite, which
// testBalloon rejects at discovery ("does not contain any child tests"), breaking the whole module's
// discovery. So no integration suite for empty terminal data layers here — the matrix code may want to
// skip registering an empty layer. Empty-source behaviour is covered at unit level in MatrixDataSourceTest.

val matrixPropertyIterationsTest by matrixSuite(matrixConfig {
        execution = ExecutionMode.Sequential
        defaultPropertyIterations = 3
    }) {
    var seen = 0
    property("uses default", Arb.of(1, 2, 3)) test { seen++ }
    "a property without an explicit count uses the suite default" {
        seen shouldBe 3
    }

    var explicit = 0
    property("explicit", Arb.of(1), iterations = 5) test { explicit++ }
    "an explicit iteration count overrides the default" {
        explicit shouldBe 5
    }
}

val matrixDisabledLayerTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var ran = 0
    data("!disabled", listOf(1, 2)) test { ran++ }
    "a layer disabled by a leading bang runs no cases" {
        ran shouldBe 0
    }
}

val matrixCompactPassingTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    // A compact block whose every case passes must not throw.
    compact("all good") { report = CompactReport.AllCases } - {
        data("d", listOf(1, 2, 3)) test { it shouldBeGreaterThan 0 }
        property("p", Arb.of(2, 4, 6), iterations = 3) test { it shouldBeGreaterThan 0 }
    }
}

val matrixCompactNestedTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    compact("nested", ) { report = CompactReport.SummaryOnly } - {
        "group" - {
            property("p", Arb.of(2, 4, 6), iterations = 3) - { outer ->
                data("d", listOf(outer, outer + 1)) test { it shouldBeGreaterThan 0 }
            }
        }
    }
}

val matrixCompactSharedPassingTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    compact("shared") {
        report = CompactReport.SummaryOnly
        concurrency = CompactConcurrency.Shared(4)
    } - {
        data("outer", (1..10).toList()) - {
            data("inner", (1..10).toList()) test { it shouldBeGreaterThan 0 }
        }
    }
}

// Nameless `data`/`property` overloads: no leading layer name, no intermediate grouping node — the
// per-case nodes are generated directly into the surrounding scope.

val matrixNamelessNestedDataTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var leaves = 0
    data(listOf(1, 2, 3)) - {
        data(listOf("a", "b")) test { leaves++ }
    }
    "nameless nested data dimensions still produce the cartesian product" {
        leaves shouldBe 6
    }
}

val matrixNamelessPropertyOverDataTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var leaves = 0
    property(Arb.of(1, 2), iterations = 2) - {
        data(listOf(10, 20, 30)) test { leaves++ }
    }
    "nameless property over nameless data multiplies cases" {
        leaves shouldBe 6
    }
}

val matrixNamelessSequenceLimitTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var count = 0
    data(sequenceOf(1, 2, 3, 4, 5), limit = 2) test { count++ }
    "nameless sequence limit caps the number of cases" {
        count shouldBe 2
    }
}

val matrixNamelessCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    compact("nameless compact") { report = CompactReport.AllCases } - {
        data(listOf(1, 2, 3)) test { it shouldBeGreaterThan 0 }
        property(Arb.of(2, 4, 6), iterations = 3) - {
            data(listOf(8, 10)) test { it shouldBeGreaterThan 0 }
        }
    }
}

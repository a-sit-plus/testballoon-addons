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

// Map data layers expose each entry as a destructurable `Pair<K, V>`, in iteration order.

val matrixMapDataTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var leaves = 0
    var keySum = ""
    var valueSum = 0
    data("m", linkedMapOf("a" to 1, "b" to 2, "c" to 3)) test { (k, v) ->
        leaves++
        keySum += k
        valueSum += v
    }
    "a map data layer runs one case per entry, exposing key and value" {
        leaves shouldBe 3
        keySum shouldBe "abc"
        valueSum shouldBe 6
    }
}

val matrixNamelessMapDataTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var leaves = 0
    data(linkedMapOf("x" to 10, "y" to 20)) - { (k, _) ->
        data(listOf("p", "q")) test { leaves++ }
    }
    "a nameless map nested over data still multiplies cases" {
        leaves shouldBe 4
    }
}

val matrixMapReplayTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    val ran = mutableListOf<String>()
    data("m", linkedMapOf("a" to 1, "b" to 2, "c" to 3, "d" to 4), replay = Indexes(3L, 0L)) test { (k, _) ->
        ran += k
    }
    "replay indexes pin map entries by position, in source order" {
        ran shouldBe listOf("a", "d")
    }
}

// `.asData(...)`: fluent receiver form with full knob parity (name, limit, replay, config).

val matrixAsDataTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var iterableLeaves = 0
    listOf(1, 2, 3).asData(name = "iterable") test { iterableLeaves++ }
    "Iterable.asData runs one case per element" {
        iterableLeaves shouldBe 3
    }

    var sequenceLeaves = 0
    sequenceOf(1, 2, 3, 4, 5).asData(name = "sequence", limit = 2) test { sequenceLeaves++ }
    "Sequence.asData honours limit" {
        sequenceLeaves shouldBe 2
    }

    var mapLeaves = 0
    linkedMapOf("a" to 1, "b" to 2).asData() test { (_, v) -> mapLeaves += v }
    "Map.asData runs one case per entry, exposing the value" {
        mapLeaves shouldBe 3
    }

    val replayed = mutableListOf<Int>()
    listOf(10, 20, 30, 40).asData(replay = Indexes(0L, 2L)) test { replayed += it }
    "Iterable.asData threads replay through" {
        replayed shouldBe listOf(10, 30)
    }
}

val matrixCompactMapAndAsDataTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    // Same shapes via the CompactScope (`addLayer`) path.
    compact("compact map / asData") { report = CompactReport.AllCases } - {
        data(linkedMapOf("a" to 1, "b" to 2)) test { (_, v) -> v shouldBeGreaterThan 0 }
        listOf(1, 2, 3).asData() test { it shouldBeGreaterThan 0 }
        linkedMapOf("x" to 5).asData(name = "single") test { (_, v) -> v shouldBeGreaterThan 0 }
    }
}

// `nameFn` resolves by lambda arity: a single-param namer names by value (no index); a two-param
// namer keeps the index; omitting it uses the indexed default. The namer is the observable — matrix
// computes each case name eagerly at registration, so a recording namer captures what it received.

val matrixSingleParamNameFnTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    val dataNames = mutableListOf<String>()
    data("d", listOf(10, 20), nameFn = { v -> "v=$v".also { dataNames += it } }) test { }
    "a single-param data namer omits the index" {
        dataNames shouldBe listOf("v=10", "v=20")
    }

    val implicitItNames = mutableListOf<String>()
    data("ii", listOf(1, 2), nameFn = { "n$it".also { s -> implicitItNames += s } }) test { }
    "an implicit-it namer resolves to the single-param form" {
        implicitItNames shouldBe listOf("n1", "n2")
    }

    val mapNames = mutableListOf<String>()
    data("m", linkedMapOf("a" to 1, "b" to 2), nameFn = { (k, v) -> "$k=$v".also { mapNames += it } }) test { }
    "a destructured single-param map namer binds to the value-only Map overload" {
        mapNames shouldBe listOf("a=1", "b=2")
    }

    val asDataNames = mutableListOf<String>()
    listOf("x", "y").asData(nameFn = { "id:$it".also { s -> asDataNames += s } }) test { }
    "a single-param asData namer omits the index" {
        asDataNames shouldBe listOf("id:x", "id:y")
    }

    val propNames = mutableListOf<String>()
    property("p", Arb.of(7), iterations = 2, nameFn = { v -> "p$v".also { propNames += it } }) test { }
    "a single-param property namer omits the index" {
        propNames shouldBe listOf("p7", "p7")
    }

    val indexedNames = mutableListOf<String>()
    data("two", listOf(10, 20), nameFn = { i, v -> "$i:$v".also { indexedNames += it } }) test { }
    "a two-param namer still receives the index" {
        indexedNames shouldBe listOf("0:10", "1:20")
    }
}

val matrixCompactSingleParamNameFnTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    // Value-only namers must thread through the CompactScope (`addLayer`) path too.
    compact("compact single-param namer") { report = CompactReport.AllCases } - {
        data("d", listOf(1, 2), nameFn = { "v$it" }) test { it shouldBeGreaterThan 0 }
        linkedMapOf("a" to 1).asData(nameFn = { (k, _) -> k }) test { (_, v) -> v shouldBeGreaterThan 0 }
    }
}

package at.asitplus.testballoon.matrix

import io.kotest.matchers.shouldBe

// Fixture generators must behave identically whether or not the subtree is compacted. Each suite below
// drives a fixture whose generator mutates a shared counter, runs the dependent tests sequentially, and
// asserts the counter in a trailing test. Compact runs use CompactConcurrency.Layered (the default) so the
// virtual leaves execute inline on one thread — the same sequential, shared-mutable-state observation as
// the real tree. The compact/non-compact pairs assert the same numbers.

// ---- 1) Direct fixture: one fresh value per directly nested terminal test ----

val fixtureDirectNoCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var calls = 0
    val seen = mutableListOf<Int>()
    fixture { ++calls } - {
        "a" { v -> seen += v }
        "b" { v -> seen += v }
        "c" { v -> seen += v }
    }
    "direct fixture runs once per terminal test (no compaction)" {
        calls shouldBe 3
        seen shouldBe listOf(1, 2, 3)
    }
}

val fixtureDirectCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var calls = 0
    val seen = mutableListOf<Int>()
    compact("compacted") { report = CompactReport.SummaryOnly } - {
        fixture { ++calls } - {
            "a" { v -> seen += v }
            "b" { v -> seen += v }
            "c" { v -> seen += v }
        }
    }
    "direct fixture runs once per terminal test (compaction)" {
        calls shouldBe 3
        seen shouldBe listOf(1, 2, 3)
    }
}

// ---- 2) Fixture as a suite: one value shared across its leaves ----

val fixtureSuiteSharedNoCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var calls = 0
    val seen = mutableListOf<Int>()
    fixture { ++calls } - {
        "shared" - { v ->
            "leaf 1" { seen += v }
            "leaf 2" { seen += v }
        }
    }
    "a fixture suite shares one value across its leaves (no compaction)" {
        calls shouldBe 1
        seen shouldBe listOf(1, 1)
    }
}

val fixtureSuiteSharedCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var calls = 0
    val seen = mutableListOf<Int>()
    compact("compacted") { report = CompactReport.SummaryOnly } - {
        fixture { ++calls } - {
            "shared" - { v ->
                "leaf 1" { seen += v }
                "leaf 2" { seen += v }
            }
        }
    }
    "a fixture suite shares one value across its leaves (compaction)" {
        calls shouldBe 1
        seen shouldBe listOf(1, 1)
    }
}

// ---- 3) Nested fixtures across a data layer: outer suite-fixture once, inner test-fixture per leaf ----

val fixtureNestedNoCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var outer = 0
    var inner = 0
    val seen = mutableListOf<Pair<Int, Int>>()
    fixture { ++outer } - {
        "group" - { outerValue ->
            data("d", listOf("x", "y")) - { _ ->
                fixture { ++inner } - {
                    "leaf" { innerValue -> seen += outerValue to innerValue }
                }
            }
        }
    }
    "nested fixtures: outer once, inner per leaf (no compaction)" {
        outer shouldBe 1
        inner shouldBe 2
        seen shouldBe listOf(1 to 1, 1 to 2)
    }
}

val fixtureNestedCompactTest by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var outer = 0
    var inner = 0
    val seen = mutableListOf<Pair<Int, Int>>()
    compact("compacted") { report = CompactReport.SummaryOnly } - {
        fixture { ++outer } - {
            "group" - { outerValue ->
                data("d", listOf("x", "y")) - { _ ->
                    fixture { ++inner } - {
                        "leaf" { innerValue -> seen += outerValue to innerValue }
                    }
                }
            }
        }
    }
    "nested fixtures: outer once, inner per leaf (compaction)" {
        outer shouldBe 1
        inner shouldBe 2
        seen shouldBe listOf(1 to 1, 1 to 2)
    }
}


val fixtureNestedCompactTestDifferentNesting by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var outer = 0
    var inner = 0
    val seen = mutableListOf<Pair<Int, Int>>()
    fixture { ++outer } - {
        "group" - { outerValue ->
            compact("compacted") { report = CompactReport.SummaryOnly } - {
                data("d", listOf("x", "y")) - { _ ->
                    fixture { ++inner } - {
                        "leaf" { innerValue -> seen += outerValue to innerValue }
                    }
                }
            }
        }
    }
    "nested fixtures: outer once, inner per leaf (compaction)" {
        outer shouldBe 1
        inner shouldBe 2
        seen shouldBe listOf(1 to 1, 1 to 2)
    }
}


val fixtureNestedCompactTestInnermostNesting by matrixSuite(matrixConfig { execution = ExecutionMode.Sequential }) {
    var outer = 0
    var inner = 0
    val seen = mutableListOf<Pair<Int, Int>>()
    fixture { ++outer } - {
        "group" - { outerValue ->
            data("d", listOf("x", "y")) - { _ ->
                compact("compacted") { report = CompactReport.SummaryOnly } - {
                    fixture { ++inner } - {
                        "leaf" { innerValue -> seen += outerValue to innerValue }
                    }
                }
            }
        }
    }
    "nested fixtures: outer once, inner per leaf (compaction)" {
        outer shouldBe 1
        inner shouldBe 2
        seen shouldBe listOf(1 to 1, 1 to 2)
    }
}


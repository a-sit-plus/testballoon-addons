package at.asitplus.testballoon.matrix

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds


val matrixReport by matrixSuite(matrixConfig { execution = ExecutionMode.Concurrent(12) }) {
    data("outer", listOf(1, 6, Random.nextBytes(300))) - { num ->
        property("layer 1", Arb.boolean(), iterations = 50) - { bool ->
            compact("whatever") {
                report = when (num) {
                    1 -> CompactReport.AllCases
                    2 -> CompactReport.FailuresOnly
                    else -> CompactReport.SummaryOnly
                }
            } - {
                property("property layer 2", Arb.uLong(), iterations = 10) - { uLong ->
                    data("layer 3", listOf(null, "foo", "bar", "baz")) - { word ->
                        if ((num is Int) && num % 2 == 0) "even" {
                            word shouldBe "foo"
                        } else "odd" - {
                            "terminal" {
                                word.shouldNotBeNull().also {
                                    bool.shouldBeTrue().also {
                                        uLong shouldBeGreaterThan 1uL
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


val matrixCompactReport by matrixSuite {
    compact("foo") - {
        data(List(100) { it }, nameFn = { _, v -> v.toString() }) test {
        }
    }
}

val matrix2Report by matrixSuite {
    data("first", listOf(1, 2, 3, 4, 6), nameFn = { i, v -> "$i: ${v.toHexString()}" }) {
        execution = ExecutionMode.Concurrent(12)
    } - { num ->
        compact("whatever") {
            report = when (num) {
                1 -> CompactReport.AllCases
                2 -> CompactReport.FailuresOnly
                else -> CompactReport.SummaryOnly
            }
        } - {
            data("layer 3", listOf(null, "foo", "bar", "baz")) - { word ->
                if (num % 2 == 0) "even" {
                    word shouldBe "foo"
                } else "odd" - {
                    "terminal" {
                        word.shouldNotBeNull()
                    }
                }
            }
        }
    }

    data("second", listOf(1, 2, 3, 4, 6), nameFn = { i, v -> "$i: ${v.toHexString()}" }) - { num ->
        compact("whatever") {
            report = when (num) {
                1 -> CompactReport.AllCases
                2 -> CompactReport.FailuresOnly
                else -> CompactReport.SummaryOnly
            }
            coroutineContext
        } - {
            data("layer 3", listOf(null, "foo", "bar", "baz", "bonk", "foobar", "barbaz")) {
                execution = ExecutionMode.Concurrent(12)
            } - { word ->
                if (num % 2 == 0) "even" {
                    word shouldBe "foo"
                } else "odd" - {
                    "terminal" {
                        word.shouldNotBeNull()
                    }
                }
            }
        }
    }

    data("third", listOf(1, 2, 3, 4, 6), nameFn = { i, v -> "$i: ${v.toHexString()}" }) - { num ->
        data("layer 3", listOf(null, "foo", "bar", "baz")) { execution = ExecutionMode.Sequential } - { word ->
            if (num % 2 == 0) "even" {
                word shouldBe "foo"
            } else "odd" - {
                "terminal" {
                    word.shouldNotBeNull()
                }
            }
        }
    }
}

val hugeMatrixReport by matrixSuite(matrixConfig { execution = ExecutionMode.Concurrent() }) {
    property("property layer 2", Arb.uLong(), iterations = 100) - { uLong ->
        compact("compacted") {
            report = CompactReport.AllCases
        } - {
            property("layer 3", Arb.byteArray(Arb.int(min = 0, max = 3000), Arb.byte()), iterations = 1000) - { bool ->
                "somethingsomething" {
                    uLong shouldNotBe 0uL
                }
            }
        }
    }
}

val fixtureMatrixReport by matrixSuite(matrixConfig { execution = ExecutionMode.Concurrent() }) {
    property("property layer 1", Arb.uLong(), iterations = 100) - { uLong ->
        fixture { Random.nextBytes(16) } - {
            "pinned randomness 1" - { num ->
                data("data layer 2.1", listOf(null, "foo", "bar", "baz")) - { word ->
                    "contained" {
                        delay(Random.nextInt(1, 100).milliseconds)
                        num.find { it == word?.first()?.code?.toByte() }.shouldNotBeNull()
                    }
                }
                compact("lower layers") { report = CompactReport.AllCases } - {
                    data("data layer 2.2", listOf(null, "foo", "bar", "baz")) - { word ->

                        "contained" {
                            delay(Random.nextInt(1, 100).milliseconds)
                            num.find { it == word?.first()?.code?.toByte() }.shouldNotBeNull()
                        }
                    }
                }
            }

            "pinned randomness 2" - { num ->
                compact("property and data layers") { report = CompactReport.AllCases } - {
                    property("property layer 2", Arb.byte(), iterations = 100) - { byte ->
                        "matches" {
                            delay(Random.nextInt(1, 100).milliseconds)
                            num.find { it == byte }.shouldNotBeNull()
                        }
                    }
                    data("data layer 2", listOf(null, "foo", "bar", "baz")) - { word ->
                        "contained" {
                            delay(Random.nextInt(1, 100).milliseconds)
                            num.find { it == word?.first()?.code?.toByte() }.shouldNotBeNull()
                        }
                    }
                }
            }
        }
    }
}


val combinedFeaturesReport by matrixSuite(matrixConfig { execution = ExecutionMode.Concurrent(12) }) {
    fixture { Random.nextBytes(16) } - {
        "data, properties, fixtures, and compact reports" - { fixture ->
            data("multiplier", listOf(0, 1, 2, 3)) - { multiplier ->
                compact("generated checks") {
                    report = CompactReport.AllCases
                    concurrency = CompactConcurrency.Shared(1)
                } - {
                    property(Arb.int(0..100), iterations = 500) test { offset ->
                        val result = fixture.size * multiplier + offset
                        result shouldBeGreaterThan 0
                    }
                }
            }
        }
    }
}


val showcaseReport by matrixSuite(matrixConfig { execution = ExecutionMode.Concurrent() }) {
    property(
        "first",
        Arb.int(min = 1), iterations = 5, replay = Cases(seed = 5076239242101280184L, iter = 3L)
    ) - { first ->
        property(
            "second",
            Arb.short(min = 1), iterations = 5, replay = Cases(seed = 7770592015648107305L, iter = 2L)
        ) - { second ->
            data(/*nameless third*/
                listOf(1, 2, 3, 4, 5), replay = Indexes(0L)
            ) - { third ->
                property(
                    "fourth",
                    Arb.byte(min = 1),
                    iterations = 5, replay = Cases(seed = 4895172091640526688L, iter = 3L)
                ) test { fourth -> (first / second / third / fourth).shouldBeLessThan(256_000) }
            }
        }
    }
}

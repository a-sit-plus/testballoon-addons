package at.asitplus.testballoon.matrix

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of

val terminalLayerMatrix by matrixSuite(
    execution = ExecutionMode.Sequential,
    defaultPropertyIterations = 2,
) {
    data("terminal iterable data", listOf(1, 2)) test { row ->
        row shouldBeGreaterThan 0
    }

    data(
        "terminal sequence data with config as bare lambda",
        sequenceOf("a", "bb", "ccc", "ddd", "eee"),
        nameFn = { _, data -> "$data" },
        limit = 4
    ) { execution = ExecutionMode.Concurrent() } test { row ->
        row.length shouldBeGreaterThan 0
    }

    data(
        "terminal sequence data with config",
        sequenceOf("a", "bb", "ccc"),
        limit = 2,
        config = { execution = ExecutionMode.Sequential },
    ) test { row ->
        row.length shouldBeGreaterThan 0
    }


    property("terminal property", Arb.of(1, 2), iterations = 2) test { row ->
        row shouldBeGreaterThan 0
    }

    var defaultIterationPropertySeen = 0
    property("terminal property with default iterations", Arb.of(1, 2)) test { row ->
        row shouldBeGreaterThan 0
        defaultIterationPropertySeen++
    }

    "terminal property default iterations use suite config" {
        defaultIterationPropertySeen shouldBe 2
    }

    property(
        "terminal property with config",
        Arb.of("foo", "bar"),
        iterations = 2,
        config = { execution = ExecutionMode.Sequential },
    ) test { row ->
        row.length shouldBe 3
    }

    data("nested data dimension", listOf(1, 2)) - { outer ->
        property("terminal property below data", Arb.of(outer), iterations = 1) test { inner ->
            inner shouldBe outer
        }
    }

    property("nested property dimension", Arb.of(1, 2), iterations = 2) - { outer ->
        data("terminal data below property", listOf(outer)) test { inner ->
            inner shouldBe outer
        }
    }

    fixture { 40 } - {
        "fixture suite with terminal layers" - { fixture ->
            data("terminal fixture data", listOf(1, 2)) test { row ->
                fixture + row shouldBeGreaterThan 40
            }

            property("terminal fixture property", Arb.of(1, 2), iterations = 2) test { row ->
                fixture + row shouldBeGreaterThan 40
            }
        }
    }

    compact("compact terminal layers") {
        report = CompactReport.AllCases
        progressIndicator = Indicator.None
    } - {
        data("terminal compact data", listOf(1, 2)) test { row ->
            row shouldBeGreaterThan 0
        }

        property("terminal compact property", Arb.of(1, 2), iterations = 2) test { row ->
            row shouldBeGreaterThan 0
        }

        data("compact data dimension", listOf(1, 2)) - { outer ->
            property("terminal compact property below data", Arb.of(outer), iterations = 1) test { inner ->
                inner shouldBe outer
            }
        }

        property("compact property dimension", Arb.of(1, 2), iterations = 2) - { outer ->
            data("terminal compact data below property", listOf(outer)) test { inner ->
                inner shouldBe outer
            }
        }

        fixture { 100 } - {
            "compact fixture suite with terminal layers" - { fixture ->
                data("terminal compact fixture data", listOf(1, 2)) test { row ->
                    fixture + row shouldBeGreaterThan 100
                }

                property("terminal compact fixture property", Arb.of(1, 2), iterations = 2) test { row ->
                    fixture + row shouldBeGreaterThan 100
                }
            }
        }
    }
}

package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.TestConfig
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import kotlin.random.Random

private fun explode(): Nothing = throw AssertionError("disabled matrix node executed")

val disabledMatrix by matrixSuite(execution = ExecutionMode.Sequential) {
    test("!disabled explicit test") { explode() }

    "!disabled bare test" { explode() }

    testSuite("!disabled explicit suite") {
        "nested explicit suite test" { explode() }
    }
    "alibi after disabled explicit suite" {}

    "!disabled bare suite" - {
        "nested bare suite test" { explode() }
    }
    "alibi after disabled bare suite" {}

    data("!disabled data layer", listOf(0, 1)) - {
        "nested disabled data layer test" { explode() }
    }
    "alibi after disabled data layer" {}

    property("!disabled property layer", Arb.int(), iterations = 2) - {
        "nested disabled property layer test" { explode() }
    }
    "alibi after disabled property layer" {}

    compact("!disabled compact layer") - {
        "nested disabled compact test" { explode() }
    }
    "alibi after disabled compact layer" {}

    fixtureGenerator { Random.nextInt() } - {
        "!disabled fixture bare test" { explode() }
        "!disabled fixture bare suite" - {
            "nested disabled fixture suite test" { explode() }
        }
        "alibi after disabled fixture bare suite" {}
        test("!disabled fixture explicit test") { explode() }
        testSuite("!disabled fixture explicit suite") {
            "nested disabled fixture explicit suite test" { explode() }
        }
        "alibi after disabled fixture explicit suite" {}
    }

    var explicitTestReached = false
    test("enabled explicit test", testConfig = TestConfig) {
        explicitTestReached shouldBe false
        explicitTestReached = true
    }

    var bareTestReached = false
    "enabled bare test"(testConfig = TestConfig) {
        bareTestReached shouldBe false
        bareTestReached = true
    }

    var explicitSuiteState = true
    testSuite("enabled explicit suite", testConfig = TestConfig) {
        "first explicit suite row" { explicitSuiteState = false }
        "second explicit suite row" { explicitSuiteState shouldBe false }
    }

    var bareSuiteState = true
    "enabled bare suite"(testConfig = TestConfig) - {
        "first bare suite row" { bareSuiteState = false }
        "second bare suite row" { bareSuiteState shouldBe false }
    }

    var dataLayerState = true
    data("enabled data layer", listOf(0, 1)) - { row ->
        "ordered data row $row" {
            if (row == 0) dataLayerState = false else dataLayerState shouldBe false
        }
    }

    var bangNamedDataRows = 0
    data(
        "enabled data layer with bang-named row",
        listOf(0, 1),
        nameFn = { _, row -> if (row == 0) "!bang data row" else "enabled data row" },
    ) - {
        "data row body still runs" {
            bangNamedDataRows++
        }
    }
    "bang-named data rows are not disabled" {
        bangNamedDataRows shouldBe 2
    }

    var propertyLayerState = true
    var propertyLayerSeen = 0
    property("enabled property layer", Arb.of(0, 1), iterations = 2) - {
        "ordered property row" {
            if (propertyLayerSeen == 0) propertyLayerState = false else propertyLayerState shouldBe false
            propertyLayerSeen++
        }
    }

    var bangNamedPropertyRows = 0
    property(
        "enabled property layer with bang-named row",
        Arb.of(0, 1),
        iterations = 2,
        nameFn = { _, row -> if (row == 0) "!bang property row" else "enabled property row" },
    ) - {
        "property row body still runs" {
            bangNamedPropertyRows++
        }
    }
    "bang-named property rows are not disabled" {
        bangNamedPropertyRows shouldBe 2
    }

    compact("enabled compact layer") - {
        "!disabled compact bare test" { explode() }
        test("!disabled compact explicit test") { explode() }

        "!disabled compact bare suite" - {
            "nested disabled compact suite test" { explode() }
        }
        "alibi after disabled compact bare suite" {}
        testSuite("!disabled compact explicit suite") {
            "nested disabled compact explicit suite test" { explode() }
        }
        "alibi after disabled compact explicit suite" {}

        data("!disabled compact data layer", listOf(0, 1)) - {
            "nested disabled compact data test" { explode() }
        }
        "alibi after disabled compact data layer" {}

        property("!disabled compact property layer", Arb.int(), iterations = 2) - {
            "nested disabled compact property test" { explode() }
        }
        "alibi after disabled compact property layer" {}

        fixtureGenerator { Random.nextInt() } - {
            "!disabled compact fixture bare test" { explode() }
            "!disabled compact fixture bare suite" - {
                "nested disabled compact fixture suite test" { explode() }
            }
            "alibi after disabled compact fixture bare suite" {}
            test("!disabled compact fixture explicit test") { explode() }
            testSuite("!disabled compact fixture explicit suite") {
                "nested disabled compact fixture explicit suite test" { explode() }
            }
            "alibi after disabled compact fixture explicit suite" {}
        }

        var compactSuiteState = true
        "enabled compact bare suite" - {
            "first compact suite row" { compactSuiteState = false }
            "second compact suite row" { compactSuiteState shouldBe false }
        }

        var compactDataState = true
        data("enabled compact data layer", listOf(0, 1)) - { row ->
            "ordered compact data row" {
                if (row == 0) compactDataState = false else compactDataState shouldBe false
            }
        }

        var bangNamedCompactDataRows = 0
        data(
            "enabled compact data layer with bang-named row",
            listOf(0, 1),
            nameFn = { _, row -> if (row == 0) "!bang compact data row" else "enabled compact data row" },
        ) - {
            "compact data row body still runs" {
                bangNamedCompactDataRows++
            }
        }
        "bang-named compact data rows are not disabled" {
            bangNamedCompactDataRows shouldBe 2
        }

        var compactPropertyState = true
        var compactPropertySeen = 0
        property("enabled compact property layer", Arb.of(0, 1), iterations = 2) - {
            "ordered compact property row" {
                if (compactPropertySeen == 0) compactPropertyState = false else compactPropertyState shouldBe false
                compactPropertySeen++
            }
        }

        var bangNamedCompactPropertyRows = 0
        property(
            "enabled compact property layer with bang-named row",
            Arb.of(0, 1),
            iterations = 2,
            nameFn = { _, row -> if (row == 0) "!bang compact property row" else "enabled compact property row" },
        ) - {
            "compact property row body still runs" {
                bangNamedCompactPropertyRows++
            }
        }
        "bang-named compact property rows are not disabled" {
            bangNamedCompactPropertyRows shouldBe 2
        }
    }
}

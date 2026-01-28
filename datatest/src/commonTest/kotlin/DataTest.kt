import at.asitplus.testballoon.withData
import at.asitplus.testballoon.withDataSuites
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val aDataDrivenSuite by testSuite {
    withDataSuites("foo" to 13) {
        test("bar") {
            it shouldBe 13
        }
    }
    withData(mapOf("foo" to 13)) {
        it shouldBe 13
    }



    withDataSuites(null, null, 1, 2, 3, 4, compact = true) { number ->
        withData(number.toString(), "one", null, null, null, "two", "three", "four", compact = true) { word ->
            number shouldBe number
            word shouldBe "three"

        }
    }

    //Alternative syntax for withDataSuites
    // -> NOTE the minus ↙↙↙
    withData(1, 2, 3, 4) - { number ->
        withData(null, "one", "two", "three", "four", compact = true) { word ->
            number shouldBe number
            word shouldBe "three"
        }
    }

    withData(null, 1, 2, 3, 4, compact = true) - { number ->
        withData("one", "two", "three", "four") { word ->
            number shouldBe number
            word shouldBe "three"
        }
    }
}

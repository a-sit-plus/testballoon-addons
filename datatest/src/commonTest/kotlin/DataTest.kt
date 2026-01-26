import at.asitplus.testballoon.TestBalloonAddons
import at.asitplus.testballoon.withData
import at.asitplus.testballoon.withDataSuites
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.random.nextUBytes

val aDataDrivenSuite by testSuite {
    withData("foo" to 13) {

    }
    withData(mapOf("foo" to 13)) {
    }



    withDataSuites(null, null, 1, 2, 3, 4, compact = true) { number ->
        withData(number.toString(), "one",null, null, null, "two", "three", "four", compact = true) { word ->
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

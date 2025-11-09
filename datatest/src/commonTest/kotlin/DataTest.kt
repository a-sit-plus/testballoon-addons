import at.asitplus.testballoon.withData
import at.asitplus.testballoon.withDataSuites
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val aDataDrivenSuite by testSuite {
    withDataSuites(1, 2, 3, 4, compact = true) { number ->
        withData("one", "two", "three", "four", compact = true) { word ->
            number shouldBe number
            word shouldBe "three"
        }
    }

    //Alternative syntax for withDataSuites
    // -> NOTE the minus ↙↙↙
    withData(1, 2, 3, 4) - { number ->
        withData("one", "two", "three", "four", compact = true) { word ->
            number shouldBe number
            word shouldBe "three"
        }
    }
}
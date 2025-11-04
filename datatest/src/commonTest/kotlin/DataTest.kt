import at.asitplus.testballoon.withData
import at.asitplus.testballoon.withDataSuites
import de.infix.testBalloon.framework.testSuite
import io.kotest.matchers.shouldBe

val aDataDrivenSuite by testSuite {
    withDataSuites(1, 2, 3, 4) { number ->
        withData("one", "two", "three", "four") { word ->
            number shouldBe number
            word shouldBe "three"
        }
    }
}
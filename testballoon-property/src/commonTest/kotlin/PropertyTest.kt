import at.asitplus.testballoon.checkAll
import at.asitplus.testballoon.checkAllSuites
import de.infix.testBalloon.framework.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.uLong

val propertySuite by testSuite {
    checkAllSuites(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte())) { byteArray ->
        checkAll(iterations = 10, Arb.uLong()) { number ->
            byteArray shouldBe byteArray
            number shouldBe byteArray.size
        }
    }
}
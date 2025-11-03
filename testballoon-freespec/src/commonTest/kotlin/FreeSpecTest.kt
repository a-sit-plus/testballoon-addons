import at.asitplus.testballoon.invoke
import at.asitplus.testballoon.minus
import de.infix.testBalloon.framework.*
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

val aFreeSpecSuite by testSuite {
    "The outermost blue code"(testConfig = TestConfig.singleThreaded()) - {
        "contains some more blue code" - {
            ", some green code inside the lambda" {
                true.shouldBeTrue()
            }
            ", and some more green code inside the second lambda"(testConfig = TestConfig.invocation(TestInvocation.SEQUENTIAL)) {
                1 shouldBe 1
            }
        }
        "And finally some more blue code" - {
            "!With some final green code in this lambda" {
                true.shouldBeFalse()
            }
        }
    }
}
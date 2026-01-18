import at.asitplus.testballoon.invoke
import at.asitplus.testballoon.minus
import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.singleThreaded
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

val aFreeSpecSuite by testSuite {
    "The outermost blue code"(testConfig = TestConfig.singleThreaded()) - {
        "contains some more blue code" - {
            ", some green code inside the lambda" {
                true.shouldBeTrue()
            }
            ", and some more green code inside the second lambda"(testConfig = TestConfig.invocation(TestConfig.Invocation.Sequential)) {
                1 shouldBe 1
            }
        }
        "And finally some more blue code" - {
            "!With some final disabled green code in this lambda" {
                true.shouldBeFalse()
            }
        }
    }
}
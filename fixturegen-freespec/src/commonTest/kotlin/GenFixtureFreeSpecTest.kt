import at.asitplus.testballoon.withFixtureGenerator
import at.asitplus.testballoon.invoke
import at.asitplus.testballoon.minus
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.string.shouldContain
import kotlin.random.Random

val aGeneratingFreeSpecSuite by testSuite {
    withFixtureGenerator { Random.nextBytes(32) } - {

        "A Test with fresh randomness" { freshFixture ->
            freshFixture.toHexString() shouldContain "16"
        }

        repeat(100) {
            "Generated test with fresh randomness" { freshFixture ->
                freshFixture.toHexString() shouldContain "42"
            }
        }

        //✨it ✨just ✨werks ✨
        "Test with implicit fixture name `it`" {
            it.toHexString() shouldContain "26"
        }

        "And we can even nest!" - {
            withFixtureGenerator { Random.nextBytes(16) } - {
                repeat(10) {
                    "pure, high-octane magic going on" {
                        it.toHexString() shouldContain "42"
                    }
                }
            }
        }
    }
}
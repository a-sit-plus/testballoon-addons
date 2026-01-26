import at.asitplus.testballoon.withData
import at.asitplus.testballoon.checkAll
import at.asitplus.testballoon.withFixtureGenerator
import at.asitplus.testballoon.minus
import at.asitplus.testballoon.invoke
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import kotlin.random.Random

val combinedFeaturesSuite by testSuite {

    // Generate a fresh fixture for every test
    withFixtureGenerator { Random.nextInt() } - {

        "A FreeSpec-style suite with generated fixtures" - { freshSeed ->

            // Data-driven tests
            withData(1, 2, 3) { multiplier ->
                "works for simple data-driven cases" {
                    val result = freshSeed * multiplier
                    // assert something about result
                }
            }

            // Property-based tests
            checkAll(iterations = 50, Arb.int(0..10)) { value ->
                "also supports property testing" {
                    val result = freshSeed + value
                    // assert an invariant about result
                }
            }
        }
    }
}

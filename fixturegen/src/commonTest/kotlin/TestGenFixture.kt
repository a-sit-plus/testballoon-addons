import at.asitplus.testballoon.generatingFixtureFor
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.random.Random

val aGeneratingSuite by testSuite {

    //seed the RNG for reproducible tests
    val random = Random(42)

    //reference function to be called for each test inside generatingFixtureFor
    random::nextFloat.generatingFixtureFor {
        repeat(10) {
            test("Generated test with random float") {
                it shouldBeGreaterThan 0.5f /*~50% success rate*/
            }
        }
    }


    //seed before the generator function, not inside!
    val byteRNG = Random(42);
    //We want to test with fresh randomness, so we generate a fresh fixture for each test
    { byteRNG.nextBytes(32) }.generatingFixtureFor {

        repeat(5) {
            test("Generated test with fresh randomness") { freshFixture ->
                freshFixture.toHexString() shouldContain "42" // what are the odds?
            }
        }

        repeat(5) {
            //✨ it ✨ just ✨ werks ✨
            test("Test with implicit fixture name `it`") {
                it.toHexString() shouldContain "26"
            }
        }
    }; //<- semicolon needed, because what follows is a bare lambda


    //always-the-same fixtures also work, of course
    {
        object {
            var a: Int = 1
            val b: Int = 2
        }
    }.generatingFixtureFor {
        test("one") {
            it.a++ //and we can even modify them in one test
            println("a=${it.a}, b=${it.b}") //a=2, b=2
        }
        test("two") {
            //without affecting the other!
            println("a=${it.a}, b=${it.b}") //a=1, b=2
        }
    }


    //Let's test some nasty bug that shows itself only sometimes functionality
    val ageRNG = Random(seed = 26); //<- semicolon needed, because what follows is a bare lambda
    {
        class ABuggyImplementation(val age: Int) {
            fun restrictedAction(): Boolean =
                if (age < 18) false
                else if (age > 18) true
                else Random.nextBoolean() //introduce jitter to simulate a faulty implementation
        }

        //create new object for each test
        ABuggyImplementation(ageRNG.nextInt(0, 99))
    }.generatingFixtureFor {
        repeat(1000) {
            test("Generated test accessing restricted resources") {
                if (it.age == 18) it.restrictedAction() shouldBe true
                else it.restrictedAction() shouldBe false
            }
        }
    }
}

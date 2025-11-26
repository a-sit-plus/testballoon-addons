import at.asitplus.testballoon.withFixtureGenerator
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.delay
import kotlin.random.Random

val firstGeneratingSuite by testSuite {

    //seed the RNG for reproducible tests
    val random = Random(42)

    //reference function to be called for each test inside withFixtureGenerator
    withFixtureGenerator(random::nextFloat) - {
        repeat(10) {
            test("Generated test with random float") {
                it shouldBeGreaterThan 0.5f /*~50% success rate*/
            }
        }

        test("And some other test that des not conform to the shema from the loop") {
            it shouldBeGreaterThan 0f
        }
    }
    //even suspending generators work!
    withFixtureGenerator(suspend { delay(100); Random.nextFloat() }) - {

        repeat(10) {
            test("Generated from suspending generator") {

                it shouldBeGreaterThan 0.5f
            }
        }
    }


    //seed before the generator function, not inside!
    val byteRNG = Random(42);
    //We want to test with fresh randomness, so we generate a fresh fixture for each test
    withFixtureGenerator { byteRNG.nextBytes(32) } - {

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
    }


    //always-the-same fixtures also work, of course
    withFixtureGenerator {
        object {
            var a: Int = 1
            val b: Int = 2
        }
    } - {
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
    val ageRNG = Random(seed = 26)
    withFixtureGenerator {
        class ABuggyImplementation(val age: Int) {
            fun restrictedAction(): Boolean =
                if (age < 18) false
                else if (age > 18) true
                else Random.nextBoolean() //introduce jitter to simulate a faulty implementation
        }

        //create new object for each test
        ABuggyImplementation(ageRNG.nextInt(0, 99))
    } - {

        repeat(1000) {
            test("Generated test accessing restricted resources") {
                if (it.age == 18) it.restrictedAction() shouldBe true
                else it.restrictedAction() shouldBe false
            }
        }
    }
}



val aGeneratingSuite by testSuite {

    //seed before the generator function, not inside!
    val byteRNG = Random(42);
    //We want to test with fresh randomness, so we generate a fresh fixture for each test
    withFixtureGenerator { byteRNG.nextBytes(32) } - {

        repeat(5) {
            test("Generated test with fresh randomness") { freshFixture ->
                //your test logic here
            }
        }

        repeat(5) {
            //✨ it ✨ just ✨ werks ✨
            test("Test with implicit fixture name `it`") {
                //do something with `it`, it contains fresh randomness!
            }
        }
    }


    //seed the RNG for reproducible tests
    val random = Random(42)

    //reference function to be called for each test inside withFixtureGenerator
    withFixtureGenerator(random::nextFloat) - {
        repeat(10) {
            test("Generated test with random float") {
                //test something floaty!
            }
        }
        test("And some other test that des not conform to the shema from the loop") {
            it shouldBeGreaterThan 0f
        }
    }



    //always-the-same fixtures also work, of course
    withFixtureGenerator {
        object {
            var a: Int = 1
            val b: Int = 2
        }
    } - {
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
    val ageRNG = Random(seed = 26)
    withFixtureGenerator {
        class ABuggyImplementation(val age: Int) {
            fun restrictedAction(): Boolean =
                if (age < 18) false
                else if (age > 18) true
                else Random.nextBoolean() //introduce jitter to simulate a faulty implementation
        }

        //create new object for each test
        ABuggyImplementation(ageRNG.nextInt(0, 99))
    } - {
        repeat(1000) {

            test("Generated test accessing restricted resources") {
                //test `restrictedAction` across a wide age range
                //a thousand times to unveil the bug
            }
        }
    }


    withFixtureGenerator { delay(100);3 } -{
        test("3 Test") {
            it shouldBe 3
        }
        testSuite("3 Suite") { int ->
            test("the Test") {
                int shouldBe 3
            }

        }
    }
}

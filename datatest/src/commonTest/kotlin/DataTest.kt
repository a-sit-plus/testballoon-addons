import at.asitplus.testballoon.withData
import at.asitplus.testballoon.withDataSuites
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestConfig.Invocation
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.random.nextUBytes

val aDataDrivenSuite by testSuite(testConfig = TestConfig.testScope(isEnabled = false).invocation(Invocation.Concurrent)) {

    val dummyDataSuites = mutableMapOf<String, Long>().also { map ->
        repeat(100) {
            map[it.toString()] = Random.nextLong()
        }
    }

    val dummyData = mutableMapOf<String, Long>().also { map ->
        repeat(100) {
            map[it.toString()] = Random.nextLong()
        }
    }

    withDataSuites(dummyDataSuites, prefix = "DS") {
        withData(dummyData, prefix = "DT") {
            delay(Random.nextLong(10000))
            it shouldBe it
        }
    }


    withData(Random.nextBytes(2), Random.nextUBytes(20), Random.nextBytes(4), Random.nextBytes(6), prefix = "empty") {

    }


    withDataSuites(null, null, 1, 2, 3, 4, compact = true, prefix = "S nullable") { number ->
        withData(number.toString(), "one", null, null, null, "two", "three", "four", compact = true, prefix = "nullable") { word ->
            number shouldBe number
            word shouldBe "three"

        }
    }

    //Alternative syntax for withDataSuites
    // -> NOTE the minus ↙↙↙
    withData(1, 2, 3, 4, prefix = "alt") - { number ->
        withData(null, "one", "two", "three", "four", compact = true) { word ->
            number shouldBe number
            word shouldBe "three"
        }
    }

    withData(null, 1, 2, 3, 4, compact = true, prefix = "alt") - { number ->
        withData("one", "two", "three", "four") { word ->
            number shouldBe number
            word shouldBe "three"
        }
    }
}

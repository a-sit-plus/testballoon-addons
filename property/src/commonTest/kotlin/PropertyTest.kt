import at.asitplus.testballoon.PropertyTest
import at.asitplus.testballoon.checkAll
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSession.Companion.DefaultConfiguration
import de.infix.testBalloon.framework.core.aroundAll
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.internal.TestBalloonInternalApi
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.uLong
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


val propertySuite by testSuite {
    PropertyTest.compactByDefault = false

    checkAll(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = "first") - { byteArray ->
        checkAll(iterations = 10, Arb.uLong(100u, 200u)) { number ->
            byteArray shouldBe byteArray
            number shouldBe byteArray.size.toULong()
        }
    }

    // this creates test suites
    // --> NOTE THE MINUS HERE >->-->--------------------------------------↘↘↘
    checkAll(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
        checkAll(iterations = 10, Arb.uLong(100u, 200u)) { number ->
            byteArray shouldBe byteArray
            number shouldBe byteArray.size.toULong()
        }
    }

    checkAll(iterations = 5, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
        checkAll(iterations = 5, Arb.uLong(100u, 200u)) { number ->
            byteArray shouldBe byteArray
            number shouldBe byteArray.size.toULong()
        }
        checkAll(iterations = 5, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
            checkAll(iterations = 5, Arb.uLong(100u, 200u)) { number ->
                byteArray shouldBe byteArray
                number shouldBe byteArray.size.toULong()
            }
            checkAll(iterations = 5, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
                checkAll(iterations = 5, Arb.uLong(100u, 200u)) { number ->
                    byteArray shouldBe byteArray
                    number shouldBe byteArray.size.toULong()
                }
            }
        }
    }

}


val compactingSuite by testSuite {
    PropertyTest.compactByDefault = true

    checkAll(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = "first") - { byteArray ->
        checkAll(iterations = 10, Arb.uLong(100u, 200u)) { number ->
            byteArray shouldBe byteArray
            number shouldBe byteArray.size.toULong()
        }
    }

    //Alternative syntax for checkAllSuites
    // --> NOTE THE MINUS HERE >->-->--------------------------------------↘↘↘
    checkAll(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
        checkAll(iterations = 10, Arb.uLong(100u, 200u)) { number ->
            byteArray shouldBe byteArray
            number shouldBe byteArray.size.toULong()
        }
    }

    checkAll(iterations = 5, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
        checkAll(iterations = 5, Arb.uLong(100u, 200u), prefix = "Size Layer 1 ${byteArray.size}") { number ->
            byteArray shouldBe byteArray
            number shouldBe byteArray.size.toULong()
        }
        checkAll(iterations = 5, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
            checkAll(iterations = 5, Arb.uLong(100u, 200u), prefix = "Size Layer 2 ${byteArray.size}") { number ->
                byteArray shouldBe byteArray
                number shouldBe byteArray.size.toULong()
            }
            checkAll(iterations = 5, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
                checkAll(iterations = 5, Arb.uLong(100u, 200u), prefix = "Size Layer 3 ${byteArray.size}") { number ->
                    byteArray shouldBe byteArray
                    number shouldBe byteArray.size.toULong()
                }
            }
        }
    }

}

@OptIn(TestBalloonInternalApi::class)
fun TestConfig.timed() = aroundAll { action ->
    val start = Clock.System.now()
    action()
    val duration = Clock.System.now() - start


    delay(1.seconds)

    error("TIME: $testElementPath took $duration.")

}

@OptIn(ExperimentalUuidApi::class)
val iterationsTest by testSuite(
    testConfig = DefaultConfiguration.invocation(TestConfig.Invocation.Concurrent).timed()
) {
    PropertyTest.compactByDefault = true
    val prefix = Uuid.random().toHexDashString() + Uuid.random().toHexDashString() + Uuid.random().toHexDashString()
    val factor = 33
    checkAll(iterations = factor, Arb.int(), prefix = prefix) - { seven ->
        checkAll(iterations = factor, Arb.int(), prefix = prefix) - { eight ->
            checkAll(iterations = factor, Arb.int(), prefix = prefix) - { nine ->
                checkAll(iterations = factor, Arb.int(), prefix = prefix) { ten ->
                    ten shouldNotBe ten
                }
            }
        }
    }
}

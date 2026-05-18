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


@OptIn(ExperimentalUuidApi::class)
val iterationsTest by testSuite(
    testConfig = DefaultConfiguration.invocation(TestConfig.Invocation.Concurrent)
) {
    PropertyTest.compactByDefault = true
    val prefix = Uuid.random().toHexDashString() + Uuid.random().toHexDashString() + Uuid.random().toHexDashString()
    val factor = 10
    checkAll(iterations = factor, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = prefix) - { seven ->
        checkAll(iterations = factor, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = prefix) - { eight ->
            checkAll(iterations = factor, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = prefix) - { nine ->
                checkAll(iterations = factor, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = prefix) { ten ->
                    ten shouldNotBe ten
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
val iterations by testSuite(
    testConfig = DefaultConfiguration.invocation(TestConfig.Invocation.Concurrent)
) {
    PropertyTest.compactByDefault = true
    val prefix = Uuid.random().toHexDashString() + Uuid.random().toHexDashString() + Uuid.random().toHexDashString()
    val factor = 100000
        checkAll(iterations = factor, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = prefix, compactConcurrent = true) { ten ->
            ten shouldNotBe  ten
        }
    }


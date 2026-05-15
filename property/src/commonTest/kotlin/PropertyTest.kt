import at.asitplus.testballoon.PropertyTest
import at.asitplus.testballoon.checkAll
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSession.Companion.DefaultConfiguration
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.uLong


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

val iterationsTest by testSuite(testConfig = DefaultConfiguration.invocation(TestConfig.Invocation.Concurrent)) {
    PropertyTest.compactByDefault = true
    val factor = 5
    checkAll(iterations = factor, Arb.int()) - { one->
        checkAll(iterations = factor, Arb.int()) - { two ->
            checkAll(iterations = factor, Arb.int()) - { three ->
                checkAll(iterations = factor, Arb.int()) - { four ->
                    checkAll(iterations = factor, Arb.int()) - { five ->
                        checkAll(iterations = factor, Arb.int()) - { six ->
                            checkAll(iterations = factor, Arb.int()) - { seven ->
                                checkAll(iterations = factor, Arb.int()) - { eight ->
                                    checkAll(iterations = factor, Arb.int()) - { nine ->
                                        checkAll(iterations = factor, Arb.int())  { ten ->
                                            ten shouldBeGreaterThan Int.MAX_VALUE-1
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
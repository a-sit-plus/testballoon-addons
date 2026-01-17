import at.asitplus.testballoon.PropertyTest
import at.asitplus.testballoon.checkAll
import at.asitplus.testballoon.checkAllSuites
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.uLong




val propertySuite by testSuite {
    PropertyTest.compactByDefault = false

    checkAllSuites(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = "first") { byteArray ->
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




val compactingSuite by testSuite {
    PropertyTest.compactByDefault = true

    checkAllSuites(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte()), prefix = "first") { byteArray ->
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
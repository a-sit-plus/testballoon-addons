package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val MatrixConfigTest by testSuite {

    test("global defaults preserve compact suppressed-error setting") {
        val previous = MatrixTestDefaults.defaultCompactAddSuppressedErrors
        try {
            TestConfig.MatrixTestDefaults {
                defaultCompactAddSuppressedErrors = false
            }

            MatrixTestDefaults.defaultCompactAddSuppressedErrors shouldBe false
        } finally {
            MatrixTestDefaults.defaultCompactAddSuppressedErrors = previous
        }
    }
}

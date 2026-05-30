package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.freeSpecName
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable

internal fun matrixName(name: String): String = freeSpecName(name)

internal fun isMatrixDisabledName(name: String): Boolean = name.startsWith("!")

internal fun TestConfig.disableByMatrixName(name: String): TestConfig =
    if (isMatrixDisabledName(name)) chainedWith(TestConfig.disable()) else this

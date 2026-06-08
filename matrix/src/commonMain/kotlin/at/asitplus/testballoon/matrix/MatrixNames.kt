package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.freeSpecName
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable

internal fun matrixName(name: String): String = freeSpecName(name)

internal fun isMatrixDisabledName(name: String): Boolean = name.startsWith("!")

internal fun TestConfig.disableByMatrixName(name: String): TestConfig =
    if (isMatrixDisabledName(name)) chainedWith(TestConfig.disable()) else this

/** Message for an element registered while a test body runs — almost always a forgotten enclosing `-`. */
internal fun nestedRegistrationMessage(name: String?): String =
    "Cannot register matrix element ${name?.let { "\"$it\"" } ?: "(unnamed layer)"} while a test is running. " +
        "You likely forgot the '-' that opens an enclosing suite — write \"${name?:"name"}\" - { … } instead of \"${name ?: "name"}\" { … }."

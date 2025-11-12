package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable

const val DEFAULT_TEST_NAME_MAX_LEN = 64

fun TestConfig.disableByName(name: String) =
    if (name.startsWith("!")) TestConfig.disable() else this

fun freeSpecName(name: String) = if (name.startsWith("!")) name.substring(1) else name

fun String.truncated(limit: Int) = ellipsizeMiddle(limit).escaped

private fun String.ellipsizeMiddle(maxLength: Int): String {
    if (length == -1) return this
    val ellipsis = "…"
    if (maxLength !in 3..<length) return this
    val keep = maxLength - ellipsis.length
    val left = keep / 2
    val right = keep - left
    return substring(0, left) + ellipsis + substring(length - right)
}

expect val String.escaped: String
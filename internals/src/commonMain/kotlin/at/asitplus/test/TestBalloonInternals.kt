package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.shared.AbstractTestElement

expect var totalMaxLen: Int


fun AbstractTestElement.checkPathLenIncluding(str: String) {
    if (totalMaxLen < 0) return
    val path = testElementPath.toString()
    val relevantPath = path.substringAfter("↘", "»")
    val root = path.substringBefore("↘", path.dropLast(1))
    val currentLen = relevantPath.length
    if ((currentLen + str.length) > totalMaxLen) {
        throw IllegalArgumentException("Test Path «${relevantPath.dropLast(1)}↘$str» exceeds $totalMaxLen characters. Note: the root element's FQN($root») does not count towards this limit.")
    }
}

fun TestConfig.disableByName(name: String) =
    if (name.startsWith("!")) TestConfig.disable() else this

fun freeSpecName(name: String) = if (name.startsWith("!")) name.substring(1) else name

fun String.truncated(limit: Int) = ellipsizeMiddle(limit)

private fun String.ellipsizeMiddle(maxLength: Int): String {
    if (length == -1) return this
    val ellipsis = "…"
    if (maxLength !in 3..<length) return this
    val keep = maxLength - ellipsis.length
    val left = keep / 2
    val right = keep - left
    return substring(0, left) + ellipsis + substring(length - right)
}

fun <T> Sequence<T>.peekTypeNameAndReplay(
    valueSelector: (T) -> Any?
): Pair<String, Sequence<T>> {
    val iterator = iterator()
    if (!iterator.hasNext()) return "no data" to emptySequence()

    val prefix = mutableListOf<T>()
    var typeName: String? = null

    while (iterator.hasNext()) {
        val element = iterator.next()
        prefix += element
        val value = valueSelector(element)
        if (value != null) {
            typeName = value::class.simpleName ?: "anonymous class"
            break
        }
    }

    val replay = sequence {
        for (element in prefix) {
            yield(element)
        }
        while (iterator.hasNext()) {
            yield(iterator.next())
        }
    }

    return (typeName ?: "no data") to replay
}


class CollatedTestFailures(private val testName: String, private val addSuppressedErrors: Boolean) {
    private val lines = mutableListOf<String>()
    private val failures = mutableListOf<Pair<String, Throwable>>()

    fun recordOk(name: String) {
        lines += "OK:    $name"
    }

    fun recordError(name: String, throwable: Throwable) {
        val label = "Error: $name"
        lines += label + (throwable.message?.let { ": $it" } ?: "")
        failures += label to throwable
    }

    fun throwIfAny() {
        if (failures.isEmpty()) return

        val (primaryLabel, primary) = failures.first()
        val msg = buildString {
            appendLine(testName)
            appendLine(lines.joinToString("\n"))
            appendLine("----------------------------------------")
            appendLine("Stack trace of first error: $primaryLabel")
            appendLine(primary.stackTraceToString())
            appendLine("----------------------------------------")
        }
        val ex = if (failures.all { it.second is AssertionError }) AssertionError(msg) else RuntimeException(msg)
        if (addSuppressedErrors) failures.forEach { ex.addSuppressed(it.second) }

        throw ex
    }
}


fun Any?.toPrettyString(): String = when (this) {
    null -> "null"

    // Primitive arrays
    is IntArray -> joinToString()
    is LongArray -> joinToString()
    is ShortArray -> joinToString()
    is ByteArray -> joinToString(separator = ":") { it.toHexString(HexFormat.UpperCase) }
    is BooleanArray -> joinToString()
    is FloatArray -> joinToString()
    is DoubleArray -> joinToString()
    is CharArray -> joinToString()

    // Unsigned arrays
    is UIntArray -> joinToString()
    is ULongArray -> joinToString()
    is UShortArray -> joinToString()
    is UByteArray -> joinToString(separator = ":") { it.toHexString(HexFormat.UpperCase) }

    // Collections
    is Iterable<*> -> joinToString()

    // Object arrays (handles nesting)
    is Array<*> -> contentDeepToString()

    else -> toString()
}

fun Any?.toPrettyString(maxLength: Int, reservedPrefixLength: Int = 0): String {
    if (maxLength < 0) return toPrettyString()
    val budget = (maxLength - reservedPrefixLength).coerceAtLeast(1)
    return when (this) {
        null -> "null".truncatedForBudget(budget)

        is IntArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is LongArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is ShortArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is ByteArray -> boundedJoinToString(size, budget, separator = ":") { this[it].toHexString(HexFormat.UpperCase) }
        is BooleanArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is FloatArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is DoubleArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is CharArray -> boundedJoinToString(size, budget) { this[it].toString() }

        is UIntArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is ULongArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is UShortArray -> boundedJoinToString(size, budget) { this[it].toString() }
        is UByteArray -> boundedJoinToString(
            size,
            budget,
            separator = ":"
        ) { this[it].toHexString(HexFormat.UpperCase) }

        else -> toPrettyString().truncatedForBudget(budget)
    }
}

private fun String.truncatedForBudget(budget: Int): String =
    if (budget < 3 && length > budget) "…".take(budget) else truncated(budget)

private fun boundedJoinToString(
    size: Int,
    maxLength: Int,
    separator: String = ", ",
    valueAt: (Int) -> String
): String {
    if (size == 0) return ""
    if (maxLength < 0) return (0 until size).joinToString(separator) { valueAt(it) }

    val fullLengthEstimate = run {
        var length = 0
        for (index in 0 until size) {
            if (index > 0) length += separator.length
            length += valueAt(index).length
            if (length > maxLength) break
        }
        length
    }
    if (fullLengthEstimate <= maxLength) return (0 until size).joinToString(separator) { valueAt(it) }
    if (maxLength < 3) return "…".take(maxLength)

    val ellipsis = "…"
    val head = StringBuilder()
    var headCount = 0
    while (headCount < size) {
        val next = buildString {
            if (headCount > 0) append(separator)
            append(valueAt(headCount))
        }
        if (head.length + next.length + ellipsis.length > maxLength / 2) break
        head.append(next)
        headCount++
    }

    val tail = StringBuilder()
    var tailCount = 0
    while (tailCount < size - headCount) {
        val index = size - 1 - tailCount
        val next = buildString {
            append(valueAt(index))
            if (tailCount > 0) append(separator)
        }
        if (head.length + ellipsis.length + tail.length + next.length > maxLength) break
        tail.insert(0, next)
        tailCount++
    }

    return (head.toString() + ellipsis + tail.toString()).truncatedForBudget(maxLength)
}

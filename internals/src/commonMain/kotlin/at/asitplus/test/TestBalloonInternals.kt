package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.shared.AbstractTestElement

expect var totalMaxLen: Int


fun AbstractTestElement.checkPathLenIncluding(str: String) {
    if (totalMaxLen < 0) return
    if ((testElementPath.toString().length + str.length) > totalMaxLen) {
        val substr = testElementPath.toString().dropLast(1)
        val last = testElementPath.toString().last()
        throw IllegalArgumentException("Test Element ${substr}↘$str$last exceeds $totalMaxLen characters. Note: the default max length is 200 for Android tests")
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


fun collateErrors(
    errors: MutableMap<String, Throwable?>,
    testName: String
) {
    val actualErrors = errors.values.filterNotNull()
    if (actualErrors.isNotEmpty()) {
        val (primaryLabel, primary) = errors.filterValues { it != null }.entries.first()
        val messages = errors.map { (msg, err) -> msg + (err?.let { ": ${it.message}" } ?: "") }.joinToString("\n")
        val msg = buildString {
            appendLine(testName)
            appendLine(messages)
            appendLine("----------------------------------------")
            appendLine("Stack trace of first error: $primaryLabel")
            appendLine(primary!!.stackTraceToString())  // works on all KMP targets
            appendLine("----------------------------------------")
        }
        val ex = (if (actualErrors.count { it is AssertionError } == actualErrors.size) AssertionError(msg)
        else RuntimeException(msg)).also { actualErrors.forEach(it::addSuppressed) }
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
package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
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

fun String.normalizedTestPrefix(): String =
    if (isNotEmpty()) "$this " else ""

inline fun prefixedTestName(prefix: String, name: String): String =
    "$prefix$name"

fun TestSuiteScope.checkedTruncatedName(name: String, maxLength: Int): String =
    name.truncated(maxLength).also { testSuiteInScope.checkPathLenIncluding(it) }

private fun String.ellipsizeMiddle(maxLength: Int): String {
    if (maxLength == -1) return this
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

fun <T> Sequence<T>.compactTestNameAndReplay(
    prefix: String,
    valueSelector: (T) -> Any?
): Pair<String, Sequence<T>> {
    val (compactName, replay) = peekTypeNameAndReplay(valueSelector)
    return prefixedTestName(prefix, "Σ$compactName") to replay
}

fun Any?.typeDisplayName(): String =
    if (this == null) "null" else this::class.simpleName ?: "anonymous class"

private sealed interface CollatedFailure {
    val collatedSummary: String

    class Assertion(
        override val collatedSummary: String,
        message: String,
        cause: Throwable
    ) : AssertionError(message, cause), CollatedFailure

    class Runtime(
        override val collatedSummary: String,
        message: String,
        cause: Throwable
    ) : RuntimeException(message, cause), CollatedFailure
}

fun Throwable.collatedSummary(): String? =
    when (this) {
        is CollatedFailure.Assertion -> collatedSummary
        is CollatedFailure.Runtime -> collatedSummary
        else -> message?.lineSequence()?.firstOrNull { it.isNotBlank() }
    }

fun Throwable.stackTraceForCollatedReport(): String {
    return when (this) {
        is CollatedFailure.Assertion -> cause?.stackTraceToString() ?: stackTraceToString()
        is CollatedFailure.Runtime -> cause?.stackTraceToString() ?: stackTraceToString()
        else -> stackTraceToString()
    }
}

class CollatedTestFailures(private val testName: String, private val addSuppressedErrors: Boolean) {
    private val lines = mutableListOf<String>()
    private val failures = mutableListOf<Pair<String, Throwable>>()

    fun recordOk(name: String) {
        lines += "OK:    $name"
    }

    fun recordError(name: String, throwable: Throwable) {
        val label = "Error: $name"
        lines += label + (throwable.collatedSummary()?.let { ": $it" } ?: "")
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
            appendLine(primary.stackTraceForCollatedReport())
            appendLine("----------------------------------------")
        }
        val firstFailure = failures.first().second
        val ex = if (failures.all { it.second is AssertionError }) {
            CollatedFailure.Assertion(testName, msg, firstFailure)
        } else {
            CollatedFailure.Runtime(testName, msg, firstFailure)
        }
        if (addSuppressedErrors) failures.forEach { ex.addSuppressed(it.second) }

        throw ex
    }
}

class CollatedTestRun(testName: String, addSuppressedErrors: Boolean) {
    private val errors = CollatedTestFailures(testName, addSuppressedErrors)

    fun record(
        name: String,
        result: Result<Unit>,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {}
    ) {
        result.onSuccess {
            onSuccess()
            errors.recordOk(name)
        }.onFailure {
            onFailure()
            errors.recordError(name, it)
        }
    }

    fun throwIfAny() = errors.throwIfAny()
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
    val budget = maxLength - reservedPrefixLength
    val toPrettyString = toPrettyString()
    return when {
        budget <= 0 -> ""
        budget < 3 && toPrettyString.length > budget -> "…"
        else -> toPrettyString.truncated(budget)
    }
}

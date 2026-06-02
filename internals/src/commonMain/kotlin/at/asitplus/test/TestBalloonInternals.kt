package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.shared.AbstractTestElement
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

expect var totalMaxLen: Int

internal expect fun compactProgressPrint(message: String)

private const val PRETTY_BYTE_ARRAY_BYTE_LIMIT = 512

internal var compactProgressHeartbeatInterval = 1.seconds

suspend fun withCompactProgressHeartbeat(
    snapshot: () -> String,
    body: suspend () -> Unit
) = withCompactProgressHeartbeat(compactProgressHeartbeatInterval, snapshot, body)

suspend fun withCompactProgressHeartbeat(
    interval: Duration,
    snapshot: () -> String,
    body: suspend () -> Unit
) = withCompactProgressHeartbeatInternal(interval, { snapshot() }, body)

suspend fun withCompactProgressHeartbeatSuspending(
    interval: Duration,
    snapshot: suspend () -> String,
    body: suspend () -> Unit
) = withCompactProgressHeartbeatInternal(interval, snapshot, body)

private suspend fun withCompactProgressHeartbeatInternal(
    interval: Duration,
    snapshot: suspend () -> String,
    body: suspend () -> Unit
) = coroutineScope {
    val heartbeat = launch {
        while (true) {
            delay(interval)
            compactProgressPrint(snapshot())
        }
    }

    try {
        body()
    } finally {
        heartbeat.cancelAndJoin()
    }
}

class CompactProgressTicker(
    private val interval: Duration,
    private val snapshot: () -> String,
) {
    private var last = TimeSource.Monotonic.markNow()
    var printed = false
        private set

    fun tick() {
        if (printed && last.elapsedNow() < interval) return
        printNow()
    }

    fun printNow() {
        compactProgressPrint(snapshot())
        last = TimeSource.Monotonic.markNow()
        printed = true
    }
}

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

class CollatedTestFailures(
    private val testName: String,
    private val addSuppressedErrors: Boolean,
    private val suppressSuccesses: Boolean = false
) {
    private val lines = StringBuilder()
    private var firstFailureLabel: String? = null
    private var firstFailure: Throwable? = null
    private var allFailuresAreAssertionErrors = true
    private var okCount = 0
    private var errorCount = 0
    private val suppressedFailures = if (addSuppressedErrors) mutableListOf<Throwable>() else null

    fun recordOk(name: String) {
        okCount++
        if (!suppressSuccesses) {
            lines.appendLine("OK:    $name")
        }
    }

    fun recordError(name: String, throwable: Throwable) {
        errorCount++
        val label = "Error: $name"
        lines.appendLine(label + (throwable.collatedSummary()?.let { ": $it" } ?: ""))
        if (firstFailure == null) {
            firstFailureLabel = label
            firstFailure = throwable
        }
        allFailuresAreAssertionErrors = allFailuresAreAssertionErrors && throwable is AssertionError
        suppressedFailures?.add(throwable)
    }

    fun throwIfAny() {
        val primary = firstFailure ?: return
        val primaryLabel = firstFailureLabel!!

        val msg = buildString {
            appendLine(testName)
            appendLine("Summary: $okCount OK, $errorCount failed")
            append(lines)
            appendLine("----------------------------------------")
            appendLine("Stack trace of first error: $primaryLabel")
            appendLine(primary.stackTraceForCollatedReport())
            appendLine("----------------------------------------")
        }
        val ex = if (allFailuresAreAssertionErrors) {
            CollatedFailure.Assertion(testName, msg, primary)
        } else {
            CollatedFailure.Runtime(testName, msg, primary)
        }
        suppressedFailures?.forEach { ex.addSuppressed(it) }

        throw ex
    }
}

class CollatedTestRun(
    testName: String,
    addSuppressedErrors: Boolean,
    suppressSuccesses: Boolean = false
) {
    private val errors = CollatedTestFailures(testName, addSuppressedErrors, suppressSuccesses)

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
    is ByteArray -> toBoundedHexString()
    is BooleanArray -> joinToString()
    is FloatArray -> joinToString()
    is DoubleArray -> joinToString()
    is CharArray -> joinToString()

    // Unsigned arrays
    is UIntArray -> joinToString()
    is ULongArray -> joinToString()
    is UShortArray -> joinToString()
    is UByteArray -> toByteArray().toBoundedHexString()

    // Collections
    is Iterable<*> -> joinToString()

    // Object arrays (handles nesting)
    is Array<*> -> contentDeepToString()

    else -> toString()
}

private fun ByteArray.toBoundedHexString(): String {
    if (size <= PRETTY_BYTE_ARRAY_BYTE_LIMIT) {
        return joinToString(separator = ":") { it.toHexString(HexFormat.UpperCase) }
    }
    val head = PRETTY_BYTE_ARRAY_BYTE_LIMIT / 2
    val tail = PRETTY_BYTE_ARRAY_BYTE_LIMIT - head
    return buildString {
        appendHexRange(this@toBoundedHexString, 0, head)
        append(":…:")
        appendHexRange(this@toBoundedHexString, size - tail, size)
    }
}

private fun StringBuilder.appendHexRange(bytes: ByteArray, start: Int, end: Int) {
    for (index in start until end) {
        if (index > start) append(':')
        append(bytes[index].toHexString(HexFormat.UpperCase))
    }
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

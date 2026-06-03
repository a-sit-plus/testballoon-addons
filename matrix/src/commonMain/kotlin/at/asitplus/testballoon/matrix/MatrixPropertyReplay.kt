package at.asitplus.testballoon.matrix

import at.asitplus.AssertionError
import de.infix.testBalloon.framework.core.Test
import kotlin.AssertionError as AssertionErr

internal data class MatrixPropertyReplayFrame(
    val propertyName: String,
    val seed: Long,
    val iteration: Long,
    val rowName: String,
)

internal fun List<MatrixPropertyReplayFrame>.message(
    prefix: String = "Matrix property replay:",
    firstLineIndent: String = "",
    detailLineIndent: String = "",
): String =
    buildString {
        append(firstLineIndent)
        append(prefix)
        append(" ")
        appendLine(this@message.joinToString(" / ") { frame -> "${frame.propertyName}: ${frame.rowName}" })
        this@message.forEach { frame ->
            append(detailLineIndent)
            append("- ")
            append(frame.propertyName)
            append(": seed=")
            append(frame.seed).append("L")
            append(", iteration=")
            append(frame.iteration)
            appendLine("L")
        }
    }.trimEnd()

internal inline fun AssertionErr.withMatrixPropertyReplay(frames: List<MatrixPropertyReplayFrame>): AssertionErr {
    if (frames.isEmpty() || this is AssertionError) return this
    val original = message
    val replay = frames.message(firstLineIndent = "    ", detailLineIndent = "      ")
    val wrappedMessage = if (original.isNullOrBlank()) replay else "$original\n$replay"
    return AssertionError(wrappedMessage, this)
}

internal suspend inline fun Test.ExecutionScope.withMatrixPropertyReplay(
    frames: List<MatrixPropertyReplayFrame>,
    body: suspend Test.ExecutionScope.() -> Unit,
) {
    try {
        body()
    } catch (e: AssertionErr) {
        throw e.withMatrixPropertyReplay(frames)
    }
}



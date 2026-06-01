package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.Test

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
            append(frame.seed)
            append(", iteration=")
            appendLine(frame.iteration)
        }
    }.trimEnd()

internal inline fun AssertionError.withMatrixPropertyReplay(frames: List<MatrixPropertyReplayFrame>): AssertionError {
    if (frames.isEmpty() || this is MatrixPropertyReplayAssertion) return this
    val original = message
    val replay = frames.message(firstLineIndent = "    ", detailLineIndent = "      ")
    val wrappedMessage = if (original.isNullOrBlank()) replay else "$original\n$replay"
    return MatrixPropertyReplayAssertion(wrappedMessage, this)
}

internal suspend inline fun Test.ExecutionScope.withMatrixPropertyReplay(
    frames: List<MatrixPropertyReplayFrame>,
    body: suspend Test.ExecutionScope.() -> Unit,
) {
    try {
        body()
    } catch (e: AssertionError) {
        throw e.withMatrixPropertyReplay(frames)
    }
}

internal class MatrixPropertyReplayAssertion(
    message: String,
    cause: AssertionError,
) : AssertionError(message, cause)

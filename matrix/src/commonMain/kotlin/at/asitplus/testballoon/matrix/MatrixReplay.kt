package at.asitplus.testballoon.matrix

import at.asitplus.AssertionError
import de.infix.testBalloon.framework.core.Test
import kotlin.AssertionError as AssertionErr

/**
 * One layer's contribution to a failure's reproduction info. [Property] records the seed + iteration
 * needed to regenerate a random value; [Data] records the index of a (deterministic) data case — both
 * are needed because a custom `nameFn` may omit the index from the displayed name.
 */
internal sealed interface MatrixReplayFrame {
    val layerName: String
    val rowName: String

    data class Property(
        override val layerName: String,
        val seed: Long,
        val iteration: Long,
        override val rowName: String,
    ) : MatrixReplayFrame

    data class Data(
        override val layerName: String,
        val index: Long,
        override val rowName: String,
    ) : MatrixReplayFrame
}

internal fun List<MatrixReplayFrame>.message(
    prefix: String = "Error replay info:",
    firstLineIndent: String = "",
    detailLineIndent: String = "",
): String =
    buildString {
        append(firstLineIndent)
        append(prefix)
        append(" ")
        appendLine(this@message.joinToString(" / ") { frame -> "${frame.layerName}: ${frame.rowName}" })
        this@message.forEach { frame ->
            append(detailLineIndent)
            append("- ")
            append(frame.layerName)
            when (frame) {
                is MatrixReplayFrame.Property -> {
                    append(": replay = ReplayInput(seed=")
                    append(frame.seed).append("L")
                    append(", iteration=")
                    append(frame.iteration).append("L)")
                }
                is MatrixReplayFrame.Data -> {
                    append(": replayIndex = ")
                    append(frame.index).append("L")
                }
            }
            appendLine()
        }
    }.trimEnd()

internal inline fun AssertionErr.withMatrixReplay(frames: List<MatrixReplayFrame>): AssertionErr {
    if (frames.isEmpty() || this is AssertionError) return this
    val original = message
    val replay = frames.message(firstLineIndent = "    ", detailLineIndent = "      ")
    val wrappedMessage = if (original.isNullOrBlank()) replay else "$original\n$replay"
    return AssertionError(wrappedMessage, this)
}

internal suspend inline fun Test.ExecutionScope.withMatrixReplay(
    frames: List<MatrixReplayFrame>,
    body: suspend Test.ExecutionScope.() -> Unit,
) {
    try {
        body()
    } catch (e: AssertionErr) {
        throw e.withMatrixReplay(frames)
    }
}



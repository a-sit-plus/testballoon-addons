package at.asitplus.testballoon.matrix

import at.asitplus.AssertionError
import de.infix.testBalloon.framework.core.Test
import kotlin.AssertionError as AssertionErr

/**
 * One layer's contribution to a failure's path / reproduction info. [Group] is a non-replayable structural
 * layer (a suite, a `- { }` group, or a plain `test` leaf) that only carries a path segment. [Property] and
 * [Data] are [Replayable] layers: [Property] records the seed + iteration needed to regenerate a random value,
 * [Data] records the index of a (deterministic) data case — both are needed because a custom `nameFn` may omit
 * the index from the displayed name. The full frame chain mirrors the compact report path; only [Replayable]
 * frames also emit a copy-paste replay-argument line.
 */
internal sealed interface MatrixReplayFrame {
    /** This layer's segment in the human-readable failure path (the report's top line). */
    val pathSegment: String

    /** A structural layer (suite / group / plain `test`) with no replayable value — path segment only. */
    data class Group(val name: String) : MatrixReplayFrame {
        override val pathSegment: String get() = name
    }

    /** A replayable layer (`data` / `property`) that additionally contributes a replay-argument detail line. */
    sealed interface Replayable : MatrixReplayFrame {
        /** The layer's explicit name, or `null` for a nameless layer. */
        val layerName: String?
        val rowName: String

        /** The layer kind, always available because frames are typed (rendered as a `(data)`/`(property)` marker). */
        val kind: String

        /** The replay argument to paste back onto the layer, e.g. `replayIndex = 3L`. */
        val replayArgument: String

        override val pathSegment: String
            get() = if (layerName != null) "($kind) $layerName: $rowName" else "($kind) $rowName"
    }

    data class Property(
        override val layerName: String?,
        val seed: Long,
        val iteration: Long,
        override val rowName: String,
    ) : Replayable {
        override val kind: String get() = "property"
        override val replayArgument: String get() = "replay = ReplayInput(seed=${seed}L, iteration=${iteration}L)"
    }

    data class Data(
        override val layerName: String?,
        val index: Long,
        override val rowName: String,
    ) : Replayable {
        override val kind: String get() = "data"
        override val replayArgument: String get() = "replayIndex = ${index}L"
    }
}

/** Whether this frame chain has anything to replay; group-only chains produce no replay block. */
internal fun List<MatrixReplayFrame>.hasReplayable(): Boolean = any { it is MatrixReplayFrame.Replayable }

internal fun List<MatrixReplayFrame>.message(
    prefix: String = "Error replay info:",
    firstLineIndent: String = "",
    detailLineIndent: String = "",
): String =
    buildString {
        append(firstLineIndent)
        append(prefix)
        append(" ")
        appendLine(this@message.joinToString(" ↘ ") { it.pathSegment })
        this@message.forEach { frame ->
            if (frame !is MatrixReplayFrame.Replayable) return@forEach
            append(detailLineIndent)
            append("- (")
            append(frame.kind)
            append(")")
            if (frame.layerName != null) append(" ${frame.layerName}")
            append(": ")
            append(frame.replayArgument)
            appendLine()
        }
    }.trimEnd()

internal inline fun AssertionErr.withMatrixReplay(frames: List<MatrixReplayFrame>): AssertionErr {
    if (!frames.hasReplayable() || this is AssertionError) return this
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



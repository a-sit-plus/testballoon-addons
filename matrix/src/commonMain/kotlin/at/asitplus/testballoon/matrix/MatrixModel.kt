package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.Test
import io.kotest.property.Gen

internal interface MatrixDataSource<T> {
    val knownSize: Long?
    fun open(): Iterator<T>
}

internal class IterableDataSource<T>(private val iterable: Iterable<T>) : MatrixDataSource<T> {
    override val knownSize: Long? = (iterable as? Collection<*>)?.size?.toLong()
    override fun open(): Iterator<T> = iterable.iterator()
}

internal class SequenceDataSource<T>(
    private val sequence: Sequence<T>,
    private val limit: Long?,
) : MatrixDataSource<T> {
    override val knownSize: Long? = null
    override fun open(): Iterator<T> = sequence.limited(limit).iterator()
}

private fun <T> Sequence<T>.limited(limit: Long?): Sequence<T> = sequence {
    if (limit == null) {
        yieldAll(this@limited)
    } else {
        require(limit >= 0) { "limit must be >= 0" }
        var consumed = 0L
        val iterator = this@limited.iterator()
        while (consumed < limit && iterator.hasNext()) {
            yield(iterator.next())
            consumed++
        }
    }
}

/**
 * What a `data` / `property` layer does with each case value: either expand it into child nodes
 * (a container, `- { }`) or run it as a leaf test (a terminal, `test { }`). Holding this on
 * [VirtualNode.Layer] keeps the container/terminal distinction off the node variant.
 */
internal sealed interface LayerBody {
    class Container(val expand: (Any?) -> List<VirtualNode>) : LayerBody
    class Terminal(val leaf: suspend Test.ExecutionScope.(Any?) -> Unit) : LayerBody
}

internal sealed interface VirtualNode {
    data class Suite(val name: String, val disabled: Boolean, val children: List<VirtualNode>) : VirtualNode
    data class DynamicSuite(
        val name: String,
        val disabled: Boolean,
        val children: suspend () -> List<VirtualNode>,
    ) : VirtualNode

    data class Test(val name: String, val disabled: Boolean, val body: suspend Test.ExecutionScope.() -> Unit) : VirtualNode

    /** A `data` / `property` layer: a [spec] (what kind / how to enumerate) + a [body] (container vs. terminal). */
    data class Layer(
        val name: String?,
        val disabled: Boolean,
        val spec: LayerSpec,
        val nameFn: NameFn<Any?>,
        val body: LayerBody,
    ) : VirtualNode
}

/**
 * The kind-specific identity of a `data` / `property` layer: how its cases are enumerated, which replay frame each
 * case produces, and the per-call-site case counts. Lets registration (real-tree & compact), execution, and replay-frame
 * creation be written once over a single abstraction instead of duplicated per kind.
 *
 * The public config builders/types stay separate (a `LayerSpec` wraps an already-built config; it does not merge them).
 * The three count accessors are intentionally distinct — see their per-kind values, which mirror the pre-existing
 * (and subtly different) choices the code made at each site.
 */
internal sealed interface LayerSpec {
    val defaultName: String
    val execution: ExecutionMode
    val nameMaxLength: Int

    /** Total fed to the real-tree registration heartbeat. */
    val registrationProgressTotal: Long?

    /** Total recorded in each case's [MatrixRegistrationFrame]. */
    val registrationFrameTotal: Long?

    /** Total fed to the compact execution source-case counter. */
    val executionTotal: Long?

    /** A FRESH case iterator each call — preserves property RNG seeding / data re-iteration at the right phase. */
    fun cases(): Iterator<Case<Any?>>

    fun frame(layerName: String?, case: Case<Any?>, rowName: String): MatrixReplayFrame.Replayable

    class Data(val source: MatrixDataSource<Any?>, val config: DataLayerConfig) : LayerSpec {
        override val defaultName: String get() = "data"
        override val execution: ExecutionMode get() = config.execution
        override val nameMaxLength: Int get() = config.nameMaxLength
        override val registrationProgressTotal: Long? get() = source.knownSize
        override val registrationFrameTotal: Long? get() = source.knownSize
        override val executionTotal: Long? get() = config.caseCount(source.knownSize)
        override fun cases(): Iterator<Case<Any?>> = source.cases(config.replayIndexes)
        override fun frame(layerName: String?, case: Case<Any?>, rowName: String): MatrixReplayFrame.Replayable =
            MatrixReplayFrame.Data(layerName, case.index, rowName)
    }

    class Property(val gen: Gen<Any?>, val iterations: Int, val config: PropertyLayerConfig) : LayerSpec {
        override val defaultName: String get() = "property"
        override val execution: ExecutionMode get() = config.execution
        override val nameMaxLength: Int get() = config.nameMaxLength
        override val registrationProgressTotal: Long? get() = config.caseCount(iterations)
        override val registrationFrameTotal: Long? get() = iterations.toLong()
        override val executionTotal: Long? get() = config.caseCount(iterations)
        override fun cases(): Iterator<Case<Any?>> =
            propertyCases(gen, iterations, config.edgeConfig, config.seed, config.replays)
        override fun frame(layerName: String?, case: Case<Any?>, rowName: String): MatrixReplayFrame.Replayable =
            MatrixReplayFrame.Property(layerName, case.seed!!, case.index, rowName)
    }
}

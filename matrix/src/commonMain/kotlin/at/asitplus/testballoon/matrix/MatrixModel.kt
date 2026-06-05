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

internal sealed interface VirtualNode {
    data class Suite(val name: String, val disabled: Boolean, val children: List<VirtualNode>) : VirtualNode
    data class DynamicSuite(
        val name: String,
        val disabled: Boolean,
        val children: suspend () -> List<VirtualNode>,
    ) : VirtualNode

    data class Test(val name: String, val disabled: Boolean, val body: suspend Test.ExecutionScope.() -> Unit) : VirtualNode
    data class Data(
        val name: String?,
        val disabled: Boolean,
        val source: MatrixDataSource<Any?>,
        val nameFn: NameFn<Any?>,
        val layerConfig: DataLayerConfig,
        val body: (Any?) -> List<VirtualNode>,
    ) : VirtualNode

    data class DataTest(
        val name: String?,
        val disabled: Boolean,
        val source: MatrixDataSource<Any?>,
        val nameFn: NameFn<Any?>,
        val layerConfig: DataLayerConfig,
        val body: suspend Test.ExecutionScope.(Any?) -> Unit,
    ) : VirtualNode

    data class Property(
        val name: String?,
        val disabled: Boolean,
        val gen: Gen<Any?>,
        val iterations: Int,
        val nameFn: NameFn<Any?>,
        val layerConfig: PropertyLayerConfig,
        val body: (Any?) -> List<VirtualNode>,
    ) : VirtualNode

    data class PropertyTest(
        val name: String?,
        val disabled: Boolean,
        val gen: Gen<Any?>,
        val iterations: Int,
        val nameFn: NameFn<Any?>,
        val layerConfig: PropertyLayerConfig,
        val body: suspend Test.ExecutionScope.(Any?) -> Unit,
    ) : VirtualNode
}

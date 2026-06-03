package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.Test
import io.kotest.property.Gen

@MatrixTestDsl
class CompactScope internal constructor(
    val matrixConfig: MatrixSuiteConfig,
    val config: CompactConfig,
) {
    internal val nodes: MutableList<VirtualNode> = mutableListOf()

    fun testSuite(name: String, body: CompactScope.() -> Unit) {
        val child = CompactScope(matrixConfig, config)
        child.body()
        nodes += VirtualNode.Suite(matrixName(name), isMatrixDisabledName(name), child.nodes.toList())
    }

    fun test(name: String, body: suspend Test.ExecutionScope.() -> Unit) {
        nodes += VirtualNode.Test(matrixName(name), isMatrixDisabledName(name), body)
    }

    operator fun String.invoke(body: suspend Test.ExecutionScope.() -> Unit) {
        test(this, body)
    }

    infix operator fun String.minus(body: CompactScope.() -> Unit) {
        testSuite(this, body)
    }

    fun <T> data(
        name: String,
        values: Iterable<T>,
        nameFn: NameFn<T> = { index, value -> defaultLayerName(index, value) },
        replayIndex: Long? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): CompactDataLayer<T> = CompactDataLayer(this, name, IterableDataSource(values), nameFn, replayIndex, config)

    fun <T> data(
        name: String,
        values: Sequence<T>,
        limit: Long? = null,
        nameFn: NameFn<T> = { index, value -> defaultLayerName(index, value) },
        replayIndex: Long? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): CompactDataLayer<T> = CompactDataLayer(this, name, SequenceDataSource(values, limit), nameFn, replayIndex, config)

    internal fun <T> dataInternal(
        name: String,
        source: MatrixDataSource<T>,
        nameFn: NameFn<T>,
        replayIndex: Long?,
        configBlock: DataLayerConfigBuilder.() -> Unit,
        body: CompactScope.(T) -> Unit,
    ) {
        val layerConfig = DataLayerConfigBuilder(matrixConfig).apply(configBlock).build(replayIndex)
        nodes += VirtualNode.Data(
            matrixName(name),
            disabled = isMatrixDisabledName(name),
            source = source as MatrixDataSource<Any?>,
            nameFn = nameFn as NameFn<Any?>,
            layerConfig = layerConfig,
            body = { value ->
                val child = CompactScope(matrixConfig.copy(execution = layerConfig.execution), this.config)
                @Suppress("UNCHECKED_CAST")
                (body as CompactScope.(Any?) -> Unit).invoke(child, value)
                child.nodes.toList()
            }
        )
    }

    internal fun <T> dataTestInternal(
        name: String,
        source: MatrixDataSource<T>,
        nameFn: NameFn<T>,
        replayIndex: Long?,
        configBlock: DataLayerConfigBuilder.() -> Unit,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        val layerConfig = DataLayerConfigBuilder(matrixConfig).apply(configBlock).build(replayIndex)
        nodes += VirtualNode.DataTest(
            matrixName(name),
            disabled = isMatrixDisabledName(name),
            source = source as MatrixDataSource<Any?>,
            nameFn = nameFn as NameFn<Any?>,
            layerConfig = layerConfig,
            body = body as suspend Test.ExecutionScope.(Any?) -> Unit,
        )
    }

    fun <T> property(
        name: String,
        gen: Gen<T>,
        iterations: Int = matrixConfig.defaultPropertyIterations,
        nameFn: NameFn<T> = { index, value -> defaultLayerName(index, value) },
        replay: ReplayInput? = null,
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): CompactPropertyLayer<T> = CompactPropertyLayer(this, name, gen, iterations, nameFn, replay, config)

    internal fun <T> propertyInternal(
        name: String,
        gen: Gen<T>,
        iterations: Int,
        nameFn: NameFn<T>,
        replay: ReplayInput?,
        config: PropertyLayerConfigBuilder.() -> Unit,
        body: CompactScope.(T) -> Unit,
    ) {
        require(iterations >= 0) { "iterations must be >= 0" }
        val layerConfig = PropertyLayerConfigBuilder(matrixConfig).apply(config).build(replay)
        nodes += VirtualNode.Property(
            matrixName(name),
            disabled = isMatrixDisabledName(name),
            gen = gen as Gen<Any?>,
            iterations = iterations,
            nameFn = nameFn as NameFn<Any?>,
            layerConfig = layerConfig,
            body = { value ->
                val child = CompactScope(matrixConfig.copy(execution = layerConfig.execution), this.config)
                @Suppress("UNCHECKED_CAST")
                (body as CompactScope.(Any?) -> Unit).invoke(child, value)
                child.nodes.toList()
            }
        )
    }

    internal fun <T> propertyTestInternal(
        name: String,
        gen: Gen<T>,
        iterations: Int,
        nameFn: NameFn<T>,
        replay: ReplayInput?,
        config: PropertyLayerConfigBuilder.() -> Unit,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        require(iterations >= 0) { "iterations must be >= 0" }
        val layerConfig = PropertyLayerConfigBuilder(matrixConfig).apply(config).build(replay)
        nodes += VirtualNode.PropertyTest(
            matrixName(name),
            disabled = isMatrixDisabledName(name),
            gen = gen as Gen<Any?>,
            iterations = iterations,
            nameFn = nameFn as NameFn<Any?>,
            layerConfig = layerConfig,
            body = body as suspend Test.ExecutionScope.(Any?) -> Unit,
        )
    }
}

class CompactDataLayer<T> internal constructor(
    private val scope: CompactScope,
    private val name: String,
    private val source: MatrixDataSource<T>,
    private val nameFn: NameFn<T>,
    private val replayIndex: Long?,
    private val config: DataLayerConfigBuilder.() -> Unit,
) {
    operator fun minus(body: CompactScope.(T) -> Unit) {
        scope.dataInternal(name, source, nameFn, replayIndex, config, body)
    }

    infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) {
        scope.dataTestInternal(name, source, nameFn, replayIndex, config, body)
    }
}

class CompactPropertyLayer<T> internal constructor(
    private val scope: CompactScope,
    private val name: String,
    private val gen: Gen<T>,
    private val iterations: Int,
    private val nameFn: NameFn<T>,
    private val replay: ReplayInput?,
    private val config: PropertyLayerConfigBuilder.() -> Unit,
) {
    operator fun minus(body: CompactScope.(T) -> Unit) {
        scope.propertyInternal(name, gen, iterations, nameFn, replay, config, body)
    }

    infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) {
        scope.propertyTestInternal(name, gen, iterations, nameFn, replay, config, body)
    }
}

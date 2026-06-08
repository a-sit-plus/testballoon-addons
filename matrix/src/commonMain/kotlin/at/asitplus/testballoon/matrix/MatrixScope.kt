package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.shared.TestRegistering
import io.kotest.property.Gen

/**
 * Common surface shared by the real-tree [MatrixSuiteScope] and the virtual [CompactScope]. The `data` /
 * `property` overloads below build a kind-neutral [LayerSpec] and hand it to [dispatchContainer] /
 * [dispatchTerminal], which route to the right scope's registration primitive (`registerLayer` / `addLayer`).
 * So the public overloads live in exactly one place instead of being duplicated across the two scopes.
 */
@MatrixTestDsl
sealed interface MatrixScope<SELF : MatrixScope<SELF>> {
    val matrixConfig: MatrixSuiteConfig

    /**
     * `this` as its own concrete [SELF]. Sound for every member of this sealed hierarchy: each scope declares
     * `MatrixScope<Itself>`, so the runtime type of `this` is always exactly [SELF].
     */
    private fun self(): SELF = @Suppress("UNCHECKED_CAST") (this as SELF)

    // --- `data`: named / nameless × Iterable / Sequence. `replay = Indexes(...)` pins specific case indexes. ---

    @TestRegistering
    fun <T> data(
        name: String,
        values: Iterable<T>,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = DataLayer(self(), name, IterableDataSource(values), nameFn, replay, config)

    @TestRegistering
    fun <T> data(
        name: String,
        values: Sequence<T>,
        limit: Long? = null,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = DataLayer(self(), name, SequenceDataSource(values, limit), nameFn, replay, config)

    fun <T> data(
        values: Iterable<T>,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = DataLayer(self(), null, IterableDataSource(values), nameFn, replay, config)

    fun <T> data(
        values: Sequence<T>,
        limit: Long? = null,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = DataLayer(self(), null, SequenceDataSource(values, limit), nameFn, replay, config)

    // --- `property`: named / nameless. `replay = Cases(seed = .., iter = ..)` pins recorded cases. ---

    @TestRegistering
    fun <T> property(
        name: String,
        gen: Gen<T>,
        iterations: Int = matrixConfig.defaultPropertyIterations,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Cases? = null,
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): PropertyLayer<T, SELF> = PropertyLayer(self(), name, gen, iterations, nameFn, replay, config)

    fun <T> property(
        gen: Gen<T>,
        iterations: Int = matrixConfig.defaultPropertyIterations,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Cases? = null,
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): PropertyLayer<T, SELF> = PropertyLayer(self(), null, gen, iterations, nameFn, replay, config)
}

// --- Layer builders returned by `data` / `property`; `- { }` opens a container, `test { }` a terminal. ---

class DataLayer<T, S : MatrixScope<S>> internal constructor(
    private val scope: S,
    private val name: String?,
    private val source: MatrixDataSource<T>,
    private val nameFn: NameFn<T>,
    private val replay: Indexes?,
    private val config: DataLayerConfigBuilder.() -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    private fun spec(): LayerSpec = LayerSpec.Data(
        source as MatrixDataSource<Any?>,
        DataLayerConfigBuilder(scope.matrixConfig).apply(config).build(replay?.indexes),
    )

    operator fun minus(body: S.(T) -> Unit) = scope.dispatchContainer(name, spec(), nameFn, body)

    infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) = scope.dispatchTerminal(name, spec(), nameFn, body)
}

class PropertyLayer<T, S : MatrixScope<S>> internal constructor(
    private val scope: S,
    private val name: String?,
    private val gen: Gen<T>,
    private val iterations: Int,
    private val nameFn: NameFn<T>,
    private val replay: Cases?,
    private val config: PropertyLayerConfigBuilder.() -> Unit,
) {
    @Suppress("UNCHECKED_CAST")
    private fun spec(): LayerSpec {
        require(iterations >= 0) { "iterations must be >= 0" }
        return LayerSpec.Property(
            gen as Gen<Any?>,
            iterations,
            PropertyLayerConfigBuilder(scope.matrixConfig).apply(config).build(replay?.inputs),
        )
    }

    operator fun minus(body: S.(T) -> Unit) = scope.dispatchContainer(name, spec(), nameFn, body)

    infix fun test(body: suspend Test.ExecutionScope.(T) -> Unit) = scope.dispatchTerminal(name, spec(), nameFn, body)
}

// --- Dispatch: one container + one terminal helper, kind-agnostic via the prebuilt [LayerSpec]. The dimension
//     body and nameFn are cast to the concrete scope's Any?-typed forms; sound because S is exactly this scope. ---

@Suppress("UNCHECKED_CAST")
internal fun <T, S : MatrixScope<S>> S.dispatchContainer(
    name: String?,
    spec: LayerSpec,
    nameFn: NameFn<T>,
    body: S.(T) -> Unit,
) {
    when (this) {
        is MatrixSuiteScope ->
            registerLayer(name, spec, nameFn as NameFn<Any?>, RealLayerBody.Container(body as MatrixSuiteScope.(Any?) -> Unit))
        is CompactScope ->
            addLayer(name, spec, nameFn as NameFn<Any?>, expandChild(spec.execution, body as CompactScope.(Any?) -> Unit))
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T, S : MatrixScope<S>> S.dispatchTerminal(
    name: String?,
    spec: LayerSpec,
    nameFn: NameFn<T>,
    body: suspend Test.ExecutionScope.(T) -> Unit,
) {
    val leaf = body as suspend Test.ExecutionScope.(Any?) -> Unit
    when (this) {
        is MatrixSuiteScope -> registerLayer(name, spec, nameFn as NameFn<Any?>, RealLayerBody.Terminal(leaf))
        is CompactScope -> addLayer(name, spec, nameFn as NameFn<Any?>, LayerBody.Terminal(leaf))
    }
}

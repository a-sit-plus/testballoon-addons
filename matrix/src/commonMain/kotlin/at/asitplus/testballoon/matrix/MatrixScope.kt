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

    // --- `data`: named / nameless × Iterable / Sequence / Map. `replay = Indexes(...)` pins specific case indexes.
    //     A `Map` is exposed case-by-case as a destructurable `Pair<K, V>` (entries materialized in iteration order);
    //     replay indexes are positional, so use an ordered map (the `mapOf` / `linkedMapOf` default) for reproducible
    //     replay. Every collection also reads fluently via the `.asData(...)` receiver form below.
    //
    //     `nameFn` resolves by lambda arity: a single-param namer (`{ v -> }`, `{ it }`, or destructured
    //     `{ (k, v) -> }`) names cases by value alone; the two-param `{ index, value -> }` form keeps the index;
    //     omitting it uses the indexed default (`defaultLayerName` / `defaultMapEntryName`). ---

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
        values: Iterable<T>,
        nameFn: (value: T) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(name, values, { _, v -> nameFn(v) }, replay, config)

    @TestRegistering
    fun <T> data(
        name: String,
        values: Sequence<T>,
        limit: Long? = null,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = DataLayer(self(), name, SequenceDataSource(values, limit), nameFn, replay, config)

    @TestRegistering
    fun <T> data(
        name: String,
        values: Sequence<T>,
        nameFn: (value: T) -> String,
        limit: Long? = null,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(name, values, limit, { _, v -> nameFn(v) }, replay, config)

    @TestRegistering
    fun <K, V> data(
        name: String,
        values: Map<K, V>,
        nameFn: NameFn<Pair<K, V>> = ::defaultMapEntryName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> =
        DataLayer(self(), name, IterableDataSource(values.entries.map { it.toPair() }), nameFn, replay, config)

    @TestRegistering
    fun <K, V> data(
        name: String,
        values: Map<K, V>,
        nameFn: (entry: Pair<K, V>) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> = data(name, values, { _, e -> nameFn(e) }, replay, config)

    fun <T> data(
        values: Iterable<T>,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = DataLayer(self(), null, IterableDataSource(values), nameFn, replay, config)

    fun <T> data(
        values: Iterable<T>,
        nameFn: (value: T) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(values, { _, v -> nameFn(v) }, replay, config)

    fun <T> data(
        values: Sequence<T>,
        limit: Long? = null,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = DataLayer(self(), null, SequenceDataSource(values, limit), nameFn, replay, config)

    fun <T> data(
        values: Sequence<T>,
        nameFn: (value: T) -> String,
        limit: Long? = null,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(values, limit, { _, v -> nameFn(v) }, replay, config)

    fun <K, V> data(
        values: Map<K, V>,
        nameFn: NameFn<Pair<K, V>> = ::defaultMapEntryName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> =
        DataLayer(self(), null, IterableDataSource(values.entries.map { it.toPair() }), nameFn, replay, config)

    fun <K, V> data(
        values: Map<K, V>,
        nameFn: (entry: Pair<K, V>) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> = data(values, { _, e -> nameFn(e) }, replay, config)

    // --- `.asData(...)`: fluent receiver form. Each overload just forwards to the matching `data(...)` above, so
    //     layer construction and the single-param→`NameFn` adaptation live in exactly one place (`data`). The
    //     named/nameless × two-/single-param pairing mirrors `data` and is required for `nameFn` arity resolution:
    //     Kotlin can't accept both `{ v -> }` and `{ i, v -> }` through one parameter. ---

    @TestRegistering
    fun <T> Iterable<T>.asData(
        name: String,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(name, this, nameFn, replay, config)

    @TestRegistering
    fun <T> Iterable<T>.asData(
        name: String,
        nameFn: (value: T) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(name, this, nameFn, replay, config)

    fun <T> Iterable<T>.asData(
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(this, nameFn, replay, config)

    fun <T> Iterable<T>.asData(
        nameFn: (value: T) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(this, nameFn, replay, config)

    @TestRegistering
    fun <T> Sequence<T>.asData(
        name: String,
        limit: Long? = null,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(name, this, limit, nameFn, replay, config)

    @TestRegistering
    fun <T> Sequence<T>.asData(
        name: String,
        nameFn: (value: T) -> String,
        limit: Long? = null,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(name, this, nameFn, limit, replay, config)

    fun <T> Sequence<T>.asData(
        limit: Long? = null,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(this, limit, nameFn, replay, config)

    fun <T> Sequence<T>.asData(
        nameFn: (value: T) -> String,
        limit: Long? = null,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<T, SELF> = data(this, nameFn, limit, replay, config)

    @TestRegistering
    fun <K, V> Map<K, V>.asData(
        name: String,
        nameFn: NameFn<Pair<K, V>> = ::defaultMapEntryName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> = data(name, this, nameFn, replay, config)

    @TestRegistering
    fun <K, V> Map<K, V>.asData(
        name: String,
        nameFn: (entry: Pair<K, V>) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> = data(name, this, nameFn, replay, config)

    fun <K, V> Map<K, V>.asData(
        nameFn: NameFn<Pair<K, V>> = ::defaultMapEntryName,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> = data(this, nameFn, replay, config)

    fun <K, V> Map<K, V>.asData(
        nameFn: (entry: Pair<K, V>) -> String,
        replay: Indexes? = null,
        config: DataLayerConfigBuilder.() -> Unit = {},
    ): DataLayer<Pair<K, V>, SELF> = data(this, nameFn, replay, config)

    // --- `property`: named / nameless. `replay = Cases(seed = .., iter = ..)` pins recorded cases. `nameFn`
    //     resolves by lambda arity as for `data` (single param = value-only, two params = `index, value`). ---

    @TestRegistering
    fun <T> property(
        name: String,
        gen: Gen<T>,
        iterations: Int = matrixConfig.defaultPropertyIterations,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Cases? = null,
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): PropertyLayer<T, SELF> = PropertyLayer(self(), name, gen, iterations, nameFn, replay, config)

    @TestRegistering
    fun <T> property(
        name: String,
        gen: Gen<T>,
        nameFn: (value: T) -> String,
        iterations: Int = matrixConfig.defaultPropertyIterations,
        replay: Cases? = null,
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): PropertyLayer<T, SELF> = property(name, gen, iterations, { _, v -> nameFn(v) }, replay, config)

    fun <T> property(
        gen: Gen<T>,
        iterations: Int = matrixConfig.defaultPropertyIterations,
        nameFn: NameFn<T> = ::defaultLayerName,
        replay: Cases? = null,
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): PropertyLayer<T, SELF> = PropertyLayer(self(), null, gen, iterations, nameFn, replay, config)

    fun <T> property(
        gen: Gen<T>,
        nameFn: (value: T) -> String,
        iterations: Int = matrixConfig.defaultPropertyIterations,
        replay: Cases? = null,
        config: PropertyLayerConfigBuilder.() -> Unit = {},
    ): PropertyLayer<T, SELF> = property(gen, iterations, { _, v -> nameFn(v) }, replay, config)
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

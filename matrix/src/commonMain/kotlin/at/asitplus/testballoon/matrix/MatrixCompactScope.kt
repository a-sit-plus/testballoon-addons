package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.Test

@MatrixTestDsl
class CompactScope internal constructor(
    override val matrixConfig: MatrixSuiteConfig,
    val config: CompactConfig,
) : MatrixScope<CompactScope> {
    internal val nodes: MutableList<VirtualNode> = mutableListOf()

    // True only while this scope's build body runs. Registering an element when it is false means the
    // call happened during a test body (a forgotten `-` on an enclosing `"name"`), which would be lost.
    private var registrationOpen = false

    internal fun building(block: () -> Unit) {
        registrationOpen = true
        try {
            block()
        } finally {
            registrationOpen = false
        }
    }

    internal fun requireOpen(name: String?) = check(registrationOpen) { nestedRegistrationMessage(name) }

    internal fun addDynamicSuite(name: String, children: suspend () -> List<VirtualNode>) {
        requireOpen(name)
        nodes += VirtualNode.DynamicSuite(matrixName(name), isMatrixDisabledName(name), children)
    }

    fun testSuite(name: String, body: CompactScope.() -> Unit) {
        requireOpen(name)
        val child = CompactScope(matrixConfig, config)
        child.building { child.body() }
        nodes += VirtualNode.Suite(matrixName(name), isMatrixDisabledName(name), child.nodes.toList())
    }

    fun test(name: String, body: suspend Test.ExecutionScope.() -> Unit) {
        requireOpen(name)
        nodes += VirtualNode.Test(matrixName(name), isMatrixDisabledName(name), body)
    }

    operator fun String.invoke(body: suspend Test.ExecutionScope.() -> Unit) {
        test(this, body)
    }

    infix operator fun String.minus(body: CompactScope.() -> Unit) {
        testSuite(this, body)
    }

    // A container `- { }` body builds its child nodes by replaying the dimension body into a fresh child
    // scope (whose execution is bound to the layer's). Shared verbatim by data and property containers.
    internal fun expandChild(execution: ExecutionMode, body: CompactScope.(Any?) -> Unit): LayerBody.Container =
        LayerBody.Container { value ->
            val child = CompactScope(matrixConfig.copy(execution = execution), config)
            child.building { body(child, value) }
            child.nodes.toList()
        }

    // The single compact registration primitive: both data and property, container and terminal, land here as a
    // [LayerSpec] (the kind) plus a [LayerBody] (container/terminal). The dispatcher in MatrixScope builds both.
    internal fun addLayer(name: String?, spec: LayerSpec, nameFn: NameFn<Any?>, body: LayerBody) {
        requireOpen(name)
        nodes += VirtualNode.Layer(
            name = name?.let(::matrixName),
            disabled = name?.let(::isMatrixDisabledName) ?: false,
            spec = spec,
            nameFn = nameFn,
            body = body,
        )
    }
}

package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of

// Builds a compact node tree by running [block] inside an open registration window (mirrors what
// compactInternal does), then exposes the resulting nodes for inspection.
private fun built(block: CompactScope.() -> Unit): CompactScope {
    val matrixConfig = MatrixSuiteConfigBuilder().build()
    return CompactScope(matrixConfig, CompactConfigBuilder(matrixConfig).build()).apply { building { block() } }
}

/** The compact planning DSL: how `test`/`data`/`property`/`testSuite` build the virtual node tree. */
val MatrixCompactScopeTest by testSuite {

    test("a bang-prefixed test node is marked disabled") {
        val scope = built {
            test("!skip") {}
            test("run") {}
        }
        scope.nodes.size shouldBe 2
        (scope.nodes[0] as VirtualNode.Test).disabled shouldBe true
        (scope.nodes[1] as VirtualNode.Test).disabled shouldBe false
    }

    test("a data terminal builds a Layer node with a Data spec + terminal body carrying the source size") {
        val scope = built { data("nums", listOf(1, 2, 3)) test {} }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.body.shouldBeInstanceOf<LayerBody.Terminal>()
        node.name shouldBe matrixName("nums")
        node.spec.shouldBeInstanceOf<LayerSpec.Data>().source.knownSize shouldBe 3L
    }

    test("a data container builds a Layer node with a Data spec + container body") {
        val scope = built { data("nums", listOf(1, 2)) - { } }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.spec.shouldBeInstanceOf<LayerSpec.Data>()
        node.body.shouldBeInstanceOf<LayerBody.Container>()
    }

    test("a property terminal builds a Layer node with a Property spec + terminal body and its iteration count") {
        val scope = built { property("p", Arb.of(1, 2), iterations = 7) test {} }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.body.shouldBeInstanceOf<LayerBody.Terminal>()
        node.spec.shouldBeInstanceOf<LayerSpec.Property>().iterations shouldBe 7
    }

    test("a nested suite builds a Suite node with its children") {
        val scope = built {
            testSuite("group") {
                test("a") {}
                test("b") {}
            }
        }
        val suite = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Suite>()
        suite.name shouldBe matrixName("group")
        suite.children.size shouldBe 2
    }

    test("a bang-prefixed suite is disabled") {
        val scope = built { testSuite("!group") { test("a") {} } }
        (scope.nodes.single() as VirtualNode.Suite).disabled shouldBe true
    }

    test("a compact property terminal can pin a replay seed and iteration") {
        val scope = built { property("p", Arb.of(1, 2), iterations = 5, replay = Cases(42L, 3L)) test {} }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.spec.shouldBeInstanceOf<LayerSpec.Property>().config.replays shouldBe listOf(Input(42L, listOf(3L)))
    }

    test("a compact data terminal can pin a replay index") {
        val scope = built { data("d", listOf(1, 2, 3), replay = Indexes(1L)) test {} }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.spec.shouldBeInstanceOf<LayerSpec.Data>().config.replayIndexes shouldBe listOf(1L)
    }

    test("a nameless data terminal builds a Layer node (Data) with a null layer name") {
        val scope = built { data(listOf(1, 2, 3)) test {} }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.name shouldBe null
        node.spec.shouldBeInstanceOf<LayerSpec.Data>().source.knownSize shouldBe 3L
    }

    test("a nameless data container builds a Layer node (Data) with a null layer name") {
        val scope = built { data(listOf(1, 2)) - { } }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.name shouldBe null
        node.spec.shouldBeInstanceOf<LayerSpec.Data>()
    }

    test("a nameless property terminal builds a Layer node (Property) with a null layer name") {
        val scope = built { property(Arb.of(1, 2), iterations = 7) test {} }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.name shouldBe null
        node.spec.shouldBeInstanceOf<LayerSpec.Property>().iterations shouldBe 7
    }

    test("a nameless property container builds a Layer node (Property) with a null layer name") {
        val scope = built { property(Arb.of(1, 2), iterations = 2) - { } }
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Layer>()
        node.name shouldBe null
        node.spec.shouldBeInstanceOf<LayerSpec.Property>()
    }

    test("registering on a compact scope after its build window is closed throws") {
        val scope = built { } // build window now closed
        val error = shouldThrow<IllegalStateException> { scope.test("late") {} }
        error.message!! shouldContain "forgot the '-'"
    }
}

package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.of

private fun compactScope(): CompactScope {
    val matrixConfig = MatrixSuiteConfigBuilder().build()
    return CompactScope(matrixConfig, CompactConfigBuilder(matrixConfig).build())
}

/** The compact planning DSL: how `test`/`data`/`property`/`testSuite` build the virtual node tree. */
val MatrixCompactScopeTest by testSuite {

    test("a bang-prefixed test node is marked disabled") {
        val scope = compactScope()
        scope.test("!skip") {}
        scope.test("run") {}
        scope.nodes.size shouldBe 2
        (scope.nodes[0] as VirtualNode.Test).disabled shouldBe true
        (scope.nodes[1] as VirtualNode.Test).disabled shouldBe false
    }

    test("a data terminal builds a DataTest node carrying the source size") {
        val scope = compactScope()
        scope.data("nums", listOf(1, 2, 3)) test {}
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.DataTest>()
        node.name shouldBe matrixName("nums")
        node.source.knownSize shouldBe 3L
    }

    test("a data container builds a Data node") {
        val scope = compactScope()
        scope.data("nums", listOf(1, 2)) - { }
        scope.nodes.single().shouldBeInstanceOf<VirtualNode.Data>()
    }

    test("a property terminal builds a PropertyTest node with its iteration count") {
        val scope = compactScope()
        scope.property("p", Arb.of(1, 2), iterations = 7) test {}
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.PropertyTest>()
        node.iterations shouldBe 7
    }

    test("a nested suite builds a Suite node with its children") {
        val scope = compactScope()
        scope.testSuite("group") {
            test("a") {}
            test("b") {}
        }
        val suite = scope.nodes.single().shouldBeInstanceOf<VirtualNode.Suite>()
        suite.name shouldBe matrixName("group")
        suite.children.size shouldBe 2
    }

    test("a bang-prefixed suite is disabled") {
        val scope = compactScope()
        scope.testSuite("!group") { test("a") {} }
        (scope.nodes.single() as VirtualNode.Suite).disabled shouldBe true
    }

    test("a compact property terminal can pin a replay seed and iteration") {
        val scope = compactScope()
        scope.property("p", Arb.of(1, 2), iterations = 5, replay = ReplayInput(42L, 3L)) test {}
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.PropertyTest>()
        node.layerConfig.replays shouldBe listOf(ReplayInput(42L, listOf(3L)))
    }

    test("a compact data terminal can pin a replay index") {
        val scope = compactScope()
        scope.data("d", listOf(1, 2, 3), replayIndex = 1L) test {}
        val node = scope.nodes.single().shouldBeInstanceOf<VirtualNode.DataTest>()
        node.layerConfig.replayIndexes shouldBe listOf(1L)
    }
}

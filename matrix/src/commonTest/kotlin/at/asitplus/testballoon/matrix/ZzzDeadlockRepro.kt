package at.asitplus.testballoon.matrix

import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int

// Mirrors kxs SerializationTestRoundTripContracts: Concurrent(8) suite, 12 compacts, each Shared(64) over a
// 1000-iteration property with a CPU-bound (non-suspending) body — to surface the "some compacts finish,
// others hang forever" deadlock.
val ZzzDeadlock by matrixSuite(matrixConfig {
    defaultCompactReport = CompactReport.FailuresOnly
    execution = ExecutionMode.Concurrent(8)
    defaultCompactConcurrency = CompactConcurrency.Shared(64)
    defaultProgressIndicator = Indicator.None
}) {
    "group" - {
        for (i in 1..12) {
            compact("compact $i") - {
                property("v", Arb.int(), iterations = 1000) test { v ->
                    // CPU-bound, non-suspending work (stands in for DER encode/decode).
                    var x = v
                    repeat(5000) { x = (x * 31 + it) xor (x ushr 3) }
                    (x - x) shouldBe 0
                }
            }
        }
    }
}

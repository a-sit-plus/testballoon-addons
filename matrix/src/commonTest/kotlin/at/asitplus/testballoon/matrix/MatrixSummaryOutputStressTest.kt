package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.emitCompactSummary
import kotlinx.coroutines.yield

private const val SUMMARY_STRESS_GROUP_COUNT = 16
private const val SUMMARY_STRESS_BRANCH_COUNT = 8
private const val SUMMARY_STRESS_COMPACT_COUNT = 8
private const val SUMMARY_STRESS_DATA_COUNT = 8
private const val SUMMARY_STRESS_BURST_COUNT = 1_024

val matrixSummaryOutputStressTest by matrixSuite(
    matrixConfig { execution = ExecutionMode.Concurrent() }
) {
    repeat(SUMMARY_STRESS_GROUP_COUNT) { group ->
        testSuite("summary stress group $group") {
            repeat(SUMMARY_STRESS_BRANCH_COUNT) { branch ->
                testSuite("branch $branch") {
                    repeat(SUMMARY_STRESS_COMPACT_COUNT) { compact ->
                        compact("compact $compact") {
                            report = CompactReport.SummaryOnly
                            progressIndicator = Indicator.None
                            concurrency = CompactConcurrency.Shared(1)
                        } - {
                            data("outer", 0..<SUMMARY_STRESS_DATA_COUNT) - {
                                data("inner", 0..<SUMMARY_STRESS_DATA_COUNT) test { yield() }
                            }
                            "summary boundary burst" {
                                repeat(SUMMARY_STRESS_BURST_COUNT) {
                                    emitCompactSummary("summary boundary stress")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds

val MatrixCompactConcurrencyConfigTest by testSuite {

    test("suite default compact concurrency is inherited and compact config can override") {
        val suiteConfig = MatrixSuiteConfigBuilder().apply {
            defaultCompactConcurrency = CompactConcurrency.Shared(3)
        }.build()

        CompactConfigBuilder(suiteConfig).build().concurrency shouldBe CompactConcurrency.Shared(3)
        CompactConfigBuilder(suiteConfig).apply {
            concurrency = CompactConcurrency.Layered
        }.build().concurrency shouldBe CompactConcurrency.Layered
    }
}

val MatrixCompactSharedConcurrencyTest by matrixSuite(
    execution = ExecutionMode.Sequential,
    defaultProgressIndicator = Indicator.None,
) {
    val mutex = Mutex()
    var active = 0
    var maxActive = 0
    var completed = 0

    compact("shared compact concurrency") {
        concurrency = CompactConcurrency.Shared(4)
        report = CompactReport.SummaryOnly
    } - {
        data("outer", (0 until 12).toList()) - {
            data("inner", (0 until 12).toList()) test {
                mutex.withLock {
                    active += 1
                    maxActive = maxOf(maxActive, active)
                }
                delay(5.milliseconds)
                mutex.withLock {
                    active -= 1
                    completed += 1
                }
            }
        }
    }

    "shared compact concurrency bounds active terminal bodies" {
        completed shouldBe 144
        maxActive shouldBeLessThanOrEqualTo 4
    }
}

val MatrixCompactSharedConcurrencyStressTest by testSuite {

    test("shared compact concurrency does not starve a 100x100x100 virtual matrix") {
        val matrixConfig = MatrixSuiteConfigBuilder().apply {
            execution = ExecutionMode.Concurrent(100)
        }.build()
        val layerConfig = DataLayerConfigBuilder(matrixConfig).apply {
            execution = ExecutionMode.Concurrent(100)
        }.build()
        val values = (0 until 100).map { it as Any? }
        val mutex = Mutex()
        var active = 0
        var maxActive = 0
        var completed = 0

        suspend fun doWork(caseId: Int) {
            mutex.withLock {
                active += 1
                maxActive = maxOf(maxActive, active)
            }
            consumeCpu(caseId)
            if (caseId % 10_000 == 0) delay(5.milliseconds)
            mutex.withLock {
                active -= 1
                completed += 1
            }
        }

        val nodes = listOf(
            VirtualNode.Data(
                name = "outer",
                disabled = false,
                source = IterableDataSource(values),
                nameFn = { index, value -> "$index: $value" },
                layerConfig = layerConfig,
            ) { outer ->
                val outerInt = outer as Int
                listOf(
                    VirtualNode.Test("outer work", disabled = false) {
                        doWork(outerInt)
                    },
                    VirtualNode.Data(
                        name = "middle",
                        disabled = false,
                        source = IterableDataSource(values),
                        nameFn = { index, value -> "$index: $value" },
                        layerConfig = layerConfig,
                    ) { middle ->
                        val middleInt = middle as Int
                        listOf(
                            VirtualNode.Test("middle work", disabled = false) {
                                doWork((outerInt * 100) + middleInt)
                            },
                            VirtualNode.DataTest(
                                name = "inner",
                                disabled = false,
                                source = IterableDataSource(values),
                                nameFn = { index, value -> "$index: $value" },
                                layerConfig = layerConfig,
                            ) { inner ->
                                doWork((outerInt * 10_000) + (middleInt * 100) + (inner as Int))
                            }
                        )
                    }
                )
            }
        )

        val compactLimit=1
        val run = CompactRun(
            name = "stress",
            config = CompactConfig(
                concurrency = CompactConcurrency.Shared(compactLimit),
                report = CompactReport.SummaryOnly,
                addSuppressedErrors = false,
                reportRows = 0,
                progressIndicator = Indicator.None,
                coroutineContext = EmptyCoroutineContext,
            ),
        )

        runCompactNodes(nodes, this, run)
        run.throwIfAny()

        completed shouldBe 1_010_100
        maxActive shouldBeLessThanOrEqualTo compactLimit
    }
}

private fun consumeCpu(seed: Int) {
    var value = seed
    repeat(10_000) {
        value = (value * 1_664_525) xor 1_013_904_223
    }
}

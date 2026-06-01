package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.truncated
import at.asitplus.testballoon.stackTraceForCollatedReport
import de.infix.testBalloon.framework.core.Test
import io.kotest.property.RandomSource
import io.kotest.property.Sample
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

private data class CompactFailure(
    val path: List<String>,
    val error: Throwable,
    val replayPath: List<MatrixPropertyReplayFrame>,
)

internal class CompactRun(
    private val name: String,
    val config: CompactConfig,
) {
    private val mutex = Mutex()
    private val successes = mutableListOf<List<String>>()
    private var started = 0
    private var completed = 0
    private var successCount = 0
    private var failureCount = 0
    private var sourceCaseCount = 0L
    private var omittedFailures = 0
    private var omittedSuccesses = 0
    private var firstOmittedReplayPath: List<MatrixPropertyReplayFrame> = emptyList()
    private val failureDetails = mutableListOf<CompactFailure>()

    suspend fun start() = mutex.withLock { started += 1 }

    suspend fun addSourceCases(count: Long?) {
        if (count == null) return
        mutex.withLock {
            sourceCaseCount = if (Long.MAX_VALUE - sourceCaseCount < count) {
                Long.MAX_VALUE
            } else {
                sourceCaseCount + count
            }
        }
    }

    suspend fun success(path: List<String>) = mutex.withLock {
        successCount += 1
        completed += 1
        if (config.report == CompactReport.AllCases) {
            if (withinLimit(successes.size, config.reportRows)) {
                successes += path
            } else {
                omittedSuccesses += 1
            }
        }
    }

    suspend fun failure(
        path: List<String>,
        error: Throwable,
        replayPath: List<MatrixPropertyReplayFrame> = emptyList(),
    ) = mutex.withLock {
        failureCount += 1
        completed += 1
        if (config.report != CompactReport.SummaryOnly && withinLimit(failureDetails.size, config.reportRows)) {
            failureDetails += CompactFailure(path, error, replayPath)
        } else {
            omittedFailures += 1
            if (firstOmittedReplayPath.isEmpty()) firstOmittedReplayPath = replayPath
        }
    }

    suspend fun progressMessage(): String = mutex.withLock {
        buildString {
            append("$name: compact progress: $completed of $started queued completed")
            if (sourceCaseCount > 0) append(" ($sourceCaseCount source cases)")
            append(", $failureCount failed")
            if (omittedFailures > 0 || omittedSuccesses > 0) {
                append(", omitted ")
                appendOmittedCounts(omittedFailures, omittedSuccesses)
            }
        }
    }

    fun throwIfAny() {
        if (failureCount == 0) return
        val report = buildString {
            appendLine("$name: $successCount succeeded, $failureCount failed")
            var renderedRows = 0
            var reportOmittedFailures = omittedFailures
            var reportOmittedSuccesses = omittedSuccesses
            if (config.report != CompactReport.SummaryOnly) {
                val failureRows = rowLimitCapacity(config.reportRows)
                failureDetails.take(failureRows).forEach { failure ->
                    append("Failure: ")
                    append(failure.path.joinToString(" / "))
                    appendFailureError(failure)
                    appendLine()
                    renderedRows += 1
                }
                reportOmittedFailures += (failureDetails.size - failureRows).coerceAtLeast(0)
            }
            if (config.report == CompactReport.AllCases) {
                val remainingRows = rowLimitCapacity(config.reportRows, renderedRows)
                successes.take(remainingRows).forEach { success ->
                    append("OK     : ")
                    appendLine(success.joinToString(" / "))
                    renderedRows += 1
                }
                reportOmittedSuccesses += (successes.size - remainingRows).coerceAtLeast(0)
            }
            if (reportOmittedFailures > 0 || reportOmittedSuccesses > 0) {
                append("... ")
                appendOmittedCounts(reportOmittedFailures, reportOmittedSuccesses)
                appendLine(" omitted from compact report")
            }
            if (firstOmittedReplayPath.isNotEmpty()) {
                appendLine(firstOmittedReplayPath.message("First matrix property replay omitted from row report:"))
            }
            appendLine("----------------------------------------")
            val first = failureDetails.firstOrNull()
            if (first == null) {
                appendLine("Stack traces omitted: all compact failures were omitted from compact report")
            } else {
                appendLine("Stack trace of first error: Failure: ${first.path.joinToString(" / ")}")
                appendLine(first.error.stackTraceForCollatedReport())
            }
            appendLine("----------------------------------------")
        }
        val first = failureDetails.firstOrNull()?.error
        val assertion = if (first == null) AssertionError(report) else AssertionError(report, first)
        if (config.addSuppressedErrors) {
            failureDetails.drop(1).forEach { assertion.addSuppressed(it.error) }
        }
        throw assertion
    }
}

private fun withinLimit(size: Int, limit: Int): Boolean =
    limit < 0 || size < limit

private fun rowLimitCapacity(limit: Int, used: Int = 0): Int =
    if (limit < 0) Int.MAX_VALUE else (limit - used).coerceAtLeast(0)

private fun StringBuilder.appendFailureError(failure: CompactFailure) {
    val message = if (failure.replayPath.isEmpty()) {
        failure.error.message
    } else {
        failure.error.cause?.message ?: failure.error.message
    }
    appendLine("  error: ${failure.error::class.simpleName}: $message")
    if (failure.replayPath.isNotEmpty()) {
        failure.replayPath.message().lines().forEachIndexed { index, line ->
            append(if (index == 0) "    " else "      ")
            appendLine(line)
        }
    }
}

private fun StringBuilder.appendOmittedCounts(failures: Int, successes: Int) {
    append(failures)
    append(" failures and ")
    append(successes)
    append(" OKs")
}

internal suspend fun runVirtualNodes(
    nodes: List<VirtualNode>,
    testScope: Test.ExecutionScope,
    run: CompactRun,
    path: List<String> = emptyList(),
    replayPath: List<MatrixPropertyReplayFrame> = emptyList(),
) {
    for (node in nodes) {
        when (node) {
            is VirtualNode.Suite -> if (!node.disabled) {
                runVirtualNodes(node.children, testScope, run, path + node.name, replayPath)
            }
            is VirtualNode.DynamicSuite -> if (!node.disabled) {
                runVirtualNodes(node.children(), testScope, run, path + node.name, replayPath)
            }
            is VirtualNode.Test -> {
                if (node.disabled) continue
                run.start()
                try {
                    testScope.withMatrixPropertyReplay(replayPath) { node.body(testScope) }
                    run.success(path + node.name)
                } catch (t: AssertionError) {
                    run.failure(path + node.name, t, replayPath)
                } catch (t: Throwable) {
                    run.failure(path + node.name, t)
                }
            }

            is VirtualNode.Data -> if (!node.disabled) runDataNode(node, testScope, run, path, replayPath)
            is VirtualNode.DataTest -> if (!node.disabled) runDataTestNode(node, testScope, run, path, replayPath)
            is VirtualNode.Property -> if (!node.disabled) runPropertyNode(node, testScope, run, path, replayPath)
            is VirtualNode.PropertyTest -> if (!node.disabled) runPropertyTestNode(node, testScope, run, path, replayPath)
        }
    }
}

private suspend fun runDataNode(
    node: VirtualNode.Data,
    testScope: Test.ExecutionScope,
    run: CompactRun,
    path: List<String>,
    replayPath: List<MatrixPropertyReplayFrame>,
) {
    run.addSourceCases(node.source.knownSize)
    val iterator = node.source.open()
    suspend fun runOne(index: Long, value: Any?) {
        val rawName = node.nameFn(index, value).truncated(node.layerConfig.nameMaxLength)
        val name = "${node.name}: $rawName"
        runVirtualNodes(node.body(value), testScope, run, path + name, replayPath)
    }
    when (val execution = node.layerConfig.execution) {
        ExecutionMode.Sequential -> {
            var index = 0L
            while (iterator.hasNext()) {
                runOne(index, iterator.next())
                index++
            }
        }

        is ExecutionMode.Concurrent -> iterator.forEachConcurrentBounded(
            execution.parallelism,
            run.config.coroutineContext,
            ::runOne
        )
    }
}

private suspend fun runDataTestNode(
    node: VirtualNode.DataTest,
    testScope: Test.ExecutionScope,
    run: CompactRun,
    path: List<String>,
    replayPath: List<MatrixPropertyReplayFrame>,
) {
    run.addSourceCases(node.source.knownSize)
    val iterator = node.source.open()
    suspend fun runOne(index: Long, value: Any?) {
        val rawName = node.nameFn(index, value).truncated(node.layerConfig.nameMaxLength)
        val name = "${node.name}: $rawName"
        run.start()
        try {
            testScope.withMatrixPropertyReplay(replayPath) { node.body(testScope, value) }
            run.success(path + name)
        } catch (t: AssertionError) {
            run.failure(path + name, t, replayPath)
        } catch (t: Throwable) {
            run.failure(path + name, t)
        }
    }
    when (val execution = node.layerConfig.execution) {
        ExecutionMode.Sequential -> {
            var index = 0L
            while (iterator.hasNext()) {
                runOne(index, iterator.next())
                index++
            }
        }

        is ExecutionMode.Concurrent -> iterator.forEachConcurrentBounded(
            execution.parallelism,
            run.config.coroutineContext,
            ::runOne
        )
    }
}

private suspend fun runPropertyNode(
    node: VirtualNode.Property,
    testScope: Test.ExecutionScope,
    run: CompactRun,
    path: List<String>,
    replayPath: List<MatrixPropertyReplayFrame>,
) {
    val random = node.layerConfig.seed?.let { RandomSource.seeded(it) } ?: RandomSource.default()
    val seed = random.seed
    run.addSourceCases(node.iterations.toLong())
    val iterator = node.gen.generate(random, node.layerConfig.edgeConfig).take(node.iterations).iterator()
    suspend fun runOne(index: Long, sample: Sample<Any?>) {
        val value = sample.value
        val rawName = node.nameFn(index, value).truncated(node.layerConfig.nameMaxLength)
        val name = "${node.name}: $rawName"
        val frame = MatrixPropertyReplayFrame(node.name, seed, index, rawName)
        runVirtualNodes(node.body(value), testScope, run, path + name, replayPath + frame)
    }
    when (val execution = node.layerConfig.execution) {
        ExecutionMode.Sequential -> {
            var index = 0L
            while (iterator.hasNext()) {
                runOne(index, iterator.next())
                index++
            }
        }

        is ExecutionMode.Concurrent -> iterator.forEachConcurrentBounded(
            execution.parallelism,
            run.config.coroutineContext,
            ::runOne
        )
    }
}

private suspend fun runPropertyTestNode(
    node: VirtualNode.PropertyTest,
    testScope: Test.ExecutionScope,
    run: CompactRun,
    path: List<String>,
    replayPath: List<MatrixPropertyReplayFrame>,
) {
    val random = node.layerConfig.seed?.let { RandomSource.seeded(it) } ?: RandomSource.default()
    val seed = random.seed
    run.addSourceCases(node.iterations.toLong())
    val iterator = node.gen.generate(random, node.layerConfig.edgeConfig).take(node.iterations).iterator()
    suspend fun runOne(index: Long, sample: Sample<Any?>) {
        val value = sample.value
        val rawName = node.nameFn(index, value).truncated(node.layerConfig.nameMaxLength)
        val name = "${node.name}: $rawName"
        val frame = MatrixPropertyReplayFrame(node.name, seed, index, rawName)
        val currentReplayPath = replayPath + frame
        run.start()
        try {
            testScope.withMatrixPropertyReplay(currentReplayPath) { node.body(testScope, value) }
            run.success(path + name)
        } catch (t: AssertionError) {
            run.failure(path + name, t, currentReplayPath)
        } catch (t: Throwable) {
            run.failure(path + name, t)
        }
    }
    when (val execution = node.layerConfig.execution) {
        ExecutionMode.Sequential -> {
            var index = 0L
            while (iterator.hasNext()) {
                runOne(index, iterator.next())
                index++
            }
        }

        is ExecutionMode.Concurrent -> iterator.forEachConcurrentBounded(
            execution.parallelism,
            run.config.coroutineContext,
            ::runOne
        )
    }
}

private suspend fun <T> Iterator<T>.forEachConcurrentBounded(
    parallelism: Int,
    coroutineContext: CoroutineContext,
    body: suspend (Long, T) -> Unit,
) = coroutineScope {
    val iteratorMutex = Mutex()
    var index = 0L
    val workers = List(parallelism) {
        launch(coroutineContext) {
            while (true) {
                val nextItem = iteratorMutex.withLock {
                    if (!hasNext()) null else (index++ to next())
                } ?: return@launch
                body(nextItem.first, nextItem.second)
            }
        }
    }
    workers.joinAll()
}

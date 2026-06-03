package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.truncated
import at.asitplus.testballoon.stackTraceForCollatedReport
import de.infix.testBalloon.framework.core.Test
import io.kotest.property.RandomSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

internal data class CompactFailure(
    internal val path: List<String>,
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

    inline fun throwIfAny() {
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

private typealias OnTest = suspend (
    path: List<String>,
    replayPath: List<MatrixPropertyReplayFrame>,
    body: suspend Test.ExecutionScope.() -> Unit,
) -> Unit

private data class CompactWork(
    val path: List<String>,
    val replayPath: List<MatrixPropertyReplayFrame>,
    val body: suspend Test.ExecutionScope.() -> Unit,
)

private suspend fun runCompactWork(
    work: CompactWork,
    testScope: Test.ExecutionScope,
    run: CompactRun,
) {
    run.start()
    try {
        testScope.withMatrixPropertyReplay(work.replayPath) { work.body(testScope) }
        run.success(work.path)
    } catch (t: AssertionError) {
        run.failure(work.path, t, work.replayPath)
    } catch (t: Throwable) {
        run.failure(work.path, t)
    }
}

internal suspend fun runCompactNodes(
    nodes: List<VirtualNode>,
    testScope: Test.ExecutionScope,
    run: CompactRun,
) {
    when (val concurrency = run.config.concurrency) {
        CompactConcurrency.Layered -> traverseVirtualNodes(
            nodes, run, respectLayerConcurrency = true,
        ) { path, replayPath, body ->
            run.start()
            try {
                testScope.withMatrixPropertyReplay(replayPath) { body(testScope) }
                run.success(path)
            } catch (t: AssertionError) {
                run.failure(path, t, replayPath)
            } catch (t: Throwable) {
                run.failure(path, t)
            }
        }
        is CompactConcurrency.Shared -> coroutineScope {
            val channel = Channel<CompactWork>(concurrency.parallelism)
            val producer = launch {
                try {
                    traverseVirtualNodes(nodes, run, respectLayerConcurrency = false) { path, replayPath, body ->
                        channel.send(CompactWork(path, replayPath, body))
                    }
                } finally {
                    channel.close()
                }
            }
            val workers = List(concurrency.parallelism) {
                launch(run.config.coroutineContext) {
                    for (work in channel) runCompactWork(work, testScope, run)
                }
            }
            producer.join()
            workers.joinAll()
        }
    }
}

private suspend fun traverseVirtualNodes(
    nodes: List<VirtualNode>,
    run: CompactRun,
    path: List<String> = emptyList(),
    replayPath: List<MatrixPropertyReplayFrame> = emptyList(),
    respectLayerConcurrency: Boolean,
    onTest: OnTest,
) {
    // In Shared mode a single worker pool bounds total concurrency, so layers iterate
    // sequentially and let the pool parallelize; in Layered mode each layer honors its own setting.
    fun layerExecution(mode: ExecutionMode) = if (respectLayerConcurrency) mode else ExecutionMode.Sequential

    for (node in nodes) {
        when (node) {
            is VirtualNode.Suite -> if (!node.disabled)
                traverseVirtualNodes(node.children, run, path + node.name, replayPath, respectLayerConcurrency, onTest)
            is VirtualNode.DynamicSuite -> if (!node.disabled)
                traverseVirtualNodes(node.children(), run, path + node.name, replayPath, respectLayerConcurrency, onTest)
            is VirtualNode.Test -> if (!node.disabled)
                onTest(path + node.name, replayPath, node.body)

            is VirtualNode.Data -> if (!node.disabled) traverseLayer(
                run, node.source.knownSize, node.source.open(), node.name, node.layerConfig.nameMaxLength,
                node.nameFn, frameOf = null, layerExecution(node.layerConfig.execution), path, replayPath, valueOf = { it },
            ) { childPath, childReplay, value ->
                traverseVirtualNodes(node.body(value), run, childPath, childReplay, respectLayerConcurrency, onTest)
            }
            is VirtualNode.DataTest -> if (!node.disabled) traverseLayer(
                run, node.source.knownSize, node.source.open(), node.name, node.layerConfig.nameMaxLength,
                node.nameFn, frameOf = null, layerExecution(node.layerConfig.execution), path, replayPath, valueOf = { it },
            ) { childPath, childReplay, value ->
                onTest(childPath, childReplay) { node.body(this, value) }
            }
            is VirtualNode.Property -> if (!node.disabled) {
                val random = node.layerConfig.seed?.let { RandomSource.seeded(it) } ?: RandomSource.default()
                traverseLayer(
                    run, node.iterations.toLong(),
                    node.gen.generate(random, node.layerConfig.edgeConfig).take(node.iterations).iterator(),
                    node.name, node.layerConfig.nameMaxLength, node.nameFn,
                    frameOf = { index, rawName -> MatrixPropertyReplayFrame(node.name, random.seed, index, rawName) },
                    layerExecution(node.layerConfig.execution), path, replayPath, valueOf = { it.value },
                ) { childPath, childReplay, value ->
                    traverseVirtualNodes(node.body(value), run, childPath, childReplay, respectLayerConcurrency, onTest)
                }
            }
            is VirtualNode.PropertyTest -> if (!node.disabled) {
                val random = node.layerConfig.seed?.let { RandomSource.seeded(it) } ?: RandomSource.default()
                traverseLayer(
                    run, node.iterations.toLong(),
                    node.gen.generate(random, node.layerConfig.edgeConfig).take(node.iterations).iterator(),
                    node.name, node.layerConfig.nameMaxLength, node.nameFn,
                    frameOf = { index, rawName -> MatrixPropertyReplayFrame(node.name, random.seed, index, rawName) },
                    layerExecution(node.layerConfig.execution), path, replayPath, valueOf = { it.value },
                ) { childPath, childReplay, value ->
                    onTest(childPath, childReplay) { node.body(this, value) }
                }
            }
        }
    }
}

/**
 * Iterates one matrix layer, building each case's name/replay path and handing the resulting
 * value to [visit] (which either recurses into child nodes or emits a leaf test). The only
 * differences between data and property layers are captured by [valueOf] (unwrapping a sample)
 * and [frameOf] (recording a property replay frame; `null` for data layers).
 */
private suspend fun <T> traverseLayer(
    run: CompactRun,
    sourceCases: Long?,
    samples: Iterator<T>,
    layerName: String,
    nameMaxLength: Int,
    nameOf: NameFn<Any?>,
    frameOf: ((index: Long, rawName: String) -> MatrixPropertyReplayFrame)?,
    execution: ExecutionMode,
    path: List<String>,
    replayPath: List<MatrixPropertyReplayFrame>,
    valueOf: (T) -> Any?,
    visit: suspend (path: List<String>, replayPath: List<MatrixPropertyReplayFrame>, value: Any?) -> Unit,
) {
    run.addSourceCases(sourceCases)
    samples.forEachCase(execution, run.config.coroutineContext) { index, sample ->
        val value = valueOf(sample)
        val rawName = nameOf(index, value).truncated(nameMaxLength)
        val childReplay = frameOf?.let { replayPath + it(index, rawName) } ?: replayPath
        visit(path + "$layerName: $rawName", childReplay, value)
    }
}

/** Drives [body] over the iterator either sequentially or with a bounded worker pool. */
private suspend fun <T> Iterator<T>.forEachCase(
    execution: ExecutionMode,
    coroutineContext: CoroutineContext,
    body: suspend (Long, T) -> Unit,
) {
    when (execution) {
        ExecutionMode.Sequential -> {
            var index = 0L
            while (hasNext()) body(index++, next())
        }
        is ExecutionMode.Concurrent -> coroutineScope {
            val iteratorMutex = Mutex()
            var index = 0L
            List(execution.parallelism) {
                launch(coroutineContext) {
                    while (true) {
                        val item = iteratorMutex.withLock {
                            if (!hasNext()) null else index++ to next()
                        } ?: return@launch
                        body(item.first, item.second)
                    }
                }
            }.joinAll()
        }
    }
}

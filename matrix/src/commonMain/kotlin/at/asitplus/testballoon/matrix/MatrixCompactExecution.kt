package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.truncated
import at.asitplus.testballoon.stackTraceForCollatedReport
import de.infix.testBalloon.framework.core.Test
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
    val replayPath: List<MatrixReplayFrame>,
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
    private var firstOmittedReplayPath: List<MatrixReplayFrame> = emptyList()
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
        replayPath: List<MatrixReplayFrame> = emptyList(),
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
                    append(failure.path.joinToString(" ↘ "))
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
                    appendLine(success.joinToString(" ↘ "))
                    renderedRows += 1
                }
                reportOmittedSuccesses += (successes.size - remainingRows).coerceAtLeast(0)
            }
            if (reportOmittedFailures > 0 || reportOmittedSuccesses > 0) {
                append("... ")
                appendOmittedCounts(reportOmittedFailures, reportOmittedSuccesses)
                appendLine(" omitted from compact report")
            }
            if (firstOmittedReplayPath.hasReplayable()) {
                appendLine(firstOmittedReplayPath.message("First error replay info omitted from row report:"))
            }
            appendLine("----------------------------------------")
            val first = failureDetails.firstOrNull()
            if (first == null) {
                appendLine("Stack traces omitted: all compact failures were omitted from compact report")
            } else {
                appendLine("Stack trace of first error: Failure: ${first.path.joinToString(" ↘ ")}")
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
    val replayable = failure.replayPath.hasReplayable()
    val message = if (!replayable) {
        failure.error.message
    } else {
        failure.error.cause?.message ?: failure.error.message
    }
    appendLine("  error: ${failure.error::class.simpleName}: $message")
    if (replayable) {
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
    replayPath: List<MatrixReplayFrame>,
    body: suspend Test.ExecutionScope.() -> Unit,
) -> Unit

private data class CompactWork(
    val path: List<String>,
    val replayPath: List<MatrixReplayFrame>,
    val body: suspend Test.ExecutionScope.() -> Unit,
)

private suspend fun runCompactWork(
    work: CompactWork,
    testScope: Test.ExecutionScope,
    run: CompactRun,
) {
    run.start()
    try {
        testScope.withMatrixReplay(work.replayPath) { work.body(testScope) }
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
    replayPath: List<MatrixReplayFrame> = emptyList(),
) {
    when (val concurrency = run.config.concurrency) {
        CompactConcurrency.Layered -> traverseVirtualNodes(
            nodes, run, replayPath = replayPath, respectLayerConcurrency = true,
        ) { path, replay, body ->
            run.start()
            try {
                testScope.withMatrixReplay(replay) { body(testScope) }
                run.success(path)
            } catch (t: AssertionError) {
                run.failure(path, t, replay)
            } catch (t: Throwable) {
                run.failure(path, t)
            }
        }
        is CompactConcurrency.Shared -> coroutineScope {
            val channel = Channel<CompactWork>(concurrency.parallelism)
            val producer = launch {
                try {
                    traverseVirtualNodes(nodes, run, replayPath = replayPath, respectLayerConcurrency = false) { path, replay, body ->
                        channel.send(CompactWork(path, replay, body))
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
    replayPath: List<MatrixReplayFrame> = emptyList(),
    respectLayerConcurrency: Boolean,
    onTest: OnTest,
) {
    // In Shared mode a single worker pool bounds total concurrency, so layers iterate
    // sequentially and let the pool parallelize; in Layered mode each layer honors its own setting.
    fun layerExecution(mode: ExecutionMode) = if (respectLayerConcurrency) mode else ExecutionMode.Sequential

    for (node in nodes) {
        when (node) {
            is VirtualNode.Suite -> if (!node.disabled)
                traverseVirtualNodes(node.children, run, path + node.name, replayPath + MatrixReplayFrame.Group(node.name), respectLayerConcurrency, onTest)
            is VirtualNode.DynamicSuite -> if (!node.disabled)
                traverseVirtualNodes(node.children(), run, path + node.name, replayPath + MatrixReplayFrame.Group(node.name), respectLayerConcurrency, onTest)
            is VirtualNode.Test -> if (!node.disabled)
                onTest(path + node.name, replayPath + MatrixReplayFrame.Group(node.name), node.body)

            is VirtualNode.Data -> if (!node.disabled) traverseLayer(
                run, node.layerConfig.caseCount(node.source.knownSize), node.source.cases(node.layerConfig.replayIndexes),
                node.name, node.layerConfig.nameMaxLength, node.nameFn,
                frameOf = { case, rawName -> MatrixReplayFrame.Data(node.name, case.index, rawName) },
                layerExecution(node.layerConfig.execution), path, replayPath,
            ) { childPath, childReplay, value ->
                traverseVirtualNodes(node.body(value), run, childPath, childReplay, respectLayerConcurrency, onTest)
            }
            is VirtualNode.DataTest -> if (!node.disabled) traverseLayer(
                run, node.layerConfig.caseCount(node.source.knownSize), node.source.cases(node.layerConfig.replayIndexes),
                node.name, node.layerConfig.nameMaxLength, node.nameFn,
                frameOf = { case, rawName -> MatrixReplayFrame.Data(node.name, case.index, rawName) },
                layerExecution(node.layerConfig.execution), path, replayPath,
            ) { childPath, childReplay, value ->
                onTest(childPath, childReplay) { node.body(this, value) }
            }
            is VirtualNode.Property -> if (!node.disabled) traverseLayer(
                run, node.layerConfig.caseCount(node.iterations),
                propertyCases(node.gen, node.iterations, node.layerConfig.edgeConfig, node.layerConfig.seed, node.layerConfig.replays),
                node.name, node.layerConfig.nameMaxLength, node.nameFn,
                frameOf = { case, rawName -> MatrixReplayFrame.Property(node.name, case.seed!!, case.index, rawName) },
                layerExecution(node.layerConfig.execution), path, replayPath,
            ) { childPath, childReplay, value ->
                traverseVirtualNodes(node.body(value), run, childPath, childReplay, respectLayerConcurrency, onTest)
            }
            is VirtualNode.PropertyTest -> if (!node.disabled) traverseLayer(
                run, node.layerConfig.caseCount(node.iterations),
                propertyCases(node.gen, node.iterations, node.layerConfig.edgeConfig, node.layerConfig.seed, node.layerConfig.replays),
                node.name, node.layerConfig.nameMaxLength, node.nameFn,
                frameOf = { case, rawName -> MatrixReplayFrame.Property(node.name, case.seed!!, case.index, rawName) },
                layerExecution(node.layerConfig.execution), path, replayPath,
            ) { childPath, childReplay, value ->
                onTest(childPath, childReplay) { node.body(this, value) }
            }
        }
    }
}

/**
 * Iterates one matrix layer's [cases], building each case's name and appending its replay frame
 * (via [frameOf]) before handing the value to [visit] (which either recurses into child nodes or
 * emits a leaf test).
 */
private suspend fun traverseLayer(
    run: CompactRun,
    sourceCases: Long?,
    cases: Iterator<Case<Any?>>,
    layerName: String?,
    nameMaxLength: Int,
    nameOf: NameFn<Any?>,
    frameOf: (case: Case<Any?>, rawName: String) -> MatrixReplayFrame,
    execution: ExecutionMode,
    path: List<String>,
    replayPath: List<MatrixReplayFrame>,
    visit: suspend (path: List<String>, replayPath: List<MatrixReplayFrame>, value: Any?) -> Unit,
) {
    run.addSourceCases(sourceCases)
    cases.forEachCase(execution, run.config.coroutineContext) { case ->
        val rawName = nameOf(case.index, case.value).truncated(nameMaxLength)
        val pathSegment = if (layerName != null) "$layerName: $rawName" else rawName
        visit(path + pathSegment, replayPath + frameOf(case, rawName), case.value)
    }
}

/** Drives [body] over the cases either sequentially or with a bounded worker pool. */
private suspend fun Iterator<Case<Any?>>.forEachCase(
    execution: ExecutionMode,
    coroutineContext: CoroutineContext,
    body: suspend (Case<Any?>) -> Unit,
) {
    when (execution) {
        ExecutionMode.Sequential -> for (case in this) body(case)
        is ExecutionMode.Concurrent -> coroutineScope {
            val iteratorMutex = Mutex()
            List(execution.parallelism) {
                launch(coroutineContext) {
                    while (true) {
                        val case = iteratorMutex.withLock { if (hasNext()) next() else null } ?: return@launch
                        body(case)
                    }
                }
            }.joinAll()
        }
    }
}

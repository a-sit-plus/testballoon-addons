package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.CompactProgressTicker
import kotlin.time.Duration
import kotlin.time.TimeSource

internal data class MatrixRegistrationFrame(
    val name: String,
    val index: Long,
    val sourceCases: Long?,
) {
    fun render(): String = buildString {
        append(name)
        append('[')
        append(index + 1)
        if (sourceCases != null) {
            append('/')
            append(sourceCases)
        }
        append(']')
    }
}

internal fun MatrixSuiteScope.registrationProgress(
    name: String,
    sourceCases: Long?,
    path: List<MatrixRegistrationFrame>,
    reporter: MatrixRegistrationReporter,
): MatrixRegistrationProgress =
    when (val progress = config.defaultProgressIndicator) {
        is Indicator.Heartbeat -> MatrixRegistrationProgress(name, sourceCases, path, reporter, progress.every)
        Indicator.None -> MatrixRegistrationProgress.None
    }

internal interface MatrixRegistrationProgress {
    fun registered(count: Long)
    fun completed(count: Long)

    object None : MatrixRegistrationProgress {
        override fun registered(count: Long) = Unit
        override fun completed(count: Long) = Unit
    }
}

private fun MatrixRegistrationProgress(
    name: String,
    sourceCases: Long?,
    path: List<MatrixRegistrationFrame>,
    reporter: MatrixRegistrationReporter,
    every: Duration,
): MatrixRegistrationProgress {
    var registered = 0L
    var lastPrinted = -1L
    val prefix = (path.map { it.render() } + name).joinToString(" › ")
    fun message(): String {
        lastPrinted = registered
        return buildString {
            append(prefix)
            append(": ")
            append(registered)
            if (sourceCases != null) append("/$sourceCases")
            append(" source cases registered")
        }
    }
    val ticker = CompactProgressTicker(every, ::message)
    return object : MatrixRegistrationProgress {
        override fun registered(count: Long) {
            registered = count
            if (reporter.shouldPrint(every)) ticker.printNow()
        }

        override fun completed(count: Long) {
            registered = count
            if (ticker.printed && lastPrinted != count) ticker.printNow()
        }
    }
}

internal class MatrixRegistrationReporter {
    private var printed = false
    private var last = TimeSource.Monotonic.markNow()

    fun shouldPrint(every: Duration): Boolean {
        if (printed && last.elapsedNow() < every) return false
        last = TimeSource.Monotonic.markNow()
        printed = true
        return true
    }
}

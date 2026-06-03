package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.toPrettyString
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundAll
import io.kotest.property.EdgeConfig
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import kotlinx.coroutines.sync.Semaphore

internal fun defaultLayerName(index: Long, value: Any?): String =
    "${index}: ${value.toPrettyString()}"

/** One generated/enumerated case of a matrix layer, carrying its original (authoritative) index. */
internal class Case<out T>(val index: Long, val value: T)

/**
 * Enumerates a data layer's cases. With [replayIndex] set, yields only the case at that index
 * (used to reproduce a single recorded failure).
 */
internal fun <T> MatrixDataSource<T>.cases(replayIndex: Long?): Iterator<Case<T>> {
    val all = open().asSequence().mapIndexed { index, value -> Case(index.toLong(), value) }
    return (if (replayIndex == null) all else all.filter { it.index == replayIndex }.take(1)).iterator()
}

/**
 * Generates a property layer's cases from [random]/[edgeConfig]. With [replayIteration] set, advances
 * the same generator and yields only the case at that index, exactly reproducing its recorded value.
 */
internal fun <T> propertyCases(
    gen: Gen<T>,
    iterations: Int,
    random: RandomSource,
    edgeConfig: EdgeConfig,
    replayIteration: Long?,
): Iterator<Case<T>> {
    val all = gen.generate(random, edgeConfig).mapIndexed { index, sample -> Case(index.toLong(), sample.value) }
    return (if (replayIteration == null) all.take(iterations) else all.filter { it.index == replayIteration }.take(1)).iterator()
}

internal fun ExecutionMode.caseLimiter(): Semaphore? =
    when (this) {
        is ExecutionMode.Concurrent -> Semaphore(parallelism)
        ExecutionMode.Sequential -> null
    }

internal fun TestConfig.boundBy(limiter: Semaphore?): TestConfig =
    if (limiter == null) this else chainedWith(TestConfig.aroundAll { elementAction ->
        limiter.acquire()
        try {
            elementAction()
        } finally {
            limiter.release()
        }
    })

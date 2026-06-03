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
 * Enumerates a data layer's cases. With [replayIndexes] set, yields only cases at those indexes
 * (used to reproduce recorded failures).
 */
internal fun <T> MatrixDataSource<T>.cases(replayIndexes: List<Long>?): Iterator<Case<T>> {
    val all = open().asSequence().mapIndexed { index, value -> Case(index.toLong(), value) }
    val selected = replayIndexes?.toSet()
    return (if (selected == null) all else all.filter { it.index in selected }).iterator()
}

/**
 * Generates a property layer's cases from [random]/[edgeConfig]. With [replayIterations] set, advances
 * the same generator and yields only cases at those indexes, exactly reproducing recorded values.
 */
internal fun <T> propertyCases(
    gen: Gen<T>,
    iterations: Int,
    random: RandomSource,
    edgeConfig: EdgeConfig,
    replayIterations: List<Long>?,
): Iterator<Case<T>> {
    val all = gen.generate(random, edgeConfig).mapIndexed { index, sample -> Case(index.toLong(), sample.value) }
    val selected = replayIterations?.toSet()
    return (if (selected == null) all.take(iterations) else all.filter { it.index in selected }.take(selected.size)).iterator()
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

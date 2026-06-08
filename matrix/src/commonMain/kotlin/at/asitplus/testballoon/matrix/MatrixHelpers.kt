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

/**
 * One generated/enumerated case of a matrix layer, carrying its original (authoritative) index and,
 * for property layers, the [seed] of the random source that produced it (`null` for data layers).
 */
internal class Case<out T>(val index: Long, val value: T, val seed: Long? = null)

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
 * Generates a property layer's cases. Normally generates [iterations] cases from a single random source
 * (seeded by [seed], or random when null). When [replays] is set, it instead reproduces exactly the
 * recorded cases: for each [Input] it seeds a fresh source and yields only its iterations — so a
 * group of failures can carry several seed/iteration pairs at once. Each [Case] is tagged with its seed.
 */
internal fun <T> propertyCases(
    gen: Gen<T>,
    iterations: Int,
    edgeConfig: EdgeConfig,
    seed: Long?,
    replays: List<Input>?,
): Iterator<Case<T>> {
    if (replays == null) {
        val random = seed?.let { RandomSource.seeded(it) } ?: RandomSource.default()
        return gen.generate(random, edgeConfig).take(iterations)
            .mapIndexed { index, sample -> Case(index.toLong(), sample.value, random.seed) }
            .iterator()
    }
    return replays.asSequence().flatMap { input ->
        val random = RandomSource.seeded(input.seed)
        val selected = input.iterations.toSet()
        gen.generate(random, edgeConfig)
            .mapIndexed { index, sample -> Case(index.toLong(), sample.value, input.seed) }
            .filter { it.index in selected }
            .take(selected.size)
    }.iterator()
}

/** Number of cases a layer will run: the selected replay cases when replaying, else its full size. */
internal fun PropertyLayerConfig.caseCount(iterations: Int): Long =
    replays?.sumOf { it.iterations.distinct().size.toLong() } ?: iterations.toLong()

internal fun DataLayerConfig.caseCount(knownSize: Long?): Long? =
    replayIndexes?.distinct()?.size?.toLong() ?: knownSize

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

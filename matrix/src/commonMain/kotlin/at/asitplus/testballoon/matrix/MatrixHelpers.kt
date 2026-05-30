package at.asitplus.testballoon.matrix

import at.asitplus.testballoon.toPrettyString
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundAll
import kotlinx.coroutines.sync.Semaphore

internal fun defaultLayerName(index: Long, value: Any?): String =
    "${index}: ${value.toPrettyString()}"

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

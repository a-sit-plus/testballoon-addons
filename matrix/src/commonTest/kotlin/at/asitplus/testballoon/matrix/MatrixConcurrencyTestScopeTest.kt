package at.asitplus.testballoon.matrix

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.coroutines.coroutineContext

// The matrix test module runs under the default session, which enables TestBalloon's virtual-time TestScope.
// Matrix concurrency must disable it so real concurrent / compact execution works (TestScope + concurrency is
// forbidden by TestBalloon and otherwise deadlocks). Sequential execution leaves the inherited scope intact.

val concurrentMatrixDisablesTestScope by matrixSuite(execution = ExecutionMode.Concurrent(2)) {
    "concurrent execution disables the virtual-time TestScope" {
        coroutineContext[TestCoroutineScheduler].shouldBeNull()
    }
}

val sequentialMatrixKeepsTestScope by matrixSuite(execution = ExecutionMode.Sequential) {
    "sequential execution keeps the inherited TestScope" {
        coroutineContext[TestCoroutineScheduler].shouldNotBeNull()
    }
}

val compactDisablesTestScope by matrixSuite {
    compact("c") - {
        "compact execution disables the virtual-time TestScope" {
            coroutineContext[TestCoroutineScheduler].shouldBeNull()
        }
    }
}

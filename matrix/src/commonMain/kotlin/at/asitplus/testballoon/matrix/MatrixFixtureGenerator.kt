package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.shared.TestRegistering
import kotlin.jvm.JvmInline

@MatrixTestDsl
class MatrixFixtureGeneratorScope<T> internal constructor(
    private val matrix: MatrixSuiteScope,
    internal val generator: () -> T,
) {
    @TestRegistering
    fun test(
        name: String,
        testConfig: TestConfig = TestConfig,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        matrix.test(name, testConfig) { body(generator()) }
    }

    @TestRegistering
    fun testSuite(
        name: String,
        testConfig: TestConfig = TestConfig,
        body: MatrixSuiteScope.(T) -> Unit,
    ) {
        matrix.target.apply {
            testSuite(
                name = matrixName(name),
                testConfig = matrix.config.testConfig.chainedWith(testConfig).disableByMatrixName(name),
            ) {
                MatrixSuiteScope(
                    this,
                    matrix.config,
                    matrix.registrationPath,
                    matrix.registrationReporter,
                    matrix.replayPath,
                ).body(generator())
            }
        }
    }

    @TestRegistering
    operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        test(this, testConfig, body)
    }

    @TestRegistering
    operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
    ): MatrixFixtureConfiguredSuite<T> = MatrixFixtureConfiguredSuite(this@MatrixFixtureGeneratorScope, this, testConfig)

    @TestRegistering
    infix operator fun MatrixFixtureConfiguredSuite<T>.minus(body: MatrixSuiteScope.(T) -> Unit) {
        scope.testSuite(name, testConfig, body)
    }

    @TestRegistering
    infix operator fun String.minus(body: MatrixSuiteScope.(T) -> Unit) {
        testSuite(this, body = body)
    }
}

class MatrixFixtureConfiguredSuite<T> internal constructor(
    internal val scope: MatrixFixtureGeneratorScope<T>,
    internal val name: String,
    internal val testConfig: TestConfig,
)

@MatrixTestDsl
class MatrixCompactFixtureGeneratorScope<T> internal constructor(
    private val compact: CompactScope,
    internal val generator: () -> T,
) {
    fun test(name: String, body: suspend Test.ExecutionScope.(T) -> Unit) {
        compact.test(name) { body(generator()) }
    }

    fun testSuite(name: String, body: CompactScope.(T) -> Unit) {
        compact.nodes += VirtualNode.DynamicSuite(matrixName(name), isMatrixDisabledName(name)) {
            val fixture = generator()
            val child = CompactScope(compact.matrixConfig, compact.config)
            child.body(fixture)
            child.nodes.toList()
        }
    }

    operator fun String.invoke(body: suspend Test.ExecutionScope.(T) -> Unit) {
        test(this, body)
    }

    infix operator fun String.minus(body: CompactScope.(T) -> Unit) {
        testSuite(this, body)
    }
}

fun <T> MatrixSuiteScope.fixture(
    generator: () -> T,
): MatrixFixtureGeneratorHolder<T> =
    MatrixFixtureGeneratorHolder(MatrixFixtureGeneratorScope(this, generator))

fun <T> CompactScope.fixture(
    generator: () -> T,
): MatrixCompactFixtureGeneratorHolder<T> =
    MatrixCompactFixtureGeneratorHolder(MatrixCompactFixtureGeneratorScope(this, generator))

@JvmInline
value class MatrixFixtureGeneratorHolder<T> internal constructor(
    private val scope: MatrixFixtureGeneratorScope<T>,
) {
    operator fun minus(body: MatrixFixtureGeneratorScope<T>.() -> Unit) {
        scope.body()
    }
}

@JvmInline
value class MatrixCompactFixtureGeneratorHolder<T> internal constructor(
    private val scope: MatrixCompactFixtureGeneratorScope<T>,
) {
    operator fun minus(body: MatrixCompactFixtureGeneratorScope<T>.() -> Unit) {
        scope.body()
    }
}

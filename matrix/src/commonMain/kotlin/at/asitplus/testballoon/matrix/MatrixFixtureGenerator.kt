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
        config: MatrixSuiteConfigBuilder,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        matrix.test(name, config) { body(generator()) }
    }

    /**
     * [testConfig] is for `aroundAll` / `aroundEach` / context / timeouts only. Set concurrency via `execution` (the
     * `matrixConfig` overload), not `TestConfig.invocation(...)`; `testScope(...)` is sequential-only.
     */
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
        config: MatrixSuiteConfigBuilder,
        body: MatrixSuiteScope.(T) -> Unit,
    ) {
        matrix.requireOpen(name)
        val resolved = config.build(matrix.config)
        matrix.target.apply {
            testSuite(
                name = matrixName(name),
                testConfig = resolved.testConfig.disableByMatrixName(name),
            ) {
                val value = generator()
                MatrixSuiteScope(
                    this,
                    resolved.nested(resolved.execution),
                    matrix.registrationPath,
                    matrix.registrationReporter,
                    matrix.replayPath + MatrixReplayFrame.Group(matrixName(name)),
                ).apply { building { body(value) } }
            }
        }
    }

    /**
     * [testConfig] is for `aroundAll` / `aroundEach` / context / timeouts only. Set concurrency via `execution` (the
     * `matrixConfig` overload), not `TestConfig.invocation(...)`; `testScope(...)` is sequential-only.
     */
    @TestRegistering
    fun testSuite(
        name: String,
        testConfig: TestConfig = TestConfig,
        body: MatrixSuiteScope.(T) -> Unit,
    ) = testSuite(name, matrixConfig { this.testConfig = testConfig }, body)

    @TestRegistering
    operator fun String.invoke(
        config: MatrixSuiteConfigBuilder,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        test(this, config, body)
    }

    /**
     * [testConfig] is for `aroundAll` / `aroundEach` / context / timeouts only. Set concurrency via `execution` (the
     * `matrixConfig` overload), not `TestConfig.invocation(...)`; `testScope(...)` is sequential-only.
     */
    @TestRegistering
    operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
        body: suspend Test.ExecutionScope.(T) -> Unit,
    ) {
        test(this, testConfig, body)
    }

    @TestRegistering
    operator fun String.invoke(
        config: MatrixSuiteConfigBuilder,
    ): MatrixFixtureConfiguredSuite<T> = MatrixFixtureConfiguredSuite(this@MatrixFixtureGeneratorScope, this, config)

    /**
     * [testConfig] is for `aroundAll` / `aroundEach` / context / timeouts only. Set concurrency via `execution` (the
     * `matrixConfig` overload), not `TestConfig.invocation(...)`; `testScope(...)` is sequential-only.
     */
    @TestRegistering
    operator fun String.invoke(
        testConfig: TestConfig = TestConfig,
    ): MatrixFixtureConfiguredSuite<T> =
        MatrixFixtureConfiguredSuite(this@MatrixFixtureGeneratorScope, this, matrixConfig { this.testConfig = testConfig })

    @TestRegistering
    infix operator fun MatrixFixtureConfiguredSuite<T>.minus(body: MatrixSuiteScope.(T) -> Unit) {
        scope.testSuite(name, config, body)
    }

    @TestRegistering
    infix operator fun String.minus(body: MatrixSuiteScope.(T) -> Unit) {
        testSuite(this, body = body)
    }
}

class MatrixFixtureConfiguredSuite<T> internal constructor(
    internal val scope: MatrixFixtureGeneratorScope<T>,
    internal val name: String,
    internal val config: MatrixSuiteConfigBuilder,
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
        compact.addDynamicSuite(name) {
            val fixture = generator()
            val child = CompactScope(compact.matrixConfig, compact.config)
            child.building { child.body(fixture) }
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

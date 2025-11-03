package at.asitplus.testballoon

import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestExecutionScope
import de.infix.testBalloon.framework.TestSuite
import de.infix.testBalloon.framework.disable

context(suite: TestSuite)
operator fun String.invoke(testConfig: TestConfig = TestConfig, nested: suspend TestExecutionScope.() -> Unit) {
    suite.test(this, testConfig = testConfig.disableByName(this)) { nested() }
}


data class ConfiguredSuite(val parent: TestSuite, val name: String, val config: TestConfig)

context(suite: TestSuite)
operator fun String.invoke(testConfig: TestConfig = TestConfig) = ConfiguredSuite(suite, this, testConfig)

context(suite: TestSuite)
infix operator fun String.minus(suiteBody: TestSuite.() -> Unit) {
    suite.testSuite(name = testName(this), testConfig = TestConfig.disableByName(this), content = fun TestSuite.() {
        suiteBody()
    })
}

infix operator fun ConfiguredSuite.minus(suiteBody: TestSuite.() -> Unit) {
    parent.testSuite(name, testConfig = config.disableByName(name), content = fun TestSuite.() {
        suiteBody()
    })
}


private fun TestConfig.disableByName(name: String) =
    if (name.startsWith("!")) TestConfig.disable() else this

private fun testName(name: String) = if (name.startsWith("!")) name.substring(1) else name

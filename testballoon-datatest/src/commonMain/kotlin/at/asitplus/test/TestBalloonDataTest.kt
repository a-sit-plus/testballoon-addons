package at.asitplus.testballoon

import de.infix.testBalloon.framework.TestConfig
import de.infix.testBalloon.framework.TestSuite

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    vararg parameters: Data,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (data in parameters) {
        val name = "$data"
        test(name, testConfig = testConfig) {
            action(data)
        }
    }
}

fun <Data> TestSuite.withData(
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in data) {
        val name = "$d"
        test(name, testConfig = testConfig) { action(d) }
    }
}

fun <Data> TestSuite.withData(
    map: Map<String, Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in map) {
        test(d.key, testConfig = testConfig) { action(d.value) }
    }
}

fun <Data> TestSuite.withData(
    nameFn: (Data) -> String, data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in data) {
        val name = nameFn(d)
        test(name, testConfig = testConfig) { action(d) }
    }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withData(
    nameFn: (Data) -> String, vararg arguments: Data,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in arguments) {
        val name = nameFn(d)
        test(name, testConfig = testConfig) { action(d) }
    }
}

fun <Data> TestSuite.withData(
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in data) {
        val name = "$d"
        test(name, testConfig = testConfig) { action(d) }
    }
}

fun <Data> TestSuite.withData(
    nameFn: (Data) -> String, data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: suspend (Data) -> Unit
) {
    for (d in data) {
        val name = nameFn(d)
        test(name, testConfig = testConfig) { action(d) }
    }
}


@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withDataSuites(
    vararg parameters: Data,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {
    for (d in parameters) {
        testSuite(name = d.toString(), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

fun <Data> TestSuite.withDataSuites(
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {
    for (d in data) {
        testSuite(name = d.toString(), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

fun <Data> TestSuite.withDataSuites(
    map: Map<String, Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {
    for (d in map) {
        testSuite(d.key, testConfig = testConfig, content = fun TestSuite.() {
            action(d.value)
        })
    }
}

fun <Data> TestSuite.withDataSuites(
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {
    for (d in data) {
        testSuite(name = d.toString(), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.LowPriorityInOverloadResolution
fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    vararg arguments: Data,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) = withDataSuites(nameFn, arguments.asIterable(), testConfig, action)

fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    data: Iterable<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {
    for (d in data) {
        testSuite(nameFn(d), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}

fun <Data> TestSuite.withDataSuites(
    nameFn: (Data) -> String,
    data: Sequence<Data>,
    testConfig: TestConfig = TestConfig,
    action: TestSuite.(Data) -> Unit
) {
    for (d in data) {
        testSuite(nameFn(d), testConfig = testConfig, content = fun TestSuite.() {
            action(d)
        })
    }
}
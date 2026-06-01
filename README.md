<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/tba-light-text.png">
  <source media="(prefers-color-scheme: light)" srcset="docs/tba-dark-text.png">
  <img src="docs/tba-light-text.png" alt="TestBalloon Addons" width="321" height="97">
</picture><br><br>

[![A-SIT Plus Official](https://raw.githubusercontent.com/a-sit-plus/a-sit-plus.github.io/709e802b3e00cb57916cbb254ca5e1a5756ad2a8/A-SIT%20Plus_%20official_opt.svg)](https://plus.a-sit.at/open-source.html)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Java](https://img.shields.io/badge/java-17-blue.svg?logo=OPENJDK)](https://www.oracle.com/java/technologies/downloads/#java17)
[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus.testballoon/datatest)](https://mvnrepository.com/artifact/at.asitplus.testballoon/datatest)

**This project provides addons for [TestBalloon](https://infix-de.github.io/testBalloon/), _the_ next generation KMP-first,
coroutine-first testing framework.**

</div>

TestBalloon Addons started as migration helpers for people coming from Kotest. That still matters, but the newer
*Matrix Testing* module is the greenfield version of the idea: _What would data-driven testing, property testing, fixtures, and
compact reports look like if they were designed directly for TestBalloon's KMP-first, coroutine-first execution model?_

The answer is a single DSL that keeps Kotest's excellent assertion and generator libraries, but gives TestBalloon full
control over registration, execution, concurrency, compaction, and reporting.

```kotlin
val combinedFeaturesSuite by matrixSuite(execution = ExecutionMode.Concurrent(12)) {
    fixture { Random.nextBytes(16) } - {
        "data, properties, fixtures, and compact reports" - { freshBytes ->
            data("multiplier", listOf(1, 2, 3)) - { multiplier ->
                compact("generated checks") { report = CompactReport.FailuresOnly } - {
                    property("offset", Arb.int(0..100), iterations = 50) test { offset ->
                        val result = freshBytes.size * multiplier + offset
                        result shouldBeGreaterThan 0
                    }
                }
            }
        }
    }
}
```

> [!TIP]  
> Looking for a smooth migration path from Kotest?  
> Check out the [Coming from Kotest](#coming-from-kotest) section!


## Compatibility

| TestBalloon Addons | TestBalloon                 |
|--------------------|-----------------------------|
| `0.9.0`            | `1.0.0` (Kotlin `2.3.0+`)   |
| `0.7.0` - `0.8.0`  | `0.8.2+` (Kotlin `2.3.0+`)  |
| `0.7.0-RC`         | `0.8.0-RC` (Kotlin `2.3.0`) |
| `0.1.1`–`0.6.1`    | `0.7.1` (Kotlin `2.2.21`)   |
| `0.1.0`            | `0.7.0` (Kotlin `2.2.21`)   |


## <picture><source media="(prefers-color-scheme: dark)" srcset="docs/matrix-dark.png"><source media="(prefers-color-scheme: light)" srcset="docs/matrix.png"><img src="docs/matrix.png" alt="Matrix Testing" width="61" height="46"></picture>&nbsp;&nbsp;Matrix Testing 


| Maven Coordinates | `at.asitplus.testballoon:matrix:$version` |
|-------------------|-------------------------------------------|

**The `matrix` module provides an original, advanced, next-generation testing DSL.**  
It may not be for the faint of heart but it combines data-driven testing,
property testing, FreeSpec-style names, fixture generation, concurrency controls, and compact reports – things you will need
for truly powerful, comprehensive test suites.

Matrix tests are built from composable layers. A `data` layer, a `property` layer, a generated fixture, and a plain
FreeSpec-style suite can be stacked into an n-dimensional test matrix, where every leaf test runs once for each path
through those layers. When that would create too many framework nodes, `compact` can flatten the subtree into one real
test while still executing and reporting the full virtual matrix. This way, you still get full insights including clickable
stacktraces taking you to the failing assertion(s)!

The top-level `matrixSuite(...) { ... }` is a regular TestBalloon suite so IDE gutter actions can discover and run it.
Inside the suite, every nested layer is configured first, then either opened with `- { ... }` for more nesting or finished
with `test { ... }` to create row test nodes.

```kotlin
val quickstart by matrixSuite(execution = ExecutionMode.Sequential) {
    data("input", listOf("foo", "bar")) - { input ->
        property("offset", Arb.int(0..100), iterations = 25) test { offset ->
            val result = "$input-$offset"
            result.startsWith(input) shouldBe true
        }
    }
}
```

### Data-Driven Matrix Layers

`data` layers accept `Iterable` values and lazy `Sequence` values. Use `test { ... }` when each row is the test, and
`- { ... }` when each row is a dimension that contains more layers or explicit tests.

```kotlin
val dataDrivenMatrix by matrixSuite(execution = ExecutionMode.Sequential) {
    data("numbers", listOf(1, 2, 3), nameFn = { index, value -> "$index: n=$value" }) test { number ->
        number shouldBeGreaterThan 0
    }

    data("lazy values", generateSequence(1) { it + 1 }, limit = 3) {
        execution = ExecutionMode.Concurrent(parallelism = 2)
    } test { number ->
        number shouldBeGreaterThan 0
    }

    data("as a dimension", listOf("foo", "bar")) - { word ->
        "has a length" {
            word.length shouldBeGreaterThan 0
        }
    }
}
```

Layer config is written as the trailing lambda before `test` or `-`. For `data`, the most important layer option is
`execution`, which can be `ExecutionMode.Sequential` or `ExecutionMode.Concurrent(parallelism = ...)`.

### Property Matrix Layers

`property` layers use Kotest generators directly. You get Kotest's `Arb` ecosystem, edge cases, shrinking-friendly data
models, and concise generator composition, while TestBalloon owns the test tree.

```kotlin
val propertyMatrix by matrixSuite(defaultPropertyIterations = 100) {
    property("bytes", Arb.byteArray(Arb.int(1, 64), Arb.byte())) {
        seed = 0xC0FFEE
        nameFn = { index, bytes -> "$index: ${bytes.size} bytes" }
    } test { bytes ->
        bytes.toHexString(HexFormat.UpperCase).hexToByteArray() shouldBe bytes
    }
}
```

`property` supports `iterations`, `seed`, `edgeConfig`, `nameFn`, and per-layer `execution`. If `iterations` is omitted,
the suite default is used.

### Compaction

Deep data/property nesting can generate very large test trees. `compact` collapses the virtual subtree into one real
TestBalloon test and reports the virtual rows itself.

```kotlin
val compactMatrix by matrixSuite(execution = ExecutionMode.Concurrent()) {
    compact("all generated checks") {
        report = CompactReport.FailuresOnly
        reportRows = 256
        progressIndicator = Indicator.Heartbeat(every = 1.seconds)
    } - {
        data("word", listOf("foo", "bar", "baz")) - { word ->
            property("number", Arb.int(0..100), iterations = 100) test { number ->
                (word.length + number) shouldBeGreaterThan 0
            }
        }
    }
}
```

`CompactReport.FailuresOnly` renders only failing virtual rows, `AllCases` also renders successes, and `SummaryOnly`
keeps the report short. `reportRows` bounds rendered row details. `addSuppressedErrors` controls whether retained
failures are attached as suppressed exceptions. `coroutineContext` controls where compact virtual children run.

Progress heartbeats are printed separately from the final failure report, for example:

```
all generated checks: compact progress: 512 of 900 queued completed (1200 source cases), 3 failed
```

> [!NOTE]
> Compact virtual children are not real TestBalloon nodes, so virtual `test` / `testSuite` declarations and terminal
> `data(...) test { ... }` / `property(...) test { ... }` rows inside `compact` cannot honor per-child `TestConfig`.
> Put `TestConfig` on real matrix tests/suites outside compact, or configure the compact block itself.

### Fixtures

`fixture` creates fresh values for each directly nested test or suite. It also works inside compact blocks.

```kotlin
val fixtureMatrix by matrixSuite(execution = ExecutionMode.Concurrent()) {
    fixture { Random.nextBytes(16) } - {
        "regular test with fresh bytes" { bytes ->
            bytes.size shouldBe 16
        }

        "suite with a fresh fixture" - { bytes ->
            data("word", listOf("foo", "bar")) test { word ->
                bytes.isNotEmpty() shouldBe true
                word.length shouldBeGreaterThan 0
            }
        }

        compact("compact checks with fresh fixture") { report = CompactReport.AllCases } - {
            data("byte", bytes.toList()) test { byte ->
                byte shouldBe byte
            }
        }
    }
}
```

### Configuration and Disabling

Project-wide matrix defaults are best configured based on the configuration of a `TestSession`:

```kotlin
object ProjectTestSessionConfig : TestSession(testConfig = TestConfig.apply {
    MatrixTestDefaults {
        execution = ExecutionMode.Sequential
        defaultPropertyIterations = 250
        defaultCompactReport = CompactReport.FailuresOnly
        defaultCompactReportRows = 128
        defaultProgressIndicator = Indicator.Heartbeat(every = 2.seconds)
    }
})
```

Individual suites can override those defaults with `matrixSuite` parameters. Layers can override their own execution or
generation settings. Real tests and suites also accept `TestConfig`, which is chained onto the current matrix config.

```kotlin
val configuredMatrix by matrixSuite(
    execution = ExecutionMode.Concurrent(parallelism = 4),
    defaultPropertyIterations = 50,
) {
    test("explicit test", testConfig = TestConfig) {
        // green code
    }

    testSuite("explicit suite", testConfig = TestConfig) {
        "bare FreeSpec-style test"(testConfig = TestConfig) {
            // green code
        }
    }

    "!temporarily disabled" {
        error("will not run")
    }
}
```

Prefix any matrix name with `!` to disable it. This works for `test`, `testSuite`, bare FreeSpec-style strings, `data`,
`property`, and `compact`. Generated row names from `nameFn` don't get disabled when they start with a bang.

### Notes

* `data(...) test { ... }` / `property(...) test { ... }` creates row test nodes; `data(...) - { ... }` / `property(...) - { ... }` creates row suite or dimension nodes.
* Forgetting `test { ... }` or `- { ... }` leaves a configured layer unopened, so no child tests are registered. This could leave you wondering on the innermost layer…
* Terminal row tests are named by the layer `nameFn`. Use `- { ... }` plus an explicit `"name" { ... }` leaf when the invariant itself needs a separate name.
* Generated rows are registered at runtime. Running an individual generated row from the IDE gutter is nonsensical; run the
  enclosing suite or use filters.
* Deep nesting can still create many real nodes. Use `compact` when the test tree itself becomes too large.

## <img src="https://kotest.io/img/logo.png" width="46" height="46" alt="Kotest Logo"> Coming from Kotest

If you want APIs that mirror Kotest more closely, the original addon modules are still available:

* `datatest` replicates Kotest's data-driven testing features for TestBalloon
* `property` brings Kotest's property testing to TestBalloon
* `fixturegen` introduces per-test fixture generation for TestBalloon without boilerplate, and beyond TestBalloon's current fixture generation capabilities
* `freespec` emulates Kotest's `FreeSpec` test style for TestBalloon

> [!TIP]  
> `freespec` and `fixturegen` are [modulated](https://github.com/a-sit-plus/modulator) into the `fixturegen-freespec`
> module. This means: if you add the `at.asitplus.modulator` Gradle plugin to any project that uses both, you can
> automagically combine FreeSpec syntax and per-test fixture generation.
>
> If you don't want to use modulator, you can add the
> `at.asitplus.testballoon:fixturegen-freespec:$version`
> dependency manually to your project.

### Test Name Truncation

> [!CAUTION]  
> TestBalloon jumps through quite some hoops to avoid the shortcomings of the underlying Gradle-based test infrastructure
> and file system limitations eating your cat. However, deep nesting and exceptionally long test names (both of which are
> easily produced when using data-driven testing or property testing) can still cause errors or even crashes.
>
> This is especially true for Android device/emulator-based test execution, which is a wondrous mess!
>
> Because TestBalloon can only shorten test names (not suite names), truncation becomes useful.

All modules allow setting global defaults with regard to test name truncation. These properties are called:

* `defaultTestNameLength`

The former generally defaults to 64 characters (15 on Android). Display names are not truncated by default.

Both properties can be set in two ways:
* **globally** (e.g., `TestBalloonAddons.defaultTestNameLength = 15`)
* **per test style** (e.g., `FreeSpec.defaultTestNameLength = 10`)

Per-style configuration takes precedence over global configuration. Hence, per-style configuration property setters are nullable,
**even though their getters will never return null**, as they fall back to the global configuration properties automatically.

It is also possible to set test name length for individual tests by passing the `maxLength` arameter.
Truncated names are ellipsised in the middle, not just cut off at the end.

**→ Check out [the full API docs](https://a-sit-plus.github.io/testballoon-addons/) for each test style for all configuration options!**

### By-Default Sane Test Names

TestBalloon Addons use sane default stringification for test names of collection and array types inside data-driven tests and property tests:

* All primitive arrays are correctly joined to string (i.e. `[-1, 4, -643, 34310]`)
* All unsigned arrays are correctly joined to string (i.e. `[9, 76, 145, 9365]`)
* `ByteArray` and `UByteArray` use hex uppercase notation (i.e. `CA:FE:BA:BE`)

### Compacting Test Series

Data-driven testing and property testing can easily produce millions of individual cases being tested.
To avoid making the test runner's heap explode in such cases, the `datatest` and `property` modules allow for compacting
test series.

Just pass the `compact = true` parameter when creating data-driven tests or property tests (see examples in the module
descriptions for [data-driven testing](#data-driven-testing) and [property testing](#property-testing)).

The names of compacted test series consist of an uppercase sigma (`Σ`) followed by the test series' datatype (e.g.,
`ΣULong`, `ΣByteArray`, …).

To still get intelligible output about which precise data point(s) caused failing tests, the error message of the resulting
failed assertion contains a compact summary and then lists the relevant child rows:

```
java.lang.AssertionError: ΣString
Summary: 1 OK, 7 failed
Error: 1: 4: expected:<three> but was:<4>
Error: 2: one: expected:<three> but was:<one>
Error: 3: null: Expected "three" but actual was null
Error: 4: null: Expected "three" but actual was null
Error: 5: null: Expected "three" but actual was null
Error: 6: two: expected:<three> but was:<two>
OK:    7: three
Error: 8: four: expected:<three> but was:<four>
----------------------------------------
```

If you only care about failing cases, set `suppressCompactSuccesses = true`. This can be configured globally through
`TestBalloonAddons.suppressCompactSuccesses`, per module through `DataTest.suppressCompactSuccesses` or
`PropertyTest.suppressCompactSuccesses`, and per terminal `withData` / `checkAll` call. Successful cases are still counted
in the summary, but individual `OK` rows are omitted.

The stack trace of the thrown exception is the stack trace of the first error (which is equal to the stack traces of all
failed assertions). As such, you can directly navigate to the error with the same convenience as ever!

By default, compacted reports keep only the first failure as the cause. To attach all failed child exceptions as suppressed
exceptions, set `addSuppressedErrorsToCompactedFailures = true` globally, per module, or per compacted run where the API
offers the override.

Suspending terminal compacted `withData` and `checkAll` leaves run child bodies sequentially by default. Set
`compactConcurrent = true` globally through `TestBalloonAddons.compactConcurrent`, per module through
`DataTest.compactConcurrent` or `PropertyTest.compactConcurrent`, or per terminal call if concurrent
execution is desired for that series. Intermediate suite builders keep their existing suite registration behaviour.

Long-running compacted terminal leaves also print periodic progress heartbeats while the compacted body is still running,
for example:

```
ΣByteArray: compact progress: 124/1000000 completed, 3 failed
```

This output is intentionally separate from the compacted failure report.

To globally enable compacting test series for data-driven testing and property testing, set
`DataTest.compactByDefault = true` and `PropertyTest.compactByDefault = true`, respectively.

**Compacting works on test and suite level!**

In addition, it is possible to specify a `prefix` parameter when defining data-driven tests or property tests. The prefix
is prepended to generated test names (in front of the sigma), which helps navigate large test graphs.

**→ Check out [the full API docs](https://a-sit-plus.github.io/testballoon-addons/) for each test style for all configuration options!**

### <picture><source media="(prefers-color-scheme: dark)" srcset="docs/data-dark.png"><source media="(prefers-color-scheme: light)" srcset="docs/data.png"><img src="docs/data.png" alt="Data-Driven Testing" width="63" height="13"></picture>&nbsp;&nbsp;Data-Driven Testing

| Maven Coordinates | `at.asitplus.testballoon:datatest:$version` |
|-------------------|---------------------------------------------|

> [!NOTE]  
> Deep nesting will produce a large number of tests, making the heap explode. Either manually compact tests as in the
> example below (works for both `withData` test series and `withData` suite series), or set the global
> `DataTest.compactByDefault = true` to automatically compact all data-driven tests.

TestBalloon makes it ridiculously easy to roll your own data-driven testing wrapper with just a couple of lines of code.
So we did, by replicating Kotest's data-driven testing API:

```kotlin
val aDataDrivenSuite by testSuite {
    
    // -> NOTE the minus ↙↙↙, it creates a suite
    withData(1, 2, 3, 4) - { number ->
        // Will create only a single test, but the error will contain all failed inputs
        withData("one", "two", "three", "four", compact = true) { word ->
            //your test logic being run 16 times
        }
    }
}
```

It is possible to specify a `prefix` parameter when defining data-driven tests and suites. The prefix is prepended to
generated test names, which helps navigate large test reports.

Running individual tests from the gutter is not possible, as the test suite structure and the names of suites and tests are computed at runtime.
Hence, you must run the entire suite (but you can manually filter using wildcards).

### <picture><source media="(prefers-color-scheme: dark)" srcset="docs/property-dark.png"><source media="(prefers-color-scheme: light)" srcset="docs/property.png"><img src="docs/property.png" alt="Property Testing" width="62" height="12"></picture>&nbsp;&nbsp;Property Testing

| Maven Coordinates | `at.asitplus.testballoon:property:$version` |
|-------------------|---------------------------------------------|

> [!NOTE]  
> Deep nesting will produce a large number of tests, making the heap explode. Either manually compact tests as in the
> first example below (works for both `checkAll` test series  and `checkAll` suite series), or set the global
> `PropertyTest.compactByDefault = true` to automatically compact all data-driven tests.

Although it comes with some warts, `kotest-property` is still extremely helpful for generating a large corpus of test
data—especially as it covers many edge cases out of the box. Again, since TestBalloon has been specifically crafted to be
flexible and extensible, we did just that:

```kotlin
val propertySuite by testSuite {
    // DON'T generate a suite for each item. Instead: aggregate >->-->------↘↘↘↘↘↘↘↘↘↘↘↘
    checkAll(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte()), compact = true) - { byteArray ->
        checkAll(iterations = 10, Arb.uLong()) { number ->
            //test with byte arrays and number for fun and profit
        }
    }

    checkAll(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
        checkAll(iterations = 10, Arb.uLong()) { number ->
            //test with byte arrays and number for fun and profit
        }
    }
}
```

It is possible to specify a `prefix` parameter when defining property tests and suites. The prefix is prepended to
generated test names, which helps navigate large test reports.

Running individual tests from the gutter is not possible, as the test suite structure and the names of suites and tests are computed at runtime.
Hence, you must run the entire suite (but you can manually filter using wildcards).

### <picture><source media="(prefers-color-scheme: dark)" srcset="docs/fixture-dark.png"><source media="(prefers-color-scheme: light)" srcset="docs/fixture.png"><img src="docs/fixture.png" alt="Fixture Generation" width="60" height="13"></picture>&nbsp;&nbsp;On-Demand Fixture Generation

| Maven Coordinates | `at.asitplus.testballoon:fixturegen:$version` |
|-------------------|-----------------------------------------------|

TestBalloon enforces a strict separation between blue code and green code. This is a good thing—especially for deeply
nested test suites—and it supports deep concurrency. Hence, ye olde JUnit4-style `@Before` and `@After` hacks mutating
global state are deliberately not supported.

Sometimes, though, you really want fresh data for every test or suite&mdash;in effect, **you want to generate a fresh test
fixture for every test/suite**.

> [!NOTE]  
> Fixture generation as provided by the addons does not use TestBalloon's native fixtures, as those only work in green code.
> The flavour of fixture generation provided by TestBalloon Addons works for suites (blue code) and tests (green code),
> as shown below.

```kotlin
import at.asitplus.testballoon.withFixtureGenerator //<- Look ma, only a single import!
import de.infix.testBalloon.framework.core.testSuite
import kotlin.random.Random
import kotlinx.coroutines.delay //just to get some suspending demo generator

val aGeneratingSuite by testSuite {

    //seed before the generator function, not inside!
    val byteRNG = Random(42);
    //We want to test with fresh randomness, so we generate a fresh fixture for each test
    withFixtureGenerator { byteRNG.nextBytes(32) } - {

        repeat(5) {
            test("Generated test with fresh randomness") { freshFixture ->
                //your test logic here
            }
        }

        testSuite("Generated Suite with fresh randomness") { freshFixture ->
            test("using the outer fixture") {
                //your logic based on freshFixture here
            }
        }
        repeat(5) {
            //✨ it ✨ just ✨ werks ✨
            test("Test with implicit fixture name `it`") {
                //do something with `it`, it contains fresh randomness!
            }
        }
    }

    //seed the RNG for reproducible tests
    val random = Random(42)

    //reference function to be called for each test inside withFixtureGenerator
    withFixtureGenerator(random::nextFloat) - {
        repeat(10) {
            test("Generated test with random float") {
                //test something floaty!
            }
        }
        test("And some other test that des not conform to the shema from the loop") {
            //test something different, with a fresh float
        }
    }

    //always-the-same fixtures also work, of course
    withFixtureGenerator {
        object {
            var a: Int = 1
            val b: Int = 2
        }
    } - {
        test("one") {
            it.a++ //and we can even modify them in one test
            println("a=${it.a}, b=${it.b}") //a=2, b=2
        }
        test("two") {
            //without affecting the other!
            println("a=${it.a}, b=${it.b}") //a=1, b=2
        }
    }

    //Let's test some nasty bug that shows itself only sometimes functionality
    val ageRNG = Random(seed = 26)
    withFixtureGenerator {
        class ABuggyImplementation(val age: Int) {
            fun restrictedAction(): Boolean =
                if (age < 18) false
                else if (age > 18) true
                else Random.nextBoolean() //introduce jitter to simulate a faulty implementation
        }

        //create new object for each test
        ABuggyImplementation(ageRNG.nextInt(0, 99))
    } - {
        repeat(1000) {
            test("Generated test accessing restricted resources") {
                //test `restrictedAction` across a wide age range
                //a thousand times to unveil the bug
            }
        }
    }
}
```

> [!WARNING]  
> A fixture-generating scope is intended to be consumed by the scope directly below it (i.e. the outermost test suite,
> or directly by a test). Programmatically, you can mix this up and it will compile, but it will not run!
>
> The following is an antipattern:
> ```kotlin
> val outermostSuite by testSuite {
>   withFixtureGenerator(random::nextFloat) - {
>     testSuite("outer") { /*fixture implicitly available as `it`*/
>       test("nested") { float -> /**`it` is not available, explicit parameter specification messes things up*/
>         //This will throw a runtime error, because "nested" will be erroneously wired directly below the outermos suite
>       }
>     }
>   }
> }
> ```

### <picture><source media="(prefers-color-scheme: dark)" srcset="docs/freespec-dark.png"><source media="(prefers-color-scheme: light)" srcset="docs/freespec.png"><img src="docs/freespec.png" alt="FreeSpec" width="56" height="11"></picture>&nbsp;&nbsp;FreeSpec

| Maven Coordinates | `at.asitplus.testballoon:freespec:$version` |
|-------------------|---------------------------------------------|

At A-SIT Plus, we've been using Kotest's [FreeSpec](https://kotest.io/docs/framework/testing-styles.html#free-spec) for its
expressiveness, as it allows modeling tests and test dependencies close to natural language.

TestBalloon is flexible enough to emulate FreeSpec with very little code, **if** you have
[context parameters](https://kotlinlang.org/docs/context-parameters.html) enabled for your codebase:

<details>
<summary>Setting up context parameters</summary>

```kotlin
// build.gradle.kts
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
```

</details>

```kotlin
import at.asitplus.testballoon.invoke
import at.asitplus.testballoon.minus
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestInvocation
import de.infix.testBalloon.framework.core.invocation
import de.infix.testBalloon.framework.core.singleThreaded
import de.infix.testBalloon.framework.core.testSuite

val aFreeSpecSuite by testSuite {
    //testConfigs are supported for suites
    "The outermost blue code"(testConfig = TestConfig.singleThreaded()) - {
        "contains some more blue code" - {
            ", some green code inside the lambda" {
                // your test logic here
            }
            //testConfigs are supported for Tests
            ", and some more green code inside the second lambda"(testConfig = TestConfig.invocation(TestInvocation.SEQUENTIAL)) {
                // more test logic here
            }
        }
        "And finally some more blue code" - {
            "!With some final disabled green code in this lambda" {
                //additional, disabled test logic here
            }
        }
    }
}
```

Running individual tests from the gutter is not (yet) possible, due to the intricacies of how code analysis works.
Hence, you must run the entire suite (but you can manually filter using wildcards).  
(You can, of course, just migrate off FreeSpec and use TestBalloon's native functions to create suites and tests.)

<details>
<summary>Combining with FixtureGen</summary>

| Maven Coordinates (if not using [modulator](https://github.com/a-sit-plus/modulator)) | `at.asitplus.testballoon:fixturegen-freespec:$version` |
|---------------------------------------------------------------------------------------|--------------------------------------------------------|

> [!WARNING]  
> As without FreeSpec syntax, a fixture-generating scope is intended to be consumed by the scope directly below it (i.e. the outermost test suite,
> or directly by a test). To disambiguate and be explicit about this, explicit parameter specification is required, starting with TestBalloon Addons 0.6.0.

```kotlin
import at.asitplus.testballoon.withFixtureGenerator //   <- Look ma, only regular generatingFixture import!
import at.asitplus.testballoon.invoke //                 <- Look ma, only regular freespec import!
import at.asitplus.testballoon.minus  //                 <- Look ma, only regular freespec import!
import de.infix.testBalloon.framework.core.testSuite
import kotlin.random.Random

val aGeneratingFreeSpecSuite by testSuite {

    //any lambda with any return type is a fixture generator. Type is reified.
    withFixtureGenerator { Random.nextBytes(32) } - {

        "A Suite with fresh randomness" - { freshFixture ->
            "Consuming outer fixture" {
                //your freshFixture-based test logic here
            }

            withFixtureGenerator { Random.nextBytes(32) } - {
                "With fresh inner fixture" { inner ->
                    //your test logic here with always fresh inner
                    //and fixed freshFixture from outer scope
                }
            }

        }

        repeat(100) {
            "Generated test with fresh randomness" { freshFixture ->
                //some more test logic; each call gets fresh randomness
            }
        }

        //parameter must be explicitly specified to disambiguate
        "Test with fixture name `it`" { it ->
            //no need for an explicit parameter name here, just use `it`
        }

        "And we can even nest!" - {
            withFixtureGenerator { Random.nextBytes(16) } - {
                repeat(10) {
                    "pure, high-octane magic going on" { it ->
                        //Woohoo! more randomness each run
                    }
                }
            }
        }
    }
}
```

</details>

## Contributing

External contributions are greatly appreciated!
Just be sure to observe the contribution guidelines (see [CONTRIBUTING.md](CONTRIBUTING.md)).

<br>

---

<p align="center">
The Apache License does not apply to the logos, (including the A-SIT logo) and the project/module name(s), as these are the sole property of
A-SIT/A-SIT Plus GmbH and may not be used in derivative works without explicit permission!
</p>

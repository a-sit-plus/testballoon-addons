<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/tba-light-text.png">
  <source media="(prefers-color-scheme: light)" srcset="docs/tba-dark-text.png">
  <img src="docs/tba-light-text.png" alt="TestBalloon Addons" width="321" height="97">
</picture><br><br>

[![A-SIT Plus Official](https://raw.githubusercontent.com/a-sit-plus/a-sit-plus.github.io/709e802b3e00cb57916cbb254ca5e1a5756ad2a8/A-SIT%20Plus_%20official_opt.svg)](https://plus.a-sit.at/open-source.html)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Java](https://img.shields.io/badge/java-17-blue.svg?logo=OPENJDK)](https://www.oracle.com/java/technologies/downloads/#java17)
[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus.testballoon/datatest)](https://mvnrepository.com/artifact/at.asitplus.testballoon/datatest)

</div>

This project provides addons for [TestBalloon](https://infix-de.github.io/testBalloon/), _the_ next generation Kotlin
test
framework, built from the ground up for Kotlin Multiplatform and coroutines.

> [!IMPORTANT]  
> Always explicitly add `de.infix.testBalloon:testBalloon-framework-core` **&ge; 0.7.0** to your test dependencies!
> You will run into an unresolved dependency error otherwise!

The code here started as a shim to make migration from Kotest easier, after being dissatisfied with the Kotest
_framework's_
second-class KMP, and third-class Android support.
At the same time, **the Kotest _libraries_, like its assertions, the way it models property testing, etc. are still
unrivaled** and don't suffer from the framework's shortcomings. Paired with TestBalloon's flexibility and its small API
surface, we can get the best of both worlds.


> [!CAUTION]  
> On Android (JVM, not native), forward slashed in test names and display names are **always** replaced
> with the `⧸` character because the Android test runner cannot deal with slashes in test names. This can bite you when
> using base64-encoded data inside test names.

## Modules

This project consists of the following modules:

* `freespec` emulating Kotest's `FreeSpec` test style for TestBalloon
* `datatest` replicates Kotest's data-driven testing features for TestBalloon
* `property` bringing Kotest's property testing to TestBalloon
* `fixturegen` introducing per-test fixture generation for TestBalloon without boilerplate

> [!TIP]  
> `freespec` and `fixturegen` are [modulated](https://github.com/a-sit-plus/modulator) into the `fixturegen-freespec`
> module, meaning that if you add the
> `at.asitplus.modulator` gradle plugin to any project that uses both, you can automagically combine FreeSpec syntax
> and per-test fixture generation! If you don't want to use modulator, you can just add the
`at.asitplus.testballoon:fixturegen-freespec:$version`
> dependency manually to your project.


All modules allow for setting global defaults wrt. test names. These properties are called:

* `defaultTestNameLength`
* `defaultDisplayNameLength`

The former defaults to 64 characters, while display names are not truncated by default.
Both properties can be set per test style (e.g., `FreeSpec.defaultTestNameLength = 10`,
`PropertyTest.defaultDisplayNameLength = 100`)
It is also possible to set test name length and display name length for individual tests by passing the `maxLength` and
`displayNameMaxLength` parameters, respectively.

In Addition, TestBalloon Addons use sane default stringification for test names of collection and arrays types
* All primitive arrays are correctly joined to string (i.e. `[-1, 4, -643, 34310]`)
* All unsigned arrays are correctly joined to string (i.e. `[9, 76, 145, 9365]`)
* `ByteArray` and `UByteArray` use hex notation (i.e. `CA:FE:BA:BE`)

### FreeSpec

| Maven Coordinates | `at.asitplus.testballoon:freespec:$version` |
|-------------------|---------------------------------------------|

At A-SIT Plus, we've been using Kotest's [FreeSpec](https://kotest.io/docs/framework/testing-styles.html#free-spec) for
its expressiveness, as it allows modeling tests and test dependencies close to natural language.

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

> [!NOTE]  
> Running individual tests from the gutter is not (yet) possible, due to the intricacies of how code analysis works.
> Hence, you must run the entire suite (but you can manually filter using wildcards).  
> (You can, of course, just migrate off FreeSpec and use TestBalloon's native functions to create suites and tests.)

### Data-Driven Testing

| Maven Coordinates | `at.asitplus.testballoon:datatest:$version` |
|-------------------|---------------------------------------------|

> [!NOTE]  
> Deep nesting will produce a large number of tests, making the heap explode. Either manually compact tests as in the
> second example below (works for both `withData` and `withDataSuites`), or set the global
> `DataTest.compactByDefault = true` to automatically compact all data-driven tests.

TestBalloon makes it ridiculously easy to roll your own data-driven testing wrapper with just a couple of lines of code.
So we did, by replicating Kotest's data-driven testing API:

```kotlin
import at.asitplus.testballoon.withData
import at.asitplus.testballoon.withDataSuites
import de.infix.testBalloon.framework.core.testSuite

val aDataDrivenSuite by testSuite {
    withDataSuites(1, 2, 3, 4) { number ->
        withData("one", "two", "three", "four") { word ->
            //your test logic being run 16 times
        }
    }

    //Alternative syntax for withDataSuites
    // -> NOTE the minus ↙↙↙
    withData(1, 2, 3, 4) - { number ->
        // Will create only a single test, but the error will contain all failed inputs
        withData("one", "two", "three", "four", compact = true) { word ->
            //your test logic being run 16 times
        }
    }
}
```

> [!NOTE]  
> Running individual tests from the gutter is not possible, as the test suite structure and the names of
> suites and tests are computed at runtime.
> Hence, you must run the entire suite (but you can manually filter using wildcards)

### Property Testing

| Maven Coordinates | `at.asitplus.testballoon:property:$version` |
|-------------------|---------------------------------------------|

> [!NOTE]  
> Deep nesting will produce a large number of tests, making the heap explode. Either manually compact tests as in the
> second example below (works for both `checkAll` and `checkAllSuites`), or set the global
> `PropertyTest.compactByDefault = true` to automatically compact all data-driven tests.

Although it comes with some warts, `kotest-property` is still extremely helpful to generate a large corpus of test data,
especially as it covers many edge cases out of the box. Again, since TestBalloon has been specifically crafted to be
flexible and extensible, we did just that:

```kotlin
import at.asitplus.testballoon.checkAll
import at.asitplus.testballoon.checkAllSuites
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.uLong

val propertySuite by testSuite {
    // DON'T Generate a suite for each item. Instead: aggregate >->-->------------↘↘↘↘↘↘↘↘↘↘↘↘
    checkAllSuites(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte()), compact = true) { byteArray ->
        checkAll(iterations = 10, Arb.uLong()) { number ->
            //test with byte arrays and number for fun and profit
        }
    }

    //Alternative syntax for checkAllSuites
    // --> NOTE THE MINUS HERE >->-->--------------------------------------↘↘↘
    checkAll(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte())) - { byteArray ->
        checkAll(iterations = 10, Arb.uLong()) { number ->
            //test with byte arrays and number for fun and profit
        }
    }
}
```

> [!NOTE]  
> Running individual tests from the gutter is not possible, as the test suite structure and the names of
> suites and tests are computed at runtime.
> Hence, you must run the entire suite (but you can manually filter using wildcards)

### Per-Test Fixture Generation

| Maven Coordinates | `at.asitplus.testballoon:fixturegen:$version` |
|-------------------|-----------------------------------------------|

TestBalloon enforces a strict separation between blue code and green code. This is a good thing, especially for deeply
nested
test suites, and it supports deep concurrency,
Hence, ye olde JUnit4-style `@Before` and `@After` hacks mutating global state are deliberately not supported.
Sometimes, though, you really want fresh data for every test or suite&mdash;in effect, **you want to generate a fresh test
fixture for every test/suite**.

Look no further:

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

<details>
<summary>Combining with FreeSpec</summary> 

| Maven Coordinates (if not using [modulator](https://github.com/a-sit-plus/modulator)) | `at.asitplus.testballoon:fixturegen-freespec:$version` |
|---------------------------------------------------------------------------------------|--------------------------------------------------------|


> [!WARNING]  
> As without FreeSpec syntax, a fixture-generating scope is intended to be consumed by the scope directly below it (i.e. the outermost test suite,
> or directly by a test). To disambiguate and be explicit about this, explicit parameter specification is required, starting with TestBalloon Addons 0.6.0.

```kotlin
import at.asitplus.testballoon.withFixtureGenerator //   <- Look ma, only regular generatingFixture import!
import at.asitplus.testballoon.invoke //              <- Look ma, only regular freespec import!
import at.asitplus.testballoon.minus  //              <- Look ma, only regular freespec import!
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

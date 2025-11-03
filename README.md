<div align="center">

# TestBalloon Addons

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

The code here started as a shim to make migration from Kotest easier, after being dissatisfied with the Kotest
_framework's_
second-class KMP, and third-class Android support.
At the same time, **the Kotest _libraries_, like its assertions, the way it models property testing, etc. are still
unrivaled** and don't suffer from the framework's shortcomings. Paired with TestBalloon's flexibility and its small API
surface, we can get the best of both worlds.

## Modules

This project consists of three modules:

* `testballoon-freespec` emulating Kotest's `FreeSpec` test style for TestBalloon
* `testballoon-datatest` replicates Kotest's data-driven testing features for TestBalloon
* `testballoon-property` bringing Kotest's property testing to TestBalloon

### FreeSpec

At A-SIT Plus, we've been using Kotest's [FreeSpec](https://kotest.io/docs/framework/testing-styles.html#free-spec) for
its expressiveness, as it allows modeling tests and test dependencies close to natural language.

TestBalloon is flexible enough to emulate FreeSpec with very little code, **if** you have
[context parameters](https://kotlinlang.org/docs/context-parameters.html) enabled for your codebase:

```kotlin
import at.asitplus.testballoon.invoke
import at.asitplus.testballoon.minus
import de.infix.testBalloon.framework.testSuite

val aFreeSpecSuite by testSuite {
    "The outermost blue code" - {
        "contains some more blue code" - {
            ", some green code inside the lambda" {
                //your test logic here
            }
            ", and some more green code inside the second lambda" {
                //more test logic
            }
        }
        "And finally some more blue code" - {
            "With some final green code in this lambda" {
                //an more test logic here
            }
        }
    }
}
```

### Data-Driven Testing

TestBalloon makes it ridiculously easy to roll your own data-driven testing wrapper with just a couple of lines of code.
So we did, by replicating Kotest's data-driven testing API:

```kotlin
import at.asitplus.testballoon.withData
import at.asitplus.testballoon.withDataSuites
import de.infix.testBalloon.framework.testSuite

val aDataDrivenSuite by testSuite {
    withDataSuites(1, 2, 3, 4) { number ->
        withData("one", "two", "three", "four") { word ->
            //your test logic being run 16 times
        }
    }
}
```

### Property Testing

Although it comes with some warts, `kotest-property` is still extremely helpful to generate a large corpus of test data,
especially as it covers many edge cases out of the box. Again, since TestBalloon has been specifically crafted to be
flexible and extensible, we did just that:

```kotlin
import at.asitplus.testballoon.checkAll
import at.asitplus.testballoon.checkAllSuites
import de.infix.testBalloon.framework.testSuite
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.uLong

val propertySuite by testSuite {
    checkAllSuites(iterations = 100, Arb.byteArray(Arb.int(100, 200), Arb.byte())) { byteArray ->
        checkAll(iterations = 1000, Arb.uLong()) { number ->
            //test with byte arrays and number for fun and profit
        }
    }
}
```

## Contributing

External contributions are greatly appreciated!
Just be sure to observe the contribution guidelines (see [CONTRIBUTING.md](CONTRIBUTING.md)).

<br>

---

<p align="center">
The Apache License does not apply to the logos, (including the A-SIT logo) and the project/module name(s), as these are the sole property of
A-SIT/A-SIT Plus GmbH and may not be used in derivative works without explicit permission!
</p>

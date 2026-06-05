package at.asitplus.testballoon.matrix

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** Edge cases for the data sources backing data layers, and the replay-aware `cases()` selector. */
val MatrixDataSourceTest by testSuite {

    test("iterable data source reports size for collections") {
        IterableDataSource(listOf(1, 2, 3)).knownSize shouldBe 3L
        IterableDataSource(emptyList<Int>()).knownSize shouldBe 0L
    }

    test("iterable data source has no known size for non-collection iterables") {
        IterableDataSource(sequenceOf(1, 2, 3).asIterable()).knownSize.shouldBeNull()
    }

    test("iterable data source opens fresh iterators each time") {
        val src = IterableDataSource(listOf("a", "b"))
        src.open().asSequence().toList() shouldBe listOf("a", "b")
        src.open().asSequence().toList() shouldBe listOf("a", "b")
    }

    test("sequence data source never reports a known size") {
        SequenceDataSource(sequenceOf(1, 2, 3), limit = null).knownSize.shouldBeNull()
    }

    test("sequence data source without limit yields everything") {
        SequenceDataSource(sequenceOf(1, 2, 3), limit = null).open().asSequence().toList() shouldBe listOf(1, 2, 3)
    }

    test("sequence data source truncates to limit") {
        SequenceDataSource(sequenceOf(1, 2, 3, 4, 5), limit = 2).open().asSequence().toList() shouldBe listOf(1, 2)
    }

    test("sequence data source limit beyond length yields all") {
        SequenceDataSource(sequenceOf(1, 2), limit = 10).open().asSequence().toList() shouldBe listOf(1, 2)
    }

    test("sequence data source zero limit yields nothing") {
        SequenceDataSource(sequenceOf(1, 2, 3), limit = 0).open().asSequence().toList().shouldBeEmpty()
    }

    test("sequence data source rejects a negative limit when opened") {
        shouldThrow<IllegalArgumentException> {
            SequenceDataSource(sequenceOf(1, 2), limit = -1).open().asSequence().toList()
        }
    }

    test("cases over empty data yield nothing") {
        IterableDataSource(emptyList<Int>()).cases(null).asSequence().toList().shouldBeEmpty()
    }

    test("cases carry ascending indexes, the values, and a null seed") {
        val cases = IterableDataSource(listOf("x", "y")).cases(null).asSequence().toList()
        cases.map { it.index } shouldBe listOf(0L, 1L)
        cases.map { it.value } shouldBe listOf("x", "y")
        cases.all { it.seed == null } shouldBe true
    }

    test("cases with an empty replay-index list select nothing") {
        IterableDataSource(listOf(1, 2, 3)).cases(emptyList()).asSequence().toList().shouldBeEmpty()
    }

    test("cases with an out-of-range replay index select nothing") {
        IterableDataSource(listOf(1, 2)).cases(listOf(5L)).asSequence().toList().shouldBeEmpty()
    }

    test("cases with replay indexes select those cases in source order") {
        val cases = IterableDataSource(listOf(10, 20, 30, 40)).cases(listOf(3L, 0L)).asSequence().toList()
        cases.map { it.index } shouldBe listOf(0L, 3L)
        cases.map { it.value } shouldBe listOf(10, 40)
    }

    test("cases dedup duplicate replay indexes") {
        IterableDataSource(listOf(10, 20, 30)).cases(listOf(1L, 1L)).asSequence().toList()
            .map { it.value } shouldBe listOf(20)
    }
}

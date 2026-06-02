package at.asitplus.testballoon

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

val PrettyStringTest by testSuite {
    
    test("single huge primitive array element keeps context") {
        val result = intArrayOf(1234567890).toPrettyString(maxLength = 8)

        result shouldBe "123…7890"
    }

  test ("Multi Element Primitive Array Honors maxLength") {
        val result = intArrayOf(1000, 2000, 3000, 4000).toPrettyString(maxLength = 12)

      result.length shouldBeLessThanOrEqual 12
      result shouldBe "1000,…, 4000"
    }

    test("Reserved PrefixLength Reduces Value Budget") {
        val result = intArrayOf(1234567890).toPrettyString(
            maxLength = 12,
            reservedPrefixLength = 4,
        )

        result shouldBe "123…7890"
    }

    test("Negative maxLength Returns Untruncated String") {
        val result = intArrayOf(1234567890).toPrettyString(maxLength = -1)

        result shouldBe "1234567890"
    }

    test("Tiny Budgets Are Deterministic") {
        val value = "abcd"

        value.toPrettyString(maxLength = 0) shouldBe ""
        value.toPrettyString(maxLength = 1) shouldBe "…"
        value.toPrettyString(maxLength = 2) shouldBe "…"
        value.toPrettyString(maxLength = 3) shouldBe "a…d"
    }

    test("Huge ByteArray Is Bounded Before Name Truncation") {
        val value = ByteArray(1_000) { it.toByte() }

        val result = value.toPrettyString()

        result.length shouldBeLessThanOrEqual 512 * 3 + 8
        result.contains(":…:") shouldBe true
    }

    test("Huge UByteArray Is Bounded Before Name Truncation") {
        val value = UByteArray(1_000) { it.toUByte() }

        val result = value.toPrettyString()

        result.length shouldBeLessThanOrEqual 512 * 3 + 8
        result.contains(":…:") shouldBe true
    }
}

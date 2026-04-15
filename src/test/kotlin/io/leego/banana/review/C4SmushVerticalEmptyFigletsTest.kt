package io.leego.banana.review

import io.leego.banana.BananaUtils
import io.leego.banana.Layout
import io.leego.banana.Option
import io.leego.banana.Rule
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * CODE_REVIEW.md finding C4 — `BananaUtils.java:552-553`.
 *
 * `smushVerticalFigletLines` reads `figlet1[0].length()` /
 * `figlet2[0].length()` without checking the outer arrays are
 * non-empty, so a zero-height figlet (e.g. an empty text line) crashes
 * with `ArrayIndexOutOfBoundsException`. After the fix the method
 * short-circuits cleanly: passing an empty figlet must not throw.
 */
class C4SmushVerticalEmptyFigletsTest {

    @Test
    fun `smushVerticalFigletLines handles empty figlet arrays without AIOOBE`() {
        val option = Option()
        option.hardBlank = "$"
        option.height = 0
        val rule = Rule()
        rule.setHorizontal(Layout.SMUSH_R, true, true, true, true, true, true)
        rule.verticalLayout = Layout.SMUSH_U
        option.rule = rule

        val method = BananaUtils::class.java.getDeclaredMethod(
            "smushVerticalFigletLines",
            Array<String>::class.java,
            Array<String>::class.java,
            Option::class.java,
        )
        method.isAccessible = true

        val empty = arrayOf<String>()
        val nonEmpty = arrayOf("abc")

        // Case 1: first arg empty — used to throw AIOOBE on figlet1[0].
        val r1 = runCatching { method.invoke(null, empty, nonEmpty, option) }
        assert(r1.isSuccess) {
            "empty figlet1 should not crash; got ${r1.exceptionOrNull()?.cause}"
        }

        // Case 2: second arg empty — used to throw AIOOBE on figlet2[0].
        val r2 = runCatching { method.invoke(null, nonEmpty, empty, option) }
        assert(r2.isSuccess) {
            "empty figlet2 should not crash; got ${r2.exceptionOrNull()?.cause}"
        }

        // Case 3: both empty.
        val r3 = runCatching { method.invoke(null, empty, empty, option) }
        assert(r3.isSuccess) {
            "both empty should not crash; got ${r3.exceptionOrNull()?.cause}"
        }
    }
}

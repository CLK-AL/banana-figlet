package io.leego.banana.review

import io.leego.banana.BananaUtils
import io.leego.banana.Layout
import io.leego.banana.Option
import io.leego.banana.Rule
import org.junit.jupiter.api.Test

/**
 * CODE_REVIEW.md finding C5 — `BananaUtils.java:586`.
 *
 * `getVerticalSmushDist` uses `maxDist = figlet1.length` and then
 * slices `figlet2` with a bound derived from `maxDist`, even when
 * `figlet2` is shorter than `figlet1`. On the second iteration of the
 * `curDist` loop this reaches past the end of `figlet2`.
 *
 * After the fix the method must return a sensible distance without
 * throwing an ArrayIndexOutOfBoundsException.
 */
class C5GetVerticalSmushDistBoundsTest {

    @Test
    fun `getVerticalSmushDist does not overrun shorter figlet2`() {
        val option = Option()
        option.hardBlank = "$"
        option.height = 3
        val rule = Rule()
        rule.setHorizontal(Layout.SMUSH_R, true, true, true, true, true, true)
        rule.verticalLayout = Layout.SMUSH_U
        rule.isVertical1 = true
        rule.isVertical2 = true
        rule.isVertical3 = true
        rule.isVertical4 = true
        rule.isVertical5 = true
        option.rule = rule

        val method = BananaUtils::class.java.getDeclaredMethod(
            "getVerticalSmushDist",
            Array<String>::class.java,
            Array<String>::class.java,
            Option::class.java,
        )
        method.isAccessible = true

        // figlet1 is taller (3 rows) than figlet2 (1 row).
        // All-whitespace contents ensure canSmushVertical returns VALID
        // on curDist=1, so the outer loop advances to curDist=2 and
        // tries to read past the end of figlet2.
        val figlet1 = arrayOf("   ", "   ", "   ")
        val figlet2 = arrayOf("   ")

        val result = runCatching { method.invoke(null, figlet1, figlet2, option) }
        assert(result.isSuccess) {
            "getVerticalSmushDist should not throw; got ${result.exceptionOrNull()?.cause}"
        }
    }
}

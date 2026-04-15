package io.leego.banana.review

import io.leego.banana.BananaUtils
import io.leego.banana.Layout
import io.leego.banana.Option
import io.leego.banana.Rule
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * CODE_REVIEW.md finding C2 — `BananaUtils.java:297`.
 *
 * `generateFigletLine` used to dereference `figlet[i]` with no null check,
 * so a truncated glyph (a `String[]` with a `null` slot somewhere inside
 * the expected height) blew up with `NullPointerException` at render
 * time. After the fix we expect an [IllegalArgumentException] with a
 * message naming the bad glyph.
 */
class C2TruncatedGlyphRenderTest {

    @Test
    fun `generateFigletLine rejects glyph with null row instead of NPE`() {
        val height = 3
        val option = Option()
        option.hardBlank = "$"
        option.height = height
        val rule = Rule()
        // Force the smushing branch that reads figlet[i] (line 297).
        rule.setHorizontal(Layout.SMUSH_R, true, true, true, true, true, true)
        rule.verticalLayout = Layout.FULL
        option.rule = rule

        // Glyph for 'A' (code 65) with a null middle row — the exact
        // shape buildMeta could theoretically produce from a truncated
        // .flf if the per-row population were ever interrupted.
        val truncatedGlyph = arrayOf<String?>("AAA", null, "AAA")
        val figletMap = HashMap<Int, Array<String>>()
        @Suppress("UNCHECKED_CAST")
        figletMap[65] = truncatedGlyph as Array<String>

        val method = BananaUtils::class.java.getDeclaredMethod(
            "generateFigletLine",
            String::class.java,
            Map::class.java,
            Option::class.java,
        )
        method.isAccessible = true

        val thrown = assertFailsWith<java.lang.reflect.InvocationTargetException> {
            method.invoke(null, "A", figletMap, option)
        }
        val cause = thrown.cause
        assert(cause is IllegalArgumentException) {
            "expected IllegalArgumentException, got ${cause?.javaClass?.name}: ${cause?.message}"
        }
    }
}

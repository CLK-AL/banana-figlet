package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage S4 — commonMain tests for [FigletRenderer].
 *
 * Uses inline minimal `.flf` content parsed via [parseFlfFont] so the
 * test is fully self-contained — no font files, no I/O, pure commonMain.
 */
class FigletRendererTest {

    /**
     * Tiny 3-row .flf with glyphs for 'H' (72) and 'i' (105) crafted
     * inline. Endmark is '@', hardblank is '$'. oldLayout=15 gives
     * SMUSH_R with all 6 horizontal rules active.
     *
     * Glyphs:
     *   H: "|_|" over 3 rows
     *   i: "i"  single column
     * Space (32) is a two-column blank to give non-trivial overlap.
     */
    private fun tinyFont(): Meta {
        // Fill CODES (32..126 + extras) — for each code provide 3 rows of
        // "x@" (any non-whitespace) so parser doesn't truncate. Then
        // overwrite the ones we care about.
        val lines = mutableListOf<String>()
        // Header: flf2a$ 3 2 5 15 1 0 24463
        lines.add("flf2a\$ 3 2 5 15 1 0 24463")
        lines.add("Tiny test font")
        // Codes: 32..126 (95 codes) + 196,214,220,223,228,246,252 (7) = 102
        // Provide glyphs for all of them to satisfy parser.
        val allCodes = (32..126).toList() + listOf(196, 214, 220, 223, 228, 246, 252)
        for (code in allCodes) {
            when (code) {
                32 -> { // space — two blank columns (use hardblanks so they don't smush away)
                    lines.add("\$\$@")
                    lines.add("\$\$@")
                    lines.add("\$\$@")
                }
                'H'.code -> {
                    lines.add("| |@")
                    lines.add("|_|@")
                    lines.add("| |@")
                }
                'i'.code -> {
                    lines.add("o@")
                    lines.add("i@")
                    lines.add("i@")
                }
                else -> {
                    lines.add("x@")
                    lines.add("x@")
                    lines.add("x@")
                }
            }
        }
        return parseFlfFont(lines)
    }

    @Test
    fun `render Hi produces non-empty output`() {
        val meta = tinyFont()
        val rows = FigletRenderer.generateLine("Hi", meta.figletMap, meta.option)
        assertFalse(rows.isEmpty(), "output rows should be non-empty")
        assertTrue(rows.any { it.isNotEmpty() }, "at least one row should contain content")
    }

    @Test
    fun `render Hi output height matches Option height`() {
        val meta = tinyFont()
        val rows = FigletRenderer.generateLine("Hi", meta.figletMap, meta.option)
        assertEquals(meta.option.height, rows.size)
    }

    @Test
    fun `hardblank is replaced by space in output`() {
        val meta = tinyFont()
        val rows = FigletRenderer.generateLine("Hi", meta.figletMap, meta.option)
        val hardBlank = meta.option.hardBlank!!
        for (row in rows) {
            assertFalse(
                row.contains(hardBlank),
                "output row '$row' still contains hardBlank '$hardBlank'",
            )
        }
    }

    @Test
    fun `render single character matches that glyph height`() {
        val meta = tinyFont()
        val rows = FigletRenderer.generateLine("H", meta.figletMap, meta.option)
        assertEquals(3, rows.size)
        // Each row should contain the 'H' glyph content (after hardblank strip)
        // The 'H' glyph is "| |" / "|_|" / "| |" — so row 1 should contain "_"
        assertTrue(rows[1].contains("_"), "row 1 should contain '_' from 'H' glyph: was '${rows[1]}'")
    }

    @Test
    fun `render text with unknown codepoint skips it`() {
        val meta = tinyFont()
        // Use a codepoint outside ASCII/extras range, e.g. U+0001 (SOH)
        val rows = FigletRenderer.generateLine("\u0001H", meta.figletMap, meta.option)
        // Should still render H only
        assertEquals(3, rows.size)
        assertTrue(rows[1].contains("_"))
    }

    @Test
    fun `render empty text produces empty rows`() {
        val meta = tinyFont()
        val rows = FigletRenderer.generateLine("", meta.figletMap, meta.option)
        assertEquals(meta.option.height, rows.size)
        assertTrue(rows.all { it.isEmpty() }, "all rows should be empty for empty input")
    }

    // -- Vertical smushing (combineVertically) ----------------------------

    /**
     * Helper: copy [base] overriding `rule.verticalLayout` and the
     * per-rule toggles. A FULL layout means "no smushing" so vertical
     * rules are all off.
     */
    private fun withVerticalLayout(base: Option, layout: Layout, rulesOn: Boolean = true): Option {
        val oldRule = base.rule ?: Rule.default()
        val flag = rulesOn && layout == Layout.SMUSH_R
        return base.copy(
            rule = oldRule.copy(
                verticalLayout = layout,
                vertical1 = flag, vertical2 = flag, vertical3 = flag,
                vertical4 = flag, vertical5 = flag,
            ),
        )
    }

    @Test
    fun `combineVertically - empty input returns empty list`() {
        val meta = tinyFont()
        val rows = FigletRenderer.combineVertically(emptyList(), meta.option)
        assertTrue(rows.isEmpty(), "empty input should produce empty output")
    }

    @Test
    fun `combineVertically - single input returns it unchanged`() {
        val meta = tinyFont()
        val one = FigletRenderer.generateLine("A", meta.figletMap, meta.option)
        val rows = FigletRenderer.combineVertically(listOf(one), meta.option)
        assertEquals(one, rows)
    }

    @Test
    fun `combineVertically - FULL layout stacks without smushing`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.FULL)
        val one = FigletRenderer.generateLine("A", meta.figletMap, opt)
        val rows = FigletRenderer.combineVertically(listOf(one, one), opt)
        // FULL = no smushing, output height = 2 * single-line height
        assertEquals(one.size * 2, rows.size)
    }

    @Test
    fun `combineVertically - FITTED layout may not compress height`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.FITTED)
        val one = FigletRenderer.generateLine("A", meta.figletMap, opt)
        val rows = FigletRenderer.combineVertically(listOf(one, one), opt)
        // FITTED: as soon as both rows have a non-whitespace column,
        // they stop smushing. For our "x" glyphs every row is full, so
        // no overlap is possible — height is 2*height.
        assertEquals(one.size * 2, rows.size)
    }

    @Test
    fun `combineVertically - SMUSH_U allows overlap`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.SMUSH_U)
        val one = FigletRenderer.generateLine("A", meta.figletMap, opt)
        val rows = FigletRenderer.combineVertically(listOf(one, one), opt)
        // SMUSH_U: "end" result can still collapse one row.
        assertTrue(rows.size <= one.size * 2)
    }

    @Test
    fun `combineVertically - SMUSH_R with all vertical rules active`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.SMUSH_R, rulesOn = true)
        val one = FigletRenderer.generateLine("H", meta.figletMap, opt)
        val rows = FigletRenderer.combineVertically(listOf(one, one), opt)
        // Height cannot exceed 2*height.
        assertTrue(rows.size <= one.size * 2)
        assertTrue(rows.size >= one.size, "output must be at least one copy tall")
    }

    @Test
    fun `combineVertically - multi-char and multi-line composes`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.SMUSH_R)
        val row1 = FigletRenderer.generateLine("Hi", meta.figletMap, opt)
        val row2 = FigletRenderer.generateLine("i", meta.figletMap, opt)
        val row3 = FigletRenderer.generateLine("Hi", meta.figletMap, opt)
        val combined = FigletRenderer.combineVertically(listOf(row1, row2, row3), opt)
        assertFalse(combined.isEmpty(), "combined output should be non-empty")
        // Each row in the final output should have equal width (padding)
        val widths = combined.map { it.length }.toSet()
        assertEquals(1, widths.size, "all rows must be padded to the same width: $widths")
    }

    @Test
    fun `combineVertically - different widths pad to max`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.FULL)
        val wide = FigletRenderer.generateLine("Hi", meta.figletMap, opt)
        val narrow = FigletRenderer.generateLine("i", meta.figletMap, opt)
        val combined = FigletRenderer.combineVertically(listOf(wide, narrow), opt)
        val widths = combined.map { it.length }.toSet()
        assertEquals(1, widths.size, "mixed-width inputs must be padded to a single width")
        assertEquals(wide[0].length, widths.first())
    }

    // -- C4 regression: empty figlet line array guard ---------------------

    @Test
    fun `C4 - combineVertically handles empty first entry (smushVerticalFigletLines guard)`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.SMUSH_R)
        val one = FigletRenderer.generateLine("H", meta.figletMap, opt)
        // Directly pokes the C4 guard via the combineVertically driver.
        val combined = FigletRenderer.combineVertically(listOf(emptyList(), one), opt)
        assertEquals(one, combined, "empty first entry must fall through to the second one")
    }

    @Test
    fun `C4 - combineVertically handles empty second entry`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.SMUSH_R)
        val one = FigletRenderer.generateLine("H", meta.figletMap, opt)
        val combined = FigletRenderer.combineVertically(listOf(one, emptyList()), opt)
        assertEquals(one, combined, "empty second entry must fall through to the first one")
    }

    // -- C5 regression: mismatched heights cap curDist --------------------

    @Test
    fun `C5 - combineVertically with mismatched heights caps overlap at shorter`() {
        val meta = tinyFont()
        val opt = withVerticalLayout(meta.option, Layout.SMUSH_R)
        // Build two arrays with different heights by hand. Both are
        // single-column whitespace → they SMUSH_R-collapse as long as
        // we don't slice past the shorter array.
        val short = listOf(" ")
        val tall = listOf(" ", " ", " ")
        val combined = FigletRenderer.combineVertically(listOf(short, tall), opt)
        // C5 guarantees we don't AIOOBE here; result's total rows must
        // be between max(short,tall)=3 and short+tall=4.
        assertTrue(combined.size in 3..4, "C5 cap: rows=${combined.size}, want 3..4")
    }
}

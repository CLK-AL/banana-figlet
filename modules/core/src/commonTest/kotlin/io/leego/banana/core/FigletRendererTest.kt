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
}

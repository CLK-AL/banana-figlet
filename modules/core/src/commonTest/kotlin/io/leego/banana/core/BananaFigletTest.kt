package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stage S4 -- commonMain tests for [BananaFiglet] top-level API.
 *
 * Uses a minimal inline `.flf` font (height 2) to exercise the
 * orchestration without any platform I/O.
 */
class BananaFigletTest {

    /**
     * Minimal inline FLF font with height 2.
     * Defines glyphs for 'H' (code 72) and 'i' (code 105).
     *
     * Header: signature "flf2a$" hardblank=$, height=2, baseline=2,
     * maxLength=4, oldLayout=0, numCommentLines=1
     *
     * We need glyphs for codes 32..126. We define space (32) through
     * 'i' (105) with minimal 2-row glyphs. Codes we don't care about
     * get a simple space glyph.
     */
    private val miniFontLines: List<String> by lazy {
        buildMiniFontLines()
    }

    private fun buildMiniFontLines(): List<String> {
        val lines = mutableListOf<String>()
        // Header: flf2a + hardblank=$, height=2, baseline=2, maxLength=8, oldLayout=0, numCommentLines=1
        lines.add("flf2a\$ 2 2 8 0 1")
        // Comment
        lines.add("Mini test font")
        // Now we need glyphs for codes 32..126 then the 7 German extras.
        // That's 95 ASCII chars + 7 = 102 glyphs, each 2 rows.
        // We define meaningful glyphs for H (72) and i (105).
        val codes = buildList {
            for (c in 32..126) add(c)
            addAll(listOf(196, 214, 220, 223, 228, 246, 252))
        }
        for (code in codes) {
            when (code) {
                72 -> { // 'H'
                    lines.add("H H@")
                    lines.add("HHH@")
                }
                105 -> { // 'i'
                    lines.add(" o@")
                    lines.add(" i@")
                }
                else -> {
                    // Space / filler glyph
                    lines.add(" @")
                    lines.add(" @")
                }
            }
        }
        return lines
    }

    // ---- bananaify basic ---------------------------------------------------

    @Test
    fun `bananaify renders Hi with mini font`() {
        val result = BananaFiglet.bananaify("Hi", miniFontLines)
        assertTrue(result.isNotEmpty(), "bananaify output should not be empty")
        val outputLines = result.split("\n")
        assertEquals(2, outputLines.size, "Output should have 2 lines (font height)")
        // The H glyph is "H H" / "HHH", the i glyph is " o" / " i"
        // With oldLayout=0 (FITTED), they should be concatenated closely.
        assertTrue(outputLines[0].contains("H"), "First row should contain H")
        assertTrue(outputLines[1].contains("H"), "Second row should contain H")
    }

    @Test
    fun `bananaify output has correct height for mini font`() {
        val result = BananaFiglet.bananaify("H", miniFontLines)
        val outputLines = result.split("\n")
        assertEquals(2, outputLines.size, "Single character should produce font-height lines")
    }

    // ---- S8.5 empty-input short-circuit ------------------------------------

    @Test
    fun `bananaify returns empty for null input`() {
        assertEquals("", BananaFiglet.bananaify(null, miniFontLines))
    }

    @Test
    fun `bananaify returns empty for empty string`() {
        assertEquals("", BananaFiglet.bananaify("", miniFontLines))
    }

    // ---- Multi-line input --------------------------------------------------

    @Test
    fun `bananaify handles multi-line input`() {
        val result = BananaFiglet.bananaify("H\nH", miniFontLines)
        assertTrue(result.isNotEmpty(), "Multi-line output should not be empty")
        // Two input lines, each producing 2 rows of output (height 2).
        // With default vertical layout (FULL for this font since no fullLayout header),
        // the result should have 4 lines.
        val outputLines = result.split("\n")
        assertTrue(outputLines.size >= 2, "Multi-line output should have multiple rows")
    }

    // ---- bananansi ---------------------------------------------------------

    @Test
    fun `bananansi wraps output with ANSI codes`() {
        val result = BananaFiglet.bananansi(
            "H", miniFontLines,
            styles = arrayOf(Ansi.RED),
        )
        assertTrue(result.isNotEmpty(), "bananansi output should not be empty")
        // Each line should start with \033[31m and end with \033[0m
        for (line in result.split("\n")) {
            assertTrue(
                line.startsWith("\u001B[31m"),
                "Each line should start with red ANSI code, got: '$line'"
            )
            assertTrue(
                line.endsWith("\u001B[0m"),
                "Each line should end with reset ANSI code, got: '$line'"
            )
        }
    }

    @Test
    fun `bananansi returns empty for null input`() {
        assertEquals("", BananaFiglet.bananansi(null, miniFontLines, styles = arrayOf(Ansi.RED)))
    }

    @Test
    fun `bananansi with multiple styles combines codes`() {
        val result = BananaFiglet.bananansi(
            "H", miniFontLines,
            styles = arrayOf(Ansi.RED, Ansi.BOLD),
        )
        assertTrue(result.isNotEmpty())
        // Combined code: \033[31;1m
        val firstLine = result.split("\n")[0]
        assertTrue(
            firstLine.startsWith("\u001B[31;1m"),
            "Combined ANSI codes expected, got: '$firstLine'"
        )
    }

    // ---- Layout overrides --------------------------------------------------

    @Test
    fun `bananaify with FULL layout produces wider output`() {
        val defaultResult = BananaFiglet.bananaify("Hi", miniFontLines)
        val fullResult = BananaFiglet.bananaify("Hi", miniFontLines, hLayout = Layout.FULL)
        // FULL layout should produce wider (or equal) output than the default FITTED
        val defaultWidth = defaultResult.split("\n").maxOf { it.length }
        val fullWidth = fullResult.split("\n").maxOf { it.length }
        assertTrue(fullWidth >= defaultWidth,
            "FULL layout ($fullWidth) should be at least as wide as default ($defaultWidth)")
    }

    // ---- Ansi tests --------------------------------------------------------

    @Test
    fun `Ansi ansify with single style`() {
        val result = Ansi.ansify("hello", Ansi.RED)
        assertEquals("\u001B[31mhello\u001B[0m", result)
    }

    @Test
    fun `Ansi ansify with empty text returns text`() {
        assertEquals("", Ansi.ansify("", Ansi.RED))
    }

    @Test
    fun `Ansi ansify with null text returns null`() {
        assertEquals(null, Ansi.ansify(null, Ansi.RED))
    }

    @Test
    fun `Ansi ansify with no styles returns text`() {
        assertEquals("hello", Ansi.ansify("hello"))
    }

    @Test
    fun `Ansi ansify with all null styles returns text`() {
        assertEquals("hello", Ansi.ansify("hello", null, null))
    }

    @Test
    fun `Ansi ansify with multiple styles`() {
        val result = Ansi.ansify("hello", Ansi.RED, Ansi.BOLD)
        assertEquals("\u001B[31;1mhello\u001B[0m", result)
    }
}

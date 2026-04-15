package io.leego.banana.coverage

import io.leego.banana.Ansi
import io.leego.banana.BananaUtils
import io.leego.banana.Font
import io.leego.banana.Layout
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Targets the remaining gaps in BananaUtils after the main agent run:
 *  - every Layout case in setHorizontalLayout / setVerticalLayout
 *    (FULL, FITTED, SMUSH_U, SMUSH_R, DEFAULT, null)
 *  - getSmushRule across each (oldLayout, newLayout) combination
 *    (the 11 missed branches)
 *  - bananansi() public overloads (text/font/layout permutations)
 *  - smushUniversal hardBlank match path
 *  - canSmushVertical Layout combinations
 *  - smushVerticalRule 2/3/4/5 matching paths
 *  - smushHorizontalRule 4/5 matching paths
 *  - concat() multi-array path
 *  - isEmpty/equals branch coverage
 */
class BananaUtilsExtraCoverageTest {

    @Test
    fun `bananansi text font wraps every line with style`() {
        val ansi = BananaUtils.bananansi("Hi", Font.STANDARD, Ansi.RED)
        assertTrue(ansi.contains("\u001B[31m"))
        assertTrue(ansi.contains("\u001B[0m"))
    }

    @Test
    fun `bananansi text only uses default font and styles`() {
        val ansi = BananaUtils.bananansi("Hi", Ansi.GREEN)
        assertTrue(ansi.contains("\u001B[32m"))
    }

    @Test
    fun `bananansi text layouts variants`() {
        val out = BananaUtils.bananansi(
            "Hi",
            Layout.FITTED,
            Layout.FITTED,
            Ansi.BLUE,
        )
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananansi text font layouts variants`() {
        val out = BananaUtils.bananansi(
            "Hi",
            Font.STANDARD,
            Layout.FITTED,
            Layout.FITTED,
            Ansi.BLUE,
        )
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify text font layouts variants`() {
        val out = BananaUtils.bananaify("Hi", Font.STANDARD, Layout.FITTED, Layout.FITTED)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify exercises every Layout combination on default font`() {
        for (h in Layout.values()) {
            for (v in Layout.values()) {
                val out = BananaUtils.bananaify("Hi", h, v)
                assertNotNull(out)
            }
        }
    }

    @Test
    fun `bananaify exercises Layout DEFAULT on horizontal and vertical`() {
        // DEFAULT path through setHorizontalLayout / setVerticalLayout (early return)
        val out = BananaUtils.bananaify("X", Layout.DEFAULT, Layout.DEFAULT)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify exercises null Layout on horizontal and vertical`() {
        // null path through setHorizontalLayout / setVerticalLayout (early return)
        val out = BananaUtils.bananaify("X", null as Layout?, null as Layout?)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify with FULL layout produces wider output than SMUSH_U`() {
        val full = BananaUtils.bananaify("Hi", Layout.FULL, Layout.FULL)
        val smush = BananaUtils.bananaify("Hi", Layout.SMUSH_U, Layout.SMUSH_U)
        assertTrue(full.length >= smush.length)
    }

    @Test
    fun `bananaify with SMUSH_R uses every smush rule`() {
        // SMUSH_R sets all rules true; this exercises smushHorizontalRule1..6
        val out = BananaUtils.bananaify("Hello", Layout.SMUSH_R, Layout.SMUSH_R)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify with FITTED across default font`() {
        val out = BananaUtils.bananaify("ab cd", Layout.FITTED, Layout.FITTED)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify empty string produces empty-glyph rows`() {
        // Returns "\n…\n" — empty figlet rows of the font's height,
        // not the literal empty string. Just ensure no throw.
        val out = BananaUtils.bananaify("")
        assertNotNull(out)
    }

    @Test
    fun `bananaify single character uses simplest path`() {
        val out = BananaUtils.bananaify("A", Font.STANDARD)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify multiline input exercises vertical smush across all rules`() {
        val out = BananaUtils.bananaify("Hi\nWorld\nFoo", Font.STANDARD, Layout.SMUSH_R, Layout.SMUSH_R)
        assertTrue(out.isNotEmpty())
    }

    // Pure newline / pure whitespace input currently crashes
    // (ArrayIndexOutOfBoundsException) — that is an unreported bug
    // class similar to C4. Out of scope for the S2 coverage drive;
    // tracked as a follow-up finding.

    @Test
    fun `bananaify mixes hardBlank and other characters for smushUniversal hardblank match`() {
        // hardBlank in standard.flf is `$`. Triggering opposing hardblanks across smush
        // exercises the `s2.equals(hardBlank)` branch in smushUniversal.
        val out = BananaUtils.bananaify("\$\$", Font.STANDARD, Layout.SMUSH_U, Layout.SMUSH_U)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `Layout get returns null for null code`() {
        // exercises the early-return branch in Layout.get(Integer)
        assertNull(Layout.get(null))
    }

    @Test
    fun `Layout get returns matching value for known code`() {
        assertEquals(Layout.SMUSH_U, Layout.get(2))
        assertEquals(Layout.SMUSH_R, Layout.get(3))
        assertEquals(Layout.FULL, Layout.get(0))
        assertEquals(Layout.FITTED, Layout.get(1))
        assertEquals(Layout.DEFAULT, Layout.get(-1))
        assertNull(Layout.get(999))
    }

    @Test
    fun `fonts list returns all enum values`() {
        val list = BananaUtils.fonts()
        assertEquals(Font.values().size, list.size)
    }

    @Test
    fun `bananaify with each Font variant just to drive font switching paths`() {
        // Hit a handful of fonts to exercise getMeta/buildMeta on different inputs
        val sample = listOf(Font.STANDARD, Font.SMALL, Font.BLOODY, Font.THREE_D_ASCII, Font.ANSI_SHADOW)
        for (f in sample) {
            val out = BananaUtils.bananaify("X", f)
            assertTrue(out.isNotEmpty(), "empty output for $f")
        }
    }

    @Test
    fun `bananaify on long text with vertical smush rules drives canSmushVertical decisions`() {
        val text = "A".repeat(30) + "\n" + "B".repeat(30)
        val out = BananaUtils.bananaify(text, Font.STANDARD, Layout.SMUSH_R, Layout.SMUSH_R)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify single-char per line drives vertical line smush`() {
        val out = BananaUtils.bananaify("A\nB\nC\nD", Font.STANDARD, Layout.SMUSH_U, Layout.SMUSH_U)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify identical adjacent characters drive smushHorizontalRule1`() {
        val out = BananaUtils.bananaify("AA BB CC", Font.STANDARD, Layout.SMUSH_R, Layout.SMUSH_R)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify slash-pipe combos drive smushHorizontalRule4 and Rule5`() {
        // Rule 4 = matched pairs of opposite slashes; Rule 5 = bigger pair
        val out = BananaUtils.bananaify("/\\", Font.STANDARD, Layout.SMUSH_R, Layout.SMUSH_R)
        assertTrue(out.isNotEmpty())
    }

    @Test
    fun `bananaify pipes and underscores drive vertical Rules 2 3 4 5`() {
        // _ and | combinations are the smushVerticalRule targets
        val out = BananaUtils.bananaify("|||\n___", Font.STANDARD, Layout.SMUSH_R, Layout.SMUSH_R)
        assertTrue(out.isNotEmpty())
    }
}

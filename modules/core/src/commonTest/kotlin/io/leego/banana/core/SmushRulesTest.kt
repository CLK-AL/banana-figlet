package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 — exhaustive commonMain coverage of [SmushRules].
 *
 * Includes a dedicated `§17.5` block that proves the `\\/ → Y` branch
 * is now reachable in the commonMain port (the frozen Java still has
 * it as dead code; documented in `CODE_REVIEW.md` §17.5).
 */
class SmushRulesTest {

    private val hb = "\$"

    // -- Rule 1: EQUAL CHARACTER SMUSHING ---------------------------------

    @Test
    fun `rule1 - equal non-hardblank chars smush to that char`() {
        assertEquals("|", SmushRules.smushHorizontalRule1("|", "|", hb))
        assertEquals("a", SmushRules.smushHorizontalRule1("a", "a", hb))
    }

    @Test
    fun `rule1 - hardblank pairs do not smush`() {
        assertEquals("", SmushRules.smushHorizontalRule1(hb, hb, hb))
    }

    @Test
    fun `rule1 - different chars return empty`() {
        assertEquals("", SmushRules.smushHorizontalRule1("|", "/", hb))
    }

    @Test
    fun `rule1 - null hardblank treats equal chars as smushable`() {
        assertEquals("|", SmushRules.smushHorizontalRule1("|", "|", null))
    }

    // -- Rule 2: UNDERSCORE SMUSHING --------------------------------------

    @Test
    fun `rule2 - underscore replaced by pipe`() {
        assertEquals("|", SmushRules.smushHorizontalRule2("_", "|"))
        assertEquals("|", SmushRules.smushHorizontalRule2("|", "_"))
    }

    @Test
    fun `rule2 - underscore replaced by every rule char`() {
        for (c in "|/\\[]{}()<>") {
            val s = c.toString()
            assertEquals(s, SmushRules.smushHorizontalRule2("_", s), "s1=_ s2=$s")
            assertEquals(s, SmushRules.smushHorizontalRule2(s, "_"), "s1=$s s2=_")
        }
    }

    @Test
    fun `rule2 - non-matching chars return empty`() {
        assertEquals("", SmushRules.smushHorizontalRule2("_", "a"))
        assertEquals("", SmushRules.smushHorizontalRule2("a", "_"))
        assertEquals("", SmushRules.smushHorizontalRule2("a", "b"))
    }

    // -- Rule 3: HIERARCHY SMUSHING ---------------------------------------

    @Test
    fun `rule3 - later class wins`() {
        // pipe < slash-pair < brackets < braces < parens < angles
        assertEquals("/", SmushRules.smushHorizontalRule3("|", "/"))
        assertEquals("[", SmushRules.smushHorizontalRule3("|", "["))
        assertEquals("{", SmushRules.smushHorizontalRule3("[", "{"))
        assertEquals("(", SmushRules.smushHorizontalRule3("{", "("))
        assertEquals("<", SmushRules.smushHorizontalRule3("(", "<"))
    }

    @Test
    fun `rule3 - same class returns empty`() {
        // '/' and '\' are same class (adjacent in "| /\ ...")
        assertEquals("", SmushRules.smushHorizontalRule3("/", "\\"))
        assertEquals("", SmushRules.smushHorizontalRule3("[", "]"))
    }

    @Test
    fun `rule3 - identical positions return empty`() {
        assertEquals("", SmushRules.smushHorizontalRule3("|", "|"))
    }

    @Test
    fun `rule3 - char not in rule returns empty`() {
        assertEquals("", SmushRules.smushHorizontalRule3("a", "|"))
        assertEquals("", SmushRules.smushHorizontalRule3("|", "a"))
    }

    // -- Rule 4: OPPOSITE PAIR SMUSHING -----------------------------------

    @Test
    fun `rule4 - opposing brackets produce pipe`() {
        assertEquals("|", SmushRules.smushHorizontalRule4("[", "]"))
        assertEquals("|", SmushRules.smushHorizontalRule4("]", "["))
        assertEquals("|", SmushRules.smushHorizontalRule4("{", "}"))
        assertEquals("|", SmushRules.smushHorizontalRule4("}", "{"))
        assertEquals("|", SmushRules.smushHorizontalRule4("(", ")"))
        assertEquals("|", SmushRules.smushHorizontalRule4(")", "("))
    }

    @Test
    fun `rule4 - non-opposing pairs return empty`() {
        assertEquals("", SmushRules.smushHorizontalRule4("[", "{"))
        assertEquals("", SmushRules.smushHorizontalRule4("[", "("))
    }

    @Test
    fun `rule4 - char not in rule returns empty`() {
        assertEquals("", SmushRules.smushHorizontalRule4("a", "]"))
    }

    // -- Rule 5: BIG X SMUSHING (includes §17.5 divergence tests) ---------

    @Test
    fun `rule5 - slash-backslash smushes to pipe`() {
        assertEquals("|", SmushRules.smushHorizontalRule5("/", "\\"))
    }

    @Test
    fun `rule5 §17_5 - backslash-slash smushes to Y (was dead branch in Java)`() {
        // This is the headline §17.5 fix: the frozen Java returns "" here
        // because `indexOf("\\")` always returns 1, never 3. The commonMain
        // port uses explicit pair matching so Y is now reachable.
        assertEquals("Y", SmushRules.smushHorizontalRule5("\\", "/"))
    }

    @Test
    fun `rule5 - gt-lt smushes to X`() {
        assertEquals("X", SmushRules.smushHorizontalRule5(">", "<"))
    }

    @Test
    fun `rule5 - lt-gt is not smushed (spec requirement)`() {
        assertEquals("", SmushRules.smushHorizontalRule5("<", ">"))
    }

    @Test
    fun `rule5 - same char pairs return empty`() {
        assertEquals("", SmushRules.smushHorizontalRule5("/", "/"))
        assertEquals("", SmushRules.smushHorizontalRule5("\\", "\\"))
    }

    @Test
    fun `rule5 - unrelated chars return empty`() {
        assertEquals("", SmushRules.smushHorizontalRule5("a", "b"))
        assertEquals("", SmushRules.smushHorizontalRule5("|", "|"))
    }

    // -- Rule 6: HARDBLANK SMUSHING ---------------------------------------

    @Test
    fun `rule6 - two hardblanks smush to one`() {
        assertEquals(hb, SmushRules.smushHorizontalRule6(hb, hb, hb))
    }

    @Test
    fun `rule6 - non-hardblanks return empty`() {
        assertEquals("", SmushRules.smushHorizontalRule6("a", "a", hb))
        assertEquals("", SmushRules.smushHorizontalRule6(hb, "a", hb))
        assertEquals("", SmushRules.smushHorizontalRule6("a", hb, hb))
    }

    @Test
    fun `rule6 - null hardblank returns empty`() {
        assertEquals("", SmushRules.smushHorizontalRule6("a", "a", null))
    }

    // -- Universal smushing ------------------------------------------------

    @Test
    fun `universal - space s2 returns s1`() {
        assertEquals("a", SmushRules.smushUniversal("a", " ", hb))
    }

    @Test
    fun `universal - empty s2 returns s1`() {
        assertEquals("a", SmushRules.smushUniversal("a", "", hb))
    }

    @Test
    fun `universal - s2 equals hardblank and s1 not space returns s1`() {
        assertEquals("a", SmushRules.smushUniversal("a", hb, hb))
    }

    @Test
    fun `universal - s2 equals hardblank and s1 is space returns s2`() {
        assertEquals(hb, SmushRules.smushUniversal(" ", hb, hb))
    }

    @Test
    fun `universal - general case returns s2`() {
        assertEquals("b", SmushRules.smushUniversal("a", "b", hb))
    }

    // -- Vertical Rule 1: EQUAL CHARACTER SMUSHING -------------------------

    @Test
    fun `vRule1 - equal chars smush to that char`() {
        assertEquals("|", SmushRules.smushVerticalRule1("|", "|"))
        assertEquals("a", SmushRules.smushVerticalRule1("a", "a"))
        assertEquals("_", SmushRules.smushVerticalRule1("_", "_"))
    }

    @Test
    fun `vRule1 - different chars return empty`() {
        assertEquals("", SmushRules.smushVerticalRule1("|", "/"))
        assertEquals("", SmushRules.smushVerticalRule1("a", "b"))
    }

    @Test
    fun `vRule1 - unlike horizontal rule1, no hardblank guard`() {
        // Vertical rule 1 does NOT special-case hardblanks — by the
        // time vertical smushing runs, hardblanks have been replaced
        // with spaces. Even a literal `$` pair smushes to `$`.
        assertEquals("\$", SmushRules.smushVerticalRule1("\$", "\$"))
    }

    // -- Vertical Rule 2: UNDERSCORE SMUSHING ------------------------------

    @Test
    fun `vRule2 - underscore replaced by every rule char`() {
        for (c in "|/\\[]{}()<>") {
            val s = c.toString()
            assertEquals(s, SmushRules.smushVerticalRule2("_", s), "s1=_ s2=$s")
            assertEquals(s, SmushRules.smushVerticalRule2(s, "_"), "s1=$s s2=_")
        }
    }

    @Test
    fun `vRule2 - non-matching chars return empty`() {
        assertEquals("", SmushRules.smushVerticalRule2("_", "a"))
        assertEquals("", SmushRules.smushVerticalRule2("a", "_"))
        assertEquals("", SmushRules.smushVerticalRule2("a", "b"))
    }

    // -- Vertical Rule 3: HIERARCHY SMUSHING -------------------------------

    @Test
    fun `vRule3 - later class wins`() {
        assertEquals("/", SmushRules.smushVerticalRule3("|", "/"))
        assertEquals("[", SmushRules.smushVerticalRule3("|", "["))
        assertEquals("{", SmushRules.smushVerticalRule3("[", "{"))
        assertEquals("(", SmushRules.smushVerticalRule3("{", "("))
        assertEquals("<", SmushRules.smushVerticalRule3("(", "<"))
    }

    @Test
    fun `vRule3 - same class returns empty`() {
        assertEquals("", SmushRules.smushVerticalRule3("/", "\\"))
        assertEquals("", SmushRules.smushVerticalRule3("[", "]"))
        assertEquals("", SmushRules.smushVerticalRule3("(", ")"))
    }

    @Test
    fun `vRule3 - identical chars return empty`() {
        assertEquals("", SmushRules.smushVerticalRule3("|", "|"))
        assertEquals("", SmushRules.smushVerticalRule3("<", "<"))
    }

    @Test
    fun `vRule3 - char not in rule returns empty`() {
        assertEquals("", SmushRules.smushVerticalRule3("a", "|"))
        assertEquals("", SmushRules.smushVerticalRule3("|", "a"))
    }

    // -- Vertical Rule 4: HORIZONTAL LINE SMUSHING -------------------------

    @Test
    fun `vRule4 - dash over underscore smushes to equals`() {
        assertEquals("=", SmushRules.smushVerticalRule4("-", "_"))
    }

    @Test
    fun `vRule4 - underscore over dash smushes to equals`() {
        assertEquals("=", SmushRules.smushVerticalRule4("_", "-"))
    }

    @Test
    fun `vRule4 - identical pairs return empty (handled by rule 1)`() {
        assertEquals("", SmushRules.smushVerticalRule4("-", "-"))
        assertEquals("", SmushRules.smushVerticalRule4("_", "_"))
    }

    @Test
    fun `vRule4 - unrelated pairs return empty`() {
        assertEquals("", SmushRules.smushVerticalRule4("-", "|"))
        assertEquals("", SmushRules.smushVerticalRule4("a", "b"))
    }

    // -- Vertical Rule 5: VERTICAL LINE SUPERSMUSHING ----------------------

    @Test
    fun `vRule5 - pipe over pipe smushes to pipe (super-smush)`() {
        assertEquals("|", SmushRules.smushVerticalRule5("|", "|"))
    }

    @Test
    fun `vRule5 - any non-pipe pair returns empty`() {
        assertEquals("", SmushRules.smushVerticalRule5("|", "/"))
        assertEquals("", SmushRules.smushVerticalRule5("/", "|"))
        assertEquals("", SmushRules.smushVerticalRule5("a", "a"))
        assertEquals("", SmushRules.smushVerticalRule5("_", "_"))
    }
}

package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * Stage S4 — positive tests for the commonMain `Option` data class.
 *
 * Verifies default values match the Java no-arg constructor,
 * copy() produces distinct instances, and immutability (val fields).
 */
class OptionTest {

    // --- default values ------------------------------------------------

    @Test
    fun `default baseline is null`() {
        assertNull(Option.default().baseline)
    }

    @Test
    fun `default codeTagCount is null`() {
        assertNull(Option.default().codeTagCount)
    }

    @Test
    fun `default rule is null`() {
        assertNull(Option.default().rule)
    }

    @Test
    fun `default fullLayout is null`() {
        assertNull(Option.default().fullLayout)
    }

    @Test
    fun `default hardBlank is null`() {
        assertNull(Option.default().hardBlank)
    }

    @Test
    fun `default height is null`() {
        assertNull(Option.default().height)
    }

    @Test
    fun `default maxLength is null`() {
        assertNull(Option.default().maxLength)
    }

    @Test
    fun `default numCommentLines is null`() {
        assertNull(Option.default().numCommentLines)
    }

    @Test
    fun `default oldLayout is null`() {
        assertNull(Option.default().oldLayout)
    }

    @Test
    fun `default printDirection is null`() {
        assertNull(Option.default().printDirection)
    }

    // --- construction with explicit values -----------------------------

    @Test
    fun `construction with all fields`() {
        val rule = Rule(horizontalLayout = Layout.SMUSH_R, horizontal1 = true)
        val o = Option(
            baseline = 1,
            codeTagCount = 2,
            rule = rule,
            fullLayout = 3,
            hardBlank = "$",
            height = 6,
            maxLength = 80,
            numCommentLines = 4,
            oldLayout = 5,
            printDirection = 0,
        )
        assertEquals(1, o.baseline)
        assertEquals(2, o.codeTagCount)
        assertEquals(rule, o.rule)
        assertEquals(3, o.fullLayout)
        assertEquals("$", o.hardBlank)
        assertEquals(6, o.height)
        assertEquals(80, o.maxLength)
        assertEquals(4, o.numCommentLines)
        assertEquals(5, o.oldLayout)
        assertEquals(0, o.printDirection)
    }

    // --- copy() --------------------------------------------------------

    @Test
    fun `copy produces a distinct instance`() {
        val original = Option.default()
        val copied = original.copy()
        assertNotSame(original, copied)
        assertEquals(original, copied)
    }

    @Test
    fun `copy with changed field only changes that field`() {
        val original = Option(height = 10, hardBlank = "$")
        val modified = original.copy(height = 20)
        assertEquals(10, original.height)
        assertEquals(20, modified.height)
        // everything else unchanged
        assertEquals(original.hardBlank, modified.hardBlank)
        assertEquals(original.baseline, modified.baseline)
        assertEquals(original.rule, modified.rule)
    }

    @Test
    fun `copy with new rule does not mutate original`() {
        val rule1 = Rule(horizontal1 = true)
        val rule2 = Rule(horizontal2 = true)
        val original = Option(rule = rule1)
        val modified = original.copy(rule = rule2)
        assertEquals(rule1, original.rule)
        assertEquals(rule2, modified.rule)
    }

    // --- data class equality -------------------------------------------

    @Test
    fun `equals and hashCode by value`() {
        val a = Option(baseline = 5, height = 8, hardBlank = "#")
        val b = Option(baseline = 5, height = 8, hardBlank = "#")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}

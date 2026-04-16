package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * Stage S4 — positive tests for the commonMain `Rule` data class.
 *
 * Verifies default values match the Java no-arg constructor,
 * copy() produces distinct instances, and immutability (val fields).
 */
class RuleTest {

    // --- default values ------------------------------------------------

    @Test
    fun `default horizontalLayout is null`() {
        assertNull(Rule.default().horizontalLayout)
    }

    @Test
    fun `default verticalLayout is null`() {
        assertNull(Rule.default().verticalLayout)
    }

    @Test
    fun `default horizontal flags are false`() {
        val r = Rule.default()
        assertFalse(r.horizontal1)
        assertFalse(r.horizontal2)
        assertFalse(r.horizontal3)
        assertFalse(r.horizontal4)
        assertFalse(r.horizontal5)
        assertFalse(r.horizontal6)
    }

    @Test
    fun `default vertical flags are false`() {
        val r = Rule.default()
        assertFalse(r.vertical1)
        assertFalse(r.vertical2)
        assertFalse(r.vertical3)
        assertFalse(r.vertical4)
        assertFalse(r.vertical5)
    }

    // --- construction with explicit values -----------------------------

    @Test
    fun `construction with all fields`() {
        val r = Rule(
            horizontalLayout = Layout.SMUSH_R,
            horizontal1 = true,
            horizontal2 = false,
            horizontal3 = true,
            horizontal4 = false,
            horizontal5 = true,
            horizontal6 = false,
            verticalLayout = Layout.FITTED,
            vertical1 = true,
            vertical2 = false,
            vertical3 = true,
            vertical4 = false,
            vertical5 = true,
        )
        assertEquals(Layout.SMUSH_R, r.horizontalLayout)
        assertEquals(true, r.horizontal1)
        assertEquals(false, r.horizontal2)
        assertEquals(true, r.horizontal3)
        assertEquals(false, r.horizontal4)
        assertEquals(true, r.horizontal5)
        assertEquals(false, r.horizontal6)
        assertEquals(Layout.FITTED, r.verticalLayout)
        assertEquals(true, r.vertical1)
        assertEquals(false, r.vertical2)
        assertEquals(true, r.vertical3)
        assertEquals(false, r.vertical4)
        assertEquals(true, r.vertical5)
    }

    // --- copy() --------------------------------------------------------

    @Test
    fun `copy produces a distinct instance`() {
        val original = Rule.default()
        val copied = original.copy()
        assertNotSame(original, copied)
        assertEquals(original, copied)
    }

    @Test
    fun `copy with changed field only changes that field`() {
        val original = Rule.default()
        val modified = original.copy(horizontal3 = true)
        assertFalse(original.horizontal3)
        assertEquals(true, modified.horizontal3)
        // everything else unchanged
        assertEquals(original.horizontalLayout, modified.horizontalLayout)
        assertEquals(original.horizontal1, modified.horizontal1)
        assertEquals(original.horizontal2, modified.horizontal2)
        assertEquals(original.vertical1, modified.vertical1)
    }

    @Test
    fun `copy with changed layout`() {
        val original = Rule(horizontalLayout = Layout.FULL)
        val modified = original.copy(horizontalLayout = Layout.SMUSH_U)
        assertEquals(Layout.FULL, original.horizontalLayout)
        assertEquals(Layout.SMUSH_U, modified.horizontalLayout)
    }

    // --- data class equality -------------------------------------------

    @Test
    fun `equals and hashCode by value`() {
        val a = Rule(horizontalLayout = Layout.DEFAULT, horizontal1 = true)
        val b = Rule(horizontalLayout = Layout.DEFAULT, horizontal1 = true)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}

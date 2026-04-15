package io.leego.banana.coverage

import io.leego.banana.Layout
import io.leego.banana.Rule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the individual `setHorizontalN` mutators which are not called from
 * production code (it only ever uses the bulk [Rule.setHorizontal]).
 */
class RuleCoverageTest {

    @Test
    fun `no-arg constructor yields all defaults`() {
        val r = Rule()
        assertNull(r.horizontalLayout)
        assertNull(r.verticalLayout)
        assertFalse(r.isHorizontal1)
        assertFalse(r.isHorizontal2)
        assertFalse(r.isHorizontal3)
        assertFalse(r.isHorizontal4)
        assertFalse(r.isHorizontal5)
        assertFalse(r.isHorizontal6)
        assertFalse(r.isVertical1)
        assertFalse(r.isVertical2)
        assertFalse(r.isVertical3)
        assertFalse(r.isVertical4)
        assertFalse(r.isVertical5)
    }

    @Test
    fun `individual horizontal setters flip matching flags`() {
        val r = Rule()
        r.isHorizontal1 = true
        r.isHorizontal2 = true
        r.isHorizontal3 = true
        r.isHorizontal4 = true
        r.isHorizontal5 = true
        r.isHorizontal6 = true
        assertTrue(r.isHorizontal1)
        assertTrue(r.isHorizontal2)
        assertTrue(r.isHorizontal3)
        assertTrue(r.isHorizontal4)
        assertTrue(r.isHorizontal5)
        assertTrue(r.isHorizontal6)
        r.isHorizontal1 = false
        r.isHorizontal2 = false
        r.isHorizontal3 = false
        r.isHorizontal4 = false
        r.isHorizontal5 = false
        r.isHorizontal6 = false
        assertFalse(r.isHorizontal1)
        assertFalse(r.isHorizontal2)
        assertFalse(r.isHorizontal3)
        assertFalse(r.isHorizontal4)
        assertFalse(r.isHorizontal5)
        assertFalse(r.isHorizontal6)
    }

    @Test
    fun `copy duplicates every field`() {
        val r = Rule(
            Layout.SMUSH_R, true, true, true, true, true, true,
            Layout.SMUSH_U, true, true, true, true, true,
        )
        val c = r.copy()
        assertEquals(r.horizontalLayout, c.horizontalLayout)
        assertEquals(r.verticalLayout, c.verticalLayout)
        assertTrue(c.isHorizontal1 && c.isHorizontal2 && c.isHorizontal3)
        assertTrue(c.isHorizontal4 && c.isHorizontal5 && c.isHorizontal6)
        assertTrue(c.isVertical1 && c.isVertical2 && c.isVertical3)
        assertTrue(c.isVertical4 && c.isVertical5)
    }
}

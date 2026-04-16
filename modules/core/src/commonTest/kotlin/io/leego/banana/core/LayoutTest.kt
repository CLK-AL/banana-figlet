package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Stage S4 — positive tests for the commonMain `Layout` port.
 *
 * The jvmTest side-car `LayoutJvmParityTest` re-runs the SAME codes
 * through the frozen Java `io.leego.banana.Layout.get` and asserts
 * both implementations return the same value.
 */
class LayoutTest {

    @Test
    fun `get null returns null`() {
        assertNull(Layout.get(null))
    }

    @Test
    fun `get -1 returns DEFAULT`() {
        assertEquals(Layout.DEFAULT, Layout.get(-1))
    }

    @Test
    fun `get 0 returns FULL`() {
        assertEquals(Layout.FULL, Layout.get(0))
    }

    @Test
    fun `get 1 returns FITTED`() {
        assertEquals(Layout.FITTED, Layout.get(1))
    }

    @Test
    fun `get 2 returns SMUSH_U`() {
        assertEquals(Layout.SMUSH_U, Layout.get(2))
    }

    @Test
    fun `get 3 returns SMUSH_R`() {
        assertEquals(Layout.SMUSH_R, Layout.get(3))
    }

    @Test
    fun `get 999 returns null`() {
        assertNull(Layout.get(999))
    }

    // --- code round-trip --------------------------------------------
    // Kotlin's `val code` generates `getCode()` on the JVM, matching
    // the frozen Java Layout.getCode() signature byte-for-byte.

    @Test
    fun `code matches wire format`() {
        assertEquals(-1, Layout.DEFAULT.code)
        assertEquals(0, Layout.FULL.code)
        assertEquals(1, Layout.FITTED.code)
        assertEquals(2, Layout.SMUSH_U.code)
        assertEquals(3, Layout.SMUSH_R.code)
    }
}

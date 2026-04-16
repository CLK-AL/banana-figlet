package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 differential-parity gate for `Layout.get`.
 *
 * For every code — known and unknown, including `null` — the
 * commonMain `Layout.get` and the frozen-Java
 * `JavaLegacyAdapter.lookupViaJava` must return the same value.
 *
 * This test is what turns the KMP port from "a rewrite" into
 * "a certified byte-compatible replacement".
 */
class LayoutJvmParityTest {

    @Test
    fun `null input — parity`() = assertParity(null)

    @Test
    fun `code -1 (DEFAULT) — parity`() = assertParity(-1)

    @Test
    fun `code 0 (FULL) — parity`() = assertParity(0)

    @Test
    fun `code 1 (FITTED) — parity`() = assertParity(1)

    @Test
    fun `code 2 (SMUSH_U) — parity`() = assertParity(2)

    @Test
    fun `code 3 (SMUSH_R) — parity`() = assertParity(3)

    @Test
    fun `unknown positive code — parity`() = assertParity(999)

    @Test
    fun `unknown negative code — parity`() = assertParity(-42)

    @Test
    fun `boundary Int MIN_VALUE — parity`() = assertParity(Int.MIN_VALUE)

    @Test
    fun `boundary Int MAX_VALUE — parity`() = assertParity(Int.MAX_VALUE)

    /** Drive both paths and assert byte-exact equality of the result. */
    private fun assertParity(code: Int?) {
        val k: Layout? = Layout.get(code)
        val j: Layout? = JavaLegacyAdapter.lookupViaJava(code)
        assertEquals(j, k, "parity mismatch for code=$code: kotlin=$k java=$j")
    }
}

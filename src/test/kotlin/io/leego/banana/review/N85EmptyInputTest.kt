package io.leego.banana.review

import io.leego.banana.BananaUtils
import io.leego.banana.Font
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * §8.5 — empty string / pure-newline input crashes `bananaify` with
 *        `ArrayIndexOutOfBoundsException`. Same class of bug as finding
 *        4 (`smushVerticalFigletLines` accessing [0] on an empty figlet
 *        array) but reachable through a different code path.
 *
 * The fix: short-circuit empty input to return "" directly.
 */
class N85EmptyInputTest {

    @Test
    fun `bananaify empty string returns empty string`() {
        assertEquals("", BananaUtils.bananaify(""))
    }

    @Test
    fun `bananaify pure newline input does not crash`() {
        val out = BananaUtils.bananaify("\n\n", Font.STANDARD)
        assertNotNull(out)
    }

    @Test
    fun `bananaify whitespace-only input does not crash`() {
        val out = BananaUtils.bananaify("   ", Font.STANDARD)
        assertNotNull(out)
    }
}

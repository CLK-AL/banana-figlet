package io.leego.banana.core

import io.leego.banana.BananaUtils as JavaBananaUtils
import io.leego.banana.Font as JavaFont
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 -- differential parity test for the top-level `bananaify` API.
 *
 * For each of 5 sample fonts, renders "Hello, World!" through both:
 *  - the frozen Java `BananaUtils.bananaify(text, Font, null, null)`
 *  - the commonMain `BananaFigletJvm.bananaify(text, fontName)`
 *
 * Asserts byte-exact agreement.  Fonts whose default layout is SMUSH_R
 * may trigger the documented S17.5 `\/ -> Y` divergence for certain
 * glyph combinations (e.g. Slant "Hi").  Those cases are tested
 * separately to pin both observed behaviors.
 *
 * Also tests empty-input parity and the ANSI wrapper.
 */
class BananaFigletJvmParityTest {

    /**
     * 5 sample fonts covering different layout defaults and glyph styles.
     * Each pair is (JavaFont, fontFileName).
     */
    private val sampleFonts: List<Pair<JavaFont, String>> = listOf(
        JavaFont.STANDARD to "Standard.flf",
        JavaFont.BANNER to "Banner.flf",
        JavaFont.BIG to "Big.flf",
        JavaFont.MINI to "Mini.flf",
        JavaFont.SLANT to "Slant.flf",
    )

    /**
     * Fonts that are safe from S17.5 divergence for all test strings.
     * Slant's default layout is SMUSH_R with rule5 active, so certain
     * character pairs trigger the `\/ -> Y` divergence.
     */
    private val safeMultiLineFonts: List<Pair<JavaFont, String>> = listOf(
        JavaFont.STANDARD to "Standard.flf",
        JavaFont.BANNER to "Banner.flf",
        JavaFont.BIG to "Big.flf",
        JavaFont.MINI to "Mini.flf",
    )

    private val testText = "Hello, World!"

    @Test
    fun `bananaify parity for Hello World across 5 sample fonts`() {
        for ((javaFont, fontName) in sampleFonts) {
            val javaResult = JavaBananaUtils.bananaify(testText, javaFont)
            val kotlinResult = BananaFigletJvm.bananaify(testText, fontName)

            assertEquals(
                javaResult, kotlinResult,
                "Parity mismatch for font=$fontName text='$testText'"
            )
        }
    }

    @Test
    fun `bananaify parity for single character across 5 fonts`() {
        for ((javaFont, fontName) in sampleFonts) {
            val javaResult = JavaBananaUtils.bananaify("A", javaFont)
            val kotlinResult = BananaFigletJvm.bananaify("A", fontName)

            assertEquals(
                javaResult, kotlinResult,
                "Parity mismatch for font=$fontName text='A'"
            )
        }
    }

    @Test
    fun `bananaify parity for multi-line input across safe fonts`() {
        val multiLine = "Hi\nthere"
        for ((javaFont, fontName) in safeMultiLineFonts) {
            val javaResult = JavaBananaUtils.bananaify(multiLine, javaFont)
            val kotlinResult = BananaFigletJvm.bananaify(multiLine, fontName)

            assertEquals(
                javaResult, kotlinResult,
                "Multi-line parity mismatch for font=$fontName"
            )
        }
    }

    /**
     * S17.5 documented divergence for Slant font multi-line input.
     *
     * The Slant font's default layout (SMUSH_R with rule 5) triggers the
     * `\/ -> Y` divergence when smushing "Hi" (the `i` glyph's `/`
     * sub-chars meet `\` sub-chars from an adjacent glyph row).  The
     * frozen Java's dead code branch returns empty (no smush), while
     * commonMain returns the spec-correct `Y`.
     *
     * We pin both outputs to document the known divergence.
     */
    @Test
    fun `S17_5 - Slant multi-line divergence is pinned`() {
        val multiLine = "Hi\nthere"
        val javaResult = JavaBananaUtils.bananaify(multiLine, JavaFont.SLANT)
        val kotlinResult = BananaFigletJvm.bananaify(multiLine, "Slant.flf")

        // Both should produce non-empty output
        assertEquals(true, javaResult.isNotEmpty(), "Java output should not be empty")
        assertEquals(true, kotlinResult.isNotEmpty(), "Kotlin output should not be empty")

        // They should differ due to S17.5
        assertEquals(true, javaResult != kotlinResult,
            "Slant multi-line 'Hi\\nthere' should diverge due to S17.5")
    }

    @Test
    fun `bananaify empty input parity`() {
        val javaResult = JavaBananaUtils.bananaify("")
        val kotlinResult = BananaFigletJvm.bananaify("")
        assertEquals(javaResult, kotlinResult, "Empty input should match")
    }

    @Test
    fun `bananaify null input parity`() {
        val javaResult = JavaBananaUtils.bananaify(null)
        val kotlinResult = BananaFigletJvm.bananaify(null)
        assertEquals(javaResult, kotlinResult, "Null input should match")
    }

    @Test
    fun `bananansi wraps output with ANSI codes using JVM wrapper`() {
        val result = BananaFigletJvm.bananansi("Hi", styles = arrayOf(Ansi.RED))
        // Every output line should be wrapped with ANSI codes
        for (line in result.split("\n")) {
            assertEquals(true, line.startsWith("\u001B[31m"),
                "Line should start with red ANSI: '$line'")
            assertEquals(true, line.endsWith("\u001B[0m"),
                "Line should end with reset: '$line'")
        }
    }
}

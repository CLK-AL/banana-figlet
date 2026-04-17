package io.leego.banana.core

import io.leego.banana.Font as JavaFont
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Stage S4 differential-parity gate for [parseFlfFont].
 *
 * Loads a real `.flf` font (Standard), parses it via both paths
 * (the commonMain Kotlin parser and the frozen Java `BananaUtils.buildMeta`
 * invoked via reflection through [JavaLegacyAdapter]), and asserts that
 * all [Meta] fields match.
 */
class FlfParserJvmParityTest {

    /**
     * Read the Standard.flf font file as a List<String> the same way
     * the Java code does (via Font.getInputStream and the classloader).
     */
    private fun loadFontLines(font: JavaFont): List<String> {
        val lines = mutableListOf<String>()
        font.inputStream.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, font.charset)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    lines.add(line)
                    line = reader.readLine()
                }
            }
        }
        return lines
    }

    @Test
    fun `Standard font - option fields match`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))

        assertEquals(jMeta.option.hardBlank, kMeta.option.hardBlank, "hardBlank")
        assertEquals(jMeta.option.height, kMeta.option.height, "height")
        assertEquals(jMeta.option.baseline, kMeta.option.baseline, "baseline")
        assertEquals(jMeta.option.maxLength, kMeta.option.maxLength, "maxLength")
        assertEquals(jMeta.option.oldLayout, kMeta.option.oldLayout, "oldLayout")
        assertEquals(jMeta.option.numCommentLines, kMeta.option.numCommentLines, "numCommentLines")
        assertEquals(jMeta.option.printDirection, kMeta.option.printDirection, "printDirection")
        assertEquals(jMeta.option.fullLayout, kMeta.option.fullLayout, "fullLayout")
        assertEquals(jMeta.option.codeTagCount, kMeta.option.codeTagCount, "codeTagCount")
    }

    @Test
    fun `Standard font - height matches`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))
        assertEquals(jMeta.height, kMeta.height, "height")
    }

    @Test
    fun `Standard font - comment matches`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))
        assertEquals(jMeta.comment, kMeta.comment, "comment")
    }

    @Test
    fun `Standard font - figletMap glyph count matches`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))
        assertEquals(jMeta.figletMap.size, kMeta.figletMap.size, "figletMap size")
    }

    @Test
    fun `Standard font - figletMap keys match`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))
        assertEquals(jMeta.figletMap.keys, kMeta.figletMap.keys, "figletMap keys")
    }

    @Test
    fun `Standard font - figletMap glyph rows match`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))

        for (code in jMeta.figletMap.keys) {
            val jGlyph = jMeta.figletMap[code]
            val kGlyph = kMeta.figletMap[code]
            assertNotNull(kGlyph, "Missing glyph for code $code in Kotlin parser")
            assertEquals(
                jGlyph!!.size, kGlyph.size,
                "Glyph row count mismatch for code $code"
            )
            for (row in jGlyph.indices) {
                assertEquals(
                    jGlyph[row], kGlyph[row],
                    "Glyph row $row mismatch for code $code"
                )
            }
        }
    }

    @Test
    fun `Standard font - rule matches`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))
        assertEquals(jMeta.option.rule, kMeta.option.rule, "rule")
    }

    @Test
    fun `Standard font - full Meta equality`() {
        val font = JavaFont.STANDARD
        val jMeta = JavaLegacyAdapter.buildMetaViaJava(font)
        val kMeta = parseFlfFont(loadFontLines(font))
        assertEquals(jMeta, kMeta, "Full Meta equality")
    }

    @Test
    fun `figletMap has expected ASCII range`() {
        val font = JavaFont.STANDARD
        val kMeta = parseFlfFont(loadFontLines(font))
        // Standard font should have all printable ASCII
        for (code in 32..126) {
            assertTrue(code in kMeta.figletMap, "Missing glyph for code $code")
        }
    }
}

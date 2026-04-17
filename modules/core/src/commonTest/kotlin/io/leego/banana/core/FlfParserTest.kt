package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Stage S4 — commonMain tests for [parseFlfFont].
 *
 * Uses minimal, hand-crafted `.flf` content supplied as `listOf(...)`.
 * Covers: valid header parsing, short-header rejection (C1),
 * truncated glyph rows (C2), and endmark-only rows (C3).
 */
class FlfParserTest {

    // -- helpers ----------------------------------------------------------

    /**
     * Build a minimal valid FLF file with the Standard font's header format.
     * Two characters (space=32, '!'=33), each with [height] rows.
     * Each glyph row ends with the endmark '@'.
     */
    private fun minimalFlf(
        height: Int = 2,
        commentLines: Int = 1,
        oldLayout: Int = 15,
        fullLayout: Int? = 24463,
    ): List<String> = buildList {
        val fl = if (fullLayout != null) " $fullLayout" else ""
        // Header: "flf2a$ <height> <baseline> <maxLen> <oldLayout> <commentLines> 0<fullLayout>"
        add("flf2a\$ $height 4 16 $oldLayout $commentLines 0$fl")
        // Comment lines
        repeat(commentLines) { add("Test comment line ${it + 1}") }
        // Glyph rows for space (32): empty content + endmark
        repeat(height) { add(" @") }
        // Glyph rows for '!' (33): "!" content + endmark
        repeat(height) { add("!@") }
    }

    // -- valid header parsing ---------------------------------------------

    @Test
    fun `valid header parses hardblank`() {
        val meta = parseFlfFont(minimalFlf())
        assertEquals("$", meta.option.hardBlank)
    }

    @Test
    fun `valid header parses height`() {
        val meta = parseFlfFont(minimalFlf(height = 6))
        assertEquals(6, meta.height)
        assertEquals(6, meta.option.height)
    }

    @Test
    fun `valid header parses oldLayout`() {
        val meta = parseFlfFont(minimalFlf(oldLayout = 15))
        assertEquals(15, meta.option.oldLayout)
    }

    @Test
    fun `valid header parses fullLayout`() {
        val meta = parseFlfFont(minimalFlf(fullLayout = 24463))
        assertEquals(24463, meta.option.fullLayout)
    }

    @Test
    fun `valid header with missing fullLayout defaults to null`() {
        val lines = listOf(
            "flf2a\$ 2 4 16 15 1 0",
            "Comment",
            " @",
            " @",
        )
        val meta = parseFlfFont(lines)
        assertEquals(null, meta.option.fullLayout)
    }

    @Test
    fun `comment lines are collected`() {
        val meta = parseFlfFont(minimalFlf(commentLines = 2))
        assertTrue(meta.comment.contains("Test comment line 1"))
        assertTrue(meta.comment.contains("Test comment line 2"))
    }

    @Test
    fun `printDirection defaults to 0`() {
        val meta = parseFlfFont(minimalFlf())
        assertEquals(0, meta.option.printDirection)
    }

    @Test
    fun `rule is computed from layout values`() {
        val meta = parseFlfFont(minimalFlf(oldLayout = 15, fullLayout = 24463))
        assertNotNull(meta.option.rule)
    }

    // -- glyph map --------------------------------------------------------

    @Test
    fun `space glyph is parsed`() {
        val meta = parseFlfFont(minimalFlf())
        val spaceGlyph = meta.figletMap[32]
        assertNotNull(spaceGlyph)
        assertEquals(2, spaceGlyph.size)
        // " @" -> endmark '@' stripped -> " " (one space)
        assertEquals(" ", spaceGlyph[0])
    }

    @Test
    fun `exclamation glyph is parsed`() {
        val meta = parseFlfFont(minimalFlf())
        val bangGlyph = meta.figletMap[33]
        assertNotNull(bangGlyph)
        assertEquals(2, bangGlyph.size)
        // "!@" -> endmark '@' stripped -> "!"
        assertEquals("!", bangGlyph[0])
    }

    // -- C1: short header throws ------------------------------------------

    @Test
    fun `C1 - header with fewer than 6 tokens throws`() {
        val lines = listOf("flf2a\$ 6 5 16 15") // only 5 tokens after split
        assertFailsWith<IllegalArgumentException> {
            parseFlfFont(lines)
        }
    }

    @Test
    fun `C1 - header first token shorter than 6 chars throws`() {
        val lines = listOf("flf2 6 5 16 15 13 0")
        assertFailsWith<IllegalArgumentException> {
            parseFlfFont(lines)
        }
    }

    @Test
    fun `C1 - empty data throws`() {
        assertFailsWith<IllegalArgumentException> {
            parseFlfFont(emptyList())
        }
    }

    // -- C2: truncated glyph rows -----------------------------------------

    @Test
    fun `C2 - truncated glyph data stops gracefully`() {
        // Header says height=3 but only 2 glyph rows present for first char
        val lines = listOf(
            "flf2a\$ 3 4 16 15 1 0 24463",
            "Comment",
            " @",
            " @",
            // missing 3rd row for space glyph
        )
        val meta = parseFlfFont(lines)
        // The incomplete glyph should be discarded
        assertTrue(meta.figletMap.isEmpty() || meta.figletMap[32] == null)
    }

    @Test
    fun `C2 - complete glyphs before truncation are kept`() {
        val lines = listOf(
            "flf2a\$ 2 4 16 15 1 0 24463",
            "Comment",
            // space glyph — complete (2 rows)
            " @",
            " @",
            // '!' glyph — only 1 row (truncated)
            "!@",
        )
        val meta = parseFlfFont(lines)
        // Space should be present, '!' should be discarded
        assertNotNull(meta.figletMap[32])
        assertEquals(null, meta.figletMap[33])
    }

    // -- C3: endmark-only rows produce empty strings ----------------------

    @Test
    fun `C3 - endmark-only row produces empty string`() {
        val lines = listOf(
            "flf2a\$ 2 4 16 15 1 0 24463",
            "Comment",
            // space glyph: first row is endmark-only "@@"
            "@@",
            "x@",
        )
        val meta = parseFlfFont(lines)
        val glyph = meta.figletMap[32]
        assertNotNull(glyph)
        // "@@" -> endmark '@', strip all '@' -> ""
        assertEquals("", glyph[0])
        // "x@" -> endmark '@', strip -> "x"
        assertEquals("x", glyph[1])
    }

    @Test
    fun `C3 - double endmark row produces empty string`() {
        val lines = listOf(
            "flf2a\$ 1 4 16 15 1 0 24463",
            "Comment",
            "##", // single-height glyph, endmark '#', content empty
        )
        val meta = parseFlfFont(lines)
        val glyph = meta.figletMap[32]
        assertNotNull(glyph)
        assertEquals("", glyph[0])
    }

    // -- Standard font header format --------------------------------------

    @Test
    fun `Standard font header format parses correctly`() {
        // Exact header from Standard.flf
        val lines = buildList {
            add("flf2a\$ 6 5 16 15 13 0 24463 229")
            repeat(13) { add("Comment line ${it + 1}") }
            // Provide enough glyph rows for at least one character (space=32)
            repeat(6) { add(" @") }
        }
        val meta = parseFlfFont(lines)
        assertEquals("$", meta.option.hardBlank)
        assertEquals(6, meta.height)
        assertEquals(5, meta.option.baseline)
        assertEquals(16, meta.option.maxLength)
        assertEquals(15, meta.option.oldLayout)
        assertEquals(13, meta.option.numCommentLines)
        assertEquals(0, meta.option.printDirection)
        assertEquals(24463, meta.option.fullLayout)
        assertEquals(229, meta.option.codeTagCount)
    }
}

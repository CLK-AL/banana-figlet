package io.leego.banana.core

/**
 * Stage S4 — pure-function port of `BananaUtils.generateFigletLine`
 * plus its supporting horizontal-smush pipeline.
 *
 * Takes a text string + figlet glyph map + [Option], and produces the
 * rendered figlet rows.  No I/O, no `java.*` usage, no font loading —
 * the caller supplies the `figletMap` (from [parseFlfFont] or elsewhere).
 *
 * Vertical smushing (`smushVerticalFigletLines` et al.) is deliberately
 * out of scope for this stage — it lands in a follow-up round.
 *
 * Carries all C1–C5 / §8.5 fixes from the frozen Java tree.  §17.5
 * (the `\\/ → Y` dead-code fix) lives in [SmushRules.smushHorizontalRule5]
 * and is reachable from this renderer.
 */
public object FigletRenderer {

    private const val EMPTY = ""
    private const val WHITESPACE = " "

    /**
     * Render [text] through the given [figletMap] and [option],
     * producing the rendered figlet rows with hardblanks already
     * replaced by spaces.
     *
     * @param text the text to render
     * @param figletMap codepoint → list of glyph rows (as from [parseFlfFont])
     * @param option the rendering [Option] (rule, height, hardBlank must be set)
     * @return the rendered figlet as a list of `height` row strings
     */
    public fun generateLine(
        text: String,
        figletMap: Map<Int, List<String>>,
        option: Option,
    ): List<String> {
        val height = requireNotNull(option.height) { "Option.height must be set." }
        val rule = requireNotNull(option.rule) { "Option.rule must be set." }
        val hardBlank = option.hardBlank

        var output: List<String> = List(height) { EMPTY }

        for (index in text.indices) {
            val code = text[index].code
            val figlet = figletMap[code] ?: continue

            if (figlet.size < height) {
                throw IllegalArgumentException(
                    "Malformed font: glyph for character '${text[index]}' (code $code) " +
                        "has ${figlet.size} rows but font height is $height."
                )
            }
            for (i in 0 until height) {
                // parity with Java null-row guard; List<String> in Kotlin
                // cannot hold null, but we still defend against under-height
                // glyphs which is the meaningful failure mode here.
                @Suppress("SENSELESS_COMPARISON")
                if (figlet[i] == null) {
                    throw IllegalArgumentException(
                        "Malformed font: glyph for character '${text[index]}' (code $code) " +
                            "is missing row $i of $height."
                    )
                }
            }

            val overlap: Int = if (rule.horizontalLayout != Layout.FULL) {
                var length = Int.MAX_VALUE
                for (i in 0 until height) {
                    length = kotlin.math.min(
                        length,
                        getHorizontalSmushLength(output[i], figlet[i], option),
                    )
                }
                if (length == Int.MAX_VALUE) 0 else length
            } else {
                0
            }

            output = smushHorizontal(output, figlet, overlap, option)
        }

        // Hardblank -> space
        return if (hardBlank != null) {
            output.map { it.replace(hardBlank, WHITESPACE) }
        } else {
            output
        }
    }

    /**
     * Horizontally smush [figlet1] (the accumulated output) with [figlet2]
     * (the next glyph) given a pre-computed [overlap] column count.
     * Returns a fresh list of rows.
     */
    private fun smushHorizontal(
        figlet1: List<String>,
        figlet2: List<String>,
        overlap: Int,
        option: Option,
    ): List<String> {
        val height = option.height!!
        val rule = option.rule!!
        val hardBlank = option.hardBlank
        val result = ArrayList<String>(height)

        for (i in 0 until height) {
            val text1 = figlet1[i]
            val text2 = figlet2[i]
            val len1 = text1.length
            val len2 = text2.length
            val overlapStart = len1 - overlap
            val piece = StringBuilder()
            piece.append(substr(text1, 0, kotlin.math.max(0, overlapStart)))

            val seg1 = substr(text1, kotlin.math.max(0, len1 - overlap), overlap)
            val seg2 = substr(text2, 0, kotlin.math.min(overlap, len2))

            for (j in 0 until overlap) {
                val ch1 = if (j < len1) substr(seg1, j, 1) else WHITESPACE
                val ch2 = if (j < len2) substr(seg2, j, 1) else WHITESPACE
                if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                    when (rule.horizontalLayout) {
                        Layout.FITTED ->
                            piece.append(SmushRules.smushUniversal(ch1, ch2, hardBlank))
                        Layout.SMUSH_U ->
                            piece.append(SmushRules.smushUniversal(ch1, ch2, hardBlank))
                        else -> {
                            var nextCh = EMPTY
                            if (nextCh.isEmpty() && rule.horizontal1)
                                nextCh = SmushRules.smushHorizontalRule1(ch1, ch2, hardBlank)
                            if (nextCh.isEmpty() && rule.horizontal2)
                                nextCh = SmushRules.smushHorizontalRule2(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal3)
                                nextCh = SmushRules.smushHorizontalRule3(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal4)
                                nextCh = SmushRules.smushHorizontalRule4(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal5)
                                nextCh = SmushRules.smushHorizontalRule5(ch1, ch2)
                            if (nextCh.isEmpty() && rule.horizontal6)
                                nextCh = SmushRules.smushHorizontalRule6(ch1, ch2, hardBlank)
                            if (nextCh.isEmpty())
                                nextCh = SmushRules.smushUniversal(ch1, ch2, hardBlank)
                            piece.append(nextCh)
                        }
                    }
                } else {
                    piece.append(SmushRules.smushUniversal(ch1, ch2, hardBlank))
                }
            }
            if (overlap < len2) {
                piece.append(substr(text2, overlap, kotlin.math.max(0, len2 - overlap)))
            }
            result.add(piece.toString())
        }
        return result
    }

    /**
     * Compute the maximum number of columns by which [text2] can be
     * slid left into [text1] without violating the active smush rules.
     */
    private fun getHorizontalSmushLength(
        text1: String,
        text2: String,
        option: Option,
    ): Int {
        val rule = option.rule!!
        val hardBlank = option.hardBlank
        if (rule.horizontalLayout == Layout.FULL) {
            return 0
        }
        val len1 = text1.length
        val len2 = text2.length
        var curDist = 1
        val maxDist = text1.length
        var breakAfter = false
        if (len1 == 0) {
            return 0
        }
        while (curDist <= maxDist) {
            var skip = false
            val seg1 = substr(text1, len1 - curDist, len1)
            val seg2 = substr(text2, 0, kotlin.math.min(curDist, len2))
            val iters = kotlin.math.min(curDist, len2)
            for (i in 0 until iters) {
                val ch1 = substr(seg1, i, 1)
                val ch2 = substr(seg2, i, 1)
                if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                    when (rule.horizontalLayout) {
                        Layout.FITTED -> {
                            curDist -= 1
                            skip = true
                        }
                        Layout.SMUSH_U -> {
                            if (ch1 == hardBlank || ch2 == hardBlank) {
                                curDist -= 1
                            }
                            skip = true
                        }
                        else -> {
                            breakAfter = true
                            var validSmush = false
                            if (rule.horizontal1)
                                validSmush = SmushRules.smushHorizontalRule1(ch1, ch2, hardBlank).isNotEmpty()
                            if (!validSmush && rule.horizontal2)
                                validSmush = SmushRules.smushHorizontalRule2(ch1, ch2).isNotEmpty()
                            if (!validSmush && rule.horizontal3)
                                validSmush = SmushRules.smushHorizontalRule3(ch1, ch2).isNotEmpty()
                            if (!validSmush && rule.horizontal4)
                                validSmush = SmushRules.smushHorizontalRule4(ch1, ch2).isNotEmpty()
                            if (!validSmush && rule.horizontal5)
                                validSmush = SmushRules.smushHorizontalRule5(ch1, ch2).isNotEmpty()
                            if (!validSmush && rule.horizontal6)
                                validSmush = SmushRules.smushHorizontalRule6(ch1, ch2, hardBlank).isNotEmpty()
                            if (!validSmush) {
                                curDist -= 1
                                skip = true
                            }
                        }
                    }
                    if (skip) break
                }
            }
            if (skip) break
            if (breakAfter) break
            curDist++
        }
        return kotlin.math.min(maxDist, curDist)
    }

    /**
     * Java-style substring: returns up to [length] chars of [s]
     * starting at [start], clamped to the string length. Returns
     * empty if [s] is empty.
     */
    private fun substr(s: String, start: Int, length: Int): String {
        if (s.isEmpty()) return EMPTY
        val end = kotlin.math.min(start + length, s.length)
        if (start < 0 || start > end) return EMPTY
        return s.substring(start, end)
    }
}

package io.leego.banana.core

/**
 * Stage S4 — pure-function port of `BananaUtils.generateFigletLine`
 * plus its supporting horizontal-smush pipeline.
 *
 * Takes a text string + figlet glyph map + [Option], and produces the
 * rendered figlet rows.  No I/O, no `java.*` usage, no font loading —
 * the caller supplies the `figletMap` (from [parseFlfFont] or elsewhere).
 *
 * Also hosts the vertical smushing pipeline — `combineVertically`
 * glues two or more rendered figlet line arrays vertically using the
 * same rule engine as the frozen Java `smushVerticalFigletLines`.
 *
 * Carries all C1–C5 / §8.5 fixes from the frozen Java tree.  §17.5
 * (the `\\/ → Y` dead-code fix) lives in [SmushRules.smushHorizontalRule5]
 * and is reachable from this renderer.  The vertical rules do NOT have
 * the §17.5 issue and so port byte-exact from the frozen Java.
 */
public object FigletRenderer {

    private const val EMPTY = ""
    private const val WHITESPACE = " "

    // Vertical-smush predicate return codes (mirrors the frozen Java
    // constants BananaUtils.INVALID/VALID/END).
    private const val INVALID = 0
    private const val VALID = 1
    private const val END = 2

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
     * Glue multiple rendered figlet-line arrays vertically into a
     * single output, smushing adjacent pairs per the rule engine.
     *
     * Mirrors the top-level accumulator loop in
     * `BananaUtils.generateFiglet`:
     * ```
     * String[] output = figletLines[0];
     * for (int i = 1; i < figletLines.length; i++) {
     *     output = smushVerticalFigletLines(output, figletLines[i], option);
     * }
     * ```
     *
     * Carries C4 (empty-array guards in [smushVerticalFigletLines])
     * and C5 (curDist cap in [getVerticalSmushDist]).
     *
     * @param figletLines per-input-line rendered figlet rows (as
     *   produced by [generateLine]).  May be empty.
     * @param option the [Option] whose `rule.verticalLayout` / vertical
     *   rule toggles drive the smushing.
     * @return the combined output rows. Empty list if [figletLines] is
     *   empty.  Equal to `figletLines[0]` when only one entry.
     */
    public fun combineVertically(
        figletLines: List<List<String>>,
        option: Option,
    ): List<String> {
        if (figletLines.isEmpty()) return emptyList()
        var output: List<String> = figletLines[0]
        for (i in 1 until figletLines.size) {
            output = smushVerticalFigletLines(output, figletLines[i], option)
        }
        return output
    }

    /**
     * Port of `BananaUtils.smushVerticalFigletLines` — pads the two
     * rendered figlet arrays to equal width, computes the overlap, and
     * merges them via [smushVertical].  Carries C4 (empty-array guard).
     */
    internal fun smushVerticalFigletLines(
        figlet1: List<String>,
        figlet2: List<String>,
        option: Option,
    ): List<String> {
        // C4: short-circuit on empty inputs so we never index [0] on an
        // empty array below.
        if (figlet1.isEmpty()) return figlet2
        if (figlet2.isEmpty()) return figlet1

        val len1 = figlet1[0].length
        val len2 = figlet2[0].length

        val padded1: List<String>
        val padded2: List<String>
        when {
            len1 > len2 -> {
                padded1 = figlet1
                padded2 = padLines(figlet2, len1 - len2)
            }
            len2 > len1 -> {
                padded1 = padLines(figlet1, len2 - len1)
                padded2 = figlet2
            }
            else -> {
                padded1 = figlet1
                padded2 = figlet2
            }
        }

        val overlap = getVerticalSmushDist(padded1, padded2, option)
        return smushVertical(padded1, padded2, overlap, option)
    }

    /** Append [numSpaces] spaces to every row of [lines]. */
    private fun padLines(lines: List<String>, numSpaces: Int): List<String> {
        if (numSpaces <= 0) return lines
        val padding = WHITESPACE.repeat(numSpaces)
        return lines.map { it + padding }
    }

    /**
     * Port of `BananaUtils.getVerticalSmushDist` — computes the maximum
     * number of rows by which [figlet2] can be slid up into [figlet1]
     * without violating the active vertical smush rules.
     *
     * C5: the overlap is capped at `min(figlet1.size, figlet2.size)`
     * so we never slice past the shorter one.
     */
    internal fun getVerticalSmushDist(
        figlet1: List<String>,
        figlet2: List<String>,
        option: Option,
    ): Int {
        var curDist = 1
        val maxDist = kotlin.math.min(figlet1.size, figlet2.size)
        val len1 = figlet1.size
        while (curDist <= maxDist) {
            val subLines1 = figlet1.subList(kotlin.math.max(0, len1 - curDist), len1)
            val subLines2 = figlet2.subList(0, kotlin.math.min(maxDist, curDist))
            var result = VALID
            val iters = kotlin.math.min(subLines1.size, subLines2.size)
            for (i in 0 until iters) {
                val ret = canSmushVertical(subLines1[i], subLines2[i], option)
                if (ret == END) {
                    result = ret
                } else if (ret == INVALID) {
                    result = ret
                    break
                }
            }
            if (result == INVALID) {
                curDist--
                break
            }
            if (result == END) {
                break
            }
            curDist++
        }
        return kotlin.math.min(maxDist, curDist)
    }

    /**
     * Port of `BananaUtils.canSmushVertical` — returns:
     *  - [VALID]   — the two lines can be smushed and continue;
     *  - [END]     — they can smush but we're at a stop point;
     *  - [INVALID] — the two lines cannot be smushed.
     */
    internal fun canSmushVertical(
        line1: String,
        line2: String,
        option: Option,
    ): Int {
        val rule = option.rule!!
        if (rule.verticalLayout == Layout.FULL) return INVALID
        val len = kotlin.math.min(line1.length, line2.length)
        if (len == 0) return INVALID

        var endSmush = false
        for (i in 0 until len) {
            val ch1 = substr(line1, i, 1)
            val ch2 = substr(line2, i, 1)
            if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                when (rule.verticalLayout) {
                    Layout.FITTED -> return INVALID
                    Layout.SMUSH_U -> return END
                    else -> {
                        if (SmushRules.smushVerticalRule5(ch1, ch2).isNotEmpty()) {
                            // rule 5 super-smushing permits this pair but
                            // does NOT mark it as an end — continue.
                            continue
                        }
                        var validSmush = false
                        if (rule.vertical1)
                            validSmush = SmushRules.smushVerticalRule1(ch1, ch2).isNotEmpty()
                        if (!validSmush && rule.vertical2)
                            validSmush = SmushRules.smushVerticalRule2(ch1, ch2).isNotEmpty()
                        if (!validSmush && rule.vertical3)
                            validSmush = SmushRules.smushVerticalRule3(ch1, ch2).isNotEmpty()
                        if (!validSmush && rule.vertical4)
                            validSmush = SmushRules.smushVerticalRule4(ch1, ch2).isNotEmpty()
                        endSmush = true
                        if (!validSmush) return INVALID
                    }
                }
            }
        }
        return if (endSmush) END else VALID
    }

    /**
     * Port of `BananaUtils.smushVertical` — glues [figlet1] and [figlet2]
     * with the given [overlap], smushing the overlapping rows.
     */
    private fun smushVertical(
        figlet1: List<String>,
        figlet2: List<String>,
        overlap: Int,
        option: Option,
    ): List<String> {
        val len1 = figlet1.size
        val len2 = figlet2.size
        val piece1 = figlet1.subList(0, kotlin.math.max(0, len1 - overlap))
        val piece21 = figlet1.subList(kotlin.math.max(0, len1 - overlap), len1)
        val piece22 = figlet2.subList(0, kotlin.math.min(overlap, len2))
        val piece2 = ArrayList<String>(piece21.size)
        for (i in piece21.indices) {
            val line = if (i >= len2) piece21[i] else smushVerticalLines(piece21[i], piece22[i], option)
            piece2.add(line)
        }
        val piece3 = figlet2.subList(kotlin.math.min(overlap, len2), len2)
        val result = ArrayList<String>(piece1.size + piece2.size + piece3.size)
        result.addAll(piece1)
        result.addAll(piece2)
        result.addAll(piece3)
        return result
    }

    /**
     * Port of `BananaUtils.smushVerticalLines` — merges two rows.
     * For each column, if both sub-chars are non-whitespace the chosen
     * rule's output is appended; otherwise [SmushRules.smushUniversal]
     * is used.
     */
    internal fun smushVerticalLines(
        line1: String,
        line2: String,
        option: Option,
    ): String {
        val rule = option.rule!!
        val hardBlank = option.hardBlank
        val len = kotlin.math.min(line1.length, line2.length)
        val result = StringBuilder(len)
        for (i in 0 until len) {
            val ch1 = substr(line1, i, 1)
            val ch2 = substr(line2, i, 1)
            if (ch1 != WHITESPACE && ch2 != WHITESPACE) {
                when (rule.verticalLayout) {
                    Layout.FITTED -> result.append(SmushRules.smushUniversal(ch1, ch2, null))
                    Layout.SMUSH_U -> result.append(SmushRules.smushUniversal(ch1, ch2, null))
                    else -> {
                        var validSmush: String = EMPTY
                        if (rule.vertical5)
                            validSmush = SmushRules.smushVerticalRule5(ch1, ch2)
                        if (validSmush.isEmpty() && rule.vertical1)
                            validSmush = SmushRules.smushVerticalRule1(ch1, ch2)
                        if (validSmush.isEmpty() && rule.vertical2)
                            validSmush = SmushRules.smushVerticalRule2(ch1, ch2)
                        if (validSmush.isEmpty() && rule.vertical3)
                            validSmush = SmushRules.smushVerticalRule3(ch1, ch2)
                        if (validSmush.isEmpty() && rule.vertical4)
                            validSmush = SmushRules.smushVerticalRule4(ch1, ch2)
                        result.append(validSmush)
                    }
                }
            } else {
                result.append(SmushRules.smushUniversal(ch1, ch2, hardBlank))
            }
        }
        return result.toString()
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

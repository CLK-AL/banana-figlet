package io.leego.banana.core

/**
 * Stage S4 -- top-level public API for the FIGlet library port.
 *
 * This is the commonMain entry point that ties together [parseFlfFont],
 * [FigletRenderer.generateLine], and [FigletRenderer.combineVertically]
 * into a single `bananaify` call.
 *
 * Platform-neutral: accepts pre-loaded font lines (`List<String>`)
 * so there is no I/O or `java.*` dependency.  Platform-specific
 * convenience wrappers (e.g. JVM `loadFontResource`) delegate here.
 *
 * Carries the S8.5 fix: empty/null input short-circuits to empty string.
 */
public object BananaFiglet {

    private const val EMPTY = ""

    /**
     * Render [text] as FIGlet art using the font described by [fontLines].
     *
     * @param text           the input text (may contain newlines)
     * @param fontLines      all lines of the `.flf` font file
     * @param hLayout        optional horizontal layout override
     * @param vLayout        optional vertical layout override
     * @return the rendered FIGlet string (lines joined with `\n`)
     */
    public fun bananaify(
        text: String?,
        fontLines: List<String>,
        hLayout: Layout? = null,
        vLayout: Layout? = null,
    ): String {
        // S8.5: empty/null input short-circuit
        if (text.isNullOrEmpty()) return EMPTY

        val meta = parseFlfFont(fontLines)
        val option = applyLayouts(meta.option, hLayout, vLayout)

        val inputLines = text.split(Regex("\\r?\\n"))
        // S8.5: Java's split discards trailing empty strings, so an input
        // like "\n\n" may produce a zero-length array after splitting.
        if (inputLines.isEmpty()) return EMPTY

        val figletLines: List<List<String>> = inputLines.map { line ->
            FigletRenderer.generateLine(line, meta.figletMap, option)
        }

        val output = FigletRenderer.combineVertically(figletLines, option)
        if (output.isEmpty()) return EMPTY

        return output.joinToString("\n")
    }

    /**
     * Render [text] as FIGlet art and wrap each output line with ANSI
     * escape codes from [styles].
     *
     * @param text           the input text (may contain newlines)
     * @param fontLines      all lines of the `.flf` font file
     * @param hLayout        optional horizontal layout override
     * @param vLayout        optional vertical layout override
     * @param styles         ANSI styles to apply to each output line
     * @return the rendered, ANSI-wrapped FIGlet string
     */
    public fun bananansi(
        text: String?,
        fontLines: List<String>,
        hLayout: Layout? = null,
        vLayout: Layout? = null,
        vararg styles: Ansi?,
    ): String {
        // S8.5: empty/null input short-circuit
        if (text.isNullOrEmpty()) return EMPTY

        val meta = parseFlfFont(fontLines)
        val option = applyLayouts(meta.option, hLayout, vLayout)

        val inputLines = text.split(Regex("\\r?\\n"))
        if (inputLines.isEmpty()) return EMPTY

        val figletLines: List<List<String>> = inputLines.map { line ->
            FigletRenderer.generateLine(line, meta.figletMap, option)
        }

        val output = FigletRenderer.combineVertically(figletLines, option)
        if (output.isEmpty()) return EMPTY

        return output.joinToString("\n") { line ->
            Ansi.ansify(line, *styles) ?: line
        }
    }

    /**
     * Apply optional horizontal/vertical layout overrides to the parsed
     * [Option], mirroring `BananaUtils.setLayout`.
     */
    internal fun applyLayouts(
        base: Option,
        hLayout: Layout?,
        vLayout: Layout?,
    ): Option {
        if ((hLayout == null || hLayout == Layout.DEFAULT) &&
            (vLayout == null || vLayout == Layout.DEFAULT)
        ) {
            return base
        }
        var option = base
        option = applyHorizontalLayout(option, hLayout)
        option = applyVerticalLayout(option, vLayout)
        return option
    }

    private fun applyHorizontalLayout(option: Option, layout: Layout?): Option {
        if (layout == null || layout == Layout.DEFAULT) return option
        val oldRule = option.rule ?: Rule.default()
        val newRule = when (layout) {
            Layout.FULL -> oldRule.copy(
                horizontalLayout = Layout.FULL,
                horizontal1 = false, horizontal2 = false, horizontal3 = false,
                horizontal4 = false, horizontal5 = false, horizontal6 = false,
            )
            Layout.FITTED -> oldRule.copy(
                horizontalLayout = Layout.FITTED,
                horizontal1 = false, horizontal2 = false, horizontal3 = false,
                horizontal4 = false, horizontal5 = false, horizontal6 = false,
            )
            Layout.SMUSH_U -> oldRule.copy(
                horizontalLayout = Layout.SMUSH_U,
                horizontal1 = false, horizontal2 = false, horizontal3 = false,
                horizontal4 = false, horizontal5 = false, horizontal6 = false,
            )
            Layout.SMUSH_R -> oldRule.copy(
                horizontalLayout = Layout.SMUSH_R,
                horizontal1 = true, horizontal2 = true, horizontal3 = true,
                horizontal4 = true, horizontal5 = true, horizontal6 = true,
            )
            Layout.DEFAULT -> oldRule
        }
        return option.copy(rule = newRule)
    }

    private fun applyVerticalLayout(option: Option, layout: Layout?): Option {
        if (layout == null || layout == Layout.DEFAULT) return option
        val oldRule = option.rule ?: Rule.default()
        val newRule = when (layout) {
            Layout.FULL -> oldRule.copy(
                verticalLayout = Layout.FULL,
                vertical1 = false, vertical2 = false, vertical3 = false,
                vertical4 = false, vertical5 = false,
            )
            Layout.FITTED -> oldRule.copy(
                verticalLayout = Layout.FITTED,
                vertical1 = false, vertical2 = false, vertical3 = false,
                vertical4 = false, vertical5 = false,
            )
            Layout.SMUSH_U -> oldRule.copy(
                verticalLayout = Layout.SMUSH_U,
                vertical1 = false, vertical2 = false, vertical3 = false,
                vertical4 = false, vertical5 = false,
            )
            Layout.SMUSH_R -> oldRule.copy(
                verticalLayout = Layout.SMUSH_R,
                vertical1 = true, vertical2 = true, vertical3 = true,
                vertical4 = true, vertical5 = true,
            )
            Layout.DEFAULT -> oldRule
        }
        return option.copy(rule = newRule)
    }
}

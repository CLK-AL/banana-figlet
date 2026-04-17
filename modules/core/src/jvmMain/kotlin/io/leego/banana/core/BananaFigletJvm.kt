package io.leego.banana.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Stage S4 -- JVM convenience wrappers that mirror the frozen Java
 * `BananaUtils.bananaify(text, Font, hLayout, vLayout)` API.
 *
 * Loads the font resource via [loadFontResource], caches parsed font
 * lines in a thread-safe [ConcurrentHashMap], and delegates to the
 * commonMain [BananaFiglet] for rendering.
 *
 * The font name is the file name (e.g. `"Standard.flf"`) as used in
 * the frozen Java `Font` class.
 */
public object BananaFigletJvm {

    /** Thread-safe cache of font file name -> pre-split lines. */
    private val fontLinesCache = ConcurrentHashMap<String, List<String>>()

    /** Default font file name, matching `Font.STANDARD`. */
    public const val DEFAULT_FONT: String = "Standard.flf"

    /**
     * Load and cache the font lines for the given font file [name].
     */
    private fun getFontLines(name: String): List<String> {
        return fontLinesCache.computeIfAbsent(name) { key ->
            loadFontResource(key).lines()
        }
    }

    /**
     * Render [text] as FIGlet art using the named font resource.
     *
     * @param text      the input text
     * @param fontName  font file name (default: `"Standard.flf"`)
     * @param hLayout   optional horizontal layout override
     * @param vLayout   optional vertical layout override
     */
    public fun bananaify(
        text: String?,
        fontName: String = DEFAULT_FONT,
        hLayout: Layout? = null,
        vLayout: Layout? = null,
    ): String {
        val fontLines = getFontLines(fontName)
        return BananaFiglet.bananaify(text, fontLines, hLayout, vLayout)
    }

    /**
     * Render [text] as FIGlet art with ANSI escape codes.
     *
     * @param text      the input text
     * @param fontName  font file name (default: `"Standard.flf"`)
     * @param hLayout   optional horizontal layout override
     * @param vLayout   optional vertical layout override
     * @param styles    ANSI styles to apply
     */
    public fun bananansi(
        text: String?,
        fontName: String = DEFAULT_FONT,
        hLayout: Layout? = null,
        vLayout: Layout? = null,
        vararg styles: Ansi?,
    ): String {
        val fontLines = getFontLines(fontName)
        return BananaFiglet.bananansi(text, fontLines, hLayout, vLayout, *styles)
    }
}

package io.leego.banana.core

/**
 * Stage S4 — immutable port of the frozen Java `io.leego.banana.Meta`.
 *
 * Holds the parsed FLF font data: the option (header fields + smush rule),
 * the figlet glyph map (codepoint -> list of row strings), the comment
 * block, and the font height (denormalized from option for convenience).
 *
 * Unlike the Java original, this is a `data class` with `val` fields —
 * safe to cache and share without defensive copying (M10 fix).
 */
public data class Meta(
    val option: Option,
    val figletMap: Map<Int, List<String>>,
    val comment: String,
    val height: Int,
)

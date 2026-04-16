package io.leego.banana.core

/**
 * Stage S4 — immutable port of the frozen Java `io.leego.banana.Option`.
 *
 * The Java original is a mutable JavaBean with setters for every field,
 * meaning that the cached `Option` inside `Meta` constitutes a
 * shared-state hazard (CODE_REVIEW.md finding M10).
 *
 * This commonMain port uses `val` fields exclusively, making instances
 * safe to cache and share across threads without defensive copying.
 * Callers use the Kotlin `copy()` idiom to derive modified variants.
 */
public data class Option(
    val baseline: Int? = null,
    val codeTagCount: Int? = null,
    val rule: Rule? = null,
    val fullLayout: Int? = null,
    val hardBlank: String? = null,
    val height: Int? = null,
    val maxLength: Int? = null,
    val numCommentLines: Int? = null,
    val oldLayout: Int? = null,
    val printDirection: Int? = null,
) {
    public companion object {
        /**
         * Returns an `Option` with all defaults matching the Java
         * no-arg constructor: every field `null`.
         */
        public fun default(): Option = Option()
    }
}

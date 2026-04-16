package io.leego.banana.core

/**
 * Stage S4 — immutable port of the frozen Java `io.leego.banana.Rule`.
 *
 * The Java original is a mutable JavaBean with setters for every field.
 * This commonMain port uses `val` fields exclusively, making instances
 * safe to cache and share across threads without defensive copying.
 * Callers use the Kotlin `copy()` idiom to derive modified variants.
 *
 * Addresses CODE_REVIEW.md finding M10 (shared-state hazard in Meta).
 */
public data class Rule(
    val horizontalLayout: Layout? = null,
    val horizontal1: Boolean = false,
    val horizontal2: Boolean = false,
    val horizontal3: Boolean = false,
    val horizontal4: Boolean = false,
    val horizontal5: Boolean = false,
    val horizontal6: Boolean = false,
    val verticalLayout: Layout? = null,
    val vertical1: Boolean = false,
    val vertical2: Boolean = false,
    val vertical3: Boolean = false,
    val vertical4: Boolean = false,
    val vertical5: Boolean = false,
) {
    public companion object {
        /**
         * Returns a `Rule` with all defaults matching the Java
         * no-arg constructor: layouts `null`, all flags `false`.
         */
        public fun default(): Rule = Rule()
    }
}

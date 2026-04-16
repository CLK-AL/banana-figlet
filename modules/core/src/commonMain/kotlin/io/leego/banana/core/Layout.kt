package io.leego.banana.core

/**
 * Stage S4 — proof-of-concept port of the frozen Java `io.leego.banana.Layout`.
 *
 * Mirrors the semantics of the Java enum byte-exact:
 *  - five values, identical names and codes;
 *  - `get(code)` returns `null` on `null` input, returns the enum
 *    whose `code` matches, or `null` when no match;
 *  - `getCode()` returns the original wire-format code.
 *
 * The jvmTest side-car `LayoutJvmParityTest` drives the SAME inputs
 * through this pure-Kotlin port and through the frozen Java enum via
 * [io.leego.banana.core.JavaLegacyAdapter], asserting both return
 * the same value for every code.
 */
public enum class Layout(public val code: Int) {
    DEFAULT(-1),
    FULL(0),
    FITTED(1),
    SMUSH_U(2),
    SMUSH_R(3);

    // Java parity: Kotlin's `public val code: Int` compiles to a
    // `public final int getCode()` JVM method, matching the frozen
    // Java `io.leego.banana.Layout.getCode()` signature exactly.
    // We intentionally do NOT declare a separate `fun getCode()` —
    // that would clash with the auto-generated property getter.

    public companion object {
        /**
         * Look up a `Layout` by its wire-format code. Mirrors
         * `io.leego.banana.Layout.get(Integer)` exactly:
         *  - `null` input returns `null`;
         *  - a matching code returns the corresponding enum constant;
         *  - otherwise returns `null`.
         */
        public fun get(code: Int?): Layout? {
            if (code == null) return null
            for (e in entries) {
                if (code == e.code) return e
            }
            return null
        }
    }
}

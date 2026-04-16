package io.leego.banana.core

import io.leego.banana.Layout as JavaLayout

/**
 * JVM-only adapter that bridges the frozen Java `io.leego.banana.Layout`
 * into the commonMain `Layout` enum. Used by `LayoutJvmParityTest` to
 * drive the same fixtures through both implementations and assert
 * byte-exact agreement.
 *
 * Never becomes commonMain — it exists only for the duration of
 * Stage S4 so `jvmTest` can perform differential-parity checks.
 * Deleted once every frozen-Java type has reached parity with its
 * commonMain port and the legacy Java profile is retired.
 */
public object JavaLegacyAdapter {

    /**
     * Look up a `Layout` via the frozen-Java code path, then map the
     * returned Java enum constant back onto the commonMain `Layout`
     * by name. Accepts the exact same `Integer?`/`null` calling
     * convention as the Java method.
     */
    public fun lookupViaJava(code: Int?): Layout? {
        // Integer auto-boxes from Int? — null passes through unchanged.
        val javaLayout: JavaLayout? = JavaLayout.get(code)
        return javaLayout?.let { Layout.valueOf(it.name) }
    }
}

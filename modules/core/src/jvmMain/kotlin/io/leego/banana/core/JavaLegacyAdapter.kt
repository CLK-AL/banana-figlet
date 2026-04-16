package io.leego.banana.core

import io.leego.banana.Layout as JavaLayout
import io.leego.banana.Option as JavaOption
import io.leego.banana.Rule as JavaRule

/**
 * JVM-only adapter that bridges frozen Java types into their
 * commonMain immutable ports.  Used by `*JvmParityTest` classes
 * to drive the same fixtures through both implementations and
 * assert byte-exact agreement.
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
        val javaLayout: JavaLayout? = JavaLayout.get(code)
        return javaLayout?.let { Layout.valueOf(it.name) }
    }

    /**
     * Map a frozen-Java `Layout` enum constant (nullable) into the
     * commonMain `Layout` enum by name.
     */
    private fun layoutFromJava(javaLayout: JavaLayout?): Layout? {
        return javaLayout?.let { Layout.valueOf(it.name) }
    }

    /**
     * Convert a frozen-Java mutable `Rule` into an immutable
     * commonMain `Rule` data class by reading every getter.
     */
    public fun ruleFromJava(javaRule: JavaRule?): Rule? {
        if (javaRule == null) return null
        return Rule(
            horizontalLayout = layoutFromJava(javaRule.horizontalLayout),
            horizontal1 = javaRule.isHorizontal1,
            horizontal2 = javaRule.isHorizontal2,
            horizontal3 = javaRule.isHorizontal3,
            horizontal4 = javaRule.isHorizontal4,
            horizontal5 = javaRule.isHorizontal5,
            horizontal6 = javaRule.isHorizontal6,
            verticalLayout = layoutFromJava(javaRule.verticalLayout),
            vertical1 = javaRule.isVertical1,
            vertical2 = javaRule.isVertical2,
            vertical3 = javaRule.isVertical3,
            vertical4 = javaRule.isVertical4,
            vertical5 = javaRule.isVertical5,
        )
    }

    /**
     * Convert a frozen-Java mutable `Option` into an immutable
     * commonMain `Option` data class by reading every getter.
     *
     * This is the canonical fix for CODE_REVIEW.md finding M10:
     * the returned `Option` is an immutable `data class` that is
     * safe to cache and share without defensive copying.
     */
    public fun optionFromJava(javaOption: JavaOption?): Option? {
        if (javaOption == null) return null
        return Option(
            baseline = javaOption.baseline,
            codeTagCount = javaOption.codeTagCount,
            rule = ruleFromJava(javaOption.rule),
            fullLayout = javaOption.fullLayout,
            hardBlank = javaOption.hardBlank,
            height = javaOption.height,
            maxLength = javaOption.maxLength,
            numCommentLines = javaOption.numCommentLines,
            oldLayout = javaOption.oldLayout,
            printDirection = javaOption.printDirection,
        )
    }
}

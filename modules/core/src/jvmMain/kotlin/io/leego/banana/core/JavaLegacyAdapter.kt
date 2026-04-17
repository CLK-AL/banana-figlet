package io.leego.banana.core

import io.leego.banana.BananaUtils as JavaBananaUtils
import io.leego.banana.Font as JavaFont
import io.leego.banana.Layout as JavaLayout
import io.leego.banana.Meta as JavaMeta
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

    /**
     * Convert a frozen-Java `Meta` into an immutable commonMain `Meta`.
     * Converts the embedded `Option` and the `figletMap` from
     * `Map<Integer, String[]>` to `Map<Int, List<String>>`.
     */
    public fun metaFromJava(javaMeta: JavaMeta): Meta {
        val option = optionFromJava(javaMeta.option)!!
        val figletMap: Map<Int, List<String>> = javaMeta.figletMap.entries.associate { (k, v) ->
            k.toInt() to v.toList()
        }
        return Meta(
            option = option,
            figletMap = figletMap,
            comment = javaMeta.comment,
            height = option.height ?: 0,
        )
    }

    /**
     * Call the frozen Java `BananaUtils.buildMeta(Font)` private method
     * via reflection and convert the result into a commonMain [Meta].
     *
     * This is used by `FlfParserJvmParityTest` to drive the same font
     * through both the Java and Kotlin parsers and assert parity.
     */
    public fun buildMetaViaJava(font: JavaFont): Meta {
        val method = JavaBananaUtils::class.java.getDeclaredMethod("buildMeta", JavaFont::class.java)
        method.isAccessible = true
        val javaMeta = method.invoke(null, font) as JavaMeta
        return metaFromJava(javaMeta)
    }

    /**
     * Call the frozen Java `BananaUtils.generateFigletLine` private method
     * via reflection and return its output as a commonMain `List<String>`.
     *
     * Used by `FigletRendererJvmParityTest` to drive identical inputs
     * through both renderers and assert byte-exact agreement (modulo the
     * documented §17.5 `\\/ → Y` divergence).
     *
     * @param text the text to render
     * @param javaMeta the frozen-Java [io.leego.banana.Meta] whose
     *   `figletMap` and `option` will be passed into the private method
     */
    public fun generateLineViaJava(text: String, javaMeta: JavaMeta): List<String> {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        val mapClass: Class<*> = java.util.Map::class.java
        val method = JavaBananaUtils::class.java.getDeclaredMethod(
            "generateFigletLine",
            String::class.java,
            mapClass,
            JavaOption::class.java,
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = method.invoke(null, text, javaMeta.figletMap, javaMeta.option) as Array<String>
        return result.toList()
    }
}

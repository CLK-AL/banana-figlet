package io.leego.banana.core

import io.leego.banana.Font as JavaFont
import io.leego.banana.Layout as JavaLayout
import io.leego.banana.Meta as JavaMeta
import io.leego.banana.Option as JavaOption
import io.leego.banana.BananaUtils as JavaBananaUtils
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 differential-parity gate for [FigletRenderer].
 *
 * Drives the same fixtures through:
 *  - the commonMain [FigletRenderer] (+ [parseFlfFont] for the font data);
 *  - the frozen Java `BananaUtils.generateFigletLine` via reflection
 *    through [JavaLegacyAdapter].
 *
 * Asserts byte-exact agreement for a matrix of (text × layout).
 *
 * A dedicated test documents the §17.5 divergence: for the `\\/` sub-char
 * pair the commonMain port now returns `Y` (spec-correct) while the
 * frozen Java still produces the dead-code empty result.  We assert that
 * divergence explicitly as a pinned-behavior test.
 */
class FigletRendererJvmParityTest {

    private val font = JavaFont.STANDARD

    /** Cached frozen-Java `Meta` for the Standard font via reflection. */
    private val javaMeta: JavaMeta by lazy {
        val method = JavaBananaUtils::class.java.getDeclaredMethod("buildMeta", JavaFont::class.java)
        method.isAccessible = true
        method.invoke(null, font) as JavaMeta
    }

    /** Invoke the frozen-Java `setLayout(Option, Layout, Layout)` via reflection. */
    private fun applyLayoutJava(base: JavaOption, hLayout: JavaLayout?): JavaOption {
        val method = JavaBananaUtils::class.java.getDeclaredMethod(
            "setLayout",
            JavaOption::class.java,
            JavaLayout::class.java,
            JavaLayout::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, base, hLayout, null) as JavaOption
    }

    /** Render via the frozen Java path, returning a `List<String>`. */
    private fun renderJava(text: String, javaOption: JavaOption): List<String> {
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
        val result = method.invoke(null, text, javaMeta.figletMap, javaOption) as Array<String>
        return result.toList()
    }

    /** Render via the commonMain path. */
    private fun renderKotlin(text: String, option: Option, figletMap: Map<Int, List<String>>): List<String> {
        return FigletRenderer.generateLine(text, figletMap, option)
    }

    private fun layoutMatrix(): List<Pair<JavaLayout?, Layout?>> = listOf(
        null to null,
        JavaLayout.FULL to Layout.FULL,
        JavaLayout.FITTED to Layout.FITTED,
        JavaLayout.SMUSH_U to Layout.SMUSH_U,
        JavaLayout.SMUSH_R to Layout.SMUSH_R,
    )

    /**
     * Strings that do not trigger the §17.5 `\\/ → Y` divergence.
     * `<>` is deliberately excluded from the parity matrix: the Standard
     * font's `<` / `>` glyphs contain a `\` / `/` sub-char pair which,
     * when smushed at SMUSH_R, hits the commonMain-vs-Java divergence
     * and is covered by the dedicated §17.5 test below.
     */
    private val testStrings = listOf(
        "Hi",
        "A",
        "Hello",
        "abc",
        "The quick",
        "123",
        "||",
    )

    @Test
    fun `commonMain and Java render identically for Standard font across layout matrix`() {
        val kMeta = JavaLegacyAdapter.metaFromJava(javaMeta)

        for ((jLayout, kLayout) in layoutMatrix()) {
            val javaOption = applyLayoutJava(javaMeta.option, jLayout)
            val kotlinOption = kotlinOptionWithLayout(kMeta.option, kLayout)

            for (text in testStrings) {
                val jRows = renderJava(text, javaOption)
                val kRows = renderKotlin(text, kotlinOption, kMeta.figletMap)
                assertEquals(
                    jRows, kRows,
                    "Parity mismatch for text='$text' layout=$jLayout",
                )
            }
        }
    }

    /**
     * Replicate the frozen Java `setLayout`/`setHorizontalLayout` behavior
     * on a commonMain [Option] — returns a copy with the requested
     * horizontal layout applied (vertical untouched).
     */
    private fun kotlinOptionWithLayout(base: Option, layout: Layout?): Option {
        if (layout == null || layout == Layout.DEFAULT) return base
        val oldRule = base.rule ?: Rule.default()
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
        return base.copy(rule = newRule)
    }

    // ---- §17.5 documented divergence -----------------------------------

    /**
     * The frozen Java `smushHorizontalRule5` uses `indexOf` on the rule
     * string `"/\\ \\/ ><"`, which always returns the earliest match.
     * `indexOf("\\")` therefore returns 1, never 3, so the `pos1 == 3`
     * branch (`\\/ → Y`) is dead code. commonMain uses explicit pair
     * matching and so now returns the spec-correct `Y`.
     *
     * This test pins both observed behaviors.
     */
    @Test
    fun `§17_5 divergence - commonMain returns Y, frozen Java returns empty for backslash-slash`() {
        // commonMain — reachable, spec-correct
        assertEquals("Y", SmushRules.smushHorizontalRule5("\\", "/"))

        // frozen Java — unreachable Y branch, returns empty
        val jMethod = JavaBananaUtils::class.java.getDeclaredMethod(
            "smushHorizontalRule5",
            String::class.java, String::class.java,
        )
        jMethod.isAccessible = true
        val jResult = jMethod.invoke(null, "\\", "/") as String
        assertEquals(
            "", jResult,
            "Frozen Java is expected to return empty here — §17.5 documented dead-code behavior.",
        )
    }

    /**
     * End-to-end confirmation that `<>` under SMUSH_R triggers the §17.5
     * divergence at the renderer level: the Standard-font `<` glyph
     * contains `\` sub-chars and the `>` glyph contains `/` sub-chars,
     * so smushing `<>` at SMUSH_R hits the `\\/` pair.
     *
     * commonMain smushes that pair to `Y` (spec-correct), the frozen
     * Java leaves the pair un-smushed. We pin both outputs.
     */
    @Test
    fun `§17_5 end-to-end - lt gt renders Y in commonMain, not in Java`() {
        val kMeta = JavaLegacyAdapter.metaFromJava(javaMeta)
        val jOption = applyLayoutJava(javaMeta.option, JavaLayout.SMUSH_R)
        val kOption = kotlinOptionWithLayout(kMeta.option, Layout.SMUSH_R)

        val jRows = renderJava("<>", jOption)
        val kRows = renderKotlin("<>", kOption, kMeta.figletMap)

        val kJoined = kRows.joinToString("\n")
        val jJoined = jRows.joinToString("\n")

        // commonMain: reachable Y branch → "Y" appears in the middle row
        assertEquals(true, kJoined.contains("Y"),
            "commonMain output should contain 'Y' (§17.5 reachable branch): was\n$kJoined")

        // Java: dead branch → no Y anywhere
        assertEquals(false, jJoined.contains("Y"),
            "Frozen Java output should NOT contain 'Y' (§17.5 dead branch): was\n$jJoined")
    }

    /**
     * Sanity check — the `/\\` and `><` branches ARE reachable in both
     * implementations and produce identical output, validating that the
     * divergence is isolated to the `\\/` case.
     */
    @Test
    fun `§17_5 - slash-backslash and gt-lt agree between commonMain and Java`() {
        val jMethod = JavaBananaUtils::class.java.getDeclaredMethod(
            "smushHorizontalRule5",
            String::class.java, String::class.java,
        )
        jMethod.isAccessible = true

        assertEquals("|", SmushRules.smushHorizontalRule5("/", "\\"))
        assertEquals("|", jMethod.invoke(null, "/", "\\") as String)

        assertEquals("X", SmushRules.smushHorizontalRule5(">", "<"))
        assertEquals("X", jMethod.invoke(null, ">", "<") as String)
    }
}

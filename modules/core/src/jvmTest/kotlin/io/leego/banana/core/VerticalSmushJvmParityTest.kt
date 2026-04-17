package io.leego.banana.core

import io.leego.banana.BananaUtils as JavaBananaUtils
import io.leego.banana.Font as JavaFont
import io.leego.banana.Layout as JavaLayout
import io.leego.banana.Meta as JavaMeta
import io.leego.banana.Option as JavaOption
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 differential-parity gate for the vertical smushing pipeline.
 *
 * Drives multi-line fixtures through:
 *  - the commonMain [FigletRenderer.combineVertically];
 *  - the frozen Java `BananaUtils.smushVerticalFigletLines` via
 *    [JavaLegacyAdapter.combineVerticallyViaJava].
 *
 * Asserts byte-exact agreement for a matrix of (multi-line text ×
 * vertical layout).
 *
 * Unlike the horizontal rule 5 (§17.5), the vertical rules do NOT have
 * a dead-code issue — the commonMain port is expected to be byte-exact
 * against the frozen Java for every vertical layout and every input.
 */
class VerticalSmushJvmParityTest {

    private val font = JavaFont.STANDARD

    private val javaMeta: JavaMeta by lazy {
        val method = JavaBananaUtils::class.java.getDeclaredMethod("buildMeta", JavaFont::class.java)
        method.isAccessible = true
        method.invoke(null, font) as JavaMeta
    }

    /** Apply both horizontal and vertical layouts via the frozen setLayout. */
    private fun applyLayoutJava(
        base: JavaOption,
        hLayout: JavaLayout?,
        vLayout: JavaLayout?,
    ): JavaOption {
        val method = JavaBananaUtils::class.java.getDeclaredMethod(
            "setLayout",
            JavaOption::class.java,
            JavaLayout::class.java,
            JavaLayout::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, base, hLayout, vLayout) as JavaOption
    }

    private fun renderLineJava(text: String, javaOption: JavaOption): List<String> {
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

    private fun kotlinOptionWithLayouts(
        base: Option,
        hLayout: Layout?,
        vLayout: Layout?,
    ): Option {
        val oldRule = base.rule ?: Rule.default()
        val newHorizontal = when (hLayout) {
            null, Layout.DEFAULT -> oldRule
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
        }
        val newRule = when (vLayout) {
            null, Layout.DEFAULT -> newHorizontal
            Layout.FULL -> newHorizontal.copy(
                verticalLayout = Layout.FULL,
                vertical1 = false, vertical2 = false, vertical3 = false,
                vertical4 = false, vertical5 = false,
            )
            Layout.FITTED -> newHorizontal.copy(
                verticalLayout = Layout.FITTED,
                vertical1 = false, vertical2 = false, vertical3 = false,
                vertical4 = false, vertical5 = false,
            )
            Layout.SMUSH_U -> newHorizontal.copy(
                verticalLayout = Layout.SMUSH_U,
                vertical1 = false, vertical2 = false, vertical3 = false,
                vertical4 = false, vertical5 = false,
            )
            Layout.SMUSH_R -> newHorizontal.copy(
                verticalLayout = Layout.SMUSH_R,
                vertical1 = true, vertical2 = true, vertical3 = true,
                vertical4 = true, vertical5 = true,
            )
        }
        return base.copy(rule = newRule)
    }

    private fun verticalLayoutMatrix(): List<Pair<JavaLayout?, Layout?>> = listOf(
        null to null, // default from header
        JavaLayout.FULL to Layout.FULL,
        JavaLayout.FITTED to Layout.FITTED,
        JavaLayout.SMUSH_U to Layout.SMUSH_U,
        JavaLayout.SMUSH_R to Layout.SMUSH_R,
    )

    /**
     * Multi-line fixtures. Inputs excluding `<>` so we don't trip the
     * documented §17.5 horizontal divergence — vertical rules have no
     * such divergence, but the per-line horizontal render must match.
     */
    private val textLineFixtures: List<List<String>> = listOf(
        listOf("Hi", "Hi"),
        listOf("A", "B"),
        listOf("A", "A", "A"),
        listOf("Hello", "world"),
        listOf("abc", "def", "ghi"),
        listOf("||", "||"),
        listOf("--", "__"),
        listOf("", "Hi"),         // C4: empty first line
        listOf("Hi", ""),         // C4: empty second line
        listOf("Hi"),             // single line short-circuit
    )

    @Test
    fun `commonMain and Java combine vertically identically across vertical layout matrix`() {
        val kMeta = JavaLegacyAdapter.metaFromJava(javaMeta)

        for ((jVLayout, kVLayout) in verticalLayoutMatrix()) {
            // Pin horizontal to SMUSH_R for the multi-line pipeline so
            // we stress the full stack. Skip inputs that would hit §17.5.
            val jOpt = applyLayoutJava(javaMeta.option, JavaLayout.SMUSH_R, jVLayout)
            val kOpt = kotlinOptionWithLayouts(kMeta.option, Layout.SMUSH_R, kVLayout)

            for (textLines in textLineFixtures) {
                val jRendered = textLines.map { renderLineJava(it, jOpt) }
                val kRendered = textLines.map {
                    FigletRenderer.generateLine(it, kMeta.figletMap, kOpt)
                }

                val jCombined = JavaLegacyAdapter.combineVerticallyViaJava(jRendered, jOpt)
                val kCombined = FigletRenderer.combineVertically(kRendered, kOpt)

                assertEquals(
                    jCombined, kCombined,
                    "Vertical parity mismatch for lines=$textLines vLayout=$jVLayout",
                )
            }
        }
    }

    @Test
    fun `vertical rule 1 pairs agree between commonMain and Java`() {
        val jMethod = JavaBananaUtils::class.java.getDeclaredMethod(
            "smushVerticalRule1", String::class.java, String::class.java,
        )
        jMethod.isAccessible = true
        for (ch in listOf("|", "a", "_", "-", " ", "/")) {
            assertEquals(
                jMethod.invoke(null, ch, ch) as String,
                SmushRules.smushVerticalRule1(ch, ch),
                "vRule1 mismatch for equal pair '$ch'",
            )
            assertEquals(
                jMethod.invoke(null, ch, "x") as String,
                SmushRules.smushVerticalRule1(ch, "x"),
                "vRule1 mismatch for '$ch'/x",
            )
        }
    }

    @Test
    fun `vertical rule 4 agrees between commonMain and Java`() {
        val jMethod = JavaBananaUtils::class.java.getDeclaredMethod(
            "smushVerticalRule4", String::class.java, String::class.java,
        )
        jMethod.isAccessible = true
        for ((a, b) in listOf("-" to "_", "_" to "-", "-" to "-", "_" to "_", "a" to "b")) {
            assertEquals(
                jMethod.invoke(null, a, b) as String,
                SmushRules.smushVerticalRule4(a, b),
                "vRule4 mismatch for '$a'/'$b'",
            )
        }
    }

    @Test
    fun `vertical rule 5 agrees between commonMain and Java`() {
        val jMethod = JavaBananaUtils::class.java.getDeclaredMethod(
            "smushVerticalRule5", String::class.java, String::class.java,
        )
        jMethod.isAccessible = true
        for ((a, b) in listOf("|" to "|", "|" to "/", "a" to "a")) {
            assertEquals(
                jMethod.invoke(null, a, b) as String,
                SmushRules.smushVerticalRule5(a, b),
                "vRule5 mismatch for '$a'/'$b'",
            )
        }
    }
}

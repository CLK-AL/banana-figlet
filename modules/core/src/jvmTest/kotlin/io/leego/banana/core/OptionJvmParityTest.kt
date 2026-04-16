package io.leego.banana.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import io.leego.banana.Layout as JavaLayout
import io.leego.banana.Option as JavaOption
import io.leego.banana.Rule as JavaRule

/**
 * Stage S4 differential-parity gate for `Option` and `Rule`.
 *
 * Constructs Java mutable objects with specific values, converts
 * them via [JavaLegacyAdapter], and asserts every field matches.
 */
class OptionJvmParityTest {

    // --- Rule conversion -----------------------------------------------

    @Test
    fun `null Java Rule converts to null`() {
        assertNull(JavaLegacyAdapter.ruleFromJava(null))
    }

    @Test
    fun `default Java Rule converts to default commonMain Rule`() {
        val javaRule = JavaRule()
        val kRule = JavaLegacyAdapter.ruleFromJava(javaRule)
        assertNotNull(kRule)
        assertEquals(Rule.default(), kRule)
    }

    @Test
    fun `populated Java Rule converts field-by-field`() {
        val javaRule = JavaRule()
        javaRule.setHorizontal(
            JavaLayout.SMUSH_R,
            true, false, true, false, true, false
        )
        javaRule.setVertical(
            JavaLayout.FITTED,
            true, false, true, false, true
        )

        val kRule = JavaLegacyAdapter.ruleFromJava(javaRule)
        assertNotNull(kRule)

        assertEquals(Layout.SMUSH_R, kRule.horizontalLayout)
        assertEquals(true, kRule.horizontal1)
        assertEquals(false, kRule.horizontal2)
        assertEquals(true, kRule.horizontal3)
        assertEquals(false, kRule.horizontal4)
        assertEquals(true, kRule.horizontal5)
        assertEquals(false, kRule.horizontal6)

        assertEquals(Layout.FITTED, kRule.verticalLayout)
        assertEquals(true, kRule.vertical1)
        assertEquals(false, kRule.vertical2)
        assertEquals(true, kRule.vertical3)
        assertEquals(false, kRule.vertical4)
        assertEquals(true, kRule.vertical5)
    }

    // --- Option conversion ---------------------------------------------

    @Test
    fun `null Java Option converts to null`() {
        assertNull(JavaLegacyAdapter.optionFromJava(null))
    }

    @Test
    fun `default Java Option converts to default commonMain Option`() {
        val javaOption = JavaOption()
        val kOption = JavaLegacyAdapter.optionFromJava(javaOption)
        assertNotNull(kOption)
        assertEquals(Option.default(), kOption)
    }

    @Test
    fun `fully populated Java Option converts field-by-field`() {
        val javaRule = JavaRule()
        javaRule.horizontalLayout = JavaLayout.SMUSH_U
        javaRule.isHorizontal1 = true
        javaRule.isHorizontal4 = true

        val javaOption = JavaOption()
        javaOption.baseline = 7
        javaOption.codeTagCount = 3
        javaOption.rule = javaRule
        javaOption.fullLayout = 42
        javaOption.hardBlank = "$"
        javaOption.height = 8
        javaOption.maxLength = 120
        javaOption.numCommentLines = 5
        javaOption.oldLayout = 15
        javaOption.printDirection = 1

        val kOption = JavaLegacyAdapter.optionFromJava(javaOption)
        assertNotNull(kOption)

        assertEquals(7, kOption.baseline)
        assertEquals(3, kOption.codeTagCount)
        assertEquals(42, kOption.fullLayout)
        assertEquals("$", kOption.hardBlank)
        assertEquals(8, kOption.height)
        assertEquals(120, kOption.maxLength)
        assertEquals(5, kOption.numCommentLines)
        assertEquals(15, kOption.oldLayout)
        assertEquals(1, kOption.printDirection)

        // Rule sub-object
        val kRule = kOption.rule
        assertNotNull(kRule)
        assertEquals(Layout.SMUSH_U, kRule.horizontalLayout)
        assertEquals(true, kRule.horizontal1)
        assertEquals(false, kRule.horizontal2)
        assertEquals(false, kRule.horizontal3)
        assertEquals(true, kRule.horizontal4)
        assertEquals(false, kRule.horizontal5)
        assertEquals(false, kRule.horizontal6)
        assertNull(kRule.verticalLayout)
    }

    @Test
    fun `Option with null rule converts correctly`() {
        val javaOption = JavaOption()
        javaOption.height = 10
        javaOption.hardBlank = "#"
        // rule intentionally left null

        val kOption = JavaLegacyAdapter.optionFromJava(javaOption)
        assertNotNull(kOption)
        assertEquals(10, kOption.height)
        assertEquals("#", kOption.hardBlank)
        assertNull(kOption.rule)
    }

    @Test
    fun `mutation of Java Option after conversion does not affect Kotlin copy`() {
        val javaOption = JavaOption()
        javaOption.height = 10

        val kOption = JavaLegacyAdapter.optionFromJava(javaOption)
        assertNotNull(kOption)

        // Mutate the Java original — the Kotlin copy must be unaffected
        javaOption.height = 999
        assertEquals(10, kOption.height)
    }
}

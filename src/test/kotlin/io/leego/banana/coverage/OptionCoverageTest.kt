package io.leego.banana.coverage

import io.leego.banana.Layout
import io.leego.banana.Option
import io.leego.banana.Rule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Covers [Option]'s rarely-exercised getters and the null-rule branch of
 * [Option.copy].
 */
class OptionCoverageTest {

    @Test
    fun `zero-arg constructor leaves every field null`() {
        val opt = Option()
        assertNull(opt.baseline)
        assertNull(opt.codeTagCount)
        assertNull(opt.rule)
        assertNull(opt.fullLayout)
        assertNull(opt.hardBlank)
        assertNull(opt.height)
        assertNull(opt.maxLength)
        assertNull(opt.numCommentLines)
        assertNull(opt.oldLayout)
        assertNull(opt.printDirection)
    }

    @Test
    fun `getters return values stored through setters`() {
        val opt = Option()
        opt.baseline = 7
        opt.codeTagCount = 8
        opt.maxLength = 9
        opt.printDirection = 1
        opt.fullLayout = 24680
        opt.hardBlank = "$"
        opt.height = 3
        opt.numCommentLines = 2
        opt.oldLayout = 13
        val rule = Rule()
        rule.horizontalLayout = Layout.SMUSH_U
        opt.rule = rule

        assertEquals(7, opt.baseline)
        assertEquals(8, opt.codeTagCount)
        assertEquals(9, opt.maxLength)
        assertEquals(1, opt.printDirection)
        assertEquals(24680, opt.fullLayout)
        assertEquals("$", opt.hardBlank)
        assertEquals(3, opt.height)
        assertEquals(2, opt.numCommentLines)
        assertEquals(13, opt.oldLayout)
        assertSame(rule, opt.rule)
    }

    @Test
    fun `copy deep-copies the rule when present and propagates null rule`() {
        val withRule = Option()
        val rule = Rule()
        rule.horizontalLayout = Layout.FITTED
        withRule.rule = rule
        val copy1 = withRule.copy()
        assertNotSame(rule, copy1.rule)
        assertEquals(rule.horizontalLayout, copy1.rule.horizontalLayout)

        // null-rule branch of Option.copy
        val noRule = Option()
        val copy2 = noRule.copy()
        assertNull(copy2.rule)
    }
}

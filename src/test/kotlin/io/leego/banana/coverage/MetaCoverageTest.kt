package io.leego.banana.coverage

import io.leego.banana.Font
import io.leego.banana.Meta
import io.leego.banana.Option
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Exercises the [Meta] POJO: constructor, every getter and every setter.
 * The production code never calls the setters or [Meta.getComment]/[Meta.getFont]
 * directly, so they show up as uncovered in JaCoCo.
 */
class MetaCoverageTest {

    @Test
    fun `constructor stores every argument and getters round-trip`() {
        val opt = Option()
        val figletMap: Map<Int, Array<String>> = mapOf(65 to arrayOf("A"))
        val meta = Meta(Font.STANDARD, opt, figletMap, "hello comment")

        assertSame(Font.STANDARD, meta.font)
        assertSame(opt, meta.option)
        assertSame(figletMap, meta.figletMap)
        assertEquals("hello comment", meta.comment)
    }

    @Test
    fun `setters mutate the corresponding fields`() {
        val meta = Meta(null, null, null, null)
        assertNull(meta.font)
        assertNull(meta.comment)

        meta.font = Font.SMALL
        val opt = Option()
        meta.option = opt
        val figletMap: Map<Int, Array<String>> = mapOf(66 to arrayOf("B"))
        meta.figletMap = figletMap
        meta.comment = "other"

        assertSame(Font.SMALL, meta.font)
        assertSame(opt, meta.option)
        assertSame(figletMap, meta.figletMap)
        assertEquals("other", meta.comment)
    }
}

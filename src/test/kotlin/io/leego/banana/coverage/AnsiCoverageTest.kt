package io.leego.banana.coverage

import io.leego.banana.Ansi
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Exhausts [Ansi.ansify] short-circuits and the static lookup helpers.
 */
class AnsiCoverageTest {

    @Test
    fun `single-style ansify wraps non-empty text with ANSI escape`() {
        val out = Ansi.ansify("hi", Ansi.RED)
        assertEquals("\u001B[31mhi\u001B[0m", out)
    }

    @Test
    fun `single-style ansify returns text unchanged for null text`() {
        assertNull(Ansi.ansify(null, Ansi.RED))
    }

    @Test
    fun `single-style ansify returns text unchanged for empty text`() {
        assertEquals("", Ansi.ansify("", Ansi.RED))
    }

    @Test
    fun `single-style ansify returns text unchanged for null style`() {
        assertEquals("hi", Ansi.ansify("hi", null as Ansi?))
    }

    @Test
    fun `varargs ansify wraps text using every non-null style`() {
        val out = Ansi.ansify("hi", Ansi.RED, Ansi.BOLD)
        assertEquals("\u001B[31;1mhi\u001B[0m", out)
    }

    @Test
    fun `varargs ansify skips null styles but still wraps when any remain`() {
        val out = Ansi.ansify("hi", null, Ansi.GREEN)
        assertEquals("\u001B[32mhi\u001B[0m", out)
    }

    @Test
    fun `varargs ansify returns text unchanged when every style is null`() {
        val out = Ansi.ansify("hi", null, null)
        assertEquals("hi", out)
    }

    @Test
    fun `varargs ansify returns text unchanged for null text`() {
        assertNull(Ansi.ansify(null, Ansi.RED, Ansi.BOLD))
    }

    @Test
    fun `varargs ansify returns text unchanged for empty text`() {
        assertEquals("", Ansi.ansify("", Ansi.RED, Ansi.BOLD))
    }

    @Test
    fun `varargs ansify returns text unchanged when styles array is null`() {
        // Reflective call to hit the `styles == null` branch, which Kotlin
        // varargs syntax cannot express directly.
        val m = Ansi::class.java.getMethod("ansify", String::class.java, Array<Ansi>::class.java)
        val out = m.invoke(null, "hi", null) as String?
        assertEquals("hi", out)
    }

    @Test
    fun `varargs ansify returns text unchanged when styles is empty`() {
        assertEquals("hi", Ansi.ansify("hi"))
    }

    @Test
    fun `get returns matching instance by code and null for unknown`() {
        assertSame(Ansi.RED, Ansi.get("31"))
        assertNull(Ansi.get("nope"))
    }

    @Test
    fun `getOrDefault returns match or fallback`() {
        assertSame(Ansi.RED, Ansi.getOrDefault("31", Ansi.GREEN))
        assertSame(Ansi.GREEN, Ansi.getOrDefault("missing", Ansi.GREEN))
    }
}

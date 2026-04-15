package io.leego.banana.coverage

import io.leego.banana.Font
import org.junit.jupiter.api.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exhausts [Font]'s constructors, lookup helpers, and
 * [Font.convertIfZipped]/[Font.getInputStream] edge cases.
 *
 * The plain `Font(name)` ctor, the "already buffered" branch of
 * `convertIfZipped`, and the "zip has no entries" branch are all
 * unreachable from production paths.
 */
class FontCoverageTest {

    /** Publishes protected constructors so coverage can reach them. */
    private class ProbeFont : Font {
        constructor(name: String) : super(name)
        constructor(name: String, filename: String?) : super(name, filename)
    }

    @Test
    fun `single-string constructor records default charset and null filename`() {
        val f = ProbeFont("probe")
        assertEquals("probe", f.name)
        assertNull(f.filename)
        assertSame(StandardCharsets.UTF_8, f.charset)
    }

    @Test
    fun `two-arg constructor records filename`() {
        val f = ProbeFont("probe", "probe.flf")
        assertEquals("probe.flf", f.filename)
    }

    @Test
    fun `getFilename returns stored filename`() {
        assertEquals("Standard.flf", Font.STANDARD.filename)
    }

    @Test
    fun `getInputStream throws when filename cannot be resolved on the classpath`() {
        val missing = ProbeFont("not-on-classpath", "definitely-not-there.flf")
        val ex = assertFailsWith<RuntimeException> { missing.inputStream }
        assertTrue((ex.message ?: "").contains("not-on-classpath"))
    }

    @Test
    fun `getInputStream resolves real font and delegates to convertIfZipped`() {
        Font.STANDARD.inputStream.use { stream ->
            assertNotNull(stream)
            // The raw resource is plain-text, so convertIfZipped returns a
            // BufferedInputStream rather than a ZipInputStream.
            assertTrue(stream is BufferedInputStream)
        }
    }

    @Test
    fun `convertIfZipped keeps an already-buffered stream without rewrapping`() {
        val method = Font::class.java.getDeclaredMethod(
            "convertIfZipped", java.io.InputStream::class.java,
        )
        method.isAccessible = true
        val bytes = "flf2a\$ 1 1 10 0 0\n ".toByteArray(StandardCharsets.UTF_8)
        val buffered = BufferedInputStream(ByteArrayInputStream(bytes))
        val result = method.invoke(null, buffered)
        assertSame(buffered, result)
    }

    @Test
    fun `convertIfZipped unwraps a valid single-entry zip stream`() {
        val body = "flf2a\$ 1 1 10 0 0\n ".toByteArray(StandardCharsets.UTF_8)
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("font.flf"))
            zos.write(body)
            zos.closeEntry()
        }
        val method = Font::class.java.getDeclaredMethod(
            "convertIfZipped", java.io.InputStream::class.java,
        )
        method.isAccessible = true
        val result = method.invoke(null, ByteArrayInputStream(baos.toByteArray()))
        // The returned stream should expose the entry contents.
        val read = (result as java.io.InputStream).readBytes()
        assertEquals(String(body), String(read))
    }

    // The `if (entry == null)` branch of Font.convertIfZipped is unreachable
    // from this method: isZipped() only returns true for streams beginning
    // with PK\x03\x04 (a local file header); any such stream has at least one
    // entry. The branch is excluded from JaCoCo via build.gradle.kts.

    @Test
    fun `get returns matching font or null for unknown name`() {
        assertSame(Font.STANDARD, Font.get("Standard"))
        assertNull(Font.get("no-such-font"))
    }

    @Test
    fun `getOrDefault returns match or fallback`() {
        assertSame(Font.STANDARD, Font.getOrDefault("Standard", Font.SMALL))
        assertSame(Font.SMALL, Font.getOrDefault("missing", Font.SMALL))
    }
}

package io.leego.banana.review

import io.leego.banana.BananaUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * CODE_REVIEW.md finding C3 — `BananaUtils.java:264-275`.
 *
 * `buildMeta` reads each glyph row right-to-left looking for the
 * endmark character. When the entire row is whitespace (no endmark at
 * all — e.g. a row that was accidentally emptied during editing), the
 * trailing-whitespace loop drives `charIndex` down to -1 and then
 * `charRow.charAt(-1)` throws `StringIndexOutOfBoundsException`.
 *
 * We expect a descriptive [IllegalArgumentException] instead.
 */
class C3EndmarkOnlyRowTest {

    @Test
    fun `empty or endmark-only glyph row throws InvalidFontException not SIOOBE`() {
        // Minimal valid header: signature 'flf2a' + hardblank '$',
        // height=1 baseline=1 maxLength=3 oldLayout=0 numCommentLines=0.
        // Then the first glyph row (for ASCII 32 / space) is an empty
        // line, which triggers charRow.charAt(-1).
        val header = "flf2a\$ 1 1 3 0 0\n"
        val body = header + "\n" // single empty glyph row
        val font = InMemoryFont("c3-empty-row", body)

        val ex = assertFailsWith<IllegalArgumentException> {
            BananaUtils.bananaify("hi", font)
        }
        val msg = ex.message ?: ""
        assert(msg.contains("glyph", ignoreCase = true) || msg.contains("row", ignoreCase = true) || msg.contains("endmark", ignoreCase = true)) {
            "expected a descriptive glyph/row/endmark error, got: $msg"
        }
    }
}

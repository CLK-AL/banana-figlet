package io.leego.banana.review

import io.leego.banana.BananaUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * CODE_REVIEW.md finding C1 — `BananaUtils.java:224`.
 *
 * The FIGlet header's first token must be at least 6 characters long
 * (`flf2a` plus the hardblank). A truncated or malformed `.flf` currently
 * propagates `StringIndexOutOfBoundsException` out of the library; we
 * expect a descriptive [IllegalArgumentException] instead.
 */
class C1HardblankHeaderBoundsTest {

    @Test
    fun `header shorter than 6 chars throws InvalidFontException not SIOOBE`() {
        // Valid counts for the remaining header fields so the first
        // boundary check is what trips, not a later parse.
        val body = "flf2a 6 5 16 0 0\n"
        val font = InMemoryFont("c1-short-header", body)

        val ex = assertFailsWith<IllegalArgumentException> {
            BananaUtils.bananaify("hi", font)
        }
        // Error message should mention the header for debuggability.
        val msg = ex.message ?: ""
        assert(msg.contains("header", ignoreCase = true) || msg.contains("hardblank", ignoreCase = true)) {
            "expected a descriptive header/hardblank error, got: $msg"
        }
    }
}

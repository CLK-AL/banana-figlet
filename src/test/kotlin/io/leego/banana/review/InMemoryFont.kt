package io.leego.banana.review

import io.leego.banana.Font
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * A [Font] that serves its contents from an in-memory [String] instead of a
 * classpath resource. Used by the review test suite to feed malformed fonts
 * to [io.leego.banana.BananaUtils] without shipping binary fixtures.
 */
internal class InMemoryFont(
    name: String,
    private val body: String,
    private val bodyCharset: Charset = StandardCharsets.UTF_8,
) : Font(name, null, bodyCharset) {

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(body.toByteArray(bodyCharset))
    }
}

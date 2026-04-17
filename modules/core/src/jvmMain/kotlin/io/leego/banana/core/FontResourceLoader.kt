package io.leego.banana.core

import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import java.util.Arrays
import java.util.zip.ZipInputStream

/**
 * JVM `actual` for [loadFontResource].
 *
 * Reads the font file from the classpath at `banana/fonts/<name>`,
 * auto-detecting and decompressing ZIP-wrapped fonts exactly like
 * the frozen Java `Font.getInputStream()` / `convertIfZipped()`.
 */
public actual fun loadFontResource(name: String): String {
    val path = "banana/fonts/$name"
    val raw = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
        ?: throw RuntimeException("Failed to load font resource '$name': resource not found at '$path'.")

    val buffered = if (raw is BufferedInputStream) raw else BufferedInputStream(raw)

    // Detect ZIP header (PK\x03\x04)
    val stream = run {
        val buf = ByteArray(4)
        buffered.mark(4)
        buffered.read(buf)
        buffered.reset()
        if (Arrays.equals(buf, byteArrayOf(0x50, 0x4b, 0x03, 0x04))) {
            val zip = ZipInputStream(buffered)
            zip.nextEntry ?: throw RuntimeException("Failed to decompress font '$name'.")
            zip
        } else {
            buffered
        }
    }

    return InputStreamReader(stream, Charsets.UTF_8).use { reader ->
        BufferedReader(reader).use { br ->
            br.readText()
        }
    }
}

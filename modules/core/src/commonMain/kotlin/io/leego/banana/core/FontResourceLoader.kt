package io.leego.banana.core

/**
 * Stage S4 -- platform-abstracted font resource loading.
 *
 * commonMain declares the `expect` signature; each platform target
 * provides an `actual` implementation.  The JVM actual reads from
 * the classpath (`banana/fonts/<name>`).
 *
 * The font cache uses a plain `HashMap`.  Thread-safe caching is a
 * JVM-side concern -- callers on JVM should synchronise externally
 * or rely on the JVM convenience wrapper's own cache.
 */

/**
 * Load the raw text of a font resource by [name] (e.g. `"Standard.flf"`).
 * Returns the full file content as a single `String`.
 *
 * @throws RuntimeException if the resource cannot be found or read.
 */
public expect fun loadFontResource(name: String): String

package io.leego.banana.core

/**
 * Stage S4 — pure-function FLF (FIGlet font) parser.
 *
 * Ports the frozen Java `BananaUtils.buildMeta(Font)` header-parsing and
 * glyph-parsing logic to commonMain with no I/O dependencies.  The caller
 * is responsible for reading the `.flf` file into a `List<String>`;
 * this function does the rest.
 *
 * Carries all C1–C5 header-validation fixes from the frozen Java tree.
 */

/**
 * The standard codepoints expected in an FLF font file:
 * ASCII 32..126 followed by the German extra characters.
 */
private val CODES: List<Int> = buildList {
    for (i in 32..126) add(i)
    addAll(listOf(196, 214, 220, 223, 228, 246, 252))
}

/**
 * Internal representation of the RuleEnum entries used to decode the
 * smush rule bitmask, mirroring `io.leego.banana.RuleEnum` exactly.
 *
 * Entries are ordered from highest bit to lowest, matching the Java
 * enum's declaration order which is critical for the subtraction loop.
 */
private data class RuleEntry(val key: String, val code: Int, val value: Int)

private val RULE_ENTRIES: List<RuleEntry> = listOf(
    RuleEntry("vLayout", 16384, Layout.SMUSH_U.code),
    RuleEntry("vLayout", 8192, Layout.FITTED.code),
    RuleEntry("v5", 4096, 1),
    RuleEntry("v4", 2048, 1),
    RuleEntry("v3", 1024, 1),
    RuleEntry("v2", 512, 1),
    RuleEntry("v1", 256, 1),
    RuleEntry("hLayout", 128, Layout.SMUSH_U.code),
    RuleEntry("hLayout", 64, Layout.FITTED.code),
    RuleEntry("h6", 32, 1),
    RuleEntry("h5", 16, 1),
    RuleEntry("h4", 8, 1),
    RuleEntry("h3", 4, 1),
    RuleEntry("h2", 2, 1),
    RuleEntry("h1", 1, 1),
)

/**
 * Compute the smush [Rule] from the oldLayout and optional fullLayout
 * header values, exactly mirroring `BananaUtils.getSmushRule`.
 */
internal fun getSmushRule(oldLayout: Int, fullLayout: Int?): Rule {
    val rules = mutableMapOf<String, Int>()
    var layout = fullLayout ?: oldLayout
    for (entry in RULE_ENTRIES) {
        if (layout >= entry.code) {
            layout -= entry.code
            if (entry.key !in rules) {
                rules[entry.key] = entry.value
            }
        } else if (entry.key != "hLayout" && entry.key != "vLayout") {
            rules[entry.key] = 0
        }
    }

    var horizontalLayout: Layout? = Layout.get(rules["hLayout"])
    val h1 = rules["h1"] == 1
    val h2 = rules["h2"] == 1
    val h3 = rules["h3"] == 1
    val h4 = rules["h4"] == 1
    val h5 = rules["h5"] == 1
    val h6 = rules["h6"] == 1

    var verticalLayout: Layout? = Layout.get(rules["vLayout"])
    val v1 = rules["v1"] == 1
    val v2 = rules["v2"] == 1
    val v3 = rules["v3"] == 1
    val v4 = rules["v4"] == 1
    val v5 = rules["v5"] == 1

    // Horizontal layout fallback logic — mirrors Java exactly
    if (horizontalLayout == null) {
        horizontalLayout = when {
            oldLayout == 0 -> Layout.FITTED
            oldLayout == -1 -> Layout.FULL
            h1 || h2 || h3 || h4 || h5 || h6 -> Layout.SMUSH_R
            else -> Layout.SMUSH_U
        }
    } else if (horizontalLayout == Layout.SMUSH_U && h1
        || h2 || h3 || h4 || h5 || h6) {
        // NOTE: the Java code has a precedence bug here where the ||
        // conditions are not fully parenthesized with the SMUSH_U check.
        // We replicate the exact Java behavior for parity.
        horizontalLayout = Layout.SMUSH_R
    }

    // Vertical layout fallback logic — mirrors Java exactly
    if (verticalLayout == null) {
        verticalLayout = if (v1 || v2 || v3 || v4 || v5) {
            Layout.SMUSH_R
        } else {
            Layout.FULL
        }
    } else if (verticalLayout == Layout.SMUSH_U && v1
        || v2 || v3 || v4 || v5) {
        // Same precedence pattern as horizontal for exact Java parity.
        verticalLayout = Layout.SMUSH_R
    }

    return Rule(
        horizontalLayout = horizontalLayout,
        horizontal1 = h1,
        horizontal2 = h2,
        horizontal3 = h3,
        horizontal4 = h4,
        horizontal5 = h5,
        horizontal6 = h6,
        verticalLayout = verticalLayout,
        vertical1 = v1,
        vertical2 = v2,
        vertical3 = v3,
        vertical4 = v4,
        vertical5 = v5,
    )
}

/**
 * Parse an FLF font file from its lines and produce a [Meta].
 *
 * This is a pure function — no I/O. The caller reads the `.flf` file
 * into a `List<String>` and passes it here.
 *
 * @param data all lines of the `.flf` file
 * @return the parsed [Meta]
 * @throws IllegalArgumentException on malformed headers or glyph data
 */
public fun parseFlfFont(data: List<String>): Meta {
    require(data.isNotEmpty()) { "FLF data is empty." }

    // --- Parse header (C1: validate minimum header length) ---------------
    val header = data[0].split(" ")
    require(header.size >= 6) {
        "Invalid FLF header: expected at least 6 space-separated tokens, got ${header.size}."
    }
    // C1: first token must be at least 6 chars (5 for "flf2a" + 1 for hardblank)
    require(header[0].length >= 6) {
        "Invalid font header: first token must be at least 6 characters " +
            "(signature + hardblank), got '${header[0]}'."
    }

    val hardBlank = header[0].substring(5, 6)
    val height = header[1].toInt()
    val baseline = header[2].toInt()
    val maxLength = header[3].toInt()
    val oldLayout = header[4].toInt()
    val numCommentLines = header[5].toInt()
    val printDirection = if (header.size > 6) header[6].toInt() else 0
    val fullLayout = if (header.size > 7) header[7].toInt() else null
    val codeTagCount = if (header.size > 8) header[8].toInt() else null

    val rule = getSmushRule(oldLayout, fullLayout)

    val option = Option(
        baseline = baseline,
        codeTagCount = codeTagCount,
        rule = rule,
        fullLayout = fullLayout,
        hardBlank = hardBlank,
        height = height,
        maxLength = maxLength,
        numCommentLines = numCommentLines,
        oldLayout = oldLayout,
        printDirection = printDirection,
    )

    // --- Read comment lines ----------------------------------------------
    val comment = buildString {
        var num = 0
        while (++num <= numCommentLines) {
            if (num < data.size) {
                append(data[num])
            }
            append("\n")
        }
    }

    // --- Build FIGlet glyph map ------------------------------------------
    val num = numCommentLines + 1 // first glyph row index
    val figletMap = mutableMapOf<Int, List<String>>()

    for (i in CODES.indices) {
        val code = CODES[i]
        if (i * height + num >= data.size) {
            break // C2: truncated glyph rows — stop gracefully
        }

        val figlet = mutableListOf<String>()
        var complete = true
        for (j in 0 until height) {
            val row = i * height + j + num
            if (row >= data.size) {
                // C2: incomplete glyph — discard it
                complete = false
                break
            }
            val charRow = data[row]

            // Endmark stripping — inspired by readfontchar in figlet.c
            var charIndex = charRow.length - 1

            // Remove trailing whitespace
            while (charIndex >= 0 && charRow[charIndex].isWhitespace()) {
                charIndex--
            }

            if (charIndex < 0) {
                // C3: endmark-only or empty row — produce empty string
                figlet.add("")
                continue
            }

            val endChar = charRow[charIndex] // first endmark
            // Remove all endmarks
            while (charIndex >= 0 && charRow[charIndex] == endChar) {
                charIndex--
            }

            figlet.add(charRow.substring(0, charIndex + 1))
        }

        if (complete) {
            figletMap[code] = figlet
        }
    }

    return Meta(
        option = option,
        figletMap = figletMap,
        comment = comment,
        height = height,
    )
}

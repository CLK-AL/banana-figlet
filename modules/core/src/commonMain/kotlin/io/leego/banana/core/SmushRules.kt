package io.leego.banana.core

/**
 * Stage S4 — pure-function port of the 6 horizontal FIGlet smushing
 * rules plus the universal-smush fallback.
 *
 * Mirrors `BananaUtils.smushHorizontalRule1..6` + `smushUniversal`
 * byte-exact, with one deliberate divergence:
 *
 * §17.5 — `smushHorizontalRule5` in the frozen Java uses
 * `String.indexOf` to locate the pair inside `"/\\ \\/ ><"`, which
 * always returns the earliest occurrence.  `indexOf("\\")` returns
 * `1`, never `3`, so the `\\/ → Y` branch is unreachable dead code.
 * This commonMain port replaces the `indexOf`-based dispatch with
 * explicit pair matching so every spec-defined branch (`|`, `Y`, `X`)
 * is actually reachable.  The frozen Java side keeps its historical
 * behavior; [JavaLegacyAdapter] documents the divergence in tests.
 */
public object SmushRules {

    private const val EMPTY = ""
    private const val WHITESPACE = " "

    /**
     * Rule 1: EQUAL CHARACTER SMUSHING (code value 1).
     * Two sub-characters are smushed into a single sub-character
     * if they are the same.  Does not smush hardblanks (see rule 6).
     */
    public fun smushHorizontalRule1(s1: String, s2: String, hardBlank: String?): String {
        return if (s1 == s2 && s1 != hardBlank) s1 else EMPTY
    }

    /**
     * Rule 2: UNDERSCORE SMUSHING (code value 2).
     * An underscore (`_`) will be replaced by any of:
     * `|`, `/`, `\`, `[`, `]`, `{`, `}`, `(`, `)`, `<` or `>`.
     */
    public fun smushHorizontalRule2(s1: String, s2: String): String {
        val rule = "|/\\[]{}()<>"
        return when {
            s1 == "_" && rule.contains(s2) -> s2
            s2 == "_" && rule.contains(s1) -> s1
            else -> EMPTY
        }
    }

    /**
     * Rule 3: HIERARCHY SMUSHING (code value 4).
     * A hierarchy of six classes: `|`, `/\`, `[]`, `{}`, `()`, `<>`.
     * When two smushing sub-characters are from different classes,
     * the one from the later class wins.
     */
    public fun smushHorizontalRule3(s1: String, s2: String): String {
        val rule = "| /\\ [] {} () <>"
        val pos1 = rule.indexOf(s1)
        val pos2 = rule.indexOf(s2)
        if (pos1 != -1 && pos2 != -1 && pos1 != pos2 && kotlin.math.abs(pos1 - pos2) != 1) {
            return rule.substring(kotlin.math.max(pos1, pos2), kotlin.math.max(pos1, pos2) + 1)
        }
        return EMPTY
    }

    /**
     * Rule 4: OPPOSITE PAIR SMUSHING (code value 8).
     * Smushes opposing brackets (`[]`/`][`), braces (`{}`/`}{`)
     * and parentheses (`()`/`)(`) together, replacing any such pair
     * with a vertical bar (`|`).
     */
    public fun smushHorizontalRule4(s1: String, s2: String): String {
        val rule = "[] {} ()"
        val pos1 = rule.indexOf(s1)
        val pos2 = rule.indexOf(s2)
        if (pos1 != -1 && pos2 != -1 && kotlin.math.abs(pos1 - pos2) <= 1) {
            return "|"
        }
        return EMPTY
    }

    /**
     * Rule 5: BIG X SMUSHING (code value 16).
     *
     * Smushes `/\` into `|`, `\/` into `Y`, and `><` into `X`.
     * Note that `<>` is not smushed by this rule.
     *
     * §17.5 fix — the frozen Java uses `indexOf` on `"/\\ \\/ ><"`
     * which makes `\\/ → Y` unreachable (the first `\` in the rule
     * string is at position 1, so `indexOf("\\")` can never return 3).
     * The commonMain port replaces that logic with explicit pair
     * matching so every spec-defined branch is reachable.
     */
    public fun smushHorizontalRule5(s1: String, s2: String): String {
        return when {
            s1 == "/" && s2 == "\\" -> "|"
            s1 == "\\" && s2 == "/" -> "Y"
            s1 == ">" && s2 == "<" -> "X"
            else -> EMPTY
        }
    }

    /**
     * Rule 6: HARDBLANK SMUSHING (code value 32).
     * Smushes two hardblanks together, replacing them with a
     * single hardblank.
     */
    public fun smushHorizontalRule6(s1: String, s2: String, hardBlank: String?): String {
        if (hardBlank == null) return EMPTY
        return if (s1 == hardBlank && s2 == hardBlank) hardBlank else EMPTY
    }

    /**
     * Universal smushing — overrides the sub-character from the
     * earlier FIGcharacter with the sub-character from the later
     * FIGcharacter.  Produces an "overlapping" effect.
     */
    public fun smushUniversal(s1: String, s2: String, hardBlank: String?): String {
        return when {
            s2 == WHITESPACE || s2 == EMPTY -> s1
            s2 == hardBlank && s1 != WHITESPACE -> s1
            else -> s2
        }
    }

    // ---------------------------------------------------------------
    // Vertical smushing rules (byte-exact port of
    // BananaUtils.smushVerticalRule1..5).
    //
    // Unlike rule 5 on the horizontal side, the vertical rules do NOT
    // have the §17.5 indexOf/dead-code issue. Rule 3 reuses the same
    // hierarchy string as the horizontal rule 3 and has the same
    // behavior; rules 1/2/4/5 are trivial character-pair tests.
    // ---------------------------------------------------------------

    /**
     * Vertical rule 1: EQUAL CHARACTER SMUSHING (code value 256).
     * Two sub-characters are smushed into a single sub-character if
     * they are the same.  Unlike the horizontal rule 1, this rule
     * does NOT special-case hardblanks — vertical smushing never sees
     * hardblanks (they are replaced with spaces before vertical
     * smushing runs), matching the frozen Java behavior exactly.
     */
    public fun smushVerticalRule1(s1: String, s2: String): String {
        return if (s1 == s2) s1 else EMPTY
    }

    /**
     * Vertical rule 2: UNDERSCORE SMUSHING (code value 512).
     * Same as horizontal smushing rule 2.
     */
    public fun smushVerticalRule2(s1: String, s2: String): String {
        val rule = "|/\\[]{}()<>"
        return when {
            s1 == "_" && rule.contains(s2) -> s2
            s2 == "_" && rule.contains(s1) -> s1
            else -> EMPTY
        }
    }

    /**
     * Vertical rule 3: HIERARCHY SMUSHING (code value 1024).
     * Same as horizontal smushing rule 3.
     */
    public fun smushVerticalRule3(s1: String, s2: String): String {
        val rule = "| /\\ [] {} () <>"
        val pos1 = rule.indexOf(s1)
        val pos2 = rule.indexOf(s2)
        if (pos1 != -1 && pos2 != -1 && pos1 != pos2 && kotlin.math.abs(pos1 - pos2) != 1) {
            return rule.substring(kotlin.math.max(pos1, pos2), kotlin.math.max(pos1, pos2) + 1)
        }
        return EMPTY
    }

    /**
     * Vertical rule 4: HORIZONTAL LINE SMUSHING (code value 2048).
     * Smushes stacked pairs of `-` and `_`, replacing them with a
     * single `=` sub-character.  Either order produces `=`.
     * Note: this rule smushes pairs of DIFFERENT sub-characters;
     * identical pairs are handled by vertical rule 1.
     */
    public fun smushVerticalRule4(s1: String, s2: String): String {
        return if ((s1 == "-" && s2 == "_") || (s1 == "_" && s2 == "-")) "=" else EMPTY
    }

    /**
     * Vertical rule 5: VERTICAL LINE SUPERSMUSHING (code value 4096).
     * Supersmushes two `|` sub-characters together into a single `|`.
     * Unlike other rules, this rule allows super-smushing — many
     * rows of `|` can collapse into one, producing the illusion
     * that FIGcharacters have slid vertically against each other.
     */
    public fun smushVerticalRule5(s1: String, s2: String): String {
        return if (s1 == "|" && s2 == "|") "|" else EMPTY
    }
}

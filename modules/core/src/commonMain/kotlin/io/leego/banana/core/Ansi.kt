package io.leego.banana.core

/**
 * Stage S4 -- port of the frozen Java `io.leego.banana.Ansi`.
 *
 * Represents ANSI escape-code styles (foreground/background colours and
 * text decorations).  The companion object exposes the same named
 * constants as the Java original.
 *
 * [ansify] wraps a text string with escape codes, exactly matching the
 * Java `Ansi.ansify(String, Ansi...)` behaviour: builds a combined
 * `\033[code1;code2;...m` prefix and appends the NORMAL reset suffix.
 */
public class Ansi(public val code: String) {

    /** Returns the full ANSI escape sequence for this single style. */
    public fun getAnsi(): String = "\u001B[${code}m"

    public companion object {
        // Foreground colours
        public val BLACK: Ansi = Ansi("30")
        public val RED: Ansi = Ansi("31")
        public val GREEN: Ansi = Ansi("32")
        public val YELLOW: Ansi = Ansi("33")
        public val BLUE: Ansi = Ansi("34")
        public val PURPLE: Ansi = Ansi("35")
        public val CYAN: Ansi = Ansi("36")
        public val WHITE: Ansi = Ansi("37")

        // Background colours
        public val BG_BLACK: Ansi = Ansi("40")
        public val BG_RED: Ansi = Ansi("41")
        public val BG_GREEN: Ansi = Ansi("42")
        public val BG_YELLOW: Ansi = Ansi("43")
        public val BG_BLUE: Ansi = Ansi("44")
        public val BG_PURPLE: Ansi = Ansi("45")
        public val BG_CYAN: Ansi = Ansi("46")
        public val BG_WHITE: Ansi = Ansi("47")

        // Text decorations
        public val NORMAL: Ansi = Ansi("0")
        public val BOLD: Ansi = Ansi("1")
        public val FAINT: Ansi = Ansi("2")
        public val ITALIC: Ansi = Ansi("3")
        public val UNDERLINE: Ansi = Ansi("4")
        public val SLOW_BLINK: Ansi = Ansi("5")
        public val RAPID_BLINK: Ansi = Ansi("6")
        public val REVERSE_VIDEO: Ansi = Ansi("7")
        public val CONCEAL: Ansi = Ansi("8")
        public val CROSSED_OUT: Ansi = Ansi("9")
        public val PRIMARY: Ansi = Ansi("10")

        /**
         * Wrap [text] with the given [styles] ANSI escape codes.
         *
         * Mirrors the frozen Java `Ansi.ansify(String, Ansi...)` exactly:
         * - null/empty text or null/empty styles returns [text] unchanged;
         * - all-null styles returns [text] unchanged;
         * - otherwise builds `\033[c1;c2;...m` + text + `\033[0m`.
         */
        public fun ansify(text: String?, vararg styles: Ansi?): String? {
            if (text.isNullOrEmpty() || styles.isEmpty()) {
                return text
            }
            var nullCount = 0
            val sb = StringBuilder("\u001B[")
            for (style in styles) {
                if (style != null) {
                    sb.append(style.code).append(";")
                } else {
                    nullCount++
                }
            }
            if (nullCount == styles.size) {
                return text
            }
            sb.deleteCharAt(sb.length - 1)
            return sb.append("m").append(text).append(NORMAL.getAnsi()).toString()
        }
    }
}

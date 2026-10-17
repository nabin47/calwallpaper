package io.github.nabin47.calwallpaper.render.text

/** Appended to text that was cut to fit. */
const val ELLIPSIS: String = "…"

/**
 * Returns [text] unchanged if it fits in [maxWidth], otherwise the longest prefix that fits followed by
 * [ELLIPSIS]. Never splits a surrogate pair. Returns an empty string if not even the ellipsis fits. Pure.
 */
fun TextMeasurer.ellipsize(
    text: String,
    style: TextStyle,
    maxWidth: Float,
): String =
    when {
        width(text, style) <= maxWidth -> text
        width(ELLIPSIS, style) > maxWidth -> ""
        else -> prefix(text, longestPrefixWithEllipsis(text, style, maxWidth)) + ELLIPSIS
    }

/** Binary search for the longest prefix length whose ellipsized form fits; 0 if only the ellipsis fits. */
private fun TextMeasurer.longestPrefixWithEllipsis(
    text: String,
    style: TextStyle,
    maxWidth: Float,
): Int {
    var low = 0
    var high = text.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (width(prefix(text, mid) + ELLIPSIS, style) <= maxWidth) low = mid else high = mid - 1
    }
    return low
}

/**
 * Greedy word wrap into at most [maxLines] lines no wider than [maxWidth]. Words longer than a line are broken
 * by character; text left over after the last line is ellipsized into it. Pure.
 */
fun TextMeasurer.wrap(
    text: String,
    style: TextStyle,
    maxWidth: Float,
    maxLines: Int,
): List<String> {
    require(maxLines >= 1) { "maxLines must be at least 1" }
    val lines = mutableListOf<String>()
    var rest = text.trim()
    while (rest.isNotEmpty() && lines.size < maxLines - 1) {
        val line = fittingLine(rest, style, maxWidth)
        if (line.isEmpty()) break
        lines += line
        rest = rest.substring(line.length).trimStart()
    }
    if (rest.isNotEmpty()) lines += ellipsize(rest, style, maxWidth)
    return lines
}

/** The longest leading part of [text] that fits: whole words if possible, otherwise as many characters as fit. */
private fun TextMeasurer.fittingLine(
    text: String,
    style: TextStyle,
    maxWidth: Float,
): String {
    if (width(text, style) <= maxWidth) return text
    var end = 0
    var next = text.indexOf(' ')
    while (next > 0 && width(text.substring(0, next), style) <= maxWidth) {
        end = next
        next = text.indexOf(' ', next + 1)
    }
    return if (end > 0) text.substring(0, end) else fittingChars(text, style, maxWidth)
}

/** As many leading characters of [text] as fit in [maxWidth] (no word boundaries). */
private fun TextMeasurer.fittingChars(
    text: String,
    style: TextStyle,
    maxWidth: Float,
): String {
    var chars = 0
    while (chars < text.length && width(prefix(text, chars + 1), style) <= maxWidth) chars++
    return prefix(text, chars)
}

/** First [length] chars of [text], shortened by one if that would split a surrogate pair. */
private fun prefix(
    text: String,
    length: Int,
): String {
    val safe = if (length in 1 until text.length && text[length - 1].isHighSurrogate()) length - 1 else length
    return text.substring(0, safe).trimEnd()
}

package io.github.nabin47.calwallpaper.render.text

import io.github.nabin47.calwallpaper.domain.settings.FontChoice

/** Font weight; renderers map these to the closest available typeface weight. */
enum class FontWeight {
    /** Regular body text. */
    NORMAL,

    /** Slightly heavier, for titles. */
    MEDIUM,

    /** Headings and emphasis. */
    BOLD,
}

/**
 * How a run of text looks. [sizePx] already includes density, the system font scale and the user's text scale.
 */
data class TextStyle(
    val sizePx: Float,
    val weight: FontWeight = FontWeight.NORMAL,
    val font: FontChoice = FontChoice.SYSTEM,
    val strikethrough: Boolean = false,
)

/** Vertical font metrics in pixels, both positive: [ascent] above the baseline, [descent] below it. */
data class FontMetrics(
    val ascent: Float,
    val descent: Float,
) {
    /** Height of one line of text. */
    val lineHeight: Float get() = ascent + descent
}

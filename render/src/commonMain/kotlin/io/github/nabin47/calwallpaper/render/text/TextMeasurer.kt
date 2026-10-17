package io.github.nabin47.calwallpaper.render.text

/**
 * Measures text for the layout engine. Implemented by each platform with the same fonts its renderer draws
 * with (Android: `Paint`), so fitted text never overflows. Must be deterministic and thread-safe for a given
 * style; it is called many times per layout, so implementations should cache paints.
 */
interface TextMeasurer {
    /** Advance width of [text] drawn in [style], in pixels. */
    fun width(
        text: String,
        style: TextStyle,
    ): Float

    /** Vertical metrics for [style]. */
    fun metrics(style: TextStyle): FontMetrics
}

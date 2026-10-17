package io.github.nabin47.calwallpaper.wallpaper.render

import android.graphics.Paint
import io.github.nabin47.calwallpaper.render.text.FontMetrics
import io.github.nabin47.calwallpaper.render.text.TextMeasurer
import io.github.nabin47.calwallpaper.render.text.TextStyle

/**
 * [TextMeasurer] backed by an Android [Paint], configured exactly like [SceneCanvasRenderer]'s text paint so
 * fitted text never overflows when drawn. Reuses one paint; not thread-safe (one instance per layout thread).
 */
class AndroidTextMeasurer(
    private val textPaints: TextPaints = TextPaints(),
) : TextMeasurer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
    private val metrics = Paint.FontMetrics()

    override fun width(
        text: String,
        style: TextStyle,
    ): Float {
        textPaints.apply(paint, style)
        return paint.measureText(text)
    }

    override fun metrics(style: TextStyle): FontMetrics {
        textPaints.apply(paint, style)
        paint.getFontMetrics(metrics)
        return FontMetrics(ascent = -metrics.ascent, descent = metrics.descent)
    }
}

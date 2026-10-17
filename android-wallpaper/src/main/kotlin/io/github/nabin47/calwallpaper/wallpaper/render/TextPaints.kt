package io.github.nabin47.calwallpaper.wallpaper.render

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import io.github.nabin47.calwallpaper.domain.settings.FontChoice
import io.github.nabin47.calwallpaper.render.text.FontWeight
import io.github.nabin47.calwallpaper.render.text.TextStyle

/**
 * Maps [TextStyle]s to Android typefaces and configures paints. Shared by [AndroidTextMeasurer] and
 * [SceneCanvasRenderer] so measured and drawn text always match. Caches typefaces; not thread-safe, so give
 * each thread its own instance.
 */
class TextPaints {
    // Indexed by font and weight ordinals, so lookups in the draw path allocate nothing.
    private val cache = arrayOfNulls<Typeface>(FontChoice.entries.size * FontWeight.entries.size)

    /** Sets [paint]'s size, typeface and strikethrough for [style]. Allocation-free after the first use of a style. */
    fun apply(
        paint: Paint,
        style: TextStyle,
    ) {
        paint.textSize = style.sizePx
        paint.typeface = typeface(style.font, style.weight)
        paint.isStrikeThruText = style.strikethrough
    }

    private fun typeface(
        font: FontChoice,
        weight: FontWeight,
    ): Typeface {
        val index = font.ordinal * FontWeight.entries.size + weight.ordinal
        return cache[index] ?: create(font, weight).also { cache[index] = it }
    }

    private fun create(
        font: FontChoice,
        weight: FontWeight,
    ): Typeface {
        val family =
            when (font) {
                FontChoice.SYSTEM -> Typeface.DEFAULT
                FontChoice.SANS -> Typeface.SANS_SERIF
                FontChoice.SERIF -> Typeface.SERIF
                FontChoice.MONO -> Typeface.MONOSPACE
            }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(family, weight.value(), false)
        } else {
            Typeface.create(family, if (weight == FontWeight.NORMAL) Typeface.NORMAL else Typeface.BOLD)
        }
    }

    private fun FontWeight.value(): Int =
        when (this) {
            FontWeight.NORMAL -> WEIGHT_NORMAL
            FontWeight.MEDIUM -> WEIGHT_MEDIUM
            FontWeight.BOLD -> WEIGHT_BOLD
        }

    private companion object {
        const val WEIGHT_NORMAL = 400
        const val WEIGHT_MEDIUM = 500
        const val WEIGHT_BOLD = 700
    }
}

package io.github.nabin47.calwallpaper.ui.settings

import io.github.nabin47.calwallpaper.domain.settings.AppearanceSettings
import io.github.nabin47.calwallpaper.render.theme.Palette
import kotlin.math.pow

/**
 * Text contrast as the wallpaper will show it (CLAUDE.md §10: contrast warnings in settings). The panel is
 * blended over the background with its opacity, and each text color over the panel with the text opacity, then
 * the WCAG 2.x contrast ratio is computed for every color the layout draws text in. Pure.
 */
object Contrast {
    /** WCAG AA minimum for normal-size text. Below this the settings screen shows a warning. */
    const val MINIMUM_RATIO: Float = 4.5f

    /** The colors the wallpaper draws text in. */
    enum class TextRole {
        /** Titles and dates. */
        TEXT,

        /** Times, counts, weekday initials, completed items and "more" lines. */
        SECONDARY,

        /** Today's heading in the agenda list. */
        ACCENT,

        /** Today's date on the accent circle. */
        ON_ACCENT,
    }

    /** Contrast ratio (1 to 21) of each [TextRole] for [appearance], using [dynamic] if dynamic color is on. */
    fun ratios(
        appearance: AppearanceSettings,
        dynamic: Palette? = null,
    ): Map<TextRole, Float> {
        val normalized = appearance.normalized()
        val palette = Palette.resolve(normalized, dynamic)
        val panel = blend(palette.panel, palette.background, normalized.panelOpacity * alpha(palette.panel))
        val textOpacity = normalized.textOpacity

        fun over(
            color: Int,
            bottom: Int,
        ): Int = blend(color, bottom, textOpacity * alpha(color))
        val accent = over(palette.accent, panel)
        return mapOf(
            TextRole.TEXT to ratioOf(over(palette.text, panel), panel),
            TextRole.SECONDARY to ratioOf(over(palette.secondaryText, panel), panel),
            TextRole.ACCENT to ratioOf(accent, panel),
            TextRole.ON_ACCENT to ratioOf(over(palette.onAccent, accent), accent),
        )
    }

    /** The weakest contrast ratio over every [TextRole]: what the settings warning reports. */
    fun ratio(
        appearance: AppearanceSettings,
        dynamic: Palette? = null,
    ): Float = ratios(appearance, dynamic).values.min()

    /** True if [ratio] is below [MINIMUM_RATIO]. */
    fun isLow(
        appearance: AppearanceSettings,
        dynamic: Palette? = null,
    ): Boolean = ratio(appearance, dynamic) < MINIMUM_RATIO

    private fun ratioOf(
        a: Int,
        b: Int,
    ): Float {
        val la = luminance(a)
        val lb = luminance(b)
        return ((maxOf(la, lb) + OFFSET) / (minOf(la, lb) + OFFSET)).toFloat()
    }

    /** [top] over [bottom] with [opacity]; the result is opaque. */
    private fun blend(
        top: Int,
        bottom: Int,
        opacity: Float,
    ): Int {
        val o = opacity.coerceIn(0f, 1f)

        fun mix(shift: Int): Int {
            val t = (top shr shift) and MASK
            val b = (bottom shr shift) and MASK
            return ((t * o + b * (1 - o)) + HALF).toInt().coerceIn(0, MASK) shl shift
        }
        return OPAQUE or mix(RED) or mix(GREEN) or mix(BLUE)
    }

    private fun alpha(color: Int): Float = ((color ushr ALPHA) and MASK) / MASK.toFloat()

    /** WCAG relative luminance. */
    private fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and MASK) / MASK.toDouble()
            return if (c <= LINEAR_LIMIT) c / LINEAR_DIVISOR else ((c + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA)
        }
        return R_WEIGHT * channel(RED) + G_WEIGHT * channel(GREEN) + B_WEIGHT * channel(BLUE)
    }

    private const val MASK = 0xFF
    private const val OPAQUE = -0x1000000
    private const val ALPHA = 24
    private const val RED = 16
    private const val GREEN = 8
    private const val BLUE = 0
    private const val HALF = 0.5f
    private const val OFFSET = 0.05
    private const val LINEAR_LIMIT = 0.04045
    private const val LINEAR_DIVISOR = 12.92
    private const val GAMMA_OFFSET = 0.055
    private const val GAMMA_DIVISOR = 1.055
    private const val GAMMA = 2.4
    private const val R_WEIGHT = 0.2126
    private const val G_WEIGHT = 0.7152
    private const val B_WEIGHT = 0.0722
}

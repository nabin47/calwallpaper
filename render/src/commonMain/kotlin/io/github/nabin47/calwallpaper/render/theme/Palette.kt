package io.github.nabin47.calwallpaper.render.theme

import io.github.nabin47.calwallpaper.domain.settings.AppearanceSettings
import io.github.nabin47.calwallpaper.domain.settings.CustomColors
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset

/**
 * Colors of a theme as `0xAARRGGBB`, before the user's opacity settings are applied (see [applyOpacity]).
 *
 * @property onAccent text drawn on top of [accent] (e.g. today's date in a filled circle).
 * @property background fills the whole wallpaper behind the panel. Opaque; opacity settings never change it.
 */
data class Palette(
    val panel: Int,
    val text: Int,
    val secondaryText: Int,
    val accent: Int,
    val onAccent: Int,
    val divider: Int,
    val background: Int,
) {
    /** Built-in palettes and resolution from settings. */
    companion object {
        /** Light text on a dark panel. */
        val CALM_DARK: Palette =
            Palette(
                panel = 0xFF121417.toInt(),
                text = 0xFFF2F3F5.toInt(),
                secondaryText = 0xFFA9AFB8.toInt(),
                accent = 0xFF8AB4F8.toInt(),
                onAccent = 0xFF10131A.toInt(),
                divider = 0xFF2A2E35.toInt(),
                background = 0xFF2B3440.toInt(),
            )

        /** Dark text on a light panel. */
        val CALM_LIGHT: Palette =
            Palette(
                panel = 0xFFF7F8FA.toInt(),
                text = 0xFF1B1F24.toInt(),
                secondaryText = 0xFF5B6470.toInt(),
                accent = 0xFF1F5FBF.toInt(),
                onAccent = 0xFFFFFFFF.toInt(),
                divider = 0xFFDADDE2.toInt(),
                background = 0xFFC9D6E3.toInt(),
            )

        /** Warm off-white with ink-like text. */
        val PAPER: Palette =
            Palette(
                panel = 0xFFF4EFE6.toInt(),
                text = 0xFF2B2620.toInt(),
                secondaryText = 0xFF6E6458.toInt(),
                accent = 0xFFA0522D.toInt(),
                onAccent = 0xFFFFFFFF.toInt(),
                divider = 0xFFE0D7C8.toInt(),
                background = 0xFFE6DCCB.toInt(),
            )

        /** True black with a cool accent, for OLED screens. */
        val MIDNIGHT: Palette =
            Palette(
                panel = 0xFF000000.toInt(),
                text = 0xFFE6E9EF.toInt(),
                secondaryText = 0xFF8B93A1.toInt(),
                accent = 0xFF7DD3FC.toInt(),
                onAccent = 0xFF001018.toInt(),
                divider = 0xFF1C2028.toInt(),
                background = 0xFF000000.toInt(),
            )

        private const val SECONDARY_ALPHA = 0.7f
        private const val DIVIDER_ALPHA = 0.15f
        private const val LUMINANCE_THRESHOLD = 0.5f

        /**
         * The palette for [appearance]. [dynamic] is the platform's Material You palette, used when the user enabled
         * dynamic color and the platform supports it (pass `null` otherwise). [PalettePreset.CUSTOM] without custom
         * colors falls back to [CALM_DARK].
         */
        fun resolve(
            appearance: AppearanceSettings,
            dynamic: Palette? = null,
        ): Palette {
            if (appearance.useDynamicColor && dynamic != null) return dynamic
            return when (appearance.palette) {
                PalettePreset.CALM_DARK -> CALM_DARK
                PalettePreset.CALM_LIGHT -> CALM_LIGHT
                PalettePreset.PAPER -> PAPER
                PalettePreset.MIDNIGHT -> MIDNIGHT
                PalettePreset.CUSTOM -> appearance.customColors?.let(::fromCustom) ?: CALM_DARK
            }
        }

        private fun fromCustom(colors: CustomColors): Palette {
            val text = colors.text.argb
            val accent = colors.accent.argb
            val fallbackBackground =
                if (luminance(colors.panel.argb) > LUMINANCE_THRESHOLD) CALM_LIGHT.background else CALM_DARK.background
            return Palette(
                panel = colors.panel.argb,
                text = text,
                secondaryText = withAlpha(text, SECONDARY_ALPHA),
                accent = accent,
                onAccent = if (luminance(accent) > LUMINANCE_THRESHOLD) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                divider = withAlpha(text, DIVIDER_ALPHA),
                background = colors.background?.argb?.let(::opaque) ?: fallbackBackground,
            )
        }
    }
}

/**
 * The palette with the user's opacity settings applied: [panelOpacity] scales the panel's alpha and
 * [textOpacity] scales the alpha of every text and accent color.
 */
fun Palette.applyOpacity(
    panelOpacity: Float,
    textOpacity: Float,
): Palette =
    copy(
        panel = withAlpha(panel, panelOpacity),
        text = withAlpha(text, textOpacity),
        secondaryText = withAlpha(secondaryText, textOpacity),
        accent = withAlpha(accent, textOpacity),
        onAccent = withAlpha(onAccent, textOpacity),
        divider = withAlpha(divider, textOpacity),
    )

private const val CHANNEL_MAX = 255
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val RGB_MASK = 0x00FFFFFF
private const val RED_WEIGHT = 0.299f
private const val GREEN_WEIGHT = 0.587f
private const val BLUE_WEIGHT = 0.114f

/** [color] with its alpha multiplied by [factor] (clamped to 0..1). Pure. */
fun withAlpha(
    color: Int,
    factor: Float,
): Int {
    val alpha = (color ushr ALPHA_SHIFT) and CHANNEL_MASK
    val scaled = (alpha * factor.coerceIn(0f, 1f) + 0.5f).toInt().coerceIn(0, CHANNEL_MAX)
    return (scaled shl ALPHA_SHIFT) or (color and RGB_MASK)
}

/** [color] with full alpha: the wallpaper background must not be see-through. */
private fun opaque(color: Int): Int = color or (CHANNEL_MAX shl ALPHA_SHIFT)

/** The alpha channel of [color], 0..255. */
fun alphaOf(color: Int): Int = (color ushr ALPHA_SHIFT) and CHANNEL_MASK

/** Perceived brightness of [color], 0 (black) to 1 (white). */
private fun luminance(color: Int): Float {
    val r = (color shr RED_SHIFT) and CHANNEL_MASK
    val g = (color shr GREEN_SHIFT) and CHANNEL_MASK
    val b = color and CHANNEL_MASK
    return (RED_WEIGHT * r + GREEN_WEIGHT * g + BLUE_WEIGHT * b) / CHANNEL_MAX
}

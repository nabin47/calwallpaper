package io.github.nabin47.calwallpaper.ui.settings

import io.github.nabin47.calwallpaper.domain.settings.AppearanceSettings
import io.github.nabin47.calwallpaper.domain.settings.ArgbColor
import io.github.nabin47.calwallpaper.domain.settings.CustomColors
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContrastTest {
    @Test
    fun builtInPalettesAreReadableWithDefaults() {
        for (preset in PalettePreset.entries - PalettePreset.CUSTOM) {
            assertFalse(Contrast.isLow(AppearanceSettings(palette = preset)), "$preset with default opacity")
        }
    }

    @Test
    fun blackOnWhiteIsTheMaximumRatio() {
        val appearance =
            AppearanceSettings(
                palette = PalettePreset.CUSTOM,
                customColors =
                    CustomColors(
                        panel = ArgbColor(0xFFFFFFFF.toInt()),
                        text = ArgbColor(0xFF000000.toInt()),
                        accent = ArgbColor(0xFF000000.toInt()),
                        background = ArgbColor(0xFFFFFFFF.toInt()),
                    ),
                panelOpacity = 1f,
            )
        assertEquals(21f, Contrast.ratio(appearance), 0.01f)
    }

    @Test
    fun fadedTextIsFlagged() {
        assertTrue(Contrast.isLow(AppearanceSettings(textOpacity = 0.2f)))
    }
}

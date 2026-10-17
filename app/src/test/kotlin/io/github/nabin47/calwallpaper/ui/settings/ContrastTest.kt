package io.github.nabin47.calwallpaper.ui.settings

import io.github.nabin47.calwallpaper.domain.settings.AppearanceSettings
import io.github.nabin47.calwallpaper.domain.settings.ArgbColor
import io.github.nabin47.calwallpaper.domain.settings.CustomColors
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContrastTest {
    @Test
    fun builtInPalettesAreReadableWithDefaults() {
        for (preset in PalettePreset.entries - PalettePreset.CUSTOM) {
            Contrast.ratios(AppearanceSettings(palette = preset)).forEach { (role, ratio) ->
                assertTrue(ratio >= Contrast.MINIMUM_RATIO, "$preset $role with default opacity: $ratio")
            }
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
        val ratios = Contrast.ratios(appearance)
        assertEquals(21f, ratios.getValue(Contrast.TextRole.TEXT), 0.01f)
        assertEquals(21f, ratios.getValue(Contrast.TextRole.ACCENT), 0.01f)
        assertEquals(21f, ratios.getValue(Contrast.TextRole.ON_ACCENT), 0.01f)
        // Secondary text is the main text at 70% alpha, so it is the weakest role and what the warning reports.
        assertEquals(ratios.getValue(Contrast.TextRole.SECONDARY), Contrast.ratio(appearance), 0.001f)
        assertTrue(Contrast.ratio(appearance) < 21f)
    }

    @Test
    fun aFaintAccentIsFlaggedEvenWhenTheMainTextIsFine() {
        val appearance =
            AppearanceSettings(
                palette = PalettePreset.CUSTOM,
                customColors =
                    CustomColors(
                        panel = ArgbColor(0xFFFFFFFF.toInt()),
                        text = ArgbColor(0xFF000000.toInt()),
                        // Pale yellow on white: about 1.1:1.
                        accent = ArgbColor(0xFFFFF59D.toInt()),
                        background = ArgbColor(0xFFFFFFFF.toInt()),
                    ),
                panelOpacity = 1f,
            )
        assertTrue(Contrast.ratios(appearance).getValue(Contrast.TextRole.TEXT) > Contrast.MINIMUM_RATIO)
        assertTrue(Contrast.isLow(appearance))
    }

    @Test
    fun fadedTextIsFlagged() {
        assertTrue(Contrast.isLow(AppearanceSettings(textOpacity = 0.2f)))
    }
}

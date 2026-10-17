package io.github.nabin47.calwallpaper.render.theme

import io.github.nabin47.calwallpaper.domain.settings.AppearanceSettings
import io.github.nabin47.calwallpaper.domain.settings.ArgbColor
import io.github.nabin47.calwallpaper.domain.settings.CustomColors
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import kotlin.test.Test
import kotlin.test.assertEquals

class PaletteTest {
    @Test
    fun resolvesPresetsDynamicAndCustom() {
        assertEquals(Palette.CALM_LIGHT, Palette.resolve(AppearanceSettings(palette = PalettePreset.CALM_LIGHT)))
        val dynamic = Palette.PAPER
        assertEquals(dynamic, Palette.resolve(AppearanceSettings(useDynamicColor = true), dynamic))
        assertEquals(Palette.CALM_DARK, Palette.resolve(AppearanceSettings(useDynamicColor = true), null))
        assertEquals(Palette.CALM_DARK, Palette.resolve(AppearanceSettings(palette = PalettePreset.CUSTOM)))
        val custom =
            Palette.resolve(
                AppearanceSettings(
                    palette = PalettePreset.CUSTOM,
                    customColors =
                        CustomColors(
                            ArgbColor(0xFF000000.toInt()),
                            ArgbColor(0xFFFFFFFF.toInt()),
                            ArgbColor(0xFFFFEB3B.toInt()),
                        ),
                ),
            )
        assertEquals(0xFF000000.toInt(), custom.onAccent, "bright accent gets dark text")
        assertEquals(Palette.CALM_DARK.background, custom.background, "dark panel gets the dark default background")
    }

    @Test
    fun customBackgroundIsOpaqueAndSurvivesOpacity() {
        val custom =
            Palette.resolve(
                AppearanceSettings(
                    palette = PalettePreset.CUSTOM,
                    customColors =
                        CustomColors(
                            panel = ArgbColor(0xFFFFFFFF.toInt()),
                            text = ArgbColor(0xFF000000.toInt()),
                            accent = ArgbColor(0xFF1F5FBF.toInt()),
                            background = ArgbColor(0x40123456),
                        ),
                ),
            )
        assertEquals(0xFF123456.toInt(), custom.background)
        assertEquals(custom.background, custom.applyOpacity(0f, 0f).background)
    }

    @Test
    fun opacityScalesAlpha() {
        val applied = Palette.CALM_DARK.applyOpacity(panelOpacity = 0.5f, textOpacity = 0.25f)
        assertEquals(128, alphaOf(applied.panel))
        assertEquals(64, alphaOf(applied.text))
        assertEquals(0x00FFFFFF and Palette.CALM_DARK.text, 0x00FFFFFF and applied.text)
    }
}

package io.github.nabin47.calwallpaper.widgets

import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.LayoutSettings
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.VerticalPosition
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetSettingsTest {
    private val user =
        Settings(
            layout =
                LayoutSettings(
                    preset = LayoutPreset.MONTH_GRID,
                    position = VerticalPosition.BOTTOM,
                    safeZoneTop = 0.3f,
                    safeZoneBottom = 0.2f,
                    horizontalInsetDp = 24,
                ),
            privacyMode = PrivacyMode.ALWAYS,
        )

    @Test
    fun eachWidgetUsesItsOwnPresetWithoutWallpaperInsets() {
        val expected = mapOf(WidgetKind.AGENDA to LayoutPreset.AGENDA, WidgetKind.WEEK to LayoutPreset.WEEK_STRIP)
        for ((kind, preset) in expected) {
            val layout = widgetSettings(user, kind).layout
            assertEquals(preset, layout.preset)
            for (locked in listOf(false, true)) {
                val placement = layout.placementFor(locked)
                assertEquals(VerticalPosition.TOP, placement.position)
                assertEquals(0f, placement.safeZoneTop)
                assertEquals(0f, placement.safeZoneBottom)
            }
            assertEquals(0, layout.horizontalInsetDp)
        }
    }

    @Test
    fun everythingElseFollowsTheUser() {
        val widget = widgetSettings(user, WidgetKind.AGENDA)
        assertEquals(user.copy(layout = widget.layout), widget)
    }

    @Test
    fun presetsAreDistinct() {
        assertEquals(
            WidgetKind.entries.size,
            WidgetKind.entries
                .map { it.preset }
                .toSet()
                .size,
        )
    }
}

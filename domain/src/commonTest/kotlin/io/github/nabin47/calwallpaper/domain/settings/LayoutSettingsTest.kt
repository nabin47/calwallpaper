package io.github.nabin47.calwallpaper.domain.settings

import io.github.nabin47.calwallpaper.domain.SafeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutSettingsTest {
    @Test
    fun safeZoneNeverThrowsOnBadValues() {
        assertEquals(SafeZone.DEFAULT, LayoutSettings(safeZoneTop = Float.NaN, safeZoneBottom = Float.NaN).safeZone)
        assertEquals(SafeZone(0.5f, 0.2f), LayoutSettings(safeZoneTop = 3f, safeZoneBottom = 0.2f).safeZone)
    }

    @Test
    fun defaultsMatchSafeZoneDefault() {
        assertEquals(SafeZone.DEFAULT, LayoutSettings().safeZone)
    }
}

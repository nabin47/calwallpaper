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

    @Test
    fun lockScreenHasItsOwnPlacement() {
        val layout = LayoutSettings()
        assertEquals(Placement(VerticalPosition.CENTER, 0.25f, 0.12f), layout.placementFor(locked = false))
        assertEquals(
            Placement(
                VerticalPosition.BOTTOM,
                LayoutSettings.DEFAULT_LOCK_SAFE_ZONE_TOP,
                LayoutSettings.DEFAULT_LOCK_SAFE_ZONE_BOTTOM,
            ),
            layout.placementFor(locked = true),
        )
    }

    @Test
    fun lockSafeZonesAreClampedAndMustLeaveRoom() {
        val clamped = LayoutSettings(lockSafeZoneTop = 2f, lockSafeZoneBottom = 0.1f).placementFor(locked = true)
        assertEquals(0.75f, clamped.safeZoneTop, "top clamps to the lock range")
        val noRoom = LayoutSettings(lockSafeZoneTop = 0.75f, lockSafeZoneBottom = 0.5f).placementFor(locked = true)
        assertEquals(LayoutSettings.DEFAULT_LOCK_SAFE_ZONE_TOP, noRoom.safeZoneTop, "falls back when nothing fits")
        assertEquals(LayoutSettings.DEFAULT_LOCK_SAFE_ZONE_BOTTOM, noRoom.safeZoneBottom)
        val nan = LayoutSettings(lockSafeZoneTop = Float.NaN).placementFor(locked = true)
        assertEquals(LayoutSettings.DEFAULT_LOCK_SAFE_ZONE_TOP, nan.safeZoneTop)
    }
}

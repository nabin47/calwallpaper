package io.github.nabin47.calwallpaper.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SafeZoneTest {
    @Test
    fun defaultLeavesRoomForContent() {
        assertTrue(SafeZone.DEFAULT.top + SafeZone.DEFAULT.bottom < 1f)
    }

    @Test
    fun rejectsOutOfRangeValues() {
        assertFailsWith<IllegalArgumentException> { SafeZone(top = -0.1f, bottom = 0f) }
        assertFailsWith<IllegalArgumentException> { SafeZone(top = 0f, bottom = 0.6f) }
    }
}

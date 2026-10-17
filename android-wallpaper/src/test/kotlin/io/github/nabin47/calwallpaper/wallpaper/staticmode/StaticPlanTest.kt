package io.github.nabin47.calwallpaper.wallpaper.staticmode

import io.github.nabin47.calwallpaper.domain.settings.LayoutSettings
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.VerticalPosition
import io.github.nabin47.calwallpaper.domain.settings.WallpaperTarget
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class StaticPlanTest {
    @Test
    fun singleScreenTargetsGetOneImage() {
        assertEquals(
            listOf(StaticImage(locked = false, screens = setOf(Screen.HOME))),
            StaticPlan.images(WallpaperTarget.HOME, PrivacyMode.WHEN_LOCKED, LayoutSettings()),
        )
        assertEquals(
            listOf(StaticImage(locked = true, screens = setOf(Screen.LOCK))),
            StaticPlan.images(WallpaperTarget.LOCK, PrivacyMode.OFF, LayoutSettings()),
        )
    }

    @Test
    fun bothScreensShareAnImageOnlyWhenTheyLookTheSame() {
        val samePlacement =
            LayoutSettings(
                position = VerticalPosition.BOTTOM,
                safeZoneTop = 0.3f,
                safeZoneBottom = 0.2f,
                lockPosition = VerticalPosition.BOTTOM,
                lockSafeZoneTop = 0.3f,
                lockSafeZoneBottom = 0.2f,
            )
        val split = StaticPlan.images(WallpaperTarget.HOME_AND_LOCK, PrivacyMode.WHEN_LOCKED, samePlacement)
        assertEquals(2, split.size)
        assertEquals(setOf(false, true), split.map { it.locked }.toSet())
        for (privacy in listOf(PrivacyMode.OFF, PrivacyMode.ALWAYS)) {
            val shared = StaticPlan.images(WallpaperTarget.HOME_AND_LOCK, privacy, samePlacement)
            assertEquals(1, shared.size, "$privacy: home and lock look the same")
            assertEquals(setOf(Screen.HOME, Screen.LOCK), shared.single().screens)
        }
    }

    @Test
    fun bothScreensSplitWhenTheLockScreenHasItsOwnPlacement() {
        for (privacy in PrivacyMode.entries) {
            val images = StaticPlan.images(WallpaperTarget.HOME_AND_LOCK, privacy, LayoutSettings())
            assertEquals(
                listOf(
                    StaticImage(locked = false, screens = setOf(Screen.HOME)),
                    StaticImage(locked = true, screens = setOf(Screen.LOCK)),
                ),
                images,
                "$privacy: the default lock placement differs from home",
            )
        }
    }

    @Test
    fun midnightRefreshIsJustAfterTheNextLocalMidnight() {
        val dhaka = TimeZone.of("Asia/Dhaka") // UTC+6, no DST.
        assertEquals(
            Instant.parse("2026-10-17T18:01:00Z"),
            StaticPlan.nextMidnightRefresh(Instant.parse("2026-10-17T10:00:00Z"), dhaka),
        )
        // Just after midnight already: the next one is a day later.
        assertEquals(
            Instant.parse("2026-10-18T18:01:00Z"),
            StaticPlan.nextMidnightRefresh(Instant.parse("2026-10-17T18:00:30Z"), dhaka),
        )
    }

    @Test
    fun midnightRefreshFollowsDaylightSavingChanges() {
        val berlin = TimeZone.of("Europe/Berlin") // Clocks go back on 2026-10-25.
        assertEquals(
            Instant.parse("2026-10-25T23:01:00Z"),
            StaticPlan.nextMidnightRefresh(Instant.parse("2026-10-25T12:00:00Z"), berlin),
        )
    }
}

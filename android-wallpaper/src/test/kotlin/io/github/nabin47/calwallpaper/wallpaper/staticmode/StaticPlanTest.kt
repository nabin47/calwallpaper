package io.github.nabin47.calwallpaper.wallpaper.staticmode

import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
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
            StaticPlan.images(WallpaperTarget.HOME, PrivacyMode.WHEN_LOCKED),
        )
        assertEquals(
            listOf(StaticImage(locked = true, screens = setOf(Screen.LOCK))),
            StaticPlan.images(WallpaperTarget.LOCK, PrivacyMode.OFF),
        )
    }

    @Test
    fun bothScreensSplitOnlyWhenPrivacyDependsOnLockState() {
        val split = StaticPlan.images(WallpaperTarget.HOME_AND_LOCK, PrivacyMode.WHEN_LOCKED)
        assertEquals(2, split.size)
        assertEquals(setOf(false, true), split.map { it.locked }.toSet())
        for (privacy in listOf(PrivacyMode.OFF, PrivacyMode.ALWAYS)) {
            val shared = StaticPlan.images(WallpaperTarget.HOME_AND_LOCK, privacy)
            assertEquals(1, shared.size, "$privacy: home and lock look the same")
            assertEquals(setOf(Screen.HOME, Screen.LOCK), shared.single().screens)
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

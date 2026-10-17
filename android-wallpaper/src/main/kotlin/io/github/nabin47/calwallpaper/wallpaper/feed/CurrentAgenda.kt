package io.github.nabin47.calwallpaper.wallpaper.feed

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * One-shot read of the agenda for [settings] as of now, for renderers that draw once instead of observing (the
 * static image, widgets). Before the first unlock only the Direct Boot cache is readable, so that is returned.
 * A source that does not answer within a few seconds counts as empty. Call from a background coroutine.
 */
suspend fun WallpaperGraph.currentAgenda(settings: Settings): List<AgendaItem> {
    if (!deviceState.isUserUnlocked()) return directBootCache.read()
    val zone = TimeZone.currentSystemDefault()
    val key =
        FeedKey.of(
            settings = settings,
            today =
                Clock.System
                    .now()
                    .toLocalDateTime(zone)
                    .date,
            zone = zone,
            localeFirstDayOfWeek = requestFactory.localeFirstDayOfWeek(),
            unlocked = true,
            calendarPermission = deviceState.hasCalendarPermission(),
        )
    return withTimeoutOrNull(LOAD_TIMEOUT) { feed.observe(key).first() }.orEmpty()
}

private val LOAD_TIMEOUT = 10.seconds

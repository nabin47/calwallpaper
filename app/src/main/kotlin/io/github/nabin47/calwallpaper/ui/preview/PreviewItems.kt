package io.github.nabin47.calwallpaper.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.nabin47.calwallpaper.AppServices
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.wallpaper.feed.FeedKey
import io.github.nabin47.calwallpaper.wallpaper.staticmode.ScreenSize
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Items for the previews, whether they are the sample agenda, and the "now" they are laid out at. */
data class PreviewItems(
    val items: List<AgendaItem>,
    val isSample: Boolean,
    val now: Instant,
)

/**
 * The user's agenda for the previews, observed live, or a sample when calendar access is missing and there is
 * nothing else to show. "Now" is fixed per resume so the previews do not re-lay out on every recomposition.
 */
@Composable
fun rememberPreviewItems(
    services: AppServices,
    settings: Settings,
    calendarGranted: Boolean,
    resumeCount: Int,
): PreviewItems {
    val now = remember(resumeCount) { Clock.System.now() }
    val zone = remember(resumeCount) { TimeZone.currentSystemDefault() }
    val today = now.toLocalDateTime(zone).date
    val key =
        FeedKey.of(
            settings = settings,
            today = today,
            zone = zone,
            localeFirstDayOfWeek = services.graph.requestFactory.localeFirstDayOfWeek(),
            unlocked = true,
            calendarPermission = calendarGranted,
        )
    val live by produceState<List<AgendaItem>?>(null, key) {
        services.graph.feed
            .observe(key)
            .collect { value = it }
    }
    val titles =
        SampleTitles(
            standup = stringResource(R.string.sample_standup),
            lunch = stringResource(R.string.sample_lunch),
            bill = stringResource(R.string.sample_bill),
            dentist = stringResource(R.string.sample_dentist),
            trip = stringResource(R.string.sample_trip),
            groceries = stringResource(R.string.sample_groceries),
        )
    val useSample = !calendarGranted && live.isNullOrEmpty()
    val items = if (useSample) remember(today, titles) { SampleAgenda.items(today, zone, titles) } else live.orEmpty()
    return PreviewItems(items = items, isSample = useSample, now = now)
}

/**
 * A full-screen render request for the previews: this phone's portrait size, the given [settings] and
 * [preview] items, as the home screen or, with [locked], as the lock screen (lock placement and privacy).
 */
@Composable
fun rememberPreviewRequest(
    services: AppServices,
    settings: Settings,
    preview: PreviewItems,
    locked: Boolean,
): RenderRequest {
    val context = LocalContext.current
    val screen = remember { ScreenSize.portrait(context) }
    return remember(settings, preview, locked) {
        services.graph.requestFactory.create(preview.items, settings, screen.width, screen.height, locked, preview.now)
    }
}

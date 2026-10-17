package io.github.nabin47.calwallpaper.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.AppServices
import io.github.nabin47.calwallpaper.BuildConfig
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.getOrNull
import io.github.nabin47.calwallpaper.data.calendar.DeviceCalendar
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.render.theme.Palette
import io.github.nabin47.calwallpaper.ui.CalendarPermissionState
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.preview.LivePreview
import io.github.nabin47.calwallpaper.ui.preview.SampleAgenda
import io.github.nabin47.calwallpaper.ui.preview.SampleTitles
import io.github.nabin47.calwallpaper.ui.rememberCalendarPermission
import io.github.nabin47.calwallpaper.wallpaper.LiveWallpaper
import io.github.nabin47.calwallpaper.wallpaper.diagnostics.PerfReport
import io.github.nabin47.calwallpaper.wallpaper.feed.FeedKey
import io.github.nabin47.calwallpaper.wallpaper.staticmode.ScreenSize
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Every setting of CLAUDE.md §6 under a live preview that stays on screen while scrolling, so each change is
 * visible immediately. The preview uses the user's real agenda once calendar access is granted, and a sample
 * agenda before that.
 */
@Composable
fun SettingsScreen(
    services: AppServices,
    editor: SettingsEditor,
    settings: Settings,
    resumeCount: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permission = rememberCalendarPermission(resumeCount)
    var previewLocked by rememberSaveable { mutableStateOf(false) }
    val items = rememberPreviewItems(services, settings, permission.granted, resumeCount)
    val screen = remember { ScreenSize.portrait(context) }
    val factory = services.graph.requestFactory
    val request = factory.create(items.items, settings, screen.width, screen.height, previewLocked, items.now)
    val dynamicPalette = remember(resumeCount, settings.appearance.useDynamicColor) { factory.dynamicPalette() }
    val calendars by produceState(emptyList<DeviceCalendar>(), permission.granted, resumeCount) {
        value =
            services.calendars
                .list()
                .getOrNull()
                .orEmpty()
    }
    val liveActive = remember(resumeCount) { LiveWallpaper.isActive(context) }
    val report by produceState(PerfReport(), resumeCount) { value = services.graph.diagnostics.read() }
    RefreshOnPermissionChange(services, settings, permission.granted)

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(PREVIEW_HEIGHT_FRACTION).padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LivePreview(
                request = request,
                description = stringResource(R.string.preview_description),
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.preview_locked), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = previewLocked, onCheckedChange = { previewLocked = it })
            }
        }
        HorizontalDivider()
        SettingsList(
            settings = settings,
            editor = editor,
            permission = permission,
            calendars = calendars,
            isSample = items.isSample,
            liveActive = liveActive,
            report = report,
            dynamicPalette = dynamicPalette,
            applyStatic = { services.staticUpdater.refresh(force = true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SettingsList(
    settings: Settings,
    editor: SettingsEditor,
    permission: CalendarPermissionState,
    calendars: List<DeviceCalendar>,
    isSample: Boolean,
    liveActive: Boolean,
    report: PerfReport,
    dynamicPalette: Palette?,
    applyStatic: suspend () -> Outcome<Unit>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        if (isSample) item { SettingHint(stringResource(R.string.preview_sample)) }
        item { WallpaperSection(settings, editor, liveActive, applyStatic) }
        item { AppearanceSection(settings, editor, dynamicPalette) }
        item { LayoutSection(settings, editor) }
        item { ContentSection(settings, editor, permission, calendars) }
        item { TimeSection(settings, editor) }
        item { PrivacySection(settings, editor) }
        item { DiagnosticsSection(report) }
        item {
            Text(
                text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }
    }
}

/**
 * Re-applies the battery saver image and redraws the widgets when calendar access is granted or revoked (in
 * the dialog or in system settings): the content-trigger job only sees calendar data changes, not permission
 * changes.
 */
@Composable
private fun RefreshOnPermissionChange(
    services: AppServices,
    settings: Settings,
    granted: Boolean,
) {
    val currentSettings by rememberUpdatedState(settings)
    var lastSeen by remember { mutableStateOf(granted) }
    LaunchedEffect(granted) {
        if (granted == lastSeen) return@LaunchedEffect
        lastSeen = granted
        if (currentSettings.batteryMode == BatteryMode.STATIC) services.staticUpdater.refresh()
        services.widgets.requestUpdate()
    }
}

/** Items for the preview and whether they are the sample agenda. */
private data class PreviewItems(
    val items: List<AgendaItem>,
    val isSample: Boolean,
    val now: Instant,
)

/**
 * The user's agenda for the preview, observed live, or a sample when calendar access is missing and there is
 * nothing else to show. "Now" is fixed per resume so the preview does not re-lay out on every recomposition.
 */
@Composable
private fun rememberPreviewItems(
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

/** Share of the screen height the preview takes; the settings scroll below it. */
private const val PREVIEW_HEIGHT_FRACTION = 0.4f

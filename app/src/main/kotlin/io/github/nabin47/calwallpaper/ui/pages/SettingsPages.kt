package io.github.nabin47.calwallpaper.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.getOrNull
import io.github.nabin47.calwallpaper.data.calendar.DeviceCalendar
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.components.NavigationRow
import io.github.nabin47.calwallpaper.ui.components.PageScaffold
import io.github.nabin47.calwallpaper.ui.components.PreviewPane
import io.github.nabin47.calwallpaper.ui.navigation.Route
import io.github.nabin47.calwallpaper.ui.settings.AppearanceSection
import io.github.nabin47.calwallpaper.ui.settings.ContentSection
import io.github.nabin47.calwallpaper.ui.settings.LayoutSection
import io.github.nabin47.calwallpaper.ui.settings.LockScreenPlacementSection
import io.github.nabin47.calwallpaper.ui.settings.PrivacySection
import io.github.nabin47.calwallpaper.ui.settings.TimeSection
import io.github.nabin47.calwallpaper.ui.settings.TodoistSection
import io.github.nabin47.calwallpaper.ui.settings.WallpaperSection
import io.github.nabin47.calwallpaper.wallpaper.LiveWallpaper

/** Colors, font, text size, opacity, corners and spacing, with a preview of either screen. */
@Composable
fun AppearancePage(state: ScreenState) {
    var locked by rememberSaveable { mutableStateOf(false) }
    val factory = state.services.graph.requestFactory
    val dynamicPalette =
        remember(state.resumeCount, state.settings.appearance.useDynamicColor) { factory.dynamicPalette() }
    PageScaffold(
        state = state,
        title = stringResource(R.string.section_appearance),
        preview = { PreviewPane(state, locked, onLockedChange = { locked = it }) },
    ) {
        item { AppearanceSection(state.settings, state.editor, dynamicPalette) }
    }
}

/** Layout preset and home-screen placement, previewed as the home screen. */
@Composable
fun LayoutPage(state: ScreenState) {
    PageScaffold(
        state = state,
        title = stringResource(R.string.section_layout),
        preview = { PreviewPane(state, locked = false, onLockedChange = null) },
    ) {
        item { LayoutSection(state.settings, state.editor) }
    }
}

/**
 * Privacy mode and lock-screen placement. Always previewed as the lock screen, because nothing on this page
 * changes the home screen.
 */
@Composable
fun LockScreenPage(state: ScreenState) {
    PageScaffold(
        state = state,
        title = stringResource(R.string.section_lock_screen),
        preview = { PreviewPane(state, locked = true, onLockedChange = null) },
    ) {
        item { PrivacySection(state.settings, state.editor) }
        item { LockScreenPlacementSection(state.settings, state.editor) }
    }
}

/** Calendars, local tasks, the date range and completed tasks, with a link to Todoist. */
@Composable
fun SourcesPage(state: ScreenState) {
    var locked by rememberSaveable { mutableStateOf(false) }
    val granted = state.permission.granted
    val calendars by produceState(emptyList<DeviceCalendar>(), granted, state.resumeCount) {
        value =
            state.services.calendars
                .list()
                .getOrNull()
                .orEmpty()
    }
    val todoistOn = SourceId.TODOIST in state.settings.content.enabledSources
    PageScaffold(
        state = state,
        title = stringResource(R.string.section_content),
        preview = { PreviewPane(state, locked, onLockedChange = { locked = it }) },
    ) {
        item { ContentSection(state.settings, state.editor, state.permission, calendars) }
        item {
            NavigationRow(
                icon = R.drawable.ic_checklist,
                title = stringResource(R.string.section_todoist),
                summary =
                    stringResource(
                        if (todoistOn) R.string.summary_todoist_on else R.string.summary_todoist_off,
                    ),
                onClick = { state.navigate(Route.Todoist) },
            )
        }
    }
}

/** Connecting Todoist with a personal token, sync status and project choice. */
@Composable
fun TodoistPage(state: ScreenState) {
    PageScaffold(state = state, title = stringResource(R.string.section_todoist)) {
        item { TodoistSection(state.settings, state.editor, state.services.todoist) }
    }
}

/** Week start and time format, with a preview of either screen. */
@Composable
fun DateTimePage(state: ScreenState) {
    var locked by rememberSaveable { mutableStateOf(false) }
    PageScaffold(
        state = state,
        title = stringResource(R.string.section_time),
        preview = { PreviewPane(state, locked, onLockedChange = { locked = it }) },
    ) {
        item { TimeSection(state.settings, state.editor) }
    }
}

/** Live wallpaper or battery-saver image, and applying it. */
@Composable
fun WallpaperModePage(state: ScreenState) {
    val context = LocalContext.current
    val liveActive = remember(state.resumeCount) { LiveWallpaper.isActive(context) }
    PageScaffold(state = state, title = stringResource(R.string.section_wallpaper)) {
        item {
            WallpaperSection(
                settings = state.settings,
                editor = state.editor,
                liveActive = liveActive,
                applyStatic = { state.services.staticUpdater.refresh(force = true) },
                snackbar = state.snackbar,
            )
        }
    }
}

package io.github.nabin47.calwallpaper.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.nabin47.calwallpaper.AppServices
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.ui.home.HomeScreen
import io.github.nabin47.calwallpaper.ui.navigation.Route
import io.github.nabin47.calwallpaper.ui.onboarding.OnboardingScreen
import io.github.nabin47.calwallpaper.ui.pages.AboutPage
import io.github.nabin47.calwallpaper.ui.pages.AppearancePage
import io.github.nabin47.calwallpaper.ui.pages.BackupPage
import io.github.nabin47.calwallpaper.ui.pages.DateTimePage
import io.github.nabin47.calwallpaper.ui.pages.LayoutPage
import io.github.nabin47.calwallpaper.ui.pages.LockScreenPage
import io.github.nabin47.calwallpaper.ui.pages.SourcesPage
import io.github.nabin47.calwallpaper.ui.pages.TodoistPage
import io.github.nabin47.calwallpaper.ui.pages.WallpaperModePage
import io.github.nabin47.calwallpaper.ui.pages.WidgetsPage
import io.github.nabin47.calwallpaper.ui.preview.rememberPreviewItems
import io.github.nabin47.calwallpaper.ui.settings.SettingsEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Root of the UI. Loads the settings (the splash screen stays up until they are in, see [onReady]), then shows
 * onboarding on first launch and afterwards the home screen and its pages, hosted by Navigation 3 with
 * predictive back (ADR 0009). Owns the [SettingsEditor], the calendar permission, the preview agenda and the
 * snackbar, so every screen shares one view of them, and keeps static (battery) mode and the widgets in sync
 * after each change.
 */
@Composable
fun AppRoot(
    services: AppServices,
    resumeCount: Int,
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val staticSync = remember { StaticSync(services, scope) }
    val editor = remember { SettingsEditor(services.graph.settings, scope, staticSync::onCommitted) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        var first = true
        services.graph.settings.settings.collect { stored ->
            editor.onStored(stored)
            // Re-assert static-mode triggers once per launch (e.g. after an app update reset components).
            if (first) services.staticScheduler.sync(stored)
            first = false
        }
    }
    val settings = editor.current
    Surface(modifier = modifier.fillMaxSize()) {
        if (settings == null) return@Surface
        LaunchedEffect(Unit) { onReady() }
        val permission = rememberCalendarPermission(resumeCount)
        RefreshOnPermissionChange(services, settings, permission.granted)
        val preview = rememberPreviewItems(services, settings, permission.granted, resumeCount)
        val backStack = rememberNavBackStack(Route.Home)
        val state =
            ScreenState(
                services = services,
                editor = editor,
                settings = settings,
                permission = permission,
                preview = preview,
                resumeCount = resumeCount,
                snackbar = snackbar,
                navigate = { route -> backStack.add(route) },
                back = { backStack.pop() },
            )
        var onboardingDone by rememberSaveable { mutableStateOf(services.onboarding.isDone()) }
        if (onboardingDone) {
            Screens(state, backStack)
        } else {
            OnboardingScreen(state) {
                services.onboarding.markDone()
                onboardingDone = true
            }
        }
    }
}

/** The home screen and every page, keyed by [Route]. */
@Composable
private fun Screens(
    state: ScreenState,
    backStack: MutableList<NavKey>,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.pop() },
        entryProvider =
            entryProvider {
                entry<Route.Home> { HomeScreen(state) }
                entry<Route.Appearance> { AppearancePage(state) }
                entry<Route.Layout> { LayoutPage(state) }
                entry<Route.LockScreen> { LockScreenPage(state) }
                entry<Route.Sources> { SourcesPage(state) }
                entry<Route.Todoist> { TodoistPage(state) }
                entry<Route.DateTime> { DateTimePage(state) }
                entry<Route.WallpaperMode> { WallpaperModePage(state) }
                entry<Route.Widgets> { WidgetsPage(state) }
                entry<Route.Backup> { BackupPage(state) }
                entry<Route.About> { AboutPage(state) }
            },
    )
}

/** Removes the top screen, never the home screen at the bottom. */
private fun MutableList<NavKey>.pop() {
    if (size > 1) removeAt(lastIndex)
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

/**
 * Bookkeeping after each settings change: background refresh triggers, the static image, the widgets and the
 * Direct Boot cache. Main thread only.
 */
private class StaticSync(
    private val services: AppServices,
    private val scope: CoroutineScope,
) {
    private var pendingApply: Job? = null

    fun onCommitted(
        before: Settings,
        after: Settings,
    ) {
        if (before.batteryMode != after.batteryMode) services.staticScheduler.sync(after)
        if (before.privacyMode != after.privacyMode) {
            // Strip titles from the Direct Boot cache right away, even if the live wallpaper is not running.
            scope.launch { services.graph.directBootCache.restrictTo(after.privacyMode) }
        }
        pendingApply?.cancel()
        val switched = before.batteryMode != BatteryMode.STATIC && after.batteryMode == BatteryMode.STATIC
        pendingApply =
            scope.launch {
                if (!switched) delay(APPLY_DELAY_MS)
                if (after.batteryMode == BatteryMode.STATIC) services.staticUpdater.refresh(force = switched)
                services.widgets.requestUpdate()
            }
    }

    private companion object {
        /** Waits for a pause in editing, so a burst of changes costs one wallpaper and widget update. */
        const val APPLY_DELAY_MS = 1_500L
    }
}

package io.github.nabin47.calwallpaper.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.nabin47.calwallpaper.AppServices
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.ui.onboarding.OnboardingScreen
import io.github.nabin47.calwallpaper.ui.settings.SettingsEditor
import io.github.nabin47.calwallpaper.ui.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Root of the UI: onboarding on first launch, then settings. Owns the [SettingsEditor] so both share one view of
 * the settings, and keeps static (battery) mode in sync: its triggers follow the mode, and in static mode each
 * change is applied to the wallpaper shortly after the user stops changing things.
 */
@Composable
fun AppRoot(
    services: AppServices,
    resumeCount: Int,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val staticSync = remember { StaticSync(services, scope) }
    val editor = remember { SettingsEditor(services.graph.settings, scope, staticSync::onCommitted) }
    LaunchedEffect(Unit) {
        var first = true
        services.graph.settings.settings.collect { stored ->
            editor.onStored(stored)
            // Re-assert static-mode triggers once per launch (e.g. after an app update reset components).
            if (first) services.staticScheduler.sync(stored)
            first = false
        }
    }
    var onboardingDone by rememberSaveable { mutableStateOf(services.onboarding.isDone()) }
    Surface(modifier = modifier.fillMaxSize()) {
        val settings = editor.current ?: return@Surface
        if (onboardingDone) {
            SettingsScreen(services = services, editor = editor, settings = settings, resumeCount = resumeCount)
        } else {
            OnboardingScreen(services = services, editor = editor, settings = settings, resumeCount = resumeCount) {
                services.onboarding.markDone()
                onboardingDone = true
            }
        }
    }
}

/** Static-mode bookkeeping after each settings change. Main thread only. */
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
        if (after.batteryMode != BatteryMode.STATIC) return
        pendingApply?.cancel()
        val switched = before.batteryMode != BatteryMode.STATIC
        pendingApply =
            scope.launch {
                if (!switched) delay(APPLY_DELAY_MS)
                services.staticUpdater.refresh(force = switched)
            }
    }

    private companion object {
        /** Waits for a pause in editing, so a burst of changes costs one wallpaper update. */
        const val APPLY_DELAY_MS = 1_500L
    }
}

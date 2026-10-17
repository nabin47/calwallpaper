package io.github.nabin47.calwallpaper.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import io.github.nabin47.calwallpaper.AppServices
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.ui.navigation.Route
import io.github.nabin47.calwallpaper.ui.preview.PreviewItems
import io.github.nabin47.calwallpaper.ui.settings.SettingsEditor

/**
 * What every screen needs, gathered once in [AppRoot] so screens take one parameter instead of ten.
 *
 * @property settings what the screen shows: the editor's draft while dragging, else the stored settings.
 * @property preview the agenda the previews draw (the user's, or a sample before calendar access).
 * @property resumeCount bumped on every resume; screens re-check things the user may have changed elsewhere.
 * @property navigate opens a screen on top of the current one; [back] closes the current one.
 */
@Immutable
data class ScreenState(
    val services: AppServices,
    val editor: SettingsEditor,
    val settings: Settings,
    val permission: CalendarPermissionState,
    val preview: PreviewItems,
    val resumeCount: Int,
    val snackbar: SnackbarHostState,
    val navigate: (Route) -> Unit,
    val back: () -> Unit,
)

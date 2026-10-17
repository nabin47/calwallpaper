package io.github.nabin47.calwallpaper.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.WallpaperTarget
import io.github.nabin47.calwallpaper.ui.components.Choice
import io.github.nabin47.calwallpaper.ui.components.ChoiceSetting
import io.github.nabin47.calwallpaper.ui.components.SectionHeader
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.wallpaper.LiveWallpaper
import kotlinx.coroutines.launch

/**
 * Live versus battery-saver (static) mode, and how to apply it. Live mode hands over to the system's live
 * wallpaper preview, which asks home, lock or both. Static mode applies the image itself, to the screens chosen
 * here. [applyStatic] sets the static image now and reports the outcome.
 */
@Composable
fun WallpaperSection(
    settings: Settings,
    editor: SettingsEditor,
    liveActive: Boolean,
    applyStatic: suspend () -> Outcome<Unit>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<Int?>(null) }

    Column(modifier) {
        SectionHeader(stringResource(R.string.section_wallpaper))
        ChoiceSetting(
            label = stringResource(R.string.mode_label),
            choices =
                listOf(
                    Choice(BatteryMode.LIVE, stringResource(R.string.mode_live)),
                    Choice(BatteryMode.STATIC, stringResource(R.string.mode_static)),
                ),
            selected = settings.batteryMode,
            onSelect = { mode ->
                message = null
                editor.commit { it.copy(batteryMode = mode) }
            },
        )
        when (settings.batteryMode) {
            BatteryMode.LIVE -> {
                SettingHint(stringResource(R.string.mode_live_body))
                SettingHint(stringResource(if (liveActive) R.string.live_active else R.string.live_inactive))
                Button(
                    onClick = { message = if (openLivePicker(context)) null else R.string.live_unavailable },
                    modifier = Modifier.padding(vertical = 4.dp),
                ) { Text(stringResource(R.string.live_set)) }
                SettingHint(stringResource(R.string.live_set_hint))
            }

            BatteryMode.STATIC -> {
                SettingHint(stringResource(R.string.mode_static_body))
                ChoiceSetting(
                    label = stringResource(R.string.static_target),
                    choices =
                        listOf(
                            Choice(WallpaperTarget.HOME, stringResource(R.string.target_home)),
                            Choice(WallpaperTarget.LOCK, stringResource(R.string.target_lock)),
                            Choice(WallpaperTarget.HOME_AND_LOCK, stringResource(R.string.target_both)),
                        ),
                    selected = settings.staticTarget,
                    onSelect = { target -> editor.commit { it.copy(staticTarget = target) } },
                )
                Button(
                    onClick = {
                        message = null
                        scope.launch {
                            message =
                                when (applyStatic()) {
                                    is Outcome.Success -> R.string.static_applied
                                    is Outcome.Failure -> R.string.static_failed
                                }
                        }
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
                ) { Text(stringResource(R.string.static_apply)) }
            }
        }
        message?.let { SettingHint(stringResource(it)) }
    }
}

/** Opens the system preview of the live wallpaper, or the live wallpaper list. False if neither exists. */
private fun openLivePicker(context: Context): Boolean =
    listOf(LiveWallpaper.previewIntent(context), LiveWallpaper.chooserIntent()).any { intent ->
        runCatching { context.startActivity(intent) }.isSuccess
    }

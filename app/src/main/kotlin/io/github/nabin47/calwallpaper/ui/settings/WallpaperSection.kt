package io.github.nabin47.calwallpaper.ui.settings

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.WallpaperTarget
import io.github.nabin47.calwallpaper.ui.components.Choice
import io.github.nabin47.calwallpaper.ui.components.ChoiceSetting
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.components.TonalIcon
import io.github.nabin47.calwallpaper.wallpaper.LiveWallpaper
import kotlinx.coroutines.launch

/**
 * Live versus battery-saver (static) mode as two cards, and how to apply the chosen one. Live mode hands over to
 * the system's live wallpaper preview, which asks home, lock or both. Static mode applies the image itself, to the
 * screens chosen here. Results show in [snackbar].
 */
@Composable
fun WallpaperSection(
    settings: Settings,
    editor: SettingsEditor,
    liveActive: Boolean,
    applyStatic: suspend () -> Outcome<Unit>,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeCard(
            icon = R.drawable.ic_wallpaper,
            title = stringResource(R.string.mode_live),
            body = stringResource(R.string.mode_live_body),
            selected = settings.batteryMode == BatteryMode.LIVE,
            onSelect = { editor.commit { it.copy(batteryMode = BatteryMode.LIVE) } },
        )
        ModeCard(
            icon = R.drawable.ic_battery,
            title = stringResource(R.string.mode_static),
            body = stringResource(R.string.mode_static_body),
            selected = settings.batteryMode == BatteryMode.STATIC,
            onSelect = { editor.commit { it.copy(batteryMode = BatteryMode.STATIC) } },
        )
        when (settings.batteryMode) {
            BatteryMode.LIVE -> LiveActions(liveActive, snackbar)
            BatteryMode.STATIC -> StaticActions(settings, editor, applyStatic, snackbar)
        }
    }
}

@Composable
private fun LiveActions(
    liveActive: Boolean,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.live_unavailable)
    SettingHint(stringResource(if (liveActive) R.string.live_active else R.string.live_inactive))
    Button(
        onClick = { if (!openLivePicker(context)) scope.launch { snackbar.showSnackbar(unavailable) } },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(if (liveActive) R.string.live_reapply else R.string.live_set)) }
    SettingHint(stringResource(R.string.live_set_hint))
}

@Composable
private fun StaticActions(
    settings: Settings,
    editor: SettingsEditor,
    applyStatic: suspend () -> Outcome<Unit>,
    snackbar: SnackbarHostState,
) {
    val scope = rememberCoroutineScope()
    val applied = stringResource(R.string.static_applied)
    val failed = stringResource(R.string.static_failed)
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
            scope.launch {
                val message = if (applyStatic() is Outcome.Success) applied else failed
                snackbar.showSnackbar(message)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.static_apply)) }
    SettingHint(stringResource(R.string.static_auto_hint))
}

/** A selectable card with an icon, a title, a description and a radio button. */
@Composable
private fun ModeCard(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TonalIcon(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

/** Opens the system preview of the live wallpaper, or the live wallpaper list. False if neither exists. */
internal fun openLivePicker(context: Context): Boolean =
    listOf(LiveWallpaper.previewIntent(context), LiveWallpaper.chooserIntent()).any { intent ->
        runCatching { context.startActivity(intent) }.isSuccess
    }

package io.github.nabin47.calwallpaper.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.components.TonalIcon
import io.github.nabin47.calwallpaper.ui.settings.openLivePicker
import kotlinx.coroutines.launch

/**
 * Says plainly whether CalWallpaper is the wallpaper right now, and offers the one next step: set it, re-apply
 * the battery-saver image, or nothing when all is well. [liveActive] is re-read on every resume, so coming back
 * from Android's wallpaper preview updates the card.
 */
@Composable
fun StatusCard(
    state: ScreenState,
    liveActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.live_unavailable)
    val applied = stringResource(R.string.static_applied)
    val failed = stringResource(R.string.static_failed)
    val colors = MaterialTheme.colorScheme

    when {
        state.settings.batteryMode == BatteryMode.STATIC -> {
            Status(
                icon = R.drawable.ic_battery,
                title = stringResource(R.string.status_static_title),
                body = stringResource(R.string.status_static_body),
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                modifier = modifier,
            ) {
                Button(onClick = {
                    scope.launch {
                        val outcome = state.services.staticUpdater.refresh(force = true)
                        state.snackbar.showSnackbar(if (outcome is Outcome.Success) applied else failed)
                    }
                }) { Text(stringResource(R.string.static_apply)) }
            }
        }

        liveActive -> {
            Status(
                icon = R.drawable.ic_check_circle,
                title = stringResource(R.string.status_live_on_title),
                body = stringResource(R.string.status_live_on_body),
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                modifier = modifier,
            ) {}
        }

        else -> {
            Status(
                icon = R.drawable.ic_wallpaper,
                title = stringResource(R.string.status_live_off_title),
                body = stringResource(R.string.status_live_off_body),
                container = colors.primaryContainer,
                content = colors.onPrimaryContainer,
                modifier = modifier,
            ) {
                Button(onClick = {
                    if (!openLivePicker(context)) scope.launch { state.snackbar.showSnackbar(unavailable) }
                }) { Text(stringResource(R.string.live_set)) }
                TextButton(onClick = {
                    state.editor.commit { it.copy(batteryMode = BatteryMode.STATIC) }
                }) { Text(stringResource(R.string.status_use_static)) }
            }
        }
    }
}

@Composable
private fun Status(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    container: Color,
    content: Color,
    modifier: Modifier,
    actions: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TonalIcon(icon)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                actions()
            }
        }
    }
}

package io.github.nabin47.calwallpaper.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.preview.LivePreview
import io.github.nabin47.calwallpaper.ui.preview.rememberPreviewRequest

/**
 * The wallpaper as it will look, in a phone-shaped frame, with a Home screen / Lock screen switch below it.
 * Pass `null` for [onLockedChange] on pages that only concern one screen: the switch is replaced by a label
 * saying which screen is shown. The frame takes the height it is given and keeps the phone's aspect ratio.
 */
@Composable
fun PreviewPane(
    state: ScreenState,
    locked: Boolean,
    onLockedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val request = rememberPreviewRequest(state.services, state.settings, state.preview, locked)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(PHONE_CORNER),
            border = BorderStroke(PHONE_BEZEL, MaterialTheme.colorScheme.outlineVariant),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            LivePreview(
                request = request,
                description =
                    stringResource(
                        if (locked) R.string.preview_description_lock else R.string.preview_description_home,
                    ),
                modifier = Modifier.padding(PHONE_BEZEL),
            )
        }
        if (onLockedChange != null) {
            ScreenSwitch(locked = locked, onLockedChange = onLockedChange)
        } else {
            Text(
                text = stringResource(if (locked) R.string.preview_showing_lock else R.string.preview_showing_home),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.preview.isSample) {
            Text(
                text = stringResource(R.string.preview_sample_short),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Home screen / Lock screen segmented buttons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenSwitch(
    locked: Boolean,
    onLockedChange: (Boolean) -> Unit,
) {
    val options =
        listOf(
            Triple(false, R.string.target_home, R.drawable.ic_wallpaper),
            Triple(true, R.string.target_lock, R.drawable.ic_lock),
        )
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (value, label, icon) ->
            SegmentedButton(
                selected = locked == value,
                onClick = { onLockedChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
                },
            ) { Text(stringResource(label)) }
        }
    }
}

private val PHONE_CORNER = 28.dp
private val PHONE_BEZEL = 4.dp

package io.github.nabin47.calwallpaper.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.LayoutSettings
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.VerticalPosition
import io.github.nabin47.calwallpaper.ui.components.Choice
import io.github.nabin47.calwallpaper.ui.components.ChoiceSetting
import io.github.nabin47.calwallpaper.ui.components.SectionHeader
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.components.SliderSetting
import kotlin.math.roundToInt

/** Layout preset, vertical position, safe-zone insets and side margin (CLAUDE.md §6). */
@Composable
fun LayoutSection(
    settings: Settings,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    val layout = settings.layout

    fun edit(transform: (LayoutSettings) -> LayoutSettings): (Settings) -> Settings =
        { it.copy(layout = transform(it.layout)) }

    Column(modifier) {
        SectionHeader(stringResource(R.string.section_layout))
        ChoiceSetting(
            label = stringResource(R.string.preset_label),
            choices =
                listOf(
                    Choice(LayoutPreset.AGENDA, stringResource(R.string.preset_agenda)),
                    Choice(LayoutPreset.WEEK_STRIP, stringResource(R.string.preset_week)),
                    Choice(LayoutPreset.MONTH_GRID, stringResource(R.string.preset_month)),
                    Choice(LayoutPreset.NEXT_THREE, stringResource(R.string.preset_next_three)),
                ),
            selected = layout.preset,
            onSelect = { preset -> editor.commit(edit { it.copy(preset = preset) }) },
        )
        ChoiceSetting(
            label = stringResource(R.string.position_label),
            choices =
                listOf(
                    Choice(VerticalPosition.TOP, stringResource(R.string.position_top)),
                    Choice(VerticalPosition.CENTER, stringResource(R.string.position_center)),
                    Choice(VerticalPosition.BOTTOM, stringResource(R.string.position_bottom)),
                ),
            selected = layout.position,
            onSelect = { position -> editor.commit(edit { it.copy(position = position) }) },
        )
        SliderSetting(
            label = stringResource(R.string.safe_top),
            valueLabel = stringResource(R.string.percent_value, (layout.safeZoneTop * 100).roundToInt()),
            value = layout.safeZoneTop,
            range = LayoutSettings.SAFE_ZONE,
            onPreview = { v -> editor.preview(edit { it.copy(safeZoneTop = v) }) },
            onCommit = { editor.commit() },
        )
        SliderSetting(
            label = stringResource(R.string.safe_bottom),
            valueLabel = stringResource(R.string.percent_value, (layout.safeZoneBottom * 100).roundToInt()),
            value = layout.safeZoneBottom,
            range = LayoutSettings.SAFE_ZONE,
            onPreview = { v -> editor.preview(edit { it.copy(safeZoneBottom = v) }) },
            onCommit = { editor.commit() },
        )
        SettingHint(stringResource(R.string.safe_hint))
        LockScreenPlacement(layout) { transform, commit ->
            if (commit) editor.commit(edit(transform)) else editor.preview(edit(transform))
        }
        SliderSetting(
            label = stringResource(R.string.side_margin),
            valueLabel = stringResource(R.string.dp_value, layout.horizontalInsetDp),
            value = layout.horizontalInsetDp.toFloat(),
            range = LayoutSettings.HORIZONTAL_INSET_DP.toFloatRange(),
            onPreview = { v -> editor.preview(edit { it.copy(horizontalInsetDp = v.roundToInt()) }) },
            onCommit = { editor.commit() },
        )
    }
}

/**
 * Lock-screen placement: lock-screen clocks are usually much bigger than home-screen ones, so the lock screen has
 * its own position and safe zones. [onEdit] previews (`commit = false`, while dragging) or saves a change.
 */
@Composable
private fun LockScreenPlacement(
    layout: LayoutSettings,
    onEdit: (transform: (LayoutSettings) -> LayoutSettings, commit: Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.lock_screen_header),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp),
    )
    SettingHint(stringResource(R.string.lock_screen_hint))
    ChoiceSetting(
        label = stringResource(R.string.position_label),
        choices =
            listOf(
                Choice(VerticalPosition.TOP, stringResource(R.string.position_top)),
                Choice(VerticalPosition.CENTER, stringResource(R.string.position_center)),
                Choice(VerticalPosition.BOTTOM, stringResource(R.string.position_bottom)),
            ),
        selected = layout.lockPosition,
        onSelect = { position -> onEdit({ it.copy(lockPosition = position) }, true) },
    )
    SliderSetting(
        label = stringResource(R.string.safe_top),
        valueLabel = stringResource(R.string.percent_value, (layout.lockSafeZoneTop * 100).roundToInt()),
        value = layout.lockSafeZoneTop,
        range = LayoutSettings.LOCK_SAFE_ZONE_TOP,
        onPreview = { v -> onEdit({ it.copy(lockSafeZoneTop = v) }, false) },
        onCommit = { onEdit({ it }, true) },
    )
    SliderSetting(
        label = stringResource(R.string.safe_bottom),
        valueLabel = stringResource(R.string.percent_value, (layout.lockSafeZoneBottom * 100).roundToInt()),
        value = layout.lockSafeZoneBottom,
        range = LayoutSettings.SAFE_ZONE,
        onPreview = { v -> onEdit({ it.copy(lockSafeZoneBottom = v) }, false) },
        onCommit = { onEdit({ it }, true) },
    )
}

/** An integer setting range as a slider range. */
internal fun IntRange.toFloatRange(): ClosedFloatingPointRange<Float> = first.toFloat()..last.toFloat()

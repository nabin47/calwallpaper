package io.github.nabin47.calwallpaper.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.data.calendar.DeviceCalendar
import io.github.nabin47.calwallpaper.domain.settings.ContentSettings
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.ui.CalendarPermissionState
import io.github.nabin47.calwallpaper.ui.components.SectionHeader
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.components.SliderSetting
import io.github.nabin47.calwallpaper.ui.components.SwitchSetting
import kotlin.math.roundToInt

/** Sources, calendar selection, date range and completed tasks (CLAUDE.md §6). */
@Composable
fun ContentSection(
    settings: Settings,
    editor: SettingsEditor,
    permission: CalendarPermissionState,
    calendars: List<DeviceCalendar>,
    modifier: Modifier = Modifier,
) {
    val content = settings.content

    fun edit(transform: (ContentSettings) -> ContentSettings): (Settings) -> Settings =
        { it.copy(content = transform(it.content)) }

    fun toggleSource(
        source: SourceId,
        on: Boolean,
    ) = editor.commit(
        edit { it.copy(enabledSources = if (on) it.enabledSources + source else it.enabledSources - source) },
    )

    Column(modifier) {
        SectionHeader(stringResource(R.string.section_content))
        SwitchSetting(
            label = stringResource(R.string.source_calendar),
            checked = SourceId.CALENDAR_PROVIDER in content.enabledSources,
            onCheckedChange = { on -> toggleSource(SourceId.CALENDAR_PROVIDER, on) },
        )
        if (SourceId.CALENDAR_PROVIDER in content.enabledSources) {
            if (permission.granted) {
                CalendarChoices(calendars, content.selectedCalendarIds) { ids ->
                    editor.commit(edit { it.copy(selectedCalendarIds = ids) })
                }
            } else {
                PermissionPrompt(permission)
            }
        }
        SwitchSetting(
            label = stringResource(R.string.source_local),
            checked = SourceId.LOCAL_TASKS in content.enabledSources,
            onCheckedChange = { on -> toggleSource(SourceId.LOCAL_TASKS, on) },
        )
        SliderSetting(
            label = stringResource(R.string.days_ahead),
            valueLabel = stringResource(R.string.days_value, content.daysAhead),
            value = content.daysAhead.toFloat(),
            range = ContentSettings.DAYS_AHEAD.toFloatRange(),
            steps = ContentSettings.DAYS_AHEAD.last - ContentSettings.DAYS_AHEAD.first - 1,
            onPreview = { v -> editor.preview(edit { it.copy(daysAhead = v.roundToInt()) }) },
            onCommit = { editor.commit() },
        )
        SwitchSetting(
            label = stringResource(R.string.show_completed),
            checked = content.showCompleted,
            onCheckedChange = { on -> editor.commit(edit { it.copy(showCompleted = on) }) },
        )
    }
}

/** Asks for calendar access in context, with a way out when Android no longer shows the dialog. */
@Composable
fun PermissionPrompt(
    permission: CalendarPermissionState,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 4.dp)) {
        if (permission.blocked) {
            SettingHint(stringResource(R.string.calendar_blocked))
            OutlinedButton(onClick = permission::openAppSettings) { Text(stringResource(R.string.open_app_settings)) }
        } else {
            SettingHint(stringResource(R.string.calendar_needed))
            OutlinedButton(onClick = permission::launchRequest) { Text(stringResource(R.string.calendar_allow)) }
        }
    }
}

/** "All calendars" plus one checkbox per calendar. `null` selection means all, including calendars added later. */
@Composable
private fun CalendarChoices(
    calendars: List<DeviceCalendar>,
    selected: Set<String>?,
    onChange: (Set<String>?) -> Unit,
) {
    val all = selected == null
    CheckRow(label = stringResource(R.string.calendars_all), checked = all, color = null) { on ->
        onChange(if (on) null else calendars.map { it.id }.toSet())
    }
    if (!all) {
        calendars.forEach { calendar ->
            val checked = calendar.id in selected.orEmpty()
            CheckRow(
                label = calendar.displayName,
                detail = calendar.accountName,
                checked = checked,
                color = calendar.colorArgb,
            ) { on ->
                val current = selected.orEmpty()
                onChange(if (on) current + calendar.id else current - calendar.id)
            }
        }
    }
}

/** A checkbox row with an optional color dot and detail line, for calendar and project choices. */
@Composable
internal fun CheckRow(
    label: String,
    checked: Boolean,
    color: Int?,
    detail: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        if (color != null) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(color)))
            Spacer(Modifier.width(8.dp))
        }
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (!detail.isNullOrEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

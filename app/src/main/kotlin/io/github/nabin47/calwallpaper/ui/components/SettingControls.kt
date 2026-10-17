package io.github.nabin47.calwallpaper.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** A section title in the settings list; a heading for screen readers, so TalkBack can jump between sections. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() },
    )
}

/** Small explanatory text under a setting. */
@Composable
fun SettingHint(
    text: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

/** One choice of a [ChoiceSetting]. */
data class Choice<T>(
    val value: T,
    val label: String,
)

/** A labeled single choice shown as chips, scrollable sideways when they do not fit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceSetting(
    label: String,
    choices: List<Choice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { choice ->
                FilterChip(
                    selected = choice.value == selected,
                    onClick = { onSelect(choice.value) },
                    label = { Text(choice.label) },
                )
            }
        }
    }
}

/**
 * A labeled slider. [onPreview] runs while dragging (the preview follows the finger); [onCommit] runs once when
 * the finger lifts, so storage is written once per gesture. Screen readers announce [label] and [valueLabel]
 * (e.g. "Text size, 1.2×") instead of a bare percentage.
 */
@Composable
fun SliderSetting(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onPreview: (Float) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onPreview,
            onValueChangeFinished = onCommit,
            valueRange = range,
            steps = steps,
            modifier =
                Modifier.semantics {
                    contentDescription = label
                    stateDescription = valueLabel
                },
        )
    }
}

/** A labeled on/off switch; the whole row is the touch target. */
@Composable
fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // The row handles the toggle, so the label and the switch are read and tapped as one control.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

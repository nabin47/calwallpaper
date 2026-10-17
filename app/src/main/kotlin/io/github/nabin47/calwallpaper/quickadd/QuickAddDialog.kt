package io.github.nabin47.calwallpaper.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.data.local.LocalTasks
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** The quick-add form: a title (focused, keyboard up) and a day. [onAdd] gets the trimmed title and due date. */
@Composable
fun QuickAddDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, due: LocalDate?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf(QuickDue.TODAY) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val canAdd = title.isNotBlank() && title.length <= LocalTasks.MAX_TITLE_LENGTH

    fun add() {
        if (canAdd) {
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            onAdd(title.trim(), due.dateFrom(today))
        }
    }

    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.quick_add_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.quick_add_hint)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DueChip(QuickDue.TODAY, R.string.quick_add_today, due) { due = it }
                DueChip(QuickDue.TOMORROW, R.string.quick_add_tomorrow, due) { due = it }
                DueChip(QuickDue.NONE, R.string.quick_add_no_date, due) { due = it }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.quick_add_cancel)) }
                TextButton(onClick = ::add, enabled = canAdd) { Text(stringResource(R.string.quick_add_add)) }
            }
        }
    }
}

@Composable
private fun DueChip(
    value: QuickDue,
    label: Int,
    selected: QuickDue,
    onSelect: (QuickDue) -> Unit,
) {
    FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(stringResource(label)) })
}

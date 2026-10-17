package io.github.nabin47.calwallpaper.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.data.todoist.CachedProject
import io.github.nabin47.calwallpaper.data.todoist.SyncError
import io.github.nabin47.calwallpaper.data.todoist.SyncStatus
import io.github.nabin47.calwallpaper.data.todoist.needsNewToken
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.todoist.TodoistConnection
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.components.SwitchSetting
import kotlinx.coroutines.launch

/**
 * Todoist: turn the source on, connect with a personal API token, see how syncing goes, and pick projects
 * (CLAUDE.md §6, §7). The token field never shows a stored token; once saved, only "Connected" is shown.
 */
@Composable
fun TodoistSection(
    settings: Settings,
    editor: SettingsEditor,
    connection: TodoistConnection,
    modifier: Modifier = Modifier,
) {
    val enabled = SourceId.TODOIST in settings.content.enabledSources
    val connected by connection.connected.collectAsState()
    val cache by connection.cache.collectAsState(initial = null)
    LaunchedEffect(enabled) { connection.refresh(enabled) }

    Column(modifier) {
        SwitchSetting(
            label = stringResource(R.string.source_todoist),
            checked = enabled,
            onCheckedChange = { on ->
                editor.commit { s ->
                    val sources = s.content.enabledSources
                    s.copy(
                        content =
                            s.content.copy(
                                enabledSources =
                                    if (on) {
                                        sources + SourceId.TODOIST
                                    } else {
                                        sources -
                                            SourceId.TODOIST
                                    },
                            ),
                    )
                }
            },
        )
        val status = cache?.status ?: SyncStatus()
        when {
            !enabled || connected == null -> Unit
            connected == false -> TokenEntry(connection, problem = null)
            status.needsNewToken -> TokenEntry(connection, problem = status.error)
            else -> Connected(settings, editor, connection, status, cache?.projects.orEmpty())
        }
    }
}

/** The token field, with what went wrong last time ([problem]) when the stored token stopped working. */
@Composable
private fun TokenEntry(
    connection: TodoistConnection,
    problem: SyncError?,
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf(false) }
    val rejectedText = stringResource(R.string.todoist_token_invalid)
    val supporting: (@Composable () -> Unit)? = if (rejected) ({ Text(rejectedText) }) else null
    Column(Modifier.padding(vertical = 4.dp)) {
        if (problem != null) SettingHint(stringResource(syncErrorText(problem)))
        SettingHint(stringResource(R.string.todoist_token_hint))
        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                rejected = false
            },
            label = { Text(stringResource(R.string.todoist_token_label)) },
            singleLine = true,
            isError = rejected,
            supportingText = supporting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    rejected = !connection.connect(input)
                    if (!rejected) input = ""
                }
            },
            enabled = input.isNotBlank(),
            modifier = Modifier.padding(top = 8.dp),
        ) { Text(stringResource(R.string.todoist_connect)) }
    }
}

/** Sync status, actions and the project picker. */
@Composable
private fun Connected(
    settings: Settings,
    editor: SettingsEditor,
    connection: TodoistConnection,
    status: SyncStatus,
    projects: List<CachedProject>,
) {
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            text = statusLine(status),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (status.error ==
                    null
                ) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            OutlinedButton(onClick = connection::syncNow) { Text(stringResource(R.string.todoist_sync_now)) }
            OutlinedButton(onClick = { scope.launch { connection.disconnect() } }) {
                Text(stringResource(R.string.todoist_disconnect))
            }
        }
        if (projects.isNotEmpty()) {
            ProjectChoices(projects, settings.content.selectedTaskListIds) { ids ->
                editor.commit { it.copy(content = it.content.copy(selectedTaskListIds = ids)) }
            }
        }
    }
}

/** "All projects" plus one checkbox per project; `null` selection means all, including new projects. */
@Composable
private fun ProjectChoices(
    projects: List<CachedProject>,
    selected: Set<String>?,
    onChange: (Set<String>?) -> Unit,
) {
    val all = selected == null
    CheckRow(label = stringResource(R.string.todoist_projects_all), checked = all, color = null) { on ->
        onChange(if (on) null else projects.map { it.id }.toSet())
    }
    if (!all) {
        projects.forEach { project ->
            CheckRow(
                label = project.name,
                checked = project.id in selected.orEmpty(),
                color = project.colorArgb,
            ) { on ->
                val current = selected.orEmpty()
                onChange(if (on) current + project.id else current - project.id)
            }
        }
    }
}

/** "Synced 5 minutes ago", "Syncing…", or the last error with the time of the last good sync. */
@Composable
private fun statusLine(status: SyncStatus): String {
    val synced =
        status.lastSuccessMs?.let {
            DateUtils
                .getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                .toString()
        }
    val error = status.error
    return when {
        error != null && synced != null -> {
            stringResource(R.string.todoist_status_error_since, stringResource(syncErrorText(error)), synced)
        }

        error != null -> {
            stringResource(syncErrorText(error))
        }

        synced != null -> {
            stringResource(R.string.todoist_status_synced, synced)
        }

        else -> {
            stringResource(R.string.todoist_status_syncing)
        }
    }
}

/** User-facing text for a sync error. None of them contains user data. */
private fun syncErrorText(error: SyncError): Int =
    when (error) {
        SyncError.AUTH -> R.string.todoist_error_auth
        SyncError.TOKEN_UNREADABLE -> R.string.todoist_error_token_unreadable
        SyncError.NETWORK -> R.string.todoist_error_network
        SyncError.RATE_LIMITED -> R.string.todoist_error_rate_limited
        SyncError.SERVER -> R.string.todoist_error_server
        SyncError.BAD_RESPONSE -> R.string.todoist_error_bad_response
    }

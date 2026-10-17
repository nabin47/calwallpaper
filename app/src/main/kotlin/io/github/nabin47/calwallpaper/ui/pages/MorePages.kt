package io.github.nabin47.calwallpaper.ui.pages

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.BuildConfig
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.backup.SettingsFiles
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SettingsCodec
import io.github.nabin47.calwallpaper.quickadd.QuickAddTileService
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.components.PageScaffold
import io.github.nabin47.calwallpaper.ui.components.SectionHeader
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.components.TonalIcon
import io.github.nabin47.calwallpaper.ui.settings.DiagnosticsSection
import io.github.nabin47.calwallpaper.wallpaper.diagnostics.PerfReport
import io.github.nabin47.calwallpaper.widgets.AgendaWidgetReceiver
import io.github.nabin47.calwallpaper.widgets.WeekWidgetReceiver
import kotlinx.coroutines.launch

/**
 * The two widgets, each with an "Add to home screen" button where the launcher supports pinning, plus the Quick
 * Settings tile (added from here on Android 13+) and the launcher shortcut.
 */
@Composable
fun WidgetsPage(state: ScreenState) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    val canPin = remember { manager.isRequestPinAppWidgetSupported }
    PageScaffold(state = state, title = stringResource(R.string.section_widgets)) {
        item { SettingHint(stringResource(R.string.widgets_intro)) }
        item {
            InfoCard(
                R.drawable.ic_dashboard,
                stringResource(R.string.widget_agenda_name),
                stringResource(R.string.widget_agenda_body),
            ) {
                PinButton(
                    canPin,
                ) { manager.requestPinAppWidget(ComponentName(context, AgendaWidgetReceiver::class.java), null, null) }
            }
        }
        item {
            InfoCard(
                R.drawable.ic_event,
                stringResource(R.string.widget_week_name),
                stringResource(R.string.widget_week_body),
            ) {
                PinButton(
                    canPin,
                ) { manager.requestPinAppWidget(ComponentName(context, WeekWidgetReceiver::class.java), null, null) }
            }
        }
        item {
            InfoCard(R.drawable.ic_add, stringResource(R.string.quick_add_tile), stringResource(R.string.tile_body)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    FilledTonalButton(onClick = { requestTile(context) }) { Text(stringResource(R.string.tile_add)) }
                } else {
                    SettingHint(stringResource(R.string.tile_manual))
                }
            }
        }
        item {
            InfoCard(
                R.drawable.ic_checklist,
                stringResource(R.string.shortcut_title),
                stringResource(R.string.shortcut_body),
            ) {}
        }
    }
}

@Composable
private fun PinButton(
    canPin: Boolean,
    pin: () -> Unit,
) {
    if (canPin) {
        FilledTonalButton(onClick = pin) { Text(stringResource(R.string.widget_add)) }
    } else {
        SettingHint(stringResource(R.string.widget_manual))
    }
}

/** Asks System UI to add the Quick add tile (Android 13+); the system shows its own confirmation. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun requestTile(context: Context) {
    val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
    statusBar.requestAddTileService(
        ComponentName(context, QuickAddTileService::class.java),
        context.getString(R.string.quick_add_tile),
        Icon.createWithResource(context, R.drawable.ic_quick_add),
        context.mainExecutor,
    ) { }
}

/** Export and import the settings as a file, and reset them, with Undo. */
@Composable
fun BackupPage(state: ScreenState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember { SettingsFiles(context.contentResolver) }
    val codec = remember { SettingsCodec() }
    val exported = stringResource(R.string.backup_exported)
    val exportFailed = stringResource(R.string.backup_export_failed)
    val imported = stringResource(R.string.backup_imported)
    val importFailed = stringResource(R.string.backup_import_failed)
    val resetDone = stringResource(R.string.backup_reset_done)
    val undo = stringResource(R.string.undo)
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(SettingsFiles.MIME_TYPE)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val json = codec.encode(state.settings)
            scope.launch { state.snackbar.showSnackbar(if (files.write(uri, json)) exported else exportFailed) }
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val decoded = files.read(uri)?.let(codec::decode)
                if (decoded is Outcome.Success) {
                    state.editor.commit { decoded.value }
                    state.snackbar.showSnackbar(imported)
                } else {
                    state.snackbar.showSnackbar(importFailed)
                }
            }
        }

    PageScaffold(state = state, title = stringResource(R.string.section_backup)) {
        item {
            InfoCard(
                R.drawable.ic_download,
                stringResource(R.string.backup_export),
                stringResource(R.string.backup_export_body),
            ) {
                FilledTonalButton(onClick = { exportLauncher.launch(SettingsFiles.DEFAULT_NAME) }) {
                    Text(stringResource(R.string.backup_export_action))
                }
            }
        }
        item {
            InfoCard(
                R.drawable.ic_upload,
                stringResource(R.string.backup_import),
                stringResource(R.string.backup_import_body),
            ) {
                FilledTonalButton(onClick = { importLauncher.launch(arrayOf(SettingsFiles.MIME_TYPE, "text/plain")) }) {
                    Text(stringResource(R.string.backup_import_action))
                }
            }
        }
        item {
            InfoCard(
                R.drawable.ic_restart,
                stringResource(R.string.backup_reset),
                stringResource(R.string.backup_reset_body),
            ) {
                OutlinedButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.backup_reset_action)) }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.backup_reset_confirm_title)) },
            text = { Text(stringResource(R.string.backup_reset_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    val before = state.settings
                    state.editor.commit { Settings() }
                    scope.launch {
                        val result =
                            state.snackbar.showSnackbar(
                                resetDone,
                                actionLabel = undo,
                                withDismissAction = true,
                            )
                        if (result == SnackbarResult.ActionPerformed) state.editor.commit { before }
                    }
                }) { Text(stringResource(R.string.backup_reset_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Answers to common questions, the performance numbers, the version and links. */
@Composable
fun AboutPage(state: ScreenState) {
    val uriHandler = LocalUriHandler.current
    val report by produceState(PerfReport(), state.resumeCount) {
        value =
            state.services.graph.diagnostics
                .read()
    }
    val faq =
        listOf(
            R.string.faq_taps_q to R.string.faq_taps_a,
            R.string.faq_lock_q to R.string.faq_lock_a,
            R.string.faq_events_q to R.string.faq_events_a,
            R.string.faq_battery_q to R.string.faq_battery_a,
            R.string.faq_privacy_q to R.string.faq_privacy_a,
        )
    PageScaffold(state = state, title = stringResource(R.string.section_about)) {
        item { SectionHeader(stringResource(R.string.faq_title)) }
        faq.forEach { (question, answer) -> item { FaqItem(stringResource(question), stringResource(answer)) } }
        item { DiagnosticsSection(report) }
        item { SectionHeader(stringResource(R.string.about_title)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge,
                )
                SettingHint(stringResource(R.string.about_body))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { uriHandler.openUri(PRIVACY_URL) },
                    ) { Text(stringResource(R.string.about_privacy)) }
                    OutlinedButton(
                        onClick = { uriHandler.openUri(SOURCE_URL) },
                    ) { Text(stringResource(R.string.about_source)) }
                }
            }
        }
    }
}

/** A question that expands to its answer when tapped. */
@Composable
private fun FaqItem(
    question: String,
    answer: String,
) {
    var open by rememberSaveable(question) { mutableStateOf(false) }
    Card(
        onClick = { open = !open },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(question, style = MaterialTheme.typography.titleSmall)
            if (open) {
                Text(
                    answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A card with an icon, a title, a description and an action row. */
@Composable
private fun InfoCard(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    action: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TonalIcon(icon)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) { action() }
            }
        }
    }
}

private const val PRIVACY_URL = "https://nabin47.github.io/calwallpaper/privacy/"
private const val SOURCE_URL = "https://github.com/nabin47/calwallpaper"

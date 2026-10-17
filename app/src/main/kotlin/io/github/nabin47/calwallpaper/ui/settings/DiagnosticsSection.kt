package io.github.nabin47.calwallpaper.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.ui.components.SectionHeader
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.wallpaper.diagnostics.PerfReport
import io.github.nabin47.calwallpaper.wallpaper.diagnostics.PerfSnapshot

/** Numbers the wallpaper measured on this phone (CLAUDE.md §5), for `docs/performance.md`. */
@Composable
fun DiagnosticsSection(
    report: PerfReport,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_diagnostics))
        SettingHint(stringResource(R.string.diagnostics_hint))
        Snapshot(stringResource(R.string.diagnostics_live), report.liveWallpaper)
        Snapshot(stringResource(R.string.diagnostics_static), report.staticImage)
    }
}

@Composable
private fun Snapshot(
    title: String,
    snapshot: PerfSnapshot?,
) {
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
    if (snapshot == null) {
        SettingHint(stringResource(R.string.diagnostics_none))
        return
    }
    val pss =
        snapshot.pssKb?.let { stringResource(R.string.diagnostics_pss, it / 1024f) }
            ?: stringResource(R.string.diagnostics_pss_unknown)
    SettingHint(stringResource(R.string.diagnostics_timing, snapshot.layoutMicros / 1000f, snapshot.drawMicros / 1000f))
    SettingHint(stringResource(R.string.diagnostics_memory, snapshot.bitmapBytes / (1024f * 1024f), pss))
    if (snapshot.frames > 1) SettingHint(stringResource(R.string.diagnostics_frames, snapshot.frames))
}

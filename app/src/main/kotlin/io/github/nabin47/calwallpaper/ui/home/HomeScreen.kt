package io.github.nabin47.calwallpaper.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.quickadd.QuickAddActivity
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.components.PreviewPane
import io.github.nabin47.calwallpaper.ui.navigation.Route
import io.github.nabin47.calwallpaper.wallpaper.LiveWallpaper

/**
 * The main screen. Top to bottom: what the wallpaper looks like (home or lock screen), whether it is set and the
 * one next step, one-tap layout and color choices, then every settings page with a summary of its current values.
 * The button at the bottom adds a task to the local list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: ScreenState) {
    val context = LocalContext.current
    var locked by rememberSaveable { mutableStateOf(false) }
    val liveActive = remember(state.resumeCount) { LiveWallpaper.isActive(context) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { state.navigate(Route.About) }) {
                        Icon(
                            painterResource(R.drawable.ic_help),
                            contentDescription = stringResource(R.string.section_about),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { context.startActivity(Intent(context, QuickAddActivity::class.java)) },
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.home_add_task)) },
            )
        },
        snackbarHost = { SnackbarHost(state.snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PreviewPane(
                    state = state,
                    locked = locked,
                    onLockedChange = { locked = it },
                    modifier = Modifier.fillParentMaxHeight(PREVIEW_HEIGHT_FRACTION).padding(top = 8.dp),
                )
            }
            item { StatusCard(state, liveActive, Modifier.padding(horizontal = 16.dp)) }
            if (needsCalendarAccess(state)) item { CalendarAccessCard(state) }
            item { Heading(stringResource(R.string.home_layout_title)) }
            item { LayoutPicker(state) }
            item { Heading(stringResource(R.string.home_colors_title)) }
            item { PalettePicker(state) }
            item { SettingsList(state, Modifier.padding(horizontal = 16.dp)) }
        }
    }
}

/** Calendar events are on, but the app may not read them yet. */
private fun needsCalendarAccess(state: ScreenState): Boolean =
    SourceId.CALENDAR_PROVIDER in state.settings.content.enabledSources && !state.permission.granted

/** Explains why the preview shows sample events, with the way to show real ones. */
@Composable
private fun CalendarAccessCard(state: ScreenState) {
    val permission = state.permission
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.home_calendar_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(if (permission.blocked) R.string.calendar_blocked else R.string.home_calendar_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { if (permission.blocked) permission.openAppSettings() else permission.launchRequest() }) {
                Text(stringResource(if (permission.blocked) R.string.open_app_settings else R.string.calendar_allow))
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp).semantics { heading() },
    )
}

/** Share of the screen the big preview takes. */
private const val PREVIEW_HEIGHT_FRACTION = 0.58f

/** Room under the list so the floating button never covers the last row. */
private val FAB_CLEARANCE = 96.dp

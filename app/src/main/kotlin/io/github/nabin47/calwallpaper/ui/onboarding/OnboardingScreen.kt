package io.github.nabin47.calwallpaper.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.components.PreviewPane
import io.github.nabin47.calwallpaper.ui.components.TonalIcon
import io.github.nabin47.calwallpaper.ui.home.LayoutPicker
import io.github.nabin47.calwallpaper.ui.home.PalettePicker
import io.github.nabin47.calwallpaper.ui.settings.openLivePicker
import io.github.nabin47.calwallpaper.wallpaper.LiveWallpaper
import kotlinx.coroutines.launch

/**
 * First-run flow in four short steps, with a progress bar, Back and Skip:
 * 1. what the app does, on a live preview;
 * 2. calendar access with its rationale (asked here, in context, never at startup; CLAUDE.md §7);
 * 3. picking a layout and colors;
 * 4. setting the wallpaper, with what Android's own preview will ask.
 *
 * Returning from Android's preview with CalWallpaper set finishes the flow by itself. [onFinish] opens the home
 * screen.
 */
@Composable
fun OnboardingScreen(
    state: ScreenState,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(STEP_WELCOME) }
    BackHandler(enabled = step > STEP_WELCOME) { step-- }
    val next: () -> Unit = { if (step == LAST_STEP) onFinish() else step++ }
    LaunchedEffect(state.permission.granted) {
        if (state.permission.granted && step == STEP_CALENDAR) step++
    }

    Scaffold(snackbarHost = { SnackbarHost(state.snackbar) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
            Progress(step, onSkip = onFinish)
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / SLIDE_FRACTION else -it / SLIDE_FRACTION } + fadeIn())
                        .togetherWith(
                            slideOutHorizontally { if (forward) -it / SLIDE_FRACTION else it / SLIDE_FRACTION } +
                                fadeOut(),
                        )
                },
                modifier = Modifier.weight(1f),
                label = "onboarding step",
            ) { shown ->
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (shown) {
                        STEP_WELCOME -> WelcomeStep(state, next)
                        STEP_CALENDAR -> CalendarStep(state, next)
                        STEP_STYLE -> StyleStep(state, next)
                        else -> WallpaperStep(state, onFinish)
                    }
                }
            }
        }
    }
}

/** "Step 2 of 4", a progress bar and Skip. */
@Composable
private fun Progress(
    step: Int,
    onSkip: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.onboarding_step, step + 1, STEP_COUNT),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (step < LAST_STEP) TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
        }
        LinearProgressIndicator(progress = { (step + 1f) / STEP_COUNT }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ColumnScope.WelcomeStep(
    state: ScreenState,
    next: () -> Unit,
) {
    PreviewPane(state, locked = false, onLockedChange = null, modifier = Modifier.weight(1f))
    Title(stringResource(R.string.onboarding_welcome_title))
    Body(stringResource(R.string.onboarding_welcome_body))
    Button(onClick = next, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(stringResource(R.string.onboarding_start))
    }
}

@Composable
private fun ColumnScope.CalendarStep(
    state: ScreenState,
    next: () -> Unit,
) {
    val permission = state.permission
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        TonalIcon(R.drawable.ic_event)
        Title(stringResource(R.string.onboarding_calendar_title))
        Body(stringResource(R.string.onboarding_calendar_body))
        if (permission.blocked) Body(stringResource(R.string.calendar_blocked))
    }
    when {
        permission.granted -> {
            Button(onClick = next, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_next))
            }
        }

        permission.blocked -> {
            Button(onClick = permission::openAppSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_app_settings))
            }
        }

        else -> {
            Button(onClick = permission::launchRequest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_calendar_allow))
            }
        }
    }
    if (!permission.granted) {
        TextButton(onClick = next, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(stringResource(R.string.onboarding_calendar_skip))
        }
    }
}

@Composable
private fun ColumnScope.StyleStep(
    state: ScreenState,
    next: () -> Unit,
) {
    Title(stringResource(R.string.onboarding_style_title))
    Body(stringResource(R.string.onboarding_style_body))
    PreviewPane(state, locked = false, onLockedChange = null, modifier = Modifier.weight(1f))
    LayoutPicker(state, Modifier.fillMaxWidth())
    PalettePicker(state, Modifier.fillMaxWidth())
    Button(onClick = next, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(stringResource(R.string.onboarding_next))
    }
}

@Composable
private fun ColumnScope.WallpaperStep(
    state: ScreenState,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveActive = remember(state.resumeCount) { LiveWallpaper.isActive(context) }
    val unavailable = stringResource(R.string.live_unavailable)
    val failed = stringResource(R.string.static_failed)
    // Coming back from Android's preview with the wallpaper set: done.
    LaunchedEffect(liveActive) { if (liveActive) onFinish() }

    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        TonalIcon(R.drawable.ic_wallpaper)
        Title(stringResource(R.string.onboarding_wallpaper_title))
        Body(stringResource(R.string.onboarding_wallpaper_body))
        listOf(
            R.string.onboarding_wallpaper_step1,
            R.string.onboarding_wallpaper_step2,
            R.string.onboarding_wallpaper_step3,
        ).forEachIndexed { index, text -> NumberedStep(index + 1, stringResource(text)) }
    }
    Button(
        onClick = { if (!openLivePicker(context)) scope.launch { state.snackbar.showSnackbar(unavailable) } },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.live_set)) }
    OutlinedButton(
        onClick = {
            state.editor.commit { it.copy(batteryMode = BatteryMode.STATIC) }
            scope.launch {
                val outcome = state.services.staticUpdater.refresh(force = true)
                if (outcome is Outcome.Success) onFinish() else state.snackbar.showSnackbar(failed)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.onboarding_use_static)) }
    TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(stringResource(R.string.onboarding_later))
    }
}

@Composable
private fun NumberedStep(
    number: Int,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val STEP_WELCOME = 0
private const val STEP_CALENDAR = 1
private const val STEP_STYLE = 2
private const val STEP_COUNT = 4
private const val LAST_STEP = STEP_COUNT - 1

/** Steps slide in by a third of the width, with a fade. */
private const val SLIDE_FRACTION = 3

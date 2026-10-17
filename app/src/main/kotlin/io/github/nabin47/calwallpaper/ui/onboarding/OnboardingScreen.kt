package io.github.nabin47.calwallpaper.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.AppServices
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.ui.CalendarPermissionState
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.rememberCalendarPermission
import io.github.nabin47.calwallpaper.ui.settings.SettingsEditor
import io.github.nabin47.calwallpaper.ui.settings.WallpaperSection
import io.github.nabin47.calwallpaper.wallpaper.LiveWallpaper

/**
 * First-run flow: what the app does, calendar access with its rationale (asked here, in context, never at
 * startup; CLAUDE.md §7), then choosing and setting the wallpaper. Every step can be skipped; [onFinish] opens
 * the settings.
 */
@Composable
fun OnboardingScreen(
    services: AppServices,
    editor: SettingsEditor,
    settings: Settings,
    resumeCount: Int,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(STEP_WELCOME) }
    val permission = rememberCalendarPermission(resumeCount) { granted -> if (granted) step = STEP_READY }
    BackHandler(enabled = step > STEP_WELCOME) { step-- }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        when (step) {
            STEP_WELCOME -> WelcomeStep { step = STEP_CALENDAR }
            STEP_CALENDAR -> CalendarStep(permission) { step = STEP_READY }
            else -> ReadyStep(services, editor, settings, resumeCount, onFinish)
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Title(stringResource(R.string.onboarding_welcome_title))
    Body(stringResource(R.string.onboarding_welcome_body))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_next)) }
}

@Composable
private fun CalendarStep(
    permission: CalendarPermissionState,
    onNext: () -> Unit,
) {
    Title(stringResource(R.string.onboarding_calendar_title))
    Body(stringResource(R.string.onboarding_calendar_body))
    when {
        permission.granted -> {
            SettingHint(stringResource(R.string.onboarding_calendar_granted))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_next))
            }
        }

        permission.blocked -> {
            SettingHint(stringResource(R.string.calendar_blocked))
            OutlinedButton(onClick = permission::openAppSettings, modifier = Modifier.fillMaxWidth()) {
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
        TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_calendar_skip))
        }
    }
}

@Composable
private fun ReadyStep(
    services: AppServices,
    editor: SettingsEditor,
    settings: Settings,
    resumeCount: Int,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val liveActive = remember(resumeCount) { LiveWallpaper.isActive(context) }
    Title(stringResource(R.string.onboarding_ready_title))
    Body(stringResource(R.string.onboarding_ready_body))
    WallpaperSection(
        settings = settings,
        editor = editor,
        liveActive = liveActive,
        applyStatic = { services.staticUpdater.refresh(force = true) },
    )
    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_finish)) }
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

private const val STEP_WELCOME = 0
private const val STEP_CALENDAR = 1
private const val STEP_READY = 2

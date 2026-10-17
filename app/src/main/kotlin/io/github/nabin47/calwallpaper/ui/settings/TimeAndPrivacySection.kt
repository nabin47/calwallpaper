package io.github.nabin47.calwallpaper.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.domain.settings.ClockFormat
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.WeekStart
import io.github.nabin47.calwallpaper.ui.components.Choice
import io.github.nabin47.calwallpaper.ui.components.ChoiceSetting
import io.github.nabin47.calwallpaper.ui.components.SectionHeader

/** Week start and 12/24-hour time (CLAUDE.md §6). */
@Composable
fun TimeSection(
    settings: Settings,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ChoiceSetting(
            label = stringResource(R.string.week_start),
            hint = stringResource(R.string.week_start_hint),
            choices =
                listOf(
                    Choice(WeekStart.LOCALE, stringResource(R.string.week_locale)),
                    Choice(WeekStart.MONDAY, stringResource(R.string.week_monday)),
                    Choice(WeekStart.SATURDAY, stringResource(R.string.week_saturday)),
                    Choice(WeekStart.SUNDAY, stringResource(R.string.week_sunday)),
                ),
            selected = settings.time.weekStart,
            onSelect = { start -> editor.commit { it.copy(time = it.time.copy(weekStart = start)) } },
        )
        ChoiceSetting(
            label = stringResource(R.string.clock_label),
            hint = stringResource(R.string.clock_hint),
            choices =
                listOf(
                    Choice(ClockFormat.SYSTEM, stringResource(R.string.clock_system)),
                    Choice(ClockFormat.H12, stringResource(R.string.clock_12)),
                    Choice(ClockFormat.H24, stringResource(R.string.clock_24)),
                ),
            selected = settings.time.clockFormat,
            onSelect = { format -> editor.commit { it.copy(time = it.time.copy(clockFormat = format)) } },
        )
    }
}

/** Privacy mode: when titles are hidden (CLAUDE.md §2, §6). */
@Composable
fun PrivacySection(
    settings: Settings,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SectionHeader(stringResource(R.string.section_privacy))
        ChoiceSetting(
            label = stringResource(R.string.privacy_label),
            hint = stringResource(R.string.privacy_hint),
            choices =
                listOf(
                    Choice(PrivacyMode.OFF, stringResource(R.string.privacy_off)),
                    Choice(PrivacyMode.WHEN_LOCKED, stringResource(R.string.privacy_when_locked)),
                    Choice(PrivacyMode.ALWAYS, stringResource(R.string.privacy_always)),
                ),
            selected = settings.privacyMode,
            onSelect = { mode -> editor.commit { it.copy(privacyMode = mode) } },
        )
    }
}

package io.github.nabin47.calwallpaper.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.domain.settings.BatteryMode
import io.github.nabin47.calwallpaper.domain.settings.ClockFormat
import io.github.nabin47.calwallpaper.domain.settings.FontChoice
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.VerticalPosition
import io.github.nabin47.calwallpaper.domain.settings.WeekStart
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.components.NavigationRow
import io.github.nabin47.calwallpaper.ui.components.SettingsGroup
import io.github.nabin47.calwallpaper.ui.navigation.Route
import kotlin.math.roundToInt

/** Every settings page, grouped, each row summarizing its current values so nothing is hidden behind a tap. */
@Composable
fun SettingsList(
    state: ScreenState,
    modifier: Modifier = Modifier,
) {
    val s = state.settings
    val go = state.navigate
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsGroup(title = stringResource(R.string.home_customize_title)) {
            NavigationRow(R.drawable.ic_palette, stringResource(R.string.section_appearance), appearanceSummary(s)) {
                go(Route.Appearance)
            }
            NavigationRow(R.drawable.ic_dashboard, stringResource(R.string.section_layout), layoutSummary(s)) {
                go(Route.Layout)
            }
            NavigationRow(R.drawable.ic_lock, stringResource(R.string.section_lock_screen), lockSummary(s)) {
                go(Route.LockScreen)
            }
            NavigationRow(R.drawable.ic_event, stringResource(R.string.section_content), sourcesSummary(s)) {
                go(Route.Sources)
            }
            NavigationRow(R.drawable.ic_schedule, stringResource(R.string.section_time), timeSummary(s)) {
                go(Route.DateTime)
            }
        }
        SettingsGroup(title = stringResource(R.string.home_more_title)) {
            NavigationRow(R.drawable.ic_wallpaper, stringResource(R.string.section_wallpaper), modeSummary(s)) {
                go(Route.WallpaperMode)
            }
            NavigationRow(
                R.drawable.ic_widgets,
                stringResource(R.string.section_widgets),
                stringResource(R.string.summary_widgets),
            ) {
                go(Route.Widgets)
            }
            NavigationRow(
                R.drawable.ic_backup,
                stringResource(R.string.section_backup),
                stringResource(R.string.summary_backup),
            ) {
                go(Route.Backup)
            }
            NavigationRow(
                R.drawable.ic_info,
                stringResource(R.string.section_about),
                stringResource(R.string.summary_about),
            ) {
                go(Route.About)
            }
        }
    }
}

@Composable
private fun appearanceSummary(s: Settings): String {
    val a = s.appearance
    val colors =
        if (a.useDynamicColor) {
            stringResource(R.string.palette_dynamic)
        } else {
            stringResource(
                when (a.palette) {
                    PalettePreset.CALM_DARK -> R.string.palette_calm_dark
                    PalettePreset.CALM_LIGHT -> R.string.palette_calm_light
                    PalettePreset.PAPER -> R.string.palette_paper
                    PalettePreset.MIDNIGHT -> R.string.palette_midnight
                    PalettePreset.CUSTOM -> R.string.palette_custom
                },
            )
        }
    val font =
        stringResource(
            when (a.font) {
                FontChoice.SYSTEM -> R.string.font_system
                FontChoice.SANS -> R.string.font_sans
                FontChoice.SERIF -> R.string.font_serif
                FontChoice.MONO -> R.string.font_mono
            },
        )
    return joined(
        colors,
        stringResource(R.string.summary_font, font),
        stringResource(
            R.string.summary_text_size,
            (
                a.textScale *
                    100
            ).roundToInt(),
        ),
    )
}

@Composable
private fun layoutSummary(s: Settings): String =
    joined(stringResource(presetLabel(s.layout.preset)), stringResource(positionLabel(s.layout.position)))

@Composable
private fun lockSummary(s: Settings): String {
    val privacy =
        stringResource(
            when (s.privacyMode) {
                PrivacyMode.OFF -> R.string.summary_privacy_off
                PrivacyMode.WHEN_LOCKED -> R.string.summary_privacy_when_locked
                PrivacyMode.ALWAYS -> R.string.summary_privacy_always
            },
        )
    return joined(privacy, stringResource(positionLabel(s.layout.lockPosition)))
}

@Composable
private fun sourcesSummary(s: Settings): String {
    val sources =
        listOfNotNull(
            stringResource(R.string.source_calendar).takeIf { SourceId.CALENDAR_PROVIDER in s.content.enabledSources },
            stringResource(R.string.source_local).takeIf { SourceId.LOCAL_TASKS in s.content.enabledSources },
            stringResource(R.string.source_todoist).takeIf { SourceId.TODOIST in s.content.enabledSources },
        )
    val shown = if (sources.isEmpty()) stringResource(R.string.summary_nothing) else sources.joinToString(", ")
    return joined(shown, pluralStringResource(R.plurals.summary_days, s.content.daysAhead, s.content.daysAhead))
}

@Composable
private fun timeSummary(s: Settings): String {
    val week =
        stringResource(
            when (s.time.weekStart) {
                WeekStart.LOCALE -> R.string.week_locale
                WeekStart.MONDAY -> R.string.week_monday
                WeekStart.SATURDAY -> R.string.week_saturday
                WeekStart.SUNDAY -> R.string.week_sunday
            },
        )
    val clock =
        stringResource(
            when (s.time.clockFormat) {
                ClockFormat.SYSTEM -> R.string.summary_clock_system
                ClockFormat.H12 -> R.string.clock_12
                ClockFormat.H24 -> R.string.clock_24
            },
        )
    return joined(stringResource(R.string.summary_week_start, week), clock)
}

@Composable
private fun modeSummary(s: Settings): String =
    stringResource(if (s.batteryMode == BatteryMode.LIVE) R.string.summary_mode_live else R.string.summary_mode_static)

private fun presetLabel(preset: LayoutPreset): Int =
    when (preset) {
        LayoutPreset.AGENDA -> R.string.preset_agenda
        LayoutPreset.WEEK_STRIP -> R.string.preset_week
        LayoutPreset.MONTH_GRID -> R.string.preset_month
        LayoutPreset.NEXT_THREE -> R.string.preset_next_three
    }

private fun positionLabel(position: VerticalPosition): Int =
    when (position) {
        VerticalPosition.TOP -> R.string.summary_position_top
        VerticalPosition.CENTER -> R.string.summary_position_center
        VerticalPosition.BOTTOM -> R.string.summary_position_bottom
    }

/** Summary parts separated by a middle dot. */
private fun joined(vararg parts: String): String = parts.joinToString(" · ")

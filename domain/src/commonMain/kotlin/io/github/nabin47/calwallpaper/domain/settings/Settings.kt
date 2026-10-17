package io.github.nabin47.calwallpaper.domain.settings

import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.domain.SafeZone
import kotlinx.serialization.Serializable

/**
 * Every user setting (CLAUDE.md §6), with sensible defaults. Immutable; change it with `copy` and store it
 * through a [SettingsRepository].
 *
 * Values from storage or imports may be out of range; call [normalized] (the codec and repository do) before
 * rendering. Adding a field with a default is backward compatible; renaming or changing meaning needs a
 * [SettingsMigration] and a bump of [SettingsCodec.CURRENT_VERSION].
 */
@Serializable
data class Settings(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val content: ContentSettings = ContentSettings(),
    val time: TimeSettings = TimeSettings(),
    val privacyMode: PrivacyMode = PrivacyMode.WHEN_LOCKED,
    val batteryMode: BatteryMode = BatteryMode.LIVE,
) {
    /** A copy with every value clamped into its valid range; non-finite numbers revert to defaults. */
    fun normalized(): Settings =
        copy(
            appearance = appearance.normalized(),
            layout = layout.normalized(),
            content = content.normalized(),
        )
}

/**
 * Colors, type and surfaces.
 *
 * @property customColors used when [palette] is [PalettePreset.CUSTOM]; ignored otherwise.
 * @property useDynamicColor derive colors from the system wallpaper (Material You, Android 12+).
 * @property textScale multiplier on the base text size, in [TEXT_SCALE].
 * @property textOpacity text alpha, in [UNIT].
 * @property panelOpacity alpha of the panel behind the text (0 = no panel), in [UNIT].
 * @property cornerRadiusDp panel corner radius, in [CORNER_RADIUS_DP].
 */
@Serializable
data class AppearanceSettings(
    val palette: PalettePreset = PalettePreset.CALM_DARK,
    val customColors: CustomColors? = null,
    val useDynamicColor: Boolean = false,
    val font: FontChoice = FontChoice.SYSTEM,
    val textScale: Float = 1f,
    val textOpacity: Float = 1f,
    val panelOpacity: Float = DEFAULT_PANEL_OPACITY,
    val cornerRadiusDp: Int = DEFAULT_CORNER_RADIUS_DP,
    val density: SpacingDensity = SpacingDensity.COMFORTABLE,
) {
    /** A copy with all numbers in range. */
    fun normalized(): AppearanceSettings =
        copy(
            textScale = textScale.clampOr(TEXT_SCALE, 1f),
            textOpacity = textOpacity.clampOr(UNIT, 1f),
            panelOpacity = panelOpacity.clampOr(UNIT, DEFAULT_PANEL_OPACITY),
            cornerRadiusDp = cornerRadiusDp.coerceIn(CORNER_RADIUS_DP),
        )

    /** Valid ranges and defaults. */
    companion object {
        /** Allowed text scale; the upper bound keeps large-font users readable without overflowing. */
        val TEXT_SCALE: ClosedFloatingPointRange<Float> = 0.8f..1.6f

        /** Allowed opacity. */
        val UNIT: ClosedFloatingPointRange<Float> = 0f..1f

        /** Allowed corner radius in dp. */
        val CORNER_RADIUS_DP: IntRange = 0..32

        /** Default panel opacity: subtle, keeps the wallpaper visible. */
        const val DEFAULT_PANEL_OPACITY: Float = 0.35f

        /** Default corner radius in dp. */
        const val DEFAULT_CORNER_RADIUS_DP: Int = 16
    }
}

/** The user's own colors for [PalettePreset.CUSTOM]. */
@Serializable
data class CustomColors(
    val panel: ArgbColor,
    val text: ArgbColor,
    val accent: ArgbColor,
)

/**
 * Placement on the screen.
 *
 * @property safeZoneTop fraction of screen height kept clear at the top (lock-screen clock), see [SafeZone].
 * @property safeZoneBottom fraction kept clear at the bottom (shortcuts, gesture bar).
 * @property horizontalInsetDp side margin, in [HORIZONTAL_INSET_DP].
 */
@Serializable
data class LayoutSettings(
    val preset: LayoutPreset = LayoutPreset.AGENDA,
    val position: VerticalPosition = VerticalPosition.CENTER,
    val safeZoneTop: Float = SafeZone.DEFAULT.top,
    val safeZoneBottom: Float = SafeZone.DEFAULT.bottom,
    val horizontalInsetDp: Int = DEFAULT_HORIZONTAL_INSET_DP,
) {
    /** The validated safe zone. Safe to call on non-normalized values (falls back to the default). */
    val safeZone: SafeZone
        get() = with(normalized()) { SafeZone(top = safeZoneTop, bottom = safeZoneBottom) }

    /** A copy with all numbers in range, guaranteeing [SafeZone]'s invariants. */
    fun normalized(): LayoutSettings {
        val top = safeZoneTop.clampOr(SAFE_ZONE, SafeZone.DEFAULT.top)
        val bottom = safeZoneBottom.clampOr(SAFE_ZONE, SafeZone.DEFAULT.bottom)
        // Both at the 0.5 maximum would leave no room; fall back to the defaults.
        val fits = top + bottom < 1f
        return copy(
            safeZoneTop = if (fits) top else SafeZone.DEFAULT.top,
            safeZoneBottom = if (fits) bottom else SafeZone.DEFAULT.bottom,
            horizontalInsetDp = horizontalInsetDp.coerceIn(HORIZONTAL_INSET_DP),
        )
    }

    /** Valid ranges and defaults. */
    companion object {
        /** Allowed safe-zone fraction per edge; matches [SafeZone]. */
        val SAFE_ZONE: ClosedFloatingPointRange<Float> = 0f..0.5f

        /** Allowed side margin in dp. */
        val HORIZONTAL_INSET_DP: IntRange = 0..64

        /** Default side margin in dp. */
        const val DEFAULT_HORIZONTAL_INSET_DP: Int = 24
    }
}

/**
 * What to show.
 *
 * @property enabledSources sources to read; a source still needs its permission or token to produce items.
 * @property selectedCalendarIds Calendar Provider calendar ids to show; `null` means all visible calendars.
 * @property selectedTaskListIds task list or project ids to show; `null` means all.
 * @property daysAhead length of the date range starting today, in [DAYS_AHEAD].
 * @property showCompleted include completed tasks.
 */
@Serializable
data class ContentSettings(
    val enabledSources: Set<SourceId> = setOf(SourceId.CALENDAR_PROVIDER, SourceId.LOCAL_TASKS),
    val selectedCalendarIds: Set<String>? = null,
    val selectedTaskListIds: Set<String>? = null,
    val daysAhead: Int = DEFAULT_DAYS_AHEAD,
    val showCompleted: Boolean = false,
) {
    /** A copy with all numbers in range. */
    fun normalized(): ContentSettings = copy(daysAhead = daysAhead.coerceIn(DAYS_AHEAD))

    /** Valid ranges and defaults. */
    companion object {
        /** Allowed range length in days (today plus up to a month). */
        val DAYS_AHEAD: IntRange = 1..31

        /** Default range: one week. */
        const val DEFAULT_DAYS_AHEAD: Int = 7
    }
}

/** Calendar conventions. */
@Serializable
data class TimeSettings(
    val weekStart: WeekStart = WeekStart.LOCALE,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
)

/** Clamps into [range]; NaN and infinities become [fallback]. */
private fun Float.clampOr(
    range: ClosedFloatingPointRange<Float>,
    fallback: Float,
): Float = if (isFinite()) coerceIn(range) else fallback

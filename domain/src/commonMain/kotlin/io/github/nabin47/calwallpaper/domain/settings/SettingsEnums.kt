package io.github.nabin47.calwallpaper.domain.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Serial names are persisted: never rename them; add new entries instead. Unknown names (from a newer app
// version) fall back to the property's default when decoding (SettingsCodec uses coerceInputValues).

/** Built-in color schemes. [CUSTOM] uses [AppearanceSettings.customColors]. */
@Serializable
enum class PalettePreset {
    /** Light text on a dark translucent panel. */
    @SerialName("calm_dark")
    CALM_DARK,

    /** Dark text on a light translucent panel. */
    @SerialName("calm_light")
    CALM_LIGHT,

    /** Warm off-white panel, ink-like text. */
    @SerialName("paper")
    PAPER,

    /** Near-black panel with a cool accent, for OLED screens. */
    @SerialName("midnight")
    MIDNIGHT,

    /** The user's own colors from [AppearanceSettings.customColors]. */
    @SerialName("custom")
    CUSTOM,
}

/** Typeface for wallpaper and widget text. Bundled fonts arrive later (CLAUDE.md §5); system is the default. */
@Serializable
enum class FontChoice {
    /** The device's default UI font. */
    @SerialName("system")
    SYSTEM,

    /** The device's sans-serif family. */
    @SerialName("sans")
    SANS,

    /** The device's serif family. */
    @SerialName("serif")
    SERIF,

    /** The device's monospace family. */
    @SerialName("mono")
    MONO,
}

/** How tightly rows are packed. */
@Serializable
enum class SpacingDensity {
    /** Tight rows; fits the most items. */
    @SerialName("compact")
    COMPACT,

    /** Balanced default. */
    @SerialName("comfortable")
    COMFORTABLE,

    /** Generous rows for readability. */
    @SerialName("spacious")
    SPACIOUS,
}

/** Overall layout of the agenda on the wallpaper (CLAUDE.md §6). */
@Serializable
enum class LayoutPreset {
    /** Vertical list of upcoming items grouped by day. */
    @SerialName("agenda")
    AGENDA,

    /** One row of days with items beneath. */
    @SerialName("week_strip")
    WEEK_STRIP,

    /** Month calendar with dots or counts. */
    @SerialName("month_grid")
    MONTH_GRID,

    /** Only the next three items, large and minimal. */
    @SerialName("next_three")
    NEXT_THREE,
}

/** Where the agenda block sits between the safe-zone insets. */
@Serializable
enum class VerticalPosition {
    /** Just below the top safe zone. */
    @SerialName("top")
    TOP,

    /** Centered between the safe zones. */
    @SerialName("center")
    CENTER,

    /** Just above the bottom safe zone. */
    @SerialName("bottom")
    BOTTOM,
}

/** First day of the week in week and month layouts. */
@Serializable
enum class WeekStart {
    /** Follow the device locale. */
    @SerialName("locale")
    LOCALE,

    /** Monday. */
    @SerialName("monday")
    MONDAY,

    /** Saturday. */
    @SerialName("saturday")
    SATURDAY,

    /** Sunday. */
    @SerialName("sunday")
    SUNDAY,
}

/** 12- or 24-hour times. */
@Serializable
enum class ClockFormat {
    /** Follow the device setting. */
    @SerialName("system")
    SYSTEM,

    /** 12-hour with AM/PM. */
    @SerialName("h12")
    H12,

    /** 24-hour. */
    @SerialName("h24")
    H24,
}

/** What the wallpaper hides, since the lock screen is visible to anyone (CLAUDE.md §2). */
@Serializable
enum class PrivacyMode {
    /** Always show titles. */
    @SerialName("off")
    OFF,

    /** Hide titles (show counts or dots) while the device is locked. */
    @SerialName("when_locked")
    WHEN_LOCKED,

    /** Never show titles on the wallpaper. */
    @SerialName("always")
    ALWAYS,
}

/** Live wallpaper versus a battery-saving static image. */
@Serializable
enum class BatteryMode {
    /** Live wallpaper: redraws on data, settings and time changes; supports taps. */
    @SerialName("live")
    LIVE,

    /** Static image regenerated on changes; no live wallpaper service. */
    @SerialName("static")
    STATIC,
}

/** Screens the static wallpaper image is applied to (Android's home and lock wallpapers). */
@Serializable
enum class WallpaperTarget {
    /** Home screen only. */
    @SerialName("home")
    HOME,

    /** Lock screen only. */
    @SerialName("lock")
    LOCK,

    /** Both screens. */
    @SerialName("home_and_lock")
    HOME_AND_LOCK,
}

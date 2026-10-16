package io.github.nabin47.calwallpaper.domain

/**
 * Screen area the wallpaper layout must leave free, as fractions of the screen height.
 *
 * The lock-screen clock and notifications overlap the wallpaper (CLAUDE.md §2), so the layout keeps
 * [top] and [bottom] clear. Each fraction is in `0.0..0.5`; together they must leave some height.
 */
data class SafeZone(
    val top: Float,
    val bottom: Float,
) {
    init {
        require(top in RANGE) { "top must be in $RANGE" }
        require(bottom in RANGE) { "bottom must be in $RANGE" }
        require(top + bottom < 1f) { "top + bottom must leave some height" }
    }

    /** Defaults and limits. */
    companion object {
        private val RANGE = 0f..0.5f

        /** Clears a typical lock-screen clock at the top and the shortcuts row at the bottom. */
        val DEFAULT = SafeZone(top = 0.25f, bottom = 0.12f)
    }
}

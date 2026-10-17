package io.github.nabin47.calwallpaper.wallpaper.staticmode

import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.WallpaperTarget
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Which Android wallpaper a static image goes to. */
enum class Screen {
    /** The home screen ("system") wallpaper. */
    HOME,

    /** The lock-screen wallpaper. */
    LOCK,
}

/**
 * One image to render and the screens it is set on. [locked] is passed to the layout as the lock state, so the
 * lock-screen image honors privacy mode while the home image shows titles.
 */
data class StaticImage(
    val locked: Boolean,
    val screens: Set<Screen>,
)

/** Pure planning for static (battery) mode. */
object StaticPlan {
    /**
     * The images for [target]. Home and lock share one image when they would look the same (privacy mode off or
     * always on); otherwise each screen gets its own, because privacy mode hides titles only while locked.
     */
    fun images(
        target: WallpaperTarget,
        privacy: PrivacyMode,
    ): List<StaticImage> =
        when (target) {
            WallpaperTarget.HOME -> {
                listOf(StaticImage(locked = false, screens = setOf(Screen.HOME)))
            }

            WallpaperTarget.LOCK -> {
                listOf(StaticImage(locked = true, screens = setOf(Screen.LOCK)))
            }

            WallpaperTarget.HOME_AND_LOCK -> {
                if (privacy == PrivacyMode.WHEN_LOCKED) {
                    listOf(
                        StaticImage(locked = false, screens = setOf(Screen.HOME)),
                        StaticImage(locked = true, screens = setOf(Screen.LOCK)),
                    )
                } else {
                    listOf(
                        StaticImage(locked = privacy == PrivacyMode.ALWAYS, screens = setOf(Screen.HOME, Screen.LOCK)),
                    )
                }
            }
        }

    /**
     * When to refresh next for the date change: shortly after the next local midnight in [zone]. The small delay
     * makes sure "today" has rolled over even if the alarm fires a little early.
     */
    fun nextMidnightRefresh(
        now: Instant,
        zone: TimeZone,
    ): Instant {
        val tomorrow: LocalDate = now.toLocalDateTime(zone).date.plus(1, DateTimeUnit.DAY)
        return tomorrow.atStartOfDayIn(zone) + MIDNIGHT_GRACE
    }

    private val MIDNIGHT_GRACE = 1.minutes
}

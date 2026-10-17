package io.github.nabin47.calwallpaper.quickadd

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** When a quick-added task is due. */
enum class QuickDue {
    /** No date: shown on today until done. */
    NONE,

    /** Today. */
    TODAY,

    /** Tomorrow. */
    TOMORROW,
    ;

    /** The due date for this choice, with [today] as reference; `null` for [NONE]. */
    fun dateFrom(today: LocalDate): LocalDate? =
        when (this) {
            NONE -> null
            TODAY -> today
            TOMORROW -> today.plus(1, DateTimeUnit.DAY)
        }
}

package io.github.nabin47.calwallpaper.core.time

import kotlin.time.Instant

/** A half-open span on the timeline, `[start, endExclusive)`. Immutable. */
data class InstantRange(
    val start: Instant,
    val endExclusive: Instant,
) {
    init {
        require(endExclusive >= start) { "endExclusive must not be before start" }
    }

    /**
     * True if the span `[from, to)` shares any time with this range. A zero-length span (`from == to`) counts
     * as a point and overlaps when it lies inside the range.
     */
    fun overlaps(
        from: Instant,
        to: Instant,
    ): Boolean =
        if (from == to) {
            from >= start && from < endExclusive
        } else {
            from < endExclusive && to > start
        }
}

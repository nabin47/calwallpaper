package io.github.nabin47.calwallpaper.data.calendar

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.provider.CalendarContract
import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.core.time.DateRange
import io.github.nabin47.calwallpaper.domain.AgendaSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone

/**
 * [AgendaSource] backed by the Android Calendar Provider: every calendar synced to the device (including Google
 * Calendar), without OAuth. Needs `READ_CALENDAR`; without it, emits one [Outcome.Failure] and completes, so
 * callers re-collect after the permission is granted.
 *
 * Re-queries whenever the provider reports a change through a [ContentObserver] (no polling, CLAUDE.md §5).
 * Bursts of changes during a sync are conflated, so at most one query is pending at a time. Queries run on
 * [ioDispatcher]. Only visible calendars and non-cancelled events are returned.
 */
class CalendarProviderSource(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AgendaSource {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    override val id: SourceId = SourceId.CALENDAR_PROVIDER

    override fun observe(
        range: DateRange,
        zone: TimeZone,
    ): Flow<Outcome<List<AgendaItem>>> {
        if (!hasPermission()) return flowOf(Outcome.Failure(PERMISSION_MISSING))
        return providerChanges()
            .conflate()
            .map { query(range, zone) }
            .flowOn(ioDispatcher)
    }

    /** True if `READ_CALENDAR` is granted. */
    fun hasPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Emits once immediately, then once per provider change notification, until cancelled. */
    private fun providerChanges(): Flow<Unit> =
        callbackFlow {
            val observer =
                object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) {
                        trySend(Unit)
                    }
                }
            resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            trySend(Unit)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }

    private fun query(
        range: DateRange,
        zone: TimeZone,
    ): Outcome<List<AgendaItem>> {
        val window = range.toInstantRange(zone)
        val uri =
            CalendarContract.Instances.CONTENT_URI
                .buildUpon()
                .also {
                    ContentUris.appendId(it, window.start.toEpochMilliseconds())
                    ContentUris.appendId(it, window.endExclusive.toEpochMilliseconds())
                }.build()
        return try {
            val rows =
                resolver
                    .query(uri, PROJECTION, SELECTION, null, SORT_ORDER)
                    ?.use { cursor -> cursor.readRows() }
                    ?: return Outcome.Failure(PROVIDER_UNAVAILABLE)
            Outcome.Success(rows.map { it.toAgendaItem() })
        } catch (e: SecurityException) {
            // Permission revoked while observing.
            Outcome.Failure("$PERMISSION_MISSING (${e::class.simpleName})")
        }
    }

    private fun Cursor.readRows(): List<CalendarInstanceRow> {
        val eventId = getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val begin = getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val end = getColumnIndexOrThrow(CalendarContract.Instances.END)
        val title = getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val allDay = getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
        val calendarId = getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
        val color = getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)
        val rows = ArrayList<CalendarInstanceRow>(count)
        while (moveToNext()) {
            rows +=
                CalendarInstanceRow(
                    eventId = getLong(eventId),
                    beginMillis = getLong(begin),
                    endMillis = if (isNull(end)) getLong(begin) else getLong(end),
                    title = if (isNull(title)) null else getString(title),
                    allDay = getInt(allDay) != 0,
                    calendarId = getLong(calendarId),
                    displayColor = if (isNull(color)) null else getInt(color),
                )
        }
        return rows
    }

    private companion object {
        const val PERMISSION_MISSING = "Calendar permission not granted"
        const val PROVIDER_UNAVAILABLE = "Calendar provider unavailable"

        val PROJECTION =
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.DISPLAY_COLOR,
            )

        /** Visible calendars only; skip cancelled events. */
        const val SELECTION =
            "${CalendarContract.Instances.VISIBLE} = 1 AND " +
                "(${CalendarContract.Instances.STATUS} IS NULL OR " +
                "${CalendarContract.Instances.STATUS} != ${CalendarContract.Events.STATUS_CANCELED})"

        const val SORT_ORDER = "${CalendarContract.Instances.BEGIN} ASC"
    }
}

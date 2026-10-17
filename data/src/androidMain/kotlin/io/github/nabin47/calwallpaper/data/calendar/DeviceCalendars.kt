package io.github.nabin47.calwallpaper.data.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import io.github.nabin47.calwallpaper.core.Outcome
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One calendar on the device, for the "which calendars" setting.
 *
 * @property id the Calendar Provider id as a string, matching [io.github.nabin47.calwallpaper.core.AgendaItem.groupId].
 * @property accountName the owning account (often an email address). User data: show it, never log it.
 */
data class DeviceCalendar(
    val id: String,
    val displayName: String,
    val accountName: String,
    val colorArgb: Int?,
) {
    /** Redacts names so calendars can appear in logs safely. */
    override fun toString(): String = "DeviceCalendar(id=$id)"
}

/**
 * Lists the calendars synced to the device (Calendar Provider), visible ones only, sorted by account and name.
 * Needs `READ_CALENDAR`; without it returns [Outcome.Failure]. Runs the query on [ioDispatcher].
 */
class DeviceCalendars(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appContext = context.applicationContext

    /** Queries the provider once. */
    suspend fun list(): Outcome<List<DeviceCalendar>> =
        withContext(ioDispatcher) {
            if (appContext.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext Outcome.Failure(PERMISSION_MISSING)
            }
            try {
                val calendars =
                    appContext.contentResolver
                        .query(CalendarContract.Calendars.CONTENT_URI, PROJECTION, SELECTION, null, null)
                        ?.use { cursor ->
                            val id = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                            val name = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                            val account = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                            val color = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
                            buildList<DeviceCalendar> {
                                while (cursor.moveToNext()) {
                                    add(
                                        DeviceCalendar(
                                            id = cursor.getLong(id).toString(),
                                            displayName = cursor.getString(name).orEmpty(),
                                            accountName = cursor.getString(account).orEmpty(),
                                            colorArgb = if (cursor.isNull(color)) null else cursor.getInt(color),
                                        ),
                                    )
                                }
                            }
                        } ?: return@withContext Outcome.Failure(PROVIDER_UNAVAILABLE)
                Outcome.Success(calendars.sortedWith(compareBy({ it.accountName }, { it.displayName })))
            } catch (e: SecurityException) {
                Outcome.Failure("$PERMISSION_MISSING (${e::class.simpleName})")
            }
        }

    private companion object {
        const val PERMISSION_MISSING = "Calendar permission not granted"
        const val PROVIDER_UNAVAILABLE = "Calendar provider unavailable"

        val PROJECTION =
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
            )

        const val SELECTION = "${CalendarContract.Calendars.VISIBLE} = 1"
    }
}

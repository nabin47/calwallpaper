package io.github.nabin47.calwallpaper.wallpaper.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/** System changes that can make the wallpaper's picture outdated. */
enum class SystemEvent {
    /** The date, time or time zone changed (includes midnight). */
    TIME,

    /** The device was unlocked or the screen turned on: privacy mode may need to show or hide titles. */
    LOCK_STATE,

    /** The user unlocked for the first time since boot: live data is now readable (Direct Boot). */
    USER_UNLOCKED,
}

/**
 * Listens for the broadcasts behind [SystemEvent] while registered. These replace timers (CLAUDE.md §5):
 * nothing runs between events. Registered at runtime (several of these broadcasts cannot be received from the
 * manifest) and only while the wallpaper engine exists. Callbacks arrive on the main thread.
 */
class SystemEvents(
    context: Context,
    private val onEvent: (SystemEvent) -> Unit,
) {
    private val appContext = context.applicationContext
    private var registered = false

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                eventFor(intent.action)?.let(onEvent)
            }
        }

    /** Starts listening. Idempotent. */
    fun register() {
        if (registered) return
        // Listed one by one (not from ACTIONS) so Android Lint can see they are all protected system broadcasts.
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_UNLOCKED)
            }
        // All of these are protected system broadcasts, so a not-exported receiver still gets them.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
        registered = true
    }

    /** Stops listening. Idempotent. */
    fun unregister() {
        if (!registered) return
        appContext.unregisterReceiver(receiver)
        registered = false
    }

    /** Mapping from broadcast action to event. */
    companion object {
        private val ACTIONS: Map<String, SystemEvent> =
            mapOf(
                Intent.ACTION_DATE_CHANGED to SystemEvent.TIME,
                Intent.ACTION_TIME_CHANGED to SystemEvent.TIME,
                Intent.ACTION_TIMEZONE_CHANGED to SystemEvent.TIME,
                Intent.ACTION_USER_PRESENT to SystemEvent.LOCK_STATE,
                Intent.ACTION_SCREEN_ON to SystemEvent.LOCK_STATE,
                Intent.ACTION_USER_UNLOCKED to SystemEvent.USER_UNLOCKED,
            )

        /** The event for a broadcast [action], or `null` for anything else. Pure. */
        fun eventFor(action: String?): SystemEvent? = action?.let(ACTIONS::get)
    }
}

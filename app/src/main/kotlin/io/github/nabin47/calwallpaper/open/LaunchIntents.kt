package io.github.nabin47.calwallpaper.open

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract

/**
 * Turns [LaunchSpec]s into Intents and starts the first one that works. Call from an Activity on the main
 * thread (see [OpenTargetActivity]).
 *
 * Two passes, so a gap in package visibility can never leave a tap dead:
 * 1. Candidates some installed app can handle, checked with `resolveActivity`. That check needs the `<queries>`
 *    entries in the app manifest (Android 11+ package visibility).
 * 2. If none started, every candidate again, relying on `ActivityNotFoundException`. Starting an activity is not
 *    limited by package visibility, so this also covers a calendar or browser that the queries do not match.
 */
class LaunchIntents(
    private val context: Context,
) {
    /** Starts the first candidate that an installed app handles. Returns false if none could be started. */
    fun startFirst(candidates: List<LaunchSpec>): Boolean {
        val intents = candidates.mapNotNull(::intentFor)
        // resolveActivity ignores FLAG_ACTIVITY_REQUIRE_NON_BROWSER, so an app-only link may still fail to start;
        // tryStart then moves on to the next candidate.
        val resolvable = intents.filter { it.resolveActivity(context.packageManager) != null }
        return resolvable.any(::tryStart) || intents.any(::tryStart)
    }

    /**
     * The Intent for [spec], or `null` if this device cannot express it (an app-only web link before Android 11,
     * or this app without a launcher entry).
     */
    fun intentFor(spec: LaunchSpec): Intent? {
        val intent =
            when (spec) {
                is LaunchSpec.CalendarTime -> {
                    view(
                        CalendarContract.CONTENT_URI
                            .buildUpon()
                            .appendPath("time")
                            .appendPath(spec.epochMillis.toString())
                            .build(),
                    )
                }

                is LaunchSpec.CalendarEvent -> {
                    view(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, spec.eventId))
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, spec.beginMillis)
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, spec.endMillis)
                }

                is LaunchSpec.AppLink -> {
                    view(Uri.parse(spec.uri))
                }

                is LaunchSpec.Web -> {
                    webIntent(spec)
                }

                LaunchSpec.OwnApp -> {
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                }
            }
        // Opens in the target app's own task, not on top of the router's.
        return intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun view(uri: Uri): Intent = Intent(Intent.ACTION_VIEW, uri)

    /** A browsable view of the page; app-only links need Android 11's non-browser flag and are skipped before. */
    private fun webIntent(spec: LaunchSpec.Web): Intent? {
        val intent = view(Uri.parse(spec.url)).addCategory(Intent.CATEGORY_BROWSABLE)
        return when {
            !spec.appOnly -> intent
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> intent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
            else -> null
        }
    }

    private fun tryStart(intent: Intent): Boolean =
        try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            // A handler that requires a permission we do not hold; try the next candidate.
            false
        }
}

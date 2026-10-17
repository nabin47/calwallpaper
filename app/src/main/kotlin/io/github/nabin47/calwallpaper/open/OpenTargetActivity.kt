package io.github.nabin47.calwallpaper.open

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.OpenTargetLink
import kotlinx.datetime.TimeZone

/**
 * The deep-link router (CLAUDE.md §4): every tap on the wallpaper or a widget arrives here as an
 * [OpenTargetLink] in the Intent's data, and leaves as the best Intent an installed app can handle.
 *
 * Why an activity in between:
 * - **Background activity starts.** Widget taps reach it through the app's own `PendingIntent`, wallpaper taps
 *   while the wallpaper is visible. Once it is in the foreground, starting the target app is an ordinary
 *   foreground start. See ADR 0005.
 * - **Resolved at tap time.** The calendar or Todoist app may be installed or removed after a widget was drawn;
 *   the choice is made here, when the user taps, not when the widget was drawn.
 *
 * It has no UI (`Theme.NoDisplay`) and finishes in [onCreate], as that theme requires. Not exported, so other
 * apps cannot make it open links.
 */
class OpenTargetActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent?.dataString?.let(OpenTargetLink::decode)
        if (target != null) {
            val opened = LaunchIntents(this).startFirst(LaunchPlan.candidates(target, TimeZone.currentSystemDefault()))
            if (!opened) Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}

package io.github.nabin47.calwallpaper.quickadd

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat

/**
 * The "Quick add" Quick Settings tile. A tap opens [QuickAddActivity] and collapses the shade. On a locked phone
 * it asks to unlock first, because the task list is in credential-protected storage.
 * [TileServiceCompat] picks the right API (Android 14+ needs a PendingIntent).
 */
class QuickAddTileService : TileService() {
    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::open) else open()
    }

    private fun open() {
        val intent = Intent(this, QuickAddActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        TileServiceCompat.startActivityAndCollapse(
            this,
            PendingIntentActivityWrapper(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT, false),
        )
    }
}

package io.github.nabin47.calwallpaper.wallpaper.system

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager

/**
 * Point-in-time reads of device state the wallpaper depends on. Each call asks the system again; nothing is
 * cached, so callers always see the current value. Cheap enough to call before every frame.
 */
class DeviceState(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val keyguard = appContext.getSystemService(KeyguardManager::class.java)
    private val users = appContext.getSystemService(UserManager::class.java)

    /** True while the lock screen is showing (privacy mode hides titles then, CLAUDE.md §2). */
    fun isLocked(): Boolean = keyguard?.isKeyguardLocked ?: true

    /**
     * False before the first unlock after boot (Direct Boot): credential-protected storage, the calendar
     * provider and the local task database are unavailable until then.
     */
    fun isUserUnlocked(): Boolean = users?.isUserUnlocked ?: true

    /** True if `READ_CALENDAR` is granted. */
    fun hasCalendarPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
}

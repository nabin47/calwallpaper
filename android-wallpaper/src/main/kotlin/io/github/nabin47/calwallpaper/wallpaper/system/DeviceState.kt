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

    /**
     * True while the device is locked: privacy mode hides titles and the lock-screen placement is used
     * (CLAUDE.md §2).
     *
     * With a PIN, pattern or password this is [KeyguardManager.isDeviceLocked], which turns false as soon as the
     * user authenticates (including face unlock). The lock screen can stay up a moment longer than that, but
     * Android announces unlocking (`USER_PRESENT`) at authentication, so asking "is the keyguard showing" at
     * that point would wrongly keep the home screen in privacy mode until the next redraw. Without a secure lock,
     * nothing protects the phone anyway, so the keyguard simply showing counts as locked.
     */
    fun isLocked(): Boolean {
        val manager = keyguard ?: return true
        return if (manager.isDeviceSecure) manager.isDeviceLocked else manager.isKeyguardLocked
    }

    /**
     * False before the first unlock after boot (Direct Boot): credential-protected storage, the calendar
     * provider and the local task database are unavailable until then.
     */
    fun isUserUnlocked(): Boolean = users?.isUserUnlocked ?: true

    /** True if `READ_CALENDAR` is granted. */
    fun hasCalendarPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
}

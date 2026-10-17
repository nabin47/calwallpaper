package io.github.nabin47.calwallpaper.wallpaper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.nabin47.calwallpaper.core.OpenTargetLink
import io.github.nabin47.calwallpaper.render.Scene
import io.github.nabin47.calwallpaper.wallpaper.system.DeviceState

/**
 * Turns a launcher's tap on the wallpaper into an opened item (ADR 0005): hit-tests the drawn [Scene] and starts
 * the app's router with the target as an [OpenTargetLink].
 *
 * Starting an activity from the wallpaper is allowed while its window is visible: Android exempts apps with a
 * visible non-app window, naming live wallpapers. Main thread only.
 */
internal class WallpaperTaps(
    private val context: Context,
    private val deviceState: DeviceState,
) {
    /**
     * A tap at ([x], [y]) in screen coordinates on [scene], the frame on screen. Screen and surface coordinates
     * match because the wallpaper fills the screen. Ignored while the device is locked: the lock screen never
     * reports taps, but a launcher on top of a not-yet-dismissed keyguard might.
     */
    fun onTap(
        scene: Scene?,
        x: Float,
        y: Float,
    ) {
        if (deviceState.isLocked()) return
        val target = scene?.hitTest(x, y) ?: return
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(OpenTargetLink.encode(target)))
                .setClassName(context.packageName, OpenTargetLink.ROUTER_CLASS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Only possible in a build without the app module (tests); there is nothing to open.
        }
    }
}

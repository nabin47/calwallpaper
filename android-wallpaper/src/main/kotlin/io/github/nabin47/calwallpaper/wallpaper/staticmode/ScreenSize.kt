package io.github.nabin47.calwallpaper.wallpaper.staticmode

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.WindowManager

/** A pixel size. */
data class PixelSize(
    val width: Int,
    val height: Int,
)

/** The full display size in portrait, for rendering the static wallpaper image. */
object ScreenSize {
    /**
     * The largest window size on the device (the full screen including system bars), in portrait orientation:
     * wallpapers are set in portrait even if the phone is held sideways right now.
     */
    fun portrait(context: Context): PixelSize {
        val (w, h) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
                bounds.width() to bounds.height()
            } else {
                // Before Android 11, the system display metrics are the best non-deprecated source.
                val metrics = Resources.getSystem().displayMetrics
                metrics.widthPixels to metrics.heightPixels
            }
        return PixelSize(width = minOf(w, h), height = maxOf(w, h))
    }
}

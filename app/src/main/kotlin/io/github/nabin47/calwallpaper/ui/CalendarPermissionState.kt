package io.github.nabin47.calwallpaper.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * `READ_CALENDAR` state for Compose. The permission is asked in context, after a rationale (CLAUDE.md §7),
 * never at startup. When Android stops showing the dialog ("don't ask again"), [openAppSettings] is the way out.
 */
@Stable
class CalendarPermissionState internal constructor(
    private val context: Context,
) {
    /** True if the permission is granted. */
    var granted by mutableStateOf(hasCalendarPermission(context))
        private set

    /** True after a denial that Android will no longer show a dialog for. */
    var blocked by mutableStateOf(false)
        private set

    internal var request: () -> Unit = {}

    /** Shows the system permission dialog. */
    fun launchRequest() = request()

    /** Opens this app's page in system settings, where the user can grant the permission manually. */
    fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Re-reads the grant state, e.g. after returning from system settings. */
    fun refresh() {
        granted = hasCalendarPermission(context)
        if (granted) blocked = false
    }

    internal fun onDialogResult(result: Boolean) {
        granted = result
        // After a denial, "no rationale" means Android will not show the dialog again.
        blocked = !result &&
            context.findActivity()?.shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR) == false
    }
}

/**
 * Remembers [CalendarPermissionState]. [resumeKey] changes on every activity resume so the state is re-read after
 * the user visits system settings. [onResult] runs after the dialog closes.
 */
@Composable
fun rememberCalendarPermission(
    resumeKey: Int,
    onResult: (granted: Boolean) -> Unit = {},
): CalendarPermissionState {
    val context = LocalContext.current
    val state = remember(context) { CalendarPermissionState(context) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            state.onDialogResult(granted)
            onResult(granted)
        }
    state.request = { launcher.launch(Manifest.permission.READ_CALENDAR) }
    LaunchedEffect(resumeKey) { state.refresh() }
    return state
}

private fun hasCalendarPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

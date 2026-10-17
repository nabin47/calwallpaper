package io.github.nabin47.calwallpaper.quickadd

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.ui.CalWallpaperTheme
import io.github.nabin47.calwallpaper.wallpaper.WallpaperGraph
import io.github.nabin47.calwallpaper.wallpaper.staticmode.BackgroundRefresh
import kotlinx.coroutines.launch

/**
 * Quick-add to the local task list, opened from the Quick Settings tile and the launcher shortcut. A small
 * dialog: a title and an optional day, nothing more (CLAUDE.md §11: a minimal list, not an editor).
 *
 * The task is written in this process. Room's multi-instance invalidation tells the wallpaper process, and the
 * widgets and battery-saver image are redrawn here. If local tasks were turned off, adding one turns them on, so
 * the new task is visible.
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = WallpaperGraph.get(this)
        setContent {
            CalWallpaperTheme {
                QuickAddDialog(
                    onDismiss = ::finish,
                    onAdd = { title, due ->
                        graph.processScope.launch {
                            val result = graph.localTasks.add(title, due)
                            if (result is Outcome.Success) {
                                graph.settings.update { s ->
                                    s.copy(
                                        content =
                                            s.content.copy(
                                                enabledSources =
                                                    s.content.enabledSources + SourceId.LOCAL_TASKS,
                                            ),
                                    )
                                }
                                BackgroundRefresh.run(applicationContext)
                            }
                        }
                        Toast.makeText(this, R.string.quick_add_done, Toast.LENGTH_SHORT).show()
                        finish()
                    },
                )
            }
        }
    }
}

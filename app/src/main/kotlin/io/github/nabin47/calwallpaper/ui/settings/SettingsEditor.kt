package io.github.nabin47.calwallpaper.ui.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Edits settings with an immediate preview. [current] is what the screen and the live preview show: a draft
 * while a slider is being dragged, otherwise the stored settings. Storage is written once per change (on chip
 * tap, switch toggle or slider release), never per drag frame. Values are normalized before they are shown.
 *
 * Main thread only. [onCommitted] runs after each change is stored, with the settings before and after it
 * (e.g. to sync static-mode triggers when the mode changed).
 */
@Stable
class SettingsEditor(
    private val repository: SettingsRepository,
    private val scope: CoroutineScope,
    private val onCommitted: (before: Settings, after: Settings) -> Unit = { _, _ -> },
) {
    private var stored by mutableStateOf<Settings?>(null)
    private var draft by mutableStateOf<Settings?>(null)

    /** Settings to display, or `null` until the first value is loaded. */
    val current: Settings? get() = draft ?: stored

    /** Feeds a value read from storage. A draft that storage has caught up with is dropped. */
    fun onStored(settings: Settings) {
        stored = settings
        if (draft == settings) draft = null
    }

    /** Shows [transform] in the preview without saving (while dragging). */
    fun preview(transform: (Settings) -> Settings) {
        val base = current ?: return
        draft = transform(base).normalized()
    }

    /** Applies [transform] and saves it. Pass an identity transform to save the current draft. */
    fun commit(transform: (Settings) -> Settings = { it }) {
        val base = current ?: return
        val next = transform(base).normalized()
        draft = next
        scope.launch {
            repository.update { next }
            onCommitted(base, next)
        }
    }
}

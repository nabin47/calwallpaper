package io.github.nabin47.calwallpaper.ui.settings

import io.github.nabin47.calwallpaper.core.Outcome
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsEditorTest {
    private class FakeRepository : SettingsRepository {
        val state = MutableStateFlow(Settings())
        var writes = 0

        override val settings: Flow<Settings> = state

        override suspend fun update(transform: (Settings) -> Settings) {
            writes++
            state.value = transform(state.value)
        }

        override suspend fun exportJson(): String = ""

        override suspend fun importJson(json: String): Outcome<Settings> = Outcome.Failure("unused")
    }

    private val repository = FakeRepository()
    private val committed = mutableListOf<Pair<Settings, Settings>>()
    private val editor =
        SettingsEditor(repository, CoroutineScope(Dispatchers.Unconfined)) { before, after ->
            committed += before to after
        }

    @Test
    fun nothingIsShownBeforeSettingsLoad() {
        assertNull(editor.current)
        editor.commit { it.copy(privacyMode = it.privacyMode) }
        assertEquals(0, repository.writes)
    }

    @Test
    fun dragPreviewsWithoutWritingAndReleaseWritesOnce() {
        editor.onStored(Settings())
        repeat(5) { i -> editor.preview { it.copy(appearance = it.appearance.copy(textScale = 1f + i * 0.1f)) } }
        assertEquals(0, repository.writes, "dragging must not write storage")
        assertEquals(1.4f, editor.current?.appearance?.textScale ?: 0f, 0.0001f)

        editor.commit()
        assertEquals(1, repository.writes)
        assertEquals(1.4f, repository.state.value.appearance.textScale, 0.0001f)
        assertEquals(1, committed.size)
    }

    @Test
    fun valuesAreNormalizedAndDraftClearsWhenStorageCatchesUp() {
        editor.onStored(Settings())
        editor.commit { it.copy(appearance = it.appearance.copy(textScale = 99f)) }
        val stored = repository.state.value
        assertEquals(1.6f, stored.appearance.textScale, "out-of-range values are clamped before saving")
        editor.onStored(stored)
        assertEquals(stored, editor.current)
    }
}

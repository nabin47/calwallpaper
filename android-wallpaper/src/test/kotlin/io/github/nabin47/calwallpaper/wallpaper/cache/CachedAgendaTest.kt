package io.github.nabin47.calwallpaper.wallpaper.cache

import io.github.nabin47.calwallpaper.core.AgendaItem
import io.github.nabin47.calwallpaper.core.ItemKey
import io.github.nabin47.calwallpaper.core.ItemKind
import io.github.nabin47.calwallpaper.core.ItemTime
import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.core.SourceId
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CachedAgendaTest {
    private val items =
        listOf(
            item(
                "e1",
                ItemKind.EVENT,
                ItemTime.Timed(Instant.parse("2026-10-17T09:30:00Z"), Instant.parse("2026-10-17T10:00:00Z")),
            ),
            item("e2", ItemKind.EVENT, ItemTime.AllDay(LocalDate(2026, 10, 18), LocalDate(2026, 10, 20))),
            item("t1", ItemKind.TASK, ItemTime.Due(LocalDate(2026, 10, 17), LocalTime(11, 0)), completed = true),
            item("t2", ItemKind.TASK, ItemTime.Due(LocalDate(2026, 10, 19))),
        )

    private fun item(
        id: String,
        kind: ItemKind,
        time: ItemTime,
        completed: Boolean = false,
    ) = AgendaItem(
        key = ItemKey(if (kind == ItemKind.TASK) SourceId.LOCAL_TASKS else SourceId.CALENDAR_PROVIDER, id),
        kind = kind,
        title = "Secret $id",
        time = time,
        openTarget = OpenTarget.LocalItem(1),
        isCompleted = completed,
        groupId = "g",
        colorArgb = 0xFF336699.toInt(),
    )

    private fun roundTrip(privacy: PrivacyMode): List<AgendaItem> {
        val json = Json.encodeToString(CachedAgenda.serializer(), CachedAgenda.fromItems(items, privacy))
        return Json.decodeFromString(CachedAgenda.serializer(), json).toItems()
    }

    @Test
    fun everyTimeKindSurvivesARoundTrip() {
        val back = roundTrip(PrivacyMode.OFF)
        assertEquals(items.map { it.key }, back.map { it.key })
        assertEquals(items.map { it.time }, back.map { it.time })
        assertEquals(items.map { it.kind }, back.map { it.kind })
        assertEquals(items.map { it.isCompleted }, back.map { it.isCompleted })
        assertEquals(items.map { it.title }, back.map { it.title })
        assertEquals(items.map { it.colorArgb }, back.map { it.colorArgb })
    }

    @Test
    fun titlesAreNotStoredWhenPrivacyModeIsOn() {
        for (privacy in listOf(PrivacyMode.WHEN_LOCKED, PrivacyMode.ALWAYS)) {
            val json = Json.encodeToString(CachedAgenda.serializer(), CachedAgenda.fromItems(items, privacy))
            assertTrue("Secret" !in json, "$privacy must not write titles to device-protected storage")
            assertTrue(roundTrip(privacy).all { it.title.isEmpty() })
        }
    }

    @Test
    fun turningPrivacyOnStripsTitlesFromAnExistingSnapshot() {
        val withTitles = CachedAgenda.fromItems(items, PrivacyMode.OFF)
        assertEquals(withTitles, withTitles.restrictedTo(PrivacyMode.OFF))
        for (privacy in listOf(PrivacyMode.WHEN_LOCKED, PrivacyMode.ALWAYS)) {
            val restricted = withTitles.restrictedTo(privacy)
            assertTrue(restricted.items.all { it.title.isEmpty() }, "$privacy")
            assertEquals(withTitles.items.map { it.id }, restricted.items.map { it.id }, "only titles change")
        }
    }

    @Test
    fun malformedEntriesAreSkipped() {
        val good = CachedItem.from(items[0], keepTitle = true)
        val agenda =
            CachedAgenda(
                listOf(
                    good,
                    good.copy(source = "unknown_source"),
                    good.copy(start = "not a number"),
                    good.copy(timeKind = "future_kind"),
                ),
            )
        assertEquals(1, agenda.toItems().size)
    }

    @Test
    fun toStringNeverContainsTheTitle() {
        assertTrue("Secret" !in CachedItem.from(items[0], keepTitle = true).toString())
    }
}

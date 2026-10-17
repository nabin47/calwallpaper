package io.github.nabin47.calwallpaper.data.todoist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodoistMapperTest {
    @Test
    fun dueDatesInAllThreeDocumentedForms() {
        assertEquals(TodoistMapper.Due("2026-10-18", null, null), TodoistMapper.parseDue("2026-10-18"))
        assertEquals(
            TodoistMapper.Due("2026-10-18", "09:30:00", null),
            TodoistMapper.parseDue("2026-10-18T09:30:00"),
        )
        assertEquals(
            TodoistMapper.Due("2026-10-18", "09:30:00", "2026-10-18T09:30:00Z"),
            TodoistMapper.parseDue("2026-10-18T09:30:00Z"),
        )
        assertEquals(
            TodoistMapper.Due("2026-10-18", "09:30:00.000", null),
            TodoistMapper.parseDue("2026-10-18T09:30:00.000"),
        )
    }

    @Test
    fun malformedDueDatesAreRejected() {
        listOf("", "tomorrow", "2026-10", "2026-10-18T", "2026-10-18Tnoon", "2026-10-18 09:30").forEach {
            assertNull(TodoistMapper.parseDue(it), it)
        }
    }

    @Test
    fun onlyOpenDatedTasksAreKept() {
        val due = DueDto("2026-10-18")
        assertEquals(
            CachedTask("t1", "Title", "p1", "2026-10-18", order = 3),
            TodoistMapper.task(TaskDto("t1", "Title", "p1", due = due, childOrder = 3)),
        )
        assertNull(TodoistMapper.task(TaskDto("t2", "Done", "p1", checked = true, due = due)))
        assertNull(TodoistMapper.task(TaskDto("t3", "Deleted", "p1", isDeleted = true, due = due)))
        assertNull(TodoistMapper.task(TaskDto("t4", "Someday", "p1")))
        assertNull(TodoistMapper.task(TaskDto("t5", "Bad", "p1", due = DueDto("soon"))))
    }

    @Test
    fun projectsTakeTheirColorAndArchivedOnesAreDropped() {
        assertEquals(
            CachedProject("p1", "Work", 0xFFB8255F.toInt()),
            TodoistMapper.project(ProjectDto("p1", "Work", "berry_red")),
        )
        assertEquals(CachedProject("p2", "Misc", null), TodoistMapper.project(ProjectDto("p2", "Misc", "unknown")))
        assertNull(TodoistMapper.project(ProjectDto("p3", "Old", isArchived = true)))
        assertNull(TodoistMapper.project(ProjectDto("p4", "Gone", isDeleted = true)))
    }

    @Test
    fun everyColorIsOpaque() {
        assertEquals(0xFF808080.toInt(), TodoistColors.argb("charcoal"))
        assertNull(TodoistColors.argb(null))
    }
}

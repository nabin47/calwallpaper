package io.github.nabin47.calwallpaper.data.todoist

/**
 * Todoist's wire objects reduced to the offline cache. Pure.
 *
 * - **Tasks** need a due date. Completed, deleted and undated tasks are dropped: the wallpaper shows days.
 * - **Projects** that are archived or deleted are dropped.
 */
internal object TodoistMapper {
    /** The cached form of [task], or `null` if it is not shown. */
    fun task(task: TaskDto): CachedTask? {
        val due = task.due?.let { parseDue(it.date) }
        return if (task.checked || task.isDeleted || due == null) {
            null
        } else {
            CachedTask(
                id = task.id,
                title = task.content,
                projectId = task.projectId,
                dueDate = due.date,
                dueTime = due.time,
                dueInstant = due.instant,
                order = task.childOrder,
            )
        }
    }

    /** The cached form of [project], or `null` if it is archived or deleted. */
    fun project(project: ProjectDto): CachedProject? =
        if (project.isArchived || project.isDeleted) {
            null
        } else {
            CachedProject(project.id, project.name, TodoistColors.argb(project.color))
        }

    /** Splits Todoist's `due.date` into its parts; `null` if it is not one of the three documented forms. */
    fun parseDue(raw: String): Due? {
        val date = raw.substringBefore('T')
        val rest = if ('T' in raw) raw.substringAfter('T') else null
        return when {
            !DATE.matches(date) -> null
            rest == null -> Due(date, time = null, instant = null)
            rest.endsWith('Z') && TIME.matches(rest.dropLast(1)) -> Due(date, rest.dropLast(1), instant = raw)
            TIME.matches(rest) -> Due(date, rest, instant = null)
            else -> null
        }
    }

    /** The parts of a due date: local [date], optional local [time], and the [instant] for fixed-zone dates. */
    data class Due(
        val date: String,
        val time: String?,
        val instant: String?,
    )

    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    private val TIME = Regex("""\d{2}:\d{2}(:\d{2}(\.\d+)?)?""")
}

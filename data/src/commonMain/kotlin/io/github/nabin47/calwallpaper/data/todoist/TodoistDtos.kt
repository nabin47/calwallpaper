package io.github.nabin47.calwallpaper.data.todoist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Wire format of Todoist API v1 (https://api.todoist.com/api/v1/). Only the fields the app uses are declared;
// unknown fields are ignored, so additions on Todoist's side do not break parsing.

/** One page of a paginated list; [nextCursor] is `null` on the last page. */
@Serializable
internal data class PageDto<T>(
    val results: List<T> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

/** A task. `content` is its title: user content, never logged. */
@Serializable
internal data class TaskDto(
    val id: String,
    val content: String = "",
    @SerialName("project_id") val projectId: String = "",
    val checked: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    val due: DueDto? = null,
    @SerialName("child_order") val childOrder: Int = 0,
)

/**
 * A due date. [date] is `YYYY-MM-DD` (all day), `YYYY-MM-DDTHH:MM:SS` (floating local time) or the same with a
 * trailing `Z` (fixed to [timezone], given in UTC).
 */
@Serializable
internal data class DueDto(
    val date: String,
    val timezone: String? = null,
)

/** A project; [color] is a Todoist color name such as `berry_red`. */
@Serializable
internal data class ProjectDto(
    val id: String,
    val name: String = "",
    val color: String? = null,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

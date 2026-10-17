package io.github.nabin47.calwallpaper.core

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Encodes an [OpenTarget] as a URI string and back, so a tap can travel from the wallpaper process or a widget
 * to the app's router in an Intent's data. The data also keeps each widget item's `PendingIntent` distinct.
 *
 * Format: `calwallpaper://open/<kind>?<key>=<value>&…`, values percent-encoded as UTF-8. The format is internal to
 * the app (the router is not exported), but it must stay readable across an app update, because widgets keep
 * their old intents until they are redrawn. Pure and thread-safe.
 */
object OpenTargetLink {
    /**
     * Class name of the app's router activity. Senders (wallpaper, widgets) address it explicitly by this name,
     * with the link as data: the router is not exported, and Android 14+ delivers no implicit intents to
     * unexported components. A string, so those modules need not depend on the app module.
     */
    const val ROUTER_CLASS: String = "io.github.nabin47.calwallpaper.open.OpenTargetActivity"

    /** URI scheme of every link. */
    const val SCHEME: String = "calwallpaper"

    private const val PREFIX = "$SCHEME://open/"

    /** The link for [target]. */
    fun encode(target: OpenTarget): String {
        val (kind, params) =
            when (target) {
                is OpenTarget.CalendarDay -> {
                    "day" to listOf("date" to target.date.toString())
                }

                is OpenTarget.CalendarEvent -> {
                    "event" to
                        listOf(
                            "id" to target.eventId.toString(),
                            "begin" to target.begin.toEpochMilliseconds().toString(),
                            "end" to target.end.toEpochMilliseconds().toString(),
                        )
                }

                is OpenTarget.TodoistTask -> {
                    "todoist-task" to listOf("id" to target.taskId)
                }

                is OpenTarget.TodoistProject -> {
                    "todoist-project" to listOf("id" to target.projectId)
                }

                is OpenTarget.LocalItem -> {
                    "local" to listOf("id" to target.id.toString())
                }

                is OpenTarget.Url -> {
                    "url" to listOf("u" to target.url)
                }
            }
        return PREFIX + kind + "?" + params.joinToString("&") { (key, value) -> key + "=" + percentEncode(value) }
    }

    /** The target in [link], or `null` if it is not a valid link (wrong scheme, unknown kind, bad values). */
    fun decode(link: String): OpenTarget? {
        if (!link.startsWith(PREFIX)) return null
        val rest = link.removePrefix(PREFIX)
        val kind = rest.substringBefore('?')
        val params = parseQuery(rest.substringAfter('?', missingDelimiterValue = "")) ?: return null
        return runCatching { targetOf(kind, params) }.getOrNull()
    }

    private fun targetOf(
        kind: String,
        params: Map<String, String>,
    ): OpenTarget? =
        when (kind) {
            "day" -> {
                params["date"]?.let { OpenTarget.CalendarDay(LocalDate.parse(it)) }
            }

            "event" -> {
                OpenTarget.CalendarEvent(
                    eventId = params.getValue("id").toLong(),
                    begin = Instant.fromEpochMilliseconds(params.getValue("begin").toLong()),
                    end = Instant.fromEpochMilliseconds(params.getValue("end").toLong()),
                )
            }

            "todoist-task" -> {
                params["id"]?.takeIf { it.isNotEmpty() }?.let(OpenTarget::TodoistTask)
            }

            "todoist-project" -> {
                params["id"]?.takeIf { it.isNotEmpty() }?.let(OpenTarget::TodoistProject)
            }

            "local" -> {
                params["id"]?.toLong()?.let(OpenTarget::LocalItem)
            }

            "url" -> {
                params["u"]?.let(OpenTarget::Url)
            }

            else -> {
                null
            }
        }

    private fun parseQuery(query: String): Map<String, String>? {
        if (query.isEmpty()) return emptyMap()
        val pairs =
            query.split('&').map { part ->
                val key = part.substringBefore('=')
                val value = percentDecode(part.substringAfter('=', missingDelimiterValue = "")) ?: return null
                key to value
            }
        return pairs.toMap()
    }

    /** Percent-encodes everything except RFC 3986 unreserved characters. */
    private fun percentEncode(value: String): String =
        buildString {
            value.encodeToByteArray().forEach { byte ->
                val char = (byte.toInt() and BYTE_MASK).toChar()
                if (char.isUnreserved()) {
                    append(char)
                } else {
                    append('%')
                    append(HEX[(byte.toInt() shr NIBBLE_BITS) and NIBBLE_MASK])
                    append(HEX[byte.toInt() and NIBBLE_MASK])
                }
            }
        }

    /** The decoded string, or `null` for a malformed escape. */
    private fun percentDecode(value: String): String? {
        val bytes = ArrayList<Byte>(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '%') {
                if (index + 2 >= value.length) return null
                val high = HEX.indexOf(value[index + 1].uppercaseChar())
                val low = HEX.indexOf(value[index + 2].uppercaseChar())
                if (high < 0 || low < 0) return null
                bytes += ((high shl NIBBLE_BITS) or low).toByte()
                index += ESCAPE_LENGTH
            } else {
                bytes += char.code.toByte()
                index++
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    private fun Char.isUnreserved(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this in "-._~"

    private const val HEX = "0123456789ABCDEF"
    private const val BYTE_MASK = 0xFF
    private const val NIBBLE_MASK = 0x0F
    private const val NIBBLE_BITS = 4
    private const val ESCAPE_LENGTH = 3
}

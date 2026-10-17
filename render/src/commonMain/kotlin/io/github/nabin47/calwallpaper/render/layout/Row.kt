package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.domain.settings.VerticalPosition
import io.github.nabin47.calwallpaper.render.Bounds
import io.github.nabin47.calwallpaper.render.theme.alphaOf

/**
 * One horizontal band of a layout. [draw] emits its ops given the band's top edge. [itemCount] is how many
 * agenda items the row shows, so overflow can say how many were hidden.
 */
internal class Row(
    val height: Float,
    val itemCount: Int = 0,
    val draw: (top: Float) -> Unit,
)

/**
 * Places [rows] as one block inside the safe area at the user's vertical position, draws the panel behind it,
 * then the rows. Rows that do not fit are dropped and replaced by [overflow] (called with the number of hidden
 * items); content therefore never leaves the safe area.
 */
internal fun LayoutContext.placeBlock(
    rows: List<Row>,
    overflow: (hiddenItems: Int) -> Row,
) {
    val fitted = fitRows(rows, content.height - 2 * padding, overflow)
    val height = fitted.fold(2 * padding) { sum, row -> sum + row.height }
    val top =
        when (settings.layout.position) {
            VerticalPosition.TOP -> content.top
            VerticalPosition.CENTER -> content.top + (content.height - height) / 2
            VerticalPosition.BOTTOM -> content.bottom - height
        }.coerceAtLeast(content.top)
    if (alphaOf(palette.panel) > 0) {
        roundRect(Bounds(content.left, top, content.right, top + height), cornerRadius, palette.panel)
    }
    var y = top + padding
    fitted.forEach { row ->
        row.draw(y)
        y += row.height
    }
}

private fun fitRows(
    rows: List<Row>,
    available: Float,
    overflow: (Int) -> Row,
): List<Row> {
    val total = rows.fold(0f) { sum, row -> sum + row.height }
    if (total <= available) return rows
    val overflowHeight = overflow(0).height
    val kept = mutableListOf<Row>()
    var used = 0f
    for (row in rows) {
        if (used + row.height + overflowHeight > available) break
        kept += row
        used += row.height
    }
    val hidden = rows.drop(kept.size).sumOf { it.itemCount }
    if (hidden > 0) kept += overflow(hidden)
    return kept
}

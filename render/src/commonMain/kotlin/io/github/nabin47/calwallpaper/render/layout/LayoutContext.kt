package io.github.nabin47.calwallpaper.render.layout

import io.github.nabin47.calwallpaper.core.OpenTarget
import io.github.nabin47.calwallpaper.domain.settings.Placement
import io.github.nabin47.calwallpaper.domain.settings.PrivacyMode
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SpacingDensity
import io.github.nabin47.calwallpaper.render.Bounds
import io.github.nabin47.calwallpaper.render.DrawOp
import io.github.nabin47.calwallpaper.render.HitRegion
import io.github.nabin47.calwallpaper.render.RenderRequest
import io.github.nabin47.calwallpaper.render.Scene
import io.github.nabin47.calwallpaper.render.text.FontWeight
import io.github.nabin47.calwallpaper.render.text.TextMeasurer
import io.github.nabin47.calwallpaper.render.text.TextStyle
import io.github.nabin47.calwallpaper.render.theme.Palette
import io.github.nabin47.calwallpaper.render.theme.applyOpacity
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/**
 * Per-layout state: resolved metrics, colors and text styles, plus the growing list of draw ops and hit regions.
 * One instance per [io.github.nabin47.calwallpaper.render.LayoutEngine.layout] call; not thread-safe.
 */
internal class LayoutContext(
    val request: RenderRequest,
    val settings: Settings,
    val measurer: TextMeasurer,
) {
    val zone = request.zone
    val strings = request.strings
    val today: LocalDate = request.now.toLocalDateTime(request.zone).date
    val palette: Palette =
        Palette
            .resolve(settings.appearance, request.dynamicPalette)
            .applyOpacity(settings.appearance.panelOpacity, settings.appearance.textOpacity)
    val hideTitles: Boolean =
        when (settings.privacyMode) {
            PrivacyMode.OFF -> false
            PrivacyMode.WHEN_LOCKED -> request.isLocked
            PrivacyMode.ALWAYS -> true
        }

    private val density = request.screen.density
    private val textScale = request.screen.fontScale * settings.appearance.textScale
    private val spacing =
        when (settings.appearance.density) {
            SpacingDensity.COMPACT -> COMPACT_SPACING
            SpacingDensity.COMFORTABLE -> 1f
            SpacingDensity.SPACIOUS -> SPACIOUS_SPACING
        }

    /** Home or lock-screen placement, by the request's lock state. */
    val placement: Placement = settings.layout.placementFor(request.isLocked)

    /** The safe area: screen minus system insets, the user's safe zone and side margins. */
    val content: Bounds = safeArea()

    /** Padding inside the panel. */
    val padding: Float = dp(PANEL_PADDING_DP) * spacing

    /** Panel corner radius in pixels. */
    val cornerRadius: Float = dp(settings.appearance.cornerRadiusDp.toFloat())

    /** Vertical gap added to each list row. */
    val rowGap: Float = dp(ROW_GAP_DP) * spacing

    /** Gap between groups (days, entries). */
    val groupGap: Float = dp(GROUP_GAP_DP) * spacing

    /** Left edge of content inside the panel. */
    val innerLeft: Float get() = content.left + padding

    /** Right edge of content inside the panel. */
    val innerRight: Float get() = content.right - padding

    /** Width available for content inside the panel. */
    val innerWidth: Float get() = (innerRight - innerLeft).coerceAtLeast(0f)

    val header = style(HEADER_SP, FontWeight.BOLD)
    val body = style(BODY_SP, FontWeight.MEDIUM)
    val caption = style(CAPTION_SP)
    val small = style(SMALL_SP)
    val large = style(LARGE_SP, FontWeight.MEDIUM)

    private val ops = mutableListOf<DrawOp>()
    private val regions = mutableListOf<HitRegion>()

    /** Density-independent pixels to pixels. */
    fun dp(value: Float): Float = value * density

    /** Line height of [style]. */
    fun lineHeight(style: TextStyle): Float = measurer.metrics(style).lineHeight

    /** Distance from a line's top to its baseline. */
    fun ascent(style: TextStyle): Float = measurer.metrics(style).ascent

    /** Text width. */
    fun width(
        text: String,
        style: TextStyle,
    ): Float = measurer.width(text, style)

    /** Adds a text op; empty strings are skipped. */
    fun text(
        text: String,
        x: Float,
        baseline: Float,
        style: TextStyle,
        color: Int,
    ) {
        if (text.isNotEmpty()) ops += DrawOp.Text(text, x, baseline, style, color)
    }

    /** Adds a filled or outlined circle. */
    fun circle(
        centerX: Float,
        centerY: Float,
        radius: Float,
        color: Int,
        strokeWidth: Float = 0f,
    ) {
        ops += DrawOp.Circle(centerX, centerY, radius, color, strokeWidth)
    }

    /** Adds a rounded rectangle. */
    fun roundRect(
        bounds: Bounds,
        cornerRadius: Float,
        color: Int,
    ) {
        ops += DrawOp.RoundRect(bounds, cornerRadius, color)
    }

    /** Adds a tap target. Later regions win over earlier overlapping ones. */
    fun region(
        bounds: Bounds,
        target: OpenTarget,
    ) {
        regions += HitRegion(bounds, target)
    }

    /** The finished scene. */
    fun build(): Scene =
        Scene(
            widthPx = request.screen.widthPx,
            heightPx = request.screen.heightPx,
            ops = ops.toList(),
            hitRegions = regions.toList(),
            background = palette.background,
        )

    private fun style(
        sp: Float,
        weight: FontWeight = FontWeight.NORMAL,
    ) = TextStyle(sizePx = sp * density * textScale, weight = weight, font = settings.appearance.font)

    private fun safeArea(): Bounds {
        val screen = request.screen
        val insets = request.insets
        val layout = settings.layout
        val side = dp(layout.horizontalInsetDp.toFloat())
        val top = maxOf(insets.top, screen.heightPx * placement.safeZoneTop)
        val bottom = screen.heightPx - maxOf(insets.bottom, screen.heightPx * placement.safeZoneBottom)
        return Bounds(
            left = insets.left + side,
            top = top,
            right = (screen.widthPx - insets.right - side).coerceAtLeast(insets.left + side),
            bottom = bottom.coerceAtLeast(top),
        )
    }

    private companion object {
        const val COMPACT_SPACING = 0.75f
        const val SPACIOUS_SPACING = 1.3f
        const val PANEL_PADDING_DP = 16f
        const val ROW_GAP_DP = 8f
        const val GROUP_GAP_DP = 12f
        const val HEADER_SP = 14f
        const val BODY_SP = 15f
        const val CAPTION_SP = 13f
        const val SMALL_SP = 11f
        const val LARGE_SP = 22f
    }
}

package io.github.nabin47.calwallpaper.ui.home

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.domain.settings.LayoutPreset
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import io.github.nabin47.calwallpaper.render.theme.Palette
import io.github.nabin47.calwallpaper.ui.ScreenState
import io.github.nabin47.calwallpaper.ui.preview.LivePreview
import io.github.nabin47.calwallpaper.ui.preview.rememberPreviewRequest

/**
 * The four layouts as small live thumbnails, drawn by the same layout engine as the wallpaper with the user's own
 * colors and agenda. Tapping one applies it at once; the big preview above shows the result.
 */
@Composable
fun LayoutPicker(
    state: ScreenState,
    modifier: Modifier = Modifier,
) {
    val presets =
        listOf(
            LayoutPreset.AGENDA to R.string.preset_agenda,
            LayoutPreset.WEEK_STRIP to R.string.preset_week,
            LayoutPreset.MONTH_GRID to R.string.preset_month,
            LayoutPreset.NEXT_THREE to R.string.preset_next_three,
        )
    LazyRow(
        modifier = modifier.selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(presets, key = { it.first }) { (preset, label) ->
            val selected = state.settings.layout.preset == preset
            val variant =
                remember(
                    state.settings,
                    preset,
                ) { state.settings.copy(layout = state.settings.layout.copy(preset = preset)) }
            val request = rememberPreviewRequest(state.services, variant, state.preview, locked = false)
            OutlinedCard(
                shape = RoundedCornerShape(20.dp),
                border = selectionBorder(selected),
                modifier =
                    Modifier
                        .width(THUMB_WIDTH)
                        .selectable(selected = selected, role = Role.RadioButton) {
                            state.editor.commit { it.copy(layout = it.layout.copy(preset = preset)) }
                        },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LivePreview(
                        request = request,
                        description = stringResource(label),
                        modifier = Modifier.height(THUMB_HEIGHT).padding(8.dp),
                    )
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}

/**
 * The built-in color palettes, plus "Wallpaper colors" (Material You) on Android 12+, as swatches: the panel
 * color with the accent inside. Custom colors are on the Appearance page.
 */
@Composable
fun PalettePicker(
    state: ScreenState,
    modifier: Modifier = Modifier,
) {
    val appearance = state.settings.appearance
    val dynamic =
        remember(state.resumeCount) {
            state.services.graph.requestFactory
                .dynamicPalette()
        }
    val options =
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamic != null) {
                add(Swatch(null, R.string.palette_dynamic, dynamic))
            }
            add(Swatch(PalettePreset.CALM_DARK, R.string.palette_calm_dark, Palette.CALM_DARK))
            add(Swatch(PalettePreset.CALM_LIGHT, R.string.palette_calm_light, Palette.CALM_LIGHT))
            add(Swatch(PalettePreset.PAPER, R.string.palette_paper, Palette.PAPER))
            add(Swatch(PalettePreset.MIDNIGHT, R.string.palette_midnight, Palette.MIDNIGHT))
        }
    LazyRow(
        modifier = modifier.selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(options, key = { it.label }) { swatch ->
            val selected =
                if (swatch.preset == null) {
                    appearance.useDynamicColor
                } else {
                    !appearance.useDynamicColor && appearance.palette == swatch.preset
                }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier.width(SWATCH_ITEM_WIDTH).selectable(selected = selected, role = Role.RadioButton) {
                        state.editor.commit { s ->
                            val a = s.appearance
                            s.copy(
                                appearance =
                                    if (swatch.preset == null) {
                                        a.copy(useDynamicColor = true)
                                    } else {
                                        a.copy(useDynamicColor = false, palette = swatch.preset)
                                    },
                            )
                        }
                    },
            ) {
                SwatchCircle(swatch.palette, selected)
                Text(
                    text = stringResource(swatch.label),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private data class Swatch(
    val preset: PalettePreset?,
    val label: Int,
    val palette: Palette,
)

@Composable
private fun SwatchCircle(
    palette: Palette,
    selected: Boolean,
) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier =
            Modifier
                .size(SWATCH_SIZE)
                .border(BorderStroke(if (selected) 3.dp else 1.dp, ring), CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(palette.panel)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color(palette.accent))
        } else {
            Box(Modifier.size(14.dp).clip(CircleShape).background(Color(palette.accent)))
        }
    }
}

@Composable
private fun selectionBorder(selected: Boolean): BorderStroke =
    if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

private val THUMB_WIDTH = 112.dp
private val THUMB_HEIGHT = 200.dp
private val SWATCH_SIZE = 56.dp
private val SWATCH_ITEM_WIDTH = 72.dp

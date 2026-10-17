package io.github.nabin47.calwallpaper.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nabin47.calwallpaper.R
import io.github.nabin47.calwallpaper.domain.settings.AppearanceSettings
import io.github.nabin47.calwallpaper.domain.settings.ArgbColor
import io.github.nabin47.calwallpaper.domain.settings.CustomColors
import io.github.nabin47.calwallpaper.domain.settings.FontChoice
import io.github.nabin47.calwallpaper.domain.settings.PalettePreset
import io.github.nabin47.calwallpaper.domain.settings.Settings
import io.github.nabin47.calwallpaper.domain.settings.SpacingDensity
import io.github.nabin47.calwallpaper.render.theme.Palette
import io.github.nabin47.calwallpaper.ui.components.Choice
import io.github.nabin47.calwallpaper.ui.components.ChoiceSetting
import io.github.nabin47.calwallpaper.ui.components.SettingHint
import io.github.nabin47.calwallpaper.ui.components.SliderSetting
import io.github.nabin47.calwallpaper.ui.components.SwitchSetting
import kotlin.math.roundToInt

/** Colors, font, text size, opacities, corner radius and spacing (CLAUDE.md §6), with a contrast warning. */
@Composable
fun AppearanceSection(
    settings: Settings,
    editor: SettingsEditor,
    dynamicPalette: Palette?,
    modifier: Modifier = Modifier,
) {
    val appearance = settings.appearance

    fun edit(transform: (AppearanceSettings) -> AppearanceSettings): (Settings) -> Settings =
        { it.copy(appearance = transform(it.appearance)) }

    Column(modifier) {
        ChoiceSetting(
            label = stringResource(R.string.palette_label),
            choices =
                listOf(
                    Choice(PalettePreset.CALM_DARK, stringResource(R.string.palette_calm_dark)),
                    Choice(PalettePreset.CALM_LIGHT, stringResource(R.string.palette_calm_light)),
                    Choice(PalettePreset.PAPER, stringResource(R.string.palette_paper)),
                    Choice(PalettePreset.MIDNIGHT, stringResource(R.string.palette_midnight)),
                    Choice(PalettePreset.CUSTOM, stringResource(R.string.palette_custom)),
                ),
            selected = appearance.palette,
            onSelect = { preset ->
                editor.commit(
                    edit {
                        it.copy(palette = preset, customColors = it.customColors ?: defaultCustomColors())
                    },
                )
            },
        )
        if (appearance.palette == PalettePreset.CUSTOM) {
            CustomColorsEditor(appearance.customColors ?: defaultCustomColors()) { colors ->
                editor.commit(edit { it.copy(customColors = colors) })
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SwitchSetting(
                label = stringResource(R.string.dynamic_color),
                checked = appearance.useDynamicColor,
                onCheckedChange = { on -> editor.commit(edit { it.copy(useDynamicColor = on) }) },
            )
        }
        ChoiceSetting(
            label = stringResource(R.string.font_label),
            choices =
                listOf(
                    Choice(FontChoice.SYSTEM, stringResource(R.string.font_system)),
                    Choice(FontChoice.SANS, stringResource(R.string.font_sans)),
                    Choice(FontChoice.SERIF, stringResource(R.string.font_serif)),
                    Choice(FontChoice.MONO, stringResource(R.string.font_mono)),
                ),
            selected = appearance.font,
            onSelect = { font -> editor.commit(edit { it.copy(font = font) }) },
        )
        SliderSetting(
            label = stringResource(R.string.text_size),
            hint = stringResource(R.string.text_size_hint),
            valueLabel = stringResource(R.string.scale_value, appearance.textScale),
            value = appearance.textScale,
            range = AppearanceSettings.TEXT_SCALE,
            onPreview = { v -> editor.preview(edit { it.copy(textScale = v) }) },
            onCommit = { editor.commit() },
        )
        SliderSetting(
            label = stringResource(R.string.text_opacity),
            valueLabel = stringResource(R.string.percent_value, (appearance.textOpacity * 100).roundToInt()),
            value = appearance.textOpacity,
            range = AppearanceSettings.UNIT,
            onPreview = { v -> editor.preview(edit { it.copy(textOpacity = v) }) },
            onCommit = { editor.commit() },
        )
        SliderSetting(
            label = stringResource(R.string.panel_opacity),
            hint = stringResource(R.string.panel_opacity_hint),
            valueLabel = stringResource(R.string.percent_value, (appearance.panelOpacity * 100).roundToInt()),
            value = appearance.panelOpacity,
            range = AppearanceSettings.UNIT,
            onPreview = { v -> editor.preview(edit { it.copy(panelOpacity = v) }) },
            onCommit = { editor.commit() },
        )
        val contrast = Contrast.ratio(appearance, dynamicPalette)
        if (contrast < Contrast.MINIMUM_RATIO) {
            SettingHint(stringResource(R.string.contrast_warning, contrast), isWarning = true)
        }
        SliderSetting(
            label = stringResource(R.string.corner_radius),
            valueLabel = stringResource(R.string.dp_value, appearance.cornerRadiusDp),
            value = appearance.cornerRadiusDp.toFloat(),
            range = AppearanceSettings.CORNER_RADIUS_DP.toFloatRange(),
            onPreview = { v -> editor.preview(edit { it.copy(cornerRadiusDp = v.roundToInt()) }) },
            onCommit = { editor.commit() },
        )
        ChoiceSetting(
            label = stringResource(R.string.spacing_label),
            choices =
                listOf(
                    Choice(SpacingDensity.COMPACT, stringResource(R.string.spacing_compact)),
                    Choice(SpacingDensity.COMFORTABLE, stringResource(R.string.spacing_comfortable)),
                    Choice(SpacingDensity.SPACIOUS, stringResource(R.string.spacing_spacious)),
                ),
            selected = appearance.density,
            onSelect = { density -> editor.commit(edit { it.copy(density = density) }) },
        )
    }
}

/** Swatch rows for each custom color role. */
@Composable
private fun CustomColorsEditor(
    colors: CustomColors,
    onChange: (CustomColors) -> Unit,
) {
    SwatchRow(stringResource(R.string.custom_background), colors.background ?: ArgbColor(BACKGROUNDS[0]), BACKGROUNDS) {
        onChange(colors.copy(background = it))
    }
    SwatchRow(stringResource(R.string.custom_panel), colors.panel, PANELS) { onChange(colors.copy(panel = it)) }
    SwatchRow(stringResource(R.string.custom_text), colors.text, TEXTS) { onChange(colors.copy(text = it)) }
    SwatchRow(stringResource(R.string.custom_accent), colors.accent, ACCENTS) { onChange(colors.copy(accent = it)) }
}

@Composable
private fun SwatchRow(
    label: String,
    selected: ArgbColor,
    options: List<Int>,
    onSelect: (ArgbColor) -> Unit,
) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val colors = MaterialTheme.colorScheme
            options.forEach { argb ->
                val color = ArgbColor(argb)
                val isSelected = color == selected
                val description = stringResource(R.string.color_swatch, color.toHex())
                Box(
                    modifier =
                        Modifier
                            .size(SWATCH_SIZE)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) colors.primary else colors.outline,
                                shape = CircleShape,
                            ).clickable(role = Role.RadioButton) { onSelect(color) }
                            .semantics {
                                contentDescription = description
                                this.selected = isSelected
                            },
                )
            }
        }
    }
}

/** Swatch diameter: Material's 48dp minimum touch target. */
private val SWATCH_SIZE = 48.dp

/** Starting point when the user first picks "Custom": the calm dark palette. */
private fun defaultCustomColors(): CustomColors =
    CustomColors(
        panel = ArgbColor(Palette.CALM_DARK.panel),
        text = ArgbColor(Palette.CALM_DARK.text),
        accent = ArgbColor(Palette.CALM_DARK.accent),
        background = ArgbColor(Palette.CALM_DARK.background),
    )

private val BACKGROUNDS =
    listOf(
        0xFF2B3440,
        0xFF1E293B,
        0xFF0F172A,
        0xFF3F2A37,
        0xFF23362B,
        0xFF000000,
        0xFFC9D6E3,
        0xFFE6DCCB,
        0xFFF1E4E8,
        0xFFDDE9DD,
        0xFFFFFFFF,
    ).map { it.toInt() }

private val PANELS =
    listOf(0xFF121417, 0xFF1F2937, 0xFF000000, 0xFF2D1B24, 0xFFF7F8FA, 0xFFF4EFE6, 0xFFFFFFFF).map { it.toInt() }

private val TEXTS =
    listOf(0xFFF2F3F5, 0xFFFFFFFF, 0xFFE6E9EF, 0xFF1B1F24, 0xFF2B2620, 0xFF000000).map { it.toInt() }

private val ACCENTS =
    listOf(
        0xFF8AB4F8,
        0xFF1F5FBF,
        0xFF7DD3FC,
        0xFF34A853,
        0xFFFBBC04,
        0xFFEA4335,
        0xFF934A27,
        0xFFC084FC,
    ).map { it.toInt() }

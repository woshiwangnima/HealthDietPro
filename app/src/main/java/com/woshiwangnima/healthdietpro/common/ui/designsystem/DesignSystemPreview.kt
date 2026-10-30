package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import java.util.Locale

private val PREVIEW_INNER_INSET = 12.dp
private val PREVIEW_SWATCH = 40.dp
private val PREVIEW_BAR_MAX = 140.dp

@Composable
fun DesignSystemPreviewSection() {
    var darkTheme by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacingDefault.s3)) {
        Text(
            text = stringResource(R.string.design_preview_section_title),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.design_preview_section_hint),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ThemeToggle(
            darkTheme = darkTheme,
            onToggle = { darkTheme = it },
        )
        AppTheme(darkTheme = darkTheme) {
            val tokens = LocalAppTokens.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(tokens.radii.container))
                    .background(tokens.colors.bg)
                    .border(
                        width = tokens.radii.stroke,
                        color = tokens.colors.border,
                        shape = RoundedCornerShape(tokens.radii.container),
                    )
                    .padding(PREVIEW_INNER_INSET),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s5),
            ) {
                ColorTokenBlock(tokens)
                FoodKindTokenBlock(tokens)
                SpacingTokenBlock(tokens)
                TypeTokenBlock(tokens)
                SurfaceTokenBlock(tokens)
                MotionTokenBlock(tokens)
                ButtonStateBlock(tokens)
            }
        }
    }
}

@Composable
private fun ThemeToggle(
    darkTheme: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val tokens = DarkAppTokens
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
        SegmentChip(
            text = stringResource(R.string.design_preview_theme_dark),
            selected = darkTheme,
            tokens = tokens,
            onClick = { onToggle(true) },
        )
        SegmentChip(
            text = stringResource(R.string.design_preview_theme_light),
            selected = !darkTheme,
            tokens = tokens,
            onClick = { onToggle(false) },
        )
    }
}

@Composable
private fun SegmentChip(
    text: String,
    selected: Boolean,
    tokens: AppTokens,
    onClick: () -> Unit,
) {
    val container = if (selected) tokens.colors.primary else tokens.colors.panel
    val content = if (selected) tokens.colors.onPrimary else tokens.colors.text
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(tokens.radii.container))
            .background(container)
            .border(
                width = if (selected) 0.dp else tokens.radii.highlight,
                color = if (selected) Color.Transparent else tokens.colors.borderStrong,
                shape = RoundedCornerShape(tokens.radii.container),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.s4, vertical = tokens.spacing.s2),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = tokens.type.bodyS, color = content)
    }
}

private data class SwatchSpec(
    val name: String,
    val swatch: Color,
    val measuredAgainst: Color,
    val minimum: Double,
)

@Composable
private fun ColorTokenBlock(tokens: AppTokens) {
    val colors = tokens.colors
    val specs = listOf(
        SwatchSpec("color.bg", colors.bg, colors.text, 7.0),
        SwatchSpec("color.panel", colors.panel, colors.text, 7.0),
        SwatchSpec("color.panelRaised", colors.panelRaised, colors.text, 4.5),
        SwatchSpec("color.border", colors.border, colors.bg, 3.0),
        SwatchSpec("color.borderStrong", colors.borderStrong, colors.bg, 3.0),
        SwatchSpec("color.text", colors.text, colors.bg, 7.0),
        SwatchSpec("color.textMuted", colors.textMuted, colors.bg, 4.5),
        SwatchSpec("color.primary", colors.primary, colors.bg, 4.5),
        SwatchSpec("color.success", colors.success, colors.bg, 4.5),
        SwatchSpec("color.warning", colors.warning, colors.bg, 4.5),
        SwatchSpec("color.danger", colors.danger, colors.bg, 4.5),
        SwatchSpec("color.info", colors.info, colors.bg, 4.5),
        SwatchSpec("onPrimary/primary", colors.onPrimary, colors.primary, 4.5),
        SwatchSpec("onDangerFill/dangerFill", colors.onDangerFill, colors.dangerFill, 4.5),
        SwatchSpec("color.disabled", colors.disabled, colors.panel, 0.0),
    )
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
        BlockTitle(stringResource(R.string.design_preview_colors_title), tokens)
        specs.forEach { spec ->
            SwatchRow(
                name = spec.name,
                swatch = spec.swatch,
                ratioText = ratioText(spec.swatch, spec.measuredAgainst),
                passed = spec.minimum <= 0.0 || contrastRatioOf(spec.swatch, spec.measuredAgainst) >= spec.minimum,
                showVerdict = spec.minimum > 0.0,
                minimum = spec.minimum,
                tokens = tokens,
            )
        }
    }
}

@Composable
private fun FoodKindTokenBlock(tokens: AppTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
        BlockTitle(stringResource(R.string.design_preview_food_kind_title), tokens)
        KindRow(
            label = stringResource(R.string.design_preview_food_kind_current),
            colors = AppFoodKindCurrent,
            tokens = tokens,
        )
        KindRow(
            label = stringResource(R.string.design_preview_food_kind_suggested),
            colors = AppFoodKindAccessible,
            tokens = tokens,
        )
    }
}

@Composable
private fun KindRow(
    label: String,
    colors: AppFoodKindColors,
    tokens: AppTokens,
) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
        Text(text = label, style = tokens.type.caption, color = tokens.colors.textMuted)
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KindChip(stringResource(R.string.nutrition_kind_ingredient), colors.ingredient, colors.onKind, tokens)
            KindChip(stringResource(R.string.nutrition_kind_food), colors.preparedFood, colors.onKind, tokens)
            KindChip(stringResource(R.string.nutrition_kind_dish), colors.dish, colors.onKind, tokens)
        }
    }
}

@Composable
private fun KindChip(
    text: String,
    container: Color,
    content: Color,
    tokens: AppTokens,
) {
    val ratio = contrastRatioOf(content, container)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(tokens.radii.label))
                .background(container)
                .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1),
        ) {
            Text(text = text, style = tokens.type.bodyS, color = content)
        }
        Text(
            text = formatRatio(ratio),
            style = tokens.type.caption,
            color = if (ratio >= 4.5) tokens.colors.success else tokens.colors.danger,
        )
    }
}

@Composable
private fun SpacingTokenBlock(tokens: AppTokens) {
    val entries = listOf(
        "space.1" to tokens.spacing.s1,
        "space.2" to tokens.spacing.s2,
        "space.3" to tokens.spacing.s3,
        "space.4" to tokens.spacing.s4,
        "space.5" to tokens.spacing.s5,
        "space.6" to tokens.spacing.s6,
        "space.7" to tokens.spacing.s7,
        "space.8" to tokens.spacing.s8,
        "page" to tokens.spacing.page,
        "rowMinHeight" to tokens.spacing.rowMinHeight,
        "hitMin" to tokens.spacing.hitMin,
    )
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
        BlockTitle(stringResource(R.string.design_preview_spacing_title), tokens)
        entries.forEach { (name, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = monospace(tokens.type.caption),
                    color = tokens.colors.textMuted,
                    modifier = Modifier.width(96.dp),
                )
                Box(
                    modifier = Modifier
                        .width(value.coerceAtMost(PREVIEW_BAR_MAX))
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tokens.colors.primary),
                )
                Text(
                    text = " ${value.value.toInt()}dp",
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun TypeTokenBlock(tokens: AppTokens) {
    val sample = stringResource(R.string.design_preview_type_sample)
    val rows = listOf(
        "caption 12/16" to tokens.type.caption,
        "bodyS 14/20" to tokens.type.bodyS,
        "body 16/24" to tokens.type.body,
        "numeric 16/24 mono" to tokens.type.numeric,
        "titleS 20/28" to tokens.type.titleS,
        "title 24/32" to tokens.type.title,
        "titleL 32/40" to tokens.type.titleL,
        "display 40/48" to tokens.type.display,
    )
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
        BlockTitle(stringResource(R.string.design_preview_type_title), tokens)
        rows.forEach { (name, style) ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = name, style = monospace(tokens.type.caption), color = tokens.colors.textMuted)
                Text(text = sample, style = style, color = tokens.colors.text)
            }
        }
    }
}

@Composable
private fun SurfaceTokenBlock(tokens: AppTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
        BlockTitle(stringResource(R.string.design_preview_surfaces_title), tokens)
        SurfaceSample(
            label = stringResource(R.string.design_preview_surface_panel),
            container = tokens.colors.panel,
            tokens = tokens,
        )
        SurfaceSample(
            label = stringResource(R.string.design_preview_surface_raised),
            container = tokens.colors.panelRaised,
            tokens = tokens,
        )
    }
}

@Composable
private fun SurfaceSample(
    label: String,
    container: Color,
    tokens: AppTokens,
) {
    val shape = RoundedCornerShape(tokens.radii.container)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
        Text(text = label, style = monospace(tokens.type.caption), color = tokens.colors.textMuted)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(container)
                .border(tokens.radii.stroke, tokens.colors.border, shape)
                .padding(tokens.spacing.s4),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tokens.radii.highlight)
                    .background(tokens.colors.borderStrong),
            )
        }
    }
}

@Composable
private fun MotionTokenBlock(tokens: AppTokens) {
    val motion = tokens.motion
    val entries = listOf(
        "motion.instant" to motion.instantMs,
        "motion.fast" to motion.fastMs,
        "motion.exit" to motion.exitMs,
        "motion.base" to motion.baseMs,
        "motion.sheet" to motion.sheetMs,
        "motion.slow" to motion.slowMs,
        "motion.emphasis" to motion.emphasisMs,
        "motion.cinematic" to motion.cinematicMs,
    )
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
        BlockTitle(stringResource(R.string.design_preview_motion_title), tokens)
        entries.forEach { (name, millis) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = monospace(tokens.type.caption),
                    color = tokens.colors.textMuted,
                    modifier = Modifier.width(104.dp),
                )
                Box(
                    modifier = Modifier
                        .width(PREVIEW_BAR_MAX * (millis.toFloat() / motion.cinematicMs))
                        .height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tokens.colors.info),
                )
                Text(
                    text = " ${millis}ms",
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
            }
        }
    }
}

private enum class PreviewButtonVariant { Primary, Secondary, Ghost, Danger }

private enum class PreviewButtonState { Default, Pressed, Selected, Disabled, Loading }

@Composable
private fun ButtonStateBlock(tokens: AppTokens) {
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3)) {
        BlockTitle(stringResource(R.string.design_preview_buttons_title), tokens)
        PreviewButtonVariant.entries.forEach { variant ->
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
                Text(
                    text = variantLabel(variant),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PreviewButtonState.entries.forEach { state ->
                        ButtonStateChip(variant, state, tokens, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun variantLabel(variant: PreviewButtonVariant): String = when (variant) {
    PreviewButtonVariant.Primary -> stringResource(R.string.design_preview_variant_primary)
    PreviewButtonVariant.Secondary -> stringResource(R.string.design_preview_variant_secondary)
    PreviewButtonVariant.Ghost -> stringResource(R.string.design_preview_variant_ghost)
    PreviewButtonVariant.Danger -> stringResource(R.string.design_preview_variant_danger)
}

@Composable
private fun stateLabel(state: PreviewButtonState): String = when (state) {
    PreviewButtonState.Default -> stringResource(R.string.design_preview_state_default)
    PreviewButtonState.Pressed -> stringResource(R.string.design_preview_state_pressed)
    PreviewButtonState.Selected -> stringResource(R.string.design_preview_state_selected)
    PreviewButtonState.Disabled -> stringResource(R.string.design_preview_state_disabled)
    PreviewButtonState.Loading -> stringResource(R.string.design_preview_state_loading)
}

@Composable
private fun ButtonStateChip(
    variant: PreviewButtonVariant,
    state: PreviewButtonState,
    tokens: AppTokens,
    modifier: Modifier = Modifier,
) {
    val colors = tokens.colors
    val shape = RoundedCornerShape(tokens.radii.container)
    val disabledFill = colors.disabled.copy(alpha = 0.35f)
    val container = when (variant) {
        PreviewButtonVariant.Primary -> when (state) {
            PreviewButtonState.Disabled -> disabledFill
            PreviewButtonState.Pressed -> lerp(colors.primary, Color.Black, 0.18f)
            PreviewButtonState.Selected -> colors.primaryContainer
            else -> colors.primary
        }
        PreviewButtonVariant.Danger -> when (state) {
            PreviewButtonState.Disabled -> disabledFill
            PreviewButtonState.Pressed -> lerp(colors.dangerFill, Color.Black, 0.18f)
            PreviewButtonState.Selected -> lerp(colors.dangerFill, Color.Black, 0.28f)
            else -> colors.dangerFill
        }
        PreviewButtonVariant.Secondary -> when (state) {
            PreviewButtonState.Disabled -> disabledFill
            PreviewButtonState.Pressed -> lerp(colors.panelRaised, Color.Black, 0.18f)
            PreviewButtonState.Selected -> colors.primaryContainer
            else -> colors.panelRaised
        }
        PreviewButtonVariant.Ghost -> when (state) {
            PreviewButtonState.Disabled -> disabledFill
            PreviewButtonState.Pressed -> lerp(colors.panel, Color.Black, 0.20f)
            PreviewButtonState.Selected -> colors.panelRaised
            else -> Color.Transparent
        }
    }
    val content = when (variant) {
        PreviewButtonVariant.Primary -> when (state) {
            PreviewButtonState.Disabled -> colors.disabled
            PreviewButtonState.Selected -> colors.onPrimaryContainer
            else -> colors.onPrimary
        }
        PreviewButtonVariant.Danger -> if (state == PreviewButtonState.Disabled) colors.disabled else colors.onDangerFill
        PreviewButtonVariant.Secondary -> if (state == PreviewButtonState.Disabled) colors.disabled else colors.text
        PreviewButtonVariant.Ghost -> when (state) {
            PreviewButtonState.Disabled -> colors.disabled
            PreviewButtonState.Selected -> colors.primary
            else -> colors.text
        }
    }
    val stroke = when {
        state == PreviewButtonState.Selected -> colors.primary
        variant == PreviewButtonVariant.Secondary && state != PreviewButtonState.Disabled -> colors.borderStrong
        variant == PreviewButtonVariant.Ghost && state == PreviewButtonState.Default -> colors.border
        else -> Color.Transparent
    }
    val label = if (state == PreviewButtonState.Loading) "···" else stateLabel(state)

    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(container)
            .border(
                width = if (stroke == Color.Transparent) 0.dp else tokens.radii.stroke,
                color = stroke,
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = tokens.type.caption,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun BlockTitle(
    text: String,
    tokens: AppTokens,
) {
    Text(
        text = text,
        style = tokens.type.titleS,
        color = tokens.colors.text,
    )
}

@Composable
private fun SwatchRow(
    name: String,
    swatch: Color,
    ratioText: String,
    passed: Boolean,
    showVerdict: Boolean,
    minimum: Double,
    tokens: AppTokens,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
    ) {
        Box(
            modifier = Modifier
                .size(PREVIEW_SWATCH)
                .clip(RoundedCornerShape(tokens.radii.container))
                .background(swatch)
                .border(tokens.radii.highlight, tokens.colors.borderStrong, RoundedCornerShape(tokens.radii.container)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = monospace(tokens.type.bodyS), color = tokens.colors.text)
            Text(text = hexOf(swatch), style = monospace(tokens.type.caption), color = tokens.colors.textMuted)
        }
        if (showVerdict) {
            VerdictBadge(
                text = ratioText,
                passed = passed,
                required = minimum,
                tokens = tokens,
            )
        } else {
            Text(text = ratioText, style = tokens.type.caption, color = tokens.colors.textMuted)
        }
    }
}

@Composable
private fun VerdictBadge(
    text: String,
    passed: Boolean,
    required: Double,
    tokens: AppTokens,
) {
    val color = if (passed) tokens.colors.success else tokens.colors.danger
    Column(horizontalAlignment = Alignment.End) {
        Text(text = text, style = monospace(tokens.type.caption), color = color)
        Text(
            text = stringResource(R.string.design_preview_need_ratio, required),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
    }
}

private fun monospace(style: TextStyle): TextStyle = style.copy(fontFamily = FontFamily.Monospace)

private fun ratioText(foreground: Color, background: Color): String =
    formatRatio(contrastRatioOf(foreground, background))

private fun formatRatio(ratio: Double): String =
    String.format(Locale.US, "%.2f:1", ratio)

private fun hexOf(color: Color): String =
    "#" + String.format(Locale.US, "%06X", color.toArgb() and 0xFFFFFF)

package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class SingleChoiceSegmentedOption(
    val id: String,
    @param:StringRes val labelRes: Int = 0,
    val labelArgs: List<Any> = emptyList(),
    val label: String? = null,
    val enabled: Boolean = true,
)

/** A compact single-choice control for filters and data views, distinct from page navigation tabs. */
@Composable
fun SingleChoiceSegmentedSelector(
    options: List<SingleChoiceSegmentedOption>,
    selectedId: String,
    onOptionSelected: (SingleChoiceSegmentedOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return

    val shape = RoundedCornerShape(8.dp)
    val selectedIndex = options.indexOfFirst { it.id == selectedId }
    Surface(
        modifier = modifier.fillMaxWidth().height(40.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (selectedIndex >= 0) {
                MagneticFluidSliderIndicator(
                    selectedIndex = selectedIndex,
                    itemCount = options.size,
                    color = navigationIndicatorColor(),
                    cornerRadius = 6.dp,
                    horizontalInset = 3.dp,
                    verticalInset = 3.dp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(Modifier.fillMaxSize()) {
                options.forEach { option ->
                    SingleChoiceSegmentedSelectorItem(
                        option = option,
                        selected = option.id == selectedId,
                        rippleShape = RoundedCornerShape(6.dp),
                        onClick = { onOptionSelected(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.SingleChoiceSegmentedSelectorItem(
    option: SingleChoiceSegmentedOption,
    selected: Boolean,
    rippleShape: Shape,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            !option.enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "singleChoiceSegmentedSelectorTextColor",
    )
    val text = when {
        option.label != null -> option.label
        option.labelRes != 0 -> stringResource(option.labelRes, *option.labelArgs.toTypedArray())
        else -> ""
    }
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(rippleShape)
            .centerExpandingRipple(interactionSource, contentColor)
            .selectable(
                selected = selected,
                enabled = option.enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        TextOverflowText(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

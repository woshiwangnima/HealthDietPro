package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
internal fun AnimatedNavigationRow(
    itemCount: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    indicator: @Composable (Modifier, Int) -> Unit,
    item: @Composable RowScope.(Int) -> Unit,
) {
    if (itemCount <= 0) return

    Box(modifier = modifier.fillMaxWidth().fillMaxHeight()) {
        val resolvedSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
        indicator(Modifier.fillMaxWidth().fillMaxHeight(), resolvedSelectedIndex)
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            repeat(itemCount) { index -> item(index) }
        }
    }
}

@Composable
internal fun RowScope.AnimatedNavigationItem(
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    rippleShape: Shape = RectangleShape,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "navigationItemColor",
    )
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(rippleShape)
            .centerExpandingRipple(interactionSource, contentColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(contentColor)
    }
}

@Composable
internal fun Modifier.centerExpandingRipple(
    interactionSource: MutableInteractionSource,
    color: Color,
): Modifier {
    val rippleProgress = remember { Animatable(1f) }
    LaunchedEffect(interactionSource) {
        var rippleJob: Job? = null
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                rippleJob?.cancel()
                rippleJob = launch {
                    rippleProgress.snapTo(0f)
                    rippleProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    )
                }
            }
        }
    }
    return drawWithContent {
        drawContent()
        if (rippleProgress.value < 1f) {
            val radius = sqrt(size.width * size.width + size.height * size.height) *
                0.5f * rippleProgress.value
            drawCircle(
                color = color.copy(alpha = (1f - rippleProgress.value) * 0.20f),
                radius = radius,
                center = center,
            )
        }
    }
}

@Composable
internal fun navigationIndicatorColor(): Color =
    androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

@Composable
internal fun MagneticFluidSliderIndicator(
    selectedIndex: Int,
    itemCount: Int,
    color: Color,
    cornerRadius: Dp,
    horizontalInset: Dp,
    verticalInset: Dp,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    var startPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var targetPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var previousIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (previousIndex != selectedIndex) {
            startPosition += (targetPosition - startPosition) * easeInOutQuad(progress.value)
            targetPosition = selectedIndex.toFloat()
            previousIndex = selectedIndex
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 400, easing = LinearEasing))
        }
    }
    BoxWithConstraints(modifier) {
        val cellWidth = maxWidth / itemCount.coerceAtLeast(1)
        val baseWidth = (cellWidth - horizontalInset * 2).coerceAtLeast(0.dp)
        val baseHeight = (maxHeight - verticalInset * 2).coerceAtLeast(0.dp)
        val cellWidthPx = with(LocalDensity.current) { cellWidth.toPx() }
        Box(
            modifier = Modifier
                .width(baseWidth)
                .height(baseHeight)
                .offset(x = horizontalInset, y = verticalInset)
                .graphicsLayer {
                    val currentProgress = progress.value
                    val centerPosition = startPosition + (targetPosition - startPosition) * easeInOutQuad(currentProgress)
                    translationX = cellWidthPx * centerPosition
                    scaleX = magneticWidthScale(currentProgress)
                    scaleY = magneticHeightScale(currentProgress)
                    transformOrigin = TransformOrigin.Center
                }
                .background(color, RoundedCornerShape(cornerRadius)),
        )
    }
}

private fun easeInOutQuad(progress: Float): Float = if (progress < 0.5f) {
    2f * progress * progress
} else {
    1f - (-2f * progress + 2f).let { it * it } / 2f
}

private fun magneticWidthScale(progress: Float): Float {
    return magneticMorphScale(progress, overshoot = 1.10f)
}

private fun magneticHeightScale(progress: Float): Float {
    return magneticMorphScale(progress, overshoot = 1.04f)
}

private fun magneticMorphScale(progress: Float, overshoot: Float): Float = when {
    progress < 0.12f -> interpolateMotionValue(1f, 0.82f, progress / 0.12f)
    progress < 0.38f -> interpolateMotionValue(0.82f, 0.45f, (progress - 0.12f) / 0.26f)
    progress < 0.56f -> interpolateMotionValue(0.45f, 0.86f, (progress - 0.38f) / 0.18f)
    progress < 0.76f -> interpolateMotionValue(0.86f, overshoot, (progress - 0.56f) / 0.20f)
    else -> interpolateMotionValue(overshoot, 1f, (progress - 0.76f) / 0.24f)
}

private fun interpolateMotionValue(start: Float, end: Float, progress: Float): Float =
    start + (end - start) * easeInOutQuad(progress.coerceIn(0f, 1f))

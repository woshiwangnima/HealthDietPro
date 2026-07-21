package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

@Composable
internal fun AnimatedNavigationRow(
    itemCount: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    indicator: @Composable (Modifier) -> Unit,
    item: @Composable RowScope.(Int) -> Unit,
) {
    if (itemCount <= 0) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val itemWidth = maxWidth / itemCount
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex.coerceIn(0, itemCount - 1),
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
            label = "navigationIndicatorOffset",
        )
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            indicator(
                Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight(),
            )
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                repeat(itemCount) { index -> item(index) }
            }
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
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "navigationItemColor",
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "navigationItemScale",
    )
    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
            },
        contentAlignment = Alignment.Center,
    ) {
        content(contentColor)
    }
}

@Composable
internal fun navigationIndicatorColor(): Color =
    androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val MARQUEE_HOLD_MILLIS = 1_200L
private const val MARQUEE_TRAVEL_MILLIS = 3_600
private val MarqueeMinOverflow = 4.dp

/**
 * 单行内容在容器内放不下时缓慢往返滚动（跑马灯），放得下时完全静止。
 * 用于缩略图角标这类「既不能缩小字号、又必须显示完整文字」的场景。
 * 注意：这是循环动画，仅用于承载必须可读的信息，不用于装饰。
 */
@Composable
fun MarqueeRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable RowScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val minOverflowPx = with(LocalDensity.current) { MarqueeMinOverflow.roundToPx() }
    val overflow = scrollState.maxValue
    LaunchedEffect(overflow, minOverflowPx) {
        if (overflow <= minOverflowPx) return@LaunchedEffect
        scrollState.scrollTo(0)
        while (true) {
            delay(MARQUEE_HOLD_MILLIS)
            scrollState.animateScrollTo(overflow, tween(MARQUEE_TRAVEL_MILLIS, easing = LinearEasing))
            delay(MARQUEE_HOLD_MILLIS)
            scrollState.animateScrollTo(0, tween(MARQUEE_TRAVEL_MILLIS, easing = LinearEasing))
        }
    }
    Row(
        modifier = modifier.horizontalScroll(scrollState, enabled = false),
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

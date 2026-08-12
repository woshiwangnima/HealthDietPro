package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Vertical percent slider: top = 100%, bottom = 0%.
 *
 * 使用方只需传入 [value]（0..1，**1 = 顶部 100%，0 = 底部 0%**）与 [onValueChange]。
 * 轨道有效区、滑块位置、点按/拖动手势均在本组件内部共用同一套坐标换算
 * （顶部/底部各留出刻度标签高度），因此显示与交互天然一致。
 *
 * 手势在按下（down）时立即 `consume()`，因此即便本组件被置于可滚动容器中，
 * 竖直拖动也不会被父级 scrollable 抢走——调用方无需任何额外处理。
 */
@Composable
internal fun VerticalPercentSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val trackWidthPx = with(density) { 4.dp.toPx() }
    val thumbRadiusPx = with(density) { 9.dp.toPx() }
    val labelInsetPx = with(density) { 18.dp.toPx() }
    var heightPx by remember { mutableIntStateOf(0) }

    // 唯一的位置换算来源：轨道有效区 = [labelInsetPx, heightPx - labelInsetPx]。
    fun percentForY(y: Float): Float {
        val trackTop = labelInsetPx
        val trackBottom = (heightPx - labelInsetPx).coerceAtLeast(trackTop)
        val trackHeight = (trackBottom - trackTop).coerceAtLeast(1f)
        return (1f - (y - trackTop) / trackHeight).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .onSizeChanged { heightPx = it.height }
            .pointerInput(heightPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onValueChange(percentForY(down.position.y))
                    var event = awaitPointerEvent()
                    while (true) {
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed || change.isConsumed) break
                        change.consume()
                        onValueChange(percentForY(change.position.y))
                        event = awaitPointerEvent()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackTop = labelInsetPx
            val trackBottom = (size.height - labelInsetPx).coerceAtLeast(trackTop)
            val trackHeight = (trackBottom - trackTop).coerceAtLeast(0f)
            val cx = size.width / 2f
            val trackLeft = cx - trackWidthPx / 2f
            val corner = CornerRadius(trackWidthPx / 2f)

            drawRoundRect(
                color = scheme.surfaceVariant,
                topLeft = Offset(trackLeft, trackTop),
                size = Size(trackWidthPx, trackHeight),
                cornerRadius = corner,
            )
            val clamped = value.coerceIn(0f, 1f)
            val fillHeight = trackHeight * clamped
            if (fillHeight > 0f) {
                drawRoundRect(
                    color = scheme.primary,
                    topLeft = Offset(trackLeft, trackBottom - fillHeight),
                    size = Size(trackWidthPx, fillHeight),
                    cornerRadius = corner,
                )
            }
            val thumbY = trackBottom - fillHeight
            drawCircle(color = scheme.primary, radius = thumbRadiusPx, center = Offset(cx, thumbY))
            drawCircle(
                color = scheme.onPrimary,
                radius = thumbRadiusPx * 0.4f,
                center = Offset(cx, thumbY),
            )
        }
        Text(
            text = "100%",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Text(
            text = "0%",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

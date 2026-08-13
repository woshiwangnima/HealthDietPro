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
import androidx.compose.runtime.rememberUpdatedState
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
 * 轨道有效区、滑块位置、点按/拖动手势均通过共享的 [VerticalTrack] 坐标换算
 * （与侧视图红线、轮廓共用同一来源），因此显示与交互天然一致。
 *
 * 手势协程在组合期间只启动一次，并通过 [rememberUpdatedState] 始终读取最新的
 * [onValueChange]；这样即使父级重建了回调闭包（例如编辑器整体重置状态），
 * 拖拽仍能正确反馈，不会出现“进度条拖不动”。
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
    val topInsetPx = with(density) { TRACK_LABEL_INSET_DP.dp.toPx() }
    val bottomInsetPx = with(density) { TRACK_LABEL_INSET_DP.dp.toPx() }
    var heightPx by remember { mutableIntStateOf(0) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentHeightPx by rememberUpdatedState(heightPx)
    val currentTopInsetPx by rememberUpdatedState(topInsetPx)
    val currentBottomInsetPx by rememberUpdatedState(bottomInsetPx)

    Box(
        modifier = modifier
            .onSizeChanged { heightPx = it.height }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val track = VerticalTrack(
                        heightPx = currentHeightPx.toFloat(),
                        topInsetPx = currentTopInsetPx,
                        bottomInsetPx = currentBottomInsetPx,
                    )
                    currentOnValueChange(track.percentForY(down.position.y))
                    var event = awaitPointerEvent()
                    while (true) {
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed || change.isConsumed) break
                        change.consume()
                        currentOnValueChange(track.percentForY(change.position.y))
                        event = awaitPointerEvent()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val track = VerticalTrack(
                heightPx = size.height,
                topInsetPx = currentTopInsetPx,
                bottomInsetPx = currentBottomInsetPx,
            )
            val cx = size.width / 2f
            val trackLeft = cx - trackWidthPx / 2f
            val corner = CornerRadius(trackWidthPx / 2f)

            drawRoundRect(
                color = scheme.surfaceVariant,
                topLeft = Offset(trackLeft, track.trackTop),
                size = Size(trackWidthPx, track.trackHeight),
                cornerRadius = corner,
            )
            val clamped = value.coerceIn(0f, 1f)
            val thumbY = track.yForPercent(clamped)
            if (thumbY < track.trackBottom) {
                drawRoundRect(
                    color = scheme.primary,
                    topLeft = Offset(trackLeft, thumbY),
                    size = Size(trackWidthPx, (track.trackBottom - thumbY).coerceAtLeast(0f)),
                    cornerRadius = corner,
                )
            }
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

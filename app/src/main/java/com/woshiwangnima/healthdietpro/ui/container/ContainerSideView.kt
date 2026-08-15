package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.model.container.CrossSection
import com.woshiwangnima.healthdietpro.model.container.CrossSectionProfile
import com.woshiwangnima.healthdietpro.model.container.equivalentDiameterCm

/** Max usable drawing height (dp) reserved for the container silhouette. */
private const val MAX_DRAW_HEIGHT_DP = 280

/**
 * Container side view driven by a vertical percent slider on the right.
 *
 * 真实比例绘制：轮廓宽度（∝√面积，经 [equivalentDiameterCm]）与高度（真实 cm）使用同一
 * px-per-cm 比例尺，再按可用绘制区宽/高取整盒缩放（contain），因此高宽比始终与真实容器一致
 * （例如总高 5cm、底径 15cm 时看起来就是矮而宽，不会出现“高度比直径还大”）。
 *
 * 进度条与绘制高度的映射转换：右侧 [VerticalPercentSlider] 的盒高度由真实比例下的绘制高度
 * （总高 × px-per-cm）加顶部/底部刻度标签内边距得出，因此滑块与侧视图共用同一个
 * [VerticalTrack] 坐标 —— 滑块 thumb、预览红线与轮廓纵坐标全部经 `yForPercent(heightCm/totalHeightCm)`
 * 换算，比例一致且像素级对齐；修改容器高宽比时只改变预览盒高度，进度条行为不受影响。
 */
@Composable
internal fun ContainerSideView(
    profile: CrossSectionProfile,
    currentEditPercent: Float,
    onHeightChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val topInsetPx = with(density) { TRACK_LABEL_INSET_DP.dp.toPx() }
    val bottomInsetPx = with(density) { TRACK_LABEL_INSET_DP.dp.toPx() }
    var drawingWidthPx by remember { mutableIntStateOf(0) }
    val maxDiaCm = profile.points.maxOf { it.shape.equivalentDiameterCm() }.coerceAtLeast(0.01)
    val totalHeightCm = profile.totalHeightCm.coerceAtLeast(0.01)
    val maxDrawHeightPx = with(density) { MAX_DRAW_HEIGHT_DP.dp.toPx() }
    val boxHeightPx = remember(profile, drawingWidthPx) {
        if (drawingWidthPx <= 0) {
            with(density) { 200.dp.toPx() }
        } else {
            val pxPerCm = minOf(
                (drawingWidthPx * 0.72f) / maxDiaCm.toFloat(),
                maxDrawHeightPx / totalHeightCm.toFloat(),
            ).coerceAtLeast(0.01f)
            totalHeightCm.toFloat() * pxPerCm + topInsetPx + bottomInsetPx
        }
    }
    val boxHeightDp = with(density) { boxHeightPx.toDp() }
    Row(
        modifier = modifier.fillMaxWidth().height(boxHeightDp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .onSizeChanged { drawingWidthPx = it.width },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val track = VerticalTrack(heightPx = h, topInsetPx = topInsetPx, bottomInsetPx = bottomInsetPx)
                val pxPerCm = track.trackHeight / totalHeightCm.toFloat()

                fun halfWidthPx(point: CrossSection): Float = (point.shape.equivalentDiameterCm() / 2.0).toFloat() * pxPerCm
                fun yFor(point: CrossSection): Float =
                    track.yForPercent((point.heightCm / totalHeightCm).toFloat().coerceIn(0f, 1f))

                val path = Path()
                val n = profile.points.size
                var first = true
                for (i in 0 until n) {
                    val p = profile.points[i]
                    val y = yFor(p)
                    val hw = halfWidthPx(p)
                    if (first) {
                        path.moveTo(cx - hw, y)
                        first = false
                    } else {
                        path.lineTo(cx - hw, y)
                    }
                }
                for (i in n - 1 downTo 0) {
                    val p = profile.points[i]
                    val y = yFor(p)
                    val hw = halfWidthPx(p)
                    path.lineTo(cx + hw, y)
                }
                path.close()
                drawPath(path, scheme.primary.copy(alpha = 0.20f))
                drawPath(path, scheme.primary, style = Stroke(2.dp.toPx()))

                val lineY = track.yForPercent(currentEditPercent.coerceIn(0f, 1f))
                drawLine(scheme.error, Offset(0f, lineY), Offset(w, lineY), strokeWidth = 2.dp.toPx())
                drawCircle(scheme.error, 6.dp.toPx(), Offset(cx, lineY))
            }
            Text(
                text = "${(currentEditPercent * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.error,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
        VerticalPercentSlider(
            value = currentEditPercent,
            onValueChange = onHeightChanged,
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(),
        )
    }
}
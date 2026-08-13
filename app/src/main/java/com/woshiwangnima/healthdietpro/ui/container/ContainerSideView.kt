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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.model.container.CrossSection
import com.woshiwangnima.healthdietpro.model.container.CrossSectionProfile
import com.woshiwangnima.healthdietpro.model.container.equivalentDiameterCm

/**
 * Container side view driven by a vertical percent slider on the right.
 *
 * The silhouette width at each height is derived from the cross-section area
 * (width ∝ √area). The red slice line follows the slider: top = 100%, bottom = 0%.
 *
 * 轮廓与红线共用与右侧 [VerticalPercentSlider] 相同的 [VerticalTrack] 坐标换算
 * （顶部/底部各留出刻度标签内边距），因此红线高度、滑块位置与预览绘制始终严格对应。
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
    Row(
        modifier = modifier.fillMaxWidth().height(200.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val track = VerticalTrack(
                    heightPx = h,
                    topInsetPx = topInsetPx,
                    bottomInsetPx = bottomInsetPx,
                )
                val maxDiaCm = profile.points.maxOf { it.shape.equivalentDiameterCm() }
                val scale = if (maxDiaCm > 0) (w * 0.72f) / maxDiaCm.toFloat() else 1f

                fun halfWidthPx(point: CrossSection): Float = (point.shape.equivalentDiameterCm() / 2.0).toFloat() * scale
                fun yFor(point: CrossSection): Float =
                    track.yForPercent((point.heightCm / profile.totalHeightCm).toFloat())

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

package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.range.Range
import kotlin.math.cos
import kotlin.math.sin

/** A named historical statistic rendered as a marker on its metric group's track. */
internal data class GaugeReferenceValue(
    val id: String,
    val label: String,
    val value: Double,
    val color: Color,
)

/** One independently scaled metric and its historical reference values. */
internal data class GaugeMetricGroup(
    val id: String,
    val label: String,
    val currentValue: Double,
    val range: Range<Double>,
    val color: Color,
    val references: List<GaugeReferenceValue> = emptyList(),
    val unit: String = "",
)

private data class GaugeTooltip(val anchor: Offset)

/** A semi-circular gauge that overlays independently scaled metric groups. */
@Composable
internal fun MappedValueGauge(
    groups: List<GaugeMetricGroup>,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = true,
    gaugeHeight: androidx.compose.ui.unit.Dp = 140.dp,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(groups) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    var tooltip by remember { mutableStateOf<GaugeTooltip?>(null) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    var tooltipSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    Column(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(gaugeHeight).padding(horizontal = 6.dp)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val tooltipWidth = 176.dp
            Canvas(
                Modifier.fillMaxWidth().height(gaugeHeight)
                    .onSizeChanged { canvasSize = it }
                    .then(if (showTooltip) Modifier.pointerInput(groups, progress.value, canvasSize) {
                        detectTapGestures { tap -> tooltip = GaugeTooltip(tap) }
                    } else Modifier),
            ) {
            val stroke = size.minDimension * 0.075f
            val radius = ((size.width - stroke) / 2f).coerceAtMost(size.height - stroke)
            val center = Offset(size.width / 2f, size.height - stroke / 2f)
            val arcBounds = androidx.compose.ui.geometry.Rect(center = center, radius = radius)
            drawArc(trackColor, 180f, 180f, false, arcBounds.topLeft, arcBounds.size, style = Stroke(stroke, cap = StrokeCap.Round))
            groups.forEachIndexed { index, group ->
                val minimum = group.range.min
                val maximum = group.range.max
                if (minimum != null && maximum != null && maximum > minimum) {
                    val groupRadius = radius - stroke * (0.8f + index * 0.72f)
                    val groupBounds = androidx.compose.ui.geometry.Rect(center = center, radius = groupRadius)
                    drawArc(group.color.copy(alpha = 0.25f), 180f, 180f, false, groupBounds.topLeft, groupBounds.size, style = Stroke(stroke * 0.48f, cap = StrokeCap.Round))
                    group.references.forEach { reference ->
                        val fraction = ((reference.value - minimum) / (maximum - minimum)).toFloat().coerceIn(0f, 1f)
                        val angle = Math.toRadians((180f + fraction * 180f).toDouble())
                        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                        val markerHalfLength = stroke * 0.05f
                        val markerStart = Offset(center.x + direction.x * (groupRadius - markerHalfLength), center.y + direction.y * (groupRadius - markerHalfLength))
                        val markerEnd = Offset(center.x + direction.x * (groupRadius + markerHalfLength), center.y + direction.y * (groupRadius + markerHalfLength))
                        drawLine(reference.color, markerStart, markerEnd, strokeWidth = stroke * 0.34f, cap = StrokeCap.Square)
                    }
                    val fraction = ((group.currentValue - minimum) / (maximum - minimum)).toFloat().coerceIn(0f, 1f) * progress.value
                    val angle = Math.toRadians((180f + fraction * 180f).toDouble())
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val end = Offset(center.x + direction.x * (groupRadius - stroke * 0.4f), center.y + direction.y * (groupRadius - stroke * 0.4f))
                    drawLine(group.color, center, end, strokeWidth = stroke * 0.3f, cap = StrokeCap.Round)
                    val perpendicular = Offset(-direction.y, direction.x)
                    val tip = Offset(end.x + direction.x * stroke * 0.52f, end.y + direction.y * stroke * 0.52f)
                    val base = Offset(end.x - direction.x * stroke * 0.22f, end.y - direction.y * stroke * 0.22f)
                    drawPath(
                        Path().apply {
                            moveTo(tip.x, tip.y)
                            lineTo(base.x + perpendicular.x * stroke * 0.34f, base.y + perpendicular.y * stroke * 0.34f)
                            lineTo(base.x - perpendicular.x * stroke * 0.34f, base.y - perpendicular.y * stroke * 0.34f)
                            close()
                        },
                        color = group.color,
                    )
                }
            }
            drawCircle(trackColor, stroke * 0.48f, center)
            }
            if (showTooltip) tooltip?.let { target ->
                val tooltipWidthPx = tooltipSize.width.takeIf { it > 0 }?.toFloat() ?: with(density) { tooltipWidth.toPx() }
                val tooltipHeightPx = tooltipSize.height.toFloat()
                val maxX = canvasSize.width - tooltipWidthPx
                val above = target.anchor.y > tooltipHeightPx + with(density) { 12.dp.toPx() }
                val x = (target.anchor.x - tooltipWidthPx / 2f).coerceIn(0f, maxX.coerceAtLeast(0f))
                val y = if (above) {
                    target.anchor.y - tooltipHeightPx - with(density) { 8.dp.toPx() }
                } else {
                    target.anchor.y + with(density) { 8.dp.toPx() }
                }.coerceIn(0f, (canvasSize.height - tooltipHeightPx).coerceAtLeast(0f))
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.20f),
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .onSizeChanged { tooltipSize = it }
                        .offset { IntOffset(x.toInt(), y.toInt()) }
                        .width(tooltipWidth),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        groups.forEach { group ->
                            TextOverflowText(
                                text = "${group.label}: ${group.currentValue}",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = group.color,
                                maxLines = 1,
                            )
                            group.references.forEach { reference ->
                                TextOverflowText(
                                    text = "${reference.label}: ${reference.value}",
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = reference.color,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

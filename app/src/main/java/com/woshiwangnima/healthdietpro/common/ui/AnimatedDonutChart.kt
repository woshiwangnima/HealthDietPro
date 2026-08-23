package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

@Immutable
internal data class DonutChartSegment(
    val id: String,
    val label: String,
    val value: Float,
    val color: Color? = null,
    val needsAttention: Boolean = false,
)

/** A compact animated donut chart with a bounded text legend for narrow layouts. */
@Composable
internal fun AnimatedDonutChart(
    segments: List<DonutChartSegment>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
    labelMaxLines: Int = 1,
    centerAction: (@Composable () -> Unit)? = null,
    chartHeight: Dp = 216.dp,
    centerContentColor: Color? = null,
    centerContentModifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = remember(scheme) {
        listOf(scheme.primary, scheme.secondary, Color(0xFF039BE5), Color(0xFFF9A825), Color(0xFF8E24AA), Color(0xFF00897B))
    }
    val normalized = remember(segments, palette) {
        segments.filter { it.value > 0f }.mapIndexed { index, segment ->
            segment.copy(color = segment.color ?: palette[index % palette.size])
        }
    }
    val total = normalized.sumOf { it.value.toDouble() }.toFloat()
    val hasData = total > 0f
    val animation = remember { Animatable(0f) }
    LaunchedEffect(normalized, total) {
        animation.snapTo(0f)
        animation.animateTo(if (hasData) 1f else 0f, tween(900, easing = FastOutSlowInEasing))
    }
    var labelLayouts by remember(normalized) { androidx.compose.runtime.mutableStateOf<List<DonutLabelLayout>>(emptyList()) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = minOf(28.dp.toPx(), size.minDimension * .16f)
                val diameter = ((size.minDimension - stroke * 2f) * .9f).coerceAtLeast(0f)
                val topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                drawArc(scheme.surfaceVariant.copy(alpha = .65f), -90f, 360f, false, topLeft, androidx.compose.ui.geometry.Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                var startAngle = -90f
                normalized.forEach { segment ->
                    val sweep = segment.value / total * 360f
                    val gap = minOf(3f, sweep / 4f)
                    val displayedSweep = (sweep - gap) * animation.value
                    drawArc(requireNotNull(segment.color), startAngle + gap / 2f, displayedSweep, false, topLeft, androidx.compose.ui.geometry.Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                    startAngle += sweep
                }
                labelLayouts.forEach { layout ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val direction = Offset(layout.anchor.x - center.x, layout.anchor.y - center.y)
                    val length = kotlin.math.sqrt(direction.x * direction.x + direction.y * direction.y).coerceAtLeast(1f)
                    val start = Offset(
                        center.x + direction.x / length * (diameter / 2f + stroke / 2f),
                        center.y + direction.y / length * (diameter / 2f + stroke / 2f),
                    )
                    val labelEdge = layout.bounds.closestPointTo(start)
                    val gap = 6.dp.toPx()
                    val toStart = Offset(start.x - labelEdge.x, start.y - labelEdge.y)
                    val edgeDistance = kotlin.math.sqrt(toStart.x * toStart.x + toStart.y * toStart.y).coerceAtLeast(1f)
                    val end = Offset(
                        labelEdge.x + toStart.x / edgeDistance * minOf(gap, edgeDistance),
                        labelEdge.y + toStart.y / edgeDistance * minOf(gap, edgeDistance),
                    )
                    drawLine(layout.color, start, end, 1.dp.toPx())
                    drawCircle(layout.color, 3.dp.toPx(), end)
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .widthIn(max = 136.dp)
                    .padding(horizontal = 8.dp)
                    .then(centerContentModifier),
            ) {
                Text(
                    text = if (hasData) centerValue else stringResource(R.string.no_record),
                    style = MaterialTheme.typography.headlineSmall,
                    color = centerContentColor ?: scheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hasData) {
                    Text(
                        centerLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = centerContentColor ?: scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                centerAction?.invoke()
            }
            DynamicDonutLabels(
                segments = normalized,
                onLayoutsChanged = { labelLayouts = it },
                maxLines = labelMaxLines,
            )
        }
        if (showLegend) {
            normalized.forEach { segment ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.padding(end = 8.dp).height(10.dp).widthIn(min = 10.dp, max = 10.dp)) {
                        drawCircle(requireNotNull(segment.color))
                    }
                    Text(segment.label, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private data class DonutLabelLayout(
    val id: String,
    val anchor: Offset,
    val bounds: Rect,
    val color: Color,
)

private fun Rect.closestPointTo(point: Offset): Offset = Offset(
    point.x.coerceIn(left, right),
    point.y.coerceIn(top, bottom),
)

@Composable
private fun DynamicDonutLabels(
    segments: List<DonutChartSegment>,
    onLayoutsChanged: (List<DonutLabelLayout>) -> Unit,
    maxLines: Int = 1,
) {
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()
    var size by remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val layouts = remember(segments, size, textStyle, density, maxLines) {
        donutLabelLayouts(
            segments = segments,
            width = size.width.toFloat(),
            height = size.height.toFloat(),
            radius = donutOuterRadius(size.width.toFloat(), size.height.toFloat(), with(density) { 28.dp.toPx() }),
            textMeasure = { text, maxWidth ->
                textMeasurer.measure(
                    AnnotatedString(text),
                    textStyle,
                    constraints = if (maxLines > 1) Constraints(maxWidth = maxWidth) else Constraints(),
                ).size
            },
        )
    }
    androidx.compose.runtime.LaunchedEffect(layouts) { onLayoutsChanged(layouts) }
    Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
        layouts.forEach { layout ->
            val segment = segments.first { it.id == layout.id }
            val shakeOffset = rememberAttentionShakeOffset(
                active = segment.needsAttention,
                label = "donutLabelShake_${segment.id}",
            )
            val width = layout.bounds.width.toInt().coerceAtLeast(1)
            val height = layout.bounds.height.toInt().coerceAtLeast(1)
            val labelModifier = Modifier
                .offset { IntOffset(0, shakeOffset.roundToPx()) }
                .offset { IntOffset(layout.bounds.left.toInt(), layout.bounds.top.toInt()) }
                .widthIn(min = with(density) { width.toDp() }, max = with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
            val labelAlign = if (layout.bounds.center.x < size.width / 2f) TextAlign.End else TextAlign.Start
            if (maxLines > 1) {
                Text(
                    text = segment.label,
                    modifier = labelModifier,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = labelAlign,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                TextOverflowText(
                    text = segment.label,
                    modifier = labelModifier,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = labelAlign,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun donutOuterRadius(width: Float, height: Float, preferredStroke: Float): Float {
    val minimum = minOf(width, height)
    val stroke = minOf(preferredStroke, minimum * .16f)
    val diameter = ((minimum - stroke * 2f) * .9f).coerceAtLeast(0f)
    return diameter / 2f + stroke / 2f
}

private fun donutLabelLayouts(
    segments: List<DonutChartSegment>,
    width: Float,
    height: Float,
    radius: Float,
    textMeasure: (String, Int) -> androidx.compose.ui.unit.IntSize,
): List<DonutLabelLayout> {
    if (width <= 0f || height <= 0f || segments.isEmpty()) return emptyList()
    val minLabelWidth = 52f
    val gap = 10f
    val maxLabelWidth = (width / 2f - radius - gap).coerceAtLeast(minLabelWidth)
    val center = Offset(width / 2f, height / 2f)
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    var angle = -90f
    val candidates = segments.map { segment ->
        val sweep = segment.value / total * 360f
        val middle = Math.toRadians((angle + sweep / 2f).toDouble())
        angle += sweep
        val direction = Offset(kotlin.math.cos(middle).toFloat(), kotlin.math.sin(middle).toFloat())
        val anchor = Offset(center.x + direction.x * radius, center.y + direction.y * radius)
        val leftSide = direction.x < 0f
        val measured = textMeasure(segment.label, maxLabelWidth.toInt())
        val labelWidth = measured.width.toFloat().coerceIn(minLabelWidth, maxLabelWidth)
        val labelHeight = measured.height.toFloat().coerceAtLeast(20f)
        val x = if (leftSide) {
            center.x - radius - gap - labelWidth
        } else {
            center.x + radius + gap
        }.coerceIn(6f, width - labelWidth - 6f)
        DonutLabelCandidate(
            segment = segment,
            anchor = anchor,
            direction = direction,
            width = labelWidth,
            height = labelHeight,
            x = x,
            y = (anchor.y - labelHeight / 2f).coerceIn(6f, height - labelHeight - 6f),
            leftSide = leftSide,
        )
    }
    return candidates.groupBy { it.leftSide }.flatMap { (_, side) ->
        var previousBottom = 6f
        side.sortedBy { it.y }.map { candidate ->
            val placedY = candidate.y.coerceAtLeast(previousBottom).coerceAtMost(height - candidate.height - 6f)
            previousBottom = placedY + candidate.height + 4f
            val initial = Rect(candidate.x, placedY, candidate.x + candidate.width, placedY + candidate.height)
            val nearest = initial.closestPointTo(center)
            val distance = kotlin.math.sqrt((nearest.x - center.x) * (nearest.x - center.x) + (nearest.y - center.y) * (nearest.y - center.y))
            val push = (radius + gap - distance).coerceAtLeast(0f)
            val pushed = initial.translate(candidate.direction.x * push, candidate.direction.y * push)
            val bounds = Rect(
                pushed.left.coerceIn(6f, width - candidate.width - 6f),
                pushed.top.coerceIn(6f, height - candidate.height - 6f),
                pushed.left.coerceIn(6f, width - candidate.width - 6f) + candidate.width,
                pushed.top.coerceIn(6f, height - candidate.height - 6f) + candidate.height,
            )
            DonutLabelLayout(candidate.segment.id, candidate.anchor, bounds, requireNotNull(candidate.segment.color))
        }
    }
}

private data class DonutLabelCandidate(
    val segment: DonutChartSegment,
    val anchor: Offset,
    val direction: Offset,
    val width: Float,
    val height: Float,
    val x: Float,
    val y: Float,
    val leftSide: Boolean,
)

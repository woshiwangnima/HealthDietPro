package com.woshiwangnima.healthdietpro.common.ui

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Immutable
internal data class DonutChartSegment(
    val id: String,
    val label: String,
    val value: Float,
    val color: Color? = null,
)

/** A compact animated donut chart with a center summary and accessible text legend. */
@Composable
internal fun AnimatedDonutChart(
    segments: List<DonutChartSegment>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
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
    val animation = remember { Animatable(0f) }
    LaunchedEffect(normalized, total) {
        animation.snapTo(0f)
        animation.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.size(196.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 28.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = androidx.compose.ui.geometry.Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                drawArc(scheme.surfaceVariant.copy(alpha = .65f), -90f, 360f, false, topLeft, androidx.compose.ui.geometry.Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                var startAngle = -90f
                normalized.forEach { segment ->
                    val sweep = segment.value / total * 360f
                    val gap = minOf(3f, sweep / 4f)
                    val displayedSweep = (sweep - gap) * animation.value
                    drawArc(requireNotNull(segment.color), startAngle + gap / 2f, displayedSweep, false, topLeft, androidx.compose.ui.geometry.Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                    if (sweep >= 28f && animation.value > .86f) {
                        val middle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                        val radius = diameter / 2f
                        val labelRadius = radius
                        val x = size.width / 2f + kotlin.math.cos(middle).toFloat() * labelRadius
                        val y = size.height / 2f + kotlin.math.sin(middle).toFloat() * labelRadius
                        drawContext.canvas.nativeCanvas.drawText(
                            "${(segment.value / total * 100f).toInt()}%",
                            x,
                            y + 5.dp.toPx(),
                            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.WHITE
                                textAlign = Paint.Align.CENTER
                                textSize = 11.dp.toPx()
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            },
                        )
                    }
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                Text(centerValue, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(centerLabel, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

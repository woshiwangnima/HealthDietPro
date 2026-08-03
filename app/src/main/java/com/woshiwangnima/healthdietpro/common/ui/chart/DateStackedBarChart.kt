package com.woshiwangnima.healthdietpro.common.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

internal data class DateStackedBarSegment(
    val id: String,
    val value: Double,
    val color: Color,
)

internal data class DateStackedBarEntry<T>(
    val date: T,
    val label: String,
    val segments: List<DateStackedBarSegment>,
)

/** Horizontally scrollable stacked bars with a fixed Y axis and selectable date columns. */
@Composable
internal fun <T> DateStackedBarChart(
    entries: List<DateStackedBarEntry<T>>,
    yAxisTitle: String,
    formatValue: (Double) -> String,
    labelEvery: Int,
    selectedEntry: DateStackedBarEntry<T>?,
    onEntrySelected: (DateStackedBarEntry<T>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = entries.maxOfOrNull { entry -> entry.segments.sumOf(DateStackedBarSegment::value) } ?: 0.0
    val axisMax = if (maxValue <= 0.0) 100.0 else ceil(maxValue / 100.0) * 100.0
    val axisWidth = 58.dp
    val dayWidth = 52.dp
    val density = LocalDensity.current

    Column(modifier.fillMaxWidth()) {
        Row {
            Column(Modifier.width(axisWidth).height(196.dp)) {
                Text(yAxisTitle, style = MaterialTheme.typography.labelSmall, color = labelColor)
                Text(formatValue(axisMax), style = MaterialTheme.typography.labelSmall, color = labelColor, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Text(formatValue(axisMax / 2.0), style = MaterialTheme.typography.labelSmall, color = labelColor, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 62.dp))
                Text("0", style = MaterialTheme.typography.labelSmall, color = labelColor, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().padding(top = 61.dp))
            }
            Column(Modifier.weight(1f).horizontalScroll(scrollState)) {
                Canvas(
                    Modifier
                        .width(dayWidth * entries.size)
                        .height(196.dp)
                        .pointerInput(entries) {
                            detectTapGestures { offset ->
                                val index = (offset.x / with(density) { dayWidth.toPx() }).toInt()
                                entries.getOrNull(index)?.let(onEntrySelected)
                            }
                        },
                ) {
                    val chartHeight = size.height - 28.dp.toPx()
                    val baseline = chartHeight
                    val halfLine = baseline / 2f
                    drawLine(axisColor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                    drawLine(axisColor, Offset(0f, halfLine), Offset(size.width, halfLine), 1.dp.toPx())
                    drawLine(axisColor, Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())
                    entries.forEachIndexed { index, entry ->
                        val x = index * dayWidth.toPx() + 10.dp.toPx()
                        val width = dayWidth.toPx() - 20.dp.toPx()
                        var top = baseline
                        entry.segments.forEach { segment ->
                            val height = (segment.value / axisMax * baseline).toFloat()
                            top -= height
                            drawRect(segment.color, Offset(x, top), Size(width, height))
                        }
                        if (entry.date == selectedEntry?.date) {
                            drawRect(
                                color = labelColor,
                                topLeft = Offset(x - 2.dp.toPx(), 2.dp.toPx()),
                                size = Size(width + 4.dp.toPx(), baseline - 2.dp.toPx()),
                                style = Stroke(1.dp.toPx()),
                            )
                        }
                    }
                }
                Row {
                    entries.forEachIndexed { index, entry ->
                        Text(
                            text = if (index % labelEvery == 0 || index == entries.lastIndex) entry.label else "",
                            modifier = Modifier.width(dayWidth),
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

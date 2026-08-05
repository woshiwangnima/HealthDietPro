package com.woshiwangnima.healthdietpro.common.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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

internal data class DateStackedBarReferenceLine(
    val value: Double,
    val label: String,
    val color: Color,
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
    referenceLine: DateStackedBarReferenceLine? = null,
    yAxisTickCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = maxOf(
        entries.maxOfOrNull { entry -> entry.segments.sumOf(DateStackedBarSegment::value) } ?: 0.0,
        referenceLine?.value ?: 0.0,
    )
    val axisMax = if (maxValue <= 0.0) 100.0 else ceil(maxValue / 100.0) * 100.0
    val axisWidth = 58.dp
    val dayWidth = 52.dp
    val density = LocalDensity.current
    val plotTop = 28.dp
    val plotBottom = 168.dp
    val plotHeight = plotBottom - plotTop
    val tickValues = axisTickValues(axisMax, yAxisTickCount)
    val averageLabelOffset = referenceLine?.let { line ->
        axisLabelOffset(
            value = line.value,
            axisMax = axisMax,
            tickValues = tickValues,
            plotTop = plotTop,
            plotHeight = plotHeight,
        )
    }

    Column(modifier.fillMaxWidth()) {
        Row {
            androidx.compose.foundation.layout.Box(Modifier.width(axisWidth).height(196.dp)) {
                Text(yAxisTitle, style = MaterialTheme.typography.labelSmall, color = labelColor)
                tickValues.forEach { value ->
                    AxisValueLabel(
                        text = formatValue(value),
                        color = labelColor,
                        offset = axisTickLabelOffset(value, axisMax, plotTop, plotHeight),
                    )
                }
                if (referenceLine != null && averageLabelOffset != null) {
                    AxisValueLabel(referenceLine.label, referenceLine.color, averageLabelOffset)
                }
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
                    val plotTopPx = plotTop.toPx()
                    val baseline = plotBottom.toPx()
                    val plotHeightPx = baseline - plotTopPx
                    tickValues.forEach { value ->
                        val y = baseline - (value / axisMax * plotHeightPx).toFloat()
                        drawLine(axisColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                    referenceLine?.takeIf { it.value > 0.0 }?.let { line ->
                        val y = baseline - (line.value.coerceIn(0.0, axisMax) / axisMax * plotHeightPx).toFloat()
                        drawLine(line.color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())))
                    }
                    entries.forEachIndexed { index, entry ->
                        val x = index * dayWidth.toPx() + 10.dp.toPx()
                        val width = dayWidth.toPx() - 20.dp.toPx()
                        var top = baseline
                        entry.segments.forEach { segment ->
                            val height = (segment.value / axisMax * plotHeightPx).toFloat()
                            top -= height
                            drawRect(segment.color, Offset(x, top), Size(width, height))
                        }
                        if (entry.date == selectedEntry?.date) {
                            drawRect(
                                color = labelColor,
                                topLeft = Offset(x - 2.dp.toPx(), plotTopPx),
                                size = Size(width + 4.dp.toPx(), plotHeightPx),
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

@Composable
private fun AxisValueLabel(text: String, color: Color, offset: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier
            .fillMaxWidth()
            .offset(y = offset)
            .horizontalScroll(rememberScrollState()),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private fun axisTickValues(axisMax: Double, tickCount: Int): List<Double> {
    val count = tickCount.coerceAtLeast(2)
    val interval = axisMax / (count - 1)
    return List(count) { index -> axisMax - index * interval }
}

private fun axisTickLabelOffset(
    value: Double,
    axisMax: Double,
    plotTop: Dp,
    plotHeight: Dp,
): Dp = plotTop + plotHeight * (1f - (value / axisMax).toFloat()) - 8.dp

private fun axisLabelOffset(
    value: Double,
    axisMax: Double,
    tickValues: List<Double>,
    plotTop: Dp,
    plotHeight: Dp,
): Dp {
    val lineHeight = 16.dp
    val minimumGap = 2.dp
    val boundedValue = value.coerceIn(0.0, axisMax)
    val exactTickIndex = tickValues.indexOfFirst { it == boundedValue }
    val (upperTick, lowerTick) = when {
        exactTickIndex == 0 -> tickValues[0] to tickValues[1]
        exactTickIndex == tickValues.lastIndex -> tickValues[tickValues.lastIndex - 1] to tickValues.last()
        exactTickIndex > 0 -> tickValues[exactTickIndex] to tickValues[exactTickIndex + 1]
        else -> {
            val lowerIndex = tickValues.indexOfFirst { it < boundedValue }
            tickValues[lowerIndex - 1] to tickValues[lowerIndex]
        }
    }
    val desired = axisTickLabelOffset(boundedValue, axisMax, plotTop, plotHeight)
    val upperBoundary = axisTickLabelOffset(upperTick, axisMax, plotTop, plotHeight) + lineHeight + minimumGap
    val lowerBoundary = axisTickLabelOffset(lowerTick, axisMax, plotTop, plotHeight) - lineHeight - minimumGap
    return desired.coerceIn(upperBoundary, lowerBoundary)
}

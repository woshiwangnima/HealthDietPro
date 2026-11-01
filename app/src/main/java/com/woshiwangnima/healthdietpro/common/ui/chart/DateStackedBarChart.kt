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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    columnWidth: Dp = 52.dp,
    /**
     * 打开时先滚到最右（最新的一天）。
     *
     * 时间轴是「早 → 近」，日期一多（30 / 90 天）默认停在最左就看不到最近的数据——
     * 而读者进这一屏基本都是想知道「最近怎么样」。
     */
    scrollToEnd: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // 贴右不能靠构造初始值：`rememberScrollState` 只在首次组合取值，切换时间范围时
    // 组合复用同一个 `ScrollState`（maxValue 从 0 变成几千），视图仍停在最左——
    // 30 / 90 天一切换就看不见最近几根柱子。改为等尺寸量出来后再滚到最右。
    val scrollState = rememberScrollState()
    if (scrollToEnd) {
        LaunchedEffect(entries.size) {
            snapshotFlow { scrollState.maxValue }.filter { it > 0 }.first()
            scrollState.scrollTo(scrollState.maxValue)
        }
    }
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    // 选中列用主色描边：灰色描边在浅色主题下和柱子轮廓混在一起，看着像「这根柱没上色」。
    val selectionColor = MaterialTheme.colorScheme.primary
    val maxValue = maxOf(
        entries.maxOfOrNull { entry -> entry.segments.sumOf(DateStackedBarSegment::value) } ?: 0.0,
        referenceLine?.value ?: 0.0,
    )
    val axisMax = if (maxValue <= 0.0) 100.0 else ceil(maxValue / 100.0) * 100.0
    val axisWidth = 58.dp
    // 每根柱占的宽度可调：7 天 7 根柱用 52 dp 会比卡片宽 20 多 dp，
    // 横向可滚却让最右边的日期标签被切掉一半，看起来像画坏了；由调用方按天数收窄即可整屏放下。
    val dayWidth = columnWidth
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
                    AxisValueLabel(referenceLine.label, referenceLine.color, averageLabelOffset, allowOverflow = true)
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
                    // 柱宽两侧的留白按列宽收放：固定 10 dp 在 24 dp 一列的 90 天档位里
                    // 只剩 4 dp 柱身，看着像头发丝；列宽窄时留白跟着缩到 1/4。
                    val barInset = (dayWidth.toPx() * 0.25f).coerceAtMost(10.dp.toPx())
                    entries.forEachIndexed { index, entry ->
                        val x = index * dayWidth.toPx() + barInset
                        val width = dayWidth.toPx() - barInset * 2
                        var top = baseline
                        entry.segments.forEach { segment ->
                            val height = (segment.value / axisMax * plotHeightPx).toFloat()
                            top -= height
                            drawRect(segment.color, Offset(x, top), Size(width, height))
                        }
                        if (entry.date == selectedEntry?.date) {
                            drawRect(
                                color = selectionColor,
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
                            // 列宽 24 dp 时「10-27」装不下会折成两行（真机上是「10-2 / 7」），
                            // 单行 + 允许溢出：标签本来就隔几列才写一个，撞不到隔壁。
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AxisValueLabel(
    text: String,
    color: Color,
    offset: androidx.compose.ui.unit.Dp,
    /** 参考线标签比刻度值长（「平均 1h30m」），58 dp 的轴宽装不下就会被切成「1h30r」。 */
    allowOverflow: Boolean = false,
) {
    // `horizontalScroll` 不能在无限宽约束下测量，两者只能二选一：
    // 参考线标签走「不裁剪」，普通刻度仍走「可横滚」。
    Row(
        Modifier
            .fillMaxWidth()
            .offset(y = offset)
            .then(
                if (allowOverflow) {
                    Modifier.wrapContentWidth(unbounded = true)
                } else {
                    Modifier.horizontalScroll(rememberScrollState())
                },
            ),
    ) {
        // 同理：单行不换行，字才不会被 `maxLines` 之外的宽度约束截断。
        val singleLine = if (allowOverflow) 1 else Int.MAX_VALUE
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = singleLine,
            softWrap = !allowOverflow,
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

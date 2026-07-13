package com.woshiwangnima.healthdietpro.ui.profile.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeBaseChart
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeBaseChartSpec
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartAxisSide
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartAxisSpec
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartPoint
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartRangeBand
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartSeries
import com.woshiwangnima.healthdietpro.common.ui.chart.ChartDataSeries
import com.woshiwangnima.healthdietpro.common.ui.chart.NumericChartTickPolicy
import com.woshiwangnima.healthdietpro.common.ui.chart.TimeChartTickPolicy
import com.woshiwangnima.healthdietpro.model.chart.ComposeChartState
import com.woshiwangnima.healthdietpro.model.chart.ChartTimeConfigRepository
import com.woshiwangnima.healthdietpro.model.unit.UnitRepository
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import android.view.Gravity

@Immutable
data class ComposeChartSpec(
    val title: String,
    val series: List<ChartSeries>,
    val xAxisLabel: String,
    val yAxisLabel: String,
    val chartStateKey: String = "",
    val yAxisBands: List<YAxisBand> = emptyList(),
    val canvasStyle: ChartCanvasStyle = ChartCanvasStyle(),
    val controlLabels: ChartControlLabels? = null,
    val titleGravity: Int = Gravity.CENTER,
    val titleVisible: Boolean = true,
)

@Composable
internal fun ComposeChart(
    spec: ComposeChartSpec,
    chartState: ComposeChartState? = null,
    onChartStateChanged: (ComposeChartState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val style = if (spec.yAxisBands.isEmpty()) spec.canvasStyle else spec.canvasStyle.copy(yAxisBands = spec.yAxisBands)
    val xTickPolicy = remember(context, style.xAxisKind) {
        when (style.xAxisKind) {
            ChartAxisKind.Numeric -> NumericChartTickPolicy
            ChartAxisKind.TimestampMs -> TimeChartTickPolicy.fromConfig(
                config = ChartTimeConfigRepository(context, UnitRepository.fromContext(context)).load(),
                unitRepository = UnitRepository.fromContext(context),
            )
        }
    }
    ComposeBaseChart(
        spec = ComposeBaseChartSpec(
            title = spec.title,
            chartStateKey = spec.chartStateKey,
            series = spec.series.mapIndexed { index, series ->
                ComposeChartSeries(
                    id = index.toString(),
                    label = series.label,
                    points = series.points.map { point ->
                        ComposeChartPoint(point.timestamp.toDouble(), point.value.toDouble(), point.dateLabel)
                    },
                    color = Color(series.color),
                )
            },
            yBands = style.yAxisBands.map { band ->
                ComposeChartRangeBand(band.minValue.toDouble(), band.maxValue.toDouble(), Color(band.color))
            },
            xAxis = ComposeChartAxisSpec(
                label = spec.xAxisLabel,
                position = if (style.xAxisPosition == ChartHorizontalAxisPosition.Top) ComposeChartAxisSide.Top else ComposeChartAxisSide.Bottom,
                valueFormatter = { value, step ->
                    style.xValueFormatter?.invoke(value.toLong(), step.toLong()) ?: value.toLong().toString()
                },
                tickPolicy = xTickPolicy,
            ),
            yAxis = ComposeChartAxisSpec(
                label = spec.yAxisLabel,
                position = if (style.yAxisPosition == ChartVerticalAxisPosition.Left) ComposeChartAxisSide.Start else ComposeChartAxisSide.End,
                valueFormatter = { value, _ -> style.yValueFormatter(value.toFloat()) },
                tickPolicy = NumericChartTickPolicy,
            ),
        ),
        savedState = chartState,
        onChartStateChanged = onChartStateChanged,
        modifier = modifier,
    )
}

internal fun ChartDataSeries.toChartSeries(
    color: Int,
    lineStyle: LineStyle = LineStyle.LINEAR,
    lineType: LineType = LineType.SOLID,
    pointShape: PointShape = PointShape.CIRCLE,
    pointFill: PointFill = PointFill.FILLED,
): ChartSeries = ChartSeries(
    points = points.map { point ->
        DataPoint(
            timestamp = point.x,
            value = point.y,
            dateLabel = point.label,
        )
    },
    label = label,
    color = color,
    lineStyle = lineStyle,
    lineType = lineType,
    pointShape = pointShape,
    pointFill = pointFill,
)

package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.woshiwangnima.healthdietpro.common.ui.chart.ChartDataSeries
import com.woshiwangnima.healthdietpro.model.profile.DataPoint

@Immutable
data class ComposeChartSpec(
    val title: String,
    val series: List<ChartSeries>,
    val unitLabel: String,
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
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> ChartView(context) },
        update = { chartView ->
            chartView.setChartTitle(
                title = spec.title,
                gravity = spec.titleGravity,
                visible = spec.titleVisible,
            )
            spec.controlLabels?.let(chartView::setControlLabels)
            val style = if (spec.yAxisBands.isNotEmpty()) {
                spec.canvasStyle.copy(yAxisBands = spec.yAxisBands)
            } else {
                spec.canvasStyle
            }
            chartView.applyCanvasStyle(style)
            chartView.setChartStateKey(spec.chartStateKey)
            chartView.setSeries(spec.series, spec.unitLabel)
        },
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

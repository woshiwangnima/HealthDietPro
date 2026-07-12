package com.woshiwangnima.healthdietpro.ui.profile.chart

enum class ChartVerticalAxisPosition {
    Left,
    Right,
}

enum class ChartHorizontalAxisPosition {
    Top,
    Bottom,
}

data class XAxisBand(
    val minTimestamp: Long,
    val maxTimestamp: Long,
    val color: Int,
)

data class ChartCanvasStyle(
    val yAxisPosition: ChartVerticalAxisPosition = ChartVerticalAxisPosition.Right,
    val xAxisPosition: ChartHorizontalAxisPosition = ChartHorizontalAxisPosition.Bottom,
    val yGridLineStyles: List<LineType> = listOf(LineType.DOTTED),
    val xGridLineStyles: List<LineType> = emptyList(),
    val yAxisBands: List<YAxisBand> = emptyList(),
    val xAxisBands: List<XAxisBand> = emptyList(),
    val yValueFormatter: (Float) -> String = { value -> "%.0f".format(value) },
    val xValueFormatter: ((timestamp: Long, intervalMs: Long) -> String)? = null,
    val crosshairValueFormatter: ((value: Float, unitLabel: String) -> String)? = null,
    val crosshairTimeFormatter: ((timestamp: Long) -> String)? = null,
)

data class ChartControlLabels(
    val lineStyle: String,
    val xAxisRange: String,
    val xAxisInterval: String,
    val yAxisBounds: String,
    val fullscreen: String,
)

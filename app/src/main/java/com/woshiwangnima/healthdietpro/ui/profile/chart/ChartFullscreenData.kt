package com.woshiwangnima.healthdietpro.ui.profile.chart

data class ChartFullscreenData(
    val series: List<ChartSeries>,
    val unitLabel: String,
    val visibleRangeMs: Long,
    val windowStartMs: Long,
    val yMinPct: Float,
    val yMaxPct: Float,
    val labelIntervalMs: Long,
    val yAxisBands: List<YAxisBand>,
    val chartTitle: String,
    val chartStateKey: String
)

object ChartFullscreenHolder {
    var data: ChartFullscreenData? = null
}

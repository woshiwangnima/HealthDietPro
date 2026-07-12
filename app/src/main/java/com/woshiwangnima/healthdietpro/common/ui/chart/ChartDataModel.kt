package com.woshiwangnima.healthdietpro.common.ui.chart

internal data class ChartDataPoint(
    val x: Long,
    val y: Float,
    val label: String = "",
)

internal data class ChartDataSeries(
    val id: String,
    val label: String,
    val points: List<ChartDataPoint>,
)

internal enum class ChartDataJoinPolicy {
    ExactX,
    CarryForward,
}

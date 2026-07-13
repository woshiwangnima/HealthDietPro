package com.woshiwangnima.healthdietpro.model.chart

import kotlinx.serialization.Serializable

@Serializable
internal data class ComposeChartState(
    val xWindowMode: String,
    val yWindowMode: String,
    val xRangePercent: Double,
    val yRangePercent: Double,
    val xMinPercent: Double,
    val xMaxPercent: Double,
    val yMinPercent: Double,
    val yMaxPercent: Double,
    val xInterval: Double,
    val yInterval: Double,
    val xTickOptionId: String? = null,
    val yTickOptionId: String? = null,
    val xAxisSide: String,
    val yAxisSide: String,
    val xGridPattern: String,
    val yGridPattern: String,
    val crosshairBasis: String,
    val fullscreenAreas: Set<String>,
    val seriesStyles: Map<String, ComposeChartSeriesState>,
)

@Serializable
internal data class ComposeChartSeriesState(
    val colorArgb: Long,
    val lineStyle: String,
    val linePattern: String,
    val pointShape: String,
    val pointFill: String,
)

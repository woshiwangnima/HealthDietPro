package com.woshiwangnima.healthdietpro.common.ui.chart

internal fun deriveChartDataSeries(
    id: String,
    label: String,
    sources: List<ChartDataSeries>,
    expression: ChartValueExpression,
    joinPolicy: ChartDataJoinPolicy = ChartDataJoinPolicy.ExactX,
    pointLabel: (Long) -> String = { "" },
): ChartDataSeries {
    if (sources.isEmpty()) {
        return ChartDataSeries(id = id, label = label, points = emptyList())
    }
    val points = when (joinPolicy) {
        ChartDataJoinPolicy.ExactX -> deriveExactX(sources, expression, pointLabel)
        ChartDataJoinPolicy.CarryForward -> deriveCarryForward(sources, expression, pointLabel)
    }
    return ChartDataSeries(id = id, label = label, points = points)
}

private fun deriveExactX(
    sources: List<ChartDataSeries>,
    expression: ChartValueExpression,
    pointLabel: (Long) -> String,
): List<ChartDataPoint> {
    val sourceMaps = sources.associate { series ->
        series.id to series.points.associateBy { it.x }
    }
    val allX = sources.flatMap { series -> series.points.map { it.x } }
        .distinct()
        .sorted()
    return allX.mapNotNull { x ->
        val values = sourceMaps.mapValues { (_, points) -> points[x]?.y }
            .filterValues { it != null }
            .mapValues { (_, value) -> value ?: 0f }
        evaluateChartValueExpression(expression, values)?.let { y ->
            ChartDataPoint(x = x, y = y, label = pointLabel(x))
        }
    }
}

private fun deriveCarryForward(
    sources: List<ChartDataSeries>,
    expression: ChartValueExpression,
    pointLabel: (Long) -> String,
): List<ChartDataPoint> {
    val sortedSources = sources.associate { series ->
        series.id to series.points.sortedBy { it.x }
    }
    val indexes = sortedSources.keys.associateWith { 0 }.toMutableMap()
    val latest = mutableMapOf<String, Float>()
    val allX = sources.flatMap { series -> series.points.map { it.x } }
        .distinct()
        .sorted()

    return allX.mapNotNull { x ->
        sortedSources.forEach { (seriesId, points) ->
            var index = indexes.getValue(seriesId)
            while (index < points.size && points[index].x <= x) {
                latest[seriesId] = points[index].y
                index++
            }
            indexes[seriesId] = index
        }
        evaluateChartValueExpression(expression, latest)?.let { y ->
            ChartDataPoint(x = x, y = y, label = pointLabel(x))
        }
    }
}

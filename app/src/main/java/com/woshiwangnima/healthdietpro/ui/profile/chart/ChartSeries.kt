package com.woshiwangnima.healthdietpro.ui.profile.chart

import com.woshiwangnima.healthdietpro.model.profile.DataPoint

enum class LineType { SOLID, DASHED, DOTTED }

enum class PointShape { CIRCLE, TRIANGLE, SQUARE, DIAMOND, CROSS }

enum class PointFill { FILLED, HOLLOW }

data class ChartSeries(
    val points: List<DataPoint>,
    val label: String,
    val color: Int,
    val lineStyle: LineStyle = LineStyle.LINEAR,
    val lineType: LineType = LineType.SOLID,
    val pointShape: PointShape = PointShape.CIRCLE,
    val pointFill: PointFill = PointFill.FILLED
)

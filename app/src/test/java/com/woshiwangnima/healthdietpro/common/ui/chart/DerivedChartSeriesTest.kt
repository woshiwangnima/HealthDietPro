package com.woshiwangnima.healthdietpro.common.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class DerivedChartSeriesTest {

    @Test
    fun exactXSkipsPointsMissingRequiredSources() {
        val weight = ChartDataSeries(
            id = "weight",
            label = "Weight",
            points = listOf(
                ChartDataPoint(x = 1L, y = 70f),
                ChartDataPoint(x = 2L, y = 71f),
            ),
        )
        val delta = ChartDataSeries(
            id = "delta",
            label = "Delta",
            points = listOf(ChartDataPoint(x = 2L, y = -1f)),
        )

        val derived = deriveChartDataSeries(
            id = "adjusted",
            label = "Adjusted",
            sources = listOf(weight, delta),
            expression = ChartValueExpression.Add(
                ChartValueExpression.Source("weight"),
                ChartValueExpression.Source("delta"),
            ),
        )

        assertEquals(listOf(ChartDataPoint(x = 2L, y = 70f)), derived.points)
    }

    @Test
    fun carryForwardSupportsBmiLikeDerivedSeries() {
        val weight = ChartDataSeries(
            id = "weightKg",
            label = "Weight",
            points = listOf(ChartDataPoint(x = 2L, y = 65f)),
        )
        val height = ChartDataSeries(
            id = "heightCm",
            label = "Height",
            points = listOf(ChartDataPoint(x = 1L, y = 170f)),
        )
        val heightM = ChartValueExpression.Divide(
            numerator = ChartValueExpression.Source("heightCm"),
            denominator = ChartValueExpression.Constant(100f),
        )

        val derived = deriveChartDataSeries(
            id = "bmi",
            label = "BMI",
            sources = listOf(weight, height),
            expression = ChartValueExpression.Divide(
                numerator = ChartValueExpression.Source("weightKg"),
                denominator = ChartValueExpression.Multiply(heightM, heightM),
            ),
            joinPolicy = ChartDataJoinPolicy.CarryForward,
        )

        assertEquals(1, derived.points.size)
        assertEquals(2L, derived.points.single().x)
        assertEquals(22.491f, derived.points.single().y, 0.001f)
    }

    @Test
    fun percentExpressionComputesPartOfTotal() {
        val part = ChartDataSeries(
            id = "part",
            label = "Part",
            points = listOf(ChartDataPoint(x = 1L, y = 25f)),
        )
        val total = ChartDataSeries(
            id = "total",
            label = "Total",
            points = listOf(ChartDataPoint(x = 1L, y = 200f)),
        )

        val derived = deriveChartDataSeries(
            id = "percent",
            label = "Percent",
            sources = listOf(part, total),
            expression = ChartValueExpression.Percent(
                part = ChartValueExpression.Source("part"),
                total = ChartValueExpression.Source("total"),
            ),
        )

        assertEquals(12.5f, derived.points.single().y, 0.0001f)
    }

    @Test
    fun divideByZeroDropsPoint() {
        val numerator = ChartDataSeries(
            id = "n",
            label = "Numerator",
            points = listOf(ChartDataPoint(x = 1L, y = 10f)),
        )
        val denominator = ChartDataSeries(
            id = "d",
            label = "Denominator",
            points = listOf(ChartDataPoint(x = 1L, y = 0f)),
        )

        val derived = deriveChartDataSeries(
            id = "ratio",
            label = "Ratio",
            sources = listOf(numerator, denominator),
            expression = ChartValueExpression.Divide(
                numerator = ChartValueExpression.Source("n"),
                denominator = ChartValueExpression.Source("d"),
            ),
        )

        assertEquals(emptyList<ChartDataPoint>(), derived.points)
    }
}

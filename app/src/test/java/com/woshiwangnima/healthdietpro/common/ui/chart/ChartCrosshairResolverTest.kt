package com.woshiwangnima.healthdietpro.common.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartCrosshairResolverTest {

    private val points = listOf(
        ComposeChartPoint(x = 0.0, y = 0.0),
        ComposeChartPoint(x = 10.0, y = 10.0),
        ComposeChartPoint(x = 20.0, y = 0.0),
    )

    @Test
    fun perpendicularToXAxis_usesClickedWorldX() {
        val point = ChartCrosshairResolver.resolve(
            points = points,
            lineStyle = ComposeChartLineStyle.Linear,
            basis = ComposeChartCrosshairBasis.PerpendicularToXAxis,
            rawX = 4.0,
            rawY = 99.0,
        )

        requireNotNull(point)
        assertEquals(4.0, point.x, 0.000001)
        assertEquals(4.0, point.y, 0.000001)
    }

    @Test
    fun perpendicularToYAxis_returnsExactIntersectionNearestClickedX() {
        val point = ChartCrosshairResolver.resolve(
            points = points,
            lineStyle = ComposeChartLineStyle.Linear,
            basis = ComposeChartCrosshairBasis.PerpendicularToYAxis,
            rawX = 18.0,
            rawY = 5.0,
        )

        requireNotNull(point)
        assertEquals(15.0, point.x, 0.000001)
        assertEquals(5.0, point.y, 0.000001)
    }

    @Test
    fun perpendicularToYAxis_returnsNullWhenLineDoesNotIntersect() {
        val point = ChartCrosshairResolver.resolve(
            points = points,
            lineStyle = ComposeChartLineStyle.Linear,
            basis = ComposeChartCrosshairBasis.PerpendicularToYAxis,
            rawX = 5.0,
            rawY = 20.0,
        )

        assertNull(point)
    }

    @Test
    fun steppedFront_horizontalSegmentPreservesClickedX() {
        val point = ChartCrosshairResolver.resolve(
            points = listOf(
                ComposeChartPoint(x = 0.0, y = 2.0),
                ComposeChartPoint(x = 10.0, y = 8.0),
            ),
            lineStyle = ComposeChartLineStyle.SteppedFront,
            basis = ComposeChartCrosshairBasis.PerpendicularToYAxis,
            rawX = 7.0,
            rawY = 2.0,
        )

        requireNotNull(point)
        assertEquals(7.0, point.x, 0.000001)
        assertEquals(2.0, point.y, 0.000001)
    }
}

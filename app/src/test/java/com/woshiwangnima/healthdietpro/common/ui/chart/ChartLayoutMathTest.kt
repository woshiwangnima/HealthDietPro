package com.woshiwangnima.healthdietpro.common.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartLayoutMathTest {

    @Test
    fun labelSelection_usesScreenOrderForBothAxisDirections() {
        assertEquals(
            listOf(0, 2),
            ChartLayoutMath.selectNonOverlappingLabelIndexes(
                centers = listOf(10f, 30f, 50f),
                sizes = listOf(18f, 18f, 18f),
                gap = 4f,
            ),
        )
        assertEquals(
            listOf(2, 0),
            ChartLayoutMath.selectNonOverlappingLabelIndexes(
                centers = listOf(50f, 30f, 10f),
                sizes = listOf(18f, 18f, 18f),
                gap = 4f,
            ),
        )
    }

    @Test
    fun panDelta_mapsFullDragToMovableWindowRange() {
        val delta = ChartLayoutMath.panDelta(
            dragPixels = 100f,
            trackPixels = 100f,
            fullRange = 200.0,
            visibleRange = 50.0,
            positiveDirection = 1.0,
        )

        assertEquals(150.0, delta, 0.000001)
    }

    @Test
    fun percentStep_returnsHalfPercentOfFullRange() {
        assertEquals(1.0, ChartLayoutMath.percentStep(200.0), 0.000001)
    }
}

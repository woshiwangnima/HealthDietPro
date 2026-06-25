package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartSegmentMathTest {

    private val entries = listOf(
        floatEntry(0f, 10f),
        floatEntry(100f, 20f),
        floatEntry(300f, 40f),
        floatEntry(700f, 100f),
    )

    @Test
    fun findSegmentIndex_returnsContainingSegment() {
        assertEquals(0, ChartSegmentMath.findSegmentIndex(entries, 0f))
        assertEquals(0, ChartSegmentMath.findSegmentIndex(entries, 50f))
        assertEquals(0, ChartSegmentMath.findSegmentIndex(entries, 100f))
        assertEquals(1, ChartSegmentMath.findSegmentIndex(entries, 100.01f))
        assertEquals(1, ChartSegmentMath.findSegmentIndex(entries, 200f))
        assertEquals(2, ChartSegmentMath.findSegmentIndex(entries, 400f))
        assertEquals(2, ChartSegmentMath.findSegmentIndex(entries, 700f))
    }

    @Test
    fun findSegmentIndex_clampsToLastSegment() {
        assertEquals(2, ChartSegmentMath.findSegmentIndex(entries, 5000f))
    }

    @Test
    fun interpolateLinear_returnsYAtMidpoint() {
        val y = ChartSegmentMath.interpolateLinear(entries, 50f)
        assertEquals(15f, y, 0.0001f)
    }

    @Test
    fun interpolateLinear_returnsRightYAtBoundary() {
        val y = ChartSegmentMath.interpolateLinear(entries, 100f)
        assertEquals(20f, y, 0.0001f)
    }

    @Test
    fun interpolateStepped_returnsRightY() {
        val y = ChartSegmentMath.interpolateStepped(entries, 50f)
        assertEquals(20f, y, 0.0001f)
        val y700 = ChartSegmentMath.interpolateStepped(entries, 700f)
        assertEquals(100f, y700, 0.0001f)
    }

    private fun floatEntry(x: Float, y: Float) =
        com.github.mikephil.charting.data.Entry(x, y)
}

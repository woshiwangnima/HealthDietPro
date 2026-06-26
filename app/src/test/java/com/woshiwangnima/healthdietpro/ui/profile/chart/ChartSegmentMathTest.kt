package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartSegmentMathTest {

    private val entries = listOf(
        ChartMath.ChartEntry(0f, 10f),
        ChartMath.ChartEntry(100f, 20f),
        ChartMath.ChartEntry(300f, 40f),
        ChartMath.ChartEntry(700f, 100f),
    )

    @Test
    fun findSegmentIndex_returnsContainingSegment() {
        assertEquals(0, ChartMath.findSegmentIndex(entries, 0f))
        assertEquals(0, ChartMath.findSegmentIndex(entries, 50f))
        assertEquals(0, ChartMath.findSegmentIndex(entries, 100f))
        assertEquals(1, ChartMath.findSegmentIndex(entries, 100.01f))
        assertEquals(1, ChartMath.findSegmentIndex(entries, 200f))
        assertEquals(2, ChartMath.findSegmentIndex(entries, 400f))
        assertEquals(2, ChartMath.findSegmentIndex(entries, 700f))
    }

    @Test
    fun findSegmentIndex_clampsToLastSegment() {
        assertEquals(2, ChartMath.findSegmentIndex(entries, 5000f))
    }

    @Test
    fun interpolateLinear_returnsYAtMidpoint() {
        val y = ChartMath.interpolateLinear(entries, 50f)
        assertEquals(15f, y, 0.0001f)
    }

    @Test
    fun interpolateLinear_returnsRightYAtBoundary() {
        val y = ChartMath.interpolateLinear(entries, 100f)
        assertEquals(20f, y, 0.0001f)
    }

    @Test
    fun interpolateSteppedFront_returnsLeftY() {
        val y = ChartMath.interpolateSteppedFront(entries, 50f)
        assertEquals(10f, y, 0.0001f)
        val y700 = ChartMath.interpolateSteppedFront(entries, 700f)
        assertEquals(40f, y700, 0.0001f)
    }

    @Test
    fun interpolateSteppedBack_returnsRightY() {
        val y = ChartMath.interpolateSteppedBack(entries, 50f)
        assertEquals(20f, y, 0.0001f)
        val y700 = ChartMath.interpolateSteppedBack(entries, 700f)
        assertEquals(100f, y700, 0.0001f)
    }

    @Test
    fun interpolateBezier_interiorPoint() {
        val simple = listOf(
            ChartMath.ChartEntry(0f, 0f),
            ChartMath.ChartEntry(100f, 100f),
            ChartMath.ChartEntry(200f, 0f),
            ChartMath.ChartEntry(300f, 100f)
        )
        val y = ChartMath.interpolateBezier(simple, 50f)
        assertEquals(50f, y, 30f)
    }

    @Test
    fun interpolateSpline_passesThroughPoints() {
        val pts = listOf(
            ChartMath.ChartEntry(0f, 0f),
            ChartMath.ChartEntry(100f, 100f),
            ChartMath.ChartEntry(200f, 50f),
            ChartMath.ChartEntry(300f, 150f)
        )
        assertEquals(0f, ChartMath.interpolateSpline(pts, 0f), 0.001f)
        assertEquals(100f, ChartMath.interpolateSpline(pts, 100f), 0.001f)
        assertEquals(50f, ChartMath.interpolateSpline(pts, 200f), 0.001f)
        assertEquals(150f, ChartMath.interpolateSpline(pts, 300f), 0.001f)
    }

    @Test
    fun niceScale_examples() {
        val (min1, max1) = ChartMath.niceScale(7f, 93f, 5)
        assertEquals(0f, min1, 0.001f)
        assertEquals(100f, max1, 0.001f)

        val (min2, max2) = ChartMath.niceScale(23f, 67f, 5)
        assertEquals(20f, min2, 0.001f)
        assertEquals(70f, max2, 0.001f)
    }

    @Test
    fun computeLabelInterval_examples() {
        assertEquals(2 * 60 * 60 * 1000L, ChartMath.computeLabelInterval(24 * 60 * 60 * 1000L))
        assertEquals(24 * 60 * 60 * 1000L, ChartMath.computeLabelInterval(7 * 24 * 60 * 60 * 1000L))
        assertEquals(7 * 24 * 60 * 60 * 1000L, ChartMath.computeLabelInterval(6L * 30 * 24 * 60 * 60 * 1000))
    }
}

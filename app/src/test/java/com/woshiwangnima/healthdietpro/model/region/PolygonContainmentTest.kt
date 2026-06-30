package com.woshiwangnima.healthdietpro.model.region

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolygonContainmentTest {

    // unit square 10x10 at origin, closed ring (first==last)
    private val square = listOf(
        doubleArrayOf(0.0, 0.0), doubleArrayOf(10.0, 0.0),
        doubleArrayOf(10.0, 10.0), doubleArrayOf(0.0, 10.0),
        doubleArrayOf(0.0, 0.0)
    )

    @Test fun pointInsideSquareIsContained() {
        assertTrue(PointInPolygon.contains(square, 5.0, 5.0))
    }

    @Test fun pointOutsideSquareIsNotContained() {
        assertFalse(PointInPolygon.contains(square, 11.0, 5.0))
    }

    @Test fun pointInsideConcaveNotchExcluded() {
        val cShape = listOf(
            doubleArrayOf(0.0, 0.0), doubleArrayOf(10.0, 0.0),
            doubleArrayOf(10.0, 4.0), doubleArrayOf(4.0, 4.0),
            doubleArrayOf(4.0, 6.0), doubleArrayOf(10.0, 6.0),
            doubleArrayOf(10.0, 10.0), doubleArrayOf(0.0, 10.0),
            doubleArrayOf(0.0, 0.0)
        )
        // point in the notch should be outside
        assertFalse(PointInPolygon.contains(cShape, 7.0, 5.0))
        // point in the left wall should be inside
        assertTrue(PointInPolygon.contains(cShape, 2.0, 5.0))
    }
}
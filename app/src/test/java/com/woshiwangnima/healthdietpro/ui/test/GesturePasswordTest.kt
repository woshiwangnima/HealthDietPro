package com.woshiwangnima.healthdietpro.ui.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GesturePasswordTest {
    @Test
    fun zPatternIsAccepted() {
        assertTrue(isTestGesturePassword(listOf(1, 3, 7, 9)))
    }

    @Test
    fun zPatternIncludesSkippedPoints() {
        assertEquals(listOf(1, 2, 3, 5, 7, 8, 9), normalizeGesturePattern(listOf(1, 3, 7, 9)))
    }

    @Test
    fun otherPatternsAreRejected() {
        assertFalse(isTestGesturePassword(listOf(1, 2, 3, 6, 9)))
    }

    @Test
    fun fastDiagonalMovementCollectsItsCrossedCenterPoint() {
        val points = gesturePointsCrossed(
            from = GestureCoordinate(225f, 75f),
            to = GestureCoordinate(75f, 225f),
            size = 300f,
            excluded = setOf(3),
        )

        assertEquals(listOf(5, 7), points)
    }

    @Test
    fun pointsOutsideTheHitRadiusAreIgnored() {
        assertEquals(
            null,
            gesturePointAt(GestureCoordinate(0f, 0f), size = 300f),
        )
    }
}

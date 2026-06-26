package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class LineStyleTest {

    @Test
    fun fromSpinnerPosition_mapsAllFiveSlots() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(0))
        assertEquals(LineStyle.BEZIER, LineStyle.fromSpinnerPosition(1))
        assertEquals(LineStyle.SPLINE, LineStyle.fromSpinnerPosition(2))
        assertEquals(LineStyle.STEPPED_FRONT, LineStyle.fromSpinnerPosition(3))
        assertEquals(LineStyle.STEPPED_BACK, LineStyle.fromSpinnerPosition(4))
    }

    @Test
    fun fromSpinnerPosition_outOfRangeDefaultsToLinear() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(-1))
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(99))
    }

    @Test
    fun toSpinnerPosition_roundTrip() {
        val all = LineStyle.entries
        for (style in all) {
            assertEquals(style, LineStyle.fromSpinnerPosition(LineStyle.toSpinnerPosition(style)))
        }
    }
}

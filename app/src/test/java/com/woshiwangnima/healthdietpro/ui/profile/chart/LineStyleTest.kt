package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class LineStyleTest {

    @Test
    fun fromSpinnerPosition_mapsAllFourSlots() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(0))
        assertEquals(LineStyle.CUBIC_BEZIER, LineStyle.fromSpinnerPosition(1))
        assertEquals(LineStyle.STEPPED, LineStyle.fromSpinnerPosition(2))
    }

    @Test
    fun fromSpinnerPosition_outOfRangeDefaultsToLinear() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(-1))
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(99))
    }

    @Test
    fun toSpinnerPosition_roundTrip() {
        val all = listOf(LineStyle.LINEAR, LineStyle.CUBIC_BEZIER, LineStyle.STEPPED)
        for (style in all) {
            assertEquals(style, LineStyle.fromSpinnerPosition(LineStyle.toSpinnerPosition(style)))
        }
    }
}

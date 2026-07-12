package com.woshiwangnima.healthdietpro.ui.profile.chart

import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BmiUtilTest {

    @Test
    fun buildBmiDataPointsOrdersMixedDateFormatsByTime() {
        val weights = listOf(
            BodyRecord(date = "2026-07-11 09:30", value = 72f, unit = "kg"),
            BodyRecord(date = "2026-07-10", value = 70f, unit = "kg"),
        )
        val heights = listOf(
            BodyRecord(date = "2026-07-11 08:00", value = 170f, unit = "cm"),
            BodyRecord(date = "2026-07-09", value = 169f, unit = "cm"),
        )

        val points = BmiUtil.buildBmiDataPoints(weights, heights)

        assertEquals(listOf("07-10 00:00", "07-11 08:00", "07-11 09:30"), points.map { it.dateLabel })
        assertTrue(points.zipWithNext().all { it.first.timestamp <= it.second.timestamp })
    }
}

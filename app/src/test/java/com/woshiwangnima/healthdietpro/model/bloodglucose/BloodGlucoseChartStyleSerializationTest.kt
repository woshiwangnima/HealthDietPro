package com.woshiwangnima.healthdietpro.model.bloodglucose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BloodGlucoseChartStyleSerializationTest {
    @Test
    fun chartStyle_roundTripPreservesAllCustomizedSampleStyles() {
        val style = BloodGlucoseChartStylePrefs(
            primary = BloodGlucoseSeriesStylePrefs(
                colorArgb = 0xFF123456,
                alpha = 0.34f,
                visible = false,
                lineStyle = "SteppedFront",
                linePattern = "Dashed",
                pointShape = "Diamond",
                pointFill = "Hollow",
            ),
            delayed = BloodGlucoseSeriesStylePrefs(
                colorArgb = 0xFF654321,
                alpha = 0.76f,
                visible = true,
                lineStyle = "Monotone",
                linePattern = "DotDashed",
                pointShape = "Cross",
                pointFill = "Filled",
            ),
            bars = mapOf(
                "Medication" to BloodGlucoseBarStylePrefs(0xFF112233, 0.11f, 0.22f, false),
                "Diet" to BloodGlucoseBarStylePrefs(0xFF223344, 0.33f, 0.44f, true),
                "Exercise" to BloodGlucoseBarStylePrefs(0xFF334455, 0.55f, 0.66f, false),
                "Sleep" to BloodGlucoseBarStylePrefs(0xFF445566, 0.77f, 0.88f, true),
            ),
        )

        val encoded = encodeBloodGlucoseChartStyle(style)

        assertTrue(encoded.contains("pointFill"))
        assertEquals(style, decodeBloodGlucoseChartStyle(encoded))
    }
}

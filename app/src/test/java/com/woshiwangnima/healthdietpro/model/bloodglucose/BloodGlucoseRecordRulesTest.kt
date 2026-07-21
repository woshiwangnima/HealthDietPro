package com.woshiwangnima.healthdietpro.model.bloodglucose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloodGlucoseRecordRulesTest {
    @Test
    fun `validates positive finite glucose values`() {
        assertTrue(isValidBloodGlucoseValue(5.6))
        assertFalse(isValidBloodGlucoseValue(0.0))
        assertFalse(isValidBloodGlucoseValue(-1.0))
        assertFalse(isValidBloodGlucoseValue(Double.NaN))
        assertFalse(isValidBloodGlucoseValue(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `normalizes timestamps to minute precision`() {
        assertTrue(normalizeBloodGlucoseTimestamp(125_999L) == 120_000L)
    }
}

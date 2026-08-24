package com.woshiwangnima.healthdietpro.model.bloodglucose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DietEventGlucoseAnalysisTest {
    @Test
    fun analysis_usesFixedMealStartWindowAndObservedSamplesOnly() {
        val hour = 60 * 60 * 1_000L
        val mealStart = 10 * hour
        val analysis = analyzeDietEventGlucose(
            mealStart,
            listOf(
                record(8 * hour, 4.0),
                record(9 * hour + 30 * 60_000L, 5.0),
                record(10 * hour, 6.0),
                record(11 * hour, 8.0),
                record(12 * hour + 20 * 60_000L, 7.0),
                record(14 * hour + 1L, 9.0),
            ),
        )

        assertEquals(9 * hour, analysis.windowStart)
        assertEquals(14 * hour, analysis.windowEnd)
        assertEquals(4, analysis.records.size)
        assertEquals(5.0, analysis.preMeal?.valueMmolPerL ?: 0.0, 0.0)
        assertEquals(8.0, analysis.postMealPeak?.valueMmolPerL ?: 0.0, 0.0)
        assertEquals(7.0, analysis.postMealTwoHours?.valueMmolPerL ?: 0.0, 0.0)
        assertEquals(3.0, analysis.glycemicRiseMmolPerL ?: 0.0, 0.0)
        assertEquals(2.0, analysis.variabilityMmolPerL ?: 0.0, 0.0)
        assertEquals(5.0, analysis.observedAtOffsetHours(-1)?.valueMmolPerL ?: 0.0, 0.0)
        assertEquals(6.0, analysis.observedAtOffsetHours(0)?.valueMmolPerL ?: 0.0, 0.0)
        assertEquals(8.0, analysis.observedAtOffsetHours(1)?.valueMmolPerL ?: 0.0, 0.0)
        assertEquals(7.0, analysis.observedAtOffsetHours(2)?.valueMmolPerL ?: 0.0, 0.0)
    }

    @Test
    fun analysis_leavesTwoHourMetricEmptyWithoutNearbyObservedSample() {
        val hour = 60 * 60 * 1_000L
        val mealStart = 10 * hour
        val analysis = analyzeDietEventGlucose(
            mealStart,
            listOf(record(9 * hour, 5.0), record(13 * hour, 7.0)),
        )

        assertNull(analysis.postMealTwoHours)
        assertNull(analysis.variabilityMmolPerL)
    }

    @Test
    fun analysis_recoversOnlyAfterPeakWhenAnObservedValueReturnsToPreMealLevel() {
        val hour = 60 * 60 * 1_000L
        val mealStart = 10 * hour
        val analysis = analyzeDietEventGlucose(
            mealStart,
            listOf(
                record(9 * hour, 5.0),
                record(10 * hour, 6.0),
                record(11 * hour, 8.0),
                record(12 * hour, 5.0),
            ),
        )

        assertEquals(11 * hour, analysis.postMealPeak?.timestamp ?: 0L)
        assertEquals(12 * hour, analysis.recoveryToPreMeal?.timestamp ?: 0L)
        assertEquals(hour, analysis.timeToPostMealPeakMillis ?: 0L)
        assertEquals(2 * hour, analysis.timeToPreMealRecoveryMillis ?: 0L)
    }

    private fun record(timestamp: Long, value: Double) = BloodGlucoseRecord(
        id = timestamp.toString(),
        timestamp = timestamp,
        valueMmolPerL = value,
    )
}

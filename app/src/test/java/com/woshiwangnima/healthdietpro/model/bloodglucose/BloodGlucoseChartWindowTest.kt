package com.woshiwangnima.healthdietpro.model.bloodglucose

import com.woshiwangnima.healthdietpro.common.range.UnitRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BloodGlucoseChartWindowTest {
    private val hour = 3_600_000L

    @Test
    fun fixedWindow_keepsExactDurationAndIncludesBothPrimaryEdges() {
        val index = BloodGlucoseChartIndex(recordsAt(0, 3, 6))

        val slice = index.scopedSlice(0, 6 * hour, 6 * hour, BloodGlucoseChartWindow.Hours3)

        assertEquals(3 * hour, slice.windowEnd - slice.windowStart)
        assertEquals(listOf(3L, 6L), slice.primary.map { it.timestamp / hour })
    }

    @Test
    fun delayedSeries_usesImmediatelyPrecedingIntervalWithoutDuplicatingPrimaryStart() {
        val index = BloodGlucoseChartIndex(recordsAt(0, 3, 6))

        val slice = index.scopedSlice(0, 6 * hour, 6 * hour, BloodGlucoseChartWindow.Hours3)

        assertEquals(listOf(0L), slice.delayed.map { it.timestamp / hour })
        assertTrue(slice.delayed.none { it.timestamp == slice.windowStart })
    }

    @Test
    fun requestedWindowEnd_isClampedToScopeTimeBounds() {
        val index = BloodGlucoseChartIndex(recordsAt(2, 8))

        val before = index.scopedSlice(0, 10 * hour, 0, BloodGlucoseChartWindow.Hours3)
        val after = index.scopedSlice(0, 10 * hour, 20 * hour, BloodGlucoseChartWindow.Hours3)

        assertEquals(2 * hour, before.windowEnd)
        assertEquals(10 * hour, after.windowEnd)
    }

    @Test
    fun historicalMaximum_comesFromAllCurrentRecordsNotVisibleWindow() {
        val index = BloodGlucoseChartIndex(
            listOf(record(0, 15.0), record(24, 5.0)),
        )

        val slice = index.scopedSlice(24 * hour, 24 * hour, 24 * hour, BloodGlucoseChartWindow.Hours3)

        assertEquals(15.0, slice.historicalMaximum, 0.0)
    }

    @Test
    fun emptyHistory_hasSafeDisplayMaximum() {
        val slice = BloodGlucoseChartIndex(emptyList()).scopedSlice(0, hour, hour, BloodGlucoseChartWindow.Hours3)

        assertEquals(1.0, slice.historicalMaximum, 0.0)
        assertTrue(slice.primary.isEmpty())
        assertTrue(slice.delayed.isEmpty())
    }

    @Test
    fun timeInRange_splitsObservedSegmentsAtBothTargetBoundaries() {
        val distribution = calculateGlucoseTimeRangeDistribution(
            records = listOf(record(0, 3.0), record(4, 11.0)),
            targetRange = UnitRange(4f, true, 10f, true, "glucose", "mmol_l"),
        )

        assertEquals(30 * 60_000L, distribution.lowMillis)
        assertEquals(3 * hour, distribution.inRangeMillis)
        assertEquals(30 * 60_000L, distribution.highMillis)
        assertEquals(4 * hour, distribution.coveredMillis)
    }

    @Test
    fun timeInRange_excludesTimeWithoutTwoObservedEndpoints() {
        val distribution = calculateGlucoseTimeRangeDistribution(
            records = listOf(record(0, 5.0)),
            targetRange = UnitRange(4f, true, 10f, true, "glucose", "mmol_l"),
        )

        assertEquals(0L, distribution.coveredMillis)
    }

    @Test
    fun particleLevel_mapsBloodGlucoseChangeRateToNineDirectionLevels() {
        val previous = record(0, 5.0)

        assertEquals(-4, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 4.82)))
        assertEquals(-3, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 4.83)))
        assertEquals(-2, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 4.87)))
        assertEquals(-1, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 4.91)))
        assertEquals(0, bloodGlucoseParticleLevel(previous, record(1, 5.0)))
        assertEquals(1, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 5.09)))
        assertEquals(2, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 5.13)))
        assertEquals(3, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 5.17)))
        assertEquals(4, bloodGlucoseParticleLevel(previous, recordAtMinutes(1, 5.18)))
    }

    private fun recordsAt(vararg hours: Long): List<BloodGlucoseRecord> = hours.map { record(it) }

    private fun record(hours: Long, value: Double = 5.0): BloodGlucoseRecord = BloodGlucoseRecord(
        id = hours.toString(),
        timestamp = hours * hour,
        valueMmolPerL = value,
    )

    private fun recordAtMinutes(minutes: Long, value: Double): BloodGlucoseRecord = BloodGlucoseRecord(
        id = "minutes_$minutes",
        timestamp = minutes * 60_000L,
        valueMmolPerL = value,
    )
}

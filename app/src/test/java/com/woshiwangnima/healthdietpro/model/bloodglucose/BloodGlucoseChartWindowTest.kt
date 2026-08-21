package com.woshiwangnima.healthdietpro.model.bloodglucose

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
    fun requestedWindowEnd_isClampedToScopeRecordBounds() {
        val index = BloodGlucoseChartIndex(recordsAt(2, 8))

        val before = index.scopedSlice(0, 10 * hour, 0, BloodGlucoseChartWindow.Hours3)
        val after = index.scopedSlice(0, 10 * hour, 20 * hour, BloodGlucoseChartWindow.Hours3)

        assertEquals(2 * hour, before.windowEnd)
        assertEquals(8 * hour, after.windowEnd)
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

    private fun recordsAt(vararg hours: Long): List<BloodGlucoseRecord> = hours.map { record(it) }

    private fun record(hours: Long, value: Double = 5.0): BloodGlucoseRecord = BloodGlucoseRecord(
        id = hours.toString(),
        timestamp = hours * hour,
        valueMmolPerL = value,
    )
}

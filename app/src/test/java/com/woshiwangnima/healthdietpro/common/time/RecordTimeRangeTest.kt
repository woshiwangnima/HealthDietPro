package com.woshiwangnima.healthdietpro.common.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RecordTimeRangeTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = LocalDateTime.of(2026, 8, 1, 15, 30).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `today begins at local midnight and includes the whole local day`() {
        val range = RecordTimeRangePreset.TODAY.resolve(now, zone)
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0).atZone(zone).toInstant().toEpochMilli(), range.startMillis)
        assertEquals(LocalDateTime.of(2026, 8, 2, 0, 0).atZone(zone).toInstant().toEpochMilli(), range.endMillis)
    }

    @Test
    fun `last 24 hours rolls backward from now rather than local midnight`() {
        val range = RecordTimeRangePreset.LAST_24_HOURS.resolve(now, zone)
        assertEquals(now - 24 * 60 * 60 * 1000L, range.startMillis)
        assertEquals(now, range.endMillis)
    }

    @Test
    fun `range filtering includes both endpoints`() {
        val range = RecordTimeRange(100L, 200L)
        assertTrue(range.contains(100L))
        assertTrue(range.contains(200L))
        assertFalse(range.contains(99L))
    }

    @Test
    fun `all starts at the archive epoch and ends now`() {
        val range = RecordTimeRangePreset.ALL.resolve(now, zone)
        assertEquals(0L, range.startMillis)
        assertEquals(now, range.endMillis)
    }

    @Test
    fun `rolling preset selection resolves against the latest current time`() {
        val later = now + 90_000L
        val selection = RecordTimeRangeSelection.Preset(RecordTimeRangePreset.TODAY)

        assertEquals(LocalDateTime.of(2026, 8, 2, 0, 0).atZone(zone).toInstant().toEpochMilli(), selection.resolve(later, zone).endMillis)
    }

    @Test
    fun `custom selection retains its fixed endpoints`() {
        val selection = RecordTimeRangeSelection.Custom(RecordTimeRange(100L, 200L))

        assertEquals(RecordTimeRange(100L, 200L), selection.resolve(now, zone))
    }
}

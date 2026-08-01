package com.woshiwangnima.healthdietpro.common.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecordTimeTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val source = LocalDateTime.of(2026, 8, 1, 12, 34, 56, 789_000_000)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `normalizes timestamps to requested precision`() {
        assertEquals("2026-08-01", formatRecordTimestamp(source, RecordTimePrecision.DATE, zoneId))
        assertEquals("2026-08-01 12:34", formatRecordTimestamp(normalizeRecordTimestamp(source, RecordTimePrecision.MINUTE, zoneId), RecordTimePrecision.MINUTE, zoneId))
        assertEquals("2026-08-01 12:34:56", formatRecordTimestamp(normalizeRecordTimestamp(source, RecordTimePrecision.SECOND, zoneId), RecordTimePrecision.SECOND, zoneId))
    }

    @Test
    fun `formats each picker precision consistently`() {
        assertEquals("2026-08-01", formatRecordTimestamp(source, RecordTimePrecision.DATE, zoneId))
        assertEquals("2026-08-01 12:34", formatRecordTimestamp(source, RecordTimePrecision.MINUTE, zoneId))
        assertEquals("2026-08-01 12:34:56", formatRecordTimestamp(source, RecordTimePrecision.SECOND, zoneId))
    }

    @Test
    fun `builds local date range boundaries`() {
        val date = LocalDate.of(2026, 8, 1)
        assertEquals(86_400_000L, recordDateEndExclusiveMillis(date, zoneId) - recordDateStartMillis(date, zoneId))
    }
}

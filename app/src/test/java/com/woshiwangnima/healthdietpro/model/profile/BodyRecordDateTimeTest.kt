package com.woshiwangnima.healthdietpro.model.profile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class BodyRecordDateTimeTest {

    @Test
    fun oldDateFormatParsesAtStartOfDay() {
        assertEquals(
            LocalDateTime.of(2026, 7, 11, 0, 0),
            parseBodyRecordDateTime("2026-07-11"),
        )
    }

    @Test
    fun dateTimeFormatPreservesHourAndMinute() {
        assertEquals(
            LocalDateTime.of(2026, 7, 11, 8, 35),
            parseBodyRecordDateTime("2026-07-11 08:35"),
        )
    }

    @Test
    fun formatUsesDateWithHourAndMinute() {
        assertEquals(
            "2026-07-11 08:35",
            formatBodyRecordDateTime(LocalDateTime.of(2026, 7, 11, 8, 35)),
        )
    }

    @Test
    fun displayFormatIncludesHourAndMinute() {
        assertEquals("07-11 08:35", formatBodyRecordDisplayDateTime("2026-07-11 08:35"))
    }
}

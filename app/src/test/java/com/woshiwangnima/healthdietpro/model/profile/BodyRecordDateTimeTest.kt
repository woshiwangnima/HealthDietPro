package com.woshiwangnima.healthdietpro.model.profile

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class BodyRecordDateTimeTest {

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

    @Test
    fun legacyDateOnlyRecordMigratesToStartOfDay() {
        val migrated = migrateBodyRecordDateTime(
            BodyRecord("2026-07-11", 70f, "kg", 0L),
        )

        assertEquals("2026-07-11 00:00", migrated.date)
        assertEquals(bodyRecordEpochMillis("2026-07-11 00:00"), migrated.recordedAtMillis)
    }
}

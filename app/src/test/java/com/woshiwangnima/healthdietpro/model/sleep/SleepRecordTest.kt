package com.woshiwangnima.healthdietpro.model.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepRecordTest {

    private fun record(
        id: String = "s1",
        start: Long = 1_000L * 60 * 60,
        wake: Long? = 3_000L * 60 * 60,
        recorded: Long = 4_000L * 60 * 60,
    ) = SleepRecord(id = id, kind = SleepKind.NIGHT_SLEEP, sleepStartAt = start, wakeUpAt = wake, recordedAt = recorded)

    @Test
    fun `duration derives from wake minus start`() {
        assertEquals(120L, record().durationMinutes())
        assertNull(record(wake = null).durationMinutes())
    }

    @Test
    fun `migration trims note and deduplicates ids`() {
        val archive = SleepArchive(
            schemaVersion = 0,
            records = listOf(
                record(id = " a ", start = 1000L, wake = null, recorded = 1000L).copy(note = "  hi  "),
                record(id = " a ", start = 2000L, wake = null, recorded = 2000L),
            ),
        )
        val migrated = migrateSleepArchive(archive)
        assertEquals(SLEEP_ARCHIVE_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(1, migrated.records.size)
        assertEquals("a", migrated.records.first().id)
        assertEquals("hi", migrated.records.first().note)
    }

    @Test
    fun `validation accepts valid archive`() {
        validateSleepArchive(
            SleepArchive(
                records = listOf(record()),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects duplicate ids`() {
        validateSleepArchive(SleepArchive(records = listOf(record(), record())))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects wake before start`() {
        validateSleepArchive(SleepArchive(records = listOf(record(start = 5000L, wake = 1000L))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects duration over 48 hours`() {
        validateSleepArchive(SleepArchive(records = listOf(record(start = 0L, wake = 49L * 60L * 60L * 1000L, recorded = 0L))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects missing start time`() {
        validateSleepArchive(SleepArchive(records = listOf(record(start = 0L, wake = null, recorded = 1000L))))
    }
}
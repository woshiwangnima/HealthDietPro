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

    @Test
    fun `default nocturia empty for count zero`() {
        assertEquals(emptyList<NocturiaRecord>(), generateDefaultNocturia(0, 1000L, 5000L))
        assertEquals(emptyList<NocturiaRecord>(), generateDefaultNocturia(-1, 1000L, 5000L))
    }

    @Test
    fun `default nocturia distributes evenly and ends ten minutes after start`() {
        val start = 1_000L * 60 * 60
        val wake = 9_000L * 60 * 60
        val records = generateDefaultNocturia(3, start, wake)
        assertEquals(3, records.size)
        val duration = NOCTURIA_DEFAULT_DURATION_MINUTES * 60_000L
        records.forEach { entry ->
            assertEquals(duration, entry.endAt - entry.startAt)
        }
        assertEquals(records.map(NocturiaRecord::startAt), records.map(NocturiaRecord::startAt).sorted())
        assertTrue(records.first().startAt >= start)
        assertTrue(records.last().startAt <= wake)
    }

    @Test
    fun `migration adds nocturia field defaults`() {
        val legacy = SleepArchive(
            schemaVersion = 1,
            records = listOf(record()),
        )
        val migrated = migrateSleepArchive(legacy)
        assertEquals(SLEEP_ARCHIVE_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(emptyList<NocturiaRecord>(), migrated.records.first().nocturia)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects nocturia end before start`() {
        validateSleepArchive(
            SleepArchive(
                records = listOf(
                    record().copy(
                        nocturia = listOf(NocturiaRecord(startAt = 2000L, endAt = 1000L)),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `default times before sleep uses now as start`() {
        val prefs = SleepPrefs(nightDefaultMinutes = 8 * 60, nightTiming = SleepRecordTiming.BEFORE_SLEEP)
        val (start, wake) = defaultSleepTimes(prefs, SleepKind.NIGHT_SLEEP, 1000L)
        assertEquals(1000L, start)
        assertEquals(1000L + 8 * 60 * 60_000L, wake)
    }

    @Test
    fun `default times after wake uses now as wake`() {
        val prefs = SleepPrefs(nightDefaultMinutes = 90, napTiming = SleepRecordTiming.AFTER_WAKE)
        val (start, wake) = defaultSleepTimes(prefs, SleepKind.NAP, 5000L)
        assertEquals(5000L - 90 * 60_000L, start)
        assertEquals(5000L, wake)
    }

    @Test
    fun `default times uses kind specific duration and timing`() {
        val prefs = SleepPrefs(
            nightDefaultMinutes = 8 * 60,
            napDefaultMinutes = 90,
            nightTiming = SleepRecordTiming.BEFORE_SLEEP,
            napTiming = SleepRecordTiming.AFTER_WAKE,
        )
        val night = defaultSleepTimes(prefs, SleepKind.NIGHT_SLEEP, 1000L)
        val nap = defaultSleepTimes(prefs, SleepKind.NAP, 1000L)
        assertEquals(1000L, night.first)
        assertEquals(1000L + 8 * 60 * 60_000L, night.second)
        assertEquals(1000L - 90 * 60_000L, nap.first)
        assertEquals(1000L, nap.second)
    }
}
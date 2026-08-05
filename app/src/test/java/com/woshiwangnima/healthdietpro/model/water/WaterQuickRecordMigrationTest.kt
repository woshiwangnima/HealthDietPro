package com.woshiwangnima.healthdietpro.model.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WaterQuickRecordMigrationTest {
    @Test
    fun v1QuickRecordsReceiveStableDistinctPresetIds() {
        val v1Archive = WaterArchive(
            schemaVersion = 1,
            quickRecords = listOf(
                WaterQuickRecord(beverageId = "food:water:drinking", volume = 250.0),
                WaterQuickRecord(beverageId = "food:tea:green", volume = 350.0, unit = WaterVolumeUnit.ML),
            ),
        )

        val migrated = migrateWaterArchive(v1Archive)

        assertEquals(WATER_ARCHIVE_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(v1Archive.quickRecords.map(WaterQuickRecord::beverageId), migrated.quickRecords.map(WaterQuickRecord::beverageId))
        assertTrue(migrated.quickRecords.all { it.id.isNotBlank() })
        assertEquals(2, migrated.quickRecords.map(WaterQuickRecord::id).distinct().size)
        assertEquals(migrated, migrateWaterArchive(v1Archive))
        assertTrue(migrated.quickRecords.all { it.beverageNameSuffix.isEmpty() })
    }

    @Test
    fun suffixIsTrimmedDuringArchiveMigration() {
        val migrated = migrateWaterArchive(
            WaterArchive(quickRecords = listOf(WaterQuickRecord(id = "tea", beverageId = "food:tea:green", beverageNameSuffix = "  半杯  "))),
        )

        assertEquals("半杯", migrated.quickRecords.single().beverageNameSuffix)
    }

    @Test
    fun sameBeverageCanHaveSeparateVolumePresets() {
        val archive = migrateWaterArchive(
            WaterArchive(
                quickRecords = listOf(
                    WaterQuickRecord(id = "water-small", beverageId = "food:water:drinking", volume = 250.0),
                    WaterQuickRecord(id = "water-large", beverageId = "food:water:drinking", volume = 500.0),
                ),
            ),
        )

        assertEquals(2, archive.quickRecords.size)
        assertEquals(1, archive.quickRecords.map(WaterQuickRecord::beverageId).distinct().size)
        assertNotEquals(archive.quickRecords[0].id, archive.quickRecords[1].id)
    }

    @Test
    fun reorderPreservesEveryPresetAndMakesFirstItemTheDefault() {
        val quickRecords = listOf(
            WaterQuickRecord(id = "tea-small", beverageId = "food:tea:green", volume = 250.0),
            WaterQuickRecord(id = "water-large", beverageId = "food:water:drinking", volume = 500.0),
            WaterQuickRecord(id = "tea-large", beverageId = "food:tea:green", volume = 450.0),
        )

        val reordered = reorderWaterQuickRecords(quickRecords, listOf("water-large", "tea-small", "tea-large"))

        assertEquals("water-large", reordered.first().id)
        assertEquals(listOf("water-large", "tea-small", "tea-large"), reordered.map(WaterQuickRecord::id))
        assertEquals(quickRecords.toSet(), reordered.toSet())
    }

    @Test
    fun reorderRejectsMissingDuplicateOrUnknownIds() {
        val quickRecords = listOf(
            WaterQuickRecord(id = "first", beverageId = "food:water:drinking"),
            WaterQuickRecord(id = "second", beverageId = "food:tea:green"),
        )

        listOf(listOf("first"), listOf("first", "first"), listOf("first", "unknown")).forEach { ids ->
            try {
                reorderWaterQuickRecords(quickRecords, ids)
                fail("Expected invalid order to be rejected")
            } catch (_: IllegalArgumentException) {
                // Expected: a persisted order must be a permutation of the current preset ids.
            }
        }
    }
}

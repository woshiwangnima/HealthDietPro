package com.woshiwangnima.healthdietpro.model.water

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
}

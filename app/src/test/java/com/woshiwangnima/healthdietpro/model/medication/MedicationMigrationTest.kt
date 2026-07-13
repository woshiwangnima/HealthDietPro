package com.woshiwangnima.healthdietpro.model.medication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationMigrationTest {
    @Test
    fun groupsNormalizedNamesAndUsesNewestRecordForCatalogDefaults() {
        val older = record("old", 10L, " Aspirin ", 100f, "oral")
        val newer = record("new", 20L, "aspirin", 200f, "topical")

        val result = MedicationMigration.migrate(emptyList(), listOf(older, newer))

        assertEquals(1, result.catalog.size)
        assertEquals("aspirin", result.catalog.single().name)
        assertEquals(200f, result.catalog.single().specValue)
        assertEquals("topical", result.catalog.single().defaultMethod)
        assertTrue(result.records.all { it.medicationId == result.catalog.single().id })
        assertEquals(" Aspirin ", result.records[0].medicationName)
    }

    @Test
    fun migrationIsIdempotentAndLeavesBlankLegacyNamesUnlinked() {
        val first = MedicationMigration.migrate(emptyList(), listOf(record("one", 10L, "", 1f, "oral"), record("two", 20L, "Metformin", 2f, "oral")))
        val second = MedicationMigration.migrate(first.catalog, first.records)

        assertEquals(1, first.catalog.size)
        assertEquals(null, first.records.first().medicationId)
        assertFalse(second.changed)
        assertEquals(first.catalog, second.catalog)
        assertEquals(first.records, second.records)
    }

    @Test
    fun archivedCatalogItemRemainsLinkedToEvents() {
        val catalog = MedicationCatalogItem(id = "m1", name = "Aspirin", archived = true)
        val event = record("event", 10L, "Aspirin", 100f, "oral").copy(medicationId = catalog.id)

        val result = MedicationMigration.migrate(listOf(catalog), listOf(event))

        assertFalse(result.changed)
        assertTrue(result.catalog.single().archived)
        assertEquals("m1", result.records.single().medicationId)
    }

    @Test
    fun preservesEventSnapshotsWhileLinkingRecords() {
        val legacy = record("event", 10L, "Aspirin", 100f, "oral").copy(
            manufacturer = "Legacy manufacturer",
            medicationImagePath = "legacy-medicine.jpg",
        )

        val result = MedicationMigration.migrate(emptyList(), listOf(legacy))

        assertEquals("Legacy manufacturer", result.records.single().manufacturer)
        assertEquals("legacy-medicine.jpg", result.records.single().medicationImagePath)
        assertEquals("photo.jpg", result.records.single().photoPath)
        assertEquals("oral", result.records.single().method)
    }

    private fun record(id: String, timestamp: Long, name: String, specValue: Float, method: String) =
        MedicationRecord(
            id = id,
            timestamp = timestamp,
            medicationName = name,
            doseValue = 1f,
            doseUnit = "tablet",
            specValue = specValue,
            specUnitCategory = "weight",
            specUnitId = "mg",
            method = method,
            feelings = listOf("okay"),
            feelingNote = "preserved",
            photoPath = "photo.jpg",
        )
}

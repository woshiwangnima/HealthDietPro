package com.woshiwangnima.healthdietpro.model.medication

import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationArchiveTest {
    @Test
    fun `archive keeps catalog and records together`() {
        val item = MedicationCatalogItem(id = "catalog-1", name = "Medicine")
        val record = MedicationRecord(
            id = "record-1",
            timestamp = 1_785_600_000_000,
            medicationName = "Medicine",
            doseValue = 1f,
            doseUnit = "tablet",
            specValue = 10f,
            specUnitCategory = "weight",
            specUnitId = "mg",
            method = "oral",
            medicationId = item.id,
        )

        val archive = MedicationArchive(catalog = listOf(item), records = listOf(record))

        assertEquals(item.id, archive.records.single().medicationId)
        assertEquals(record.id, archive.records.single().id)
    }
}

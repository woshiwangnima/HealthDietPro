package com.woshiwangnima.healthdietpro.model.medication

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationSerializationTest {
    @Test
    fun serializesCurrentMedicationSchemaWithEnumStringsAndImageArrays() {
        val item = MedicationCatalogItem(
            id = "medicine-1",
            name = "Aspirin",
            imagePaths = listOf("medication_catalog_images/aspirin.jpg"),
            frequency = MedicationFrequency(
                type = MedicationFrequencyType.AS_NEEDED,
                interval = 30,
                unit = MedicationFrequencyUnit.MINUTE,
                times = 1,
            ),
            intakeRules = listOf(MedicationIntakeRule(MedicationTimingAnchor.DINNER, 60)),
        )

        val encoded = Json.encodeToString(item)
        val decoded = Json.decodeFromString<MedicationCatalogItem>(encoded)

        assertTrue(encoded.contains("\"type\":\"as_needed\""))
        assertTrue(encoded.contains("\"unit\":\"min\""))
        assertTrue(encoded.contains("\"anchor\":\"dinner\""))
        assertTrue(encoded.contains("\"offsetMinutes\":60"))
        assertTrue(encoded.contains("\"imagePaths\""))
        assertEquals(item, decoded)
    }
}

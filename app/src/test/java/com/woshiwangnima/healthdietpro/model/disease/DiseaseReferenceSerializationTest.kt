package com.woshiwangnima.healthdietpro.model.disease

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiseaseReferenceSerializationTest {
    @Test
    fun `curated reference writes source and versioned ICD ID`() {
        val encoded = Json.encodeToString<DiseaseReference>(
            DiseaseReference(DiseaseSourceKind.CURATED, "ICD-11-BA00"),
        )

        assertTrue(encoded.contains("\"sourceKind\":\"CURATED\""))
        assertTrue(encoded.contains("\"diseaseId\":\"ICD-11-BA00\""))
        assertFalse(encoded.contains("curatedDiseaseId"))
        assertEquals(
            DiseaseReference(DiseaseSourceKind.CURATED, "ICD-11-BA00"),
            Json.decodeFromString<DiseaseReference>(encoded),
        )
    }

    @Test
    fun `custom reference writes a prefixed ID`() {
        val decoded = DiseaseReference(DiseaseSourceKind.CUSTOM, "CUSTOM-1B314519")

        assertEquals("CUSTOM-1B314519", decoded.diseaseId)
    }
}

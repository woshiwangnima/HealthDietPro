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
    fun `curated reference writes a discriminated ICD family map`() {
        val encoded = Json.encodeToString<DiseaseReference>(
            DiseaseReference.Curated(mapOf("11" to "diabetes_type_2")),
        )

        assertTrue(encoded.contains("\"kind\":\"curated\""))
        assertTrue(encoded.contains("\"11\":\"diabetes_type_2\""))
        assertFalse(encoded.contains("customDiseaseId"))
        assertEquals(
            DiseaseReference.Curated(mapOf("11" to "diabetes_type_2")),
            Json.decodeFromString<DiseaseReference>(encoded),
        )
    }

    @Test
    fun `legacy scalar curated reference migrates to ICD 11 map`() {
        val decoded = Json.decodeFromString<DiseaseReference>("""{"curatedDiseaseId":"hypertension"}""")

        assertEquals(DiseaseReference.Curated(mapOf("11" to "hypertension")), decoded)
    }

    @Test
    fun `legacy custom reference migrates to custom union branch`() {
        val decoded = Json.decodeFromString<DiseaseReference>("""{"customDiseaseId":"custom:123"}""")

        assertEquals(DiseaseReference.Custom("custom:123"), decoded)
    }
}

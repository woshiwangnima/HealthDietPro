package com.woshiwangnima.healthdietpro.assets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiseasesJsonMigrationTest {

    @Test fun catalogHasCurrentSchemaVersionAndNoPrevalenceData() {
        val catalog = readCatalog()

        assertEquals(1, catalog.getInt("schemaVersion"))
        val diseases = catalog.getJSONArray("diseases")
        for (index in 0 until diseases.length()) {
            assertFalse(diseases.getJSONObject(index).has("prevalence"))
        }
    }

    @Test fun everyDiseaseHasLocalizedMetadataAndResolvableReferences() {
        val catalog = readCatalog()
        val categoryIds = catalog.getJSONArray("categories").ids().toSet()
        val departmentIds = catalog.getJSONArray("departments").ids().toSet()
        val sources = catalog.getJSONArray("sources")
        val sourceIds = sources.ids().toSet()
        val sourceVersions = (0 until sources.length()).associate {
            sources.getJSONObject(it).getString("id") to sources.getJSONObject(it).optString("version")
        }
        val diseases = catalog.getJSONArray("diseases")

        for (index in 0 until diseases.length()) {
            val disease = diseases.getJSONObject(index)
            val id = disease.getString("id")
            val i18n = disease.getJSONObject("i18n")
            for (language in listOf("zh", "en")) {
                val localized = i18n.getJSONObject(language)
                assertTrue("$id $language label is blank", localized.getString("label").isNotBlank())
                assertTrue("$id $language description is blank", localized.getString("description").isNotBlank())
            }
            assertTrue("$id requires a category", disease.getJSONArray("categoryIds").length() > 0)
            assertTrue("$id category is unknown", disease.getJSONArray("categoryIds").allIn(categoryIds))
            assertTrue("$id requires a care department", disease.getJSONArray("careDepartmentIds").length() > 0)
            assertTrue("$id department is unknown", disease.getJSONArray("careDepartmentIds").allIn(departmentIds))
            assertTrue("$id requires a source", disease.getJSONArray("sourceIds").length() > 0)
            assertTrue("$id source is unknown", disease.getJSONArray("sourceIds").allIn(sourceIds))

            val references = disease.getJSONArray("icd11References")
            assertTrue("$id requires an ICD-11 reference", references.length() > 0)
            for (referenceIndex in 0 until references.length()) {
                val reference = references.getJSONObject(referenceIndex)
                assertTrue("$id ICD-11 code is blank", reference.getString("code").isNotBlank())
                assertTrue("$id ICD-11 Chinese title is blank", reference.getJSONObject("title").getString("zh").isNotBlank())
                assertTrue("$id ICD-11 English title is blank", reference.getJSONObject("title").getString("en").isNotBlank())
                val matchingSourceVersions = disease.getJSONArray("sourceIds").values()
                    .mapNotNull { sourceVersions[it] }
                assertTrue("$id ICD-11 release has no matching source version", reference.getString("release") in matchingSourceVersions)
            }
        }
    }

    @Test fun type2DiabetesUsesExpectedIcd11Reference() {
        val type2Diabetes = readCatalog().getJSONArray("diseases").firstById("type2_diabetes")
        val icd11 = type2Diabetes.getJSONArray("icd11References").getJSONObject(0)

        assertEquals("05", icd11.getString("chapterCode"))
        assertEquals("5A11", icd11.getString("code"))
    }

    private fun readCatalog(): org.json.JSONObject = org.json.JSONObject(
        java.io.File("src/main/assets/diseases.json").readText(),
    )

    private fun org.json.JSONArray.ids(): Sequence<String> = sequence {
        for (index in 0 until length()) yield(getJSONObject(index).getString("id"))
    }

    private fun org.json.JSONArray.allIn(ids: Set<String>): Boolean =
        (0 until length()).all { getString(it) in ids }

    private fun org.json.JSONArray.values(): List<String> = List(length()) { getString(it) }

    private fun org.json.JSONArray.firstById(id: String): org.json.JSONObject =
        (0 until length()).map { getJSONObject(it) }.first { it.getString("id") == id }
}

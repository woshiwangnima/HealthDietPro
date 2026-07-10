package com.woshiwangnima.healthdietpro.assets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiseasesJsonMigrationTest {

    private val allowedCodes = setOf(
        "11","12","13","14","15","21","22","23","31","32","33","34","35","36","37",
        "41","42","43","44","45","46","50","51","52","53","54","61","62","63","64","65",
        "71","81","82"
    )

    @Test fun allPrevalenceKeysAreGb2260Codes() {
        val raw = java.io.File("src/main/assets/diseases.json").readText()
        val arr = org.json.JSONArray(raw)
        for (i in 0 until arr.length()) {
            val disease = arr.getJSONObject(i)
            if (!disease.has("prevalence")) continue
            val keys = disease.getJSONObject("prevalence").keys()
            while (keys.hasNext()) {
                val k = keys.next()
                assertTrue("prevalence key `$k` is not a GB/T 2260 code", k in allowedCodes)
            }
        }
    }

    @Test fun allDiseasesHaveLocalizedLabels() {
        val raw = java.io.File("src/main/assets/diseases.json").readText()
        val arr = org.json.JSONArray(raw)
        for (i in 0 until arr.length()) {
            val disease = arr.getJSONObject(i)
            val id = disease.getString("id")
            val i18n = disease.getJSONObject("i18n")
            val zh = i18n.getJSONObject("zh")
            val en = i18n.getJSONObject("en")

            assertTrue("$id zh label is blank", zh.getString("label").isNotBlank())
            assertTrue("$id en label is blank", en.getString("label").isNotBlank())
        }
    }

    @Test fun diseaseLabelsIncludeExpectedEnglishValues() {
        val raw = java.io.File("src/main/assets/diseases.json").readText()
        val arr = org.json.JSONArray(raw)
        val labelsById = buildMap {
            for (i in 0 until arr.length()) {
                val disease = arr.getJSONObject(i)
                put(
                    disease.getString("id"),
                    disease.getJSONObject("i18n").getJSONObject("en").getString("label"),
                )
            }
        }

        assertEquals("Hypertension", labelsById["hypertension"])
        assertEquals("Type 2 Diabetes", labelsById["type2_diabetes"])
        assertEquals("Migraine", labelsById["migraine"])
    }
}

package com.woshiwangnima.healthdietpro.assets

import com.woshiwangnima.healthdietpro.model.unit.UnitRepository
import org.junit.Assert.assertFalse
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class UnitsJsonTest {

    @Test
    fun timeCategoryContainsXunBetweenWeekAndMonth() {
        val json = readUnits()
        val time = json.first { it.getString("id") == "time" }
        val ids = time.getJSONArray("units").let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
        }
        val weekIdx = ids.indexOf("week")
        val xunIdx = ids.indexOf("xun")
        val monthIdx = ids.indexOf("month")
        assertTrue("week index must exist", weekIdx >= 0)
        assertTrue("xun index must exist", xunIdx >= 0)
        assertTrue("month index must exist", monthIdx >= 0)
        assertTrue("xun must come after week", xunIdx > weekIdx)
        assertTrue("xun must come before month", xunIdx < monthIdx)
        assertEquals(6, xunIdx)
        assertEquals(1, ids.count { it == "xun" })
    }

    @Test
    fun xunHasCorrectToBase() {
        val json = readUnits()
        val time = json.first { it.getString("id") == "time" }
        val xun = (0 until time.getJSONArray("units").length())
            .map { time.getJSONArray("units").getJSONObject(it) }
            .first { it.getString("id") == "xun" }
        assertEquals(864000.0, xun.getDouble("toBase"), 0.0001)
    }

    @Test
    fun unitsUseI18nSchemaOnly() {
        val json = readUnits()
        json.forEach { category ->
            val categoryId = category.getString("id")
            assertFalse("$categoryId still has categoryCn", category.has("categoryCn"))
            assertFalse("$categoryId still has categoryEn", category.has("categoryEn"))
            assertTrue("$categoryId missing zh label", category.getJSONObject("i18n").getJSONObject("zh").getString("label").isNotBlank())
            assertTrue("$categoryId missing en label", category.getJSONObject("i18n").getJSONObject("en").getString("label").isNotBlank())

            val units = category.getJSONArray("units")
            for (i in 0 until units.length()) {
                val unit = units.getJSONObject(i)
                val unitId = unit.getString("id")
                assertFalse("$categoryId/$unitId still has symbolCn", unit.has("symbolCn"))
                assertFalse("$categoryId/$unitId still has symbolEn", unit.has("symbolEn"))
                assertTrue("$categoryId/$unitId missing zh symbol", unit.getJSONObject("i18n").getJSONObject("zh").getString("symbol").isNotBlank())
                assertTrue("$categoryId/$unitId missing en symbol", unit.getJSONObject("i18n").getJSONObject("en").getString("symbol").isNotBlank())
            }
        }
    }

    @Test
    fun repositoryReadsLocalizedUnitLabels() {
        val repo = UnitRepository.fromAsset("src/main/assets/units.json")
        val weight = repo.getCategory("weight") ?: error("weight category missing")
        val kg = repo.getUnit("weight", "kg") ?: error("kg unit missing")

        assertEquals("Weight", weight.displayName(Locale.ENGLISH))
        assertEquals("kg", kg.symbol(Locale.ENGLISH))
        assertTrue(weight.displayName(Locale.SIMPLIFIED_CHINESE).isNotBlank())
        assertTrue(kg.symbol(Locale.SIMPLIFIED_CHINESE).isNotBlank())
    }

    private fun readUnits(): List<JSONObject> {
        val raw = java.io.File("src/main/assets/units.json").readText()
        val arr = org.json.JSONArray(raw)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }
}

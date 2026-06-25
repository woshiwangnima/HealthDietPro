package com.woshiwangnima.healthdietpro.assets

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    private fun readUnits(): List<JSONObject> {
        val raw = java.io.File("src/main/assets/units.json")
            .readText()
        val arr = org.json.JSONArray(raw)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }
}
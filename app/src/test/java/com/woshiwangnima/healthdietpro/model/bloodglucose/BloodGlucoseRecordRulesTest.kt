package com.woshiwangnima.healthdietpro.model.bloodglucose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.model.unit.UnitStepMode
import com.woshiwangnima.healthdietpro.model.unit.stepSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BloodGlucoseRecordRulesTest {
    companion object {
        @JvmStatic
        @org.junit.BeforeClass
        fun setUpUnits() { UnitConverter.initFromAsset("src/main/assets/units.json") }
    }
    @Test
    fun `validates positive finite glucose values`() {
        assertTrue(isValidBloodGlucoseValue(5.6))
        assertFalse(isValidBloodGlucoseValue(0.0))
        assertFalse(isValidBloodGlucoseValue(-1.0))
        assertFalse(isValidBloodGlucoseValue(Double.NaN))
        assertFalse(isValidBloodGlucoseValue(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `normalizes timestamps to second precision`() {
        assertTrue(normalizeBloodGlucoseTimestamp(125_999L) == 125_000L)
    }

    @Test
    fun `input range converts from base mmol to selected unit`() {
        val mmol = bloodGlucoseInputRange("mmol_l")
        val mg = bloodGlucoseInputRange("mg_dl")
        assertEquals(1.1, mmol.min!!, 0.0001)
        assertEquals(33.3, mmol.max!!, 0.0001)
        assertTrue(mmol.contains(1.1))
        assertEquals(20.0, mg.min!!, 0.0001)
        assertEquals(600.0, mg.max!!, 0.0001)
    }

    @Test
    fun `unit input converts to base value and survives JSON round trip`() {
        val inputMgDl = 100f
        val record = BloodGlucoseRecord(
            id = "record-1",
            timestamp = 1_725_000_000_000L,
            valueMmolPerL = UnitConverter.toBase(UnitCategoryType.Glucose.id, inputMgDl, "mg_dl").toDouble(),
        )
        val decoded = Json.decodeFromString<BloodGlucoseRecord>(Json.encodeToString(record))

        assertEquals(5.55, decoded.valueMmolPerL, 0.0001)
        assertEquals(record, decoded)
    }

    @Test
    fun `normal glucose input steps match the active unit`() {
        assertEquals(0.1f, UnitCategoryType.Glucose.stepSpec("mmol_l").valueFor(UnitStepMode.Normal))
        assertEquals(1f, UnitCategoryType.Glucose.stepSpec("mg_dl").valueFor(UnitStepMode.Normal))
    }
}

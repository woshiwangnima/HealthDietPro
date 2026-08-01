package com.woshiwangnima.healthdietpro.ui.record

import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BloodGlucoseEditorChangeTest {
    private val record = BloodGlucoseRecord(
        id = "record-1",
        timestamp = 1_725_000_000_000L,
        valueMmolPerL = 5.6,
        note = "before breakfast",
    )
    private val initial = BloodGlucoseEditorDraft(
        timestamp = record.timestamp,
        valueText = "5.6",
        unitId = "mmol_l",
        timingAnchor = null,
        relativeMinutesText = "",
        timingRelation = BloodGlucoseTimingRelation.At,
        note = record.note,
    )

    @Test
    fun `unchanged existing record is not dirty`() {
        assertFalse(bloodGlucoseDraftChanged(initial, initial, record, record))
    }

    @Test
    fun `clearing a numeric value creates an unsaved invalid draft`() {
        assertTrue(bloodGlucoseDraftChanged(initial, initial.copy(valueText = ""), record, null))
    }

    @Test
    fun `valid numeric change enables record change detection`() {
        val changed = record.copy(valueMmolPerL = 6.1)
        assertTrue(bloodGlucoseDraftChanged(initial, initial.copy(valueText = "6.1"), record, changed))
    }

    @Test
    fun `changing display unit alone is not a record edit`() {
        assertFalse(bloodGlucoseDraftChanged(initial, initial.copy(unitId = "mg_dl"), record, record))
    }
}

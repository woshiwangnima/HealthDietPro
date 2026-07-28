package com.woshiwangnima.healthdietpro.ui.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordViewModelTest {

    @Test
    fun `initial state exposes record sections and enabled actions`() {
        val sections = defaultRecordSections()

        assertEquals(
            listOf(
                RecordActionId.Height,
                RecordActionId.Weight,
                RecordActionId.BloodGlucose,
                RecordActionId.Waist,
                RecordActionId.Period,
            ),
            sections[0].items.map { it.id },
        )
        assertEquals(
            listOf(
                RecordActionId.Diet,
                RecordActionId.Water,
                RecordActionId.Exercise,
                RecordActionId.Sleep,
                RecordActionId.Bowel,
                RecordActionId.Medication,
                RecordActionId.Habit,
            ),
            sections[1].items.map { it.id },
        )
        assertEquals(listOf(RecordActionId.Feeling), sections[2].items.map { it.id })

        assertTrue(sections[0].items.first { it.id == RecordActionId.Height }.enabled)
        assertTrue(sections[0].items.first { it.id == RecordActionId.Weight }.enabled)
        assertTrue(sections[0].items.first { it.id == RecordActionId.BloodGlucose }.enabled)
        assertTrue(sections[1].items.first { it.id == RecordActionId.Medication }.enabled)
        assertFalse(sections[1].items.first { it.id == RecordActionId.Diet }.enabled)
    }
}

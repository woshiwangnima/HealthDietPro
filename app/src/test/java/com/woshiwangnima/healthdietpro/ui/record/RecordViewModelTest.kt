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
                RecordActionId.BloodType,
                RecordActionId.Disease,
                RecordActionId.Allergy,
            ),
            sections[0].items.map { it.id },
        )
        assertEquals(
            listOf(
                RecordActionId.Height,
                RecordActionId.Weight,
                RecordActionId.BodyFat,
                RecordActionId.Waist,
                RecordActionId.Teeth,
                RecordActionId.Vision,
                RecordActionId.Hearing,
            ),
            sections[1].items.map { it.id },
        )
        assertEquals(
            listOf(
                RecordActionId.BloodGlucose,
                RecordActionId.BloodPressure,
                RecordActionId.HeartRate,
                RecordActionId.Temperature,
                RecordActionId.Period,
            ),
            sections[2].items.map { it.id },
        )
        assertEquals(
            listOf(
                RecordActionId.Diet,
                RecordActionId.Water,
                RecordActionId.Sleep,
                RecordActionId.Exercise,
                RecordActionId.Bowel,
                RecordActionId.Medication,
            ),
            sections[3].items.map { it.id },
        )
        assertEquals(
            listOf(
                RecordActionId.Container,
                RecordActionId.Price,
                RecordActionId.Habit,
                RecordActionId.Feeling,
            ),
            sections[4].items.map { it.id },
        )

        assertTrue(sections[1].items.first { it.id == RecordActionId.Height }.enabled)
        assertTrue(sections[1].items.first { it.id == RecordActionId.Weight }.enabled)
        assertTrue(sections[2].items.first { it.id == RecordActionId.BloodGlucose }.enabled)
        assertTrue(sections[3].items.first { it.id == RecordActionId.Medication }.enabled)
        assertTrue(sections[4].items.first { it.id == RecordActionId.Container }.enabled)
        assertTrue(sections[4].items.first { it.id == RecordActionId.Price }.enabled)
        assertTrue(sections[3].items.first { it.id == RecordActionId.Sleep }.enabled)
        assertTrue(sections[3].items.first { it.id == RecordActionId.Diet }.enabled)
        assertFalse(sections[4].items.first { it.id == RecordActionId.Container }.showSummary)
    }

    @Test
    fun `no disabled actions keeps every section untouched`() {
        val sections = defaultRecordSections().withoutDisabledActions(emptySet())

        assertEquals(5, sections.size)
        assertEquals(defaultRecordSections(), sections)
    }

    @Test
    fun `disabled action is removed from its section only`() {
        val sections = defaultRecordSections()
            .withoutDisabledActions(setOf(RecordActionId.Height, RecordActionId.Price))

        assertEquals(5, sections.size)
        assertEquals(
            listOf(RecordActionId.Weight, RecordActionId.BodyFat, RecordActionId.Waist, RecordActionId.Teeth, RecordActionId.Vision, RecordActionId.Hearing),
            sections[1].items.map { it.id },
        )
        assertEquals(
            listOf(RecordActionId.Container, RecordActionId.Habit, RecordActionId.Feeling),
            sections[4].items.map { it.id },
        )
        assertTrue(sections[0].items.isNotEmpty())
        assertTrue(sections[2].items.isNotEmpty())
        assertTrue(sections[3].items.isNotEmpty())
    }

    @Test
    fun `disabling a whole section empties it without dropping the section`() {
        val sections = defaultRecordSections().withoutDisabledActions(RecordActionId.entries.toSet())

        assertEquals(5, sections.size)
        assertTrue(sections[4].items.isEmpty())
    }

    @Test
    fun `filter keeps unimplemented entries order and only drops named ids`() {
        val sections = defaultRecordSections().withoutDisabledActions(setOf(RecordActionId.Allergy))

        assertEquals(
            listOf(RecordActionId.BloodType, RecordActionId.Disease),
            sections[0].items.map { it.id },
        )
    }
}

package com.woshiwangnima.healthdietpro.model.medication

import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationRecordListOpsTest {
    @Test
    fun removesMedicationRecordByStableId() {
        val first = record("one")
        val second = record("two")

        assertEquals(listOf(second), listOf(first, second).removeRecordById("one"))
    }

    @Test
    fun removesEveryDuplicateIdToPreserveIdUniqueness() {
        val records = listOf(record("same"), record("same"), record("other"))

        assertEquals(listOf(record("other")), records.removeRecordById("same"))
    }

    private fun record(id: String) = MedicationRecord(
        id = id,
        timestamp = 1L,
        medicationName = "测试药品",
        doseValue = 1f,
        doseUnit = "粒",
        specValue = 1f,
        specUnitCategory = "weight",
        specUnitId = "mg",
        method = "口服",
    )
}

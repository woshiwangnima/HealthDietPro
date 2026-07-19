package com.woshiwangnima.healthdietpro.model.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyRecordListOpsTest {
    @Test
    fun removesOnlyTheSelectedDuplicateRecord() {
        val first = BodyRecord("2026-07-19", 170f, "cm")
        val duplicate = BodyRecord("2026-07-19", 170f, "cm")
        val records = listOf(first, duplicate, BodyRecord("2026-07-20", 171f, "cm"))

        val result = records.removeRecordAt(1)

        assertEquals(listOf(first, records[2]), result)
    }

    @Test
    fun removingWeightRecordKeepsAllOtherRecords() {
        val records = listOf(BodyRecord("2026-07-19", 60f, "kg"), BodyRecord("2026-07-20", 61f, "kg"))

        assertEquals(listOf(records[1]), records.removeRecordAt(0))
    }
}

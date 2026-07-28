package com.woshiwangnima.healthdietpro.model.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyRecordListOpsTest {
    @Test
    fun removesOnlyTheSelectedDuplicateRecord() {
        val first = BodyRecord("2026-07-19 08:00", 170f, "cm", 1_784_445_600_000)
        val duplicate = BodyRecord("2026-07-19 08:00", 170f, "cm", 1_784_445_600_000)
        val records = listOf(first, duplicate, BodyRecord("2026-07-20 08:00", 171f, "cm", 1_784_532_000_000))

        val result = records.removeRecordAt(1)

        assertEquals(listOf(first, records[2]), result)
    }

    @Test
    fun removingWeightRecordKeepsAllOtherRecords() {
        val records = listOf(
            BodyRecord("2026-07-19 08:00", 60f, "kg", 1_784_445_600_000),
            BodyRecord("2026-07-20 08:00", 61f, "kg", 1_784_532_000_000),
        )

        assertEquals(listOf(records[1]), records.removeRecordAt(0))
    }
}

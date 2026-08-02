package com.woshiwangnima.healthdietpro.model.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BodyMetricsIdTest {
    @Test
    fun `records with identical values require distinct stable ids`() {
        val first = BodyRecord("2026-08-02 08:00", 70f, "kg", 1_785_622_400_000, "record-a")
        val second = BodyRecord("2026-08-02 08:00", 70f, "kg", 1_785_622_400_000, "record-b")

        assertNotEquals(first.id, second.id)
        assertEquals(listOf(second), listOf(first, second).filterNot { it.id == first.id })
    }

    @Test
    fun `editing a record keeps its stable id`() {
        val original = BodyRecord("2026-08-02 08:00", 70f, "kg", 1_785_622_400_000, "record-a")
        val edited = original.copy(date = "2026-08-03 08:00", value = 69.5f)

        assertEquals(original.id, edited.id)
    }
}

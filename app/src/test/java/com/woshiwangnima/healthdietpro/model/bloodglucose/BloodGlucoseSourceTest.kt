package com.woshiwangnima.healthdietpro.model.bloodglucose

import org.junit.Assert.assertEquals
import org.junit.Test

class BloodGlucoseSourceTest {
    private val sources = listOf(
        BloodGlucoseSource("meter", "Subcutaneous glucose monitor"),
        BloodGlucoseSource("hospital", "Hospital blood test"),
    )

    @Test
    fun reorderPreservesSpecifiedSourceOrder() {
        assertEquals(listOf("hospital", "meter"), reorderBloodGlucoseSources(sources, listOf("hospital", "meter")).map(BloodGlucoseSource::id))
    }

    @Test
    fun reorderRejectsIncompleteSourceOrder() {
        try {
            reorderBloodGlucoseSources(sources, listOf("meter"))
            throw AssertionError("Expected incomplete source order to fail")
        } catch (_: IllegalArgumentException) {
        }
    }
}

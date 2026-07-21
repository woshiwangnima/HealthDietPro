package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Test

class GlycemicClassificationTest {
    @Test
    fun glycemicIndexUsesInclusiveUpperBoundaries() {
        assertEquals(GlycemicClassification.Low, classifyGlycemicIndex(55.0))
        assertEquals(GlycemicClassification.Medium, classifyGlycemicIndex(55.1))
        assertEquals(GlycemicClassification.Medium, classifyGlycemicIndex(69.0))
        assertEquals(GlycemicClassification.High, classifyGlycemicIndex(69.1))
    }

    @Test
    fun glycemicLoadUsesInclusiveUpperBoundaries() {
        assertEquals(GlycemicClassification.Low, classifyGlycemicLoad(10.0))
        assertEquals(GlycemicClassification.Medium, classifyGlycemicLoad(10.1))
        assertEquals(GlycemicClassification.Medium, classifyGlycemicLoad(19.0))
        assertEquals(GlycemicClassification.High, classifyGlycemicLoad(19.1))
    }
}

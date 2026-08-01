package com.woshiwangnima.healthdietpro.model.unit

import org.junit.Assert.assertEquals
import org.junit.Test

class GlucoseFormatTest {
    @Test
    fun `derives display precision from glucose unit step`() {
        assertEquals(1, decimalPlacesForStep(UnitCategoryType.Glucose.stepSpec("mmol_l").valueFor(UnitStepMode.Normal)))
        assertEquals(0, decimalPlacesForStep(UnitCategoryType.Glucose.stepSpec("mg_dl").valueFor(UnitStepMode.Normal)))
        assertEquals(2, decimalPlacesForStep(UnitCategoryType.Glucose.stepSpec("mmol_l").valueFor(UnitStepMode.Fine)))
        assertEquals(1, decimalPlacesForStep(UnitCategoryType.Glucose.stepSpec("mg_dl").valueFor(UnitStepMode.Fine)))
    }
}

package com.woshiwangnima.healthdietpro.model.unit

import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.math.BigDecimal
import java.util.Locale

/** Formats glucose values with the precision implied by the selected unit's step size. */
internal fun formatGlucoseValue(
    valueMmolPerL: Double,
    unitId: String,
    mode: UnitStepMode = UnitStepMode.Normal,
    locale: Locale = Locale.getDefault(),
): String {
    val value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, valueMmolPerL.toFloat(), unitId)
    val step = UnitCategoryType.Glucose.stepSpec(unitId).valueFor(mode)
    val decimals = decimalPlacesForStep(step)
    return String.format(locale, "%.${decimals}f", value)
}

internal fun decimalPlacesForStep(step: Float): Int =
    BigDecimal(step.toString()).stripTrailingZeros().scale().coerceIn(0, MAX_DECIMAL_PLACES)

private const val MAX_DECIMAL_PLACES = 4

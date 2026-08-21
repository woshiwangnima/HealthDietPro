package com.woshiwangnima.healthdietpro.model.unit

import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.math.BigDecimal
import java.util.Locale

/** Formats glucose values with 2 decimal places. */
internal fun formatGlucoseValue(
    valueMmolPerL: Double,
    unitId: String,
    locale: Locale = Locale.getDefault(),
): String {
    val value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, valueMmolPerL.toFloat(), unitId)
    return String.format(locale, "%.2f", value)
}

internal fun decimalPlacesForStep(step: Float): Int = 2

private const val MAX_DECIMAL_PLACES = 4

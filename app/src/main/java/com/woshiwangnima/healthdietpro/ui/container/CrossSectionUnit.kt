package com.woshiwangnima.healthdietpro.ui.container

import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Locale

/** Length unit category id used by the unit system. */
internal val LENGTH_CATEGORY = UnitCategoryType.Length.id

/** Practical length units offered in the cross-section editor (subset of units.json). */
internal val PRACTICAL_LENGTH_UNITS: List<String> = listOf("mm", "cm", "dm", "m", "in", "ft", "cun", "chi")

/** Localized symbol for a length unit id, falling back to the id itself. */
internal fun lengthUnitSymbol(unitId: String): String =
    UnitConverter.getRepository()?.getUnit(LENGTH_CATEGORY, unitId)
        ?.symbol(Locale.getDefault())
        ?: unitId

/** Converts a display value (in [unitId]) to the base length unit cm. */
internal fun toBaseCm(value: Double, unitId: String): Double =
    UnitConverter.toBase(LENGTH_CATEGORY, value.toFloat(), unitId).toDouble()

/** Converts a base-cm value to the display unit [unitId]. */
internal fun fromBaseCm(baseCm: Double, unitId: String): Double =
    UnitConverter.fromBase(LENGTH_CATEGORY, baseCm.toFloat(), unitId).toDouble()

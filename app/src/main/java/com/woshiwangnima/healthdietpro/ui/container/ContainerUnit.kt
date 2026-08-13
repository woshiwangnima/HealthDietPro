package com.woshiwangnima.healthdietpro.ui.container

import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Locale

/** Practical volume units offered for container capacity. */
internal val PRACTICAL_VOLUME_UNITS: List<String> = listOf("ml", "cl", "dl", "l")

/** Practical weight units offered for the optional empty container mass. */
internal val PRACTICAL_WEIGHT_UNITS: List<String> = listOf("g", "kg", "oz", "lb")

/** Localized symbol for a volume unit id, falling back to the id itself. */
internal fun volumeUnitSymbol(unitId: String): String =
    UnitConverter.getRepository()?.getUnit(UnitCategoryType.Volume.id, unitId)
        ?.symbol(Locale.getDefault())
        ?: unitId

/** Localized symbol for a weight unit id, falling back to the id itself. */
internal fun weightUnitSymbol(unitId: String): String =
    UnitConverter.getRepository()?.getUnit(UnitCategoryType.Weight.id, unitId)
        ?.symbol(Locale.getDefault())
        ?: unitId

/** Converts a display volume (in [unitId]) to base ml. */
internal fun toMl(value: Double, unitId: String): Double =
    UnitConverter.toBase(UnitCategoryType.Volume.id, value.toFloat(), unitId).toDouble() * 1000.0

/** Converts a base-ml value to the display volume unit [unitId]. */
internal fun fromMl(baseMl: Double, unitId: String): Double =
    UnitConverter.fromBase(UnitCategoryType.Volume.id, (baseMl / 1000.0).toFloat(), unitId).toDouble()

/** Converts a display mass (in [unitId]) to base grams. */
internal fun toGrams(value: Double, unitId: String): Double =
    UnitConverter.toBase(UnitCategoryType.Weight.id, value.toFloat(), unitId).toDouble() * 1000.0

/** Converts a base-gram value to the display weight unit [unitId]. */
internal fun fromGrams(baseGrams: Double, unitId: String): Double =
    UnitConverter.fromBase(UnitCategoryType.Weight.id, (baseGrams / 1000.0).toFloat(), unitId).toDouble()

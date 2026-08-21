package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.common.range.UnitRange
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter

enum class BloodGlucoseDiabetesType(
    val glucoseReferenceRangeMmolPerL: UnitRange<Float>,
    val hbA1cReferenceRange: UnitRange<Double>,
    val available: Boolean,
) {
    Normal(UnitRange(3.9f, true, 7.8f, true, UnitCategoryType.Glucose.id, "mmol_l"), UnitRange(4.0, true, 6.0, true, "hbA1c", "%"), true),
    Type1(UnitRange(3.9f, true, 10.0f, true, UnitCategoryType.Glucose.id, "mmol_l"), UnitRange(6.5, true, 7.0, false, "hbA1c", "%"), true),
    Type2(UnitRange(3.9f, true, 10.0f, true, UnitCategoryType.Glucose.id, "mmol_l"), UnitRange(6.5, true, 7.0, false, "hbA1c", "%"), true),
    Gestational(UnitRange(3.5f, true, 7.8f, true, UnitCategoryType.Glucose.id, "mmol_l"), UnitRange(null, false, 5.5, false, "hbA1c", "%"), true),
    Other(UnitRange(3.9f, true, 10.0f, true, UnitCategoryType.Glucose.id, "mmol_l"), UnitRange(4.0, true, 6.0, true, "hbA1c", "%"), true),
    ;

    /** Returns this target in the requested blood glucose unit. */
    fun targetRange(unitId: String = UnitCategoryType.Glucose.defaultUnitId): UnitRange<Float> = UnitRange(
        min = glucoseReferenceRangeMmolPerL.min?.let { UnitConverter.fromBase(UnitCategoryType.Glucose.id, it, unitId) },
        minInclusive = glucoseReferenceRangeMmolPerL.minInclusive,
        max = glucoseReferenceRangeMmolPerL.max?.let { UnitConverter.fromBase(UnitCategoryType.Glucose.id, it, unitId) },
        maxInclusive = glucoseReferenceRangeMmolPerL.maxInclusive,
        unitCategory = UnitCategoryType.Glucose.id,
        unitId = unitId,
    )
}

class BloodGlucoseTargetRepository private constructor(context: Context) {
    private val archive = BloodGlucoseArchiveStore.current(context)

    fun loadDiabetesType(): BloodGlucoseDiabetesType = archive.load().diabetesType

    fun saveDiabetesType(type: BloodGlucoseDiabetesType) {
        archive.update { it.copy(diabetesType = type) }
    }

    companion object {
        fun fromContext(context: Context) = BloodGlucoseTargetRepository(context.applicationContext)
    }
}

package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.common.range.UnitRange
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter

enum class BloodGlucoseDiabetesType(
    val minMmolPerL: Float,
    val maxMmolPerL: Float,
    val available: Boolean,
) {
    Normal(3.9f, 7.8f, true),
    Type1(3.9f, 10.0f, true),
    Type2(3.9f, 10.0f, true),
    Gestational(3.5f, 7.8f, true),
    Other(3.9f, 10.0f, true),
    ;

    /** Returns this target in the requested blood glucose unit. */
    fun targetRange(unitId: String = UnitCategoryType.Glucose.defaultUnitId): UnitRange<Float> = UnitRange(
        min = UnitConverter.fromBase(UnitCategoryType.Glucose.id, minMmolPerL, unitId),
        max = UnitConverter.fromBase(UnitCategoryType.Glucose.id, maxMmolPerL, unitId),
        unitCategory = UnitCategoryType.Glucose.id,
        unitId = unitId,
    )
}

class BloodGlucoseTargetRepository private constructor(context: Context) {
    private val userPrefs = UserPrefs.current(context)

    fun loadDiabetesType(): BloodGlucoseDiabetesType =
        userPrefs.getString(KEY_DIABETES_TYPE, BloodGlucoseDiabetesType.Normal.name)
            .let { saved -> BloodGlucoseDiabetesType.entries.find { it.name == saved } }
            ?: BloodGlucoseDiabetesType.Normal

    fun saveDiabetesType(type: BloodGlucoseDiabetesType) {
        userPrefs.putString(KEY_DIABETES_TYPE, type.name)
    }

    companion object {
        private const val KEY_DIABETES_TYPE = "blood_glucose_diabetes_type_v1"

        fun fromContext(context: Context) = BloodGlucoseTargetRepository(context.applicationContext)
    }
}

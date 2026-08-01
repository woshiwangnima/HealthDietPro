package com.woshiwangnima.healthdietpro.model.bloodglucose

import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import com.woshiwangnima.healthdietpro.common.range.UnitRange
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import kotlin.math.round

internal fun isValidBloodGlucoseValue(value: Double?): Boolean = value?.isFinite() == true && value > 0.0

internal fun normalizeBloodGlucoseTimestamp(timestamp: Long): Long =
    normalizeRecordTimestamp(timestamp, RecordTimePrecision.SECOND)

internal fun bloodGlucoseInputRange(unitId: String): UnitRange<Double> {
    val scale = if (unitId == "mg_dl") 1.0 else 10.0
    val min = round(UnitConverter.fromBase(UnitCategoryType.Glucose.id, 1.1f, unitId) * scale) / scale
    val max = round(UnitConverter.fromBase(UnitCategoryType.Glucose.id, 33.3f, unitId) * scale) / scale
    return UnitRange(min = min, max = max, unitCategory = UnitCategoryType.Glucose.id, unitId = unitId)
}

package com.woshiwangnima.healthdietpro.model.bloodpressure

import com.woshiwangnima.healthdietpro.common.range.Criterion
import com.woshiwangnima.healthdietpro.common.range.CriterionOperator
import com.woshiwangnima.healthdietpro.common.range.Range
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.range.RangeCriterion
import com.woshiwangnima.healthdietpro.common.range.matchesCriteria
import kotlinx.serialization.Serializable

@Serializable
internal data class BloodPressureRecord(
    val id: String,
    val timestamp: Long,
    val systolicMmhg: Float,
    val diastolicMmhg: Float,
    val note: String = "",
) {
    val pulsePressureMmhg: Float get() = systolicMmhg - diastolicMmhg
}

internal enum class BloodPressureCategory {
    Normal,
    Elevated,
    HypertensionStage1,
    HypertensionStage2,
    HypertensiveCrisis,
}

internal data class BloodPressureReading(
    val systolicMmhg: Float,
    val diastolicMmhg: Float,
)

internal data class BloodPressureClassificationRule(
    val category: BloodPressureCategory,
    val operator: CriterionOperator,
    val systolicRange: Range<Float>? = null,
    val diastolicRange: Range<Float>? = null,
) {
    fun matches(reading: BloodPressureReading): Boolean {
        val criteria = buildList<Criterion<BloodPressureReading>> {
            systolicRange?.let { range -> add(RangeCriterion(range) { it.systolicMmhg }) }
            diastolicRange?.let { range -> add(RangeCriterion(range) { it.diastolicMmhg }) }
        }
        return criteria.isNotEmpty() && matchesCriteria(reading, operator, criteria)
    }
}

internal val bloodPressureClassificationRules: List<BloodPressureClassificationRule> = listOf(
    BloodPressureClassificationRule(
        category = BloodPressureCategory.HypertensiveCrisis,
        operator = CriterionOperator.Any,
        systolicRange = RangeBand(min = 180f, minInclusive = false, value = Unit),
        diastolicRange = RangeBand(min = 120f, minInclusive = false, value = Unit),
    ),
    BloodPressureClassificationRule(
        category = BloodPressureCategory.HypertensionStage2,
        operator = CriterionOperator.Any,
        systolicRange = RangeBand(min = 140f, value = Unit),
        diastolicRange = RangeBand(min = 90f, value = Unit),
    ),
    BloodPressureClassificationRule(
        category = BloodPressureCategory.HypertensionStage1,
        operator = CriterionOperator.Any,
        systolicRange = RangeBand(min = 130f, max = 139f, maxInclusive = true, value = Unit),
        diastolicRange = RangeBand(min = 80f, max = 89f, maxInclusive = true, value = Unit),
    ),
    BloodPressureClassificationRule(
        category = BloodPressureCategory.Elevated,
        operator = CriterionOperator.All,
        systolicRange = RangeBand(min = 120f, max = 129f, maxInclusive = true, value = Unit),
        diastolicRange = RangeBand(max = 80f, maxInclusive = false, value = Unit),
    ),
    BloodPressureClassificationRule(
        category = BloodPressureCategory.Normal,
        operator = CriterionOperator.All,
        systolicRange = RangeBand(max = 120f, maxInclusive = false, value = Unit),
        diastolicRange = RangeBand(max = 80f, maxInclusive = false, value = Unit),
    ),
)

internal fun BloodPressureRecord.category(): BloodPressureCategory = bloodPressureCategory(systolicMmhg, diastolicMmhg)

internal fun bloodPressureCategory(systolicMmhg: Float, diastolicMmhg: Float): BloodPressureCategory =
    bloodPressureClassificationRules.firstOrNull { it.matches(BloodPressureReading(systolicMmhg, diastolicMmhg)) }
        ?.category ?: BloodPressureCategory.Normal

internal fun isValidBloodPressure(systolicMmhg: Float?, diastolicMmhg: Float?): Boolean =
    systolicMmhg?.isFinite() == true && diastolicMmhg?.isFinite() == true &&
        systolicMmhg > 0f && diastolicMmhg > 0f && systolicMmhg >= diastolicMmhg

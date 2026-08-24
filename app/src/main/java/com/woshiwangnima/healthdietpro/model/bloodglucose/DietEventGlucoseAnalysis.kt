package com.woshiwangnima.healthdietpro.model.bloodglucose

import kotlin.math.abs

internal data class DietEventGlucoseAnalysis(
    val windowStart: Long,
    val windowEnd: Long,
    val records: List<BloodGlucoseRecord>,
    val preMeal: BloodGlucoseRecord?,
    val postMealPeak: BloodGlucoseRecord?,
    val postMealTwoHours: BloodGlucoseRecord?,
    val recoveryToPreMeal: BloodGlucoseRecord?,
) {
    fun observedAtOffsetHours(offsetHours: Int): BloodGlucoseRecord? {
        val target = windowStart + (offsetHours + 1) * HOUR_MILLIS
        return records
            .asSequence()
            .filter { abs(it.timestamp - target) <= TIME_POINT_SAMPLE_TOLERANCE_MILLIS }
            .minWithOrNull(compareBy<BloodGlucoseRecord> { abs(it.timestamp - target) }.thenBy(BloodGlucoseRecord::timestamp))
    }

    val glycemicRiseMmolPerL: Double?
        get() = if (preMeal != null && postMealPeak != null) postMealPeak.valueMmolPerL - preMeal.valueMmolPerL else null

    val variabilityMmolPerL: Double?
        get() = if (preMeal != null && postMealTwoHours != null) postMealTwoHours.valueMmolPerL - preMeal.valueMmolPerL else null

    val timeToPostMealPeakMillis: Long?
        get() = postMealPeak?.timestamp?.minus(windowStart + HOUR_MILLIS)?.takeIf { it >= 0L }

    val timeToPreMealRecoveryMillis: Long?
        get() = recoveryToPreMeal?.timestamp?.minus(windowStart + HOUR_MILLIS)?.takeIf { it >= 0L }
}

/**
 * Uses the meal start as the event anchor, matching the analysis chart's fixed -1 h to +4 h axis.
 * A two-hour value is only accepted when an observed sample exists within 30 minutes of the target.
 */
internal fun analyzeDietEventGlucose(
    mealStartAt: Long,
    allRecords: List<BloodGlucoseRecord>,
): DietEventGlucoseAnalysis {
    val windowStart = mealStartAt - HOUR_MILLIS
    val windowEnd = mealStartAt + 4 * HOUR_MILLIS
    val records = allRecords
        .asSequence()
        .filter { it.timestamp in windowStart..windowEnd }
        .sortedBy(BloodGlucoseRecord::timestamp)
        .toList()
    val preMeal = records.lastOrNull { it.timestamp < mealStartAt }
    val postMeal = records.filter { it.timestamp >= mealStartAt }
    val postMealPeak = postMeal.maxByOrNull(BloodGlucoseRecord::valueMmolPerL)
    return DietEventGlucoseAnalysis(
        windowStart = windowStart,
        windowEnd = windowEnd,
        records = records,
        preMeal = preMeal,
        postMealPeak = postMealPeak,
        postMealTwoHours = records
            .asSequence()
            .filter { it.timestamp >= mealStartAt }
            .filter { abs(it.timestamp - (mealStartAt + 2 * HOUR_MILLIS)) <= TIME_POINT_SAMPLE_TOLERANCE_MILLIS }
            .minWithOrNull(compareBy<BloodGlucoseRecord> { abs(it.timestamp - (mealStartAt + 2 * HOUR_MILLIS)) }.thenBy(BloodGlucoseRecord::timestamp)),
        recoveryToPreMeal = preMeal?.let { baseline ->
            postMealPeak?.let { peak ->
                postMeal.firstOrNull { it.timestamp > peak.timestamp && it.valueMmolPerL <= baseline.valueMmolPerL }
            }
        },
    )
}

private const val HOUR_MILLIS = 60 * 60 * 1_000L
private const val TIME_POINT_SAMPLE_TOLERANCE_MILLIS = 30 * 60 * 1_000L

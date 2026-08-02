package com.woshiwangnima.healthdietpro.model.water

import com.woshiwangnima.healthdietpro.model.profile.Gender
import kotlinx.serialization.Serializable

@Serializable
internal data class WaterRecord(
    val id: String,
    val timestamp: Long,
    val beverageId: String,
    val beverageName: String,
    val volumeMl: Double,
)

@Serializable
internal data class WaterQuickRecord(
    val beverageId: String,
    val volume: Double = 250.0,
    val unit: WaterVolumeUnit = WaterVolumeUnit.ML,
)

/** Kept extensible because activity levels are also useful to future health features. */
@Serializable
internal enum class ActivityLevel {
    NONE,
    LOW,
    MODERATE,
    HIGH,
}

@Serializable
internal enum class WaterVolumeUnit(val milliliters: Double) {
    ML(1.0),
    L(1000.0),
}

@Serializable
internal data class WaterArchive(
    val schemaVersion: Int = 1,
    val records: List<WaterRecord> = emptyList(),
    val activityLevel: ActivityLevel = ActivityLevel.NONE,
    val quickRecords: List<WaterQuickRecord> = emptyList(),
)

internal fun recommendedWaterMl(
    gender: Gender,
    age: Int?,
    latestWeightKg: Float?,
    activityLevel: ActivityLevel,
): Int {
    val baseline = latestWeightKg?.takeIf { it > 0f }?.let { weight ->
        (weight * 35f).toInt()
    } ?: when {
        age != null && age < 14 -> if (gender == Gender.MALE) 2_100 else 1_900
        age != null && age < 19 -> if (gender == Gender.MALE) 2_500 else 2_200
        gender == Gender.MALE -> 3_000
        else -> 2_700
    }
    val activityExtra = when (activityLevel) {
        ActivityLevel.NONE -> 0
        ActivityLevel.LOW -> 250
        ActivityLevel.MODERATE -> 500
        ActivityLevel.HIGH -> 750
    }
    return (baseline + activityExtra).coerceIn(1_500, 5_000)
}

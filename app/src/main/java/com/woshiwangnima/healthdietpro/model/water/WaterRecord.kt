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
    val id: String = "",
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
    val schemaVersion: Int = WATER_ARCHIVE_SCHEMA_VERSION,
    val records: List<WaterRecord> = emptyList(),
    val activityLevel: ActivityLevel = ActivityLevel.NONE,
    val quickRecords: List<WaterQuickRecord> = emptyList(),
)

internal const val WATER_ARCHIVE_SCHEMA_VERSION = 2

/** Assigns stable identities to v1 presets, which were uniquely addressed only by beverage ID. */
internal fun migrateWaterArchive(archive: WaterArchive): WaterArchive {
    val usedIds = mutableSetOf<String>()
    val quickRecords = archive.quickRecords.mapIndexed { index, record ->
        val id = record.id.takeIf { it.isNotBlank() && usedIds.add(it) }
            ?: java.util.UUID.nameUUIDFromBytes(
                "${record.beverageId}\u0000${record.volume}\u0000${record.unit.name}\u0000$index".toByteArray(Charsets.UTF_8),
            ).toString().also(usedIds::add)
        record.copy(id = id)
    }
    return archive.copy(schemaVersion = WATER_ARCHIVE_SCHEMA_VERSION, quickRecords = quickRecords)
}

internal fun reorderWaterQuickRecords(
    quickRecords: List<WaterQuickRecord>,
    orderedIds: List<String>,
): List<WaterQuickRecord> {
    val currentIds = quickRecords.map(WaterQuickRecord::id)
    require(orderedIds.size == currentIds.size) { "Incomplete water quick record order" }
    require(orderedIds.distinct().size == orderedIds.size) { "Duplicate water quick record id" }
    require(orderedIds.toSet() == currentIds.toSet()) { "Unknown water quick record id" }
    val byId = quickRecords.associateBy(WaterQuickRecord::id)
    return orderedIds.map { requireNotNull(byId[it]) }
}

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

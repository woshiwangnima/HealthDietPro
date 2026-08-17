package com.woshiwangnima.healthdietpro.model.diet

import kotlinx.serialization.Serializable

/** 一次用餐：时间、时段、食物信息数组与可选备注。 */
@Serializable
internal data class DietRecord(
    val id: String,
    val mealStartAt: Long,
    val mealEndAt: Long,
    val mealPeriod: MealPeriod,
    val entries: List<DietFoodEntry>,
    val note: String = "",
    val recordedAt: Long,
)

/** 每餐默认时长：进入编辑器时 mealEndAt 缺省 = mealStartAt + 30 分钟。 */
internal const val DEFAULT_MEAL_DURATION_MINUTES = 30L

/** 归档 schema 版本（对齐存档公约：缺失视为 0.0.0，经 migrateDietArchive 迁移）。 */
internal const val DIET_ARCHIVE_SCHEMA_VERSION = 1

@Serializable
internal data class DietArchive(
    val schemaVersion: Int = DIET_ARCHIVE_SCHEMA_VERSION,
    val records: List<DietRecord> = emptyList(),
)

/**
 * 幂等迁移链：v1 首版仅规范化字段。后续版本在此链式追加。
 * 缺失 schemaVersion 视为 0，统一迁移到当前版本。
 */
internal fun migrateDietArchive(archive: DietArchive): DietArchive {
    val records = archive.records
        .map { record ->
            record.copy(
                id = record.id.trim(),
                note = record.note.trim(),
                mealEndAt = if (record.mealEndAt >= record.mealStartAt) record.mealEndAt else record.mealStartAt,
                entries = record.entries
                    .map { entry ->
                        entry.copy(
                            foodName = entry.foodName.trim(),
                            weightUnitId = entry.weightUnitId.trim(),
                            netWeightGrams = entry.netWeightGrams.coerceAtLeast(0.0),
                        )
                    }
                    .filter { it.weightValue > 0.0 && it.netWeightGrams > 0.0 },
            )
        }
        .filter { it.mealStartAt > 0L && it.recordedAt > 0L && it.entries.isNotEmpty() }
        .distinctBy(DietRecord::id)
    return archive.copy(schemaVersion = DIET_ARCHIVE_SCHEMA_VERSION, records = records)
}

/** 纯校验（无 Android 依赖，JVM 可测）。 */
internal fun validateDietArchive(archive: DietArchive) {
    require(archive.schemaVersion == DIET_ARCHIVE_SCHEMA_VERSION) { "Unsupported diet archive schema" }
    val ids = archive.records.map(DietRecord::id)
    require(ids.all(String::isNotBlank) && ids.distinct().size == ids.size) { "Invalid diet record ids" }
    require(archive.records.all { it.mealStartAt > 0L && it.recordedAt > 0L }) { "Invalid diet record time" }
    require(archive.records.all { it.mealEndAt >= it.mealStartAt }) { "Invalid diet meal time" }
    require(archive.records.all { it.entries.isNotEmpty() }) { "Diet record has no entries" }
    require(archive.records.all { record ->
        record.entries.all { entry ->
            entry.weightValue > 0.0 &&
                entry.weightUnitId.isNotBlank() &&
                entry.netWeightGrams > 0.0 &&
                entry.foodName.isNotBlank() &&
                (entry.foodId == null) == (entry.foodKind == null) &&
                (entry.foodId != null) == (entry.foodKind != null)
        }
    }) { "Invalid diet food entry" }
}
package com.woshiwangnima.healthdietpro.model.sleep

import kotlinx.serialization.Serializable

/**
 * 一条睡眠记录（夜间睡眠或小憩）。
 *
 * - [sleepStartAt] 必填；[wakeUpAt] 睡眠进行中为 null，存在时必须 `>= sleepStartAt`。
 * - [recordedAt] 独立于入睡/醒来时间（支持补记）。
 * - [timerId] 关联的计时器实例 id（可选），计时器生命周期独立于记录。
 */
@Serializable
internal data class SleepRecord(
    val id: String,
    val kind: SleepKind,
    val sleepStartAt: Long,
    val wakeUpAt: Long? = null,
    val recordedAt: Long,
    val note: String = "",
    val timerId: String? = null,
)

/** 时长（分钟）：睡眠进行中返回 null。 */
internal fun SleepRecord.durationMinutes(): Long? = wakeUpAt?.let { (it - sleepStartAt) / 60_000L }

internal const val SLEEP_ARCHIVE_SCHEMA_VERSION = 1

@Serializable
internal data class SleepArchive(
    val schemaVersion: Int = SLEEP_ARCHIVE_SCHEMA_VERSION,
    val records: List<SleepRecord> = emptyList(),
)

/**
 * 幂等迁移链：v1 首版仅规范化字段。后续版本在此链式追加。
 * 缺失 schemaVersion 视为 0，统一迁移到当前版本。
 */
internal fun migrateSleepArchive(archive: SleepArchive): SleepArchive {
    val records = archive.records
        .map { record -> record.copy(note = record.note.trim(), id = record.id.trim()) }
        .distinctBy(SleepRecord::id)
    return archive.copy(
        schemaVersion = SLEEP_ARCHIVE_SCHEMA_VERSION,
        records = records,
    )
}

/** 纯校验（无 Android 依赖，JVM 可测）。 */
internal fun validateSleepArchive(archive: SleepArchive) {
    require(archive.schemaVersion == SLEEP_ARCHIVE_SCHEMA_VERSION) { "Unsupported sleep archive schema" }
    val ids = archive.records.map(SleepRecord::id)
    require(ids.all(String::isNotBlank) && ids.distinct().size == ids.size) { "Invalid sleep record ids" }
    require(archive.records.all { it.sleepStartAt > 0L && it.recordedAt > 0L }) { "Invalid sleep record time" }
    require(archive.records.all { it.wakeUpAt == null || it.wakeUpAt >= it.sleepStartAt }) { "Invalid sleep wake time" }
    require(archive.records.all { it.durationMinutes() == null || it.durationMinutes()!! <= MAX_SLEEP_DURATION_MINUTES }) {
        "Sleep duration exceeds limit"
    }
}

private const val MAX_SLEEP_DURATION_MINUTES = 48L * 60L
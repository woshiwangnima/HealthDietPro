package com.woshiwangnima.healthdietpro.model.sleep

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.Serializable

/** 习惯记录时机：睡前记录（当前时间作为入睡时间）或起床记录（当前时间作为醒来时间）。 */
@Serializable
internal enum class SleepRecordTiming {
    BEFORE_SLEEP,
    AFTER_WAKE,
}

/** 睡眠偏好设置（按用户隔离）。时长统一以分钟为基准单位，单位仅是 UI 提示。 */
@Serializable
internal data class SleepPrefs(
    val nightDefaultMinutes: Int = NIGHT_DEFAULT_MINUTES,
    val napDefaultMinutes: Int = NAP_DEFAULT_MINUTES,
    val nightUnitId: String = "h",
    val napUnitId: String = "h",
    val nightTiming: SleepRecordTiming = SleepRecordTiming.BEFORE_SLEEP,
    val napTiming: SleepRecordTiming = SleepRecordTiming.BEFORE_SLEEP,
)

internal const val NIGHT_DEFAULT_MINUTES = 8 * 60
internal const val NAP_DEFAULT_MINUTES = 90

/** 读取当前用户的睡眠偏好。 */
internal fun loadSleepPrefs(context: Context): SleepPrefs {
    val scope = UserPrefs.current(context)
    return SleepPrefs(
        nightDefaultMinutes = scope.getInt(KEY_NIGHT_MINUTES, NIGHT_DEFAULT_MINUTES),
        napDefaultMinutes = scope.getInt(KEY_NAP_MINUTES, NAP_DEFAULT_MINUTES),
        nightUnitId = scope.getString(KEY_NIGHT_UNIT, "h"),
        napUnitId = scope.getString(KEY_NAP_UNIT, "h"),
        nightTiming = scope.getString(KEY_NIGHT_TIMING, SleepRecordTiming.BEFORE_SLEEP.name)
            .let { name -> SleepRecordTiming.entries.firstOrNull { it.name == name } ?: SleepRecordTiming.BEFORE_SLEEP },
        napTiming = scope.getString(KEY_NAP_TIMING, SleepRecordTiming.BEFORE_SLEEP.name)
            .let { name -> SleepRecordTiming.entries.firstOrNull { it.name == name } ?: SleepRecordTiming.BEFORE_SLEEP },
    )
}

/** 持久化当前用户的睡眠偏好。 */
internal fun saveSleepPrefs(context: Context, prefs: SleepPrefs) {
    val scope = UserPrefs.current(context)
    scope.putInt(KEY_NIGHT_MINUTES, prefs.nightDefaultMinutes.coerceAtLeast(1))
    scope.putInt(KEY_NAP_MINUTES, prefs.napDefaultMinutes.coerceAtLeast(1))
    scope.putString(KEY_NIGHT_UNIT, prefs.nightUnitId)
    scope.putString(KEY_NAP_UNIT, prefs.napUnitId)
    scope.putString(KEY_NIGHT_TIMING, prefs.nightTiming.name)
    scope.putString(KEY_NAP_TIMING, prefs.napTiming.name)
}

/** 根据习惯记录时机与默认时长推导新记录的默认时间：睡前记录以当前时间为入睡时间。 */
internal fun defaultSleepTimes(
    prefs: SleepPrefs,
    kind: SleepKind,
    now: Long,
): Pair<Long, Long> {
    val minutes = if (kind == SleepKind.NIGHT_SLEEP) prefs.nightDefaultMinutes else prefs.napDefaultMinutes
    val timing = if (kind == SleepKind.NIGHT_SLEEP) prefs.nightTiming else prefs.napTiming
    val duration = minutes * 60_000L
    return when (timing) {
        SleepRecordTiming.BEFORE_SLEEP -> now to (now + duration)
        SleepRecordTiming.AFTER_WAKE -> (now - duration) to now
    }
}

private const val KEY_NIGHT_MINUTES = "sleep_night_default_minutes"
private const val KEY_NAP_MINUTES = "sleep_nap_default_minutes"
private const val KEY_NIGHT_UNIT = "sleep_night_unit_id"
private const val KEY_NAP_UNIT = "sleep_nap_unit_id"
private const val KEY_NIGHT_TIMING = "sleep_night_record_timing"
private const val KEY_NAP_TIMING = "sleep_nap_record_timing"

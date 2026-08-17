package com.woshiwangnima.healthdietpro.model.diet

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.Serializable

/** 习惯记录时机：用餐前记录（当前时间作为用餐开始时间）或用餐后记录（当前时间作为用餐结束时间）。 */
@Serializable
internal enum class DietRecordTiming {
    BEFORE_MEAL,
    AFTER_MEAL,
}

/** 单个用餐时段的默认习惯：时长（基准分钟，单位仅是 UI 提示）与记录时机。 */
@Serializable
internal data class DietPeriodPrefs(
    val defaultMinutes: Int = DEFAULT_MEAL_DURATION_MINUTES.toInt(),
    val unitId: String = "min",
    val timing: DietRecordTiming = DietRecordTiming.BEFORE_MEAL,
    /** 自定义默认区间起始分钟（0-1439），null 表示使用 [MealPeriod.defaultStartMinute]。 */
    val rangeStartMinute: Int? = null,
    /** 自定义默认区间结束分钟（0-1439，可小于 start 表示跨午夜），null 表示使用 [MealPeriod.defaultEndMinute]。 */
    val rangeEndMinute: Int? = null,
)

/** 饮食习惯偏好（按用户隔离）。[periods] 以 [MealPeriod] 枚举名称为键；未配置的时段回退默认。 */
@Serializable
internal data class DietPrefs(
    val periods: Map<String, DietPeriodPrefs> = emptyMap(),
) {
    fun forPeriod(period: MealPeriod): DietPeriodPrefs = periods[period.name] ?: DietPeriodPrefs()

    fun withPeriod(period: MealPeriod, value: DietPeriodPrefs): DietPrefs =
        copy(periods = periods + (period.name to value))
}

/** 读取当前用户的饮食习惯偏好。 */
internal fun loadDietPrefs(context: Context): DietPrefs {
    val scope = UserPrefs.current(context)
    val periods = MealPeriod.entries.mapNotNull { period ->
        val prefix = "diet_default_${period.name.lowercase()}"
        val defaults = DietPeriodPrefs()
        val minutes = scope.getInt("${prefix}_minutes", defaults.defaultMinutes)
        val unitId = scope.getString("${prefix}_unit", "min")
        val timingName = scope.getString("${prefix}_timing", DietRecordTiming.BEFORE_MEAL.name)
        val timing = DietRecordTiming.entries.firstOrNull { it.name == timingName } ?: DietRecordTiming.BEFORE_MEAL
        val hasRange = scope.getBoolean("${prefix}_range_custom", false)
        val startMinute = if (hasRange) scope.getInt("${prefix}_range_start", period.defaultStartMinute) else null
        val endMinute = if (hasRange) scope.getInt("${prefix}_range_end", period.defaultEndMinute) else null
        val value = DietPeriodPrefs(
            defaultMinutes = minutes,
            unitId = unitId,
            timing = timing,
            rangeStartMinute = startMinute,
            rangeEndMinute = endMinute,
        )
        if (value == defaults) null else period.name to value
    }.toMap()
    return DietPrefs(periods)
}

/** 持久化当前用户的饮食习惯偏好。 */
internal fun saveDietPrefs(context: Context, prefs: DietPrefs) {
    val scope = UserPrefs.current(context)
    MealPeriod.entries.forEach { period ->
        val value = prefs.forPeriod(period)
        val prefix = "diet_default_${period.name.lowercase()}"
        scope.putInt("${prefix}_minutes", value.defaultMinutes.coerceAtLeast(1))
        scope.putString("${prefix}_unit", value.unitId)
        scope.putString("${prefix}_timing", value.timing.name)
        val hasRange = value.rangeStartMinute != null && value.rangeEndMinute != null
        scope.putBoolean("${prefix}_range_custom", hasRange)
        if (hasRange) {
            scope.putInt("${prefix}_range_start", value.rangeStartMinute!!.coerceIn(0, 1439))
            scope.putInt("${prefix}_range_end", value.rangeEndMinute!!.coerceIn(0, 1439))
        }
    }
}

/**
 * 根据习惯记录时机与默认时长推导新记录的默认起止时间：
 * 用餐前记录以当前时间为开始时间；用餐后记录以当前时间为结束时间。
 */
internal fun defaultDietTimes(
    prefs: DietPrefs,
    period: MealPeriod,
    now: Long,
): Pair<Long, Long> {
    val value = prefs.forPeriod(period)
    val duration = value.defaultMinutes.toLong() * 60_000L
    return when (value.timing) {
        DietRecordTiming.BEFORE_MEAL -> now to (now + duration)
        DietRecordTiming.AFTER_MEAL -> (now - duration) to now
    }
}

/**
 * 按本地时间「时:分」解析时段，优先使用用户自定义区间，未配置时回退 [MealPeriod.defaultForMillis]。
 */
internal fun MealPeriod.resolveDefault(millis: Long, prefs: DietPrefs, zone: java.time.ZoneId): MealPeriod {
    val local = java.time.Instant.ofEpochMilli(millis).atZone(zone)
    val minuteOfDay = local.hour * 60 + local.minute
    MealPeriod.entries.forEach { period ->
        val value = prefs.forPeriod(period)
        val start = value.rangeStartMinute
        val end = value.rangeEndMinute
        if (start != null && end != null && period.containsMinute(minuteOfDay, start, end)) return period
    }
    return defaultForMillis(millis, zone)
}
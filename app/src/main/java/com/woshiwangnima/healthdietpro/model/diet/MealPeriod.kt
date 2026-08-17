package com.woshiwangnima.healthdietpro.model.diet

import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable

/**
 * 用餐时段：早餐前加餐 / 早餐 / 早午间加餐 / 午餐 / 午晚间加餐 / 晚餐 / 晚餐后加餐。
 *
 * 时段与时间字段独立存储（用户可手动改选，历史不回算）。[defaultForMillis] 仅在新建记录时用于默认值。
 */
@Serializable
internal enum class MealPeriod {
    PRE_BREAKFAST_SNACK,
    BREAKFAST,
    MID_MORNING_SNACK,
    LUNCH,
    MID_AFTERNOON_SNACK,
    DINNER,
    POST_DINNER_SNACK,
    ;

    /**
     * 按本地时间（[ZoneId.systemDefault]）的「时:分」返回默认时段。
     * 跨午夜区间（晚餐后加餐）按 `>= 开始 || < 结束` 判断。
     */
    fun defaultForMillis(millis: Long): MealPeriod = defaultForMillis(millis, ZoneId.systemDefault())

    internal fun defaultForMillis(millis: Long, zone: ZoneId): MealPeriod {
        val local = Instant.ofEpochMilli(millis).atZone(zone)
        val minuteOfDay = local.hour * 60 + local.minute
        return when {
            minuteOfDay in 5 * 60 until 7 * 60 -> PRE_BREAKFAST_SNACK
            minuteOfDay in 7 * 60 until 10 * 60 -> BREAKFAST
            minuteOfDay in 10 * 60 until 11 * 60 + 30 -> MID_MORNING_SNACK
            minuteOfDay in 11 * 60 + 30 until 13 * 60 + 30 -> LUNCH
            minuteOfDay in 13 * 60 + 30 until 17 * 60 + 30 -> MID_AFTERNOON_SNACK
            minuteOfDay in 17 * 60 + 30 until 20 * 60 -> DINNER
            else -> POST_DINNER_SNACK
        }
    }

    /** 默认区间起始分钟数（0-1439，当天 00:00 起）。跨午夜区间 end 小于 start。 */
    internal val defaultStartMinute: Int
        get() = when (this) {
            PRE_BREAKFAST_SNACK -> 5 * 60
            BREAKFAST -> 7 * 60
            MID_MORNING_SNACK -> 10 * 60
            LUNCH -> 11 * 60 + 30
            MID_AFTERNOON_SNACK -> 13 * 60 + 30
            DINNER -> 17 * 60 + 30
            POST_DINNER_SNACK -> 20 * 60
        }

    internal val defaultEndMinute: Int
        get() = when (this) {
            PRE_BREAKFAST_SNACK -> 7 * 60 - 1
            BREAKFAST -> 10 * 60 - 1
            MID_MORNING_SNACK -> 11 * 60 + 29
            LUNCH -> 13 * 60 + 29
            MID_AFTERNOON_SNACK -> 17 * 60 + 29
            DINNER -> 20 * 60 - 1
            POST_DINNER_SNACK -> 5 * 60 - 1
        }

    /** 判断某「分」是否落在 start..end 区间（跨午夜时按 `>= start || <= end`）。 */
    internal fun containsMinute(minuteOfDay: Int, startMinute: Int, endMinute: Int): Boolean =
        if (startMinute <= endMinute) minuteOfDay in startMinute..endMinute
        else minuteOfDay >= startMinute || minuteOfDay <= endMinute
}
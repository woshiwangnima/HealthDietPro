package com.woshiwangnima.healthdietpro.model.sleep

import kotlinx.serialization.Serializable

/** 睡眠类型：夜间睡眠 / 小憩。 */
@Serializable
internal enum class SleepKind {
    NIGHT_SLEEP,
    NAP,
}
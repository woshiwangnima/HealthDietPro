package com.woshiwangnima.healthdietpro.common.timer

import kotlinx.serialization.Serializable

/** 计时器状态机。 */
@Serializable
internal enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED,
}

/**
 * 一个可持久化的计时实例（基础设施模块，不绑定睡眠/菜肴等具体功能）。
 *
 * - [totalMinutes] 目标时长；[remainingSeconds] 暂停/恢复用剩余秒数。
 * - [startedAtMillis] 最近一次启动时间；RUNNING 时剩余秒数按 `total*60 - (now - startedAt)/1000` 派生。
 * - [notifyViaSystem] 是否联动系统闹钟/计时器（系统副作用，必须显式声明并二次确认）。
 */
@Serializable
internal data class TimerInstance(
    val id: String,
    val label: String,
    val totalMinutes: Int,
    val startedAtMillis: Long? = null,
    val remainingSeconds: Long = 0L,
    val state: TimerState = TimerState.IDLE,
    val notifyViaSystem: Boolean = false,
)

/** 总时长秒数。 */
internal fun TimerInstance.totalSeconds(): Long = totalMinutes.toLong() * 60L

/** 当前剩余秒数：RUNNING 时按启动时间派生；其余状态取 [TimerInstance.remainingSeconds]。 */
internal fun TimerInstance.remainingNow(nowMillis: Long): Long = when (state) {
    TimerState.RUNNING -> {
        val started = startedAtMillis ?: return remainingSeconds.coerceAtLeast(0L)
        (remainingSeconds - (nowMillis - started) / 1000L).coerceAtLeast(0L)
    }
    else -> remainingSeconds.coerceAtLeast(0L)
}

internal fun TimerInstance.advance(nowMillis: Long): TimerInstance {
    if (state != TimerState.RUNNING) return this
    val remaining = remainingNow(nowMillis)
    if (remaining <= 0L) return copy(state = TimerState.FINISHED, remainingSeconds = 0L)
    return copy(remainingSeconds = remaining)
}

internal fun TimerInstance.pause(nowMillis: Long): TimerInstance {
    if (state != TimerState.RUNNING) return this
    return copy(state = TimerState.PAUSED, remainingSeconds = remainingNow(nowMillis))
}

internal fun TimerInstance.resume(nowMillis: Long): TimerInstance {
    if (state != TimerState.PAUSED && state != TimerState.IDLE) return this
    return copy(state = TimerState.RUNNING, startedAtMillis = nowMillis, remainingSeconds = remainingSeconds.takeIf { it > 0L } ?: totalSeconds())
}

internal fun TimerInstance.reset(): TimerInstance =
    copy(state = TimerState.IDLE, startedAtMillis = null, remainingSeconds = 0L)
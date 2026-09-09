package com.woshiwangnima.healthdietpro.common.time

import kotlin.time.Duration

enum class RelativeTimeUnit(val millis: Long) {
    SECOND(1_000L),
    MINUTE(60_000L),
    HOUR(3_600_000L),
    DAY(86_400_000L),
    MONTH(2_592_000_000L),
    YEAR(31_536_000_000L),
}

data class RelativeTime(
    val amount: Long,
    val unit: RelativeTimeUnit,
)

private val relativeTimeUnits = RelativeTimeUnit.entries.toTypedArray()
private val relativeTimeThresholds = LongArray(relativeTimeUnits.size) { index -> relativeTimeUnits[index].millis }

fun relativeTimeSince(timestampMillis: Long, nowMillis: Long): RelativeTime {
    val elapsedMillis = (nowMillis - timestampMillis).coerceAtLeast(0L)
    val unitIndex = relativeTimeThresholds.binarySearch(elapsedMillis)
        .let { if (it >= 0) it else (-it - 2).coerceAtLeast(0) }
    val unit = relativeTimeUnits[unitIndex]
    return RelativeTime(amount = (elapsedMillis / unit.millis).coerceAtLeast(1L), unit = unit)
}

fun formatRelativeTimeOffset(
    offset: Duration,
    zeroLabel: String,
    dayUnit: String,
    hourUnit: String,
    minuteUnit: String,
    secondUnit: String,
): String {
    if (offset == Duration.ZERO) return zeroLabel
    val negative = offset.isNegative()
    var seconds = offset.absoluteValue.inWholeSeconds
    val days = seconds / 86_400
    seconds %= 86_400
    val hours = seconds / 3_600
    seconds %= 3_600
    val minutes = seconds / 60
    seconds %= 60
    return buildString {
        if (negative) append('-')
        if (days > 0) append(days).append(dayUnit)
        if (hours > 0) append(hours).append(hourUnit)
        if (minutes > 0) append(minutes).append(minuteUnit)
        if (seconds > 0) append(seconds).append(secondUnit)
    }
}

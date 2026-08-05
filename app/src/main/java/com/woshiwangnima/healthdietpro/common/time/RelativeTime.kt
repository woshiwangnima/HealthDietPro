package com.woshiwangnima.healthdietpro.common.time

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

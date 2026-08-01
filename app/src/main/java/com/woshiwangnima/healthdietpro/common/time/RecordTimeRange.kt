package com.woshiwangnima.healthdietpro.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal data class RecordTimeRange(
    val startMillis: Long,
    val endMillis: Long,
) {
    init {
        require(startMillis <= endMillis)
    }

    fun contains(timestamp: Long): Boolean = timestamp in startMillis..endMillis
}

internal enum class RecordTimeRangePreset {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    LAST_24_HOURS,
    LAST_72_HOURS,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_1_YEAR,
}

internal fun RecordTimeRangePreset.resolve(
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): RecordTimeRange {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    val today = now.toLocalDate()
    val start = when (this) {
        RecordTimeRangePreset.ALL -> Instant.EPOCH.atZone(zoneId)
        RecordTimeRangePreset.TODAY -> today.atStartOfDay(zoneId)
        RecordTimeRangePreset.THIS_WEEK -> today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(zoneId)
        RecordTimeRangePreset.THIS_MONTH -> today.withDayOfMonth(1).atStartOfDay(zoneId)
        RecordTimeRangePreset.THIS_YEAR -> today.withDayOfYear(1).atStartOfDay(zoneId)
        RecordTimeRangePreset.LAST_24_HOURS -> now.minusHours(24)
        RecordTimeRangePreset.LAST_72_HOURS -> now.minusHours(72)
        RecordTimeRangePreset.LAST_7_DAYS -> now.minusDays(7)
        RecordTimeRangePreset.LAST_30_DAYS -> now.minusDays(30)
        RecordTimeRangePreset.LAST_1_YEAR -> now.minusYears(1)
    }
    return RecordTimeRange(start.toInstant().toEpochMilli(), now.toInstant().toEpochMilli())
}

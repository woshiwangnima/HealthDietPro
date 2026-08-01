package com.woshiwangnima.healthdietpro.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Precision for user-entered record times. Persisted values are always Unix epoch milliseconds.
 */
internal enum class RecordTimePrecision {
    DATE,
    MINUTE,
    SECOND,
}

internal fun normalizeRecordTimestamp(
    epochMillis: Long,
    precision: RecordTimePrecision,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long = when (precision) {
    RecordTimePrecision.DATE -> Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .toLocalDate()
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
    RecordTimePrecision.MINUTE -> epochMillis / MILLIS_PER_MINUTE * MILLIS_PER_MINUTE
    RecordTimePrecision.SECOND -> epochMillis / MILLIS_PER_SECOND * MILLIS_PER_SECOND
}

internal fun formatRecordTimestamp(
    epochMillis: Long,
    precision: RecordTimePrecision,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(
    when (precision) {
        RecordTimePrecision.DATE -> DATE_FORMATTER
        RecordTimePrecision.MINUTE -> MINUTE_FORMATTER
        RecordTimePrecision.SECOND -> SECOND_FORMATTER
    },
)

internal fun recordDateStartMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zoneId).toInstant().toEpochMilli()

internal fun recordDateEndExclusiveMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val MINUTE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val SECOND_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

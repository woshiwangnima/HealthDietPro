package com.woshiwangnima.healthdietpro.model.profile

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BodyRecordDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val BodyRecordDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val BodyRecordDisplayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

fun parseBodyRecordDateTime(value: String): LocalDateTime {
    return runCatching {
        LocalDateTime.parse(value, BodyRecordDateTimeFormatter)
    }.getOrElse {
        LocalDate.parse(value, BodyRecordDateFormatter).atStartOfDay()
    }
}

fun formatBodyRecordDateTime(value: LocalDateTime): String =
    value.format(BodyRecordDateTimeFormatter)

fun formatBodyRecordDisplayDateTime(value: String): String =
    parseBodyRecordDateTime(value).format(BodyRecordDisplayFormatter)

fun bodyRecordEpochMillis(value: String): Long =
    parseBodyRecordDateTime(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Upgrades legacy date-only records to the start of their recorded day. */
internal fun migrateBodyRecordDateTime(record: BodyRecord): BodyRecord {
    val wasDateOnly = record.date.length == 10
    val date = formatBodyRecordDateTime(parseBodyRecordDateTime(record.date))
    val timestamp = if (wasDateOnly || record.recordedAtMillis <= 0L) {
        bodyRecordEpochMillis(date)
    } else {
        record.recordedAtMillis
    }
    return if (record.date == date && record.recordedAtMillis == timestamp) record else {
        record.copy(date = date, recordedAtMillis = timestamp)
    }
}

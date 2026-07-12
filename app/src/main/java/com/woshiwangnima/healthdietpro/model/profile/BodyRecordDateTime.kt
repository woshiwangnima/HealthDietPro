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
        LocalDate.parse(value.take(10), BodyRecordDateFormatter).atStartOfDay()
    }
}

fun formatBodyRecordDateTime(value: LocalDateTime): String =
    value.format(BodyRecordDateTimeFormatter)

fun formatBodyRecordDisplayDateTime(value: String): String =
    parseBodyRecordDateTime(value).format(BodyRecordDisplayFormatter)

fun bodyRecordEpochMillis(value: String): Long =
    parseBodyRecordDateTime(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

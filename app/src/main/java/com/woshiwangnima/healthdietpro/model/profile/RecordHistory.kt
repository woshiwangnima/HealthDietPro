package com.woshiwangnima.healthdietpro.model.profile

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class RecordHistory(
    val records: List<BodyRecord>,
    val category: String = ""
) {
    val sortedRecords: List<BodyRecord> by lazy { records.sortedBy { it.date } }

    val dataPoints: List<DataPoint> by lazy {
        sortedRecords.map { record ->
            val localDate = LocalDate.parse(record.date)
            val ts = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            DataPoint(
                timestamp = ts,
                value = record.value,
                dateLabel = record.date.takeLast(5)
            )
        }
    }

    val timeSpan: Long get() =
        if (dataPoints.size >= 2) dataPoints.maxOf { it.timestamp } - dataPoints.minOf { it.timestamp } else 0L

    val minInterval: Long get() =
        if (dataPoints.size >= 2) dataPoints.zipWithNext { a, b -> b.timestamp - a.timestamp }.minOrNull() ?: 0L else 0L

    val maxInterval: Long get() =
        if (dataPoints.size >= 2) dataPoints.zipWithNext { a, b -> b.timestamp - a.timestamp }.maxOrNull() ?: 0L else 0L

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd")

    fun formatTimestamp(ts: Long): String {
        val instant = Instant.ofEpochMilli(ts)
        val localDt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        return localDt.format(dateFormatter)
    }
}

package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarChart
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarEntry
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarReferenceLine
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarSegment
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.durationMinutes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class DaySleepTotals(val night: Long = 0L, val nap: Long = 0L) {
    val total: Long get() = night + nap
}

private enum class SleepStatKind { ALL, NIGHT, NAP }

@Composable
internal fun SleepStatisticsTab(records: List<SleepRecord>, modifier: Modifier = Modifier) {
    val zone = remember { ZoneId.systemDefault() }
    var trendDays by rememberSaveable { mutableIntStateOf(7) }
    var statKind by rememberSaveable { mutableStateOf(SleepStatKind.ALL) }
    val endDate = remember(trendDays) { LocalDate.now(zone) }
    val dates = remember(endDate, trendDays) { (0 until trendDays).map { offset -> endDate.minusDays(offset.toLong()) } }
    val startMillis = remember(endDate, trendDays, zone) { endDate.minusDays((trendDays - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli() }
    val endMillis = remember(endDate, zone) { endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
    val rangeRecords = remember(records, startMillis, endMillis) {
        records.filter { it.wakeUpAt != null && it.sleepStartAt in startMillis until endMillis }
    }
    val dayTotals = remember(rangeRecords, zone) {
        rangeRecords.groupBy { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.fold(DaySleepTotals()) { totals, record ->
                    val minutes = record.durationMinutes() ?: 0L
                    if (record.kind == SleepKind.NIGHT_SLEEP) totals.copy(night = totals.night + minutes)
                    else totals.copy(nap = totals.nap + minutes)
                }
            }
    }
    val validDays = remember(dayTotals) { dayTotals.size }
    val totalMinutes = remember(rangeRecords) { rangeRecords.sumOf { it.durationMinutes() ?: 0L } }
    val nightRecords = remember(rangeRecords) { rangeRecords.filter { it.kind == SleepKind.NIGHT_SLEEP } }
    val napRecords = remember(rangeRecords) { rangeRecords.filter { it.kind == SleepKind.NAP } }
    val nightMinutes = remember(nightRecords) { nightRecords.sumOf { it.durationMinutes() ?: 0L } }
    val napMinutes = remember(napRecords) { napRecords.sumOf { it.durationMinutes() ?: 0L } }
    val nightDays = remember(nightRecords, zone) { nightRecords.map { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }.distinct().size }
    val napDays = remember(napRecords, zone) { napRecords.map { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }.distinct().size }
    val totalNocturia = remember(nightRecords) { nightRecords.sumOf { it.nocturia.size } }
    val totalNocturiaMinutes = remember(nightRecords) { nightRecords.sumOf { record -> record.nocturia.sumOf { (it.endAt - it.startAt).coerceAtLeast(0L) / 60_000L } } }

    val nightColor = MaterialTheme.colorScheme.primary
    val napColor = MaterialTheme.colorScheme.secondary
    val entries = remember(dates, dayTotals, nightColor, napColor) {
        dates.map { date ->
            val totals = dayTotals[date] ?: DaySleepTotals()
            DateStackedBarEntry(
                date = date,
                label = "%02d-%02d".format(date.monthValue, date.dayOfMonth),
                segments = buildList {
                    if (totals.night > 0L) add(DateStackedBarSegment("night", totals.night.toDouble(), nightColor))
                    if (totals.nap > 0L) add(DateStackedBarSegment("nap", totals.nap.toDouble(), napColor))
                },
            )
        }
    }
    var selectedDate by remember(trendDays) { mutableStateOf(dates.firstOrNull()) }
    val selectedEntry = entries.firstOrNull { it.date == selectedDate }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (records.isEmpty()) {
            item {
                Text(stringResource(R.string.sleep_statistics_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@LazyColumn
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sleep_statistics_trend), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SleepStatKind.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = statKind == option,
                            onClick = { statKind = option },
                            shape = SegmentedButtonDefaults.itemShape(index, SleepStatKind.entries.size),
                            label = { Text(stringResource(option.labelRes())) },
                        )
                    }
                }
                when (statKind) {
                    SleepStatKind.ALL -> StatRow {
                        SleepStatAmount(stringResource(R.string.sleep_statistics_record_count), rangeRecords.size.toString())
                        SleepStatAmount(stringResource(R.string.sleep_statistics_total_duration), formatSleepDuration(totalMinutes))
                        SleepStatAmount(stringResource(R.string.sleep_statistics_average_duration), formatAverage(totalMinutes.toDouble(), validDays))
                    }
                    SleepStatKind.NIGHT -> {
                        StatRow {
                            SleepStatAmount(stringResource(R.string.sleep_statistics_night_count), nightRecords.size.toString())
                            SleepStatAmount(stringResource(R.string.sleep_statistics_night_total_duration), formatSleepDuration(nightMinutes))
                            SleepStatAmount(stringResource(R.string.sleep_statistics_night_average_duration), formatAverage(nightMinutes.toDouble(), nightDays))
                        }
                        StatRow {
                            SleepStatAmount(stringResource(R.string.sleep_statistics_night_average_start), averageClockTime(nightRecords.map { it.sleepStartAt }, zone))
                            SleepStatAmount(stringResource(R.string.sleep_statistics_night_average_wake), averageClockTime(nightRecords.mapNotNull { it.wakeUpAt }, zone))
                        }
                        StatRow {
                            SleepStatAmount(stringResource(R.string.sleep_statistics_total_nocturia), totalNocturia.toString())
                            SleepStatAmount(stringResource(R.string.sleep_statistics_average_nocturia), formatAverageCount(totalNocturia, nightDays))
                        }
                        StatRow {
                            SleepStatAmount(stringResource(R.string.sleep_statistics_nocturia_total_duration), formatSleepDuration(totalNocturiaMinutes))
                            SleepStatAmount(stringResource(R.string.sleep_statistics_nocturia_average_duration), formatAverage(totalNocturiaMinutes.toDouble(), nightDays))
                        }
                    }
                    SleepStatKind.NAP -> {
                        StatRow {
                            SleepStatAmount(stringResource(R.string.sleep_statistics_nap_count), napRecords.size.toString())
                            SleepStatAmount(stringResource(R.string.sleep_statistics_nap_total_duration), formatSleepDuration(napMinutes))
                            SleepStatAmount(stringResource(R.string.sleep_statistics_nap_average_duration), formatAverage(napMinutes.toDouble(), napDays))
                        }
                        StatRow {
                            SleepStatAmount(stringResource(R.string.sleep_statistics_nap_average_start), averageClockTime(napRecords.map { it.sleepStartAt }, zone))
                            SleepStatAmount(stringResource(R.string.sleep_statistics_nap_average_wake), averageClockTime(napRecords.mapNotNull { it.wakeUpAt }, zone))
                        }
                    }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(7, 30).forEachIndexed { index, optionDays ->
                        SegmentedButton(
                            selected = trendDays == optionDays,
                            onClick = { trendDays = optionDays },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            label = { Text(stringResource(if (optionDays == 7) R.string.sleep_statistics_7_days else R.string.sleep_statistics_30_days)) },
                        )
                    }
                }
                val nonZeroTotals = entries.map { entry -> entry.segments.sumOf(DateStackedBarSegment::value) }.filter { it > 0.0 }
                val averageValue = remember(nonZeroTotals) {
                    if (nonZeroTotals.isEmpty()) null else nonZeroTotals.average()
                }
                DateStackedBarChart(
                    entries = entries,
                    yAxisTitle = stringResource(R.string.sleep_statistics_daily_sleep),
                    formatValue = ::formatMinutesAxis,
                    labelEvery = 1,
                    selectedEntry = selectedEntry,
                    onEntrySelected = { selectedDate = it.date },
                    referenceLine = averageValue?.let { average ->
                        DateStackedBarReferenceLine(
                            value = average,
                            label = stringResource(R.string.sleep_statistics_average, formatSleepDuration(average.toLong())),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    },
                )
                SleepDayDetail(
                    entry = selectedEntry,
                    records = rangeRecords,
                    zone = zone,
                    nightColor = nightColor,
                    napColor = napColor,
                )
            }
        }
    }
}

private fun SleepStatKind.labelRes(): Int = when (this) {
    SleepStatKind.ALL -> R.string.sleep_statistics_kind_all
    SleepStatKind.NIGHT -> R.string.sleep_kind_night
    SleepStatKind.NAP -> R.string.sleep_kind_nap
}

@Composable
private fun StatRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.Bottom,
    ) {
        content()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SleepStatAmount(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SleepDayDetail(
    entry: DateStackedBarEntry<LocalDate>?,
    records: List<SleepRecord>,
    zone: ZoneId,
    nightColor: androidx.compose.ui.graphics.Color,
    napColor: androidx.compose.ui.graphics.Color,
) {
    val date = entry?.date ?: return
    val total = entry.segments.sumOf(DateStackedBarSegment::value).toLong()
    val night = entry.segments.firstOrNull { it.id == "night" }?.value?.toLong() ?: 0L
    val nap = entry.segments.firstOrNull { it.id == "nap" }?.value?.toLong() ?: 0L
    val dayRecords = remember(records, date, zone) {
        records.filter { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() == date }
            .sortedBy { it.sleepStartAt }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.sleep_statistics_selected_day, entry.label, formatSleepDuration(total)), style = MaterialTheme.typography.titleSmall)
        if (entry.segments.isEmpty()) {
            Text(stringResource(R.string.sleep_statistics_selected_day_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                SleepLegend(color = nightColor, label = stringResource(R.string.sleep_kind_night), minutes = night)
                androidx.compose.foundation.layout.Spacer(Modifier.padding(start = 12.dp))
                SleepLegend(color = napColor, label = stringResource(R.string.sleep_kind_nap), minutes = nap)
            }
        }
        dayRecords.forEach { record ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = stringResource(record.kind.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (record.kind == SleepKind.NIGHT_SLEEP) nightColor else napColor,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = formatRecordTimestamp(record.sleepStartAt, RecordTimePrecision.MINUTE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(" → ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                record.wakeUpAt?.let { wake ->
                    Text(formatRecordTimestamp(wake, RecordTimePrecision.MINUTE), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (record.kind == SleepKind.NIGHT_SLEEP && record.nocturia.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.sleep_nocturia_count_value, record.nocturia.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepLegend(color: androidx.compose.ui.graphics.Color, label: String, minutes: Long) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size(10.dp).padding(end = 6.dp)) { drawCircle(color) }
        Text("$label ${formatSleepDuration(minutes)}", style = MaterialTheme.typography.bodySmall)
    }
}

/** 平均入睡/醒来时间（24h 环形均值，正确处理跨零点）。 */
internal fun averageClockTime(timestamps: List<Long>, zone: ZoneId): String {
    if (timestamps.isEmpty()) return "-"
    val secondsOfDay = timestamps.map { Instant.ofEpochMilli(it).atZone(zone).toLocalTime().toSecondOfDay() }
    val meanX = secondsOfDay.sumOf { cos(it * 2.0 * PI / SECONDS_PER_DAY) } / secondsOfDay.size
    val meanY = secondsOfDay.sumOf { sin(it * 2.0 * PI / SECONDS_PER_DAY) } / secondsOfDay.size
    val meanSeconds = ((atan2(meanY, meanX) / (2.0 * PI)) * SECONDS_PER_DAY).let {
        ((it % SECONDS_PER_DAY) + SECONDS_PER_DAY) % SECONDS_PER_DAY
    }.roundToInt()
    return "%02d:%02d".format(meanSeconds / 3600, (meanSeconds % 3600) / 60)
}

private const val SECONDS_PER_DAY = 86_400.0

private fun formatSleepDuration(minutes: Long): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
}

private fun formatAverage(total: Double, days: Int): String =
    if (days <= 0) "-" else formatSleepDuration((total / days).toLong())

private fun formatAverageCount(total: Int, days: Int): String {
    if (days <= 0) return "-"
    val average = total.toDouble() / days
    return if (average % 1.0 == 0.0) average.toInt().toString() else "%.1f".format(average)
}

private fun formatMinutesAxis(value: Double): String = formatSleepDuration(value.toLong())
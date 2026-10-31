package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.formatRecordClockMinute
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarChart
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarEntry
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarReferenceLine
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarSegment
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterChip
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
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

/**
 * 睡眠统计。
 *
 * 这一版把「一列数字 + 一张图 + 一段明细」拆成三张卡，每张卡只回答一个问题：
 *
 * 1. **概览卡**：这段时间总量多少、平均每天多少（可切全部 / 夜间 / 小憩）。
 * 2. **趋势卡**：按天画堆叠柱，附一条平均线；7 / 30 天可切。
 * 3. **当日明细卡**：点某根柱，看那天的每一条记录。
 *
 * 旧版这三块共用一列 `Column`，中间用两个 M3 `SegmentedButton` 分隔，读者分不清
 * 「已选类型」和「时间范围」分别作用在谁身上；现在各自进卡、标题写在卡头，
 * 分段控件统一换成全站的 `EqualWidthSegmentedTabs` 与胶囊筛选。
 */
@Composable
internal fun SleepStatisticsTab(records: List<SleepRecord>, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
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

    val nightColor = tokens.colors.primary
    val napColor = tokens.colors.textMuted
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
        contentPadding = PaddingValues(
            start = spacing.page,
            end = spacing.page,
            top = spacing.s3,
            bottom = spacing.s6,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        if (records.isEmpty()) {
            item {
                AppEmptyState(
                    title = stringResource(R.string.record_no_data),
                    description = stringResource(R.string.sleep_statistics_empty),
                    iconRes = R.drawable.ic_sleep,
                )
            }
            return@LazyColumn
        }
        item(key = "summary") {
            AppSectionCard(
                iconRes = R.drawable.ic_sleep,
                title = stringResource(R.string.sleep_statistics_summary),
                action = {
                    Text(
                        text = stringResource(
                            if (trendDays == 7) R.string.sleep_statistics_7_days else R.string.sleep_statistics_30_days,
                        ),
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                    )
                },
            ) {
                EqualWidthSegmentedTabs(
                    tabs = SleepStatKind.entries.map { EqualWidthTab.text(stringResource(it.labelRes())) },
                    selectedIndex = SleepStatKind.entries.indexOf(statKind),
                    onSelected = { statKind = SleepStatKind.entries[it] },
                )
                Spacer(Modifier.height(spacing.s3))
                when (statKind) {
                    SleepStatKind.ALL -> {
                        MetricRow(
                            stringResource(R.string.sleep_statistics_record_count) to stringResource(
                                R.string.sleep_statistics_count_value,
                                rangeRecords.size,
                            ),
                            stringResource(R.string.sleep_statistics_total_duration) to sleepDurationText(totalMinutes),
                            stringResource(R.string.sleep_statistics_average_duration) to formatAverage(totalMinutes.toDouble(), validDays),
                        )
                    }
                    SleepStatKind.NIGHT -> {
                        MetricRow(
                            stringResource(R.string.sleep_statistics_night_count) to stringResource(
                                R.string.sleep_statistics_count_value,
                                nightRecords.size,
                            ),
                            stringResource(R.string.sleep_statistics_night_total_duration) to sleepDurationText(nightMinutes),
                            stringResource(R.string.sleep_statistics_night_average_duration) to formatAverage(nightMinutes.toDouble(), nightDays),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_night_average_start) to averageClockTime(nightRecords.map { it.sleepStartAt }, zone),
                            stringResource(R.string.sleep_statistics_night_average_wake) to averageClockTime(nightRecords.mapNotNull { it.wakeUpAt }, zone),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_total_nocturia) to stringResource(
                                R.string.sleep_statistics_count_value,
                                totalNocturia,
                            ),
                            stringResource(R.string.sleep_statistics_average_nocturia) to formatAverageCount(totalNocturia, nightDays),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_nocturia_total_duration) to sleepDurationText(totalNocturiaMinutes),
                            stringResource(R.string.sleep_statistics_nocturia_average_duration) to formatAverage(totalNocturiaMinutes.toDouble(), nightDays),
                        )
                    }
                    SleepStatKind.NAP -> {
                        MetricRow(
                            stringResource(R.string.sleep_statistics_nap_count) to stringResource(
                                R.string.sleep_statistics_count_value,
                                napRecords.size,
                            ),
                            stringResource(R.string.sleep_statistics_nap_total_duration) to sleepDurationText(napMinutes),
                            stringResource(R.string.sleep_statistics_nap_average_duration) to formatAverage(napMinutes.toDouble(), napDays),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_nap_average_start) to averageClockTime(napRecords.map { it.sleepStartAt }, zone),
                            stringResource(R.string.sleep_statistics_nap_average_wake) to averageClockTime(napRecords.mapNotNull { it.wakeUpAt }, zone),
                        )
                    }
                }
            }
        }
        item(key = "trend") {
            AppSectionCard(
                iconRes = R.drawable.ic_chart,
                title = stringResource(R.string.sleep_statistics_trend),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    listOf(7, 30).forEach { optionDays ->
                        AppFilterChip(
                            text = stringResource(if (optionDays == 7) R.string.sleep_statistics_7_days else R.string.sleep_statistics_30_days),
                            selected = trendDays == optionDays,
                            onClick = { trendDays = optionDays },
                        )
                    }
                }
                Spacer(Modifier.height(spacing.s2))
                val nonZeroTotals = entries.map { entry -> entry.segments.sumOf(DateStackedBarSegment::value) }.filter { it > 0.0 }
                val averageValue = remember(nonZeroTotals) {
                    if (nonZeroTotals.isEmpty()) null else nonZeroTotals.average()
                }
                DateStackedBarChart(
                    entries = entries,
                    yAxisTitle = stringResource(R.string.sleep_statistics_daily_sleep),
                    formatValue = ::formatMinutesAxis,
                    // 7 天用 40 dp 一列正好在卡内放下（52 dp 会多出二十几 dp，把最右边那天切掉一半）；
                    // 30 天横滚时也不至于一根柱占满半屏。
                    labelEvery = if (trendDays <= 7) 1 else 2,
                    columnWidth = 40.dp,
                    selectedEntry = selectedEntry,
                    onEntrySelected = { selectedDate = it.date },
                    referenceLine = averageValue?.let { average ->
                        DateStackedBarReferenceLine(
                            value = average,
                            label = stringResource(R.string.sleep_statistics_average, formatMinutesAxis(average)),
                            color = tokens.colors.warning,
                        )
                    },
                )
                Spacer(Modifier.height(spacing.s2))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s3)) {
                    LegendDot(nightColor, stringResource(R.string.sleep_kind_night))
                    LegendDot(napColor, stringResource(R.string.sleep_kind_nap))
                }
            }
        }
        item(key = "day") {
            val entry = selectedEntry
            AppSectionCard(
                iconRes = R.drawable.ic_list,
                // 卡标题里的总时长与其它卡用同一套写法（「8 小时」而不是图轴上的「8h」）。
                title = entry?.let {
                    stringResource(
                        R.string.sleep_statistics_selected_day,
                        it.label,
                        sleepDurationText(it.segments.sumOf(DateStackedBarSegment::value).toLong()),
                    )
                } ?: stringResource(R.string.sleep_statistics_day_detail),
            ) {
                if (entry == null || entry.segments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.sleep_statistics_selected_day_empty),
                        style = tokens.type.bodyS,
                        color = tokens.colors.textMuted,
                    )
                } else {
                    val dayRecords = remember(rangeRecords, entry.date, zone) {
                        rangeRecords.filter { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() == entry.date }
                            .sortedBy { it.sleepStartAt }
                    }
                    dayRecords.forEach { record ->
                        DayRecordRow(record, nightColor, napColor)
                    }
                }
            }
        }
    }
}

private fun SleepStatKind.labelRes(): Int = when (this) {
    SleepStatKind.ALL -> R.string.sleep_statistics_kind_all
    SleepStatKind.NIGHT -> R.string.sleep_kind_night
    SleepStatKind.NAP -> R.string.sleep_kind_nap
}

/** 三个并列指标：标签在上、数值在下，等宽分布；数值用 `titleS`，与卡标题同级但不着色。 */
@Composable
private fun MetricRow(vararg metrics: Pair<String, String>) {
    val tokens = LocalAppTokens.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        metrics.forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(tokens.spacing.s1))
                Text(
                    text = value,
                    // 三列各占 1/3 宽，「8 小时 0 分」用 `titleS`（24 sp）会顶到卡片边上；
                    // 降到正文级 + SemiBold，靠字重而不是字号拉开层次。
                    style = tokens.type.body,
                    color = tokens.colors.text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // 只有两个指标时补一格空白，保持与三格行同样的列宽节奏。
        if (metrics.size == 2) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    val tokens = LocalAppTokens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(text = label, style = tokens.type.caption, color = tokens.colors.textMuted)
    }
}

/** 当日的一条记录：类别 → 时间 → 时长，右侧夜尿次数。行与行之间靠 12 dp 间距分组，不画线。 */
@Composable
private fun DayRecordRow(record: SleepRecord, nightColor: androidx.compose.ui.graphics.Color, napColor: androidx.compose.ui.graphics.Color) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.label)
    val accent = if (record.kind == SleepKind.NIGHT_SLEEP) nightColor else napColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = tokens.spacing.s2, vertical = 2.dp),
        ) {
            Text(
                text = stringResource(record.kind.labelRes()),
                style = tokens.type.caption,
                color = accent,
            )
        }
        Spacer(Modifier.width(tokens.spacing.s2))
        Text(
            text = buildString {
                append(formatRecordClockMinute(record.sleepStartAt))
                record.wakeUpAt?.let { append(" → ").append(formatRecordClockMinute(it)) }
            },
            style = tokens.type.bodyS,
            color = tokens.colors.textMuted,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = record.durationMinutes()?.let { sleepDurationText(it) }.orEmpty(),
            style = tokens.type.bodyS,
            color = tokens.colors.text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (record.kind == SleepKind.NIGHT_SLEEP && record.nocturia.isNotEmpty()) {
            Spacer(Modifier.width(tokens.spacing.s2))
            Text(
                text = stringResource(R.string.sleep_nocturia_count_value, record.nocturia.size),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
            )
        }
    }
}

/** 平均入睡 / 醒来时间（24h 环形均值，正确处理跨零点）。 */
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

@Composable
private fun formatAverage(total: Double, days: Int): String =
    if (days <= 0) "-" else sleepDurationText((total / days).toLong())

private fun formatAverageCount(total: Int, days: Int): String {
    if (days <= 0) return "-"
    val average = total.toDouble() / days
    return if (average % 1.0 == 0.0) average.toInt().toString() else "%.1f".format(average)
}

/** 图表轴标要短：`7h` 而不是 `7 小时 0 分`，一格轴宽放不下整串。 */
private fun formatMinutesAxis(value: Double): String {
    val minutes = value.toLong()
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${rest}m"
    }
}

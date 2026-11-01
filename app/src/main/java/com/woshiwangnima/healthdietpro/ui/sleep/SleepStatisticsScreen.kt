package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.formatRecordClockMinute
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarChart
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarEntry
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarReferenceLine
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarSegment
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterActionButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterChip
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSplitCardBody
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSplitCardHead
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.durationMinutes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class DaySleepTotals(val night: Long = 0L, val nap: Long = 0L) {
    val total: Long get() = night + nap
}

/** 统计要看的类别：全部 / 仅夜间睡眠 / 仅小憩。 */
private enum class SleepStatKind { ALL, NIGHT, NAP }

/**
 * 时间范围。`days == null` 表示「全部」（从最早一条记录算起）。
 *
 * 90 天与全部这两档是给「想看看最近几个月到底怎么睡的」用的；7 / 30 天负责日常扫一眼。
 *
 * 枚举顺序就是胶囊顺序：**【全部】排在最左**。它既是「不加时间限制」的默认值，
 * 也是读完一行胶囊后想退回的那个位置；放在中间或末尾时，用户点完【近 7 天】要换回来
 * 得把整行扫一遍，而且按「限制由弱到强」读下来，开头才是「什么都不限」。
 */
private enum class SleepRange(val days: Int?) {
    ALL(null),
    D7(7),
    D30(30),
    D90(90),
}

/**
 * 睡眠统计。
 *
 * **筛选只有一个地方**：顶部一张「查找与筛选」卡（时间范围 + 记录类型），下面三张卡
 * （概览 / 睡眠时长趋势 / 当日明细）都读同一份筛完的数据。
 *
 * 上一版把范围写在趋势卡的胶囊里、把类别写在概览卡的分段控件里，于是「概览说 8 小时、
 * 趋势那几天却对不上」这类疑问每次都要先想一遍「现在哪些条件在生效」。现在：
 *
 * 1. 条件填在顶部一张卡里，**点到即筛**（同【记录】页的类别胶囊）：选项一共七个、
 *    每点一下三张卡的重算代价也就是一次过滤求和，中间再插一道【开始查找】按钮，
 *    等于让用户每次换条件都多点一下，还多了「改了但没应用」这种看不见的中间态。
 * 2. 卡片不再各自带筛选，标题只说明这张卡回答什么问题。
 * 3. 图与明细共用类别代表色（靛蓝＝夜间睡眠、暖橙＝小憩），并用与【记录】页同一
 *    个类别文本卡组件，跨屏不会读出两套标签样式。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SleepStatisticsTab(records: List<SleepRecord>, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val zone = remember { ZoneId.systemDefault() }
    // 点到即筛：不存在「草稿没应用」的中间态，七个选项各点各的，三张卡立刻跟着变。
    var range by rememberSaveable { mutableStateOf(SleepRange.D7) }
    var kind by rememberSaveable { mutableStateOf(SleepStatKind.ALL) }
    // 「清除筛选」＝ 把条件退回「不加限制」：全部时间 + 全部类别。
    // 注意它清的不是「回到默认的近 7 天」——默认值本身就是一道时间限制，清完还留着它等于没清。
    val hasFilters = range != SleepRange.ALL || kind != SleepStatKind.ALL

    val finished = remember(records) { records.filter { it.wakeUpAt != null && it.durationMinutes() != null } }
    val today = remember(zone) { LocalDate.now(zone) }
    val endDate = today
    val startDate = remember(range, finished, endDate, zone) {
        range.days?.let { endDate.minusDays((it - 1).toLong()) }
            ?: finished.minByOrNull { it.sleepStartAt }
                ?.let { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }
                ?.takeIf { it.isBefore(endDate) }
            ?: endDate
    }
    val dayCount = remember(startDate, endDate) { ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1 }
    // 时间轴从左到右是「早 → 近」：柱图是时间序列，读者默认向右读近。
    val dates = remember(startDate, dayCount) { (0 until dayCount).map { startDate.plusDays(it.toLong()) } }
    val startMillis = remember(startDate, zone) { startDate.atStartOfDay(zone).toInstant().toEpochMilli() }
    val endMillis = remember(endDate, zone) { endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
    val rangeRecords = remember(finished, startMillis, endMillis) {
        finished.filter { it.sleepStartAt in startMillis until endMillis }
    }
    val scopedRecords = remember(rangeRecords, kind) {
        when (kind) {
            SleepStatKind.ALL -> rangeRecords
            SleepStatKind.NIGHT -> rangeRecords.filter { it.kind == SleepKind.NIGHT_SLEEP }
            SleepStatKind.NAP -> rangeRecords.filter { it.kind == SleepKind.NAP }
        }
    }
    val dayTotals = remember(scopedRecords, zone) {
        scopedRecords.groupBy { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }
            .mapValues { (_, dayRecords) ->
                dayRecords.fold(DaySleepTotals()) { totals, record ->
                    val minutes = record.durationMinutes() ?: 0L
                    if (record.kind == SleepKind.NIGHT_SLEEP) totals.copy(night = totals.night + minutes)
                    else totals.copy(nap = totals.nap + minutes)
                }
            }
    }
    val validDays = remember(dayTotals) { dayTotals.size }
    val totalMinutes = remember(scopedRecords) { scopedRecords.sumOf { it.durationMinutes() ?: 0L } }
    val distinctDays = remember(scopedRecords, zone) {
        scopedRecords.map { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }.distinct().size
    }
    val totalNocturia = remember(scopedRecords) { scopedRecords.sumOf { it.nocturia.size } }
    val totalNocturiaMinutes = remember(scopedRecords) {
        scopedRecords.sumOf { record -> record.nocturia.sumOf { (it.endAt - it.startAt).coerceAtLeast(0L) / 60_000L } }
    }

    val nightColor = tokens.sleep.night
    val napColor = tokens.sleep.nap
    val showNight = kind != SleepStatKind.NAP
    val showNap = kind != SleepStatKind.NIGHT
    val entries = remember(dates, dayTotals, nightColor, napColor, showNight, showNap) {
        dates.map { date ->
            val totals = dayTotals[date] ?: DaySleepTotals()
            DateStackedBarEntry(
                date = date,
                label = "%02d-%02d".format(date.monthValue, date.dayOfMonth),
                segments = buildList {
                    if (showNight && totals.night > 0L) add(DateStackedBarSegment("night", totals.night.toDouble(), nightColor))
                    if (showNap && totals.nap > 0L) add(DateStackedBarSegment("nap", totals.nap.toDouble(), napColor))
                },
            )
        }
    }
    // 默认定位到最近一条有数据的那天，而不是「今天」：今天还没记录时，明细卡会显示
    // 「11-01 · 0 分钟 / 当天没有睡眠记录」，读者第一眼会以为统计坏了。
    var pickedDate by remember(startDate, dayCount) { mutableStateOf<LocalDate?>(null) }
    val defaultDate = remember(entries) {
        entries.lastOrNull { it.segments.isNotEmpty() }?.date ?: entries.lastOrNull()?.date
    }
    val selectedEntry = entries.firstOrNull { it.date == (pickedDate ?: defaultDate) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // 列表自己不加 item 间距、也不留顶部 contentPadding：
        // 筛选卡被拆成「卡头（悬浮）+ 条件（滚动）」两片，只有间隔为 0 它们才接成一张卡；
        // contentPadding 的那条留白又不裁内容，悬浮片吸顶后会从上方那条缝里漏出滚动中的卡片
        // 所以顶部的页边距画在卡头片自己的不透明底色里。其余卡片的间距各自用 padding 给。
        contentPadding = PaddingValues(
            start = spacing.page,
            end = spacing.page,
            bottom = spacing.s6,
        ),
    ) {
        if (records.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(top = spacing.s3)) {
                    AppEmptyState(
                        title = stringResource(R.string.record_no_data),
                        description = stringResource(R.string.sleep_statistics_empty),
                        iconRes = R.drawable.ic_sleep,
                    )
                }
            }
            return@LazyColumn
        }
        // 【清除筛选】写在卡头里、紧贴【查找与筛选】标题下，并且随列表吸顶：
        // 它退的是「现在生效的条件」，而这份疑问通常出现在滚过三张结果卡之后——
        // 埋在卡尾的话，得先滑回顶部才行（§1.3 主要操作不该随内容滚走）。
        // 卡头单独做 stickyHeader、条件留在普通 item：卡里的内容是随整张卡一起滚的，
        // 只有列表直属的条目才能吸顶，所以整张卡必须分成两片——好在贴在一起看仍是一张卡。
        stickyHeader(key = "filter_actions") {
            SleepFilterCardHead(
                canClear = hasFilters,
                onClear = {
                    range = SleepRange.ALL
                    kind = SleepStatKind.ALL
                },
            )
        }
        item(key = "filter") {
            SleepFilterCardBody(
                range = range,
                kind = kind,
                onSelectRange = { range = it },
                onSelectKind = { kind = it },
            )
        }
        item(key = "summary") {
            AppSectionCard(
                modifier = Modifier.padding(top = spacing.s3),
                iconRes = R.drawable.ic_sleep,
                title = stringResource(R.string.sleep_statistics_summary),
            ) {
                when (kind) {
                    SleepStatKind.ALL -> {
                        MetricRow(
                            stringResource(R.string.sleep_statistics_record_count) to stringResource(
                                R.string.sleep_statistics_count_value,
                                scopedRecords.size,
                            ),
                            stringResource(R.string.sleep_statistics_total_duration) to sleepDurationText(totalMinutes),
                            stringResource(R.string.sleep_statistics_average_duration) to formatAverage(totalMinutes.toDouble(), validDays),
                        )
                    }
                    SleepStatKind.NIGHT -> {
                        MetricRow(
                            stringResource(R.string.sleep_statistics_night_count) to stringResource(
                                R.string.sleep_statistics_count_value,
                                scopedRecords.size,
                            ),
                            stringResource(R.string.sleep_statistics_night_total_duration) to sleepDurationText(totalMinutes),
                            stringResource(R.string.sleep_statistics_night_average_duration) to formatAverage(totalMinutes.toDouble(), validDays),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_night_average_start) to averageClockTime(scopedRecords.map { it.sleepStartAt }, zone),
                            stringResource(R.string.sleep_statistics_night_average_wake) to averageClockTime(scopedRecords.mapNotNull { it.wakeUpAt }, zone),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_total_nocturia) to stringResource(
                                R.string.sleep_statistics_count_value,
                                totalNocturia,
                            ),
                            stringResource(R.string.sleep_statistics_average_nocturia) to formatAverageCount(totalNocturia, distinctDays),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_nocturia_total_duration) to sleepDurationText(totalNocturiaMinutes),
                            stringResource(R.string.sleep_statistics_nocturia_average_duration) to formatAverage(totalNocturiaMinutes.toDouble(), distinctDays),
                        )
                    }
                    SleepStatKind.NAP -> {
                        MetricRow(
                            stringResource(R.string.sleep_statistics_nap_count) to stringResource(
                                R.string.sleep_statistics_count_value,
                                scopedRecords.size,
                            ),
                            stringResource(R.string.sleep_statistics_nap_total_duration) to sleepDurationText(totalMinutes),
                            stringResource(R.string.sleep_statistics_nap_average_duration) to formatAverage(totalMinutes.toDouble(), validDays),
                        )
                        Spacer(Modifier.height(spacing.s3))
                        MetricRow(
                            stringResource(R.string.sleep_statistics_nap_average_start) to averageClockTime(scopedRecords.map { it.sleepStartAt }, zone),
                            stringResource(R.string.sleep_statistics_nap_average_wake) to averageClockTime(scopedRecords.mapNotNull { it.wakeUpAt }, zone),
                        )
                    }
                }
            }
        }
        item(key = "trend") {
            AppSectionCard(
                modifier = Modifier.padding(top = spacing.s3),
                iconRes = R.drawable.ic_chart,
                title = stringResource(R.string.sleep_statistics_trend),
            ) {
                val nonZeroTotals = entries.map { entry -> entry.segments.sumOf(DateStackedBarSegment::value) }.filter { it > 0.0 }
                val averageValue = remember(nonZeroTotals) {
                    if (nonZeroTotals.isEmpty()) null else nonZeroTotals.average()
                }
                DateStackedBarChart(
                    entries = entries,
                    yAxisTitle = stringResource(R.string.sleep_statistics_daily_sleep),
                    formatValue = ::formatMinutesAxis,
                    // 天数越多柱越窄：7 天用 40 dp 一列正好在卡内放下；90 天乃至全部必须靠横滚，
                    // 日期标签也只每隔几根写一次，否则每根柱底下都挤一串字。
                    labelEvery = when {
                        dayCount <= 7 -> 1
                        dayCount <= 31 -> 2
                        else -> 7
                    },
                    columnWidth = if (dayCount <= 31) 40.dp else 30.dp,
                    // 天数多时先贴最右：最近几天的柱子才是打开这一屏想看的东西。
                    scrollToEnd = true,
                    selectedEntry = selectedEntry,
                    onEntrySelected = { pickedDate = it.date },
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
                    if (showNight) LegendDot(nightColor, stringResource(R.string.sleep_kind_night))
                    if (showNap) LegendDot(napColor, stringResource(R.string.sleep_kind_nap))
                }
            }
        }
        item(key = "day") {
            val entry = selectedEntry
            AppSectionCard(
                modifier = Modifier.padding(top = spacing.s3),
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
                    val dayRecords = remember(scopedRecords, entry.date, zone) {
                        scopedRecords.filter { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() == entry.date }
                            .sortedBy { it.sleepStartAt }
                    }
                    dayRecords.forEach { record ->
                        DayRecordRow(record)
                    }
                }
            }
        }
    }
}

/**
 * 筛选卡的**上半片**：卡标题 + 【清除筛选】，由 LazyColumn 的 stickyHeader 承载。
 *
 * 拆成两片只有一个理由——要把【清除筛选】留在用户手边：这一屏三张结果卡加起来比一屏长，
 * 「我刚才到底筛了什么」的疑问通常出现在看完趋势图之后，那时卡内的按钮早就滚出屏幕了。
 * 卡内的内容是随整张卡一起滚的，只有列表直属的条目能吸顶，所以卡必须切成「头 + 体」两片；
 * 两片共用同一套卡面（只有拐角不同）、列表里又不加 item 间距，静止时看起来仍是一张卡。
 *
 * 外层那圈是**页面底色**而不是透明：吸顶时它压在别的卡上，留白处必须挡住正在滚过的卡片。
 *
 * 这里**没有**【开始查找】按钮（【记物价】-【统计】这一版也一并去掉了）：本屏七个选项、
 * 每次换条件只是把几十条记录重新过滤求和，点到即刻出结果；中间那道按钮只剩下「多一次点击」
 * 和「改了但没应用」的困惑。
 */
@Composable
private fun SleepFilterCardHead(canClear: Boolean, onClear: () -> Unit) {
    AppSplitCardHead(
        iconRes = R.drawable.ic_search_empty,
        title = stringResource(R.string.sleep_filter_section),
    ) {
        // 卡标题下紧接着就是它：抬手就能把条件退回去，不用先看清下面两行筛的是什么。
        // 占满卡宽——「把条件退回去」是这一个区域里唯一要做的动作，没有理由只给它一半宽。
        AppFilterActionButton(
            text = stringResource(R.string.sleep_filter_clear),
            iconRes = R.drawable.ic_refresh,
            enabled = canClear,
            onClick = onClear,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 筛选卡的**下半片**：两行条件（时间范围 / 记录类型），跟着列表一起滚。
 *
 * 两行都是互斥单选，用胶囊而不是下拉：选项各只有三四个、文字又短，一眼就能比完，
 * 下拉要多点一次才能看到有哪些可选。点到即筛，没有「草稿 / 已应用」两份状态。
 */
@Composable
private fun SleepFilterCardBody(
    range: SleepRange,
    kind: SleepStatKind,
    onSelectRange: (SleepRange) -> Unit,
    onSelectKind: (SleepStatKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    AppSplitCardBody(modifier = modifier) {
        Text(
            text = stringResource(R.string.sleep_filter_range_label),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
        Spacer(Modifier.height(spacing.s1))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            SleepRange.entries.forEach { option ->
                AppFilterChip(
                    text = stringResource(option.labelRes()),
                    selected = range == option,
                    onClick = { onSelectRange(option) },
                )
            }
        }
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = stringResource(R.string.sleep_filter_kind_label),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
        Spacer(Modifier.height(spacing.s1))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            SleepStatKind.entries.forEach { option ->
                AppFilterChip(
                    text = stringResource(option.labelRes()),
                    selected = kind == option,
                    onClick = { onSelectKind(option) },
                )
            }
        }
    }
}

private fun SleepRange.labelRes(): Int = when (this) {
    SleepRange.D7 -> R.string.sleep_statistics_7_days
    SleepRange.D30 -> R.string.sleep_statistics_30_days
    SleepRange.D90 -> R.string.sleep_statistics_90_days
    SleepRange.ALL -> R.string.sleep_filter_all
}

private fun SleepStatKind.labelRes(): Int = when (this) {
    SleepStatKind.ALL -> R.string.sleep_filter_all
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
private fun RowScope.LegendDot(color: Color, label: String) {
    val tokens = LocalAppTokens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawCircle(color) }
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(text = label, style = tokens.type.caption, color = tokens.colors.textMuted)
    }
}

/** 当日的一条记录：类别文本卡 → 时间 → 时长，右侧夜尿次数。行与行之间靠 12 dp 间距分组，不画线。 */
@Composable
private fun DayRecordRow(record: SleepRecord) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SleepKindBadge(record.kind)
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

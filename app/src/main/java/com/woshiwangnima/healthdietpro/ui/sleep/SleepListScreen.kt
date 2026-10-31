package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.formatRecordClockMinute
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.timer.TimerInstance
import com.woshiwangnima.healthdietpro.common.timer.remainingNow
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterChip
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPillIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.durationMinutes
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 睡眠首页：记录 / 统计两个页签。
 *
 * 这一版的信息节奏（对齐《通用 UI 设计规范》§1.3 与移动端设计系统技能「信息流骨架」）：
 *
 * 1. **先结论后清单**：顶部一张概览卡直接回答「最近一次睡了多久、这几天的平均是多少」。
 *    旧版一进来只有一条通栏蓝按钮和一列窄窄的记录行，最简单的问题反而要去统计页翻。
 * 2. **一屏只有一个主操作**：原来顶部通栏【添加睡眠】和空态里的【添加睡眠】是同两个动作，
 *    互相争主次。现在主操作收成右下角常驻的胶囊按钮（56 dp 高、避开页签栏），
 *    空态只说「下一步做什么」，不再重复一个按钮。
 * 3. **历史按天分组**：记录按入睡日期归到「今天 / 昨天 / M 月 D 日」下，同一天多次小憩读起来是一组，
 *    而不是一串看不出间隔的卡片。
 * 4. **卡片内部重排层级**：类别标签 → 时长（主视觉）→ 起止时间 → 夜尿 / 备注 → 计时器。
 *    编辑与删除退到时间行末端的两枚小图标，不再和时长抢第一行。
 */
@Composable
internal fun SleepHomeScreen(
    uiState: SleepUiState,
    onAdd: () -> Unit,
    onEdit: (SleepRecord) -> Unit,
    onDelete: (String) -> Unit,
    onWakeUp: (String) -> Unit,
    onDeleteTimer: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val tabs = remember {
        listOf(
            DetailTabItem("records", R.string.sleep_tab_records, R.drawable.ic_list),
            DetailTabItem("statistics", R.string.sleep_tab_statistics, R.drawable.ic_chart),
        )
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    BaseScreen(
        title = stringResource(R.string.sleep_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
        actions = {
            AppPillIconTextButton(
                text = stringResource(R.string.sleep_settings_title),
                iconRes = R.drawable.ic_settings,
                onClick = onSettings,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding),
        ) {
            Box(Modifier.weight(1f)) {
                AnimatedPageContent(
                    selectedTab,
                    Modifier.fillMaxSize(),
                    direction = { initial, target -> target - initial },
                ) { tab ->
                    if (tab == 0) {
                        SleepRecordsTab(uiState, onEdit, onDelete, onWakeUp, onDeleteTimer)
                    } else {
                        SleepStatisticsTab(uiState.records)
                    }
                }
                // 主操作常驻：两条页签都看得见「记一笔」，但只在记录页出现——
                // 统计页的动线是「读」，把记录入口浮在上面会挡住图表走线。
                if (selectedTab == 0) {
                    AddSleepFab(
                        onClick = onAdd,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = tokens.spacing.page, bottom = tokens.spacing.s4),
                    )
                }
            }
            DetailTabBar(tabs, tabs[selectedTab].id) { item -> selectedTab = tabs.indexOf(item) }
        }
    }
}

/** 主操作：右下角浮起的胶囊按钮。阴影 ≤ 8 dp，属于浮层允许的层级提示（§2.7）。 */
@Composable
private fun AddSleepFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val label = stringResource(R.string.sleep_add_short)
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(tokens.radii.pill))
            .semantics { contentDescription = label },
    ) {
        AppIconTextButton(
            text = label,
            iconRes = R.drawable.ic_add,
            onClick = onClick,
            modifier = Modifier.heightIn(min = tokens.spacing.hitMin),
        )
    }
}

/** 一条记录按入睡日期归组后的一整天。 */
private data class SleepDayGroup(val date: LocalDate, val records: List<SleepRecord>)

@Composable
private fun SleepRecordsTab(
    uiState: SleepUiState,
    onEdit: (SleepRecord) -> Unit,
    onDelete: (String) -> Unit,
    onWakeUp: (String) -> Unit,
    onDeleteTimer: (String) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val zone = remember { ZoneId.systemDefault() }
    var filterKind by remember { mutableStateOf<SleepKind?>(null) }
    var deleting by remember { mutableStateOf<SleepRecord?>(null) }
    val filtered = uiState.records.filter { filterKind == null || it.kind == filterKind }
    val ongoing = filtered.filter { it.wakeUpAt == null }
    val finished = filtered.filter { it.wakeUpAt != null }
    val dayGroups = remember(finished, zone) {
        finished
            .groupBy { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }
            .toList()
            .sortedByDescending { it.first }
            .map { (date, records) -> SleepDayGroup(date, records.sortedByDescending { it.sleepStartAt }) }
    }
    val overview = remember(uiState.records, zone) { sleepOverview(uiState.records, zone) }
    val today = remember(zone) { LocalDate.now(zone) }

    if (uiState.records.isEmpty()) {
        // 一条记录都没有时不摆筛选（没东西可筛），空态在剩余空间里居中：
        // 顶到页头下面会读成「加载失败」，居中才读成「这里还没有内容」。
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AppEmptyState(
                title = stringResource(R.string.sleep_empty),
                description = stringResource(R.string.sleep_empty_hint),
                iconRes = R.drawable.ic_sleep,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = tokens.spacing.page),
        contentPadding = PaddingValues(
            top = tokens.spacing.s3,
            // 底部让出浮起的「记一笔」：按钮高 `hitMin`、离底 `s4`，再留 `s3` 的呼吸位，
            // 滚到底时最后一张卡能整张露在按钮上方。
            bottom = tokens.spacing.hitMin + tokens.spacing.s6,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
    ) {
        if (overview != null) {
            item(key = "overview") { SleepOverviewCard(overview) }
        }
        item(key = "filter") { SleepKindFilter(filterKind) { filterKind = it } }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                // 这里只剩「筛选后为空」一种情况：整库为空已在上面居中处理。
                AppEmptyState(
                    title = stringResource(R.string.sleep_filter_empty),
                    description = stringResource(R.string.sleep_filter_empty_hint),
                    iconRes = R.drawable.ic_sleep,
                )
            }
        }
        if (ongoing.isNotEmpty()) {
            item(key = "ongoing_label") {
                SleepGroupLabel(stringResource(R.string.sleep_ongoing), highlighted = true)
            }
            items(ongoing, key = { it.id }) { record ->
                SleepCard(
                    record = record,
                    timer = uiState.timerFor(record),
                    onWakeUp = { onWakeUp(record.id) },
                    onEdit = { onEdit(record) },
                    onDelete = { deleting = record },
                    onDeleteTimer = { record.timerId?.let(onDeleteTimer) },
                    highlighted = true,
                )
            }
        }
        dayGroups.forEach { group ->
            item(key = "day_${group.date}") {
                SleepDayLabel(group.date, today)
            }
            items(group.records, key = { it.id }) { record ->
                SleepCard(
                    record = record,
                    timer = uiState.timerFor(record),
                    onWakeUp = null,
                    onEdit = { onEdit(record) },
                    onDelete = { deleting = record },
                    onDeleteTimer = { record.timerId?.let(onDeleteTimer) },
                    highlighted = false,
                )
            }
        }
    }
    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.sleep_delete_message, sleepRecordLabel(record))) },
            confirmButton = {
                TextButton(onClick = { onDelete(record.id); deleting = null }) {
                    Text(stringResource(R.string.body_record_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
            },
        )
    }
}

/** 概览卡要的几个数：最近一次、近 7 日平均、近 7 日条数。 */
private data class SleepOverview(
    val latest: SleepRecord,
    val latestMinutes: Long,
    val average7Minutes: Long?,
    val records7: Int,
)

private fun sleepOverview(records: List<SleepRecord>, zone: ZoneId): SleepOverview? {
    val finished = records.filter { it.wakeUpAt != null && it.durationMinutes() != null }
    val latest = finished.maxByOrNull { it.sleepStartAt } ?: return null
    val since = LocalDate.now(zone).minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
    val recent = finished.filter { it.sleepStartAt >= since }
    val days = recent.map { Instant.ofEpochMilli(it.sleepStartAt).atZone(zone).toLocalDate() }.distinct().size
    val average = if (days > 0) recent.sumOf { it.durationMinutes() ?: 0L } / days else null
    return SleepOverview(
        latest = latest,
        latestMinutes = latest.durationMinutes() ?: 0L,
        // 只有一次记录时「平均」等于它自己，多写一列等于骗人。
        average7Minutes = average.takeIf { recent.size > 1 },
        records7 = recent.size,
    )
}

/**
 * 概览卡：左边是「最近一次」的时长（这一页最想知道的数），右边是近 7 日平均。
 *
 * 数字用 `titleL` + 主色而不上 `display`：这张卡在列表最上方，`display` 会把整页的
 * 视觉重心顶到一张总览卡上，下面的记录反而读不清（§2.4 一屏只留一处最高层级）。
 */
@Composable
private fun SleepOverviewCard(overview: SleepOverview) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val shape = RoundedCornerShape(tokens.radii.container)
    val latest = overview.latest
    val meta = buildList {
        add(sleepRangeText(latest))
        if (latest.kind == SleepKind.NIGHT_SLEEP && latest.nocturia.isNotEmpty()) {
            add(stringResource(R.string.sleep_nocturia_count_value, latest.nocturia.size))
        }
    }.joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.stroke, tokens.colors.border, shape)
            .padding(horizontal = spacing.s3, vertical = spacing.s3),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sleep_overview_last, stringResource(latest.kind.labelRes())),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(spacing.s1))
                Text(
                    text = sleepDurationText(overview.latestMinutes),
                    style = tokens.type.titleL,
                    color = tokens.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(spacing.s1))
                Text(
                    text = meta,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            overview.average7Minutes?.let { average ->
                Spacer(Modifier.size(spacing.s3))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sleep_overview_average_7),
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(spacing.s1))
                    Text(
                        text = sleepDurationText(average),
                        style = tokens.type.titleS,
                        color = tokens.colors.text,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(spacing.s1))
                    Text(
                        text = stringResource(R.string.sleep_overview_records_7, overview.records7),
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 分组小标题：进行中 / 某一天。只做分隔，不占一张卡。 */
@Composable
private fun SleepGroupLabel(text: String, highlighted: Boolean) {
    val tokens = LocalAppTokens.current
    Text(
        text = text,
        style = tokens.type.bodyS,
        color = if (highlighted) tokens.colors.primary else tokens.colors.textMuted,
        modifier = Modifier.padding(start = tokens.spacing.s1, top = tokens.spacing.s2),
    )
}

/** 日期分隔：今天 / 昨天 / M 月 D 日。比「2026-10-31」快一步读出来。 */
@Composable
private fun SleepDayLabel(date: LocalDate, today: LocalDate) {
    val text = when (date) {
        today -> stringResource(R.string.sleep_day_today)
        today.minusDays(1) -> stringResource(R.string.sleep_day_yesterday)
        else -> stringResource(R.string.sleep_day_month_day, date.monthValue, date.dayOfMonth)
    }
    SleepGroupLabel(text, highlighted = false)
}

@Composable
private fun SleepKindFilter(selected: SleepKind?, onFilterKindChange: (SleepKind?) -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        AppFilterChip(
            text = stringResource(R.string.sleep_filter_all),
            selected = selected == null,
            onClick = { onFilterKindChange(null) },
        )
        SleepKind.entries.forEach { kind ->
            AppFilterChip(
                text = stringResource(kind.labelRes()),
                selected = selected == kind,
                onClick = { onFilterKindChange(if (selected == kind) null else kind) },
            )
        }
    }
}

/**
 * 一条睡眠记录卡。
 *
 * 层级：类别标签（小、主色）→ 时长（`titleS` 等宽数值，右对齐）→ 起止时间与夜尿（`caption`）→ 备注 → 计时器。
 * 编辑和删除压到时间行右端：它们是低频动作，不该占掉第一行的高度和注意力；
 * 整张卡仍可点击进编辑（§4.6 可点区域要有可视提示——右端箭头与两枚图标一起说明「这一行能进去」）。
 */
@Composable
internal fun SleepCard(
    record: SleepRecord,
    timer: TimerInstance?,
    onWakeUp: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = onEdit,
    onDelete: (() -> Unit)? = null,
    onDeleteTimer: (() -> Unit)? = null,
    highlighted: Boolean,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(
                width = if (highlighted) tokens.radii.stroke else tokens.radii.highlight,
                color = if (highlighted) tokens.colors.primary else tokens.colors.border,
                shape = shape,
            )
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindBadge(record.kind, highlighted)
            Spacer(Modifier.weight(1f))
            Text(
                text = sleepDurationText(record.durationMinutes() ?: 0L),
                style = tokens.type.titleS,
                color = tokens.colors.text,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(tokens.spacing.s1))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildList {
                    add(sleepRangeText(record))
                    if (record.kind == SleepKind.NIGHT_SLEEP && record.nocturia.isNotEmpty()) {
                        add(stringResource(R.string.sleep_nocturia_count_value, record.nocturia.size))
                    }
                }.joinToString(" · "),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            onEdit?.let { edit ->
                IconButton(modifier = Modifier.size(tokens.spacing.hitMin), onClick = edit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.sleep_edit),
                        modifier = Modifier.size(tokens.spacing.s4),
                        tint = tokens.colors.textMuted,
                    )
                }
            }
            onDelete?.let { delete ->
                IconButton(modifier = Modifier.size(tokens.spacing.hitMin), onClick = delete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.sleep_delete),
                        modifier = Modifier.size(tokens.spacing.s4),
                        tint = tokens.colors.danger,
                    )
                }
            }
        }
        if (record.note.isNotBlank()) {
            Text(
                text = record.note,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        timer?.let { instance ->
            // 倒计时行补了三处「它是什么」：一枚时钟图标、「剩余 15:00」的措辞、每秒自减的数字。
            // 旧版只有一个裸的 `15:00`，压在「12:47 → 20:47」下面读起来像第三个时间点，
            // 而且只有重新进页面才变（UI 状态只在保存/删除时刷新）。
            // 这里按 (remainingSeconds, startedAt) 这对一致的值本地重算，不必等 ViewModel 刷新。
            val now by produceState(System.currentTimeMillis(), instance.id) {
                while (true) {
                    delay(1_000L)
                    value = System.currentTimeMillis()
                }
            }
            val remaining = instance.remainingNow(now)
            val finished = instance.isFinished() || remaining <= 0L
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.spacing.s6)
                    .padding(top = tokens.spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_time),
                    contentDescription = null,
                    modifier = Modifier.size(tokens.spacing.s4),
                    tint = if (finished) tokens.colors.success else tokens.colors.primary,
                )
                Spacer(Modifier.width(tokens.spacing.s1))
                Text(
                    text = if (finished) {
                        stringResource(R.string.sleep_timer_finished)
                    } else {
                        stringResource(R.string.sleep_timer_remaining, sleepTimerText(remaining))
                    },
                    style = if (finished) tokens.type.bodyS else tokens.type.numeric,
                    color = if (finished) tokens.colors.success else tokens.colors.primary,
                    fontWeight = if (finished) FontWeight.SemiBold else null,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (finished && onWakeUp != null) {
                    TextButton(onClick = onWakeUp) {
                        Text(stringResource(R.string.sleep_record_wake_up))
                    }
                }
                if (onDeleteTimer != null) {
                    IconButton(modifier = Modifier.size(tokens.spacing.hitMin), onClick = onDeleteTimer) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancel),
                            contentDescription = stringResource(R.string.sleep_timer_delete),
                            modifier = Modifier.size(tokens.spacing.s4),
                            tint = tokens.colors.textMuted,
                        )
                    }
                }
            }
        }
    }
}

/** 类别标签：小胶囊，主色底 12% + 主色字。比纯色文字更容易在一列卡片里扫。 */
@Composable
private fun KindBadge(kind: SleepKind, highlighted: Boolean) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    val color = if (highlighted) tokens.colors.primary else tokens.colors.textMuted
    Text(
        text = stringResource(kind.labelRes()),
        style = tokens.type.caption,
        color = color,
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = tokens.spacing.s2, vertical = 2.dp),
    )
}

/**
 * 时长文案：中文「7 小时 32 分」、英文「7h 32m」，随语言变。
 *
 * 旧版直接拼 `"${hours}h ${rest}m"`，中文界面里读起来像开发笔记；数字本身不变，
 * 单位交给资源文件（§2.4 文案随语言），图表的短轴标另用紧凑写法。
 */
@Composable
internal fun sleepDurationText(minutes: Long): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        // 整点不写「8 小时 0 分」：多出来的「0 分」是没信息量的字数，还会把卡片撑宽。
        hours > 0 && rest == 0L -> stringResource(R.string.sleep_duration_h, hours)
        hours > 0 -> stringResource(R.string.sleep_duration_hm, hours, rest)
        else -> stringResource(R.string.sleep_duration_m, rest)
    }
}

private fun sleepRangeText(record: SleepRecord): String {
    val start = formatRecordClockMinute(record.sleepStartAt)
    val end = record.wakeUpAt?.let { formatRecordClockMinute(it) } ?: return start
    return "$start → $end"
}

/** 倒计时文案 `m:ss`（最长档位 60 分钟，不会出现三位数分钟）。 */
private fun sleepTimerText(remainingSeconds: Long): String =
    "${remainingSeconds / 60}:%02d".format(remainingSeconds % 60)

private fun sleepRecordLabel(record: SleepRecord): String =
    formatRecordTimestamp(record.sleepStartAt, RecordTimePrecision.MINUTE)

@StringRes
internal fun SleepKind.labelRes(): Int = when (this) {
    SleepKind.NIGHT_SLEEP -> R.string.sleep_kind_night
    SleepKind.NAP -> R.string.sleep_kind_nap
}

package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppChipSelectedAlpha
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
 * 睡眠首页：统计 / 记录两个页签。
 *
 * 这一版的信息节奏（对齐《通用 UI 设计规范》§1.3 与移动端设计系统技能「信息流骨架」）：
 *
 * 1. **打开就是清单**：删掉了原先压在入口按钮下的「最近一次」概览卡——它就是列表第一条
 *    记录换个说法再说一遍（同一条数据的第二份副本），还把真正的第一条记录顶出首屏；
 *    总量、日均这类「跨记录才有意义」的数在【统计】页按筛选范围算，不在这里重复。
 * 2. **两个入口就是两个类别**：夜间睡眠和小憩记的东西不一样（默认时长、夜尿、计时档位都不同），
 *    合并成一个「记一笔」再进去切类别，等于让用户多点一次。现在它们并排常驻在【记录】页顶部，
 *    点哪个进去就是哪个类别（编辑页仍可改，点错了不至于退出来重来）。
 *    按钮与卡片标签共用类别代表色（靛蓝 / 暖橙），颜色本身就是一次「这两个不是一回事」的说明。
 * 3. **历史按天分组**：记录按入睡日期归到「今天 / 昨天 / M 月 D 日」下，同一天多次小憩读起来是一组。
 * 4. **页签顺序与其它记录功能一致**：统计在左、记录在右；停在哪个页签会写进存档，
 *    下次进来还是那一页（从【记录】退出的用户不会一进来先看到统计）。
 */
@Composable
internal fun SleepHomeScreen(
    uiState: SleepUiState,
    /**
     * 当前页签 id（`statistics` / `records`）。
     *
     * 状态放在路由层：从设置页返回、或进程重建后都要回到原页签，`rememberSaveable` 只能覆盖后者。
     */
    selectedTabId: String,
    onSelectTab: (String) -> Unit,
    onAdd: (SleepKind) -> Unit,
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
            DetailTabItem("statistics", R.string.sleep_tab_statistics, R.drawable.ic_chart),
            DetailTabItem("records", R.string.sleep_tab_records, R.drawable.ic_list),
        )
    }
    val selectedTab = tabs.indexOfFirst { it.id == selectedTabId }.coerceAtLeast(0)
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
            AnimatedPageContent(
                selectedTab,
                Modifier.weight(1f),
                direction = { initial, target -> target - initial },
            ) { tab ->
                if (tabs[tab].id == "records") {
                    SleepRecordsTab(uiState, onAdd, onEdit, onDelete, onWakeUp, onDeleteTimer)
                } else {
                    SleepStatisticsTab(uiState.records)
                }
            }
            DetailTabBar(tabs, tabs[selectedTab].id) { item -> onSelectTab(item.id) }
        }
    }
}

/**
 * 记录页顶部的两个新增入口：一对等宽按钮，各自用类别代表色。
 *
 * 用「淡底 + 同色描边 + 同色字」而不是两块实心：一页里并排两个实心色块，谁也说不清哪个是主操作；
 * 淡底既让颜色可辨，又不至于把下面的记录卡压成背景。实心色只留给卡片上的类别标签。
 */
@Composable
private fun SleepAddButton(kind: SleepKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    val accent = tokens.sleep.forKind(kind == SleepKind.NIGHT_SLEEP)
    Row(
        modifier = modifier
            .heightIn(min = tokens.spacing.hitMin)
            .clip(shape)
            .background(accent.copy(alpha = AppChipSelectedAlpha))
            .border(tokens.radii.highlight, accent.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            // 夜间睡眠用月亮带 Zzz，小憩用一只钟——两者都不是通用「加号」，一眼能分出记哪一类。
            painter = painterResource(if (kind == SleepKind.NIGHT_SLEEP) R.drawable.ic_sleep else R.drawable.ic_time),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = accent,
        )
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(
            text = stringResource(
                if (kind == SleepKind.NIGHT_SLEEP) R.string.sleep_add_night else R.string.sleep_add_nap,
            ),
            style = tokens.type.bodyS,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 一条记录按入睡日期归组后的一整天。 */
private data class SleepDayGroup(val date: LocalDate, val records: List<SleepRecord>)

@Composable
private fun SleepRecordsTab(
    uiState: SleepUiState,
    onAdd: (SleepKind) -> Unit,
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
    val today = remember(zone) { LocalDate.now(zone) }

    Column(Modifier.fillMaxSize()) {
        // 两个入口常驻顶部而不是浮在右下角：它们回答的是「记哪一类」，
        // 滚到第 20 条记录时也还得点得到，不该跟着列表一起滚走。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s3,
                    bottom = tokens.spacing.s2,
                ),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        ) {
            SleepAddButton(
                kind = SleepKind.NIGHT_SLEEP,
                onClick = { onAdd(SleepKind.NIGHT_SLEEP) },
                modifier = Modifier.weight(1f),
            )
            SleepAddButton(
                kind = SleepKind.NAP,
                onClick = { onAdd(SleepKind.NAP) },
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.records.isEmpty()) {
            // 一条记录都没有时不摆筛选（没东西可筛），空态在剩余空间里居中：
            // 顶到页头下面会读成「加载失败」，居中才读成「这里还没有内容」。
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AppEmptyState(
                    title = stringResource(R.string.sleep_empty),
                    description = stringResource(R.string.sleep_empty_hint),
                    iconRes = R.drawable.ic_sleep,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = tokens.spacing.page),
            contentPadding = PaddingValues(
                top = tokens.spacing.s1,
                bottom = tokens.spacing.s6,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
        ) {
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
                        onOpen = { onEdit(record) },
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
                        onOpen = { onEdit(record) },
                        onDelete = { deleting = record },
                        onDeleteTimer = { record.timerId?.let(onDeleteTimer) },
                        highlighted = false,
                    )
                }
            }
        }
    }
    deleting?.let { record ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.sleep_delete_message, sleepRecordLabel(record))) },
            confirmButton = {
                // 与页内的动作按钮同款「图标 + 文字 + 描边」：浮层上只有一行字的 TextButton
                // 读不出「哪个能按」，也不跟这一条通往它的删除按钮长得一样。
                // 删除走 danger 描边，取消走普通描边——同一处不再出第二块红（§11.1 颜色不是唯一线索）。
                AppOutlinedIconTextButton(
                    text = stringResource(R.string.body_record_delete),
                    iconRes = R.drawable.ic_delete,
                    danger = true,
                    onClick = { onDelete(record.id); deleting = null },
                )
            },
            dismissButton = {
                AppOutlinedIconTextButton(
                    text = stringResource(R.string.compose_confirm_dialog_cancel),
                    iconRes = R.drawable.ic_cancel,
                    onClick = { deleting = null },
                )
            },
        )
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
 * 从上到下回答四个问题，每答一个换一种字级，视线顺着往下不用回头找：
 *
 * 1. **这是什么、多久**：类别文本卡 + 时长。时长染上类别代表色，一列卡片扫下来
 *    先看到颜色分组，再看数字（进行中则换成「进行中」+ 同色圆点，不留空的时长位）。
 * 2. **什么时候睡的**：入睡 / 醒来两列并排，各带一行小标签。旧版把两个时刻塞进一行
 *    `22:30 → 06:00`，与下面的夜尿、备注挤在同一级信息里，谁也说不清哪个是入睡。
 * 3. **还有什么细节**：夜尿次数（夜间且有记录时）、备注，都只在有内容时出现。
 * 4. **能做什么**：卡片右侧竖居中一枚描边【删除】（danger 色，不是纯文字）；编辑整张卡都能点进去。
 *    删除既不占单独一行、也不贴在右下角：前者要为它空出一整行高度，后者会在它与卡片内容之间
 *    留出一段只能放空气的宽度；竖居中时卡片高度完全由左侧内容决定。
 *
 * 计时器仍单独占一行：它是「正在跑的东西」，需要一个一眼能认出的位置。
 */
@Composable
internal fun SleepCard(
    record: SleepRecord,
    timer: TimerInstance?,
    onWakeUp: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDeleteTimer: (() -> Unit)? = null,
    highlighted: Boolean,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val shape = RoundedCornerShape(tokens.radii.container)
    val accent = tokens.sleep.forKind(record.kind == SleepKind.NIGHT_SLEEP)
    val minutes = record.durationMinutes()
    // 卡上除时间外的唯一附加信息——夜尿次数只在夜间睡眠且真有记录时才占位，
    // 小憩和「没起夜」的晚上不该为它空出一行。
    val nocturiaText = if (record.kind == SleepKind.NIGHT_SLEEP && record.nocturia.isNotEmpty()) {
        stringResource(R.string.sleep_nocturia_count_value, record.nocturia.size)
    } else {
        null
    }
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
            .padding(horizontal = spacing.s3, vertical = spacing.s3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SleepKindBadge(record.kind)
                    Spacer(Modifier.weight(1f))
                    if (minutes != null) {
                        Text(
                            text = sleepDurationText(minutes),
                            style = tokens.type.titleS,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(tokens.spacing.s1)
                                    .background(accent, CircleShape),
                            )
                            Spacer(Modifier.width(tokens.spacing.s1))
                            Text(
                                text = stringResource(R.string.sleep_ongoing),
                                style = tokens.type.bodyS,
                                color = accent,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(spacing.s2))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SleepCardTimeCell(
                        iconRes = R.drawable.ic_sleep,
                        label = stringResource(R.string.sleep_start_time),
                        value = formatRecordClockMinute(record.sleepStartAt),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(spacing.s2))
                    SleepCardTimeCell(
                        iconRes = R.drawable.ic_time,
                        label = stringResource(R.string.sleep_wake_time),
                        value = record.wakeUpAt?.let { formatRecordClockMinute(it) }
                            ?: stringResource(R.string.sleep_ongoing),
                        muted = record.wakeUpAt == null,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (nocturiaText != null) {
                    Spacer(Modifier.height(spacing.s1))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_description),
                            contentDescription = null,
                            modifier = Modifier.size(tokens.spacing.s4),
                            tint = tokens.colors.textMuted,
                        )
                        Spacer(Modifier.width(tokens.spacing.s1))
                        Text(
                            text = nocturiaText,
                            style = tokens.type.caption,
                            color = tokens.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (record.note.isNotBlank()) {
                    Spacer(Modifier.height(spacing.s2))
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
            onDelete?.let { delete ->
                // 删除放在卡片右侧竖居中：往右一挪就不再是左边那列内容的宽度竞争者，
                // 卡片高度完全由左侧内容决定（从上到下跑一遍也就多出 8 dp 的间距）。
                SleepCardDeleteButton(
                    text = stringResource(R.string.sleep_delete),
                    onClick = delete,
                )
            }
        }
    }
}

/**
 * 卡片右侧的【删除】：垃圾桶图标 + 两个字，外面包一圈 danger 描边，竖居中。
 *
 * 早先是没有边界的纯文字按钮，盯着一张灰白卡片读，它和备注、夜尿那些小字同级，
 * 既看不出「这里是能按的」，也说不清「按下去会丢东西」；描边把「按钮」这件事画出来，
 * danger 色把「不可逆」说出来（§5.1 次要按钮、§11.1 颜色不是唯一线索：图标与文案仍在）。
 *
 * 没有直接用 `AppDataTableDeleteAction`——它内部给文字挂了 `weight(1f)`，放进 Row 会被撑满
 * 剩余宽度：视觉上文字仍贴左，实际右半个卡片都成了可点的删除区（误触就弹「确认删除」）。
 */
@Composable
private fun SleepCardDeleteButton(text: String, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    AppOutlinedIconTextButton(
        text = text,
        iconRes = R.drawable.ic_delete,
        danger = true,
        onClick = onClick,
        // 描边按钮默认只有 40 dp 高，这里补到触控下限 48 dp：它是「点了就回不去」的那一个，
        // 热区不该比旁边的信息行还小（卡片高度由左侧内容决定，这 8 dp 加不出来）。
        modifier = Modifier.heightIn(min = tokens.spacing.hitMin),
    )
}

/**
 * 卡内的一列时刻：小标签在上、时间在下。
 *
 * 竖排而不是 `入睡 22:30` 横着写：两个时刻要能左右对齐着比（睡了多久几乎是这两列的减法），
 * 用 `numeric` 字级保 0–9 等宽，否则 1 与 8 的宽度差会让上下两行的时间抖。
 */
@Composable
private fun SleepCardTimeCell(
    @DrawableRes iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val tokens = LocalAppTokens.current
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(tokens.spacing.s4),
                tint = tokens.colors.textMuted,
            )
            Spacer(Modifier.width(tokens.spacing.s1))
            Text(
                text = label,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Spacer(Modifier.height(tokens.spacing.s1))
        Text(
            text = value,
            style = tokens.type.numeric,
            color = if (muted) tokens.colors.textMuted else tokens.colors.text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

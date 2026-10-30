package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
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

/**
 * 睡眠首页：记录 / 统计两个页签。
 *
 * 与旧版相比只动「怎么摆」，不动「怎么算」：
 * - 右上角纯齿轮图标改成 `AppPillIconTextButton`（图标 + 文字），入口不再靠猜（§5.1 次要按钮）；
 * - 记录与卡片统一到卡片语言：`panelRaised` 底 + 2 dp 描边 + 4 dp 圆角，与详情页的区块卡同族；
 * - 空态从一行灰字改成 `AppEmptyState`（结论 + 下一步），并给了「记一笔」的出口；
 * - 筛选从 M3 `FilterChip` 换成 `AppFilterChip`，与全站胶囊同高 32 dp、同一个描边规则。
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
            AnimatedPageContent(
                selectedTab,
                Modifier.weight(1f),
                direction = { initial, target -> target - initial },
            ) { tab ->
                if (tab == 0) {
                    SleepRecordsTab(uiState, onAdd, onEdit, onDelete, onWakeUp, onDeleteTimer)
                } else {
                    SleepStatisticsTab(uiState.records)
                }
            }
            DetailTabBar(tabs, tabs[selectedTab].id) { item -> selectedTab = tabs.indexOf(item) }
        }
    }
}

@Composable
private fun SleepRecordsTab(
    uiState: SleepUiState,
    onAdd: () -> Unit,
    onEdit: (SleepRecord) -> Unit,
    onDelete: (String) -> Unit,
    onWakeUp: (String) -> Unit,
    onDeleteTimer: (String) -> Unit,
) {
    val tokens = LocalAppTokens.current
    var filterKind by remember { mutableStateOf<SleepKind?>(null) }
    var deleting by remember { mutableStateOf<SleepRecord?>(null) }
    val filtered = uiState.records.filter { filterKind == null || it.kind == filterKind }
    val ongoing = filtered.filter { it.wakeUpAt == null }
    val finished = filtered.filter { it.wakeUpAt != null }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.page),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
    ) {
        AppIconTextButton(
            stringResource(R.string.sleep_add),
            R.drawable.ic_add,
            onAdd,
            Modifier.fillMaxWidth(),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(
                top = tokens.spacing.s1,
                bottom = tokens.spacing.s4,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
        ) {
            item {
                SleepKindFilter(filterKind, onFilterKindChange = { filterKind = it })
            }
            if (filtered.isEmpty()) {
                item {
                    AppEmptyState(
                        title = stringResource(R.string.sleep_empty),
                        description = stringResource(R.string.sleep_empty_hint),
                        iconRes = R.drawable.ic_sleep,
                        action = {
                            AppIconTextButton(
                                stringResource(R.string.sleep_add),
                                R.drawable.ic_add,
                                onAdd,
                            )
                        },
                    )
                }
            }
            if (ongoing.isNotEmpty()) {
                item { SleepGroupLabel(stringResource(R.string.sleep_ongoing), highlighted = true) }
                items(ongoing, key = SleepRecord::id) { record ->
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
            if (finished.isNotEmpty()) {
                item { SleepGroupLabel(stringResource(R.string.sleep_history), highlighted = false) }
                items(finished, key = SleepRecord::id) { record ->
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
    }
    deleting?.let { record ->
        androidx.compose.material3.AlertDialog(
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

/** 分组小标题：进行中 / 历史。只做分隔，不占一张卡。 */
@Composable
private fun SleepGroupLabel(text: String, highlighted: Boolean) {
    val tokens = LocalAppTokens.current
    Text(
        text = text,
        style = tokens.type.bodyS,
        color = if (highlighted) tokens.colors.primary else tokens.colors.textMuted,
        modifier = Modifier.padding(
            start = tokens.spacing.s1,
            top = tokens.spacing.s2,
        ),
    )
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
 * 层级只用字号与颜色两级：类别 + 时长在同一行（类别主色、时长等宽数值），
 * 起止时间与夜尿次数降为 `caption`，备注最多两行。编辑与删除收敛到右端两个 48 dp 图标按钮，
 * 不再和正文挤在同一段文字流里。
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
            .padding(
                start = tokens.spacing.s3,
                end = tokens.spacing.s1,
                top = tokens.spacing.s2,
                bottom = tokens.spacing.s3,
            ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(record.kind.labelRes()),
                style = tokens.type.bodyS,
                color = tokens.colors.primary,
            )
            Text(
                text = sleepDurationText(record),
                style = tokens.type.numeric,
                color = tokens.colors.text,
                modifier = Modifier.weight(1f).padding(start = tokens.spacing.s2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            onEdit?.let { edit ->
                IconButton(onClick = edit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.sleep_edit),
                        tint = tokens.colors.textMuted,
                    )
                }
            }
            onDelete?.let { delete ->
                IconButton(onClick = delete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.sleep_delete),
                        tint = tokens.colors.danger,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(end = tokens.spacing.s2),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        ) {
            Text(
                text = sleepRangeText(record),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (record.kind == SleepKind.NIGHT_SLEEP && record.nocturia.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.sleep_nocturia_count_value, record.nocturia.size),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
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
        }
        timer?.let { instance ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.spacing.s6)
                    .padding(end = tokens.spacing.s2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sleepTimerText(instance),
                    style = tokens.type.numeric,
                    color = if (instance.isFinished()) tokens.colors.success else tokens.colors.textMuted,
                    modifier = Modifier.weight(1f),
                )
                if (instance.isFinished() && onWakeUp != null) {
                    TextButton(onClick = onWakeUp) {
                        Text(stringResource(R.string.sleep_record_wake_up))
                    }
                }
            }
        }
    }
}

private fun sleepDurationText(record: SleepRecord): String =
    record.durationMinutes()?.let { minutes ->
        val hours = minutes / 60
        val rest = minutes % 60
        if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
    } ?: ""

private fun sleepRangeText(record: SleepRecord): String {
    val start = formatRecordTimestamp(record.sleepStartAt, RecordTimePrecision.MINUTE)
    val end = record.wakeUpAt?.let { formatRecordTimestamp(it, RecordTimePrecision.MINUTE) } ?: return start
    return "$start → $end"
}

private fun sleepTimerText(instance: TimerInstance): String {
    val remaining = instance.remainingNow(System.currentTimeMillis())
    return "${remaining / 60}:%02d".format(remaining % 60)
}

private fun sleepRecordLabel(record: SleepRecord): String =
    formatRecordTimestamp(record.sleepStartAt, RecordTimePrecision.MINUTE)

internal fun SleepKind.labelRes(): Int = when (this) {
    SleepKind.NIGHT_SLEEP -> R.string.sleep_kind_night
    SleepKind.NAP -> R.string.sleep_kind_nap
}

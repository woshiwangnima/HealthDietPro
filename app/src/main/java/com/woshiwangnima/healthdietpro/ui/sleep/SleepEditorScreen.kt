package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.RecordTimePickerField
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSwitchRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.sleep.NocturiaRecord
import com.woshiwangnima.healthdietpro.model.sleep.SleepDerivedField
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepPrefs
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.defaultSleepTimes
import com.woshiwangnima.healthdietpro.model.sleep.deriveSleepCounterpart
import com.woshiwangnima.healthdietpro.model.sleep.sleepDerivedField
import com.woshiwangnima.healthdietpro.model.sleep.generateDefaultNocturia

private val NAP_TIMER_MINUTES = listOf(10, 20, 30, 45, 60)
private val NIGHT_TIMER_MINUTES = listOf(15, 30, 45, 60)

/**
 * 记一条睡眠。
 *
 * 五块语义各一张卡：类别切换（置顶分段控件）→ 入睡与醒来 → 夜尿（仅夜间睡眠）→ 备注 → 计时器（仅醒来时间在未来）。
 * 旧版把它们平铺成一个 12 dp 间距的纵向列表，标题、字段、开关混在同一层，一屏看不出哪里到哪算一块；
 * 现在每块自带卡标题，夜间睡眠额外的夜尿时段退到独立的无名分组卡里，不与主字段平级。
 */
@Composable
internal fun SleepEditorScreen(
    existing: SleepRecord?,
    prefs: SleepPrefs,
    /** 新增时预选的类别：从【记夜间睡眠】或【记小憩】进来就已经定好，进页面后仍可切换。 */
    initialKind: SleepKind = SleepKind.NIGHT_SLEEP,
    onBack: () -> Unit,
    onSave: (SleepRecord) -> Unit,
    onCreateTimer: (label: String, minutes: Int, notifyViaSystem: Boolean) -> com.woshiwangnima.healthdietpro.common.timer.TimerInstance,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val now = System.currentTimeMillis()
    // 表单初值（也是后面判脏的基线）：新增时按类别与偏好算好默认起止时间，编辑时就是存档原记录。
    // 六个字段统一从这里取初值，不再各写一段 `existing?.x ?: 默认值`——旧版这么写时
    // 「入睡/醒来」的默认值只能在组合过程中补一次赋值进去，于是刚进页面 `current`
    // 就已经不是 `existing`，下面的 hasChanges 在第一帧就恒为真。
    val initialForm = remember(existing?.id, initialKind, prefs) {
        existing ?: run {
            val (defaultStart, defaultWake) = defaultSleepTimes(prefs, initialKind, now)
            SleepRecord(
                id = "",
                kind = initialKind,
                sleepStartAt = normalizeRecordTimestamp(defaultStart, RecordTimePrecision.MINUTE),
                wakeUpAt = normalizeRecordTimestamp(defaultWake, RecordTimePrecision.MINUTE),
                recordedAt = normalizeRecordTimestamp(now, RecordTimePrecision.MINUTE),
            )
        }
    }
    var kind by rememberSaveable(existing?.id, initialKind) { mutableStateOf(initialForm.kind) }
    var sleepStartAt by rememberSaveable(existing?.id, initialKind) { mutableStateOf(initialForm.sleepStartAt) }
    var wakeUpAt by rememberSaveable(existing?.id, initialKind) { mutableStateOf(initialForm.wakeUpAt) }
    var recordedAt by rememberSaveable(existing?.id, initialKind) { mutableStateOf(initialForm.recordedAt) }
    var note by rememberSaveable(existing?.id, initialKind) { mutableStateOf(initialForm.note) }
    var nocturiaCount by rememberSaveable(existing?.id, initialKind) { mutableIntStateOf(initialForm.nocturia.size) }
    var nocturia by rememberSaveable(existing?.id, initialKind) { mutableStateOf(initialForm.nocturia) }
    var startTimerEnabled by rememberSaveable(existing?.id, initialKind) { mutableStateOf(initialForm.timerId != null) }
    var timerMinutes by rememberSaveable(existing?.id, initialKind) {
        // 默认值必须落在当前类别的档位里：旧版一律取小憩的第一档（10 分钟），
        // 在夜间睡眠下这个值不在 15/30/45/60 里，界面上写着 10、存下去却被夹成 15。
        mutableIntStateOf(if (kind == SleepKind.NIGHT_SLEEP) NIGHT_TIMER_MINUTES.first() else NAP_TIMER_MINUTES.first())
    }
    var notifyViaSystem by rememberSaveable(existing?.id, initialKind) { mutableStateOf(false) }
    var pickField by remember { mutableStateOf<SleepTimeField?>(null) }
    var showSystemTimerConfirm by remember { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(existing?.id, initialKind) { mutableStateOf(false) }

    fun applyKindDefaults(newKind: SleepKind) {
        if (existing == null) {
            val (defaultStart, defaultWake) = defaultSleepTimes(prefs, newKind, now)
            sleepStartAt = normalizeRecordTimestamp(defaultStart, RecordTimePrecision.MINUTE)
            wakeUpAt = normalizeRecordTimestamp(defaultWake, RecordTimePrecision.MINUTE)
        }
    }

    // 现场把表单读成一条记录。**回调里一律重新调它**，不要复用下面的 `current`：
    // 返回键的回调在更早一次组合里就被注册/捕获，闭包里读派生 val 拿到的是那一刻的快照
    //（真机上的表现：切到小憩后按系统返回，判定仍读到切之前的夜间睡眠，于是直接退走）。
    // 这里的 kind / sleepStartAt 等都是 remember 出来的 State，闭包捕获的是 State 本身，读到的永远是最新值。
    fun snapshot(): SleepRecord = SleepRecord(
        id = existing?.id.orEmpty(),
        kind = kind,
        sleepStartAt = sleepStartAt,
        wakeUpAt = wakeUpAt,
        recordedAt = recordedAt,
        note = note.trim(),
        timerId = existing?.timerId,
        nocturia = if (kind == SleepKind.NIGHT_SLEEP) nocturia else emptyList(),
    )

    val wakeUp = wakeUpAt
    val current = snapshot()
    // 判脏：拿当前表单跟「进页面那一下的表单」（initialForm）比，而不是跟 existing 比。
    // 新增时 existing 恒为 null，`current != existing` 恒真——于是刚点开【记夜间睡眠】、
    // 一个字没改，按返回也会被问一句「放弃未保存的修改」。默认起止时间是系统给的建议值，
    // 不算改动；只有真的动了字段或计时器开关，才算「有未保存的修改」。
    val timerVisible = sleepTimerVisible(wakeUp, now)
    val hasChanges = sleepFormHasChanges(current, initialForm, timerVisible, startTimerEnabled)
    val valid = sleepStartAt > 0L && recordedAt > 0L && (wakeUp == null || wakeUp >= sleepStartAt) &&
        (kind != SleepKind.NIGHT_SLEEP || nocturia.all { it.endAt >= it.startAt })
    // 新增页允许「原样保存」：预填的默认时间本身就是一条可以直接入账的记录，
    // 用户一路点进来什么都不改再点保存，不该被一处防呆挡住；
    // 编辑页才要求有改动，否则按钮永远亮着、点了又什么都没存因人而异地困惑。
    val saveEnabled = valid && (existing == null || hasChanges)
    val timerLabel = stringResource(if (kind == SleepKind.NAP) R.string.sleep_timer_nap_label else R.string.sleep_timer_night_label)

    fun regenerateNocturia() {
        val sleepEnd = wakeUpAt ?: recordedAt
        nocturia = generateDefaultNocturia(nocturiaCount, sleepStartAt, sleepEnd)
    }

    fun save() {
        val form = snapshot()
        val timerReady = sleepTimerVisible(form.wakeUpAt, System.currentTimeMillis())
        if (timerReady && startTimerEnabled && existing?.timerId == null) {
            val minutes = if (form.kind == SleepKind.NAP) timerMinutes.coerceIn(NAP_TIMER_MINUTES) else timerMinutes.coerceIn(NIGHT_TIMER_MINUTES)
            if (notifyViaSystem) { showSystemTimerConfirm = true; return }
            val timer = onCreateTimer(timerLabel, minutes, false)
            onSave(form.copy(id = existing?.id ?: newSleepId(), timerId = timer.id))
            return
        }
        onSave(form.copy(id = existing?.id ?: newSleepId()))
    }

    fun requestBack() {
        val form = snapshot()
        val dirty = sleepFormHasChanges(
            current = form,
            baseline = initialForm,
            timerVisible = sleepTimerVisible(form.wakeUpAt, System.currentTimeMillis()),
            timerEnabled = startTimerEnabled,
        )
        if (dirty) showDiscardDialog = true else onBack()
    }
    androidx.activity.compose.BackHandler(onBack = ::requestBack)

    BaseScreen(
        title = stringResource(if (existing == null) R.string.sleep_add else R.string.sleep_edit),
        onBack = ::requestBack,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
            ) {
                item {
                    EqualWidthSegmentedTabs(
                        tabs = SleepKind.entries.map { EqualWidthTab.text(stringResource(it.labelRes())) },
                        selectedIndex = SleepKind.entries.indexOf(kind),
                        onSelected = { index ->
                            val option = SleepKind.entries[index]
                            kind = option
                            // 换类别就换一套可选档位，否则会停在新类别里不存在的值上。
                            timerMinutes = if (option == SleepKind.NIGHT_SLEEP) {
                                NIGHT_TIMER_MINUTES.first()
                            } else {
                                NAP_TIMER_MINUTES.first()
                            }
                            applyKindDefaults(option)
                        },
                    )
                }
                item {
                    // 先把「这样填下来一共睡了多久」摆在最上面：这是这一页真正要产出的数。
                    // 旧版要读完两个时间字段自己在脑子里做减法。
                    SleepEditorSummary(
                        startAt = sleepStartAt,
                        wakeAt = wakeUpAt,
                        nocturiaCount = if (kind == SleepKind.NIGHT_SLEEP) nocturiaCount else 0,
                    )
                }
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_sleep,
                        title = stringResource(R.string.sleep_time_section),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
                            // 推算按钮挂在「默认设置说该由系统推出来的那一端」：
                            // 睡前记录 → 醒来时间由入睡时间 + 默认时长算出；起床记录 → 反过来。
                            // 另一端才是用户真的会动手填的，按钮不该抢它的位置；
                            // 锚点还没值时按钮不出现（算不出东西的按钮只会挡路）。
                            val derived = sleepDerivedField(prefs, kind)
                            RecordTimePickerField(
                                title = stringResource(R.string.sleep_start_time),
                                valueMillis = sleepStartAt,
                                precision = RecordTimePrecision.MINUTE,
                                onClick = { pickField = SleepTimeField.START },
                                assistIconRes = R.drawable.ic_refresh,
                                assistLabel = if (derived == SleepDerivedField.START && wakeUpAt != null) {
                                    stringResource(R.string.sleep_time_derive_start)
                                } else {
                                    null
                                },
                                onAssistClick = {
                                    wakeUpAt?.let { wake ->
                                        sleepStartAt = normalizeRecordTimestamp(
                                            deriveSleepCounterpart(prefs, kind, wake, deriveWake = false),
                                            RecordTimePrecision.MINUTE,
                                        )
                                    }
                                },
                            )
                            RecordTimePickerField(
                                title = stringResource(R.string.sleep_wake_time),
                                valueMillis = wakeUpAt,
                                precision = RecordTimePrecision.MINUTE,
                                onClick = { pickField = SleepTimeField.WAKE },
                                emptyText = stringResource(R.string.sleep_wake_time_empty),
                                assistIconRes = R.drawable.ic_refresh,
                                assistLabel = if (derived == SleepDerivedField.WAKE && sleepStartAt > 0L) {
                                    stringResource(R.string.sleep_time_derive_wake)
                                } else {
                                    null
                                },
                                onAssistClick = {
                                    if (sleepStartAt > 0L) {
                                        wakeUpAt = normalizeRecordTimestamp(
                                            deriveSleepCounterpart(prefs, kind, sleepStartAt, deriveWake = true),
                                            RecordTimePrecision.MINUTE,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
                if (kind == SleepKind.NIGHT_SLEEP) {
                    item {
                        AppSectionCard(
                            iconRes = R.drawable.ic_daily_record,
                            title = stringResource(R.string.sleep_nocturia_title),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.sleep_nocturia_count),
                                    style = tokens.type.body,
                                    color = tokens.colors.text,
                                    modifier = Modifier.weight(1f),
                                )
                                NocturiaStepperButton(
                                    iconRes = R.drawable.ic_minus,
                                    description = stringResource(R.string.sleep_nocturia_decrease),
                                    enabled = nocturiaCount > 0,
                                    onClick = { nocturiaCount = (nocturiaCount - 1).coerceAtLeast(0) },
                                )
                                Text(
                                    text = "$nocturiaCount",
                                    style = tokens.type.numeric,
                                    color = tokens.colors.text,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.width(48.dp),
                                )
                                NocturiaStepperButton(
                                    iconRes = R.drawable.ic_add,
                                    description = stringResource(R.string.sleep_nocturia_increase),
                                    enabled = true,
                                    onClick = { nocturiaCount += 1 },
                                )
                            }
                            TextOverflowText(
                                text = stringResource(R.string.sleep_nocturia_generate),
                                style = tokens.type.bodyS,
                                color = tokens.colors.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = tokens.spacing.s2)
                                    .clickable(onClick = ::regenerateNocturia),
                            )
                        }
                    }
                    nocturia.forEachIndexed { index, entry ->
                        item(key = "nocturia-$index") {
                            AppSectionCard(
                                iconRes = R.drawable.ic_time,
                                title = stringResource(R.string.sleep_nocturia_item, index + 1),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
                                    RecordTimePickerField(
                                        title = stringResource(R.string.sleep_nocturia_start_time),
                                        valueMillis = entry.startAt,
                                        precision = RecordTimePrecision.MINUTE,
                                        onClick = { pickField = SleepTimeField.NOCTURIA_START(index) },
                                    )
                                    RecordTimePickerField(
                                        title = stringResource(R.string.sleep_nocturia_end_time),
                                        valueMillis = entry.endAt,
                                        precision = RecordTimePrecision.MINUTE,
                                        onClick = { pickField = SleepTimeField.NOCTURIA_END(index) },
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_description,
                        title = stringResource(R.string.sleep_note),
                    ) {
                        EditorTextField(
                            label = stringResource(R.string.sleep_note),
                            value = note,
                            onValueChange = { note = it },
                            required = false,
                            supportingTextOverride = {
                                Text(
                                    text = stringResource(R.string.sleep_note_hint),
                                    style = tokens.type.caption,
                                    color = tokens.colors.textMuted,
                                )
                            },
                        )
                    }
                }
                if (timerVisible) {
                    item {
                        AppSectionCard(
                            iconRes = R.drawable.ic_time,
                            title = stringResource(R.string.sleep_timer_section),
                        ) {
                            AppSwitchRow(
                                title = stringResource(R.string.sleep_start_timer),
                                tooltip = stringResource(R.string.sleep_start_timer_hint),
                                checked = startTimerEnabled,
                                onCheckedChange = { startTimerEnabled = it },
                            )
                            if (startTimerEnabled) {
                                AppListRowDivider(leadingInset = false)
                                AppDropdownField(
                                    label = stringResource(R.string.sleep_timer_duration),
                                    value = stringResource(R.string.sleep_timer_duration_value, timerMinutes),
                                    options = (if (kind == SleepKind.NAP) NAP_TIMER_MINUTES else NIGHT_TIMER_MINUTES)
                                        .map { AppDropdownOption(it.toString(), stringResource(R.string.sleep_timer_duration_value, it)) },
                                    onSelect = { timerMinutes = it.id.toInt() },
                                )
                                AppSwitchRow(
                                    title = stringResource(R.string.sleep_timer_system),
                                    tooltip = stringResource(R.string.sleep_timer_system_hint),
                                    checked = notifyViaSystem,
                                    onCheckedChange = { notifyViaSystem = it },
                                )
                            }
                        }
                    }
                }
            }
            FormSaveBar(text = stringResource(R.string.sleep_save), enabled = saveEnabled, onSave = ::save)
        }
    }

    pickField?.let { field ->
        val initial = when (field) {
            SleepTimeField.START -> sleepStartAt
            SleepTimeField.WAKE -> wakeUpAt ?: sleepStartAt
            is SleepTimeField.NOCTURIA_START -> nocturia.getOrNull(field.index)?.startAt ?: sleepStartAt
            is SleepTimeField.NOCTURIA_END -> nocturia.getOrNull(field.index)?.endAt ?: sleepStartAt
        }
        ComposeDateTimePickerDialog(
            initialMillis = initial,
            onDismiss = { pickField = null },
            onDateTimePicked = { picked ->
                when (field) {
                    SleepTimeField.START -> sleepStartAt = picked
                    SleepTimeField.WAKE -> wakeUpAt = picked
                    is SleepTimeField.NOCTURIA_START -> nocturia = nocturia.toMutableList().also { it[field.index] = it[field.index].copy(startAt = picked) }
                    is SleepTimeField.NOCTURIA_END -> nocturia = nocturia.toMutableList().also { it[field.index] = it[field.index].copy(endAt = picked) }
                }
                pickField = null
            },
            RecordTimePrecision.MINUTE,
        )
    }
    if (showSystemTimerConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSystemTimerConfirm = false },
            title = { Text(stringResource(R.string.sleep_timer_system_confirm_title)) },
            text = { Text(stringResource(R.string.sleep_timer_system_confirm_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showSystemTimerConfirm = false
                    val minutes = timerMinutes.coerceIn(NAP_TIMER_MINUTES)
                    val timer = onCreateTimer(timerLabel, minutes, true)
                    onSave(current.copy(id = existing?.id ?: newSleepId(), timerId = timer.id))
                }) { Text(stringResource(R.string.compose_confirm_dialog_ok)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSystemTimerConfirm = false }) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
            },
        )
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onBack,
            onSave = ::save,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
        )
    }
}

private sealed interface SleepTimeField {
    data object START : SleepTimeField
    data object WAKE : SleepTimeField
    data class NOCTURIA_START(val index: Int) : SleepTimeField
    data class NOCTURIA_END(val index: Int) : SleepTimeField
}

private fun Int.coerceIn(valid: List<Int>): Int = if (this in valid) this else valid.first()

/**
 * 本次时长预览：大数字 + 起止时间 + 夜尿次数。
 *
 * 放在类别切换下面而不是塞进「入睡与醒来」卡里：它是**结果**，两个时间字段是**输入**，
 * 混在同一张卡里读者会以为时长也是个能点开的字段。
 */
@Composable
private fun SleepEditorSummary(
    startAt: Long,
    wakeAt: Long?,
    nocturiaCount: Int,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val shape = RoundedCornerShape(tokens.radii.container)
    val minutes = wakeAt?.let { (it - startAt).coerceAtLeast(0L) / 60_000L }
    val meta = buildList {
        add(compactSleepRange(startAt, wakeAt))
        if (nocturiaCount > 0) add(stringResource(R.string.sleep_nocturia_count_value, nocturiaCount))
    }.joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.stroke, tokens.colors.border, shape)
            .padding(horizontal = spacing.s3, vertical = spacing.s3),
    ) {
        Text(
            text = stringResource(R.string.sleep_editor_duration_preview),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = minutes?.let { sleepDurationText(it) } ?: stringResource(R.string.sleep_ongoing),
            style = tokens.type.titleL,
            color = if (minutes != null) tokens.colors.primary else tokens.colors.textMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
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
}

/** 夜尿计数按钮：48 dp 热区、禁用态用 `disabled` 色，避免「0 次还能再减」的错点。 */
@Composable
private fun NocturiaStepperButton(
    @DrawableRes iconRes: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(tokens.spacing.hitMin),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = if (enabled) tokens.colors.primary else tokens.colors.disabled,
        )
    }
}

/**
 * 起止时间的紧凑写法：同一天只写 `12:25 → 20:25`，跨天才带月日。
 *
 * 完整时间戳（`2026-10-31 12:25 → 2026-10-31 20:25`）在卡片里要折两行，而两个日期
 * 通常是同一个——跨零点的情况一月也遇不到几次，不该让常态为它让位。
 */
private fun compactSleepRange(startAt: Long, wakeAt: Long?): String {
    val zone = java.time.ZoneId.systemDefault()
    val start = java.time.Instant.ofEpochMilli(startAt).atZone(zone)
    val end = wakeAt?.let { java.time.Instant.ofEpochMilli(it).atZone(zone) }
    fun clock(z: java.time.ZonedDateTime) = "%02d:%02d".format(z.hour, z.minute)
    fun dayClock(z: java.time.ZonedDateTime) = "%02d-%02d %s".format(z.monthValue, z.dayOfMonth, clock(z))
    if (end == null) return clock(start)
    val sameDay = start.toLocalDate() == end.toLocalDate()
    return if (sameDay) "${clock(start)} → ${clock(end)}" else "${dayClock(start)} → ${dayClock(end)}"
}

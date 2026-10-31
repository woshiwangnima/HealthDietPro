package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadioRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepPrefs
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecordTiming

/**
 * 睡眠设置首页：只有一条「默认时长」。
 *
 * 单行不值得为它起一张带标题的卡（§2.4 卡片标题），走无名分组卡 [AppSectionGroup]；
 * 解释文案不常显在行下，改成长按该行弹出的 tooltip。
 *
 * 行下补一行「当前值」（夜间 8 小时 · 小憩 1 小时 30 分）：只有标题的一行在整屏空白里
 * 既看不出默认是多少，也看不出改了没有——设置页的行该自己交代当前状态。
 */
@Composable
internal fun SleepSettingsScreen(
    prefs: SleepPrefs,
    onBack: () -> Unit,
    onDefaultDuration: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.sleep_settings_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
        ) {
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.sleep_settings_default_duration),
                    subtitle = stringResource(
                        R.string.sleep_settings_default_duration_summary,
                        sleepDurationText(prefs.nightDefaultMinutes.toLong()),
                        sleepDurationText(prefs.napDefaultMinutes.toLong()),
                    ),
                    tooltip = stringResource(R.string.sleep_settings_default_duration_description),
                    iconRes = R.drawable.ic_sleep,
                    showChevron = true,
                    onClick = onDefaultDuration,
                )
            }
        }
    }
}

/**
 * 默认时长：夜间睡眠 / 小憩各一套「默认时长 + 记录时机」。
 *
 * 时长换成「时 / 分」两行步进器（旧版是一个数字输入框 + 一个 h/min 下拉）：
 * 默认时长就那几档（8 小时、1 小时 30 分），点两下比唤出键盘再输 `7.5` 快；
 * 拉下单位下拉还得先换算这 0.5 到底是 30 分还是 50 分——步进器把单位摆在行首，
 * 不再需要「单位」这个字段，也就没有输错量纲的机会。
 *
 * 大数字（本次时长）摆最上面：这一页要回答的是「默认睡多久」，先给答案再给旋钮。
 * 记录时机仍然是二选一，用单选行而不是分段控件——它不改下方内容，只存一个偏好。
 */
@Composable
internal fun SleepDefaultDurationScreen(
    prefs: SleepPrefs,
    onBack: () -> Unit,
    onSave: (SleepPrefs) -> Unit,
) {
    val tokens = LocalAppTokens.current
    var kind by rememberSaveable { mutableStateOf(SleepKind.NIGHT_SLEEP) }
    val isNight = kind == SleepKind.NIGHT_SLEEP
    val currentMinutes = if (isNight) prefs.nightDefaultMinutes else prefs.napDefaultMinutes
    var totalMinutes by rememberSaveable(kind) { mutableIntStateOf(currentMinutes) }
    var timing by rememberSaveable(kind) {
        mutableStateOf(if (isNight) prefs.nightTiming else prefs.napTiming)
    }
    val currentTiming = if (isNight) prefs.nightTiming else prefs.napTiming
    val valid = totalMinutes > 0
    val changed = totalMinutes != currentMinutes || timing != currentTiming
    val saveEnabled = valid && changed

    fun save() {
        // 单位偏好（`nightUnitId`）不再由这一页写：时长本身已按分钟存下，
        // 保留字段只为兼容旧数据，留着原值不动比改成 h 更安全。
        val updated = prefs.copy(
            nightDefaultMinutes = if (isNight) totalMinutes else prefs.nightDefaultMinutes,
            napDefaultMinutes = if (isNight) prefs.napDefaultMinutes else totalMinutes,
            nightTiming = if (isNight) timing else prefs.nightTiming,
            napTiming = if (isNight) timing else prefs.napTiming,
        )
        onSave(updated)
    }

    BaseScreen(title = stringResource(R.string.sleep_settings_default_duration), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                        tabs = listOf(
                            EqualWidthTab.text(stringResource(SleepKind.NIGHT_SLEEP.labelRes())),
                            EqualWidthTab.text(stringResource(SleepKind.NAP.labelRes())),
                        ),
                        selectedIndex = if (isNight) 0 else 1,
                        onSelected = { index ->
                            val option = if (index == 0) SleepKind.NIGHT_SLEEP else SleepKind.NAP
                            if (option == kind) return@EqualWidthSegmentedTabs
                            kind = option
                            totalMinutes = if (option == SleepKind.NIGHT_SLEEP) {
                                prefs.nightDefaultMinutes
                            } else {
                                prefs.napDefaultMinutes
                            }
                            timing = if (option == SleepKind.NIGHT_SLEEP) prefs.nightTiming else prefs.napTiming
                        },
                    )
                }
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_sleep,
                        title = stringResource(
                            if (isNight) {
                                R.string.sleep_settings_default_duration_night
                            } else {
                                R.string.sleep_settings_default_duration_nap
                            },
                        ),
                    ) {
                        Column {
                            Text(
                                // 减到 0 就退成灰色「0 分钟」并禁用保存：0 是非法值，
                                // 但此时仍需一个数字占住大字号位置，否则整张卡会跳一下高度。
                                text = sleepDurationText(totalMinutes.toLong()),
                                style = tokens.type.titleL,
                                color = if (valid) tokens.colors.primary else tokens.colors.textMuted,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Spacer(Modifier.height(tokens.spacing.s2))
                            StepRow(
                                labelRes = R.string.sleep_settings_hours,
                                value = totalMinutes / 60,
                                range = 0..23,
                                step = 1,
                                onValueChange = { hours ->
                                    totalMinutes = hours * 60 + totalMinutes % 60
                                },
                            )
                            StepRow(
                                labelRes = R.string.sleep_settings_minutes,
                                value = totalMinutes % 60,
                                range = 0..55,
                                step = 5,
                                onValueChange = { minutes ->
                                    totalMinutes = totalMinutes / 60 * 60 + minutes
                                },
                            )
                            Spacer(Modifier.height(tokens.spacing.s1))
                            Text(
                                text = stringResource(R.string.sleep_settings_default_duration_hint),
                                style = tokens.type.caption,
                                color = tokens.colors.textMuted,
                            )
                        }
                    }
                }
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_time,
                        title = stringResource(R.string.sleep_settings_record_timing),
                        tooltip = stringResource(R.string.sleep_settings_record_timing_description),
                    ) {
                        AppRadioRow(
                            title = stringResource(R.string.sleep_settings_timing_before_sleep),
                            selected = timing == SleepRecordTiming.BEFORE_SLEEP,
                            horizontalPadding = 0.dp,
                            onSelect = { timing = SleepRecordTiming.BEFORE_SLEEP },
                        )
                        AppRadioRow(
                            title = stringResource(R.string.sleep_settings_timing_after_wake),
                            selected = timing == SleepRecordTiming.AFTER_WAKE,
                            horizontalPadding = 0.dp,
                            onSelect = { timing = SleepRecordTiming.AFTER_WAKE },
                        )
                    }
                }
            }
            FormSaveBar(
                text = stringResource(R.string.sleep_settings_save),
                enabled = saveEnabled,
                onSave = ::save,
            )
        }
    }
}

/**
 * 一行「时」或「分」的步进器：标签占满左侧，右侧 − 数值 +。
 *
 * 到边界时禁用按钮而不是让值撞墙不动——`disabled` 色能提前说明「到头了」（§4.4 禁用态）。
 */
@Composable
private fun StepRow(
    @StringRes labelRes: Int,
    value: Int,
    range: IntRange,
    step: Int,
    onValueChange: (Int) -> Unit,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            style = tokens.type.body,
            color = tokens.colors.text,
            modifier = Modifier.weight(1f),
        )
        StepButton(
            iconRes = R.drawable.ic_minus,
            description = stringResource(R.string.sleep_nocturia_decrease),
            enabled = value - step >= range.first,
            onClick = { onValueChange((value - step).coerceAtLeast(range.first)) },
        )
        Text(
            text = "$value",
            style = tokens.type.numeric,
            color = tokens.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(48.dp),
        )
        StepButton(
            iconRes = R.drawable.ic_add,
            description = stringResource(R.string.sleep_nocturia_increase),
            enabled = value + step <= range.last,
            onClick = { onValueChange((value + step).coerceAtMost(range.last)) },
        )
    }
}

/** 步进按钮：48 dp 热区，主色图标；禁用时降到 `disabled` 色且不响应点击。 */
@Composable
private fun StepButton(
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

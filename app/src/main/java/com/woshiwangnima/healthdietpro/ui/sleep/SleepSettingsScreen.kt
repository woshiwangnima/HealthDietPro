package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.NumericInputRange
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadioRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepPrefs
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecordTiming
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter

/**
 * 睡眠设置首页：只有一条「默认时长」。
 *
 * 单行不值得为它起一张带标题的卡（§2.4 卡片标题），走无名分组卡 [AppSectionGroup]；
 * 解释文案不常显在行下，改成长按该行弹出的 tooltip——设置项只留标题，列表因此保持一行一项的节奏。
 */
@Composable
internal fun SleepSettingsScreen(
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
 * 默认时长：夜间睡眠 / 小憩各一套「时长 + 单位 + 记录时机」。
 *
 * 三块语义分开装：类别是「切换」，用量一支分段控件置顶；数值与单位是「同一件事的两半」，
 * 收在一张卡里同一行；记录时机是「二选一」，用单选行而不是分段控件——它不改下方内容，只存一个偏好。
 * 「当前时间算入睡还是醒来」这句解释挂在卡标题上长按弹出，不再占卡内第一行。
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
    val unitId = if (isNight) prefs.nightUnitId else prefs.napUnitId
    var valueText by rememberSaveable(kind, currentMinutes, unitId) {
        mutableStateOf(formatDurationInput(currentMinutes, unitId))
    }
    var unit by rememberSaveable(kind, unitId) { mutableStateOf(unitId) }
    var timing by rememberSaveable(kind) {
        mutableStateOf(if (isNight) prefs.nightTiming else prefs.napTiming)
    }
    val value = valueText.toDoubleOrNull()
    val valid = value != null && value > 0.0
    val changed = value != null && unitToMinutes(value, unit) != currentMinutes ||
        unit != unitId ||
        timing != (if (isNight) prefs.nightTiming else prefs.napTiming)
    val saveEnabled = valid && changed

    fun save() {
        val minutes = requireNotNull(value).let { unitToMinutes(it, unit) }.coerceAtLeast(1)
        val updated = prefs.copy(
            nightDefaultMinutes = if (isNight) minutes else prefs.nightDefaultMinutes,
            napDefaultMinutes = if (isNight) prefs.napDefaultMinutes else minutes,
            nightUnitId = if (isNight) unit else prefs.nightUnitId,
            napUnitId = if (isNight) prefs.napUnitId else unit,
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
                            valueText = formatDurationInput(
                                if (option == SleepKind.NIGHT_SLEEP) prefs.nightDefaultMinutes else prefs.napDefaultMinutes,
                                if (option == SleepKind.NIGHT_SLEEP) prefs.nightUnitId else prefs.napUnitId,
                            )
                            unit = if (option == SleepKind.NIGHT_SLEEP) prefs.nightUnitId else prefs.napUnitId
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                            verticalAlignment = Alignment.Top,
                        ) {
                            EditorTextField(
                                label = stringResource(R.string.sleep_settings_default_duration_value),
                                value = valueText,
                                onValueChange = { valueText = it },
                                required = true,
                                numeric = true,
                                range = NumericInputRange(minimum = 0.001),
                                modifier = Modifier.weight(1f),
                            )
                            AppDropdownField(
                                label = stringResource(R.string.sleep_settings_duration_unit),
                                value = durationUnitLabel(unit),
                                options = durationUnitOptions(),
                                onSelect = { option ->
                                    val converted = value?.let { convertDurationValue(it, unit, option.id) }
                                    unit = option.id
                                    converted?.let { valueText = trimDurationValue(it) }
                                },
                                modifier = Modifier.weight(0.9f),
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

private fun durationUnitOptions(): List<AppDropdownOption> =
    UnitConverter.getRepository()?.getCategory(UnitCategoryType.Time.id)?.units
        ?.filter { it.id == "h" || it.id == "min" }
        ?.map { AppDropdownOption(it.id, durationUnitLabel(it.id)) }
        ?: listOf(AppDropdownOption("h", "h"), AppDropdownOption("min", "min"))

private fun durationUnitLabel(unitId: String): String = when (unitId) {
    "h" -> "h"
    "min" -> "min"
    else -> unitId
}

private fun unitToMinutes(value: Double, unitId: String): Int {
    val seconds = UnitConverter.toBase(UnitCategoryType.Time.id, value.toFloat(), unitId)
    return (seconds / 60f).toInt().coerceAtLeast(1)
}

private fun formatDurationInput(minutes: Int, unitId: String): String {
    val seconds = minutes * 60f
    val converted = UnitConverter.fromBase(UnitCategoryType.Time.id, seconds, unitId)
    return trimDurationValue(converted.toDouble())
}

private fun convertDurationValue(value: Double, fromUnit: String, toUnit: String): Double {
    val seconds = UnitConverter.toBase(UnitCategoryType.Time.id, value.toFloat(), fromUnit)
    return UnitConverter.fromBase(UnitCategoryType.Time.id, seconds, toUnit).toDouble()
}

private fun trimDurationValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

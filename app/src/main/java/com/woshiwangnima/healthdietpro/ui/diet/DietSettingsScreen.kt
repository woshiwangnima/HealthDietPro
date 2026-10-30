package com.woshiwangnima.healthdietpro.ui.diet

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadioRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.formatMinuteOfDay
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeClockPickerDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputRange
import com.woshiwangnima.healthdietpro.model.diet.DietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietPeriodPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRecordTiming
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.ui.container.ContainerRecordActivity

@Composable
internal fun DietSettingsScreen(
    onBack: () -> Unit,
    onDefaultHabits: () -> Unit,
    onGoals: () -> Unit,
    onContainers: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.diet_settings_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s4),
        ) {
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.diet_settings_default_habits),
                    tooltip = stringResource(R.string.diet_settings_default_habits_description),
                    iconRes = R.drawable.ic_diet,
                    showChevron = true,
                    onClick = onDefaultHabits,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.diet_settings_goals),
                    tooltip = stringResource(R.string.diet_settings_goals_description),
                    iconRes = R.drawable.ic_health_metrics,
                    showChevron = true,
                    onClick = onGoals,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.diet_settings_containers),
                    tooltip = stringResource(R.string.diet_settings_containers_description),
                    iconRes = R.drawable.ic_container,
                    showChevron = true,
                    onClick = onContainers,
                )
            }
        }
    }
}

@Composable
internal fun DietContainersScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    BaseScreen(title = stringResource(R.string.diet_settings_containers), onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            item {
                AppSectionGroup {
                    AppListRow(
                        title = stringResource(R.string.diet_containers_manage),
                        tooltip = stringResource(R.string.diet_containers_manage_description),
                        iconRes = R.drawable.ic_container,
                        showChevron = true,
                        onClick = {
                            context.startActivity(Intent(context, ContainerRecordActivity::class.java))
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun DietDefaultDurationScreen(
    prefs: DietPrefs,
    onBack: () -> Unit,
    onSave: (DietPrefs) -> Unit,
) {
    var period by rememberSaveable { mutableStateOf(MealPeriod.BREAKFAST) }
    var draftPrefs by remember { mutableStateOf(prefs) }
    val current = draftPrefs.forPeriod(period)
    var valueText by rememberSaveable(period, current.defaultMinutes, current.unitId) {
        mutableStateOf(formatDietDurationInput(current.defaultMinutes, current.unitId))
    }
    var unit by rememberSaveable(period, current.unitId) { mutableStateOf(current.unitId) }
    var timing by rememberSaveable(period) { mutableStateOf(current.timing) }
    var rangeStart by rememberSaveable(period, current.rangeStartMinute) {
        mutableStateOf(current.rangeStartMinute ?: period.defaultStartMinute)
    }
    var rangeEnd by rememberSaveable(period, current.rangeEndMinute) {
        mutableStateOf(current.rangeEndMinute ?: period.defaultEndMinute)
    }
    var showStartPicker by rememberSaveable(period) { mutableStateOf(false) }
    var showEndPicker by rememberSaveable(period) { mutableStateOf(false) }
    val value = valueText.toDoubleOrNull()
    val valid = value != null && value > 0.0
    val rangeCustom = rangeStart != period.defaultStartMinute || rangeEnd != period.defaultEndMinute
    val changed = value != null && dietUnitToMinutes(value, unit) != current.defaultMinutes ||
        unit != current.unitId ||
        timing != current.timing ||
        rangeStart != (current.rangeStartMinute ?: period.defaultStartMinute) ||
        rangeEnd != (current.rangeEndMinute ?: period.defaultEndMinute)
    val saveEnabled = valid && changed

    fun save() {
        val minutes = requireNotNull(value).let { dietUnitToMinutes(it, unit) }.coerceAtLeast(1)
        onSave(
            draftPrefs.withPeriod(
                period,
                current.copy(
                    defaultMinutes = minutes,
                    unitId = unit,
                    timing = timing,
                    rangeStartMinute = if (rangeCustom) rangeStart else null,
                    rangeEndMinute = if (rangeCustom) rangeEnd else null,
                ),
            ),
        )
    }

    fun applyPeriod(target: MealPeriod) {
        val targetPrefs = draftPrefs.forPeriod(target)
        period = target
        valueText = formatDietDurationInput(targetPrefs.defaultMinutes, targetPrefs.unitId)
        unit = targetPrefs.unitId
        timing = targetPrefs.timing
        rangeStart = targetPrefs.rangeStartMinute ?: target.defaultStartMinute
        rangeEnd = targetPrefs.rangeEndMinute ?: target.defaultEndMinute
    }

    fun applyDefaults(target: MealPeriod) {
        val defaults = DietPeriodPrefs()
        period = target
        valueText = formatDietDurationInput(defaults.defaultMinutes, defaults.unitId)
        unit = defaults.unitId
        timing = defaults.timing
        rangeStart = target.defaultStartMinute
        rangeEnd = target.defaultEndMinute
    }

    fun resetAll() {
        draftPrefs = DietPrefs()
        applyDefaults(period)
    }

    val tokens = LocalAppTokens.current
    BaseScreen(
        title = stringResource(R.string.diet_settings_default_habits),
        onBack = onBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = tokens.spacing.page, vertical = tokens.spacing.s3),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
            ) {
                // 复位是二级动作，统一成「图标 + 文字 + 描边」的同一款按钮。
                // 「重置全部」作用于整套饮食习惯（不分时段），固定放在时段选择条之上、通栏；
                // 「重置当前」只作用于当前时段，跟在时段相关的设置后面。
                item {
                    AppOutlinedIconTextButton(
                        text = stringResource(R.string.diet_settings_reset_all),
                        iconRes = R.drawable.ic_refresh,
                        onClick = ::resetAll,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    MealPeriodSelectorBar(
                        selected = period,
                        onPeriodSelected = { selected -> selected?.let(::applyPeriod) },
                        includeAllPeriod = false,
                    )
                }
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_time,
                        title = stringResource(R.string.diet_settings_default_duration),
                        tooltip = stringResource(R.string.diet_settings_default_duration_description),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                            verticalAlignment = androidx.compose.ui.Alignment.Top,
                        ) {
                            EditorTextField(
                                label = stringResource(R.string.diet_settings_default_duration_value),
                                value = valueText,
                                onValueChange = { valueText = it },
                                required = true,
                                numeric = true,
                                range = NumericInputRange(minimum = 0.001),
                                modifier = Modifier.weight(1f),
                            )
                            AppDropdownField(
                                label = stringResource(R.string.diet_settings_duration_unit),
                                value = dietDurationUnitLabel(unit),
                                options = dietDurationUnitOptions(),
                                onSelect = { option ->
                                    val converted = value?.let { convertDietDurationValue(it, unit, option.id) }
                                    unit = option.id
                                    converted?.let { valueText = trimDietDurationValue(it) }
                                },
                                modifier = Modifier.weight(0.9f),
                            )
                        }
                    }
                }
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_check,
                        title = stringResource(R.string.diet_settings_record_timing),
                        tooltip = stringResource(R.string.diet_settings_record_timing_description),
                    ) {
                        AppRadioRow(
                            title = stringResource(R.string.diet_settings_timing_before_meal),
                            selected = timing == DietRecordTiming.BEFORE_MEAL,
                            onSelect = { timing = DietRecordTiming.BEFORE_MEAL },
                        )
                        AppListRowDivider()
                        AppRadioRow(
                            title = stringResource(R.string.diet_settings_timing_after_meal),
                            selected = timing == DietRecordTiming.AFTER_MEAL,
                            onSelect = { timing = DietRecordTiming.AFTER_MEAL },
                        )
                    }
                }
                item {
                    AppSectionCard(
                        iconRes = R.drawable.ic_event,
                        title = stringResource(R.string.diet_settings_period_range),
                        tooltip = stringResource(R.string.diet_settings_meal_periods_description),
                        action = if (rangeCustom) {
                            {
                                Text(
                                    stringResource(R.string.diet_settings_period_range_custom),
                                    style = tokens.type.caption,
                                    color = tokens.colors.primary,
                                    modifier = Modifier.padding(end = tokens.spacing.s2),
                                )
                            }
                        } else {
                            null
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                            verticalAlignment = androidx.compose.ui.Alignment.Top,
                        ) {
                            DietTimeRangeField(
                                title = stringResource(R.string.diet_settings_period_range_start),
                                minuteOfDay = rangeStart,
                                onClick = { showStartPicker = true },
                                modifier = Modifier.weight(1f),
                            )
                            DietTimeRangeField(
                                title = stringResource(R.string.diet_settings_period_range_end),
                                minuteOfDay = rangeEnd,
                                onClick = { showEndPicker = true },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                // 复位是二级动作，统一成「图标 + 文字 + 描边」的同一款按钮。
                item {
                    AppOutlinedIconTextButton(
                        text = stringResource(R.string.diet_settings_reset_current),
                        iconRes = R.drawable.ic_refresh,
                        onClick = { applyDefaults(period) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            com.woshiwangnima.healthdietpro.common.ui.FormSaveBar(
                text = stringResource(R.string.diet_settings_save),
                enabled = saveEnabled,
                onSave = ::save,
            )
        }
    }
    if (showStartPicker) {
        ComposeClockPickerDialog(
            initialMinuteOfDay = rangeStart,
            onDismiss = { showStartPicker = false },
            onTimePicked = { rangeStart = it },
        )
    }
    if (showEndPicker) {
        ComposeClockPickerDialog(
            initialMinuteOfDay = rangeEnd,
            onDismiss = { showEndPicker = false },
            onTimePicked = { rangeEnd = it },
        )
    }
}

/** 时段边界的时间选择格：已经画在卡片里，底材取 `panel` + 细描边，与卡片区分而不与卡片同色。 */
@Composable
private fun DietTimeRangeField(
    title: String,
    minuteOfDay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = tokens.colors.panel,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.radii.container),
        border = androidx.compose.foundation.BorderStroke(tokens.radii.highlight, tokens.colors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2)) {
            Text(title, style = tokens.type.caption, color = tokens.colors.textMuted)
            Text(formatMinuteOfDay(minuteOfDay), style = tokens.type.body, color = tokens.colors.text)
        }
    }
}

@Composable
internal fun DietGoalsScreen(
    initialGoals: DietGoalsPrefs,
    recommendedGoals: DietGoalsPrefs,
    onBack: () -> Unit,
    onSave: (DietGoalsPrefs) -> Unit,
) {
    var energyText by rememberSaveable { mutableStateOf(initialGoals.energyKcal.toString()) }
    var carbsText by rememberSaveable { mutableStateOf(initialGoals.carbsGrams.toString()) }
    var proteinText by rememberSaveable { mutableStateOf(initialGoals.proteinGrams.toString()) }
    var fatText by rememberSaveable { mutableStateOf(initialGoals.fatGrams.toString()) }

    val energy = energyText.toIntOrNull()
    val carbs = carbsText.toIntOrNull()
    val protein = proteinText.toIntOrNull()
    val fat = fatText.toIntOrNull()
    val valid = energy != null && carbs != null && protein != null && fat != null &&
        energy >= 0 && carbs >= 0 && protein >= 0 && fat >= 0
    val changed = energy != initialGoals.energyKcal || carbs != initialGoals.carbsGrams ||
        protein != initialGoals.proteinGrams || fat != initialGoals.fatGrams
    val saveEnabled = valid && changed

    fun apply(goals: DietGoalsPrefs) {
        energyText = goals.energyKcal.toString()
        carbsText = goals.carbsGrams.toString()
        proteinText = goals.proteinGrams.toString()
        fatText = goals.fatGrams.toString()
    }

    BaseScreen(
        title = stringResource(R.string.diet_settings_goals),
        onBack = onBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 与「默认饮食习惯」的复位动作同一款按钮（图标 + 文字 + 描边），不再挂在顶栏当文字按钮。
                item {
                    AppOutlinedIconTextButton(
                        text = stringResource(R.string.diet_settings_restore_defaults),
                        iconRes = R.drawable.ic_refresh,
                        onClick = { apply(recommendedGoals) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        stringResource(R.string.diet_settings_goals_description),
                        style = LocalAppTokens.current.type.bodyS,
                        color = LocalAppTokens.current.colors.textMuted,
                    )
                }
                item {
                    EditorTextField(
                        label = stringResource(R.string.diet_summary_energy),
                        value = energyText,
                        onValueChange = { energyText = it.filter(Char::isDigit) },
                        required = true,
                        numeric = true,
                        suffix = { Text("kcal") },
                    )
                }
                item {
                    EditorTextField(
                        label = stringResource(R.string.diet_summary_carbs),
                        value = carbsText,
                        onValueChange = { carbsText = it.filter(Char::isDigit) },
                        required = true,
                        numeric = true,
                        suffix = { Text("g") },
                    )
                }
                item {
                    EditorTextField(
                        label = stringResource(R.string.diet_summary_protein),
                        value = proteinText,
                        onValueChange = { proteinText = it.filter(Char::isDigit) },
                        required = true,
                        numeric = true,
                        suffix = { Text("g") },
                    )
                }
                item {
                    EditorTextField(
                        label = stringResource(R.string.diet_summary_fat),
                        value = fatText,
                        onValueChange = { fatText = it.filter(Char::isDigit) },
                        required = true,
                        numeric = true,
                        suffix = { Text("g") },
                    )
                }
            }
            com.woshiwangnima.healthdietpro.common.ui.FormSaveBar(
                text = stringResource(R.string.diet_settings_save),
                enabled = saveEnabled,
                onSave = {
                    onSave(DietGoalsPrefs(requireNotNull(energy), requireNotNull(carbs), requireNotNull(protein), requireNotNull(fat)))
                },
            )
        }
    }
}

private fun dietDurationUnitOptions(): List<AppDropdownOption> =
    UnitConverter.getRepository()?.getCategory(UnitCategoryType.Time.id)?.units
        ?.filter { it.id == "h" || it.id == "min" }
        ?.map { AppDropdownOption(it.id, dietDurationUnitLabel(it.id)) }
        ?: listOf(AppDropdownOption("h", "h"), AppDropdownOption("min", "min"))

private fun dietDurationUnitLabel(unitId: String): String = when (unitId) {
    "h" -> "h"
    "min" -> "min"
    else -> unitId
}

private fun dietUnitToMinutes(value: Double, unitId: String): Int {
    val seconds = UnitConverter.toBase(UnitCategoryType.Time.id, value.toFloat(), unitId)
    return (seconds / 60f).toInt().coerceAtLeast(1)
}

private fun formatDietDurationInput(minutes: Int, unitId: String): String {
    val seconds = minutes * 60f
    val converted = UnitConverter.fromBase(UnitCategoryType.Time.id, seconds, unitId)
    return trimDietDurationValue(converted.toDouble())
}

private fun convertDietDurationValue(value: Double, fromUnit: String, toUnit: String): Double {
    val seconds = UnitConverter.toBase(UnitCategoryType.Time.id, value.toFloat(), fromUnit)
    return UnitConverter.fromBase(UnitCategoryType.Time.id, seconds, toUnit).toDouble()
}

private fun trimDietDurationValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

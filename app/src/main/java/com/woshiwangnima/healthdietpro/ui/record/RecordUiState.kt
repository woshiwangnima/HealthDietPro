package com.woshiwangnima.healthdietpro.ui.record

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R

data class RecordUiState(
    val sections: List<RecordSectionUiState> = emptyList(),
    /** 输入框里的原文，列表实时按它过滤——不再另有「已提交」的一份拷贝。 */
    val query: String = "",
    val searchHistory: List<String> = emptyList(),
    val recentActionIds: List<RecordActionId> = emptyList(),
)

data class RecordSectionUiState(
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val titleIconRes: Int,
    val items: List<RecordActionItemUiState>,
)

data class RecordActionItemUiState(
    val id: RecordActionId,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
    val enabled: Boolean,
    val searchAliasRes: List<Int> = emptyList(),
    val latestTimestamp: Long? = null,
    val latestValue: String? = null,
    val showSummary: Boolean = true,
)

enum class RecordActionId {
    Height,
    Weight,
    BloodGlucose,
    BloodPressure,
    Disease,
    Waist,
    Period,
    Diet,
    Water,
    Exercise,
    Sleep,
    Bowel,
    Medication,
    Habit,
    Feeling,
    Container,
    Price,
    BloodType,
    Allergy,
    BodyFat,
    Teeth,
    Vision,
    Hearing,
    HeartRate,
    Temperature,
}

/**
 * 用户在「用户设置 - 记录设置 - 功能启用」里关掉的功能，整条从记录中心移除。
 *
 * 关掉是为了精简界面，不是留一个灰块，所以不做「禁用但可见」；分节被清空后由记录页跳过渲染。
 */
internal fun List<RecordSectionUiState>.withoutDisabledActions(
    disabled: Set<RecordActionId>,
): List<RecordSectionUiState> = if (disabled.isEmpty()) {
    this
} else {
    map { section -> section.copy(items = section.items.filter { it.id !in disabled }) }
}

internal fun defaultRecordSections(): List<RecordSectionUiState> = listOf(
    RecordSectionUiState(
        titleRes = R.string.record_section_basic_profile,
        titleIconRes = R.drawable.ic_basic_profile,
        items = listOf(
            RecordActionItemUiState(RecordActionId.BloodType, R.string.record_action_blood_type, R.drawable.ic_blood_type, false),
            RecordActionItemUiState(RecordActionId.Disease, R.string.record_action_disease, R.drawable.ic_medical_history, true),
            RecordActionItemUiState(RecordActionId.Allergy, R.string.record_action_allergy, R.drawable.ic_allergy, false),
        ),
    ),
    RecordSectionUiState(
        titleRes = R.string.record_section_body_measurement,
        titleIconRes = R.drawable.ic_height,
        items = listOf(
            RecordActionItemUiState(RecordActionId.Height, R.string.record_action_height, R.drawable.ic_height, true),
            RecordActionItemUiState(RecordActionId.Weight, R.string.record_action_weight, R.drawable.ic_weight, true),
            RecordActionItemUiState(RecordActionId.BodyFat, R.string.record_action_body_fat, R.drawable.ic_body_fat, false),
            RecordActionItemUiState(RecordActionId.Waist, R.string.record_action_waist, R.drawable.ic_circumference, true, listOf(R.string.record_search_alias_circumference)),
            RecordActionItemUiState(RecordActionId.Teeth, R.string.record_action_teeth, R.drawable.ic_teeth, false),
            RecordActionItemUiState(RecordActionId.Vision, R.string.record_action_vision, R.drawable.ic_vision, false),
            RecordActionItemUiState(RecordActionId.Hearing, R.string.record_action_hearing, R.drawable.ic_hearing, false),
        ),
    ),
    RecordSectionUiState(
        titleRes = R.string.record_section_health_monitoring,
        titleIconRes = R.drawable.ic_health_metrics,
        items = listOf(
            RecordActionItemUiState(RecordActionId.BloodGlucose, R.string.record_action_blood_glucose, R.drawable.ic_blood_glucose, true),
            RecordActionItemUiState(RecordActionId.BloodPressure, R.string.record_action_blood_pressure, R.drawable.ic_blood_pressure, true),
            RecordActionItemUiState(RecordActionId.HeartRate, R.string.record_action_heart_rate, R.drawable.ic_heart_rate, false),
            RecordActionItemUiState(RecordActionId.Temperature, R.string.record_action_temperature, R.drawable.ic_temperature, false),
            RecordActionItemUiState(RecordActionId.Period, R.string.record_action_period, R.drawable.ic_birthday, false),
        ),
    ),
    RecordSectionUiState(
        titleRes = R.string.record_section_daily,
        titleIconRes = R.drawable.ic_daily_record,
        items = listOf(
            RecordActionItemUiState(RecordActionId.Diet, R.string.record_action_diet, R.drawable.ic_diet, true),
            RecordActionItemUiState(RecordActionId.Water, R.string.record_action_water, R.drawable.ic_volume, true),
            RecordActionItemUiState(RecordActionId.Sleep, R.string.record_action_sleep, R.drawable.ic_sleep, true),
            RecordActionItemUiState(RecordActionId.Exercise, R.string.record_action_exercise, R.drawable.ic_exercise, false),
            RecordActionItemUiState(RecordActionId.Bowel, R.string.record_action_bowel, R.drawable.ic_broom, false),
            RecordActionItemUiState(RecordActionId.Medication, R.string.record_action_medication, R.drawable.ic_medication, true),
        ),
    ),
    RecordSectionUiState(
        titleRes = R.string.record_section_misc,
        titleIconRes = R.drawable.ic_bell,
        items = listOf(
            RecordActionItemUiState(RecordActionId.Container, R.string.record_action_container, R.drawable.ic_container, true, listOf(R.string.record_search_alias_container), showSummary = false),
            // 物价属于「顺带记一笔」的杂项，不进日常三段；放在杂项首位，避免被两个禁用项挤到末尾。
            RecordActionItemUiState(
                RecordActionId.Price,
                R.string.record_action_price,
                R.drawable.ic_price,
                true,
                listOf(R.string.record_search_alias_price),
            ),
            RecordActionItemUiState(RecordActionId.Habit, R.string.record_action_habit, R.drawable.ic_time, false),
            RecordActionItemUiState(RecordActionId.Feeling, R.string.record_action_feeling, R.drawable.ic_placeholder, false),
        ),
    ),
)

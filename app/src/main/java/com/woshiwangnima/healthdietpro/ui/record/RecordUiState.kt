package com.woshiwangnima.healthdietpro.ui.record

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R

data class RecordUiState(
    val sections: List<RecordSectionUiState> = emptyList(),
    val query: String = "",
    val submittedQuery: String = "",
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
}

internal fun defaultRecordSections(): List<RecordSectionUiState> = listOf(
    RecordSectionUiState(
        titleRes = R.string.record_section_baseline,
        titleIconRes = R.drawable.ic_chart,
        items = listOf(
            RecordActionItemUiState(RecordActionId.Height, R.string.record_action_height, R.drawable.ic_height, true),
            RecordActionItemUiState(RecordActionId.Weight, R.string.record_action_weight, R.drawable.ic_weight, true),
            RecordActionItemUiState(RecordActionId.BloodGlucose, R.string.record_action_blood_glucose, R.drawable.ic_blood_glucose, true),
            RecordActionItemUiState(RecordActionId.BloodPressure, R.string.record_action_blood_pressure, R.drawable.ic_blood_pressure, true),
            RecordActionItemUiState(RecordActionId.Disease, R.string.record_action_disease, R.drawable.ic_medical_history, true),
            RecordActionItemUiState(RecordActionId.Waist, R.string.record_action_waist, R.drawable.ic_circumference, true, listOf(R.string.record_search_alias_circumference)),
            RecordActionItemUiState(RecordActionId.Period, R.string.record_action_period, R.drawable.ic_placeholder, false),
        ),
    ),
    RecordSectionUiState(
        titleRes = R.string.record_section_daily,
        titleIconRes = R.drawable.ic_list,
        items = listOf(
            RecordActionItemUiState(RecordActionId.Diet, R.string.record_action_diet, R.drawable.ic_diet, false),
            RecordActionItemUiState(RecordActionId.Water, R.string.record_action_water, R.drawable.ic_volume, true),
            RecordActionItemUiState(RecordActionId.Exercise, R.string.record_action_exercise, R.drawable.ic_exercise, false),
            RecordActionItemUiState(RecordActionId.Sleep, R.string.record_action_sleep, R.drawable.ic_sleep, false),
            RecordActionItemUiState(RecordActionId.Bowel, R.string.record_action_bowel, R.drawable.ic_placeholder, false),
            RecordActionItemUiState(RecordActionId.Medication, R.string.record_action_medication, R.drawable.ic_medication, true),
            RecordActionItemUiState(RecordActionId.Habit, R.string.record_action_habit, R.drawable.ic_placeholder, false),
        ),
    ),
    RecordSectionUiState(
        titleRes = R.string.record_section_status,
        titleIconRes = R.drawable.ic_bell,
        items = listOf(
            RecordActionItemUiState(RecordActionId.Feeling, R.string.record_action_feeling, R.drawable.ic_placeholder, false),
        ),
    ),
)

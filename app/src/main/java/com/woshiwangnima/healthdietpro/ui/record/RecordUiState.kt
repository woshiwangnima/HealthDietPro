package com.woshiwangnima.healthdietpro.ui.record

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R

data class RecordUiState(
    val sections: List<RecordSectionUiState> = emptyList(),
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
)

enum class RecordActionId {
    Height,
    Weight,
    BloodGlucose,
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
            RecordActionItemUiState(RecordActionId.Waist, R.string.record_action_waist, R.drawable.ic_placeholder, false),
            RecordActionItemUiState(RecordActionId.Period, R.string.record_action_period, R.drawable.ic_placeholder, false),
        ),
    ),
    RecordSectionUiState(
        titleRes = R.string.record_section_daily,
        titleIconRes = R.drawable.ic_list,
        items = listOf(
            RecordActionItemUiState(RecordActionId.Diet, R.string.record_action_diet, R.drawable.ic_placeholder, false),
            RecordActionItemUiState(RecordActionId.Water, R.string.record_action_water, R.drawable.ic_placeholder, false),
            RecordActionItemUiState(RecordActionId.Exercise, R.string.record_action_exercise, R.drawable.ic_placeholder, false),
            RecordActionItemUiState(RecordActionId.Sleep, R.string.record_action_sleep, R.drawable.ic_placeholder, false),
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

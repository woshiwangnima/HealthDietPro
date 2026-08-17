package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputRange
import com.woshiwangnima.healthdietpro.common.ui.SettingRadioRow
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepPrefs
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecordTiming
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter

@Composable
internal fun SleepSettingsScreen(
    onBack: () -> Unit,
    onDefaultDuration: () -> Unit,
) {
    BaseScreen(title = stringResource(R.string.sleep_settings_title), onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            item {
                SettingRow(
                    title = stringResource(R.string.sleep_settings_default_duration),
                    subtitle = stringResource(R.string.sleep_settings_default_duration_description),
                    leadingIconRes = R.drawable.ic_sleep,
                    onClick = onDefaultDuration,
                )
            }
        }
    }
}

@Composable
internal fun SleepDefaultDurationScreen(
    prefs: SleepPrefs,
    onBack: () -> Unit,
    onSave: (SleepPrefs) -> Unit,
) {
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
            napTiming = if (isNight) prefs.napTiming else timing,
        )
        onSave(updated)
    }

    BaseScreen(title = stringResource(R.string.sleep_settings_default_duration), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(stringResource(R.string.sleep_kind_title), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SleepKind.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = kind == option,
                                onClick = {
                                    kind = option
                                    valueText = formatDurationInput(
                                        if (option == SleepKind.NIGHT_SLEEP) prefs.nightDefaultMinutes else prefs.napDefaultMinutes,
                                        if (option == SleepKind.NIGHT_SLEEP) prefs.nightUnitId else prefs.napUnitId,
                                    )
                                    unit = if (option == SleepKind.NIGHT_SLEEP) prefs.nightUnitId else prefs.napUnitId
                                    timing = if (option == SleepKind.NIGHT_SLEEP) prefs.nightTiming else prefs.napTiming
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, SleepKind.entries.size),
                                label = { Text(stringResource(option.labelRes())) },
                            )
                        }
                    }
                }
                item {
                    Text(
                        stringResource(if (isNight) R.string.sleep_settings_default_duration_night else R.string.sleep_settings_default_duration_nap),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.Top,
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
                item {
                    Text(stringResource(R.string.sleep_settings_record_timing), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.sleep_settings_record_timing_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    SettingRadioRow(
                        title = stringResource(R.string.sleep_settings_timing_before_sleep),
                        subtitle = "",
                        selected = timing == SleepRecordTiming.BEFORE_SLEEP,
                        onClick = { timing = SleepRecordTiming.BEFORE_SLEEP },
                    )
                    SettingRadioRow(
                        title = stringResource(R.string.sleep_settings_timing_after_wake),
                        subtitle = "",
                        selected = timing == SleepRecordTiming.AFTER_WAKE,
                        onClick = { timing = SleepRecordTiming.AFTER_WAKE },
                    )
                }
            }
            com.woshiwangnima.healthdietpro.common.ui.FormSaveBar(
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
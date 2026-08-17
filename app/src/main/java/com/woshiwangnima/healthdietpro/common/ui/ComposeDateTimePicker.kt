package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeClockPickerDialog(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onTimePicked: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinuteOfDay / 60).coerceIn(0, 23),
        initialMinute = (initialMinuteOfDay % 60).coerceIn(0, 59),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diet_settings_period_range)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onTimePicked(state.hour * 60 + state.minute)
            }) {
                Text(stringResource(R.string.compose_confirm_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.compose_confirm_dialog_cancel))
            }
        },
    )
}

internal fun formatDateTime(millis: Long): String =
    formatRecordTimestamp(millis, RecordTimePrecision.MINUTE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
) {
    val initialDate = Instant.ofEpochMilli(initialMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { selectedMillis ->
                    onDatePicked(Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) {
                Text(stringResource(R.string.compose_confirm_dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.compose_confirm_dialog_cancel))
            }
        },
    ) {
        DatePicker(state = pickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeDateTimePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onDateTimePicked: (Long) -> Unit,
    precision: RecordTimePrecision = RecordTimePrecision.MINUTE,
) {
    val initialDateTime = remember(initialMillis) {
        Instant.ofEpochMilli(initialMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    var selectedDate by remember(initialDateTime) { mutableStateOf<LocalDate?>(null) }
    var seconds by remember(initialDateTime) { mutableStateOf(initialDateTime.second.toString()) }

    if (selectedDate == null) {
        ComposeDatePickerDialog(
            initialMillis = initialMillis,
            onDismiss = onDismiss,
            onDatePicked = { selectedDate = it },
        )
    } else {
        val timePickerState = rememberTimePickerState(
            initialHour = initialDateTime.hour,
            initialMinute = initialDateTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.compose_date_time_picker_select_time)) },
            text = {
                Column {
                    TimePicker(state = timePickerState)
                    if (precision == RecordTimePrecision.SECOND) {
                        OutlinedTextField(
                            value = seconds,
                            onValueChange = { seconds = it.filter(Char::isDigit).take(2) },
                            label = { Text(stringResource(R.string.blood_glucose_seconds)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val date = selectedDate ?: return@TextButton
                    val second = if (precision == RecordTimePrecision.SECOND) {
                        seconds.toIntOrNull()?.takeIf { it in 0..59 } ?: return@TextButton
                    } else {
                        0
                    }
                    val timestamp = LocalDateTime.of(
                        date,
                        java.time.LocalTime.of(timePickerState.hour, timePickerState.minute, second),
                    ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onDateTimePicked(normalizeRecordTimestamp(timestamp, precision))
                }) {
                    Text(stringResource(R.string.compose_confirm_dialog_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
            },
        )
    }
}

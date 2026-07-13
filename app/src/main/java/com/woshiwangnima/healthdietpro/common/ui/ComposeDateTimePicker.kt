package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun formatDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

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
) {
    val initialDateTime = remember(initialMillis) {
        Instant.ofEpochMilli(initialMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    var selectedDate by remember(initialDateTime) { mutableStateOf<LocalDate?>(null) }

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
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val date = selectedDate ?: return@TextButton
                    val timestamp = LocalDateTime.of(
                        date,
                        java.time.LocalTime.of(timePickerState.hour, timePickerState.minute),
                    ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onDateTimePicked(timestamp)
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

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
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    datesWithData: Set<LocalDate>? = null,
    allowNoDataSelection: Boolean = true,
) {
    val initialDate = Instant.ofEpochMilli(initialMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember(datesWithData, allowNoDataSelection) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (allowNoDataSelection || datesWithData == null) return true
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return date in datesWithData
                }
            }
        },
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
        if (datesWithData == null) {
            DatePicker(state = pickerState)
        } else {
            DataAwareDateGrid(
                initialDate = initialDate,
                datesWithData = datesWithData,
                allowNoDataSelection = allowNoDataSelection,
                onDateSelected = { pickerState.selectedDateMillis = it.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            )
        }
    }
}

@Composable
private fun DataAwareDateGrid(
    initialDate: LocalDate,
    datesWithData: Set<LocalDate>,
    allowNoDataSelection: Boolean,
    onDateSelected: (LocalDate) -> Unit,
) {
    var month by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var showYears by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    val leadingBlankDays = month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells = List(leadingBlankDays) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.compose_date_picker_select_date),
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Text(
            text = "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            TextButton(onClick = { showYears = true }, modifier = Modifier.weight(1f)) {
                Text(
                    text = "${month.year}年${month.monthValue}月",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
            }
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("\u4E00", "\u4E8C", "\u4E09", "\u56DB", "\u4E94", "\u516D", "\u65E5").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f).size(40.dp))
                        } else {
                            val hasData = date in datesWithData
                            val enabled = hasData || allowNoDataSelection
                            val selected = date == selectedDate
                            val isToday = date == today
                            val textColor = when {
                                selected -> colors.onPrimary
                                hasData -> colors.onSurface
                                else -> colors.onSurfaceVariant.copy(alpha = 0.48f)
                            }
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier.weight(1f).padding(2.dp).size(40.dp)
                                    .then(
                                        when {
                                            selected -> Modifier.background(colors.primary, CircleShape)
                                            isToday -> Modifier.border(1.dp, colors.primary, CircleShape)
                                            else -> Modifier
                                        },
                                    )
                                    .clickable(enabled = enabled) {
                                        selectedDate = date
                                        onDateSelected(date)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    textAlign = TextAlign.Center,
                                    color = textColor,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f).size(40.dp)) }
                }
            }
        }
    }
    if (showYears) {
        val yearRows = remember(today.year) { (today.year - 100..today.year + 100).toList().chunked(3) }
        val yearListState = rememberLazyListState()
        LaunchedEffect(showYears, month.year) {
            val index = yearRows.indexOfFirst { month.year in it }
            if (index >= 0) yearListState.scrollToItem(index)
        }
        AlertDialog(
            onDismissRequest = { showYears = false },
            title = { Text(stringResource(R.string.compose_date_picker_select_year)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth(), state = yearListState) {
                    items(yearRows) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { year ->
                                val isCurrent = year == today.year
                                val isSelected = year == month.year
                                androidx.compose.foundation.layout.Box(
                                    Modifier.weight(1f).padding(4.dp)
                                        .then(if (isSelected) Modifier.background(colors.primary, RoundedCornerShape(50)) else if (isCurrent) Modifier.border(1.dp, colors.primary, RoundedCornerShape(50)) else Modifier)
                                        .clickable { month = month.withYear(year); showYears = false },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(year.toString(), color = if (isSelected) colors.onPrimary else colors.onSurface, modifier = Modifier.padding(vertical = 10.dp))
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showYears = false }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } },
        )
    }
}

@Composable
internal fun ComposeDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onDatePicked: (LocalDate) -> Unit,
) {
    ComposeDatePickerDialog(
        initialMillis = initialMillis,
        onDismiss = onDismiss,
        onDatePicked = onDatePicked,
        datesWithData = null,
        allowNoDataSelection = true,
    )
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

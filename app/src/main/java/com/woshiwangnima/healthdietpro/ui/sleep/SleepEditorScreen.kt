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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.RecordTimePickerField
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord

private val NAP_TIMER_MINUTES = listOf(10, 20, 30, 45, 60)
private val NIGHT_TIMER_MINUTES = listOf(15, 30, 45, 60)

@Composable
internal fun SleepEditorScreen(
    existing: SleepRecord?,
    onBack: () -> Unit,
    onSave: (SleepRecord) -> Unit,
    onCreateTimer: (label: String, minutes: Int, notifyViaSystem: Boolean) -> com.woshiwangnima.healthdietpro.common.timer.TimerInstance,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    var kind by rememberSaveable(existing?.id) { mutableStateOf(existing?.kind ?: SleepKind.NIGHT_SLEEP) }
    var sleepStartAt by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.sleepStartAt ?: normalizeRecordTimestamp(now - 8 * 60 * 60_000L, RecordTimePrecision.MINUTE))
    }
    var wakeUpAt by rememberSaveable(existing?.id) { mutableStateOf(existing?.wakeUpAt) }
    var recordedAt by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.recordedAt ?: normalizeRecordTimestamp(now, RecordTimePrecision.MINUTE))
    }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var startTimerEnabled by rememberSaveable(existing?.id) { mutableStateOf(existing?.timerId != null) }
    var timerMinutes by rememberSaveable(existing?.id) {
        mutableStateOf(if (existing?.timerId != null) NAP_TIMER_MINUTES.first() else NAP_TIMER_MINUTES.first())
    }
    var notifyViaSystem by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var pickField by remember { mutableStateOf<SleepTimeField?>(null) }
    var showSystemTimerConfirm by remember { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(existing?.id) { mutableStateOf(false) }

    val current = SleepRecord(
        id = existing?.id.orEmpty(),
        kind = kind,
        sleepStartAt = sleepStartAt,
        wakeUpAt = wakeUpAt,
        recordedAt = recordedAt,
        note = note.trim(),
        timerId = existing?.timerId,
    )
    val timerConfigChanged = startTimerEnabled != (existing?.timerId != null)
    val hasChanges = current != existing || timerConfigChanged
    val wakeUp = wakeUpAt
    val valid = sleepStartAt > 0L && recordedAt > 0L && (wakeUp == null || wakeUp >= sleepStartAt)
    val saveEnabled = valid && hasChanges
    val timerLabel = stringResource(if (kind == SleepKind.NAP) R.string.sleep_timer_nap_label else R.string.sleep_timer_night_label)

    fun save() {
        if (startTimerEnabled && existing?.timerId == null) {
            val minutes = if (kind == SleepKind.NAP) timerMinutes.coerceIn(NAP_TIMER_MINUTES) else timerMinutes.coerceIn(NIGHT_TIMER_MINUTES)
            if (notifyViaSystem) { showSystemTimerConfirm = true; return }
            val timer = onCreateTimer(timerLabel, minutes, false)
            onSave(current.copy(id = existing?.id ?: newSleepId(), timerId = timer.id))
            return
        }
        onSave(current.copy(id = existing?.id ?: newSleepId()))
    }

    fun requestBack() {
        if (hasChanges) showDiscardDialog = true else onBack()
    }
    androidx.activity.compose.BackHandler(onBack = ::requestBack)

    BaseScreen(
        title = stringResource(if (existing == null) R.string.sleep_add else R.string.sleep_edit),
        onBack = ::requestBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(stringResource(R.string.sleep_kind_title), style = MaterialTheme.typography.titleSmall)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SleepKind.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = kind == option,
                                onClick = { kind = option; timerMinutes = NAP_TIMER_MINUTES.first() },
                                shape = SegmentedButtonDefaults.itemShape(index, SleepKind.entries.size),
                                label = { Text(stringResource(option.labelRes())) },
                            )
                        }
                    }
                }
                item { RecordTimePickerField(stringResource(R.string.sleep_start_time), sleepStartAt, RecordTimePrecision.MINUTE, { pickField = SleepTimeField.START }) }
                item { RecordTimePickerField(stringResource(R.string.sleep_wake_time), wakeUpAt, RecordTimePrecision.MINUTE, { pickField = SleepTimeField.WAKE }, emptyText = stringResource(R.string.sleep_wake_time_empty)) }
                item { EditorTextField(stringResource(R.string.sleep_note), note, { note = it }, required = false, supportingTextOverride = { Text(stringResource(R.string.sleep_note_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }) }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.sleep_start_timer), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.sleep_start_timer_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = startTimerEnabled, onCheckedChange = { startTimerEnabled = it })
                    }
                }
                if (startTimerEnabled) {
                    item {
                        val options = if (kind == SleepKind.NAP) NAP_TIMER_MINUTES else NIGHT_TIMER_MINUTES
                        AppDropdownField(
                            label = stringResource(R.string.sleep_timer_duration),
                            value = stringResource(R.string.sleep_timer_duration_value, timerMinutes),
                            options = options.map { AppDropdownOption(it.toString(), stringResource(R.string.sleep_timer_duration_value, it)) },
                            onSelect = { timerMinutes = it.id.toInt() },
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.sleep_timer_system), style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(R.string.sleep_timer_system_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = notifyViaSystem, onCheckedChange = { notifyViaSystem = it })
                        }
                    }
                }
            }
            FormSaveBar(text = stringResource(R.string.sleep_save), enabled = saveEnabled, onSave = ::save)
        }
    }

    pickField?.let { field ->
        val initial = when (field) {
            SleepTimeField.START -> sleepStartAt
            SleepTimeField.WAKE -> wakeUpAt ?: sleepStartAt
        }
        ComposeDateTimePickerDialog(
            initialMillis = initial,
            onDismiss = { pickField = null },
            onDateTimePicked = { picked ->
                when (field) {
                    SleepTimeField.START -> sleepStartAt = picked
                    SleepTimeField.WAKE -> wakeUpAt = picked
                }
                pickField = null
            },
            RecordTimePrecision.MINUTE,
        )
    }
    if (showSystemTimerConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSystemTimerConfirm = false },
            title = { Text(stringResource(R.string.sleep_timer_system_confirm_title)) },
            text = { Text(stringResource(R.string.sleep_timer_system_confirm_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showSystemTimerConfirm = false
                    val minutes = timerMinutes.coerceIn(NAP_TIMER_MINUTES)
                    val timer = onCreateTimer(timerLabel, minutes, true)
                    onSave(current.copy(id = existing?.id ?: newSleepId(), timerId = timer.id))
                }) { Text(stringResource(R.string.compose_confirm_dialog_ok)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSystemTimerConfirm = false }) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
            },
        )
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onBack,
            onSave = ::save,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
        )
    }
}

private enum class SleepTimeField { START, WAKE }

private fun Int.coerceIn(valid: List<Int>): Int = if (this in valid) this else valid.first()
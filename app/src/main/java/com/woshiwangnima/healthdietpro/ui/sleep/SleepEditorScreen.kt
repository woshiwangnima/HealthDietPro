package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.sleep.NocturiaRecord
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepPrefs
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.defaultSleepTimes
import com.woshiwangnima.healthdietpro.model.sleep.generateDefaultNocturia

private val NAP_TIMER_MINUTES = listOf(10, 20, 30, 45, 60)
private val NIGHT_TIMER_MINUTES = listOf(15, 30, 45, 60)

@Composable
internal fun SleepEditorScreen(
    existing: SleepRecord?,
    prefs: SleepPrefs,
    onBack: () -> Unit,
    onSave: (SleepRecord) -> Unit,
    onCreateTimer: (label: String, minutes: Int, notifyViaSystem: Boolean) -> com.woshiwangnima.healthdietpro.common.timer.TimerInstance,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    var kind by rememberSaveable(existing?.id) { mutableStateOf(existing?.kind ?: SleepKind.NIGHT_SLEEP) }
    var sleepStartAt by rememberSaveable(existing?.id) { mutableStateOf(existing?.sleepStartAt ?: 0L) }
    var wakeUpAt by rememberSaveable(existing?.id) { mutableStateOf(existing?.wakeUpAt) }
    var recordedAt by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.recordedAt ?: normalizeRecordTimestamp(now, RecordTimePrecision.MINUTE))
    }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var nocturiaCount by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.nocturia?.size ?: 0) }
    var nocturia by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.nocturia ?: emptyList())
    }
    var startTimerEnabled by rememberSaveable(existing?.id) { mutableStateOf(existing?.timerId != null) }
    var timerMinutes by rememberSaveable(existing?.id) {
        mutableStateOf(NAP_TIMER_MINUTES.first())
    }
    var notifyViaSystem by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var pickField by remember { mutableStateOf<SleepTimeField?>(null) }
    var showSystemTimerConfirm by remember { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(existing?.id) { mutableStateOf(false) }

    if (existing == null && sleepStartAt == 0L) {
        val (defaultStart, defaultWake) = defaultSleepTimes(prefs, kind, now)
        sleepStartAt = normalizeRecordTimestamp(defaultStart, RecordTimePrecision.MINUTE)
        wakeUpAt = normalizeRecordTimestamp(defaultWake, RecordTimePrecision.MINUTE)
    }

    fun applyKindDefaults(newKind: SleepKind) {
        if (existing == null) {
            val (defaultStart, defaultWake) = defaultSleepTimes(prefs, newKind, now)
            sleepStartAt = normalizeRecordTimestamp(defaultStart, RecordTimePrecision.MINUTE)
            wakeUpAt = normalizeRecordTimestamp(defaultWake, RecordTimePrecision.MINUTE)
        }
    }

    val wakeUp = wakeUpAt
    val timerVisible = wakeUp != null && wakeUp > now
    val current = SleepRecord(
        id = existing?.id.orEmpty(),
        kind = kind,
        sleepStartAt = sleepStartAt,
        wakeUpAt = wakeUpAt,
        recordedAt = recordedAt,
        note = note.trim(),
        timerId = existing?.timerId,
        nocturia = if (kind == SleepKind.NIGHT_SLEEP) nocturia else emptyList(),
    )
    val timerConfigChanged = timerVisible && startTimerEnabled != (existing?.timerId != null)
    val hasChanges = current != existing || timerConfigChanged
    val valid = sleepStartAt > 0L && recordedAt > 0L && (wakeUp == null || wakeUp >= sleepStartAt) &&
        (kind != SleepKind.NIGHT_SLEEP || nocturia.all { it.endAt >= it.startAt })
    val saveEnabled = valid && hasChanges
    val timerLabel = stringResource(if (kind == SleepKind.NAP) R.string.sleep_timer_nap_label else R.string.sleep_timer_night_label)

    fun regenerateNocturia() {
        val sleepEnd = wakeUpAt ?: recordedAt
        nocturia = generateDefaultNocturia(nocturiaCount, sleepStartAt, sleepEnd)
    }

    fun save() {
        if (timerVisible && startTimerEnabled && existing?.timerId == null) {
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
                                onClick = { kind = option; timerMinutes = NAP_TIMER_MINUTES.first(); applyKindDefaults(option) },
                                shape = SegmentedButtonDefaults.itemShape(index, SleepKind.entries.size),
                                label = { Text(stringResource(option.labelRes())) },
                            )
                        }
                    }
                }
                item { RecordTimePickerField(stringResource(R.string.sleep_start_time), sleepStartAt, RecordTimePrecision.MINUTE, { pickField = SleepTimeField.START }) }
                item { RecordTimePickerField(stringResource(R.string.sleep_wake_time), wakeUpAt, RecordTimePrecision.MINUTE, { pickField = SleepTimeField.WAKE }, emptyText = stringResource(R.string.sleep_wake_time_empty)) }
                if (kind == SleepKind.NIGHT_SLEEP) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.sleep_nocturia_title), style = MaterialTheme.typography.titleSmall)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { nocturiaCount = (nocturiaCount - 1).coerceAtLeast(0) }) {
                                    Icon(painterResource(R.drawable.ic_minus), stringResource(R.string.sleep_nocturia_decrease))
                                }
                                Text(stringResource(R.string.sleep_nocturia_count), modifier = Modifier.padding(horizontal = 8.dp))
                                Text("$nocturiaCount", style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { nocturiaCount += 1 }) {
                                    Icon(Icons.Filled.Add, stringResource(R.string.sleep_nocturia_increase))
                                }
                            }
                            OutlinedButton(
                                onClick = ::regenerateNocturia,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TextOverflowText(
                                    text = stringResource(R.string.sleep_nocturia_generate),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    nocturia.forEachIndexed { index, entry ->
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.sleep_nocturia_item, index + 1), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                RecordTimePickerField(stringResource(R.string.sleep_nocturia_start_time), entry.startAt, RecordTimePrecision.MINUTE, { pickField = SleepTimeField.NOCTURIA_START(index) })
                                RecordTimePickerField(stringResource(R.string.sleep_nocturia_end_time), entry.endAt, RecordTimePrecision.MINUTE, { pickField = SleepTimeField.NOCTURIA_END(index) })
                            }
                        }
                    }
                }
                item { EditorTextField(stringResource(R.string.sleep_note), note, { note = it }, required = false, supportingTextOverride = { Text(stringResource(R.string.sleep_note_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }) }
                if (timerVisible) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.sleep_timer_system), style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(R.string.sleep_timer_system_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = notifyViaSystem, onCheckedChange = { notifyViaSystem = it })
                            }
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
            is SleepTimeField.NOCTURIA_START -> nocturia.getOrNull(field.index)?.startAt ?: sleepStartAt
            is SleepTimeField.NOCTURIA_END -> nocturia.getOrNull(field.index)?.endAt ?: sleepStartAt
        }
        ComposeDateTimePickerDialog(
            initialMillis = initial,
            onDismiss = { pickField = null },
            onDateTimePicked = { picked ->
                when (field) {
                    SleepTimeField.START -> sleepStartAt = picked
                    SleepTimeField.WAKE -> wakeUpAt = picked
                    is SleepTimeField.NOCTURIA_START -> nocturia = nocturia.toMutableList().also { it[field.index] = it[field.index].copy(startAt = picked) }
                    is SleepTimeField.NOCTURIA_END -> nocturia = nocturia.toMutableList().also { it[field.index] = it[field.index].copy(endAt = picked) }
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

private sealed interface SleepTimeField {
    data object START : SleepTimeField
    data object WAKE : SleepTimeField
    data class NOCTURIA_START(val index: Int) : SleepTimeField
    data class NOCTURIA_END(val index: Int) : SleepTimeField
}

private fun Int.coerceIn(valid: List<Int>): Int = if (this in valid) this else valid.first()
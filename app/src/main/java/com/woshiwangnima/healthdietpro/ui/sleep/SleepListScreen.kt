package com.woshiwangnima.healthdietpro.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.timer.remainingNow
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.durationMinutes

@Composable
internal fun SleepListScreen(
    uiState: SleepUiState,
    onAdd: () -> Unit,
    onEdit: (SleepRecord) -> Unit,
    onDelete: (String) -> Unit,
    onWakeUp: (String) -> Unit,
    onDeleteTimer: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterKind by remember { mutableStateOf<SleepKind?>(null) }
    var deleting by remember { mutableStateOf<SleepRecord?>(null) }
    val filtered = uiState.records.filter { filterKind == null || it.kind == filterKind }
    val ongoing = filtered.filter { it.wakeUpAt == null }
    val finished = filtered.filter { it.wakeUpAt != null }
    BaseScreen(
        title = stringResource(R.string.sleep_title),
        onBack = onBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIconTextButton(stringResource(R.string.sleep_add), R.drawable.ic_add, onAdd, Modifier.fillMaxWidth())
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SleepKindFilter(filterKind, onFilterKindChange = { filterKind = it })
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.sleep_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }
                }
                if (ongoing.isNotEmpty()) {
                    item { Text(stringResource(R.string.sleep_ongoing), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
                    items(ongoing, key = SleepRecord::id) { record ->
                        SleepCard(
                            record = record,
                            timer = uiState.timerFor(record),
                            onWakeUp = { onWakeUp(record.id) },
                            onEdit = { onEdit(record) },
                            onDelete = { deleting = record },
                            onDeleteTimer = { record.timerId?.let(onDeleteTimer) },
                            highlighted = true,
                        )
                    }
                }
                if (finished.isNotEmpty()) {
                    item { Text(stringResource(R.string.sleep_history), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(finished, key = SleepRecord::id) { record ->
                        SleepCard(
                            record = record,
                            timer = uiState.timerFor(record),
                            onWakeUp = null,
                            onEdit = { onEdit(record) },
                            onDelete = { deleting = record },
                            onDeleteTimer = { record.timerId?.let(onDeleteTimer) },
                            highlighted = false,
                        )
                    }
                }
            }
        }
    }
    deleting?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.sleep_delete_message, sleepRecordLabel(record))) },
            confirmButton = {
                TextButton(onClick = { onDelete(record.id); deleting = null }) { Text(stringResource(R.string.body_record_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun SleepKindFilter(selected: SleepKind?, onFilterKindChange: (SleepKind?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.FilterChip(
            selected = selected == null,
            onClick = { onFilterKindChange(null) },
            label = { Text(stringResource(R.string.sleep_filter_all)) },
        )
        SleepKind.entries.forEach { kind ->
            androidx.compose.material3.FilterChip(
                selected = selected == kind,
                onClick = { onFilterKindChange(if (selected == kind) null else kind) },
                label = { Text(stringResource(kind.labelRes())) },
            )
        }
    }
}

@Composable
private fun SleepCard(
    record: SleepRecord,
    timer: com.woshiwangnima.healthdietpro.common.timer.TimerInstance?,
    onWakeUp: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeleteTimer: () -> Unit,
    highlighted: Boolean,
) {
    Surface(
        onClick = onEdit,
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(record.kind.labelRes()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = sleepDurationText(record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                androidx.compose.material3.IconButton(onClick = onEdit) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.sleep_edit),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                androidx.compose.material3.IconButton(onClick = onDelete) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.sleep_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = sleepRangeText(record),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            timer?.let { instance ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sleepTimerText(instance),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (instance.isFinished()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (instance.isFinished() && onWakeUp != null) {
                        TextButton(onClick = onWakeUp) { Text(stringResource(R.string.sleep_record_wake_up)) }
                    }
                }
            }
            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun sleepDurationText(record: SleepRecord): String =
    record.durationMinutes()?.let { minutes ->
        val hours = minutes / 60
        val rest = minutes % 60
        if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
    } ?: ""

private fun sleepRangeText(record: SleepRecord): String {
    val start = formatRecordTimestamp(record.sleepStartAt, RecordTimePrecision.MINUTE)
    val end = record.wakeUpAt?.let { formatRecordTimestamp(it, RecordTimePrecision.MINUTE) } ?: return start
    return "$start → $end"
}

private fun sleepTimerText(instance: com.woshiwangnima.healthdietpro.common.timer.TimerInstance): String {
    val remaining = instance.remainingNow(System.currentTimeMillis())
    return "${remaining / 60}:%02d".format(remaining % 60)
}

private fun sleepRecordLabel(record: SleepRecord): String =
    formatRecordTimestamp(record.sleepStartAt, RecordTimePrecision.MINUTE)

internal fun SleepKind.labelRes(): Int = when (this) {
    SleepKind.NIGHT_SLEEP -> R.string.sleep_kind_night
    SleepKind.NAP -> R.string.sleep_kind_nap
}
package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.formatRelativeTimeOffset
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.RecordTimePickerField
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietEditorDraftRepository
import com.woshiwangnima.healthdietpro.model.diet.DietPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.diet.defaultDietTimes
import com.woshiwangnima.healthdietpro.model.diet.DietRecordTiming
import com.woshiwangnima.healthdietpro.model.diet.resolveDefault
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun DietEditorScreen(
    existing: DietRecord?,
    prefs: DietPrefs,
    viewModel: DietViewModel,
    onBack: () -> Unit,
    onCreateCustomFood: (FoodKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val draftRepository = remember(context) { DietEditorDraftRepository.fromContext(context) }
    val restoredDraft = remember(existing?.id) { draftRepository.load(existing?.id) }
    val restored = restoredDraft ?: existing
    val now = remember { System.currentTimeMillis() }
    val defaultPeriod = remember(prefs, now) { MealPeriod.entries.first().resolveDefault(now, prefs, java.time.ZoneId.systemDefault()) }
    val (defaultStart, defaultEnd) = defaultDietTimes(prefs, defaultPeriod, now)
    var mealStartAt by rememberSaveable(existing?.id, restoredDraft?.id) { mutableStateOf(restored?.mealStartAt ?: defaultStart) }
    var mealEndAt by rememberSaveable(existing?.id, restoredDraft?.id) { mutableStateOf(restored?.mealEndAt ?: defaultEnd) }
    var mealPeriod by rememberSaveable(existing?.id) {
        mutableStateOf(restored?.mealPeriod ?: defaultPeriod)
    }
    var note by rememberSaveable(existing?.id, restoredDraft?.id) { mutableStateOf(restored?.note.orEmpty()) }
    val entriesSaver = remember {
        listSaver<List<DietFoodEntry>, String>(
            save = { entries -> entries.map { dietEntryJson.encodeToString(it) } },
            restore = { values -> values.mapNotNull { runCatching { dietEntryJson.decodeFromString<DietFoodEntry>(it) }.getOrNull() } },
        )
    }
    var entries by rememberSaveable(existing?.id, restoredDraft?.id, stateSaver = entriesSaver) {
        mutableStateOf(restored?.entries ?: emptyList())
    }
    var pickField by remember { mutableStateOf<DietTimeField?>(null) }
    var resetField by remember { mutableStateOf<DietTimeField?>(null) }
    var resetBothTimes by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<DietFoodEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<DietFoodEntry?>(null) }
    var showEntryEditor by remember { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var endAtDefault by rememberSaveable(existing?.id, restoredDraft?.id) { mutableStateOf(restoredDraft == null && existing == null) }

    val current = DietRecord(
        id = existing?.id.orEmpty(),
        mealStartAt = mealStartAt,
        mealEndAt = mealEndAt,
        mealPeriod = mealPeriod,
        entries = entries,
        note = note.trim(),
        recordedAt = restored?.recordedAt ?: now,
    )
    val hasChanges = current != existing
    val valid = mealStartAt > 0L && mealEndAt >= mealStartAt && entries.isNotEmpty()
    val saveEnabled = valid && hasChanges
    val defaultTiming = prefs.forPeriod(mealPeriod)
    val defaultDuration = defaultTiming.defaultMinutes.toLong() * 60_000L
    val startResetOffset = if (defaultTiming.timing == DietRecordTiming.BEFORE_MEAL) 0L else -defaultDuration
    val endResetOffset = if (defaultTiming.timing == DietRecordTiming.BEFORE_MEAL) defaultDuration else 0L
    val resetNowLabel = stringResource(R.string.record_time_reset_now)
    val resetDayUnit = stringResource(R.string.record_time_reset_day_unit)
    val resetHourUnit = stringResource(R.string.record_time_reset_hour_unit)
    val resetMinuteUnit = stringResource(R.string.record_time_reset_minute_unit)
    val resetSecondUnit = stringResource(R.string.record_time_reset_second_unit)
    val formatOffset: (Long) -> String = { offset ->
        formatRelativeTimeOffset(
            offset = offset.milliseconds,
            zeroLabel = resetNowLabel,
            dayUnit = resetDayUnit,
            hourUnit = resetHourUnit,
            minuteUnit = resetMinuteUnit,
            secondUnit = resetSecondUnit,
        )
    }

    androidx.compose.runtime.LaunchedEffect(current) {
        if (current != existing) draftRepository.save(existing?.id, current)
    }

    fun onSaveRecord() {
        viewModel.save(current.copy(id = existing?.id ?: viewModel.newId()))
        draftRepository.clear()
        onBack()
    }

    fun save() {
        onSaveRecord()
    }

    fun requestBack() {
        if (hasChanges) showDiscardDialog = true else { draftRepository.clear(); onBack() }
    }
    androidx.activity.compose.BackHandler(onBack = ::requestBack)

    BaseScreen(
        title = stringResource(if (existing == null) R.string.diet_add else R.string.diet_edit),
        onBack = ::requestBack,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    RecordTimePickerField(
                        title = stringResource(R.string.diet_meal_start),
                        valueMillis = mealStartAt,
                        precision = RecordTimePrecision.MINUTE,
                        onClick = { pickField = DietTimeField.START },
                        resetOffset = startResetOffset.milliseconds,
                        resetLabel = formatOffset(startResetOffset),
                        onResetClick = { resetField = DietTimeField.START },
                    )
                }
                item {
                    RecordTimePickerField(
                        title = stringResource(R.string.diet_meal_end),
                        valueMillis = mealEndAt,
                        precision = RecordTimePrecision.MINUTE,
                        onClick = { pickField = DietTimeField.END },
                        resetOffset = endResetOffset.milliseconds,
                        resetLabel = formatOffset(endResetOffset),
                        onResetClick = { resetField = DietTimeField.END },
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppDropdownField(
                            label = stringResource(R.string.diet_meal_period),
                            value = stringResource(mealPeriod.displayRes()),
                            options = MealPeriod.entries.map { period ->
                                com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption(period.name, stringResource(period.displayRes()))
                            },
                            onSelect = { option ->
                                val selected = MealPeriod.valueOf(option.id)
                                if (selected != mealPeriod && endAtDefault && mealStartAt > 0L) {
                                    val duration = prefs.forPeriod(selected).defaultMinutes.toLong() * 60_000L
                                    mealEndAt = mealStartAt + duration
                                }
                                mealPeriod = selected
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            onClick = { resetBothTimes = true },
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.diet_time_reset_both),
                                    modifier = Modifier.size(18.dp),
                                )
                                TextOverflowText(
                                    text = stringResource(R.string.diet_time_reset_both),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.diet_entries), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        AppIconTextButton(text = stringResource(R.string.diet_add_food), iconRes = R.drawable.ic_add, onClick = {
                            editingEntry = null
                            showEntryEditor = true
                        })
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (entries.isEmpty()) {
                            Text(
                                stringResource(R.string.diet_entries_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().height(EntryListHeight).nestedScroll(IsolatedNestedScroll),
                                verticalArrangement = Arrangement.spacedBy(EntryCardSpacing),
                            ) {
                                itemsIndexed(entries, key = { index, entry -> entry.foodName + entry.netWeightGrams + index }) { _, entry ->
                                    DietEntryCard(
                                        entry = entry,
                                        onEdit = { editingEntry = entry; showEntryEditor = true },
                                        onDelete = { deletingEntry = entry },
                                    )
                                }
                            }
                        }
                    }
                }
                item { EditorTextField(stringResource(R.string.diet_note), note, { note = it }, required = false, supportingTextOverride = { Text(stringResource(R.string.diet_note_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }) }
            }
            FormSaveBar(text = stringResource(R.string.diet_save), enabled = saveEnabled, onSave = ::save)
        }
    }

    pickField?.let { field ->
        ComposeDateTimePickerDialog(
            initialMillis = if (field == DietTimeField.START) mealStartAt else mealEndAt,
            onDismiss = { pickField = null },
            onDateTimePicked = { picked ->
                val minute = normalizeRecordTimestamp(picked, RecordTimePrecision.MINUTE)
                if (field == DietTimeField.START) {
                    val duration = prefs.forPeriod(mealPeriod).defaultMinutes.toLong() * 60_000L
                    val wasDefaultEnd = endAtDefault && mealEndAt == mealStartAt + duration
                    mealStartAt = minute
                    if (wasDefaultEnd) mealEndAt = minute + duration
                } else {
                    mealEndAt = minute
                    endAtDefault = false
                }
                pickField = null
            },
            RecordTimePrecision.MINUTE,
        )
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = { draftRepository.clear(); onBack() },
            onSave = ::onSaveRecord,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
        )
    }
    resetField?.let { field ->
        val offset = if (field == DietTimeField.START) startResetOffset else endResetOffset
        AlertDialog(
            onDismissRequest = { resetField = null },
            title = { Text(stringResource(R.string.diet_time_reset_title)) },
            text = { Text(stringResource(R.string.diet_time_reset_message, formatOffset(offset))) },
            confirmButton = {
                TextButton(onClick = {
                    val resetAt = normalizeRecordTimestamp(System.currentTimeMillis() + offset, RecordTimePrecision.MINUTE)
                    if (field == DietTimeField.START) {
                        val wasDefaultEnd = endAtDefault && mealEndAt == mealStartAt + defaultDuration
                        mealStartAt = resetAt
                        if (wasDefaultEnd) mealEndAt = resetAt + defaultDuration
                    } else {
                        mealEndAt = resetAt
                        endAtDefault = false
                    }
                    resetField = null
                }) { Text(stringResource(R.string.compose_confirm_dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { resetField = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
    if (resetBothTimes) {
        AlertDialog(
            onDismissRequest = { resetBothTimes = false },
            title = { Text(stringResource(R.string.diet_time_reset_title)) },
            text = { Text(stringResource(R.string.diet_time_reset_both_message, formatOffset(startResetOffset), formatOffset(endResetOffset))) },
            confirmButton = {
                TextButton(onClick = {
                    val resetNow = System.currentTimeMillis()
                    mealStartAt = normalizeRecordTimestamp(resetNow + startResetOffset, RecordTimePrecision.MINUTE)
                    mealEndAt = normalizeRecordTimestamp(resetNow + endResetOffset, RecordTimePrecision.MINUTE)
                    endAtDefault = true
                    resetBothTimes = false
                }) { Text(stringResource(R.string.compose_confirm_dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { resetBothTimes = false }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
    if (showEntryEditor) {
        DietFoodSheet(
            existing = editingEntry,
            viewModel = viewModel,
            onCreateCustomFood = onCreateCustomFood,
            onDismiss = { showEntryEditor = false },
            onConfirm = { entry ->
                entries = if (editingEntry != null) entries.map { if (it == editingEntry) entry else it } else entries + entry
                showEntryEditor = false
                editingEntry = null
            },
        )
    }
    deletingEntry?.let { target ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text(stringResource(R.string.diet_entry_delete_title)) },
            text = { Text(stringResource(R.string.diet_entry_delete_message, target.foodName)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    entries = entries.filterNot { it == target }
                    deletingEntry = null
                }) {
                    Text(stringResource(R.string.body_record_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deletingEntry = null }) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
            },
        )
    }
}

private val dietEntryJson = Json { ignoreUnknownKeys = true }

@Composable
private fun DietEntryCard(
    entry: DietFoodEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val (container, onContainer) = foodKindColors(entry.foodKind)
    val dimText = onContainer.copy(alpha = 0.78f)
    Surface(
        onClick = onEdit,
        color = container,
        contentColor = onContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(EntryCardHeight),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextOverflowText(
                text = entry.foodName,
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            NutrientCell(
                label = stringResource(R.string.diet_entry_weight),
                value = formatGrams(entry.netWeightGrams),
                fg = onContainer,
                dimText = dimText,
                labelStyle = MaterialTheme.typography.bodySmall,
                valueStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            NutrientMetric.entries.forEach { metric ->
                NutrientCell(
                    label = stringResource(metric.labelRes),
                    value = entryMetricValue(entry, metric),
                    fg = onContainer,
                    dimText = dimText,
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable(onClick = onDelete),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.diet_entry_delete),
                    tint = onContainer.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun NutrientCell(
    label: String,
    value: String,
    fg: Color,
    dimText: Color,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = MaterialTheme.typography.labelSmall,
    valueStyle: TextStyle = MaterialTheme.typography.bodySmall,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextOverflowText(
            text = label,
            style = labelStyle,
            color = dimText,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        TextOverflowText(
            text = value,
            style = valueStyle,
            color = fg,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun entryMetricValue(entry: DietFoodEntry, metric: NutrientMetric): String =
    if (metric == NutrientMetric.ENERGY) {
        "${formatCalories(entry.resolvedNutrients[metric.id]?.value ?: 0.0)} ${metric.unit}"
    } else {
        formatGrams(entry.resolvedNutrients[metric.id]?.value ?: 0.0)
    }

private val EntryCardHeight = 56.dp
private val EntryCardSpacing = 8.dp
private val EntryListHeight = EntryCardHeight * 4.5f + EntryCardSpacing * 4f

private val IsolatedNestedScroll = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
}

private sealed interface DietTimeField {
    data object START : DietTimeField
    data object END : DietTimeField
}

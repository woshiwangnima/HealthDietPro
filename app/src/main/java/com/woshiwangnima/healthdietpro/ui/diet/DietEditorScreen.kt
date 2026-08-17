package com.woshiwangnima.healthdietpro.ui.diet

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.RecordTimePickerField
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.diet.defaultDietTimes
import com.woshiwangnima.healthdietpro.model.diet.resolveDefault
import com.woshiwangnima.healthdietpro.model.food.FoodKind

@Composable
internal fun DietEditorScreen(
    existing: DietRecord?,
    prefs: DietPrefs,
    viewModel: DietViewModel,
    onBack: () -> Unit,
    onCreateCustomFood: (FoodKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val defaultPeriod = remember(prefs) { MealPeriod.entries.first().resolveDefault(now, prefs, java.time.ZoneId.systemDefault()) }
    val (defaultStart, defaultEnd) = defaultDietTimes(prefs, defaultPeriod, now)
    var mealStartAt by rememberSaveable(existing?.id) { mutableStateOf(existing?.mealStartAt ?: defaultStart) }
    var mealEndAt by rememberSaveable(existing?.id) { mutableStateOf(existing?.mealEndAt ?: defaultEnd) }
    var mealPeriod by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.mealPeriod ?: defaultPeriod)
    }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var entries by rememberSaveable(existing?.id) { mutableStateOf(existing?.entries ?: emptyList()) }
    var pickField by remember { mutableStateOf<DietTimeField?>(null) }
    var editingEntry by remember { mutableStateOf<DietFoodEntry?>(null) }
    var showEntryEditor by remember { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable(existing?.id) { mutableStateOf(false) }
    var endAtDefault by rememberSaveable(existing?.id) { mutableStateOf(existing == null) }

    val current = DietRecord(
        id = existing?.id.orEmpty(),
        mealStartAt = mealStartAt,
        mealEndAt = mealEndAt,
        mealPeriod = mealPeriod,
        entries = entries,
        note = note.trim(),
        recordedAt = existing?.recordedAt ?: now,
    )
    val hasChanges = current != existing
    val valid = mealStartAt > 0L && mealEndAt >= mealStartAt && entries.isNotEmpty()
    val saveEnabled = valid && hasChanges

    fun onSaveRecord() {
        viewModel.save(current.copy(id = existing?.id ?: viewModel.newId()))
        onBack()
    }

    fun save() {
        onSaveRecord()
    }

    fun requestBack() {
        if (hasChanges) showDiscardDialog = true else onBack()
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
                item { RecordTimePickerField(stringResource(R.string.diet_meal_start), mealStartAt, RecordTimePrecision.MINUTE, { pickField = DietTimeField.START }) }
                item { RecordTimePickerField(stringResource(R.string.diet_meal_end), mealEndAt, RecordTimePrecision.MINUTE, { pickField = DietTimeField.END }) }
                item {
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
                    )
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
                if (entries.isEmpty()) {
                    item {
                        Text(stringResource(R.string.diet_entries_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(entries, key = { it.foodName + it.netWeightGrams }) { entry ->
                        DietEntryCard(
                            entry = entry,
                            onEdit = { editingEntry = entry; showEntryEditor = true },
                            onDelete = { entries = entries.filterNot { it == entry } },
                        )
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
            onDiscard = onBack,
            onSave = ::onSaveRecord,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
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
}

@Composable
private fun DietEntryCard(
    entry: DietFoodEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        onClick = onEdit,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.foodName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatGrams(entry.netWeightGrams), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.IconButton(onClick = onDelete) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.diet_entry_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            val energy = entry.resolvedNutrients["ENERGY"]?.value
            if (energy != null) {
                Text(
                    text = stringResource(R.string.diet_entry_nutrition_summary, formatCalories(energy)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.diet_no_nutrition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private sealed interface DietTimeField {
    data object START : DietTimeField
    data object END : DietTimeField
}
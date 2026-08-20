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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
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
    var deletingEntry by remember { mutableStateOf<DietFoodEntry?>(null) }
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
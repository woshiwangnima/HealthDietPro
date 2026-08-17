package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.FoodCategoryFilterRows
import com.woshiwangnima.healthdietpro.common.ui.FoodSearchField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.NumericInputRange
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodRepository

private enum class DietFoodSource { EXISTING, FREE_NAME }

/** Add/edit one food entry within a meal: pick existing food or free name, weight, unit, container tare. */
@Composable
internal fun DietFoodSheet(
    existing: DietFoodEntry?,
    viewModel: DietViewModel,
    onCreateCustomFood: (FoodKind) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (DietFoodEntry) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    var source by rememberSaveable(existing?.foodName) { mutableStateOf(if (existing != null && existing.foodId == null) DietFoodSource.FREE_NAME else DietFoodSource.EXISTING) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var selectedKind by rememberSaveable { mutableStateOf(existing?.foodKind ?: FoodKind.INGREDIENT) }
    var customOnly by rememberSaveable { mutableStateOf(false) }
    var selectedRoot by remember { mutableStateOf<String?>(null) }
    var selectedChild by remember { mutableStateOf<String?>(null) }
    var selectedFoodId by rememberSaveable(existing?.foodId) { mutableStateOf(existing?.foodId) }
    var freeName by rememberSaveable(existing?.foodName) { mutableStateOf(existing?.foodName.orEmpty()) }
    var weightText by rememberSaveable(existing?.netWeightGrams) { mutableStateOf(if (existing != null) formatWeightInput(existing) else "") }
    var unitId by rememberSaveable(existing?.weightUnitId) { mutableStateOf(existing?.weightUnitId ?: state.defaultWeightUnitId) }
    var containerId by rememberSaveable(existing?.containerId) { mutableStateOf(existing?.containerId) }
    var showContainerPicker by remember { mutableStateOf(false) }

    val selectedFood = state.foods.firstOrNull { it.id == selectedFoodId }
    val previewFood = if (source == DietFoodSource.EXISTING) selectedFood else null
    val weightValue = weightText.toDoubleOrNull()
    val container = containerId?.let(viewModel::containerById)
    val netGrams = if (weightValue != null && weightValue > 0.0) viewModel.netGrams(weightValue, unitId, container) else 0.0
    val previewResolved = previewFood?.let(viewModel::resolvePer100g)
    val previewNutrients = if (previewResolved != null && netGrams > 0.0) viewModel.scaleToNetGrams(previewResolved, netGrams) else emptyMap()
    val freeNameValid = freeName.isNotBlank()
    val valid = weightValue != null && weightValue > 0.0 && netGrams > 0.0 && when (source) {
        DietFoodSource.EXISTING -> previewFood != null
        DietFoodSource.FREE_NAME -> freeNameValid
    }
    val existingEntryUnchanged = existing != null && source == DietFoodSource.EXISTING && weightValue == existing.weightValue &&
        unitId == existing.weightUnitId && containerId == existing.containerId && selectedFoodId == existing.foodId
    val hasChanges = existing == null || !existingEntryUnchanged ||
        (source == DietFoodSource.FREE_NAME && freeName.trim() != (existing?.foodName.orEmpty()))
    val saveEnabled = valid && hasChanges

    fun buildEntry(): DietFoodEntry {
        val name = if (source == DietFoodSource.EXISTING) previewFood?.displayName(language).orEmpty() else freeName.trim()
        return viewModel.buildEntry(previewFood, name, requireNotNull(weightValue), unitId, container)
    }

    LaunchedEffect(state.pendingCreatedFoodId) {
        val createdId = state.pendingCreatedFoodId
        if (createdId != null) {
            viewModel.consumeCreatedFoodId()
            val created = state.foods.firstOrNull { it.id == createdId }
            if (created != null) {
                source = DietFoodSource.EXISTING
                selectedKind = created.kind
                selectedFoodId = created.id
                keyword = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing != null) stringResource(R.string.diet_edit_food) else stringResource(R.string.diet_add_food),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
        },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(560.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        EqualWidthSegmentedTabs(
                            tabs = listOf(
                                EqualWidthTab.text(stringResource(R.string.diet_food_source_existing), R.drawable.ic_list),
                                EqualWidthTab.text(stringResource(R.string.diet_food_source_free_name), R.drawable.ic_edit),
                            ),
                            selectedIndex = if (source == DietFoodSource.EXISTING) 0 else 1,
                            onSelected = { source = if (it == 0) DietFoodSource.EXISTING else DietFoodSource.FREE_NAME },
                        )
                    }
                    item {
                        Text(stringResource(R.string.diet_entry_weight), style = MaterialTheme.typography.titleSmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            EditorTextField(
                                label = stringResource(R.string.diet_entry_weight_value),
                                value = weightText,
                                onValueChange = { weightText = it },
                                required = true,
                                numeric = true,
                                range = NumericInputRange(minimum = 0.001),
                                modifier = Modifier.weight(1f),
                            )
                            AppDropdownField(
                                label = stringResource(R.string.diet_entry_unit),
                                value = unitLabel(unitId),
                                options = state.weightUnitIds.map { AppDropdownOption(it, unitLabel(it)) },
                                onSelect = { unitId = it.id },
                                modifier = Modifier.weight(0.9f),
                            )
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { showContainerPicker = true }) {
                                Text(if (container != null) stringResource(R.string.diet_change_container) else stringResource(R.string.diet_select_container))
                            }
                            if (container != null) {
                                TextButton(onClick = { containerId = null }) { Text(stringResource(R.string.diet_remove_container)) }
                            }
                        }
                        if (container != null && weightValue != null && weightValue > 0.0) {
                            val grossGrams = viewModel.netGrams(weightValue, unitId, null)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.diet_tare_preview, formatGrams(grossGrams), formatGrams(container.emptyMassGrams ?: 0.0), formatGrams(netGrams)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(
                                    onClick = {
                                        weightText = formatWeightValue(viewModel.gramsToUnitValue(netGrams, unitId))
                                        containerId = null
                                    },
                                ) { Text(stringResource(R.string.diet_tare_apply_net)) }
                            }
                        }
                    }
                    if (source == DietFoodSource.EXISTING) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FoodKind.entries.forEach { kind ->
                                    FilterChip(
                                        selected = selectedKind == kind,
                                        onClick = { selectedKind = kind; selectedFoodId = null; selectedRoot = null; selectedChild = null },
                                        label = { Text(stringResource(kind.displayRes())) },
                                    )
                                }
                                FilterChip(
                                    selected = customOnly,
                                    onClick = { customOnly = !customOnly; selectedFoodId = null },
                                    label = { Text(stringResource(R.string.diet_food_source_custom)) },
                                )
                            }
                        }
                        item {
                            FoodSearchField(keyword, { keyword = it }, stringResource(R.string.diet_food_search_hint))
                        }
                        if (selectedKind != FoodKind.DISH) {
                            item {
                                FoodCategoryFilterRows(
                                    roots = viewModel.categoryRoots(),
                                    childrenFor = viewModel::categoryChildren,
                                    selectedRoot = selectedRoot,
                                    selectedChild = selectedChild,
                                    onRootSelected = { selectedRoot = it; selectedChild = null },
                                    onChildSelected = { selectedChild = it },
                                )
                            }
                        }
                        val matched = filteredFoods(state.foods, keyword, selectedKind, customOnly, selectedRoot, selectedChild, viewModel, language)
                        if (matched.isEmpty()) {
                            item { Text(stringResource(R.string.diet_food_no_match), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(matched, key = FoodItem::id) { food ->
                                val selected = food.id == selectedFoodId
                                Surface(
                                    onClick = { selectedFoodId = food.id },
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        FoodKindBadge(food.kind, language)
                                        Column(Modifier.weight(1f)) {
                                            Text(food.displayName(language), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (food.allNames(language).size > 1) {
                                                Text(
                                                    text = food.allNames(language).drop(1).joinToString(" / "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                        if (UserCustomFoodRepository.isCustom(food.id)) {
                                            androidx.compose.material3.Icon(
                                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_edit),
                                                contentDescription = stringResource(R.string.diet_food_source_custom),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            EditorTextField(
                                label = stringResource(R.string.diet_free_name),
                                value = freeName,
                                onValueChange = { freeName = it },
                                required = true,
                                supportingTextOverride = { Text(stringResource(R.string.diet_free_name_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.diet_create_custom), style = MaterialTheme.typography.titleSmall)
                                FoodKind.entries.forEach { kind ->
                                    TextButton(onClick = { onCreateCustomFood(kind) }) {
                                        Text(stringResource(R.string.diet_create_custom_kind, stringResource(kind.displayRes())))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(stringResource(R.string.diet_nutrition_preview), style = MaterialTheme.typography.titleSmall)
                        if (source == DietFoodSource.FREE_NAME) {
                            Text(stringResource(R.string.diet_no_nutrition), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (netGrams > 0.0) {
                            if (previewNutrients.isEmpty()) {
                                Text(stringResource(R.string.diet_no_nutrition), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.diet_preview_summary,
                                        formatCalories(previewNutrients["ENERGY"]?.value ?: 0.0),
                                        formatGrams(previewNutrients["PROTEIN"]?.value ?: 0.0),
                                        formatGrams(previewNutrients["FAT"]?.value ?: 0.0),
                                        formatGrams(previewNutrients["CHO"]?.value ?: 0.0),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                FormSaveBar(
                    text = stringResource(R.string.diet_entry_confirm),
                    enabled = saveEnabled,
                    onSave = { onConfirm(buildEntry()) },
                )
            }
        },
    )
    if (showContainerPicker) {
        DietContainerPickerSheet(
            containers = state.containers,
            selectedId = containerId,
            onSelect = { containerId = it },
            onDismiss = { showContainerPicker = false },
        )
    }
}

private fun formatWeightInput(entry: DietFoodEntry): String =
    if (entry.weightValue % 1.0 == 0.0) entry.weightValue.toInt().toString() else "%.1f".format(entry.weightValue)

private fun formatWeightValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Composable
private fun FoodKindBadge(kind: FoodKind, language: String) {
    val (container, onContainer) = when (kind) {
        FoodKind.INGREDIENT -> androidx.compose.ui.graphics.Color(0xFF43A047) to androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        FoodKind.FOOD -> androidx.compose.ui.graphics.Color(0xFFF57C00) to androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        FoodKind.DISH -> androidx.compose.ui.graphics.Color(0xFFE53935) to androidx.compose.ui.graphics.Color(0xFFFFFFFF)
    }
    Surface(
        color = container,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    ) {
        Text(
            text = stringResource(kind.displayRes()),
            color = onContainer,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun unitLabel(unitId: String): String = when (unitId) {
    "g" -> "g"
    "kg" -> "kg"
    "liang" -> "两"
    "jin" -> "斤"
    "oz" -> "oz"
    "lb" -> "lb"
    else -> unitId
}

private fun filteredFoods(
    foods: List<FoodItem>,
    keyword: String,
    kind: FoodKind,
    customOnly: Boolean,
    root: String?,
    child: String?,
    viewModel: DietViewModel,
    language: String,
): List<FoodItem> {
    return foods.filter { food ->
        if (food.kind != kind) return@filter false
        if (customOnly && !UserCustomFoodRepository.isCustom(food.id)) return@filter false
        val searchable = food.searchableNames().joinToString(" ").lowercase()
        if (keyword.isNotBlank() && !searchable.contains(keyword.lowercase())) return@filter false
        val tags = (food as? CategorizedFood)?.categoryTags.orEmpty()
        if (kind != FoodKind.DISH) {
            if (root != null && !viewModel.hasCategory(tags, root)) return@filter false
            if (child != null && !viewModel.hasCategory(tags, child)) return@filter false
        }
        true
    }.sortedWith(compareByDescending<FoodItem> { it.commonness }.thenBy { it.displayName(language) })
}
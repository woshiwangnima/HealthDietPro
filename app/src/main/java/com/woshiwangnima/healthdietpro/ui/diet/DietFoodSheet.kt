package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppModal
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.FoodCategoryFilterRows
import com.woshiwangnima.healthdietpro.common.ui.FoodSearchField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputRange
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodRepository
import com.woshiwangnima.healthdietpro.model.food.searchMatchRank

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
    var selectedKind by rememberSaveable { mutableStateOf(existing?.foodKind) }
    var customOnly by rememberSaveable { mutableStateOf(false) }
    var selectedRoot by remember { mutableStateOf<String?>(null) }
    var selectedChild by remember { mutableStateOf<String?>(null) }
    var selectedFoodId by rememberSaveable(existing?.foodId) { mutableStateOf(existing?.foodId) }
    var freeName by rememberSaveable(existing?.foodName) { mutableStateOf(existing?.foodName.orEmpty()) }
    var weightText by rememberSaveable(existing?.netWeightGrams) { mutableStateOf(if (existing != null) formatWeightInput(existing) else "") }
    var unitId by rememberSaveable(existing?.weightUnitId) { mutableStateOf(existing?.weightUnitId ?: state.defaultWeightUnitId) }
    var containerId by rememberSaveable(existing?.containerId) { mutableStateOf(existing?.containerId) }
    var showContainerPicker by remember { mutableStateOf(false) }
    var showNutritionPreview by rememberSaveable { mutableStateOf(false) }

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
    val previewEnabled = source == DietFoodSource.EXISTING && previewFood != null && netGrams > 0.0

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

    // 表单型浮层走 AppModal（§5.2）：改动没保存就点遮罩直接丢掉，用户会当成保存失败。
    AppModal(
        title = if (existing != null) stringResource(R.string.diet_edit_food) else stringResource(R.string.diet_add_food),
        onDismiss = onDismiss,
        dismissOnScrimClick = false,
        // 列表自己滚，不由 AppModal 套 verticalScroll：条目少时弹窗随内容缩短、真正居中，
        // 条目多时列表在弹窗内滚，底部操作条始终可见。
        contentScrollable = false,
        actions = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = previewEnabled,
                    onClick = { showNutritionPreview = true },
                ) {
                    Text(stringResource(R.string.diet_nutrition_preview))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        enabled = saveEnabled,
                        onClick = { onConfirm(buildEntry()) },
                    ) { Text(stringResource(R.string.diet_entry_confirm)) }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
                }
            }
        },
    ) {
            // 这里**不要**给 Column / LazyColumn 加 `weight`：`content` 的 receiver 是 AppModal 外层
            // Column 的 scope，但真正的父容器是内容区的 Box，权重会落到不被读取的 parentData 上；
            // 而加在 LazyColumn 上的 weight 会走真正的 Column，把它从「按内容收缩」变成「吃满剩余」，
            // 结果弹窗永远满高——条目少也不变矮，看上去就是「没有居中」。
            // 列表在内容区拿到的是有界约束，天然就是「条目少收缩、条目多封顶并可滚」。
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
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
                                    modifier = Modifier.weight(0.45f),
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                AppOutlinedIconTextButton(
                                    text = stringResource(if (container != null) R.string.diet_change_container else R.string.diet_select_container),
                                    iconRes = R.drawable.ic_container,
                                    onClick = { showContainerPicker = true },
                                )
                                if (container != null) {
                                    OutlinedButton(
                                        onClick = { containerId = null },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAppTokens.current.colors.danger),
                                    ) {
                                        Icon(painterResource(R.drawable.ic_cancel), contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.diet_remove_container))
                                    }
                                }
                            }
                            if (container != null) {
                                val hasWeight = weightValue != null && weightValue > 0.0
                                val emptyMass = container.emptyMassGrams
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.diet_tare_preview,
                                            if (hasWeight) formatGrams(viewModel.netGrams(requireNotNull(weightValue), unitId, null)) else "?",
                                            emptyMass?.let(::formatGrams) ?: "?",
                                            if (hasWeight) formatGrams(netGrams) else "?",
                                        ),
                                        style = LocalAppTokens.current.type.bodyS,
                                        color = LocalAppTokens.current.colors.textMuted,
                                        modifier = Modifier.weight(1f),
                                    )
                                    AppIconTextButton(
                                        text = stringResource(R.string.diet_tare_apply_net),
                                        iconRes = R.drawable.ic_check,
                                        enabled = hasWeight,
                                        onClick = {
                                            weightText = formatWeightValue(viewModel.gramsToUnitValue(netGrams, unitId))
                                            containerId = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (source == DietFoodSource.EXISTING) {
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FilterChip(
                                    selected = selectedKind == null,
                                    onClick = { selectedKind = null; selectedFoodId = null; selectedRoot = null; selectedChild = null },
                                    label = { TextOverflowText(stringResource(R.string.diet_food_source_all), style = LocalAppTokens.current.type.bodyS) },
                                    modifier = Modifier.weight(1f),
                                )
                                FoodKind.entries.forEach { kind ->
                                    FilterChip(
                                        selected = selectedKind == kind,
                                        onClick = { selectedKind = kind; selectedFoodId = null; selectedRoot = null; selectedChild = null },
                                        label = { TextOverflowText(stringResource(kind.displayRes()), style = LocalAppTokens.current.type.bodyS) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                FilterChip(
                                    selected = customOnly,
                                    onClick = { customOnly = !customOnly; selectedFoodId = null },
                                    label = { TextOverflowText(stringResource(R.string.diet_food_source_custom), style = LocalAppTokens.current.type.bodyS) },
                                    modifier = Modifier.weight(1f),
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
                            item { Text(stringResource(R.string.diet_food_no_match), color = LocalAppTokens.current.colors.textMuted) }
                        } else {
                            items(matched, key = FoodItem::id) { food ->
                                val selected = food.id == selectedFoodId
                                Surface(
                                    onClick = { selectedFoodId = if (selected) null else food.id },
                                    color = if (selected) LocalAppTokens.current.colors.primaryContainer.copy(alpha = 0.5f) else LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.3f),
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
                                            Text(
                                                text = food.displayName(language),
                                                style = LocalAppTokens.current.type.body,
                                                color = LocalAppTokens.current.colors.text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            if (food.allNames(language).size > 1) {
                                                Text(
                                                    text = food.allNames(language).drop(1).joinToString(" / "),
                                                    style = LocalAppTokens.current.type.bodyS,
                                                    color = LocalAppTokens.current.colors.textMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                        if (UserCustomFoodRepository.isCustom(food.id)) {
                                            androidx.compose.material3.Icon(
                                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_edit),
                                                contentDescription = stringResource(R.string.diet_food_source_custom),
                                                tint = LocalAppTokens.current.colors.textMuted,
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
                                supportingTextOverride = { Text(stringResource(R.string.diet_free_name_hint), color = LocalAppTokens.current.colors.textMuted) },
                            )
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.diet_create_custom), style = LocalAppTokens.current.type.titleS)
                                FoodKind.entries.forEach { kind ->
                                    TextButton(onClick = { onCreateCustomFood(kind) }) {
                                        Text(stringResource(R.string.diet_create_custom_kind, stringResource(kind.displayRes())))
                                    }
                                }
                            }
                        }
                    }
                }
                if (source == DietFoodSource.EXISTING) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.diet_selected_food),
                            style = LocalAppTokens.current.type.body,
                            color = if (previewFood != null) LocalAppTokens.current.colors.text else LocalAppTokens.current.colors.textMuted,
                        )
                        if (previewFood != null) {
                            Surface(
                                color = LocalAppTokens.current.colors.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FoodKindBadge(previewFood.kind, language)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = previewFood.displayName(language),
                                            style = LocalAppTokens.current.type.body,
                                            color = LocalAppTokens.current.colors.text,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (previewFood.allNames(language).size > 1) {
                                            Text(
                                                text = previewFood.allNames(language).drop(1).joinToString(" / "),
                                                style = LocalAppTokens.current.type.bodyS,
                                                color = LocalAppTokens.current.colors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { selectedFoodId = null }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_cancel),
                                    contentDescription = stringResource(R.string.diet_selected_food),
                                    tint = LocalAppTokens.current.colors.danger,
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.diet_selected_food_none),
                                style = LocalAppTokens.current.type.body,
                                color = LocalAppTokens.current.colors.textMuted.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
    }
    if (showContainerPicker) {
        DietContainerPickerSheet(
            containers = state.containers,
            selectedId = containerId,
            onSelect = { containerId = it },
            onDismiss = { showContainerPicker = false },
        )
    }
    if (showNutritionPreview) {
        AlertDialog(
            onDismissRequest = { showNutritionPreview = false },
            title = {
                Text(
                    text = stringResource(R.string.diet_nutrition_preview),
                    style = LocalAppTokens.current.type.title,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (source == DietFoodSource.FREE_NAME || netGrams <= 0.0 || previewNutrients.isEmpty()) {
                        Text(stringResource(R.string.diet_no_nutrition), color = LocalAppTokens.current.colors.textMuted)
                    } else {
                        previewFood?.let {
                            Text(it.displayName(language), style = LocalAppTokens.current.type.body)
                        }
                        Text(
                            text = stringResource(
                                R.string.diet_preview_summary,
                                formatCalories(previewNutrients["ENERGY"]?.value ?: 0.0),
                                formatGrams(previewNutrients["PROTEIN"]?.value ?: 0.0),
                                formatGrams(previewNutrients["FAT"]?.value ?: 0.0),
                                formatGrams(previewNutrients["CHO"]?.value ?: 0.0),
                            ),
                            style = LocalAppTokens.current.type.body,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNutritionPreview = false }) {
                    Text(stringResource(R.string.compose_confirm_dialog_ok))
                }
            },
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
            style = LocalAppTokens.current.type.bodyS,
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
    kind: FoodKind?,
    customOnly: Boolean,
    root: String?,
    child: String?,
    viewModel: DietViewModel,
    language: String,
): List<FoodItem> {
    return foods.filter { food ->
        if (kind != null && food.kind != kind) return@filter false
        if (customOnly && !UserCustomFoodRepository.isCustom(food.id)) return@filter false
        food.searchMatchRank(keyword) ?: return@filter false
        val tags = (food as? CategorizedFood)?.categoryTags.orEmpty()
        if (kind != FoodKind.DISH) {
            if (root != null && !viewModel.hasCategory(tags, root)) return@filter false
            if (child != null && !viewModel.hasCategory(tags, child)) return@filter false
        }
        true
    }.sortedWith(compareBy<FoodItem> { it.searchMatchRank(keyword) ?: Int.MAX_VALUE }
        .thenByDescending { it.commonness }
        .thenBy { it.displayName(language) })
}

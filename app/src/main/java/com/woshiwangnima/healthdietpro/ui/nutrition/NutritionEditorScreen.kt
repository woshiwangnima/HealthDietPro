package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.MultiLevelTagSelector
import com.woshiwangnima.healthdietpro.common.ui.TagNode
import com.woshiwangnima.healthdietpro.model.food.DishComponentDto
import com.woshiwangnima.healthdietpro.model.food.DishTaxonomy
import com.woshiwangnima.healthdietpro.model.food.FoodAmountDto
import com.woshiwangnima.healthdietpro.model.food.FoodCategories
import com.woshiwangnima.healthdietpro.model.food.FoodDerivationDto
import com.woshiwangnima.healthdietpro.model.food.FoodDto
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.FoodMetricDto
import com.woshiwangnima.healthdietpro.model.food.FoodHealthMetricsDto
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientTableDto
import com.woshiwangnima.healthdietpro.model.food.FoodQuantityDto
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.PreparedFood
import com.woshiwangnima.healthdietpro.model.food.RecipeStepDto
import com.woshiwangnima.healthdietpro.model.food.Dish
import java.util.UUID

@Composable
internal fun NutritionEditorScreen(editor: NutritionEditorState, viewModel: NutritionViewModel) {
    val language = LocaleLanguage()
    val existing = editor.editingId?.let { viewModel.foodById(it) }
    val titleRes = when (editor.kind) {
        FoodKind.INGREDIENT -> R.string.nutrition_editor_title_ingredient
        FoodKind.FOOD -> R.string.nutrition_editor_title_food
        FoodKind.DISH -> R.string.nutrition_editor_title_dish
    }
    BaseScreen(title = stringResource(titleRes), onBack = viewModel::closeEditor, includeStatusBarPadding = false) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (editor.kind) {
                FoodKind.INGREDIENT -> IngredientEditor(existing as? Ingredient, editor.editingId, viewModel, language)
                FoodKind.FOOD -> FoodEditor(existing as? PreparedFood, editor.editingId, viewModel, language)
                FoodKind.DISH -> DishEditor(existing as? Dish, editor.editingId, viewModel, language)
            }
        }
    }
}

@Composable
private fun LocaleLanguage(): String =
    androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "en"

private fun newCustomId(): String = "custom:" + UUID.randomUUID().toString()

private fun namesMapList(language: String, primary: String, aliases: List<String>): Map<String, List<String>> =
    mapOf(language to (listOf(primary.trim()) + aliases.map { it.trim() }.filter { it.isNotBlank() }))

/** 别名编辑：芯片展示已添加别名，点「添加别名」弹输入，而非逗号分隔。 */
@Composable
private fun AliasEditor(aliases: androidx.compose.runtime.snapshots.SnapshotStateList<String>, onChanged: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    Text(stringResource(R.string.nutrition_editor_aliases), style = MaterialTheme.typography.labelMedium)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        aliases.forEachIndexed { index, alias ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(alias, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    IconButton(onClick = { aliases.removeAt(index); onChanged() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.nutrition_editor_delete), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        OutlinedButton(onClick = { adding = true }) { Text(stringResource(R.string.nutrition_editor_add_alias)) }
    }
    if (adding) {
        var draft by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.nutrition_editor_add_alias)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(stringResource(R.string.nutrition_editor_alias_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = draft.trim()
                    if (v.isNotBlank() && v !in aliases) { aliases.add(v); onChanged() }
                    adding = false
                }) { Text(stringResource(R.string.nutrition_editor_add_alias)) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.body_record_cancel)) } },
        )
    }
}

/** 难度 10 星整数选择：点第 n 颗切换到 n（再次点当前值则清零）。 */
@Composable
private fun StarRatingPicker(rating: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(10) { i ->
            val filled = i < rating
            IconButton(onClick = { onChange(if (rating == i + 1) 0 else i + 1) }, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
        }
    }
}


/**
 * 编辑器骨架：可滚动内容区（weight 1f）+ 固定底部保存/取消按钮；返回时若有未保存改动弹二次确认。
 */
@Composable
private fun EditorScaffold(
    dirty: Boolean,
    canSave: Boolean,
    onSave: () -> Unit,
    onExit: () -> Unit,
    body: @Composable () -> Unit,
) {
    var showUnsaved by remember { mutableStateOf(false) }
    val attemptExit = { if (dirty) showUnsaved = true else onExit() }
    BackHandler(enabled = true) { attemptExit() }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { body() }
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = attemptExit, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.body_record_cancel))
            }
            Button(onClick = onSave, enabled = canSave, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.nutrition_editor_save))
            }
        }
    }
    if (showUnsaved) {
        AlertDialog(
            onDismissRequest = { showUnsaved = false },
            title = { Text(stringResource(R.string.nutrition_editor_unsaved_title)) },
            text = { Text(stringResource(R.string.nutrition_editor_unsaved_message)) },
            confirmButton = { TextButton(onClick = { showUnsaved = false; onExit() }) { Text(stringResource(R.string.nutrition_editor_unsaved_discard)) } },
            dismissButton = { TextButton(onClick = { showUnsaved = false }) { Text(stringResource(R.string.nutrition_editor_unsaved_keep)) } },
        )
    }
}

@Composable
private fun EditorField(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
}

/** 用公共多级 Tag 选择器选分类，可多选多个多级 Tag。 */
@Composable
private fun CategoryTagSelector(selected: List<String>, onChange: (List<String>) -> Unit) {
    MultiLevelTagSelector(
        title = stringResource(R.string.nutrition_editor_category),
        roots = foodCategoryTagTree(),
        selectedTags = selected,
        onSelectionChange = onChange,
    )
}

@Composable
private fun foodCategoryTagTree(): List<TagNode> = FoodCategories.roots.map { root ->
    TagNode(
        tag = root.tag,
        label = stringResource(root.labelRes),
        children = FoodCategories.childrenForRoots(setOf(root.tag)).map { child ->
            TagNode(child.tag, stringResource(child.labelRes))
        },
    )
}

/** 单选标签芯片组（横向滚动）。 */
@Composable
private fun SingleChoiceChips(taxa: List<Pair<String, Int>>, selected: String?, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        taxa.forEach { (id, labelRes) ->
            FilterChip(selected = selected == id, onClick = { onSelect(id) }, label = { Text(stringResource(labelRes)) })
        }
    }
}

/** 多选标签芯片组（横向滚动）。 */
@Composable
private fun MultiChoiceChips(taxa: List<Pair<String, Int>>, selected: List<String>, onToggle: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        taxa.forEach { (id, labelRes) ->
            FilterChip(selected = id in selected, onClick = { onToggle(id) }, label = { Text(stringResource(labelRes)) })
        }
    }
}

// ---------------- Ingredient ----------------

@Composable
private fun IngredientEditor(existing: Ingredient?, editingId: String?, viewModel: NutritionViewModel, language: String) {
    var name by remember { mutableStateOf(existing?.displayName(language).orEmpty()) }
    val aliases = remember { mutableStateListOf<String>().apply { existing?.allNames(language)?.drop(1)?.let { addAll(it) } } }
    val categoryTags = remember { mutableStateListOf<String>().apply { existing?.categoryTags?.let { addAll(it) } } }
    val baseTable = existing?.let { it.nutritionTables["standard.100g_edible"] ?: it.nutritionTables.values.firstOrNull() }
    var edible by remember { mutableStateOf(existing?.edibleRatio?.let { (it * 100).toInt().toString() }.orEmpty()) }
    var description by remember { mutableStateOf(existing?.displayDescription(language).orEmpty()) }
    var gi by remember { mutableStateOf(existing?.healthMetrics?.glycemicIndex?.value?.toString().orEmpty()) }
    var gl by remember { mutableStateOf(existing?.healthMetrics?.glycemicLoadPer100g?.value?.toString().orEmpty()) }
    val metas = remember { viewModel.nutrientMetas() }
    val values = remember {
        mutableStateMapOf<String, String>().apply {
            metas.forEach { meta -> baseTable?.nutrients?.get(meta.code)?.value?.let { put(meta.code, it.toString()) } }
        }
    }
    var dirty by remember { mutableStateOf(false) }
    var optionalExpanded by remember { mutableStateOf(false) }
    val required = remember(metas) { metas.filter { it.isRequired } }
    val optional = remember(metas) { metas.filterNot { it.isRequired } }

    fun markDirty() { dirty = true }

    EditorScaffold(
        dirty = dirty,
        canSave = name.isNotBlank() && required.all { values[it.code]?.toDoubleOrNull() != null },
        onExit = viewModel::closeEditor,
        onSave = {
            val nutrients = buildMap {
                metas.forEach { meta ->
                    values[meta.code]?.toDoubleOrNull()?.let { v ->
                        put(meta.code, FoodAmountDto(v, NutrientMeta.unitCategoryFor(meta.code), meta.baseUnit))
                    }
                }
            }
            val health = FoodHealthMetricsDto(
                glycemicIndex = gi.toDoubleOrNull()?.let { FoodMetricDto(it, "GI") },
                glycemicLoadPer100g = gl.toDoubleOrNull()?.let { FoodMetricDto(it, "GL") },
            )
            viewModel.saveCustomFood(
                FoodDto(
                    id = editingId ?: newCustomId(),
                    kind = "ingredient",
                    names = namesMapList(language, name, aliases),
                    categoryTags = categoryTags.toList(),
                    nutritionTables = mapOf(
                        "standard.100g_edible" to FoodNutrientTableDto(FoodQuantityDto(100.0, "weight", "g"), nutrients),
                    ),
                    edibleRatio = edible.toDoubleOrNull()?.let { it / 100.0 },
                    healthMetrics = health,
                    description = if (description.isBlank()) emptyMap() else mapOf(language to description.trim()),
                    commonness = existing?.commonness ?: 1,
                ),
            )
        },
    ) {
        SectionHeader(stringResource(R.string.nutrition_editor_section_required))
        EditorField(stringResource(R.string.nutrition_editor_name) + " *", name, { name = it; markDirty() })
        required.forEach { meta ->
            NutrientField(meta, values[meta.code].orEmpty(), required = true) { values[meta.code] = it; markDirty() }
        }

        SectionHeader(stringResource(R.string.nutrition_editor_section_optional))
        AliasEditor(aliases) { markDirty() }
        CategoryTagSelector(categoryTags) { categoryTags.clear(); categoryTags.addAll(it); markDirty() }
        EditorField(stringResource(R.string.nutrition_editor_edible_ratio), edible, { edible = it; markDirty() }, numeric = true)
        EditorField(stringResource(R.string.nutrition_editor_gi), gi, { gi = it; markDirty() }, numeric = true)
        EditorField(stringResource(R.string.nutrition_editor_gl), gl, { gl = it; markDirty() }, numeric = true)
        EditorField(stringResource(R.string.nutrition_editor_description), description, { description = it; markDirty() }, singleLine = false)

        // 可选营养素折叠，避免一次性组合 40+ 输入框造成卡顿。
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { optionalExpanded = !optionalExpanded },
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ) {
            Text(
                stringResource(R.string.nutrition_editor_optional_nutrients),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (optionalExpanded) {
            optional.forEach { meta ->
                NutrientField(meta, values[meta.code].orEmpty(), required = false) { values[meta.code] = it; markDirty() }
            }
        }
    }
}

@Composable
private fun NutrientField(meta: NutrientMeta, value: String, required: Boolean, onChange: (String) -> Unit) {
    val language = LocaleLanguage()
    val suffix = if (required) " *" else ""
    EditorField(
        label = "${meta.displayName(language)} (${meta.baseUnit}/100g)$suffix",
        value = value,
        onChange = onChange,
        numeric = true,
    )
}

// ---------------- Food ----------------

@Composable
private fun FoodEditor(existing: PreparedFood?, editingId: String?, viewModel: NutritionViewModel, language: String) {
    var name by remember { mutableStateOf(existing?.displayName(language).orEmpty()) }
    val aliases = remember { mutableStateListOf<String>().apply { existing?.allNames(language)?.drop(1)?.let { addAll(it) } } }
    val categoryTags = remember { mutableStateListOf<String>().apply { existing?.categoryTags?.let { addAll(it) } } }
    val ingredients = remember { viewModel.selectableIngredients() }
    val methods = remember { viewModel.cookingMethods() }
    var ingredientId by remember { mutableStateOf(existing?.derivedFrom?.ingredientId) }
    var methodId by remember { mutableStateOf(existing?.derivedFrom?.cookingMethodId) }
    var description by remember { mutableStateOf(existing?.displayDescription(language).orEmpty()) }
    var selectingIngredient by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    fun markDirty() { dirty = true }

    val preview = if (ingredientId != null && methodId != null) {
        viewModel.previewDerived(ingredientId!!, methodId!!)
    } else null

    EditorScaffold(
        dirty = dirty,
        canSave = name.isNotBlank() && ingredientId != null && methodId != null,
        onExit = viewModel::closeEditor,
        onSave = {
            viewModel.saveCustomFood(
                FoodDto(
                    id = editingId ?: newCustomId(),
                    kind = "food",
                    names = namesMapList(language, name, aliases),
                    categoryTags = categoryTags.toList(),
                    derivedFrom = FoodDerivationDto(requireNotNull(ingredientId), requireNotNull(methodId)),
                    description = if (description.isBlank()) emptyMap() else mapOf(language to description.trim()),
                    commonness = existing?.commonness ?: 1,
                ),
            )
        },
    ) {
        SectionHeader(stringResource(R.string.nutrition_editor_section_required))
        EditorField(stringResource(R.string.nutrition_editor_name) + " *", name, { name = it; markDirty() })
        Text(stringResource(R.string.nutrition_editor_source_ingredient) + " *", style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { selectingIngredient = true }, modifier = Modifier.fillMaxWidth()) {
            Text(ingredientId?.let { viewModel.foodById(it)?.displayName(language) } ?: stringResource(R.string.nutrition_editor_select_ingredient))
        }
        Text(stringResource(R.string.nutrition_editor_cooking_method) + " *", style = MaterialTheme.typography.labelMedium)
        // 烹饪方式标签为本地化字符串（非资源 id），直接渲染。
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            methods.forEach { m ->
                FilterChip(selected = methodId == m.id, onClick = { methodId = m.id; markDirty() }, label = { Text(m.displayLabel(language)) })
            }
        }

        // 运行时派生的营养素预览
        Text(stringResource(R.string.nutrition_editor_ratio_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (preview != null) {
            SectionHeader(stringResource(R.string.nutrition_editor_computed_nutrients))
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("ENERGY", "PROTEIN", "FAT", "CHO", "FIBER").forEach { code ->
                        preview.nutrients[code]?.let { amt ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(code, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text("%.1f %s".format(amt.value, amt.unitId), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        SectionHeader(stringResource(R.string.nutrition_editor_section_optional))
        AliasEditor(aliases) { markDirty() }
        CategoryTagSelector(categoryTags) { categoryTags.clear(); categoryTags.addAll(it); markDirty() }
        EditorField(stringResource(R.string.nutrition_editor_description), description, { description = it; markDirty() }, singleLine = false)
    }
    if (selectingIngredient) {
        IngredientPickerDialog(
            ingredients = ingredients,
            selectedId = ingredientId,
            language = language,
            title = stringResource(R.string.nutrition_editor_select_ingredient),
            onDismiss = { selectingIngredient = false },
            onSelect = { id -> ingredientId = id; selectingIngredient = false; markDirty() },
        )
    }
}

/** 可复用于来源食材和菜肴食材清单的搜索 + 分类选择弹窗。 */
@Composable
private fun IngredientPickerDialog(
    ingredients: List<Ingredient>,
    selectedId: String?,
    language: String,
    title: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var selectedRoot by remember { mutableStateOf<String?>(null) }
    var selectedChild by remember { mutableStateOf<String?>(null) }
    val children = selectedRoot?.let { FoodCategories.childrenForRoots(setOf(it)) }.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = keyword, onValueChange = { keyword = it }, label = { Text(stringResource(R.string.nutrition_editor_search_ingredient)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FoodCategories.roots.forEach { root ->
                        FilterChip(selected = selectedRoot == root.tag, onClick = { selectedRoot = if (selectedRoot == root.tag) null else root.tag; selectedChild = null }, label = { Text(stringResource(root.labelRes)) })
                    }
                }
                if (children.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        children.forEach { child ->
                            FilterChip(selected = selectedChild == child.tag, onClick = { selectedChild = if (selectedChild == child.tag) null else child.tag }, label = { Text(stringResource(child.labelRes)) })
                        }
                    }
                }
                val filtered = ingredients.filter { ing ->
                    val kw = keyword.trim().lowercase()
                    val matchesKw = kw.isEmpty() || ing.searchableNames().any { it.lowercase().contains(kw) }
                    val matchesRoot = selectedRoot == null || FoodCategories.hasTagWithin(ing.categoryTags, selectedRoot!!)
                    val matchesChild = selectedChild == null || FoodCategories.hasTagWithin(ing.categoryTags, selectedChild!!)
                    matchesKw && matchesRoot && matchesChild
                }
                Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    filtered.take(80).forEach { ing ->
                        Surface(modifier = Modifier.fillMaxWidth().clickable { onSelect(ing.id) }, shape = RoundedCornerShape(6.dp), color = if (ing.id == selectedId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                            Text(ing.displayName(language), modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.body_record_cancel)) } },
    )
}

// ---------------- Dish ----------------

private data class ComponentDraft(val foodId: String, val grams: String)
private data class StepDraft(val text: String, val minutes: String)

@Composable
private fun DishEditor(existing: Dish?, editingId: String?, viewModel: NutritionViewModel, language: String) {
    var name by remember { mutableStateOf(existing?.displayName(language).orEmpty()) }
    val aliases = remember { mutableStateListOf<String>().apply { existing?.allNames(language)?.drop(1)?.let { addAll(it) } } }
    var description by remember { mutableStateOf(existing?.displayDescription(language).orEmpty()) }
    var imageKey by remember { mutableStateOf(existing?.image?.localKey) }
    val ingredients = remember { viewModel.selectableIngredients() }
    val components = remember {
        mutableStateListOf<ComponentDraft>().apply {
            existing?.components?.forEach { add(ComponentDraft(it.foodId, it.quantity.value.toInt().toString())) }
        }
    }
    val steps = remember {
        mutableStateListOf<StepDraft>().apply {
            existing?.recipeSteps?.forEach { add(StepDraft(it.text, it.minutes?.toString().orEmpty())) }
        }
    }
    var cuisine by remember { mutableStateOf(existing?.cuisine) }
    val dishCategories = remember { mutableStateListOf<String>().apply { existing?.dishCategories?.let { addAll(it) } } }
    val tastes = remember { mutableStateListOf<String>().apply { existing?.tastes?.let { addAll(it) } } }
    val seasons = remember { mutableStateListOf<String>().apply { existing?.seasons?.let { addAll(it) } } }
    var techniqueId by remember { mutableStateOf(existing?.techniqueId) }
    var difficulty by remember { mutableStateOf(existing?.difficulty ?: 0) }
    var serves by remember { mutableStateOf(existing?.servesPeople?.toString().orEmpty()) }
    var selectingIngredient by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    val methods = remember { viewModel.cookingMethods() }
    val context = androidx.compose.ui.platform.LocalContext.current
    fun markDirty() { dirty = true }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            viewModel.saveCustomImage(context, uri) { savedKey ->
                imageKey = savedKey
                markDirty()
            }
        }
    }

    EditorScaffold(
        dirty = dirty,
        canSave = name.isNotBlank() && components.isNotEmpty() && components.all { it.grams.toDoubleOrNull() != null },
        onExit = viewModel::closeEditor,
        onSave = {
            viewModel.saveCustomFood(
                FoodDto(
                    id = editingId ?: newCustomId(),
                    kind = "dish",
                    names = namesMapList(language, name, aliases),
                    components = components.mapNotNull { d ->
                        d.grams.toDoubleOrNull()?.let { DishComponentDto(d.foodId, FoodQuantityDto(it, "weight", "g")) }
                    },
                    description = if (description.isBlank()) emptyMap() else mapOf(language to description.trim()),
                    image = imageKey?.let { com.woshiwangnima.healthdietpro.model.food.FoodImageDto(localKey = it, attribution = "user") },
                    cuisine = cuisine,
                    dishCategories = dishCategories.toList(),
                    recipeSteps = steps.filter { it.text.isNotBlank() }.map { RecipeStepDto(it.text.trim(), it.minutes.toIntOrNull()) },
                    difficulty = difficulty.takeIf { it > 0 },
                    servesPeople = serves.toIntOrNull(),
                    tastes = tastes.toList(),
                    techniqueId = techniqueId,
                    seasons = seasons.toList(),
                    commonness = existing?.commonness ?: 1,
                ),
            )
        },
    ) {
        SectionHeader(stringResource(R.string.nutrition_editor_section_required))
        EditorField(stringResource(R.string.nutrition_editor_name) + " *", name, { name = it; markDirty() })
        Text(stringResource(R.string.nutrition_ingredient_list) + " *", style = MaterialTheme.typography.labelMedium)
        components.forEachIndexed { index, draft ->
            val item = viewModel.foodById(draft.foodId)
            val label = item?.displayName(language) ?: draft.foodId
            val auxiliary = item?.let { viewModel.isAuxiliary(it) } == true
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    if (auxiliary) Text(stringResource(R.string.nutrition_ingredient_auxiliary), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                OutlinedTextField(
                    value = draft.grams,
                    onValueChange = { components[index] = draft.copy(grams = it); markDirty() },
                    label = { Text(stringResource(R.string.nutrition_editor_component_grams)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(120.dp),
                )
                IconButton(onClick = { components.removeAt(index); markDirty() }) { Icon(Icons.Filled.Close, stringResource(R.string.nutrition_editor_delete)) }
            }
        }
        OutlinedButton(onClick = { selectingIngredient = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nutrition_editor_add_ingredient))
        }

        SectionHeader(stringResource(R.string.nutrition_editor_section_optional))
        AliasEditor(aliases) { markDirty() }
        EditorField(stringResource(R.string.nutrition_editor_description), description, { description = it; markDirty() }, singleLine = false)

        // 封面图（可选）
        Text(stringResource(R.string.nutrition_editor_image), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text(stringResource(R.string.nutrition_editor_image_pick)) }
            if (imageKey != null) {
                OutlinedButton(onClick = { imageKey = null; markDirty() }) { Text(stringResource(R.string.nutrition_editor_image_clear)) }
            }
        }

        Text(stringResource(R.string.nutrition_editor_cuisine), style = MaterialTheme.typography.labelMedium)
        // 菜系多级选择（中餐可只选一级）
        val resources = context.resources
        MultiLevelTagSelector(
            title = "",
            roots = DishTaxonomy.cuisineTagNodes { resources.getString(it) },
            selectedTags = listOfNotNull(cuisine),
            onSelectionChange = { sel -> cuisine = sel.lastOrNull(); markDirty() },
        )
        Text(stringResource(R.string.nutrition_editor_dish_category), style = MaterialTheme.typography.labelMedium)
        MultiChoiceChips(DishTaxonomy.categories.map { it.id to it.labelRes }, dishCategories) { toggle(dishCategories, it); markDirty() }
        Text(stringResource(R.string.nutrition_editor_taste), style = MaterialTheme.typography.labelMedium)
        MultiChoiceChips(DishTaxonomy.tastes.map { it.id to it.labelRes }, tastes) { toggle(tastes, it); markDirty() }
        Text(stringResource(R.string.nutrition_editor_season), style = MaterialTheme.typography.labelMedium)
        MultiChoiceChips(DishTaxonomy.seasons.map { it.id to it.labelRes }, seasons) { toggle(seasons, it); markDirty() }
        Text(stringResource(R.string.nutrition_editor_technique), style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            methods.forEach { m ->
                FilterChip(selected = techniqueId == m.id, onClick = { techniqueId = if (techniqueId == m.id) null else m.id; markDirty() }, label = { Text(m.displayLabel(language)) })
            }
        }
        // 难度：10 星整数选择
        Text(stringResource(R.string.nutrition_editor_difficulty), style = MaterialTheme.typography.labelMedium)
        StarRatingPicker(difficulty) { difficulty = it; markDirty() }
        EditorField(stringResource(R.string.nutrition_editor_serves), serves, { serves = it; markDirty() }, numeric = true)

        // 制作教程/菜谱，逐步（可选每步分钟）
        Text(stringResource(R.string.nutrition_editor_recipe), style = MaterialTheme.typography.labelMedium)
        steps.forEachIndexed { index, step ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.nutrition_editor_step_number, index + 1), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { steps.removeAt(index); markDirty() }) { Icon(Icons.Filled.Close, stringResource(R.string.nutrition_editor_delete)) }
                }
                OutlinedTextField(
                    value = step.text,
                    onValueChange = { steps[index] = step.copy(text = it); markDirty() },
                    label = { Text(stringResource(R.string.nutrition_editor_step_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = step.minutes,
                    onValueChange = { steps[index] = step.copy(minutes = it); markDirty() },
                    label = { Text(stringResource(R.string.nutrition_editor_step_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        OutlinedButton(onClick = { steps.add(StepDraft("", "")); markDirty() }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nutrition_editor_add_step))
        }
    }

    if (selectingIngredient) {
        IngredientPickerDialog(
            ingredients = ingredients,
            selectedId = null,
            language = language,
            title = stringResource(R.string.nutrition_editor_add_ingredient),
            onDismiss = { selectingIngredient = false },
            onSelect = { id -> components.add(ComponentDraft(id, "100")); selectingIngredient = false; markDirty() },
        )
    }
}

private fun toggle(list: MutableList<String>, value: String) {
    if (value in list) list.remove(value) else list.add(value)
}

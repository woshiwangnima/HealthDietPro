package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.FontTokens
import com.woshiwangnima.healthdietpro.common.ui.FoodImageStore
import com.woshiwangnima.healthdietpro.common.ui.AppInfoDialog
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.range.NumericRangeBand
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.Dish
import com.woshiwangnima.healthdietpro.model.food.DishTaxonomy
import com.woshiwangnima.healthdietpro.model.food.RecipeStep
import com.woshiwangnima.healthdietpro.model.food.FoodCategories
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.FoodServing
import com.woshiwangnima.healthdietpro.model.food.GlycemicClassification
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.PreparedFood
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.model.food.classifyGlycemicIndex
import com.woshiwangnima.healthdietpro.model.food.classifyGlycemicLoad
import com.woshiwangnima.healthdietpro.model.food.glycemicIndexClassificationBands
import com.woshiwangnima.healthdietpro.model.food.glycemicLoadClassificationBands

private fun FoodItem.categoryTagsOrEmpty(): List<String> = (this as? CategorizedFood)?.categoryTags.orEmpty()

private fun FoodItem.defaultServings(): List<FoodServing> = servings.ifEmpty {
    listOf(FoodServing("per_100g", "standard.100g", 1.0, mapOf("zh" to "100 克", "en" to "100 g")))
}

@StringRes
private fun FoodKind.customLabelRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.nutrition_custom_ingredient
    FoodKind.FOOD -> R.string.nutrition_custom_food
    FoodKind.DISH -> R.string.nutrition_custom_dish
}

@StringRes
private fun FoodKind.addLabelRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.nutrition_add_custom_ingredient
    FoodKind.FOOD -> R.string.nutrition_add_custom_food
    FoodKind.DISH -> R.string.nutrition_add_custom_dish
}

@StringRes
private fun FoodKind.detailTitleRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.nutrition_detail_title_ingredient
    FoodKind.FOOD -> R.string.nutrition_detail_title_food
    FoodKind.DISH -> R.string.nutrition_detail_title_dish
}

@StringRes
private fun FoodKind.comparisonTitleRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.nutrition_comparison_title_ingredient
    FoodKind.FOOD -> R.string.nutrition_comparison_title_food
    FoodKind.DISH -> R.string.nutrition_comparison_title_dish
}

/**
 * Kind-specific (container, content) colors for the rounded name background and matching chips.
 * Fixed appetizing hues that stay identical in light/dark and contrast strongly with the green theme:
 * 食材=鲜叶绿, 食物=暖橙, 菜肴=番茄红。文字统一用白色保证对比度。
 */
private val IngredientContainer = androidx.compose.ui.graphics.Color(0xFF43A047) // leaf green
private val FoodContainer = androidx.compose.ui.graphics.Color(0xFFF57C00) // appetizing orange
private val DishContainer = androidx.compose.ui.graphics.Color(0xFFE53935) // tomato red
private val KindOnContainer = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

private fun FoodKind.nameColors(): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> = when (this) {
    FoodKind.INGREDIENT -> IngredientContainer to KindOnContainer
    FoodKind.FOOD -> FoodContainer to KindOnContainer
    FoodKind.DISH -> DishContainer to KindOnContainer
}

@Composable
internal fun NutritionScreen(viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when {
        state.editor != null -> NutritionEditorScreen(requireNotNull(state.editor), viewModel)
        state.comparisonReturnTarget != null -> ComparisonPlaceholder(state.selectedFood?.kind ?: state.selectedKind, onBack = viewModel::closeComparison)
        state.selectedFood != null -> FoodDetailScreen(requireNotNull(state.selectedFood), viewModel, viewModel::closeFood) { viewModel.openComparison(NutritionDestination.FoodDetail) }
        else -> FoodBrowseScreen(state, viewModel, modifier)
    }
}

@Composable
private fun FoodBrowseScreen(state: NutritionUiState, viewModel: NutritionViewModel, modifier: Modifier) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    val foods = viewModel.filteredFoods(language)
    var addingTag by remember { mutableStateOf(false) }
    val showSidebar = state.selectedKind != FoodKind.DISH
    Column(modifier = modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.keyword,
            onValueChange = viewModel::setKeyword,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (state.keyword.isNotBlank()) {
                {
                    IconButton(onClick = { viewModel.setKeyword("") }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.nutrition_search_clear),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            } else {
                null
            },
            placeholder = { Text(stringResource(R.string.nutrition_search_food), style = TextStyle(fontSize = FontTokens.caption), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)) },
            textStyle = TextStyle(fontSize = FontTokens.caption),
        )
        KindSegmenter(state.selectedKind, viewModel::selectKind)
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 左列：由上至下为「添加自定义XX」按钮 + 「自定义XX」与其他一级分类组合区域，同宽。
            if (showSidebar) {
                Column(modifier = Modifier.width(80.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AddCustomButton(state.selectedKind) { viewModel.openEditor(state.selectedKind) }
                    CategorySidebar(state, viewModel, Modifier.weight(1f))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                // 菜肴无侧栏，添加按钮独立置顶。
                if (!showSidebar) {
                    AddCustomButton(state.selectedKind, Modifier.fillMaxWidth().padding(bottom = 4.dp)) { viewModel.openEditor(state.selectedKind) }
                }
                TagRow(stringResource(R.string.nutrition_system_tags), listOf("common" to stringResource(R.string.nutrition_tag_common), "favorite" to stringResource(R.string.nutrition_tag_favorite), "recent" to stringResource(R.string.nutrition_tag_recent)), state.selectedSystemTag?.let(::setOf).orEmpty(), viewModel::toggleSystemTag)
                TagRow(stringResource(R.string.nutrition_user_tags), state.userTags.map { it.id to it.label }, state.selectedUserTags, viewModel::toggleUserTag, { addingTag = true })
                if (foods.isEmpty()) Text(stringResource(R.string.nutrition_no_foods), modifier = Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { items(foods, key = { it.id }) { FoodRow(it, language, viewModel, viewModel::openFood) } }
            }
        }
    }
    if (addingTag) AddTagDialog({ addingTag = false }) { viewModel.addUserTag(it); addingTag = false }
}

@Composable
private fun KindSegmenter(selected: FoodKind, onSelected: (FoodKind) -> Unit) {
    val kinds = listOf(
        FoodKind.INGREDIENT to R.string.nutrition_kind_ingredient,
        FoodKind.FOOD to R.string.nutrition_kind_food,
        FoodKind.DISH to R.string.nutrition_kind_dish,
    )
    EqualWidthSegmentedTabs(
        tabs = kinds.map { EqualWidthTab(it.second) },
        selectedIndex = kinds.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        onSelected = { onSelected(kinds[it].first) },
    )
}

/** Kind-colored「添加自定义XX」按钮，与左列同宽。 */
@Composable
private fun AddCustomButton(kind: FoodKind, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = kind.nameColors()
    Surface(
        modifier = modifier.fillMaxWidth().height(30.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = colors.first,
    ) {
        Row(modifier = Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp), tint = colors.second)
            Spacer(Modifier.width(2.dp))
            TextOverflowText(stringResource(kind.addLabelRes()), style = TextStyle(fontSize = FontTokens.caption), color = colors.second, maxLines = 1)
        }
    }
}

@Composable
private fun CategorySidebar(state: NutritionUiState, viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    val children = FoodCategories.childrenForRoots(state.selectedRoots)
    Surface(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        if (children.isEmpty()) {
            CategoryRootList(state, viewModel, Modifier.fillMaxWidth().padding(2.dp))
        } else {
            Row(Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                CategoryRootList(state, viewModel, Modifier.width(39.dp))
                LazyColumn(modifier = Modifier.width(35.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(children, key = { it.tag }) { category ->
                        CompactFilterChip(category.tag in state.selectedChildren, { viewModel.toggleChild(category.tag) }, Modifier.fillMaxWidth()) {
                            CategoryLabel(stringResource(category.labelRes))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRootList(state: NutritionUiState, viewModel: NutritionViewModel, modifier: Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            val colors = state.selectedKind.nameColors()
            Surface(
                modifier = Modifier.fillMaxWidth().height(28.dp).clickable(onClick = viewModel::toggleCustomOnly),
                shape = RoundedCornerShape(6.dp),
                color = if (state.customOnly) colors.first else colors.first.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (state.customOnly) colors.second else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(modifier = Modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
                    TextOverflowText(stringResource(state.selectedKind.customLabelRes()), style = TextStyle(fontSize = FontTokens.body), color = colors.second, maxLines = 1)
                }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
        items(FoodCategories.roots, key = { it.tag }) { category ->
            CompactFilterChip(category.tag in state.selectedRoots, { viewModel.toggleRoot(category.tag) }, Modifier.fillMaxWidth()) { CategoryLabel(stringResource(category.labelRes)) }
        }
    }
}

@Composable
private fun CategoryLabel(text: String) {
    TextOverflowText(text = text, style = TextStyle(fontSize = FontTokens.body), maxLines = 1)
}

@Composable
private fun TagRow(title: String, tags: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit, onAdd: (() -> Unit)? = null) {
    Surface(modifier = Modifier.fillMaxWidth().height(44.dp).padding(bottom = 4.dp), shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { tags.forEach { (id, label) -> CompactFilterChip(id in selected, { onToggle(id) }) { Text(label, style = TextStyle(fontSize = FontTokens.body)) } } }
            onAdd?.let { IconButton(onClick = it, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Add, stringResource(R.string.nutrition_add_tag)) } }
        }
    }
}

@Composable
private fun CompactFilterChip(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    Surface(
        modifier = modifier.height(28.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) { label() }
    }
}

/**
 * 名字区块：可选笔图标(自定义) + 用彩色圆角背景包裹的正名(按 kind 上色) +（烹饪方式）+ 别名。
 * cookingSuffix 与正名同字号并加括号；别名相对小字号，多个用 / 隔开。
 */
@Composable
private fun FoodNameHeader(
    food: FoodItem,
    language: String,
    isCustom: Boolean,
    nameFontSize: androidx.compose.ui.unit.TextUnit,
    cookingSuffix: String?,
    aliases: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = food.kind.nameColors()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isCustom) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.nutrition_custom_marker),
                modifier = Modifier.size(with(androidx.compose.ui.platform.LocalDensity.current) { nameFontSize.toDp() * 0.72f }),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(2.dp))
        }
        Surface(shape = RoundedCornerShape(6.dp), color = colors.first) {
            Text(
                food.displayName(language),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                style = TextStyle(fontSize = nameFontSize),
                color = colors.second,
            )
        }
        cookingSuffix?.let {
            Text(
                " ($it)",
                style = TextStyle(fontSize = nameFontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (aliases.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            TextOverflowText(
                text = aliases.joinToString(" / "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun FoodRow(food: FoodItem, language: String, viewModel: NutritionViewModel, onClick: (FoodItem) -> Unit) {
    val resolved = remember(food.id) { runCatching { viewModel.resolvePer100g(food) }.getOrNull() }
    val energy = resolved?.nutrients?.get("ENERGY")?.value ?: 0.0
    val image = viewModel.foodImages.image(food.image?.localKey)
    val categoryLabels = mutableListOf<String>()
    for (tag in food.categoryTagsOrEmpty()) {
        val pathLabels = mutableListOf<String>()
        for (labelRes in FoodCategories.displayTagPath(tag)) {
            pathLabels += stringResource(labelRes)
        }
        if (pathLabels.isNotEmpty()) {
            categoryLabels += pathLabels.joinToString(".")
        }
    }
    val cookingSuffix: String? = (food as? PreparedFood)?.let {
        viewModel.cookingMethodFor(it.derivedFrom.cookingMethodId)?.displayLabel(language)
    }
    val aliases = food.allNames(language).drop(1)
    // 菜肴额外显示组分数量作为次行。
    val secondaryLine: String? = (food as? Dish)?.let {
        stringResource(R.string.nutrition_dish_components) + ": " + it.components.size
    }
    var previewing by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(food) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodImage(image, Modifier.size(64.dp).clickable { previewing = true })
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            FoodNameHeader(
                food = food,
                language = language,
                isCustom = viewModel.isCustom(food.id),
                nameFontSize = FontTokens.subtitle,
                cookingSuffix = cookingSuffix,
                aliases = aliases,
            )
            Text(stringResource(R.string.nutrition_energy_per_100g, energy), style = MaterialTheme.typography.bodyMedium)
            secondaryLine?.let {
                TextOverflowText(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            categoryLabels.takeIf { it.isNotEmpty() }?.let { labels ->
                TextOverflowText(
                    text = labels.joinToString(" / "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (previewing) FoodImagePreview(image, onDismiss = { previewing = false })
}

@Composable
private fun FoodDetailScreen(food: FoodItem, viewModel: NutritionViewModel, onBack: () -> Unit, onCompare: () -> Unit) {
    val imageStore = viewModel.foodImages
    var tab by remember { mutableIntStateOf(0) }
    val servings = remember(food.id) { food.defaultServings() }
    var selectedServingId by remember(food.id) { mutableStateOf(servings.first().id) }
    var previewing by remember { mutableStateOf(false) }
    var showHealthMetricsHelp by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val resolved = remember(food.id) { runCatching { viewModel.resolvePer100g(food) }.getOrNull() }
    val relatedDishes = remember(food.id) { viewModel.relatedDishes(food.id) }
    val isCustom = viewModel.isCustom(food.id)
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    val cookingSuffix: String? = (food as? PreparedFood)?.let {
        viewModel.cookingMethodFor(it.derivedFrom.cookingMethodId)?.displayLabel(language)
    }
    BaseScreen(
        title = stringResource(food.kind.detailTitleRes()),
        onBack = onBack,
        includeStatusBarPadding = false,
    ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            FoodImage(imageStore.image(food.image?.localKey), Modifier.size(96.dp).clickable { previewing = true })
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                FoodNameHeader(
                    food = food,
                    language = language,
                    isCustom = isCustom,
                    nameFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                    cookingSuffix = cookingSuffix,
                    aliases = food.allNames(language).drop(1),
                )
                food.displayDescription(language).takeIf { it.isNotBlank() }?.let { description ->
                    Text(description, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // 右侧图标分两行：上行 编辑/删除（自定义时），下行 收藏/对比。
            Column(horizontalAlignment = Alignment.End) {
                if (isCustom) {
                    Row {
                        IconButton(onClick = { viewModel.openEditor(food.kind, food.id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.nutrition_editor_edit)) }
                        IconButton(onClick = { confirmingDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.nutrition_editor_delete)) }
                    }
                }
                Row {
                    IconButton(onClick = { }) { Icon(Icons.Filled.FavoriteBorder, stringResource(R.string.nutrition_favorite)) }
                    IconButton(onClick = onCompare) { Icon(painterResource(R.drawable.ic_chart), stringResource(R.string.nutrition_compare)) }
                }
            }
        }
        EqualWidthSegmentedTabs(
            tabs = listOf(
                EqualWidthTab(R.string.nutrition_tab_profile),
                EqualWidthTab(food.rankingTabLabelRes()),
                EqualWidthTab(R.string.nutrition_tab_estimate),
            ),
            selectedIndex = tab,
            onSelected = { tab = it },
            modifier = Modifier.padding(top = 16.dp),
        )
        if (tab == 0) Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState())) {
            KindInfoSection(food, viewModel, language, onOpenFood = { viewModel.openFood(it) })
            DetailSectionTitle(R.drawable.ic_chart, stringResource(R.string.nutrition_health_metrics)) {
                IconButton(onClick = { showHealthMetricsHelp = true }) {
                    Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.nutrition_health_metrics_help))
                }
            }
            AppDataTable(
                rows = food.healthMetricRows(stringResource(R.string.nutrition_metric_no_data)),
                columns = listOf(
                    AppDataTableColumn("nutrient", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_item)) }, ColumnWidth.Flex(1f, 94.dp)) { AppDataTableText(stringResource(it.labelRes)) },
                    AppDataTableColumn("amount", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_amount)) }, ColumnWidth.Flex(0.8f, 74.dp)) { AppDataTableText(it.value) },
                    AppDataTableColumn("classification", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_classification)) }, ColumnWidth.Flex(0.8f, 74.dp)) {
                        if (it.classification != null) GlycemicClassificationText(it.classification)
                    },
                ),
                rowKey = { _, row -> row.key },
                modifier = Modifier.fillMaxWidth().height(154.dp),
            )
            DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_nutrients), Modifier.padding(top = 12.dp))
            FoodServingSelector(
                servings = servings,
                selectedServingId = selectedServingId,
                onSelected = { selectedServingId = it },
            )
            val selectedServing = servings.first { it.id == selectedServingId }
            AppDataTable(
                rows = nutrientRows(resolved, selectedServing),
                columns = listOf(
                    AppDataTableColumn("nutrient", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_item)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(stringResource(it.labelRes)) },
                    AppDataTableColumn("amount", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_amount)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(it.value) },
                ),
                rowKey = { _, row -> row.key },
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )
            if (relatedDishes.isNotEmpty()) {
                DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_related_dishes), Modifier.padding(top = 12.dp))
                relatedDishes.forEach { dish ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.openFood(dish) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(dish.displayName(language), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        else if (tab == 1 && food is Dish) {
            Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState())) {
                DishRecipeSection(food, viewModel, language) { viewModel.openFood(it) }
            }
        } else {
            Text(stringResource(R.string.nutrition_detail_placeholder), modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (previewing) FoodImagePreview(imageStore.image(food.image?.localKey), onDismiss = { previewing = false })
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.nutrition_editor_delete)) },
            text = { Text(stringResource(R.string.nutrition_editor_delete_confirm)) },
            confirmButton = { androidx.compose.material3.TextButton({ confirmingDelete = false; viewModel.deleteCustomFood(food.id) }) { Text(stringResource(R.string.nutrition_editor_delete)) } },
            dismissButton = { androidx.compose.material3.TextButton({ confirmingDelete = false }) { Text(stringResource(R.string.body_record_cancel)) } },
        )
    }
    if (showHealthMetricsHelp) {
        AppInfoDialog(
            title = stringResource(R.string.nutrition_health_metrics_help),
            onDismiss = { showHealthMetricsHelp = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HealthMetricInfoSection(
                    title = stringResource(R.string.nutrition_metric_gi),
                    description = stringResource(R.string.nutrition_metric_gi_help),
                    bands = glycemicIndexClassificationBands(),
                )
                HealthMetricInfoSection(
                    title = stringResource(R.string.nutrition_metric_gl),
                    description = stringResource(R.string.nutrition_metric_gl_help),
                    bands = glycemicLoadClassificationBands(),
                )
                HealthMetricInfoSection(
                    title = stringResource(R.string.nutrition_metric_inflammatory_potential),
                    description = stringResource(R.string.nutrition_metric_inflammatory_potential_help),
                )
            }
        }
    }
    }
}

@Composable
private fun KindInfoSection(food: FoodItem, viewModel: NutritionViewModel, language: String, onOpenFood: (FoodItem) -> Unit) {
    when (food) {
        is Ingredient -> food.edibleRatio?.let { ratio ->
            InfoLine(stringResource(R.string.nutrition_edible_ratio), stringResource(R.string.nutrition_edible_ratio_value, (ratio * 100).toInt()))
        }
        is PreparedFood -> {
            val source = viewModel.foodById(food.derivedFrom.ingredientId)
            val method = viewModel.cookingMethodFor(food.derivedFrom.cookingMethodId)
            // 来源食材：可点击跳转
            source?.let { src ->
                IngredientJumpLine(stringResource(R.string.nutrition_derived_from), src.displayName(language)) { onOpenFood(src) }
            }
            method?.let { InfoLine(stringResource(R.string.nutrition_cooking_method), it.displayLabel(language)) }
        }
        is Dish -> DishInfoSection(food, viewModel, language, onOpenFood)
    }
}

@Composable
private fun DishInfoSection(dish: Dish, viewModel: NutritionViewModel, language: String, onOpenFood: (FoodItem) -> Unit) {
    // 元数据（菜系/口味/份量/季节）
    dish.cuisine?.let { DishTaxonomy.labelRes(it)?.let { res -> InfoLine(stringResource(R.string.nutrition_editor_cuisine), stringResource(res)) } }
    dish.dishCategories.mapNotNull { DishTaxonomy.labelRes(it) }.map { stringResource(it) }.takeIf { it.isNotEmpty() }?.let { labels ->
        InfoLine(stringResource(R.string.nutrition_editor_dish_category), labels.joinToString(" / "))
    }
    dish.tastes.mapNotNull { DishTaxonomy.labelRes(it) }.map { stringResource(it) }.takeIf { it.isNotEmpty() }?.let { labels ->
        InfoLine(stringResource(R.string.nutrition_editor_taste), labels.joinToString(" / "))
    }
    dish.seasons.mapNotNull { DishTaxonomy.labelRes(it) }.map { stringResource(it) }.takeIf { it.isNotEmpty() }?.let { labels ->
        InfoLine(stringResource(R.string.nutrition_editor_season), labels.joinToString(" / "))
    }
    dish.techniqueId?.let { viewModel.cookingMethodFor(it)?.let { m -> InfoLine(stringResource(R.string.nutrition_editor_technique), m.displayLabel(language)) } }
    // 难度：10 星整数展示
    dish.difficulty?.let { StarRatingLine(stringResource(R.string.nutrition_dish_difficulty), it) }
    dish.servesPeople?.let { InfoLine(stringResource(R.string.nutrition_dish_serves), stringResource(R.string.nutrition_dish_serves_value, it)) }

}

/** 菜肴的食材清单与菜谱在「制作步骤」页签中呈现。 */
@Composable
private fun DishRecipeSection(dish: Dish, viewModel: NutritionViewModel, language: String, onOpenFood: (FoodItem) -> Unit) {
    // 食材清单：辅料（调味品或油脂）判定，主料在前、辅料在后。
    DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_ingredient_list))
    val (auxiliary, main) = dish.components.partition { c -> viewModel.foodById(c.foodId)?.let { viewModel.isAuxiliary(it) } == true }
    if (main.isNotEmpty()) {
        Text(stringResource(R.string.nutrition_ingredient_main), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        main.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
    }
    if (auxiliary.isNotEmpty()) {
        Text(stringResource(R.string.nutrition_ingredient_auxiliary), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 4.dp))
        auxiliary.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
    }

    // 制作教程/菜谱，逐步显示序号 + 可选计时器。
    if (dish.recipeSteps.isNotEmpty()) {
        DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_recipe_title), Modifier.padding(top = 8.dp))
        dish.recipeSteps.forEachIndexed { index, step ->
            RecipeStepRow(index + 1, step)
        }
    }
}

/** 食材清单一行：名称（可跳转）+ 用量。 */
@Composable
private fun DishComponentLine(
    component: com.woshiwangnima.healthdietpro.model.food.DishComponent,
    viewModel: NutritionViewModel,
    language: String,
    onOpenFood: (FoodItem) -> Unit,
) {
    val item = viewModel.foodById(component.foodId)
    val amount = stringResource(R.string.nutrition_component_amount, component.quantity.value, component.quantity.unitId)
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (item != null) {
            Text(
                item.displayName(language),
                modifier = Modifier.weight(1f).clickable { onOpenFood(item) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(amount, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.nutrition_jump_to_ingredient), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(component.foodId, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(amount, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 可跳转的来源行（食物→来源食材）。 */
@Composable
private fun IngredientJumpLine(label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.nutrition_jump_to_ingredient), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 难度星级：10 星整数展示。 */
@Composable
private fun StarRatingLine(label: String, rating: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(10) { i ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (i < rating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@Composable
private fun RecipeStepRow(number: Int, step: com.woshiwangnima.healthdietpro.model.food.RecipeStep) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(stringResource(R.string.nutrition_recipe_step, number), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(step.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        step.minutes?.let { StepTimer(it) }
    }
}

@Composable
private fun StepTimer(minutes: Int) {
    val totalSeconds = minutes * 60
    var remaining by remember(minutes) { mutableIntStateOf(totalSeconds) }
    var running by remember(minutes) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(running) {
        while (running && remaining > 0) {
            kotlinx.coroutines.delay(1000)
            remaining -= 1
        }
        if (remaining == 0) running = false
    }
    val finished = remaining == 0
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (finished) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 固定长宽的时间显示（HH:MM:SS，等宽字体，宽度不随数字变化）。
            Box(modifier = Modifier.width(124.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "%02d:%02d:%02d".format(remaining / 3600, (remaining % 3600) / 60, remaining % 60),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    ),
                    color = if (finished) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                )
            }
            if (running) {
                IconButton(onClick = { running = false }, enabled = !finished, modifier = Modifier.size(32.dp)) {
                    PauseBars()
                }
            } else {
                TimerIconButton(icon = Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.nutrition_timer_start), enabled = !finished) { running = true }
            }
            TimerIconButton(
                icon = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.nutrition_timer_reset),
            ) { running = false; remaining = totalSeconds }
        }
    }
}

private fun FoodItem.rankingTabLabelRes(): Int = when (kind) {
    FoodKind.INGREDIENT -> R.string.nutrition_tab_ingredient_ranking
    FoodKind.FOOD -> R.string.nutrition_tab_ranking
    FoodKind.DISH -> R.string.nutrition_tab_recipe_steps
}

@Composable
private fun TimerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

/** 暂停图标（两竖条），核心图标集无 Pause 时的替代。 */
@Composable
private fun PauseBars() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .background(MaterialTheme.colorScheme.onSecondaryContainer, RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DetailSectionTitle(iconRes: Int, title: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

private data class FoodProfileRow(
    val key: String,
    @param:StringRes val labelRes: Int,
    val value: String,
    val classification: GlycemicClassification? = null,
)

// GI/GL/炎症指数缺失时仍显示该行，数据填占位符「-」。
private fun FoodItem.healthMetricRows(noData: String): List<FoodProfileRow> = listOf(
    healthMetrics.glycemicIndex.let {
        FoodProfileRow("gi", R.string.nutrition_metric_gi, it?.let { m -> "${m.value} ${m.unit}" } ?: noData, it?.let { m -> classifyGlycemicIndex(m.value) })
    },
    healthMetrics.glycemicLoadPer100g.let {
        FoodProfileRow("gl", R.string.nutrition_metric_gl, it?.let { m -> "${m.value} ${m.unit}" } ?: noData, it?.let { m -> classifyGlycemicLoad(m.value) })
    },
    healthMetrics.inflammatoryPotential.let {
        FoodProfileRow("inflammatory", R.string.nutrition_metric_inflammatory_potential, it?.let { m -> "${m.value} ${m.unit}" } ?: noData)
    },
)

@Composable
private fun HealthMetricInfoSection(
    title: String,
    description: String,
    bands: List<NumericRangeBand<GlycemicClassification>>? = null,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, modifier = Modifier.padding(top = 2.dp))
        bands?.let { MetricClassificationTable(it, Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun MetricClassificationTable(
    bands: List<NumericRangeBand<GlycemicClassification>>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
                Text(stringResource(R.string.nutrition_profile_range), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.nutrition_profile_classification), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            }
            bands.forEach { band ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(metricRangeText(band), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Box(modifier = Modifier.weight(1f)) { GlycemicClassificationText(band.value) }
                }
            }
        }
    }
}

@Composable
private fun GlycemicClassificationText(classification: GlycemicClassification) {
    val color = when (classification) {
        GlycemicClassification.Low -> MaterialTheme.colorScheme.primary
        GlycemicClassification.Medium -> MaterialTheme.colorScheme.secondary
        GlycemicClassification.High -> MaterialTheme.colorScheme.error
    }
    val labelRes = when (classification) {
        GlycemicClassification.Low -> R.string.nutrition_classification_low
        GlycemicClassification.Medium -> R.string.nutrition_classification_medium
        GlycemicClassification.High -> R.string.nutrition_classification_high
    }
    Text(stringResource(labelRes), style = MaterialTheme.typography.bodySmall, color = color)
}

@Composable
private fun metricRangeText(band: NumericRangeBand<GlycemicClassification>): String {
    val min = band.min?.formatMetricThreshold()
    val max = band.max?.formatMetricThreshold()
    return when {
        min == null -> stringResource(R.string.nutrition_metric_range_at_most, requireNotNull(max))
        max == null -> stringResource(R.string.nutrition_metric_range_more_than, min)
        else -> stringResource(R.string.nutrition_metric_range_more_than_to_at_most, min, max)
    }
}

private fun Double.formatMetricThreshold(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun nutrientRows(resolved: ResolvedNutrition?, serving: FoodServing): List<FoodProfileRow> {
    val nutrients = resolved?.nutrients ?: return emptyList()
    val multiplier = serving.ratioToTable
    return nutrients.entries.map { (code, amount) ->
        FoodProfileRow(code, code.nutrientLabelRes(), "%.1f %s".format(amount.value * multiplier, amount.unitId))
    }
}

@Composable
private fun FoodServingSelector(servings: List<FoodServing>, selectedServingId: String, onSelected: (String) -> Unit) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        servings.forEach { serving ->
            FilterChip(
                selected = serving.id == selectedServingId,
                onClick = { onSelected(serving.id) },
                label = { Text(serving.displayLabel(language)) },
            )
        }
    }
}

@StringRes
private fun String.nutrientLabelRes(): Int = when (this) {
    "ENERGY" -> R.string.nutrition_nutrient_energy
    "PROTEIN" -> R.string.nutrition_nutrient_protein
    "FAT" -> R.string.nutrition_nutrient_fat
    "CHO" -> R.string.nutrition_nutrient_carbohydrate
    "FIBER" -> R.string.nutrition_nutrient_fiber
    else -> R.string.nutrition_nutrient_other
}

@Composable
private fun FoodImagePreview(image: ImageBitmap, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.body_record_cancel)) } },
        text = {
            FoodImage(image, Modifier.fillMaxWidth().height(280.dp))
        },
    )
}

@Composable
internal fun FoodImage(image: ImageBitmap, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        bitmap = image,
        contentDescription = stringResource(R.string.nutrition_food_image),
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun ComparisonPlaceholder(kind: FoodKind, onBack: () -> Unit) {
    BaseScreen(title = stringResource(kind.comparisonTitleRes()), onBack = onBack, includeStatusBarPadding = false) { padding ->
        Text(stringResource(R.string.nutrition_detail_placeholder), modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp))
    }
}

@Composable
private fun NutritionActionButton(iconRes: Int, text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(iconRes), null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(text, style = TextStyle(fontSize = FontTokens.body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AddTagDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) { var label by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.nutrition_add_tag)) }, text = { OutlinedTextField(label, { label = it }, label = { Text(stringResource(R.string.nutrition_tag_name)) }) }, confirmButton = { androidx.compose.material3.TextButton({ onAdd(label) }) { Text(stringResource(R.string.body_record_save)) } }, dismissButton = { androidx.compose.material3.TextButton(onDismiss) { Text(stringResource(R.string.body_record_cancel)) } }) }

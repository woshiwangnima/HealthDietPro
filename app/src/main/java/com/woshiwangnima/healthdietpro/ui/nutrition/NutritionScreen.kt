package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
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
import com.woshiwangnima.healthdietpro.common.ui.FoodSearchField
import com.woshiwangnima.healthdietpro.common.ui.SearchActivityPanel
import com.woshiwangnima.healthdietpro.common.ui.RecentSearchItem
import com.woshiwangnima.healthdietpro.common.ui.FoodImageStore
import com.woshiwangnima.healthdietpro.common.ui.FilterCollapseAxis
import com.woshiwangnima.healthdietpro.common.ui.FilterCollapseButton
import com.woshiwangnima.healthdietpro.common.ui.AppInfoDialog
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.Dish
import com.woshiwangnima.healthdietpro.model.food.DishTaxonomy
import com.woshiwangnima.healthdietpro.model.food.RecipeStep
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.FoodServing
import com.woshiwangnima.healthdietpro.model.food.GlycemicClassification
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.PreparedFood
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.model.food.classifyGlycemicIndex
import com.woshiwangnima.healthdietpro.model.food.classifyGlycemicLoad
import com.woshiwangnima.healthdietpro.model.food.glycemicLevel
import com.woshiwangnima.healthdietpro.model.food.glycemicIndexClassificationBands
import com.woshiwangnima.healthdietpro.model.food.glycemicLoadClassificationBands
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private fun FoodItem.categoryTagsOrEmpty(): List<String> = (this as? CategorizedFood)?.categoryTags.orEmpty()

private fun FoodItem.defaultServings(resolved: ResolvedNutrition?): List<FoodServing> = servings.ifEmpty {
    if (this is Dish && resolved != null) {
        listOf(FoodServing("whole_dish", "whole_dish", 1.0, mapOf("zh" to "整道菜", "en" to "Whole dish")))
    } else {
        listOf(FoodServing("per_100g", "standard.100g", 1.0, mapOf("zh" to "100 克", "en" to "100 g")))
    }
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
private fun systemTagPresentation(tag: String): Pair<String, androidx.compose.ui.graphics.Color> = when (tag) {
    "common" -> stringResource(R.string.nutrition_tag_common) to androidx.compose.ui.graphics.Color(0xFF2E7D32)
    "favorite" -> stringResource(R.string.nutrition_tag_favorite) to androidx.compose.ui.graphics.Color(0xFFC62828)
    "recent" -> stringResource(R.string.nutrition_tag_recent) to androidx.compose.ui.graphics.Color(0xFF1565C0)
    else -> tag to androidx.compose.ui.graphics.Color(0xFF6A1B9A)
}

@Composable
private fun FoodImageWithSystemTags(
    food: FoodItem,
    image: ImageBitmap,
    isFavorite: Boolean,
    isRecent: Boolean,
    modifier: Modifier = Modifier,
) {
    val tags = buildList {
        addAll(food.systemTags)
        if (isFavorite) add("favorite")
        if (isRecent) add("recent")
    }.distinct()
    Box(modifier = modifier) {
        FoodImage(image, Modifier.fillMaxSize())
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(horizontal = 1.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                tags.forEach { tag ->
                    val (label, color) = systemTagPresentation(tag)
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(4.dp), color = color) {
                        TextOverflowText(text = label, modifier = Modifier.fillMaxWidth().padding(horizontal = 1.dp), style = TextStyle(fontSize = 9.sp), color = androidx.compose.ui.graphics.Color.White, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
internal fun NutritionScreen(viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when {
        state.editor != null -> NutritionEditorScreen(requireNotNull(state.editor), viewModel)
        state.comparisonReturnTarget != null -> FoodComparisonScreen(requireNotNull(state.selectedFood), state.foods, viewModel.nutrientMetas(), viewModel::resolvePer100g, viewModel::closeComparison)
        state.selectedFood != null -> FoodDetailScreen(
            food = requireNotNull(state.selectedFood),
            isFavorite = requireNotNull(state.selectedFood).id in state.favoriteFoodIds,
            isRecent = requireNotNull(state.selectedFood).id in state.recentFoodIds,
            viewModel = viewModel,
            onBack = viewModel::closeFood,
        ) { viewModel.openComparison(NutritionDestination.FoodDetail) }
        else -> FoodBrowseScreen(state, viewModel, modifier)
    }
}

@Composable
private fun FoodBrowseScreen(state: NutritionUiState, viewModel: NutritionViewModel, modifier: Modifier) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    val foods = viewModel.filteredFoods(language)
    var addingTag by remember { mutableStateOf(false) }
    var tagsExpanded by remember { mutableStateOf(true) }
    var categoriesExpanded by remember { mutableStateOf(true) }
    var searchFocused by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val showSidebar = state.selectedKind != FoodKind.DISH
    val showSearchActivity = searchFocused && state.keyword.isBlank() &&
        (state.searchHistory.isNotEmpty() || state.recentFoodIds.isNotEmpty())
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val contentModifier = if (showSearchActivity) {
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)
    } else {
        modifier.fillMaxSize().padding(10.dp)
    }
    Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FoodSearchField(state.keyword, viewModel::setKeyword, stringResource(R.string.nutrition_search_food), onSearch = { }, onFocusChanged = { searchFocused = it })
        if (showSearchActivity) {
            val recent = state.recentFoodIds.mapNotNull { id -> viewModel.foodById(id) }.map { food ->
                RecentSearchItem(food.id, food.displayName(language), when (food.kind) { FoodKind.INGREDIENT -> R.drawable.ic_food_ingredient; FoodKind.FOOD -> R.drawable.ic_nav_nutrition; FoodKind.DISH -> R.drawable.ic_food_dish })
            }
            SearchActivityPanel(state.searchHistory, recent, stringResource(R.string.nutrition_search_history), stringResource(R.string.nutrition_click_history), { viewModel.setKeyword(it) }, viewModel::removeSearchHistory, viewModel::clearSearchHistory, { item -> viewModel.foodById(item.id)?.let(viewModel::openFood) }, { item -> viewModel.removeRecentFood(item.id) }, viewModel::clearRecentFoods, { searchFocused = false; focusManager.clearFocus() })
        }
        KindSegmenter(state.selectedKind, viewModel::selectKind)
        val browseAreaModifier = if (showSearchActivity) Modifier.height(screenHeight) else Modifier.weight(1f)
        Column(modifier = browseAreaModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                tagsExpanded && showSidebar && categoriesExpanded -> {
                    // 2 x 2: controls / tags on top, categories / cards below.
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        BrowseFilterControls(state.selectedKind, true, true, { tagsExpanded = !tagsExpanded }, { categoriesExpanded = !categoriesExpanded }, Modifier.width(80.dp).height(88.dp)) { viewModel.openEditor(state.selectedKind) }
                        TagFilters(state, viewModel, { addingTag = true }, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CategorySidebar(state, viewModel, Modifier.width(80.dp))
                        FoodResults(foods, language, viewModel, Modifier.weight(1f))
                    }
                }
                tagsExpanded -> {
                    // Category filters are collapsed: cards span the entire lower row.
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        BrowseFilterControls(state.selectedKind, true, false, { tagsExpanded = !tagsExpanded }, { categoriesExpanded = !categoriesExpanded }, Modifier.width(80.dp).height(88.dp)) { viewModel.openEditor(state.selectedKind) }
                        TagFilters(state, viewModel, { addingTag = true }, Modifier.weight(1f))
                    }
                    FoodResults(foods, language, viewModel, Modifier.weight(1f))
                }
                showSidebar && categoriesExpanded -> {
                    // Tag filters are collapsed: cards span the right-hand column.
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.width(80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BrowseFilterControls(state.selectedKind, false, true, { tagsExpanded = !tagsExpanded }, { categoriesExpanded = !categoriesExpanded }, Modifier.fillMaxWidth().height(88.dp)) { viewModel.openEditor(state.selectedKind) }
                            CategorySidebar(state, viewModel, Modifier.weight(1f))
                        }
                        FoodResults(foods, language, viewModel, Modifier.weight(1f))
                    }
                }
                else -> {
                    // Both filter areas are collapsed: only the flat control row remains above cards.
                    BrowseFilterControls(state.selectedKind, false, false, { tagsExpanded = !tagsExpanded }, { categoriesExpanded = !categoriesExpanded }, Modifier.fillMaxWidth()) { viewModel.openEditor(state.selectedKind) }
                    FoodResults(foods, language, viewModel, Modifier.weight(1f))
                }
            }
        }
    }
    if (addingTag) AddTagDialog({ addingTag = false }) { viewModel.addUserTag(it); addingTag = false }
}

@Composable
private fun BrowseFilterControls(
    kind: FoodKind,
    tagsExpanded: Boolean,
    categoriesExpanded: Boolean,
    onToggleTags: () -> Unit,
    onToggleCategories: () -> Unit,
    modifier: Modifier = Modifier,
    onAddCustom: () -> Unit,
) {
    val showCategories = kind != FoodKind.DISH
    val flat = !tagsExpanded && !categoriesExpanded
    if (flat) {
        Row(modifier = modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AddCustomButton(kind, Modifier.weight(1f), onAddCustom)
            FilterCollapseButton(tagsExpanded, FilterCollapseAxis.Vertical, stringResource(R.string.nutrition_collapse_tags), stringResource(R.string.nutrition_expand_tags), onToggleTags, Modifier.weight(1f))
            if (showCategories) FilterCollapseButton(categoriesExpanded, FilterCollapseAxis.Horizontal, stringResource(R.string.nutrition_collapse_categories), stringResource(R.string.nutrition_expand_categories), onToggleCategories, Modifier.weight(1f))
        }
    } else {
        val buttonCount = if (showCategories) 3 else 2
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AddCustomButton(kind, Modifier.weight(1f / buttonCount), onAddCustom)
            FilterCollapseButton(tagsExpanded, FilterCollapseAxis.Vertical, stringResource(R.string.nutrition_collapse_tags), stringResource(R.string.nutrition_expand_tags), onToggleTags, Modifier.weight(1f / buttonCount))
            if (showCategories) FilterCollapseButton(categoriesExpanded, FilterCollapseAxis.Horizontal, stringResource(R.string.nutrition_collapse_categories), stringResource(R.string.nutrition_expand_categories), onToggleCategories, Modifier.weight(1f / buttonCount))
        }
    }
}

@Composable
private fun TagFilters(state: NutritionUiState, viewModel: NutritionViewModel, onAddTag: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TagRow(stringResource(R.string.nutrition_system_tags), listOf("common" to stringResource(R.string.nutrition_tag_common), "favorite" to stringResource(R.string.nutrition_tag_favorite), "recent" to stringResource(R.string.nutrition_tag_recent)), state.selectedSystemTags, viewModel::toggleSystemTag)
        TagRow(stringResource(R.string.nutrition_user_tags), state.userTags.map { it.id to it.label }, state.selectedUserTags, viewModel::toggleUserTag, onAddTag)
    }
}

@Composable
private fun FoodResults(foods: List<FoodItem>, language: String, viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    if (foods.isEmpty()) Text(stringResource(R.string.nutrition_no_foods), modifier = modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    else LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) { items(foods, key = { it.id }) { FoodRow(it, language, viewModel, viewModel::openFood) } }
}

@Composable
private fun KindSegmenter(selected: FoodKind, onSelected: (FoodKind) -> Unit) {
    val kinds = listOf(
        FoodKind.INGREDIENT to R.string.nutrition_kind_ingredient,
        FoodKind.FOOD to R.string.nutrition_kind_food,
        FoodKind.DISH to R.string.nutrition_kind_dish,
    )
    EqualWidthSegmentedTabs(
        tabs = kinds.map { (kind, label) -> EqualWidthTab(label, when (kind) { FoodKind.INGREDIENT -> R.drawable.ic_food_ingredient; FoodKind.FOOD -> R.drawable.ic_nav_nutrition; FoodKind.DISH -> R.drawable.ic_food_dish }) },
        selectedIndex = kinds.indexOfFirst { it.first == selected }.coerceAtLeast(0),
        onSelected = { onSelected(kinds[it].first) },
    )
}

/** Kind-colored「添加自定义XX」按钮，与左列同宽。 */
@Composable
private fun AddCustomButton(kind: FoodKind, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = kind.nameColors()
    Surface(
        modifier = modifier.fillMaxWidth().fillMaxHeight().clickable(onClick = onClick),
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
    val children = state.selectedRoots.flatMap(viewModel::categoryChildren)
    Surface(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        if (children.isEmpty()) {
            CategoryRootList(state, viewModel, Modifier.fillMaxWidth().padding(2.dp))
        } else {
            Row(Modifier.fillMaxSize().padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                CategoryRootList(state, viewModel, Modifier.width(39.dp))
                CategoryChildList(
                    children = children,
                    rootCount = viewModel.categoryRoots().size,
                    modifier = Modifier.width(35.dp).fillMaxHeight(),
                ) { category ->
                    CompactFilterChip(category.tag in state.selectedChildren, { viewModel.toggleChild(category.tag) }, Modifier.fillMaxWidth()) {
                        CategoryLabel(stringResource(category.labelRes))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRootList(state: NutritionUiState, viewModel: NutritionViewModel, modifier: Modifier) {
    val colors = state.selectedKind.nameColors()
    val roots = viewModel.categoryRoots()
    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val gap = categoryGap(maxHeight - 28.dp - 12.dp, roots.size)
        Column(Modifier.fillMaxHeight()) {
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
        // Keep the custom category visually distinct from the static category tree.
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
            roots.forEach { category ->
                CompactFilterChip(category.tag in state.selectedRoots, { viewModel.toggleRoot(category.tag) }, Modifier.fillMaxWidth()) { CategoryLabel(stringResource(category.labelRes)) }
            }
        }
    }
    }
}

@Composable
private fun CategoryChildList(
    children: List<com.woshiwangnima.healthdietpro.model.food.FoodCategory>,
    rootCount: Int,
    modifier: Modifier,
    content: @Composable (com.woshiwangnima.healthdietpro.model.food.FoodCategory) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val gap = categoryGap(maxHeight - 28.dp - 12.dp, rootCount)
        Column(
            modifier = Modifier.fillMaxHeight().padding(top = 28.dp + 12.dp + gap),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            children.forEach { category -> content(category) }
        }
    }
}

private fun categoryGap(height: androidx.compose.ui.unit.Dp, itemCount: Int): androidx.compose.ui.unit.Dp {
    if (itemCount <= 0) return 0.dp
    return ((height - (itemCount * 28).dp) / (itemCount + 1)).coerceAtLeast(0.dp)
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
private fun FoodCardNameHeader(
    food: FoodItem,
    language: String,
    isCustom: Boolean,
    cookingSuffix: String?,
    modifier: Modifier = Modifier,
) {
    val colors = food.kind.nameColors()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isCustom) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.nutrition_custom_marker),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(2.dp))
        }
        Surface(modifier = Modifier.fillMaxWidth(0.8f).height(28.dp), shape = RoundedCornerShape(6.dp), color = colors.first) {
            Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(0.6f)) {
                    TextOverflowText(
                        text = food.displayName(language),
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(fontSize = FontTokens.subtitle),
                        color = colors.second,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
                cookingSuffix?.let { Text(" ($it)", style = TextStyle(fontSize = FontTokens.subtitle), color = colors.second, maxLines = 1, softWrap = false) }
                val aliases = food.allNames(language).drop(1).joinToString(" / ")
                if (aliases.isNotEmpty()) {
                    TextOverflowText(
                        text = aliases,
                        modifier = Modifier.weight(0.4f),
                        style = TextStyle(fontSize = FontTokens.body),
                        color = colors.second.copy(alpha = 0.9f),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodRow(
    food: FoodItem,
    language: String,
    viewModel: NutritionViewModel,
    onClick: (FoodItem) -> Unit,
) {
    val resolved = remember(food.id) { runCatching { viewModel.resolvePer100g(food) }.getOrNull() }
    val energy = resolved?.nutrients?.get("ENERGY")?.value ?: 0.0
    val image = viewModel.foodImages.image(food.image?.localKey)
    val categoryLabels = mutableListOf<String>()
    for (tag in food.categoryTagsOrEmpty()) {
        val pathLabels = mutableListOf<String>()
        for (labelRes in viewModel.categoryDisplayPath(tag)) {
            pathLabels += stringResource(labelRes)
        }
        if (pathLabels.isNotEmpty()) {
            categoryLabels += pathLabels.joinToString(".")
        }
    }
    val cookingSuffix: String? = (food as? PreparedFood)?.let {
        it.derivedFrom?.let { derivation ->
            viewModel.cookingMethodFor(derivation.cookingMethodId)?.displayLabel(language)
        } ?: it.techniqueId?.let { techniqueId ->
            viewModel.cookingMethodFor(techniqueId)?.displayLabel(language)
        }
    }
    val secondaryLine: String? = when (food) {
        is Dish -> stringResource(R.string.nutrition_dish_components) + ": " + food.components.size
        is PreparedFood -> food.components.takeIf { it.isNotEmpty() }?.let {
            stringResource(R.string.nutrition_dish_components) + ": " + it.size
        }
        else -> null
    }
    val glycemic = glycemicLevel(
        resolved?.healthMetrics?.glycemicIndex?.value ?: food.healthMetrics.glycemicIndex?.value,
        resolved?.healthMetrics?.glycemicLoadPer100g?.value ?: food.healthMetrics.glycemicLoadPer100g?.value,
    )
    var previewing by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(food) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodImageWithSystemTags(
            food = food,
            image = image,
            isFavorite = viewModel.isFavorite(food.id),
            isRecent = food.id in viewModel.state.value.recentFoodIds,
            modifier = Modifier.size(64.dp).clickable { previewing = true },
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            FoodCardNameHeader(
                food = food,
                language = language,
                isCustom = viewModel.isCustom(food.id),
                cookingSuffix = cookingSuffix,
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
        GlycemicGlass(glycemic)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (previewing) FoodImagePreview(image, onDismiss = { previewing = false })
}

@Composable
private fun GlycemicGlass(level: com.woshiwangnima.healthdietpro.model.food.GlycemicLevel?) {
    val color = when (level?.classification) {
        GlycemicClassification.Low -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        GlycemicClassification.Medium -> androidx.compose.ui.graphics.Color(0xFFF9A825)
        GlycemicClassification.High -> androidx.compose.ui.graphics.Color(0xFFC62828)
        null -> MaterialTheme.colorScheme.outlineVariant
    }
    val background = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.width(20.dp).height(30.dp), contentAlignment = Alignment.Center) {
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val path = glycemicDropPath(size.width, size.height)
        drawPath(path, color = background)
        if (level != null) {
            clipPath(path) {
                drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * (1f - level.fillPercent)), size = androidx.compose.ui.geometry.Size(size.width, size.height * level.fillPercent))
            }
        }
        drawPath(path, color = color, style = Stroke(1.5f))
    }
    if (level == null) Text("?", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Samples the requested implicit curve in polar form.
 *
 * (x^2 + y^2)^2 - 2x(x^2 + y^2) + 3y^2 = 0
 * becomes r = cos(t) +/- sqrt(4cos(t)^2 - 3). The positive lobe is
 * rotated 90 degrees so its cusp points upward in the device coordinate system.
 */
private fun glycemicDropPath(width: Float, height: Float): androidx.compose.ui.graphics.Path {
    val halfAngle = PI / 6.0
    val samples = 96
    val mathematicalPoints = buildList {
        fun addPolarPoint(angle: Double, outer: Boolean) {
            val cosine = cos(angle)
            val discriminant = (4.0 * cosine * cosine - 3.0).coerceAtLeast(0.0)
            val radius = cosine + if (outer) sqrt(discriminant) else -sqrt(discriminant)
            val originalX = radius * cosine
            val originalY = radius * sin(angle)
            // Rotate the lobe so the cusp is on the mathematical positive Y axis.
            add(androidx.compose.ui.geometry.Offset((-originalY).toFloat(), originalX.toFloat()))
        }

        for (index in 0..samples) {
            val angle = -halfAngle + 2.0 * halfAngle * index / samples
            addPolarPoint(angle, outer = true)
        }
        for (index in samples downTo 0) {
            val angle = -halfAngle + 2.0 * halfAngle * index / samples
            addPolarPoint(angle, outer = false)
        }
    }
    val minX = mathematicalPoints.minOf { it.x }
    val maxX = mathematicalPoints.maxOf { it.x }
    val minY = mathematicalPoints.minOf { it.y }
    val maxY = mathematicalPoints.maxOf { it.y }
    val scale = minOf(width * 0.86f / (maxX - minX), height * 0.86f / (maxY - minY))
    val middleX = (minX + maxX) / 2f
    val middleY = (minY + maxY) / 2f
    val path = androidx.compose.ui.graphics.Path()
    mathematicalPoints.forEachIndexed { index, point ->
        val px = width / 2f + (point.x - middleX) * scale
        // Keep the mathematical positive Y direction when mapping the droplet to the card.
        val py = height / 2f + (point.y - middleY) * scale
        if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

@Composable
private fun FoodDetailScreen(
    food: FoodItem,
    isFavorite: Boolean,
    isRecent: Boolean,
    viewModel: NutritionViewModel,
    onBack: () -> Unit,
    onCompare: () -> Unit,
) {
    val imageStore = viewModel.foodImages
    var tab by remember { mutableIntStateOf(0) }
    var headerPage by remember(food.id) { mutableIntStateOf(0) }
    var headerDirection by remember(food.id) { mutableIntStateOf(1) }
    val resolved = remember(food.id) { runCatching { viewModel.resolvePer100g(food) }.getOrNull() }
    val servings = remember(food.id, resolved) { food.defaultServings(resolved) }
    var selectedServingId by remember(food.id, servings) { mutableStateOf(servings.first().id) }
    var previewing by remember { mutableStateOf(false) }
    var showHealthMetricsHelp by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val relatedDishes = remember(food.id) { viewModel.relatedDishes(food.id) }
    val isCustom = viewModel.isCustom(food.id)
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    val cookingSuffix: String? = (food as? PreparedFood)?.let {
        it.derivedFrom?.let { derivation ->
            viewModel.cookingMethodFor(derivation.cookingMethodId)?.displayLabel(language)
        } ?: it.techniqueId?.let { techniqueId ->
            viewModel.cookingMethodFor(techniqueId)?.displayLabel(language)
        }
    }
    BaseScreen(
        title = stringResource(food.kind.detailTitleRes()),
        onBack = onBack,
        includeStatusBarPadding = false,
    ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
        Row(modifier = Modifier.height(96.dp), verticalAlignment = Alignment.Top) {
            FoodImageWithSystemTags(
                food = food,
                image = imageStore.image(food.image?.localKey),
                isFavorite = isFavorite,
                isRecent = isRecent,
                modifier = Modifier.size(96.dp).clickable { previewing = true },
            )
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f).fillMaxHeight()) {
                FoodCardNameHeader(
                    food = food,
                    language = language,
                    isCustom = isCustom,
                    cookingSuffix = cookingSuffix,
                )
                FoodHeaderPages(
                    food = food,
                    viewModel = viewModel,
                    language = language,
                    page = headerPage,
                    direction = headerDirection,
                    onPrevious = {
                        headerDirection = -1
                        headerPage = if (headerPage == 0) 2 else headerPage - 1
                    },
                    onNext = {
                        headerDirection = 1
                        headerPage = (headerPage + 1) % 3
                    },
                    modifier = Modifier.weight(1f),
                ) { viewModel.openFood(it) }
            }
            Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                if (isCustom) {
                    Row {
                        IconButton(onClick = { viewModel.openEditor(food.kind, food.id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.nutrition_editor_edit)) }
                        IconButton(onClick = { confirmingDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.nutrition_editor_delete)) }
                    }
                }
                Row {
                    IconButton(onClick = { viewModel.toggleFavorite(food) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.nutrition_favorite),
                            tint = if (isFavorite) androidx.compose.ui.graphics.Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                        IconButton(onClick = onCompare) {
                            Icon(painterResource(R.drawable.ic_vs), stringResource(R.string.nutrition_compare))
                        }
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
            MacronutrientEnergyChart(resolved, Modifier.padding(top = 12.dp))
            DetailSectionTitle(R.drawable.ic_health_metrics, stringResource(R.string.nutrition_health_metrics)) {
                IconButton(onClick = { showHealthMetricsHelp = true }) {
                    Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.nutrition_health_metrics_help))
                }
            }
            HealthMetricsTable(resolved?.healthMetrics ?: food.healthMetrics, stringResource(R.string.nutrition_metric_no_data))
            DetailSectionTitle(R.drawable.ic_nutrients, stringResource(R.string.nutrition_nutrients), Modifier.padding(top = 12.dp))
            FoodServingSelector(
                servings = servings,
                selectedServingId = selectedServingId,
                onSelected = { selectedServingId = it },
            )
            val selectedServing = servings.first { it.id == selectedServingId }
            AppDataTable(
                rows = nutrientRows(resolved, selectedServing, viewModel.nutrientMetas(), language),
                columns = listOf(
                    AppDataTableColumn("nutrient", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_item)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(it.labelText ?: stringResource(requireNotNull(it.labelRes))) },
                    AppDataTableColumn("amount", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_amount)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(it.value) },
                ),
                rowKey = { _, row -> row.key },
                showRowNumber = false,
                showPager = false,
                modifier = Modifier.fillMaxWidth().height(240.dp),
            )
            if (relatedDishes.isNotEmpty()) {
                DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_related_dishes), Modifier.padding(top = 12.dp))
                relatedDishes.forEach { dish -> FoodNavigationLink(dish.displayName(language)) { viewModel.openFood(dish) } }
            }
        }
        else if (tab == 1 && food is Dish) {
            Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState())) {
                DishRecipeSection(food, viewModel, language) { viewModel.openFood(it) }
            }
        } else if (tab == 1 && food is PreparedFood && food.components.isNotEmpty()) {
            Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState())) {
                FoodRecipeSection(food, viewModel, language) { viewModel.openFood(it) }
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
private fun FoodHeaderPages(
    food: FoodItem,
    viewModel: NutritionViewModel,
    language: String,
    page: Int,
    direction: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier,
    onOpenFood: (FoodItem) -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.width(24.dp).clickable(onClick = onPrevious),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                stringResource(R.string.nutrition_header_previous),
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedPageContent(page, Modifier.weight(1f), direction = { _, _ -> direction }) { currentPage ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                val textStyle = MaterialTheme.typography.bodySmall
                if (currentPage == 0) {
                    Text(
                        food.displayDescription(language).ifBlank { stringResource(R.string.nutrition_header_introduction_empty) },
                        style = textStyle,
                    )
                } else if (currentPage == 1) {
                    KindInfoSection(food, viewModel, language, onOpenFood, textStyle)
                } else {
                    FoodSourcesSection(food, textStyle)
                }
            }
        }
        Box(
            modifier = Modifier.width(24.dp).clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(R.string.nutrition_header_next),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FoodSourcesSection(food: FoodItem, textStyle: androidx.compose.ui.text.TextStyle) {
    if (food.sources.isEmpty()) {
        Text(
            stringResource(R.string.nutrition_sources_empty),
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        food.sources.forEach { source -> SourceLink(source.dataset, source.reference, textStyle) }
    }
}

private data class MacronutrientEnergy(
    val carbohydrateKcal: Double,
    val proteinKcal: Double,
    val fatKcal: Double,
) {
    val totalKcal: Double get() = carbohydrateKcal + proteinKcal + fatKcal
}

private fun ResolvedNutrition?.macronutrientEnergy(): MacronutrientEnergy? {
    val nutrients = this?.nutrients ?: return null
    return MacronutrientEnergy(
        carbohydrateKcal = (nutrients["CHO"]?.value ?: 0.0) * 4.0,
        proteinKcal = (nutrients["PROTEIN"]?.value ?: 0.0) * 4.0,
        fatKcal = (nutrients["FAT"]?.value ?: 0.0) * 9.0,
    ).takeIf { it.totalKcal > 0.0 }
}

@Composable
private fun MacronutrientEnergyChart(resolved: ResolvedNutrition?, modifier: Modifier = Modifier) {
    val energy = resolved.macronutrientEnergy() ?: return
    DetailSectionTitle(R.drawable.ic_energy_distribution, stringResource(R.string.nutrition_macronutrient_energy), modifier)
    AnimatedDonutChart(
        segments = listOf(
            DonutChartSegment("carbohydrate", stringResource(R.string.nutrition_energy_carbohydrate, energy.carbohydrateKcal / energy.totalKcal * 100.0), energy.carbohydrateKcal.toFloat(), androidx.compose.ui.graphics.Color(0xFFF9A825)),
            DonutChartSegment("protein", stringResource(R.string.nutrition_energy_protein, energy.proteinKcal / energy.totalKcal * 100.0), energy.proteinKcal.toFloat(), androidx.compose.ui.graphics.Color(0xFF43A047)),
            DonutChartSegment("fat", stringResource(R.string.nutrition_energy_fat, energy.fatKcal / energy.totalKcal * 100.0), energy.fatKcal.toFloat(), androidx.compose.ui.graphics.Color(0xFFE53935)),
        ),
        centerValue = stringResource(R.string.nutrition_energy_kcal_value, energy.totalKcal),
        centerLabel = stringResource(R.string.nutrition_macronutrient_energy_center),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun KindInfoSection(
    food: FoodItem,
    viewModel: NutritionViewModel,
    language: String,
    onOpenFood: (FoodItem) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    food.botanicalTaxonomy?.let { taxonomy ->
        val labels = viewModel.botanicalTaxonomy()
        InfoLine(stringResource(R.string.nutrition_botanical_family), labels.familyName(taxonomy.family, language), textStyle)
        InfoLine(stringResource(R.string.nutrition_botanical_genus), labels.genusName(taxonomy.genus, language), textStyle)
        taxonomy.species?.let { InfoLine(stringResource(R.string.nutrition_botanical_species), it, textStyle) }
    }
    when (food) {
        is Ingredient -> food.edibleRatio?.let { ratio ->
            InfoLine(stringResource(R.string.nutrition_edible_ratio), stringResource(R.string.nutrition_edible_ratio_value, (ratio * 100).toInt()), textStyle)
        }
        is PreparedFood -> {
            food.derivedFrom?.let { derivation ->
                val source = viewModel.foodById(derivation.ingredientId)
                val method = viewModel.cookingMethodFor(derivation.cookingMethodId)
                source?.let { src ->
                    IngredientJumpLine(stringResource(R.string.nutrition_derived_from), src.displayName(language), textStyle) { onOpenFood(src) }
                }
                method?.let { InfoLine(stringResource(R.string.nutrition_cooking_method), it.displayLabel(language), textStyle) }
            }
            food.techniqueId?.let { techniqueId ->
                viewModel.cookingMethodFor(techniqueId)?.let { method ->
                    InfoLine(stringResource(R.string.nutrition_cooking_method), method.displayLabel(language), textStyle)
                }
            }
        }
        is Dish -> DishInfoSection(food, viewModel, language, onOpenFood, textStyle)
    }
}

@Composable
private fun DishInfoSection(
    dish: Dish,
    viewModel: NutritionViewModel,
    language: String,
    onOpenFood: (FoodItem) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    // 营养档案仅呈现描述菜肴本身的基础分类信息。
    dish.cuisine?.let { DishTaxonomy.labelRes(it)?.let { res -> InfoLine(stringResource(R.string.nutrition_editor_cuisine), stringResource(res), textStyle) } }
    dish.dishCategories.mapNotNull { DishTaxonomy.labelRes(it) }.map { stringResource(it) }.takeIf { it.isNotEmpty() }?.let { labels ->
        InfoLine(stringResource(R.string.nutrition_editor_dish_category), labels.joinToString(" / "), textStyle)
    }
    dish.tastes.mapNotNull { DishTaxonomy.labelRes(it) }.map { stringResource(it) }.takeIf { it.isNotEmpty() }?.let { labels ->
        InfoLine(stringResource(R.string.nutrition_editor_taste), labels.joinToString(" / "), textStyle)
    }
    dish.seasons.mapNotNull { DishTaxonomy.labelRes(it) }.map { stringResource(it) }.takeIf { it.isNotEmpty() }?.let { labels ->
        InfoLine(stringResource(R.string.nutrition_editor_season), labels.joinToString(" / "), textStyle)
    }
}

/** 菜肴的食材清单与菜谱在「制作步骤」页签中呈现。 */
@Composable
private fun DishRecipeSection(dish: Dish, viewModel: NutritionViewModel, language: String, onOpenFood: (FoodItem) -> Unit) {
    DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_recipe_basic_information))
    dish.techniqueId?.let { viewModel.cookingMethodFor(it)?.let { method -> InfoLine(stringResource(R.string.nutrition_editor_technique), method.displayLabel(language)) } }
    dish.difficulty?.let { StarRatingLine(stringResource(R.string.nutrition_dish_difficulty), it) }
    dish.servesPeople?.let { InfoLine(stringResource(R.string.nutrition_dish_serves), stringResource(R.string.nutrition_dish_serves_value, it)) }
    // 食材清单：辅料（调味品或油脂）判定，主料在前、辅料在后。
    DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_ingredient_list), Modifier.padding(top = 8.dp))
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

@Composable
private fun FoodRecipeSection(food: PreparedFood, viewModel: NutritionViewModel, language: String, onOpenFood: (FoodItem) -> Unit) {
    food.techniqueId?.let { techniqueId ->
        viewModel.cookingMethodFor(techniqueId)?.let { method ->
            InfoLine(stringResource(R.string.nutrition_editor_technique), method.displayLabel(language))
        }
    }
    food.servesPeople?.let { InfoLine(stringResource(R.string.nutrition_dish_serves), stringResource(R.string.nutrition_dish_serves_value, it)) }
    DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_ingredient_list), Modifier.padding(top = 8.dp))
    val (auxiliary, main) = food.components.partition { component ->
        viewModel.foodById(component.foodId)?.let(viewModel::isAuxiliary) == true
    }
    main.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
    if (auxiliary.isNotEmpty()) {
        Text(stringResource(R.string.nutrition_ingredient_auxiliary), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
        auxiliary.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
    }
    if (food.recipeSteps.isNotEmpty()) {
        DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_recipe_title), Modifier.padding(top = 8.dp))
        food.recipeSteps.forEachIndexed { index, step -> RecipeStepRow(index + 1, step) }
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
            FoodNavigationLink(item.displayName(language), Modifier.weight(1f)) { onOpenFood(item) }
            Text(amount, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(component.foodId, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(amount, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 可跳转的来源行（食物→来源食材）。 */
@Composable
private fun IngredientJumpLine(
    label: String,
    value: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        FoodNavigationLink(value, Modifier.weight(1f), textStyle, onClick)
    }
}

@Composable
private fun FoodNavigationLink(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    onClick: () -> Unit,
) {
    TextOverflowText(
        text = text,
        modifier = modifier.clickable(onClick = onClick),
        style = textStyle.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
    )
}

@Composable
private fun SourceLink(dataset: String, reference: String, textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        TextOverflowText(dataset, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextOverflowText(
            reference,
            modifier = Modifier.clickable {
                val query = android.net.Uri.encode("$dataset $reference")
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$query")))
            },
            style = textStyle.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
            color = MaterialTheme.colorScheme.primary,
        )
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
        step.heatLevel?.let { Text(stringResource(heatLabelRes(it)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        step.waterTemperatureC?.let { Text(stringResource(R.string.nutrition_water_temperature_value, it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        step.oilTemperatureC?.let { Text(stringResource(R.string.nutrition_oil_temperature_value, it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        step.minutes?.let { StepTimer(it) }
    }
}

@StringRes
private fun heatLabelRes(id: String): Int = when (id) {
    "low" -> R.string.nutrition_heat_low
    "medium_low" -> R.string.nutrition_heat_medium_low
    "medium" -> R.string.nutrition_heat_medium
    "medium_high" -> R.string.nutrition_heat_medium_high
    else -> R.string.nutrition_heat_high
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
private fun InfoLine(label: String, value: String, textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(value, style = textStyle)
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
    @param:StringRes val labelRes: Int?,
    val value: String,
    val classification: GlycemicClassification? = null,
    val labelText: String? = null,
)

@Composable
private fun HealthMetricsTable(
    metrics: com.woshiwangnima.healthdietpro.model.food.FoodHealthMetrics,
    noData: String,
) {
    val gi = metrics.glycemicIndex
    val gl = metrics.glycemicLoadPer100g
    val border = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier.fillMaxWidth().height(184.dp),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HealthMetricHeader(stringResource(R.string.nutrition_profile_item), Modifier.weight(1.3f))
                HealthMetricHeader(stringResource(R.string.nutrition_profile_amount), Modifier.weight(.9f))
                HealthMetricHeader(stringResource(R.string.nutrition_profile_classification), Modifier.weight(.5f))
                HealthMetricHeader(stringResource(R.string.nutrition_metric_glycemic_icon), Modifier.width(64.dp))
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.weight(1f)) {
                    HealthMetricDataRow(stringResource(R.string.nutrition_metric_gi), gi?.let { "${it.value.formatTableValue()} ${it.unit}" } ?: noData, gi?.let { classifyGlycemicIndex(it.value) }, border)
                    HealthMetricDataRow(stringResource(R.string.nutrition_metric_gl), gl?.let { "${it.value.formatTableValue()} ${it.unit}" } ?: noData, gl?.let { classifyGlycemicLoad(it.value) }, border)
                    HealthMetricDataRow(stringResource(R.string.nutrition_metric_inflammatory_potential), metrics.inflammatoryPotential?.let { "${it.value.formatTableValue()} ${it.unit}" } ?: noData, null, border)
                }
                Column(Modifier.width(64.dp)) {
                    Box(Modifier.height(96.dp).fillMaxWidth().border(1.dp, border), contentAlignment = Alignment.Center) {
                        GlycemicGlass(glycemicLevel(gi?.value, gl?.value))
                    }
                    Box(Modifier.height(48.dp).fillMaxWidth().border(1.dp, border))
                }
            }
        }
    }
}

@Composable
private fun HealthMetricHeader(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun HealthMetricDataRow(label: String, value: String, classification: GlycemicClassification?, border: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, border), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1.3f).padding(horizontal = 12.dp), style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(.9f).padding(horizontal = 6.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 1)
        Box(Modifier.weight(.5f), contentAlignment = Alignment.Center) {
            classification?.let { GlycemicClassificationText(it) }
        }
    }
}

@Composable
private fun HealthMetricInfoSection(
    title: String,
    description: String,
    bands: List<RangeBand<Double, GlycemicClassification>>? = null,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, modifier = Modifier.padding(top = 2.dp))
        bands?.let { MetricClassificationTable(it, Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun MetricClassificationTable(
    bands: List<RangeBand<Double, GlycemicClassification>>,
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
private fun metricRangeText(band: RangeBand<Double, GlycemicClassification>): String {
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

private fun nutrientRows(
    resolved: ResolvedNutrition?,
    serving: FoodServing,
    nutrientMetas: List<com.woshiwangnima.healthdietpro.model.food.NutrientMeta>,
    language: String,
): List<FoodProfileRow> {
    val nutrients = resolved?.nutrients ?: return emptyList()
    val multiplier = serving.ratioToTable
    val metaByCode = nutrientMetas.associateBy { it.code }
    return nutrients.entries
        .sortedBy { (code, _) -> nutrientMetas.indexOf(metaByCode[code]).let { if (it < 0) Int.MAX_VALUE else it } }
        .map { (code, amount) ->
            FoodProfileRow(
                key = code,
                labelRes = null,
                value = "${(amount.value * multiplier).formatTableValue()} ${amount.unitId}",
                labelText = metaByCode[code]?.displayName(language) ?: code,
            )
        }
}

private fun Double.formatTableValue(): String = "%.1f".format(java.util.Locale.ROOT, this)

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
private fun FoodComparisonScreen(
    food: FoodItem,
    foods: List<FoodItem>,
    nutrientMetas: List<com.woshiwangnima.healthdietpro.model.food.NutrientMeta>,
    resolve: (FoodItem) -> ResolvedNutrition,
    onBack: () -> Unit,
) {
    var opponentId by remember(food.id) { mutableStateOf<String?>(null) }
    var kindFilter by remember(food.id) { mutableStateOf<FoodKind?>(null) }
    var query by remember(food.id) { mutableStateOf("") }
    val opponents = remember(food.id, foods, kindFilter, query) {
        foods.filter { candidate ->
            candidate.id != food.id &&
                (kindFilter == null || candidate.kind == kindFilter) &&
                candidate.searchableNames().any { it.contains(query, ignoreCase = true) }
        }
    }
    val opponent = opponents.firstOrNull { it.id == opponentId }
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    BaseScreen(title = stringResource(food.kind.comparisonTitleRes()), onBack = onBack, includeStatusBarPadding = false) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(food.displayName(language), style = MaterialTheme.typography.titleMedium)
            EqualWidthSegmentedTabs(
                tabs = listOf<FoodKind?>(null, FoodKind.INGREDIENT, FoodKind.FOOD, FoodKind.DISH).map { kind ->
                    EqualWidthTab.text(if (kind == null) stringResource(R.string.nutrition_compare_all) else stringResource(kind.detailTitleRes()))
                },
                selectedIndex = listOf<FoodKind?>(null, FoodKind.INGREDIENT, FoodKind.FOOD, FoodKind.DISH).indexOf(kindFilter),
                onSelected = { kindFilter = listOf<FoodKind?>(null, FoodKind.INGREDIENT, FoodKind.FOOD, FoodKind.DISH)[it] },
            )
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.nutrition_compare_search)) })
            Text(stringResource(R.string.nutrition_compare_candidates), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppDataTable(
                rows = opponents,
                rowKey = { _, candidate -> candidate.id },
                showRowNumber = false,
                showPager = false,
                modifier = Modifier.height(150.dp),
                columns = listOf(
                    AppDataTableColumn("name", { AppDataTableHeaderText(stringResource(R.string.nutrition_compare_target)) }, ColumnWidth.Flex(1f, 140.dp)) { candidate ->
                        AppDataTableText(candidate.displayName(language))
                    },
                    AppDataTableColumn("kind", { AppDataTableHeaderText(stringResource(R.string.nutrition_compare_type)) }, ColumnWidth.Fixed(88.dp)) { candidate ->
                        AppDataTableText(stringResource(candidate.kind.detailTitleRes()))
                    },
                ),
                onRowClick = { opponentId = it.id },
            )
            if (opponent == null) {
                Text(stringResource(R.string.nutrition_compare_choose_target), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val left = runCatching { resolve(food) }.getOrNull()
                val right = runCatching { resolve(opponent) }.getOrNull()
                val metaByCode = nutrientMetas.associateBy { it.code }
                val rows = (left?.nutrients?.keys.orEmpty() + right?.nutrients?.keys.orEmpty())
                    .distinct()
                    .filter { it in metaByCode }
                    .sortedBy { nutrientMetas.indexOf(metaByCode.getValue(it)) }
                    .map { code -> ComparisonRow(metaByCode.getValue(code), left?.nutrients?.get(code), right?.nutrients?.get(code)) }
                Surface(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                    Column(Modifier.padding(6.dp)) {
                        Text(stringResource(R.string.nutrition_compare_results), style = MaterialTheme.typography.titleSmall)
                        AppDataTable(
                            rows = rows,
                            rowKey = { _, row -> row.meta.code },
                            showPager = false,
                            showRowNumber = false,
                            modifier = Modifier.weight(1f),
                            columns = listOf(
                                AppDataTableColumn("left", { AppDataTableHeaderText(food.displayName(language)) }, ColumnWidth.Flex(1f, 96.dp)) { ComparisonValue(it.left, it.right) },
                                AppDataTableColumn("metric", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_item)) }, ColumnWidth.Flex(1f, 96.dp)) { AppDataTableText(it.meta.displayName(language)) },
                                AppDataTableColumn("right", { AppDataTableHeaderText(opponent.displayName(language)) }, ColumnWidth.Flex(1f, 96.dp)) { ComparisonValue(it.right, it.left) },
                            ),
                        )
                    }
                }
            }
        }
    }
}

private data class ComparisonRow(val meta: com.woshiwangnima.healthdietpro.model.food.NutrientMeta, val left: com.woshiwangnima.healthdietpro.model.food.FoodAmount?, val right: com.woshiwangnima.healthdietpro.model.food.FoodAmount?)

@Composable
private fun ComparisonValue(value: com.woshiwangnima.healthdietpro.model.food.FoodAmount?, other: com.woshiwangnima.healthdietpro.model.food.FoodAmount?) {
    val isHigher = value != null && (other == null || value.value > other.value)
    Text(
        text = value?.let { "${it.value.formatTableValue()} ${it.unitId}" } ?: "-",
        style = if (isHigher) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        fontWeight = if (isHigher) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
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

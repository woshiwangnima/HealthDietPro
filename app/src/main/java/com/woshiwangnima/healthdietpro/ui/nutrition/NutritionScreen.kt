package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.format.formatGroupedInteger
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.FontTokens
import com.woshiwangnima.healthdietpro.common.ui.SearchActivityPanel
import com.woshiwangnima.healthdietpro.common.ui.SearchPanelDismissLayer
import com.woshiwangnima.healthdietpro.common.ui.SearchPanelMaxHeight
import com.woshiwangnima.healthdietpro.common.ui.RecentSearchItem
import com.woshiwangnima.healthdietpro.common.ui.FoodImageStore
import com.woshiwangnima.healthdietpro.common.ui.ImageVariant
import com.woshiwangnima.healthdietpro.common.ui.FilterCollapseAxis
import com.woshiwangnima.healthdietpro.common.ui.FilterCollapseButton
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.MarqueeRow
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFoodKindCurrent
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadiiDefault
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSearchField
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSheetHeight
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSpacingDefault
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.concentricRadius
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTypeDefault
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.Dish
import com.woshiwangnima.healthdietpro.model.food.DishTaxonomy
import com.woshiwangnima.healthdietpro.model.food.DriNrvRepository
import com.woshiwangnima.healthdietpro.model.food.RecipeStep
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodCardMetadata
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.FoodServing
import com.woshiwangnima.healthdietpro.model.food.GlycemicClassification
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.PreparedFood
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.model.food.NrvReference
import com.woshiwangnima.healthdietpro.util.UnitConverter
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
internal fun FoodKind.comparisonTitleRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.nutrition_comparison_title_ingredient
    FoodKind.FOOD -> R.string.nutrition_comparison_title_food
    FoodKind.DISH -> R.string.nutrition_comparison_title_dish
}

/**
 * Kind-specific (container, content) colors for the rounded name background and matching chips.
 * 取值统一来自设计令牌 AppFoodKindCurrent（食材=叶绿, 食物=橙, 菜肴=番茄红），深浅色一致。
 */
private fun FoodKind.nameColors(): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> = when (this) {
    FoodKind.INGREDIENT -> AppFoodKindCurrent.ingredient to AppFoodKindCurrent.onKind
    FoodKind.FOOD -> AppFoodKindCurrent.preparedFood to AppFoodKindCurrent.onKind
    FoodKind.DISH -> AppFoodKindCurrent.dish to AppFoodKindCurrent.onKind
}

@Composable
private fun systemTagPresentation(tag: String): Pair<String, androidx.compose.ui.graphics.Color>? = when (tag) {
    "common" -> stringResource(R.string.nutrition_tag_common) to androidx.compose.ui.graphics.Color(0xFF2E7D32)
    "favorite" -> stringResource(R.string.nutrition_tag_favorite) to androidx.compose.ui.graphics.Color(0xFFC62828)
    "recent" -> stringResource(R.string.nutrition_tag_recent) to androidx.compose.ui.graphics.Color(0xFF1565C0)
    else -> null
}

internal fun FoodItem.imageKey(): String = image?.localKey
    ?.takeUnless { it == FoodImageStore.DEFAULT_KEY }
    ?: id

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
    }.distinct().mapNotNull { systemTagPresentation(it) }
    Box(modifier = modifier) {
        // 方形缩略图槽位统一 1:1 居中裁切（移动规范 06 §三），原图是竖图或横图都不会在槽内留黑边。
        FoodImage(image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (tags.isNotEmpty()) {
            MarqueeRow(
                modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(vertical = AppSpacingDefault.s1),
                horizontalArrangement = Arrangement.spacedBy(AppSpacingDefault.s1),
            ) {
                tags.forEach { (label, color) ->
                    Surface(shape = RoundedCornerShape(AppRadiiDefault.container), color = color) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = AppSpacingDefault.s1, vertical = 1.dp),
                            style = AppTypeDefault.caption,
                            color = androidx.compose.ui.graphics.Color.White,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NutritionScreen(viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val ingredientListState = rememberSaveable("nutrition-list-ingredient", saver = LazyListState.Saver) { LazyListState() }
    val foodListState = rememberSaveable("nutrition-list-food", saver = LazyListState.Saver) { LazyListState() }
    val dishListState = rememberSaveable("nutrition-list-dish", saver = LazyListState.Saver) { LazyListState() }
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshDiseaseRisk()
        }
    }
    BackHandler(enabled = state.exerciseRequest != null) { viewModel.closeExerciseExpenditure() }
    when {
        state.exerciseRequest != null -> ExerciseExpenditureScreen(requireNotNull(state.exerciseRequest), viewModel::closeExerciseExpenditure)
        state.editor != null -> NutritionEditorScreen(requireNotNull(state.editor), viewModel)
        state.comparisonReturnTarget != null -> FoodComparisonScreen(
            food = requireNotNull(state.selectedFood),
            foods = state.foods,
            nutrientMetas = viewModel.nutrientMetas(),
            imageStore = viewModel.foodImages,
            resolve = viewModel::resolvePer100g,
            onBack = viewModel::closeComparison,
        )
        state.selectedFood != null -> FoodDetailScreen(
            food = requireNotNull(state.selectedFood),
            isFavorite = requireNotNull(state.selectedFood).id in state.favoriteFoodIds,
            isRecent = requireNotNull(state.selectedFood).id in state.recentFoodIds,
            viewModel = viewModel,
            nrvReference = state.nrvReference,
            nrvReferences = state.nrvReferences,
            onNrvReferenceSelected = viewModel::selectNrvReference,
            onOpenExerciseExpenditure = viewModel::openExerciseExpenditure,
            onBack = viewModel::closeFood,
        ) { viewModel.openComparison(NutritionDestination.FoodDetail) }
        // 浏览页与本页其它子屏（详情/编辑/对比）一样落在 AppTheme 上，搜索框、空态与卡片才共用一套令牌。
        else -> AppTheme(darkTheme = rememberAppDarkTheme()) {
            FoodBrowseScreen(state, viewModel, ingredientListState, foodListState, dishListState, modifier)
        }
    }
}

@Composable
private fun FoodBrowseScreen(
    state: NutritionUiState,
    viewModel: NutritionViewModel,
    ingredientListState: LazyListState,
    foodListState: LazyListState,
    dishListState: LazyListState,
    modifier: Modifier,
) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    val foods = viewModel.filteredCardMetadata(language)
    var addingTag by remember { mutableStateOf(false) }
    var ingredientTagsExpanded by rememberSaveable { mutableStateOf(true) }
    var ingredientCategoriesExpanded by rememberSaveable { mutableStateOf(true) }
    var foodTagsExpanded by rememberSaveable { mutableStateOf(true) }
    var foodCategoriesExpanded by rememberSaveable { mutableStateOf(true) }
    var dishTagsExpanded by rememberSaveable { mutableStateOf(true) }
    var searchFocused by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val showSidebar = state.selectedKind != FoodKind.DISH
    val tagsExpanded = when (state.selectedKind) {
        FoodKind.INGREDIENT -> ingredientTagsExpanded
        FoodKind.FOOD -> foodTagsExpanded
        FoodKind.DISH -> dishTagsExpanded
    }
    val categoriesExpanded = when (state.selectedKind) {
        FoodKind.INGREDIENT -> ingredientCategoriesExpanded
        FoodKind.FOOD -> foodCategoriesExpanded
        FoodKind.DISH -> false
    }
    val toggleTags = {
        when (state.selectedKind) {
            FoodKind.INGREDIENT -> ingredientTagsExpanded = !ingredientTagsExpanded
            FoodKind.FOOD -> foodTagsExpanded = !foodTagsExpanded
            FoodKind.DISH -> dishTagsExpanded = !dishTagsExpanded
        }
    }
    val toggleCategories = {
        when (state.selectedKind) {
            FoodKind.INGREDIENT -> ingredientCategoriesExpanded = !ingredientCategoriesExpanded
            FoodKind.FOOD -> foodCategoriesExpanded = !foodCategoriesExpanded
            FoodKind.DISH -> Unit
        }
    }
    // 同记录页：聚焦且为空即展开，输入即消失。
    val showSearchActivity = searchFocused && state.keyword.isBlank()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalAppTokens.current.colors.bg),
    ) {
        val tokens = LocalAppTokens.current
        val pagePadding = 10.dp
        // 下拉面板叠在分类栏与结果列表之上：展开时不参与排版，不再把下方内容挤成一条缝。
        val panelTop = pagePadding + tokens.spacing.hitMin + 8.dp
        val collapseSearch = {
            searchFocused = false
            focusManager.clearFocus()
        }
        // 返回键先收起面板，而不是直接退出浏览页（§5.3）。
        BackHandler(enabled = showSearchActivity) { collapseSearch() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pagePadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 输入即过滤；回车才把这次关键词记进历史（旧版 onSearch 传的是空 lambda，等于给了一个死按钮）。
            AppSearchField(
                value = state.keyword,
                onValueChange = viewModel::setKeyword,
                placeholder = stringResource(R.string.nutrition_search_food),
                onSearch = { viewModel.commitSearch() },
                onFocusChanged = { searchFocused = it },
            )
            KindSegmenter(state.selectedKind, viewModel::selectKind)
            val browseAreaModifier = Modifier.weight(1f)
            Column(modifier = browseAreaModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.selectedKind == FoodKind.DISH) {
                    if (tagsExpanded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            BrowseFilterControls(state.selectedKind, true, false, toggleTags, {}, Modifier.width(80.dp).height(88.dp), { viewModel.openEditor(state.selectedKind) }, state.customOnly, { viewModel.toggleCustomOnly() })
                            TagFilters(state, viewModel, { addingTag = true }, Modifier.weight(1f))
                        }
                    } else {
                        BrowseFilterControls(state.selectedKind, false, false, toggleTags, {}, Modifier.fillMaxWidth().height(30.dp), { viewModel.openEditor(state.selectedKind) }, state.customOnly, { viewModel.toggleCustomOnly() })
                    }
                } else if (tagsExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        BrowseFilterControls(state.selectedKind, true, categoriesExpanded, toggleTags, toggleCategories, Modifier.width(80.dp).height(88.dp), { viewModel.openEditor(state.selectedKind) }, state.customOnly, { viewModel.toggleCustomOnly() })
                        TagFilters(state, viewModel, { addingTag = true }, Modifier.weight(1f))
                    }
                } else if (!categoriesExpanded) {
                    BrowseFilterControls(state.selectedKind, false, false, toggleTags, toggleCategories, Modifier.fillMaxWidth().height(30.dp), { viewModel.openEditor(state.selectedKind) }, state.customOnly, { viewModel.toggleCustomOnly() })
                }
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.selectedKind == FoodKind.DISH) {
                        FoodResultsHost(foods, language, viewModel, state.selectedKind, state.showGlycemicIndicator, state, dishListState, Modifier.weight(1f))
                    } else {
                    if (!tagsExpanded && showSidebar && categoriesExpanded) {
                        Column(modifier = Modifier.width(80.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BrowseFilterControls(state.selectedKind, false, true, toggleTags, toggleCategories, Modifier.fillMaxWidth().height(88.dp), onAddCustom = { viewModel.openEditor(state.selectedKind) })
                            CategorySidebar(state, viewModel, Modifier.weight(1f))
                        }
                    } else if (tagsExpanded && showSidebar && categoriesExpanded) {
                        CategorySidebar(state, viewModel, Modifier.width(80.dp))
                    }
                    FoodResultsHost(
                        foods = foods,
                        language = language,
                        viewModel = viewModel,
                        kind = state.selectedKind,
                        showGlycemicIndicator = state.showGlycemicIndicator,
                        state = state,
                        listState = when (state.selectedKind) {
                            FoodKind.INGREDIENT -> ingredientListState
                            FoodKind.FOOD -> foodListState
                            FoodKind.DISH -> dishListState
                        },
                        modifier = Modifier.weight(1f),
                    )
                    }
                }
            }
        }

        if (showSearchActivity) {
            val recent = state.recentFoodIds.mapNotNull { id -> viewModel.foodById(id) }.map { food ->
                RecentSearchItem(food.id, food.displayName(language), when (food.kind) { FoodKind.INGREDIENT -> R.drawable.ic_food_ingredient; FoodKind.FOOD -> R.drawable.ic_nav_nutrition; FoodKind.DISH -> R.drawable.ic_food_dish })
            }
            // 320 dp 上限之外再按可用高度的一半封顶：横屏或大号键盘时不会顶出屏幕。
            val panelMaxHeight = minOf(
                SearchPanelMaxHeight,
                maxHeight * 0.55f,
                maxHeight - panelTop - pagePadding,
            ).coerceAtLeast(160.dp)

            SearchPanelDismissLayer(
                onDismiss = { collapseSearch() },
                modifier = Modifier.padding(top = panelTop),
            )
            SearchActivityPanel(
                history = state.searchHistory,
                recentItems = recent,
                historyTitle = stringResource(R.string.nutrition_search_history),
                recentsTitle = stringResource(R.string.nutrition_click_history),
                onSelectHistory = {
                    viewModel.setKeyword(it)
                    viewModel.commitSearch()
                    collapseSearch()
                },
                onRemoveHistory = viewModel::removeSearchHistory,
                onClearHistory = viewModel::clearSearchHistory,
                onOpenRecent = { item -> viewModel.foodById(item.id)?.let(viewModel::openFood) },
                onRemoveRecent = { item -> viewModel.removeRecentFood(item.id) },
                onClearRecents = viewModel::clearRecentFoods,
                onCollapse = { collapseSearch() },
                maxHeight = panelMaxHeight,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = pagePadding, end = pagePadding, top = panelTop),
            )
        }
    }
    if (addingTag) AddTagDialog({ addingTag = false }) { viewModel.addUserTag(it); addingTag = false }
}

@Composable
private fun CustomOnlyButton(state: NutritionUiState, viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    CustomOnlyButton(state.customOnly, state.selectedKind, viewModel::toggleCustomOnly, modifier)
}

@Composable
private fun CustomOnlyButton(
    selected: Boolean,
    kind: FoodKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = kind.nameColors()
    Surface(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) colors.first else colors.first.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) colors.second else LocalAppTokens.current.colors.border,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            TextOverflowText(
                stringResource(kind.customLabelRes()),
                style = TextStyle(fontSize = FontTokens.body),
                color = colors.second,
                maxLines = 1,
            )
        }
    }
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
    customOnly: Boolean = false,
    onToggleCustom: () -> Unit = {},
) {
    val showCategories = kind != FoodKind.DISH
    val flat = !tagsExpanded && !categoriesExpanded
    if (flat) {
        Row(modifier = modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AddCustomButton(kind, Modifier.weight(1f), onAddCustom)
            FilterCollapseButton(tagsExpanded, FilterCollapseAxis.Vertical, stringResource(R.string.nutrition_collapse_tags), stringResource(R.string.nutrition_expand_tags), onToggleTags, Modifier.weight(1f))
            if (showCategories) FilterCollapseButton(categoriesExpanded, FilterCollapseAxis.Horizontal, stringResource(R.string.nutrition_collapse_categories), stringResource(R.string.nutrition_expand_categories), onToggleCategories, Modifier.weight(1f))
            if (!showCategories) CustomOnlyButton(customOnly, kind, onToggleCustom, Modifier.weight(1f))
        }
    } else {
        val buttonCount = if (showCategories) 3 else 3
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AddCustomButton(kind, Modifier.weight(1f / buttonCount), onAddCustom)
            FilterCollapseButton(tagsExpanded, FilterCollapseAxis.Vertical, stringResource(R.string.nutrition_collapse_tags), stringResource(R.string.nutrition_expand_tags), onToggleTags, Modifier.weight(1f / buttonCount))
            if (showCategories) FilterCollapseButton(categoriesExpanded, FilterCollapseAxis.Horizontal, stringResource(R.string.nutrition_collapse_categories), stringResource(R.string.nutrition_expand_categories), onToggleCategories, Modifier.weight(1f / buttonCount))
            else CustomOnlyButton(customOnly, kind, onToggleCustom, Modifier.weight(1f / buttonCount))
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
private fun FoodResultsHost(
    foods: List<FoodCardMetadata>,
    language: String,
    viewModel: NutritionViewModel,
    kind: FoodKind,
    showGlycemicIndicator: Boolean,
    state: NutritionUiState,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    if (foods.isEmpty()) {
        // 空结果按原因分开说：搜索没命中就提示换词，筛选筛空了就给「清除筛选」的出口（§6.2）。
        val searching = state.keyword.isNotBlank()
        AppEmptyState(
            title = stringResource(if (searching) R.string.nutrition_search_empty_title else R.string.nutrition_filter_empty_title),
            description = stringResource(
                if (searching) R.string.nutrition_search_empty_hint else R.string.nutrition_no_foods,
            ),
            iconRes = if (searching) R.drawable.ic_search_empty else R.drawable.ic_list,
            action = {
                TextButton(onClick = viewModel::clearFilters) {
                    Text(stringResource(R.string.price_filter_clear))
                }
            },
            modifier = modifier,
        )
    } else {
        var activeHighlightToken by remember { mutableStateOf<Long?>(null) }
        LaunchedEffect(state.listHighlightToken, state.listHighlightFoodId) {
            if (state.listHighlightFoodId != null && state.listHighlightToken > 0) {
                withFrameNanos { }
                if (listState.layoutInfo.visibleItemsInfo.any { it.key == state.listHighlightFoodId }) {
                    activeHighlightToken = state.listHighlightToken
                }
            }
        }
        val boundaryState by remember(foods.size) {
            derivedStateOf {
                val layout = listState.layoutInfo
                if (foods.isEmpty() || layout.totalItemsCount == 0 || layout.viewportEndOffset <= layout.viewportStartOffset) {
                    FoodListBoundaryState.NotLaidOut
                } else {
                    val lastVisible = layout.visibleItemsInfo.lastOrNull()
                    val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    val atBottom = lastVisible != null &&
                        lastVisible.index == foods.lastIndex &&
                        lastVisible.offset + lastVisible.size <= layout.viewportEndOffset + 1
                    FoodListBoundaryState.Ready(atTop, atBottom)
                }
            }
        }
        LaunchedEffect(boundaryState, foods.size, kind) {
            Log.d("NutritionListBoundary", "kind=$kind size=${foods.size} state=$boundaryState index=${listState.firstVisibleItemIndex} offset=${listState.firstVisibleItemScrollOffset}")
        }
        Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(foods, key = { it.id }) {
                FoodRow(
                    it,
                    language,
                    viewModel,
                    showGlycemicIndicator,
                    state.listHighlightFoodId == it.id && activeHighlightToken == state.listHighlightToken,
                    activeHighlightToken ?: 0L,
                    { activeHighlightToken = null },
                    viewModel::openFood,
                )
            }
        }
        val boundaryColor = LocalAppTokens.current.colors.warning
        val readyBoundary = boundaryState as? FoodListBoundaryState.Ready
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .fillMaxWidth()
                .height(10.dp)
                .alpha(if (readyBoundary?.atTop == true) 1f else 0f)
                .background(Brush.verticalGradient(listOf(boundaryColor.copy(alpha = 0.82f), Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .fillMaxWidth()
                .height(10.dp)
                .alpha(if (readyBoundary?.atBottom == true) 1f else 0f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, boundaryColor.copy(alpha = 0.82f)))),
        )
        }
    }
}

private sealed interface FoodListBoundaryState {
    data object NotLaidOut : FoodListBoundaryState
    data class Ready(val atTop: Boolean, val atBottom: Boolean) : FoodListBoundaryState
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
        color = LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.3f),
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
            border = androidx.compose.foundation.BorderStroke(1.dp, if (state.customOnly) colors.second else LocalAppTokens.current.colors.border),
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
    Surface(modifier = Modifier.fillMaxWidth().height(44.dp).padding(bottom = 4.dp), shape = RoundedCornerShape(6.dp), color = LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.3f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp)) {
            Text(title, style = LocalAppTokens.current.type.caption, color = LocalAppTokens.current.colors.textMuted)
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
        color = if (selected) LocalAppTokens.current.colors.primaryContainer else LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) LocalAppTokens.current.colors.textMuted else LocalAppTokens.current.colors.border),
    ) {
        Box(modifier = Modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) { label() }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FoodCardNameHeader(
    food: FoodItem,
    language: String,
    isCustom: Boolean,
    cookingSuffix: String?,
    modifier: Modifier = Modifier,
) {
    val colors = food.kind.nameColors()
    val aliases = food.allNames(language).drop(1).filter(String::isNotBlank)
    val copyText = buildFoodNameText(food.displayName(language), cookingSuffix, food.allNames(language).drop(1))
    CopyableFoodNameArea(copyText, modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        if (isCustom) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.nutrition_custom_marker),
                modifier = Modifier.size(AppSpacingDefault.s4),
                tint = LocalAppTokens.current.colors.primary,
            )
            Spacer(Modifier.width(AppSpacingDefault.s1))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.8f).heightIn(min = AppSpacingDefault.s6),
            shape = RoundedCornerShape(AppRadiiDefault.label),
            color = colors.first,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppSpacingDefault.s2, vertical = AppSpacingDefault.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(if (aliases.isEmpty()) Modifier.weight(1f) else Modifier.weight(0.6f)) {
                    TextOverflowText(
                        text = food.displayName(language),
                        modifier = Modifier.fillMaxWidth(),
                        style = AppTypeDefault.titleS,
                        color = colors.second,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
                cookingSuffix?.let { Text(" ($it)", style = AppTypeDefault.titleS, color = colors.second, maxLines = 1, softWrap = false) }
                if (aliases.isNotEmpty()) {
                    TextOverflowText(
                        text = aliases.joinToString(" / "),
                        modifier = Modifier.weight(0.4f),
                        style = AppTypeDefault.bodyS,
                        color = colors.second.copy(alpha = 0.9f),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        }
    }
}

private fun buildFoodNameText(primaryName: String, cookingSuffix: String?, aliases: List<String>): String =
    buildList {
        add(primaryName)
        cookingSuffix?.takeIf(String::isNotBlank)?.let { add("($it)") }
        aliases.filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.let { add(it.joinToString(" / ")) }
    }.joinToString(" ")

@Composable
private fun FoodCardMetadataHeader(food: FoodCardMetadata) {
    val colors = food.kind.nameColors()
    val copyText = buildFoodNameText(food.primaryName, food.cookingMethodLabel, food.aliases)
    CopyableFoodNameArea(copyText) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        if (food.isCustom) {
            Icon(Icons.Filled.Edit, stringResource(R.string.nutrition_custom_marker), Modifier.size(14.dp), tint = LocalAppTokens.current.colors.primary)
            Spacer(Modifier.width(2.dp))
        }
        Surface(modifier = Modifier.fillMaxWidth(0.8f).height(28.dp), shape = RoundedCornerShape(6.dp), color = colors.first) {
            Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(0.6f)) {
                    TextOverflowText(food.primaryName, Modifier.fillMaxWidth(), TextStyle(fontSize = FontTokens.subtitle), color = colors.second, maxLines = 1, textAlign = TextAlign.Center)
                }
                food.cookingMethodLabel?.let { Text(" ($it)", style = TextStyle(fontSize = FontTokens.subtitle), color = colors.second, maxLines = 1, softWrap = false) }
                if (food.aliases.isNotEmpty()) {
                    TextOverflowText(food.aliases.joinToString(" / "), Modifier.weight(0.4f), TextStyle(fontSize = FontTokens.body), color = colors.second.copy(alpha = 0.9f), maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CopyableFoodNameArea(
    copyText: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Box(
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                clipboard.setText(AnnotatedString(copyText))
                Toast.makeText(context, R.string.nutrition_name_copied, Toast.LENGTH_SHORT).show()
            },
        ),
    ) {
        content()
    }
}

@Composable
private fun FoodRow(
    food: FoodCardMetadata,
    language: String,
    viewModel: NutritionViewModel,
    showGlycemicIndicator: Boolean,
    highlightOnReturn: Boolean,
    highlightToken: Long,
    onHighlightFinished: () -> Unit,
    onClick: (FoodItem) -> Unit,
) {
    val fullFood = viewModel.foodById(food.id) ?: return
    val image = viewModel.foodImages.rememberImage(food.imageKey, ImageVariant.THUMB)
    val secondaryLine = food.componentCount?.let { stringResource(R.string.nutrition_dish_components) + ": " + it }
    var previewing by remember { mutableStateOf(false) }
    val previewImage = if (previewing) {
        viewModel.foodImages.rememberImage(food.imageKey, ImageVariant.DETAIL)
    } else {
        image
    }
    val highlight = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(highlightToken, highlightOnReturn) {
        if (highlightOnReturn) {
            highlight.snapTo(0f)
            repeat(2) {
                highlight.animateTo(1f, androidx.compose.animation.core.tween(360))
                kotlinx.coroutines.delay(180)
                highlight.animateTo(0f, androidx.compose.animation.core.tween(360))
                kotlinx.coroutines.delay(180)
                if (it == 0) kotlinx.coroutines.delay(140)
            }
            onHighlightFinished()
        } else {
            highlight.snapTo(0f)
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.34f),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            LocalAppTokens.current.colors.primary.copy(alpha = highlight.value),
        ),
    ) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(fullFood) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodImageWithSystemTags(
            food = fullFood,
            image = image,
            isFavorite = food.isFavorite,
            isRecent = food.isRecent,
            modifier = Modifier.size(64.dp).clickable { previewing = true },
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
                FoodCardMetadataHeader(food)
            Text(stringResource(R.string.nutrition_energy_per_100g, food.energyPer100g), style = LocalAppTokens.current.type.body)
            secondaryLine?.let {
                TextOverflowText(it, style = LocalAppTokens.current.type.caption, color = LocalAppTokens.current.colors.textMuted)
            }
            food.categoryLabels.takeIf { it.isNotEmpty() }?.let { labels ->
                TextOverflowText(
                    text = labels.joinToString(" / "),
                    style = LocalAppTokens.current.type.caption,
                    color = LocalAppTokens.current.colors.textMuted,
                )
            }
        }
        if (showGlycemicIndicator) GlycemicGlass(glycemicLevel(food.glycemicIndex, food.glycemicLoadPer100g))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = LocalAppTokens.current.colors.textMuted)
    }
    }
    if (previewing) FoodImagePreview(previewImage, onDismiss = { previewing = false })
}

@Composable
private fun GlycemicGlass(level: com.woshiwangnima.healthdietpro.model.food.GlycemicLevel?) {
    val color = when (level?.classification) {
        GlycemicClassification.Low -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        GlycemicClassification.Medium -> androidx.compose.ui.graphics.Color(0xFFF9A825)
        GlycemicClassification.High -> androidx.compose.ui.graphics.Color(0xFFC62828)
        null -> LocalAppTokens.current.colors.border
    }
    val background = LocalAppTokens.current.colors.panelRaised
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
    if (level == null) Text("?", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = LocalAppTokens.current.colors.textMuted)
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
    nrvReference: NrvReference?,
    nrvReferences: List<NrvReference>,
    onNrvReferenceSelected: (String) -> Unit,
    onOpenExerciseExpenditure: (List<NutritionExerciseServing>) -> Unit,
    onBack: () -> Unit,
    onCompare: () -> Unit,
) {
    AppTheme(darkTheme = rememberAppDarkTheme()) {
        FoodDetailContent(
            food = food,
            isFavorite = isFavorite,
            isRecent = isRecent,
            viewModel = viewModel,
            nrvReference = nrvReference,
            nrvReferences = nrvReferences,
            onNrvReferenceSelected = onNrvReferenceSelected,
            onOpenExerciseExpenditure = onOpenExerciseExpenditure,
            onBack = onBack,
            onCompare = onCompare,
        )
    }
}

@Composable
private fun FoodDetailContent(
    food: FoodItem,
    isFavorite: Boolean,
    isRecent: Boolean,
    viewModel: NutritionViewModel,
    nrvReference: NrvReference?,
    nrvReferences: List<NrvReference>,
    onNrvReferenceSelected: (String) -> Unit,
    onOpenExerciseExpenditure: (List<NutritionExerciseServing>) -> Unit,
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
    var helpTopic by remember(food.id) { mutableStateOf<NutritionHelpTopic?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val relatedDishes = remember(food.id) { viewModel.relatedDishes(food.id) }
    val isCustom = viewModel.isCustom(food.id)
    val configuration = LocalConfiguration.current
    val language = configuration.locales[0]?.language ?: "en"
    // 营养素表在页内单独滚动（§4.8 允许一层嵌套）：高度跟随屏高，短屏也不会把页面挤到只剩半屏。
    val nutrientTableMaxHeight = (configuration.screenHeightDp * 0.45f).coerceIn(240f, 480f).dp
    val detailImage = imageStore.rememberImage(food.imageKey(), ImageVariant.DETAIL)
    val defaultServingLabel = stringResource(R.string.nutrition_serving_100g_edible)
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
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = spacing.page, vertical = spacing.s3)) {
        DetailPanel {
        // 高 104 = 两排 48 dp 图标按钮 + 8 dp 间距：相邻热区之间留够 8 dp，避免连带误触（规范 01 §三、07 检查项 04）。
        Row(modifier = Modifier.height(104.dp), verticalAlignment = Alignment.Top) {
            FoodImageWithSystemTags(
                food = food,
                image = detailImage,
                isFavorite = isFavorite,
                isRecent = isRecent,
                modifier = Modifier.size(96.dp).clickable { previewing = true },
            )
            Spacer(Modifier.width(spacing.s3)); Column(Modifier.weight(1f).fillMaxHeight()) {
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
                    modifier = Modifier.weight(1f),
                ) { viewModel.openFood(it) }
            }
            Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceBetween) {
                if (isCustom) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                        IconButton(onClick = { viewModel.openEditor(food.kind, food.id) }) {
                            Icon(Icons.Filled.Edit, stringResource(R.string.nutrition_editor_edit), tint = colors.textMuted)
                        }
                        // 删除是不可逆操作，允许使用危险色（§2.3），与「编辑」区分。
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.nutrition_editor_delete), tint = colors.danger)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    IconButton(onClick = { viewModel.toggleFavorite(food) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.nutrition_favorite),
                            // 收藏是正向状态，按 §2.3 不得借用危险色（danger 只用于删除 / 不可逆操作）。
                            tint = if (isFavorite) colors.warning else colors.textMuted,
                        )
                    }
                        IconButton(onClick = onCompare) {
                            Icon(painterResource(R.drawable.ic_vs), stringResource(R.string.nutrition_compare))
                        }
                }
            }
        }
        FoodHeaderPager(
            page = headerPage,
            pageCount = 3,
            onPrevious = {
                headerDirection = -1
                headerPage = if (headerPage == 0) 2 else headerPage - 1
            },
            onNext = {
                headerDirection = 1
                headerPage = (headerPage + 1) % 3
            },
        )
        }
        EqualWidthSegmentedTabs(
            tabs = listOf(
                EqualWidthTab(R.string.nutrition_tab_profile),
                EqualWidthTab(food.rankingTabLabelRes()),
                EqualWidthTab(R.string.nutrition_tab_estimate),
            ),
            selectedIndex = tab,
            onSelected = { tab = it },
            modifier = Modifier.padding(top = spacing.s4),
        )
        if (tab == 0) Column(
            modifier = Modifier.weight(1f).padding(top = spacing.s3).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            val selectedServing = servings.first { it.id == selectedServingId }
            val selectedServingLabel = if (selectedServing.id == "per_100g") {
                stringResource(R.string.nutrition_serving_100g_edible)
            } else {
                selectedServing.displayLabel(language)
            }
            val nutrientRowItems = remember(resolved, selectedServing, nrvReference, language) {
                nutrientRows(resolved, selectedServing, viewModel.nutrientMetas(), language, nrvReference)
            }
            // 供能 / 健康指标 / 营养信息三张区块卡：标题随卡，卡底用 panelRaised（与纯黑底 2 级明度差，§2.7-1）。
            AppSectionCard(
                iconRes = R.drawable.ic_energy_distribution,
                title = stringResource(R.string.nutrition_macronutrient_energy),
                action = {
                    HelpIconButton(
                        description = stringResource(R.string.nutrition_energy_help),
                        onClick = { helpTopic = NutritionHelpTopic.Energy },
                    )
                },
            ) {
                MacronutrientEnergyChart(resolved)
            }
            AppSectionCard(
                iconRes = R.drawable.ic_health_metrics,
                title = stringResource(R.string.nutrition_health_metrics),
                action = {
                    HelpIconButton(
                        description = stringResource(R.string.nutrition_health_metrics_help),
                        onClick = { helpTopic = NutritionHelpTopic.HealthMetrics },
                    )
                },
                // 指标表带完整网格线，铺满卡宽时最外圈单元格线会与卡片描边叠成双线，故左右内缩一档。
                contentPadding = PaddingValues(start = spacing.s3, end = spacing.s3, bottom = spacing.s3),
            ) {
                HealthMetricsTable(resolved?.healthMetrics ?: food.healthMetrics, stringResource(R.string.nutrition_metric_no_data))
            }
            AppSectionCard(
                iconRes = R.drawable.ic_nutrients,
                title = stringResource(R.string.nutrition_nutrients),
                action = {
                    HelpIconButton(
                        description = stringResource(R.string.nutrition_nutrients_help),
                        onClick = { helpTopic = NutritionHelpTopic.Nutrients },
                    )
                },
                contentPadding = PaddingValues(bottom = spacing.s3),
            ) {
                Column(Modifier.padding(horizontal = spacing.s3)) {
                    FoodServingSelector(
                        servings = servings,
                        selectedServingId = selectedServingId,
                        tableBasisGrams = resolved?.takeIf { it.basis.unitId.equals("g", ignoreCase = true) }?.basis?.value,
                        onSelected = { selectedServingId = it },
                    )
                    NutritionEnergyAndExercise(
                        resolved = resolved,
                        serving = selectedServing,
                        onClick = {
                            onOpenExerciseExpenditure(servings.map { serving ->
                                NutritionExerciseServing(
                                    label = if (serving.id == "per_100g") defaultServingLabel else serving.displayLabel(language),
                                    kilocalories = resolved.energyKilocalories(serving.ratioToTable) ?: 0.0,
                                )
                            })
                        },
                    )
                }
                // 空态：解析失败或该条目没有营养素表时不留一张只有表头的空表（§6.2）。
                if (nutrientRowItems.isEmpty()) {
                    DetailEmptyState(
                        iconRes = R.drawable.ic_nutrients,
                        title = stringResource(R.string.nutrition_profile_empty_title),
                        description = stringResource(R.string.nutrition_profile_empty_hint),
                        modifier = Modifier.padding(horizontal = spacing.s3),
                    )
                } else {
                    AppDataTable(
                        rows = nutrientRowItems,
                        columns = listOf(
                            AppDataTableColumn("nutrient", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_item)) }, ColumnWidth.Flex(1f, 96.dp)) { AppDataTableText(it.labelText ?: stringResource(requireNotNull(it.labelRes))) },
                            AppDataTableColumn("amount", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_amount_for_serving, selectedServingLabel)) }, ColumnWidth.Flex(1f, 116.dp), alignment = Alignment.CenterEnd) { AppDataTableText(it.value, numeric = true) },
                            AppDataTableColumn("nrv", {
                                NrvReferenceHeader(nrvReference, nrvReferences, onNrvReferenceSelected)
                            }, ColumnWidth.Fixed(72.dp), alignment = Alignment.CenterEnd) { AppDataTableText(it.nrvPercent ?: "-", numeric = true) },
                        ),
                        rowKey = { _, row -> row.key },
                        showRowNumber = false,
                        showPager = false,
                        modifier = Modifier.fillMaxWidth(),
                        maxBodyHeight = nutrientTableMaxHeight,
                        // 表体渐隐要融进卡片底色，否则会在卡内出现一条比卡底更暗的色带。
                        fadeColor = colors.panelRaised,
                    )
                }
            }
            if (relatedDishes.isNotEmpty()) {
                RelatedDishesSection(
                    dishes = relatedDishes,
                    language = language,
                    viewModel = viewModel,
                    onOpen = { viewModel.openFood(it) },
                )
            }
        }
        else if (tab == 1 && food is Dish) {
            Column(
                modifier = Modifier.weight(1f).padding(top = spacing.s3).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.s4),
            ) {
                DishRecipeSection(food, viewModel, language) { viewModel.openFood(it) }
            }
        } else if (tab == 1 && food is PreparedFood && food.components.isNotEmpty()) {
            Column(
                modifier = Modifier.weight(1f).padding(top = spacing.s3).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.s4),
            ) {
                FoodRecipeSection(food, viewModel, language) { viewModel.openFood(it) }
            }
        } else {
            Column(Modifier.weight(1f).padding(top = spacing.s3).verticalScroll(rememberScrollState())) {
                DetailEmptyState(
                    iconRes = R.drawable.ic_list,
                    title = stringResource(R.string.nutrition_detail_placeholder),
                    description = stringResource(R.string.nutrition_detail_empty_hint),
                )
            }
        }
    }
    if (previewing) FoodImagePreview(detailImage, onDismiss = { previewing = false })
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.nutrition_editor_delete_title, stringResource(food.kind.customLabelRes()))) },
            text = { Text(stringResource(R.string.nutrition_editor_delete_confirm)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { confirmingDelete = false; viewModel.deleteCustomFood(food.id) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = LocalAppTokens.current.colors.danger),
                ) { Text(stringResource(R.string.nutrition_editor_delete)) }
            },
            dismissButton = { androidx.compose.material3.TextButton({ confirmingDelete = false }) { Text(stringResource(R.string.body_record_cancel)) } },
        )
    }
    helpTopic?.let { topic ->
        // 三处问号共用一套浮层：80% 底部抽屉 + 一板块一张卡，标题固定、只滚内容区。
        // 说明型长图文只需要 Full 一档，不做 Peek/Half 停靠（内容量不足以分档阅读）。
        AppBottomSheet(
            title = stringResource(topic.titleRes),
            subtitle = stringResource(topic.subtitleRes),
            snapPoints = listOf(AppSheetHeight.Full),
            initialSnap = AppSheetHeight.Full,
            onDismiss = { helpTopic = null },
        ) {
            when (topic) {
                NutritionHelpTopic.HealthMetrics -> {
                    HealthMetricInfoCard(
                        iconRes = R.drawable.ic_blood_glucose,
                        title = stringResource(R.string.nutrition_metric_gi),
                        description = stringResource(R.string.nutrition_metric_gi_help),
                        bands = glycemicIndexClassificationBands(),
                    )
                    HealthMetricInfoCard(
                        iconRes = R.drawable.ic_energy_distribution,
                        title = stringResource(R.string.nutrition_metric_gl),
                        description = stringResource(R.string.nutrition_metric_gl_help),
                        bands = glycemicLoadClassificationBands(),
                    )
                    HealthMetricInfoCard(
                        iconRes = R.drawable.ic_shield,
                        title = stringResource(R.string.nutrition_metric_inflammatory_potential),
                        description = stringResource(R.string.nutrition_metric_inflammatory_potential_help),
                        note = stringResource(R.string.nutrition_health_metrics_help_no_scale),
                    )
                }

                NutritionHelpTopic.Nutrients -> {
                    EnergyUnitHelpCard()
                    EdiblePortionHelpCard()
                    NrvHelpCard()
                }

                NutritionHelpTopic.Energy -> {
                    EnergyShareHelpCard()
                    EnergyUnitHelpCard()
                }
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
    modifier: Modifier,
    onOpenFood: (FoodItem) -> Unit,
) {
    AnimatedPageContent(page, modifier.fillMaxWidth(), direction = { _, _ -> direction }) { currentPage ->
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val textStyle = LocalAppTokens.current.type.bodyS
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
}

@Composable
private fun FoodHeaderPager(
    page: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().height(tokens.spacing.hitMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HeaderPagerButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            description = stringResource(R.string.nutrition_header_previous),
            onClick = onPrevious,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                val active = index == page
                Box(
                    modifier = Modifier
                        .size(if (active) tokens.spacing.s2 else tokens.spacing.s1)
                        .clip(CircleShape)
                        .background(if (active) tokens.colors.text else tokens.colors.borderStrong),
                )
            }
        }
        HeaderPagerButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = stringResource(R.string.nutrition_header_next),
            onClick = onNext,
        )
    }
}

@Composable
private fun HeaderPagerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    Box(
        modifier = Modifier.size(tokens.spacing.hitMin).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, modifier = Modifier.size(tokens.spacing.s5), tint = tokens.colors.text)
    }
}

@Composable
private fun FoodSourcesSection(food: FoodItem, textStyle: androidx.compose.ui.text.TextStyle) {
    if (food.sources.isEmpty()) {
        Text(
            stringResource(R.string.nutrition_sources_empty),
            style = textStyle,
            color = LocalAppTokens.current.colors.textMuted,
        )
    } else {
        food.sources.forEach { source -> SourceLink(source.dataset, source.reference, textStyle) }
    }
}

private data class NutrientEnergyItem(
    val nutrientId: String,
    val kcal: Double,
)

private val energyKcalPerGram: Map<String, Double> = mapOf(
    "PROTEIN" to 4.0,
    "CHO" to 4.0,
    "FAT" to 9.0,
    "FIBER" to 2.0,
    "ALCOHOL" to 7.0,
    "ORGANIC_ACID" to 3.0,
)

private fun ResolvedNutrition?.nutrientEnergyItems(): List<NutrientEnergyItem>? {
    val nutrients = this?.nutrients ?: return null
    val items = nutrients.mapNotNull { (id, amount) ->
        val factor = energyKcalPerGram[id] ?: return@mapNotNull null
        if (amount.unitId.equals("g", ignoreCase = true)) {
            (amount.value * factor).takeIf { it > 0.0 }?.let { NutrientEnergyItem(id, it) }
        } else {
            null
        }
    }.sortedByDescending { it.kcal }
    return items.takeIf { it.isNotEmpty() }
}

internal fun ResolvedNutrition?.energyKilocalories(multiplier: Double = 1.0): Double? {
    val amount = this?.nutrients?.get("ENERGY") ?: return null
    return UnitConverter.toBase(
        "energy",
        (amount.value * multiplier).toFloat(),
        amount.unitId.lowercase(),
    ).toDouble()
}

@Composable
private fun nutrientEnergyLabel(nutrientId: String, percent: Double): String = when (nutrientId.uppercase()) {
    "CHO" -> stringResource(R.string.nutrition_energy_carbohydrate, percent)
    "PROTEIN" -> stringResource(R.string.nutrition_energy_protein, percent)
    "FAT" -> stringResource(R.string.nutrition_energy_fat, percent)
    "FIBER" -> stringResource(R.string.nutrition_energy_fiber, percent)
    "ALCOHOL" -> stringResource(R.string.nutrition_energy_alcohol, percent)
    "ORGANIC_ACID" -> stringResource(R.string.nutrition_energy_organic_acid, percent)
    else -> stringResource(R.string.nutrition_energy_other, percent)
}

@Composable
private fun MacronutrientEnergyChart(resolved: ResolvedNutrition?) {
    val tokens = LocalAppTokens.current
    val items = resolved.nutrientEnergyItems()
    val totalKcal = resolved.energyKilocalories()
    val totalItemsKcal = items?.sumOf { it.kcal } ?: 0.0
    if (items == null || totalKcal == null || totalItemsKcal <= 0.0) {
        Text(
            text = stringResource(R.string.nutrition_energy_empty),
            modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s3),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            textAlign = TextAlign.Center,
        )
        return
    }
    val top = items.take(3)
    val otherKcal = items.drop(3).sumOf { it.kcal }
    val legendItems = buildList {
        top.forEach { item ->
            add(
                MacronutrientLegendItem(
                    id = item.nutrientId,
                    label = nutrientEnergyLabel(item.nutrientId, item.kcal / totalItemsKcal * 100.0),
                    value = item.kcal,
                    color = tokens.chart.forNutrient(item.nutrientId),
                ),
            )
        }
        if (otherKcal > 0.0) {
            add(
                MacronutrientLegendItem(
                    id = "other",
                    label = stringResource(R.string.nutrition_energy_other, otherKcal / totalItemsKcal * 100.0),
                    value = otherKcal,
                    color = tokens.chart.other,
                ),
            )
        }
    }
    AnimatedDonutChart(
        segments = legendItems.map { DonutChartSegment(it.id, it.label, it.value.toFloat(), it.color) },
        centerValue = stringResource(R.string.nutrition_energy_kcal_value, formatGroupedInteger(totalKcal)),
        centerLabel = stringResource(R.string.nutrition_macronutrient_energy_center),
        modifier = Modifier.fillMaxWidth(),
        showLegend = false,
    )
    MacronutrientEnergyLegend(legendItems)
}

private data class MacronutrientLegendItem(
    val id: String,
    val label: String,
    val value: Double,
    val color: androidx.compose.ui.graphics.Color,
)

/**
 * 供能图例：文字统一取 `font.caption`（12 sp 下限，见 §2.4、§11.1），
 * 不再按数值大小分 15 / 12 / 9 sp —— 9 sp 低于最小字号，且同屏字号层级会超 4 级。
 * 数值大小差异由环形图面积与标签内置百分比表达（颜色不是唯一载体）。
 */
@Composable
private fun MacronutrientEnergyLegend(items: List<MacronutrientLegendItem>) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.weight(1f).heightIn(min = tokens.spacing.s6),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(tokens.spacing.s2)) { drawCircle(item.color) }
                Spacer(Modifier.width(tokens.spacing.s1))
                Text(
                    text = item.label,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun KindInfoSection(
    food: FoodItem,
    viewModel: NutritionViewModel,
    language: String,
    onOpenFood: (FoodItem) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle = LocalAppTokens.current.type.body,
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
    val hasBasicInfo = dish.techniqueId != null || dish.difficulty != null || dish.servesPeople != null
    if (hasBasicInfo) {
        AppSectionCard(R.drawable.ic_list, stringResource(R.string.nutrition_recipe_basic_information)) {
            dish.techniqueId?.let { viewModel.cookingMethodFor(it)?.let { method -> InfoLine(stringResource(R.string.nutrition_editor_technique), method.displayLabel(language)) } }
            dish.difficulty?.let { StarRatingLine(stringResource(R.string.nutrition_dish_difficulty), it) }
            dish.servesPeople?.let { InfoLine(stringResource(R.string.nutrition_dish_serves), stringResource(R.string.nutrition_dish_serves_value, it)) }
        }
    }
    // 食材清单：辅料（调味品或油脂）判定，主料在前、辅料在后。
    if (dish.components.isNotEmpty()) {
        AppSectionCard(R.drawable.ic_list, stringResource(R.string.nutrition_ingredient_list)) {
            val (auxiliary, main) = dish.components.partition { c -> viewModel.foodById(c.foodId)?.let { viewModel.isAuxiliary(it) } == true }
            if (main.isNotEmpty()) {
                Text(stringResource(R.string.nutrition_ingredient_main), style = LocalAppTokens.current.type.caption, color = LocalAppTokens.current.colors.textMuted, modifier = Modifier.padding(top = AppSpacingDefault.s1))
                main.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
            }
            if (auxiliary.isNotEmpty()) {
                Text(stringResource(R.string.nutrition_ingredient_auxiliary), style = LocalAppTokens.current.type.caption, color = LocalAppTokens.current.colors.textMuted, modifier = Modifier.padding(top = AppSpacingDefault.s2))
                auxiliary.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
            }
        }
    }

    // 制作教程/菜谱，逐步显示序号 + 可选计时器。
    if (dish.recipeSteps.isNotEmpty()) {
        AppSectionCard(R.drawable.ic_list, stringResource(R.string.nutrition_recipe_title)) {
            dish.recipeSteps.forEachIndexed { index, step ->
                RecipeStepRow(index + 1, step)
            }
        }
    }
}

@Composable
private fun FoodRecipeSection(food: PreparedFood, viewModel: NutritionViewModel, language: String, onOpenFood: (FoodItem) -> Unit) {
    val hasBasicInfo = food.techniqueId != null || food.servesPeople != null
    if (hasBasicInfo) {
        AppSectionCard(R.drawable.ic_list, stringResource(R.string.nutrition_recipe_basic_information)) {
            food.techniqueId?.let { techniqueId ->
                viewModel.cookingMethodFor(techniqueId)?.let { method ->
                    InfoLine(stringResource(R.string.nutrition_editor_technique), method.displayLabel(language))
                }
            }
            food.servesPeople?.let { InfoLine(stringResource(R.string.nutrition_dish_serves), stringResource(R.string.nutrition_dish_serves_value, it)) }
        }
    }
    if (food.components.isNotEmpty()) {
        AppSectionCard(R.drawable.ic_list, stringResource(R.string.nutrition_ingredient_list)) {
            val (auxiliary, main) = food.components.partition { component ->
                viewModel.foodById(component.foodId)?.let(viewModel::isAuxiliary) == true
            }
            main.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
            if (auxiliary.isNotEmpty()) {
                Text(stringResource(R.string.nutrition_ingredient_auxiliary), style = LocalAppTokens.current.type.caption, color = LocalAppTokens.current.colors.textMuted, modifier = Modifier.padding(top = AppSpacingDefault.s2))
                auxiliary.forEach { component -> DishComponentLine(component, viewModel, language, onOpenFood) }
            }
        }
    }
    if (food.recipeSteps.isNotEmpty()) {
        AppSectionCard(R.drawable.ic_list, stringResource(R.string.nutrition_recipe_title)) {
            food.recipeSteps.forEachIndexed { index, step -> RecipeStepRow(index + 1, step) }
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
    val tokens = LocalAppTokens.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s1), verticalAlignment = Alignment.CenterVertically) {
        if (item != null) {
            FoodNavigationLink(item.displayName(language), Modifier.weight(1f)) { onOpenFood(item) }
            Text(amount, style = tokens.type.numeric)
        } else {
            Text(component.foodId, modifier = Modifier.weight(1f), style = tokens.type.body)
            Text(amount, style = tokens.type.numeric)
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
    val tokens = LocalAppTokens.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s1), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = textStyle, color = tokens.colors.textMuted)
        Spacer(Modifier.width(tokens.spacing.s2))
        FoodNavigationLink(value, Modifier.weight(1f), textStyle, onClick)
    }
}

@Composable
private fun FoodNavigationLink(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = LocalAppTokens.current.type.body,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    Box(
        modifier = modifier.heightIn(min = tokens.spacing.hitMin).clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        TextOverflowText(
            text = text,
            style = textStyle.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
            color = LocalAppTokens.current.colors.primary,
            maxLines = 1,
        )
    }
}

/**
 * 相关菜肴：与其他板块一致，标题与列表都收进同一张区块卡（`AppSectionCard`）。
 * 卡内不再套二级卡片——卡中卡在纯黑 + 2 dp 描边下会出现两圈弧线；
 * 改用「无底色行 + 发丝分隔线」的移动端列表惯用式，行内整套 48 dp 热区仍然可点。
 * 同一食材可以出现在几十道菜里（鸡蛋尤甚），按常见度取前 [RelatedDishLimit] 道，其余只在标题右侧报总数。
 */
@Composable
private fun RelatedDishesSection(
    dishes: List<Dish>,
    language: String,
    viewModel: NutritionViewModel,
    onOpen: (Dish) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shown = remember(dishes) {
        dishes.sortedByDescending { it.commonness }.take(RelatedDishLimit)
    }
    AppSectionCard(
        iconRes = R.drawable.ic_food_dish,
        title = stringResource(R.string.nutrition_related_dishes),
        action = {
            Text(
                text = stringResource(R.string.nutrition_related_dishes_count, dishes.size),
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
                modifier = Modifier.padding(end = tokens.spacing.s2),
            )
        },
        // 左右内边距交给每一行，分隔线才能与文字对齐内缩。
        contentPadding = PaddingValues(bottom = tokens.spacing.s2),
    ) {
        shown.forEachIndexed { index, dish ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = tokens.spacing.s3),
                    thickness = tokens.radii.highlight,
                    color = tokens.colors.border,
                )
            }
            RelatedDishRow(
                dish = dish,
                language = language,
                viewModel = viewModel,
                onClick = { onOpen(dish) },
            )
        }
        if (dishes.size > shown.size) {
            Text(
                text = stringResource(R.string.nutrition_related_dishes_more, dishes.size - shown.size),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                modifier = Modifier.padding(
                    start = tokens.spacing.s3,
                    end = tokens.spacing.s3,
                    top = tokens.spacing.s2,
                ),
            )
        }
    }
}

private const val RelatedDishLimit = 6

/** 单行菜肴：48 dp 1:1 缩略图 + 菜名 + 「热量 / 组分」一行元信息，整行可点，最小高 56 dp。 */
@Composable
private fun RelatedDishRow(
    dish: Dish,
    language: String,
    viewModel: NutritionViewModel,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val image = viewModel.foodImages.rememberImage(dish.imageKey(), ImageVariant.THUMB)
    val resolved = remember(dish.id) { runCatching { viewModel.resolvePer100g(dish) }.getOrNull() }
    val energyLabel = resolved.energyKilocalories()?.let {
        stringResource(R.string.nutrition_energy_per_100g, it)
    }
    val componentLabel = if (dish.components.isNotEmpty()) {
        stringResource(R.string.nutrition_dish_components) + " " + dish.components.size
    } else {
        null
    }
    val meta = listOfNotNull(energyLabel, componentLabel).joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodImage(image, Modifier.size(48.dp), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(tokens.spacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dish.displayName(language),
                style = tokens.type.body,
                color = tokens.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = tokens.spacing.s1),
                )
            }
        }
        Spacer(Modifier.width(tokens.spacing.s2))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(tokens.spacing.s5),
            tint = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun SourceLink(dataset: String, reference: String, textStyle: androidx.compose.ui.text.TextStyle = LocalAppTokens.current.type.body) {
    val context = LocalContext.current
    val tokens = LocalAppTokens.current
    Column(Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s1)) {
        TextOverflowText(dataset, style = textStyle, color = tokens.colors.textMuted)
        // 可点击区域补足 48 dp 热区（§4.1-3 移动端最小热区）。
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = tokens.spacing.hitMin), contentAlignment = Alignment.CenterStart) {
            TextOverflowText(
                reference,
                modifier = Modifier.clickable {
                    val query = android.net.Uri.encode("$dataset $reference")
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=$query")))
                },
                style = textStyle.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                color = tokens.colors.primary,
            )
        }
    }
}

/** 难度星级：10 星整数展示。填充星取正文色、空星取描边色，不使用语义色（§2.3：语义色只表达状态）。 */
@Composable
private fun StarRatingLine(label: String, rating: Int) {
    val tokens = LocalAppTokens.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s1), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = tokens.type.body, color = tokens.colors.textMuted)
        Spacer(Modifier.width(tokens.spacing.s2))
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
            repeat(10) { i ->
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.spacing.s4),
                    // 空星用次要文字的 45% 透明度：卡片底已提亮到 panelRaised，原 border 色（#2B2B2B）几乎看不见刻度。
                    tint = if (i < rating) tokens.colors.text else tokens.colors.textMuted.copy(alpha = 0.45f),
                )
            }
        }
    }
}

@Composable
private fun RecipeStepRow(number: Int, step: com.woshiwangnima.healthdietpro.model.food.RecipeStep) {
    val tokens = LocalAppTokens.current
    Column(Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s1), verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
        Row {
            Text(stringResource(R.string.nutrition_recipe_step, number), style = tokens.type.bodyS, color = tokens.colors.primary)
            Spacer(Modifier.width(tokens.spacing.s2))
            Text(step.text, style = tokens.type.body, modifier = Modifier.weight(1f))
        }
        step.heatLevel?.let { Text(stringResource(heatLabelRes(it)), style = tokens.type.caption, color = tokens.colors.textMuted) }
        step.waterTemperatureC?.let { Text(stringResource(R.string.nutrition_water_temperature_value, it), style = tokens.type.caption, color = tokens.colors.textMuted) }
        step.oilTemperatureC?.let { Text(stringResource(R.string.nutrition_oil_temperature_value, it), style = tokens.type.caption, color = tokens.colors.textMuted) }
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
    val tokens = LocalAppTokens.current
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
        shape = RoundedCornerShape(tokens.radii.container),
        // 计时器位于区块卡内，卡底已是 panelRaised；这里改用叠加色，否则与卡底同色、控件边界消失。
        color = if (finished) tokens.colors.dangerFill else tokens.colors.text.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(tokens.radii.highlight, tokens.colors.text.copy(alpha = 0.16f)),
        modifier = Modifier.padding(top = tokens.spacing.s1),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        ) {
            // 固定长宽的时间显示（HH:MM:SS，等宽字体，宽度不随数字变化）。
            Box(modifier = Modifier.width(StepTimerTextWidth), contentAlignment = Alignment.Center) {
                Text(
                    text = "%02d:%02d:%02d".format(remaining / 3600, (remaining % 3600) / 60, remaining % 60),
                    style = tokens.type.numeric.copy(fontSize = tokens.type.titleS.fontSize, lineHeight = tokens.type.titleS.lineHeight),
                    color = if (finished) tokens.colors.onDangerFill else tokens.colors.text,
                    maxLines = 1,
                )
            }
            if (running) {
                IconButton(
                    onClick = { running = false },
                    enabled = !finished,
                    modifier = Modifier.size(tokens.spacing.hitMin),
                ) {
                    PauseBars(stringResource(R.string.nutrition_timer_pause))
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

/** 等宽时间显示所需的最小宽度：8 个字符（含冒号）在 monospace 下的安全值。 */
private val StepTimerTextWidth = 124.dp

/** 搜索面板限高：给下方分类栏与结果列表留出空间，超出部分在面板内滚动。 */
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
    val tokens = LocalAppTokens.current
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(tokens.spacing.hitMin)) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(tokens.spacing.s5), tint = tokens.colors.text)
    }
}

/** 暂停图标（两竖条），核心图标集无 Pause 时的替代。 */
@Composable
private fun PauseBars(contentDescription: String) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(tokens.spacing.s1)
                    .height(tokens.spacing.s5)
                    .background(tokens.colors.text, RoundedCornerShape(tokens.radii.highlight)),
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, textStyle: androidx.compose.ui.text.TextStyle = LocalAppTokens.current.type.body) {
    val tokens = LocalAppTokens.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s1)) {
        Text(label, style = textStyle, color = tokens.colors.textMuted)
        Spacer(Modifier.width(tokens.spacing.s2))
        // 值列占满剩余宽度，长文本换行而不是被裁掉。
        Text(value, style = textStyle, modifier = Modifier.weight(1f))
    }
}

/** 头部身份卡（无标题区）。底材与 [DetailSectionCard] 一致，避免同一屏出现两种卡片明度。 */
@Composable
private fun DetailPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(AppSpacingDefault.s3),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.stroke, tokens.colors.border, shape)
            .padding(contentPadding),
        content = content,
    )
}

/** 详情空态：说明「为什么空、下一步做什么」，不留一片空白（§6.2）。 */
@Composable
private fun DetailEmptyState(
    iconRes: Int,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = tokens.spacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(tokens.spacing.s6), tint = tokens.colors.disabled)
        Spacer(Modifier.height(tokens.spacing.s3))
        Text(title, style = tokens.type.body, color = tokens.colors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(tokens.spacing.s1))
        Text(description, style = tokens.type.caption, color = tokens.colors.textMuted, textAlign = TextAlign.Center)
    }
}

private data class FoodProfileRow(
    val key: String,
    @param:StringRes val labelRes: Int?,
    val value: String,
    val classification: GlycemicClassification? = null,
    val labelText: String? = null,
    val nrvPercent: String? = null,
)

@Composable
private fun HealthMetricsTable(
    metrics: com.woshiwangnima.healthdietpro.model.food.FoodHealthMetrics,
    noData: String,
) {
    val gi = metrics.glycemicIndex
    val gl = metrics.glycemicLoadPer100g
    val tokens = LocalAppTokens.current
    val border = tokens.colors.border
    // 外框由所属区块卡提供，这里只画表头底色与网格线，避免卡内卡的双层描边（§2.7 结构层）。
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.spacing.s7)
                .background(tokens.colors.text.copy(alpha = 0.06f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HealthMetricHeader(stringResource(R.string.nutrition_profile_item), Modifier.weight(1.3f))
            HealthMetricHeader(stringResource(R.string.nutrition_profile_amount), Modifier.weight(.9f))
            HealthMetricHeader(stringResource(R.string.nutrition_profile_classification), Modifier.weight(.5f))
            HealthMetricHeader(stringResource(R.string.nutrition_metric_glycemic_icon), Modifier.width(tokens.spacing.s8))
        }
        // 高度由内容撑开（IntrinsicSize.Min），字号放大到 150% 时不再被固定 184 dp 裁切。
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Column(Modifier.weight(1f)) {
                HealthMetricDataRow(stringResource(R.string.nutrition_metric_gi), gi?.let { "${it.value.formatTableValue()} ${it.unit}" } ?: noData, gi?.let { classifyGlycemicIndex(it.value) }, border)
                HealthMetricDataRow(stringResource(R.string.nutrition_metric_gl), gl?.let { "${it.value.formatTableValue()} ${it.unit}" } ?: noData, gl?.let { classifyGlycemicLoad(it.value) }, border)
                HealthMetricDataRow(stringResource(R.string.nutrition_metric_inflammatory_potential), metrics.inflammatoryPotential?.let { "${it.value.formatTableValue()} ${it.unit}" } ?: noData, null, border)
            }
            Column(Modifier.width(tokens.spacing.s8).fillMaxHeight()) {
                Box(Modifier.weight(2f).fillMaxWidth().border(tokens.radii.highlight, border), contentAlignment = Alignment.Center) {
                    GlycemicGlass(glycemicLevel(gi?.value, gl?.value))
                }
                Box(Modifier.weight(1f).fillMaxWidth().border(tokens.radii.highlight, border))
            }
        }
    }
}

@Composable
private fun HealthMetricHeader(text: String, modifier: Modifier) {
    Box(modifier.padding(horizontal = AppSpacingDefault.s1, vertical = AppSpacingDefault.s1), contentAlignment = Alignment.Center) {
        Text(text, style = LocalAppTokens.current.type.caption, color = LocalAppTokens.current.colors.textMuted, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun HealthMetricDataRow(label: String, value: String, classification: GlycemicClassification?, border: androidx.compose.ui.graphics.Color) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = tokens.spacing.s7).border(tokens.radii.highlight, border),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1.3f).padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1), style = tokens.type.bodyS, maxLines = 2)
        Text(value, modifier = Modifier.weight(.9f).padding(horizontal = tokens.spacing.s1), style = tokens.type.numeric, textAlign = TextAlign.Center, maxLines = 1)
        Box(Modifier.weight(.5f), contentAlignment = Alignment.Center) {
            classification?.let { GlycemicClassificationText(it) }
        }
    }
}

@Composable
private fun HealthMetricInfoCard(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    description: String,
    bands: List<RangeBand<Double, GlycemicClassification>>? = null,
    note: String? = null,
) {
    val tokens = LocalAppTokens.current
    AppSectionCard(iconRes = iconRes, title = title) {
        Text(description, style = tokens.type.bodyS, color = tokens.colors.textMuted)
        bands?.let { MetricClassificationTable(it, Modifier.padding(top = tokens.spacing.s2)) }
        note?.let {
            Text(
                text = it,
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
                modifier = Modifier.padding(top = tokens.spacing.s2),
            )
        }
    }
}

/** 详情页三处问号各自对应一张说明抽屉：标题与副标题只在这里定，避免三处各写一遍。 */
private enum class NutritionHelpTopic(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
) {
    HealthMetrics(R.string.nutrition_health_metrics_help, R.string.nutrition_health_metrics_help_intro),
    Nutrients(R.string.nutrition_help_nutrients_title, R.string.nutrition_help_nutrients_intro),
    Energy(R.string.nutrition_help_energy_title, R.string.nutrition_help_energy_intro),
}

/** 区块卡标题右侧的问号按钮：热区 48 dp（移动规范 01 §三），三处统一用同一个图标与色调。 */
@Composable
private fun HelpIconButton(description: String, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    IconButton(onClick = onClick, modifier = Modifier.size(tokens.spacing.hitMin)) {
        Icon(
            painter = painterResource(R.drawable.ic_help),
            contentDescription = description,
            modifier = Modifier.size(tokens.spacing.s5),
            tint = tokens.colors.textMuted,
        )
    }
}

/**
 * 说明板块：一张区块卡讲清一个概念，顺序固定为「定义 → 要点 → 公式 → 举例 → 备注」。
 * 公式用等宽字体加叠色底，举例单独成块，便于对照自己手上的数字。
 */
@Composable
private fun HelpSectionCard(
    @androidx.annotation.DrawableRes iconRes: Int,
    title: String,
    paragraphs: List<String> = emptyList(),
    bullets: List<String> = emptyList(),
    formula: String? = null,
    example: String? = null,
    note: String? = null,
) {
    val tokens = LocalAppTokens.current
    AppSectionCard(iconRes = iconRes, title = title) {
        paragraphs.forEachIndexed { index, text ->
            Text(
                text = text,
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
                modifier = Modifier.padding(top = if (index == 0) 0.dp else tokens.spacing.s2),
            )
        }
        bullets.forEach { text ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = tokens.spacing.s2)) {
                Text("·", style = tokens.type.bodyS, color = tokens.colors.textMuted)
                Text(
                    text = text,
                    style = tokens.type.bodyS,
                    color = tokens.colors.text,
                    modifier = Modifier.padding(start = tokens.spacing.s2),
                )
            }
        }
        formula?.let {
            HelpFormulaBlock(it, Modifier.padding(top = tokens.spacing.s3))
        }
        example?.let {
            HelpExampleBlock(
                label = stringResource(R.string.nutrition_help_example_label),
                text = it,
                modifier = Modifier.padding(top = tokens.spacing.s2),
            )
        }
        note?.let {
            Text(
                text = it,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                modifier = Modifier.padding(top = tokens.spacing.s2),
            )
        }
    }
}

@Composable
private fun HelpFormulaBlock(text: String, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(concentricRadius(tokens.radii.container, tokens.spacing.s3))
    Text(
        text = text,
        style = tokens.type.numeric.copy(
            fontSize = tokens.type.bodyS.fontSize,
            lineHeight = tokens.type.bodyS.lineHeight,
        ),
        color = tokens.colors.text,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.text.copy(alpha = 0.06f))
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
    )
}

/** 举例块：用主色 8% 底 + 35% 描边，与灰色的公式块区分开，读者一眼能找到「代入数字」的那段。 */
@Composable
private fun HelpExampleBlock(label: String, text: String, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(concentricRadius(tokens.radii.container, tokens.spacing.s3))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.primary.copy(alpha = 0.08f))
            .border(tokens.radii.highlight, tokens.colors.primary.copy(alpha = 0.35f), shape)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
    ) {
        Text(label, style = tokens.type.caption, color = tokens.colors.primary)
        Text(
            text = text,
            style = tokens.type.bodyS,
            color = tokens.colors.text,
            modifier = Modifier.padding(top = tokens.spacing.s1),
        )
    }
}

@Composable
private fun EnergyShareHelpCard() {
    HelpSectionCard(
        iconRes = R.drawable.ic_energy_distribution,
        title = stringResource(R.string.nutrition_help_energy_ratio_title),
        paragraphs = listOf(stringResource(R.string.nutrition_help_energy_ratio_definition)),
        bullets = listOf(stringResource(R.string.nutrition_help_energy_ratio_guideline)),
        formula = stringResource(R.string.nutrition_help_energy_ratio_formula),
        example = stringResource(R.string.nutrition_help_energy_ratio_example),
        note = stringResource(R.string.nutrition_help_energy_ratio_note),
    )
}

/** 能量单位说明：营养信息与营养素供能两个抽屉共用同一张卡，避免两处各写一份、日后改漏。 */
@Composable
private fun EnergyUnitHelpCard() {
    HelpSectionCard(
        iconRes = R.drawable.ic_description,
        title = stringResource(R.string.nutrition_help_unit_title),
        paragraphs = listOf(
            stringResource(R.string.nutrition_help_unit_kcal),
            stringResource(R.string.nutrition_help_unit_kj),
        ),
        bullets = listOf(stringResource(R.string.nutrition_help_unit_conversion)),
        example = stringResource(R.string.nutrition_help_unit_example),
    )
}

@Composable
private fun EdiblePortionHelpCard() {
    HelpSectionCard(
        iconRes = R.drawable.ic_weight,
        title = stringResource(R.string.nutrition_help_edible_title),
        paragraphs = listOf(stringResource(R.string.nutrition_help_edible_meaning)),
        bullets = listOf(stringResource(R.string.nutrition_help_edible_record)),
        formula = stringResource(R.string.nutrition_help_edible_formula),
        example = stringResource(R.string.nutrition_help_edible_example),
        note = stringResource(R.string.nutrition_help_edible_note),
    )
}

@Composable
private fun NrvHelpCard() {
    HelpSectionCard(
        iconRes = R.drawable.ic_nutrients,
        title = stringResource(R.string.nutrition_help_nrv_title),
        paragraphs = listOf(
            stringResource(R.string.nutrition_help_nrv_meaning),
            stringResource(R.string.nutrition_help_nrv_percent),
        ),
        formula = stringResource(R.string.nutrition_help_nrv_formula),
        example = stringResource(R.string.nutrition_help_nrv_example),
        note = stringResource(R.string.nutrition_help_nrv_note),
    )
}

/** 分级表：不铺自己的底，只用叠加透明度画表头与行线，放在任何卡底上都不会与卡底同色（同 `AppDataTable`）。 */
@Composable
private fun MetricClassificationTable(
    bands: List<RangeBand<Double, GlycemicClassification>>,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    // 同心圆角：外框 4 dp、内边距 12 dp，内层取 0，避免内外两圈弧线各画各的（规范 07 检查项 11）。
    val shape = RoundedCornerShape(concentricRadius(tokens.radii.container, tokens.spacing.s3))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(tokens.radii.highlight, tokens.colors.border, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.colors.text.copy(alpha = 0.06f))
                .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
        ) {
            Text(
                text = stringResource(R.string.nutrition_profile_range),
                modifier = Modifier.weight(1f),
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
            )
            Text(
                text = stringResource(R.string.nutrition_profile_classification),
                modifier = Modifier.weight(1f),
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
            )
        }
        bands.forEachIndexed { index, band ->
            if (index > 0) {
                HorizontalDivider(color = tokens.colors.border, thickness = tokens.radii.highlight)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(metricRangeText(band), modifier = Modifier.weight(1f), style = tokens.type.bodyS, color = tokens.colors.text)
                Box(modifier = Modifier.weight(1f)) { GlycemicClassificationText(band.value) }
            }
        }
    }
}

@Composable
private fun GlycemicClassificationText(classification: GlycemicClassification) {
    val tokens = LocalAppTokens.current
    // 语义色只表达状态（§2.3）：低 = success，中 = warning，高 = danger；文字标签同时存在，不靠颜色单独承载。
    val color = when (classification) {
        GlycemicClassification.Low -> tokens.colors.success
        GlycemicClassification.Medium -> tokens.colors.warning
        GlycemicClassification.High -> tokens.colors.danger
    }
    val labelRes = when (classification) {
        GlycemicClassification.Low -> R.string.nutrition_classification_low
        GlycemicClassification.Medium -> R.string.nutrition_classification_medium
        GlycemicClassification.High -> R.string.nutrition_classification_high
    }
    Text(stringResource(labelRes), style = LocalAppTokens.current.type.bodyS, color = color)
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
    nrvReference: NrvReference?,
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
                nrvPercent = nrvReference?.percent(code, amount, multiplier)?.let { "${it.formatTableValue()}%" },
            )
        }
}

private fun Double.formatTableValue(): String = "%.1f".format(java.util.Locale.ROOT, this)

@Composable
private fun FoodServingSelector(
    servings: List<FoodServing>,
    selectedServingId: String,
    tableBasisGrams: Double?,
    onSelected: (String) -> Unit,
) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    val selectedServing = servings.first { it.id == selectedServingId }
    val selectedLabel = if (selectedServing.id == "per_100g") {
        stringResource(R.string.nutrition_serving_100g_edible)
    } else {
        selectedServing.displayLabel(language)
    }
    val options = servings.map { serving ->
        AppDropdownOption(
            id = serving.id,
            label = if (serving.id == "per_100g") stringResource(R.string.nutrition_serving_100g_edible) else serving.displayLabel(language),
        )
    }
    Column(Modifier.fillMaxWidth()) {
        AppDropdownField(
            label = stringResource(R.string.nutrition_measurement_basis),
            value = selectedLabel,
            options = options,
            onSelect = { onSelected(it.id) },
            modifier = Modifier.fillMaxWidth().padding(top = AppSpacingDefault.s2),
            showOptionDividers = true,
        )
        tableBasisGrams?.let { basisGrams ->
            val edibleGrams = basisGrams * selectedServing.ratioToTable
            Text(
                text = stringResource(R.string.nutrition_serving_edible_grams, formatGrams(edibleGrams)),
                modifier = Modifier.fillMaxWidth().padding(top = AppSpacingDefault.s1),
                style = LocalAppTokens.current.type.caption,
                color = LocalAppTokens.current.colors.textMuted,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** 克重展示：整数带千分位，非整数保留一位小数。 */
private fun formatGrams(grams: Double): String {
    val rounded = kotlin.math.round(grams)
    return if (kotlin.math.abs(grams - rounded) < 0.05) {
        formatGroupedInteger(rounded)
    } else {
        "%.1f".format(java.util.Locale.ROOT, grams)
    }
}

@Composable
private fun NutritionEnergyAndExercise(resolved: ResolvedNutrition?, serving: FoodServing, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    val kcal = resolved.energyKilocalories(serving.ratioToTable)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (kcal == null) {
            Text(
                text = stringResource(R.string.nutrition_energy_empty),
                modifier = Modifier.padding(vertical = tokens.spacing.s2),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                textAlign = TextAlign.Center,
            )
            return@Column
        }
        val kilojoules = UnitConverter.fromBase("energy", kcal.toFloat(), "kj").toDouble()
        val steps = (kcal / 0.04).toInt().coerceAtLeast(0)
        Text(
            stringResource(
                R.string.nutrition_energy_kcal_kj,
                formatGroupedInteger(kcal),
                formatGroupedInteger(kilojoules),
            ),
            style = tokens.type.numeric.copy(fontSize = tokens.type.titleS.fontSize, lineHeight = tokens.type.titleS.lineHeight),
            color = tokens.colors.text,
        )
        Row(
            modifier = Modifier.heightIn(min = tokens.spacing.hitMin).clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.nutrition_exercise_consumption_steps, formatGroupedInteger(steps)), style = tokens.type.caption, color = tokens.colors.textMuted)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.nutrition_exercise_consumption), modifier = Modifier.size(16.dp), tint = tokens.colors.textMuted)
        }
    }
}

@Composable
private fun NrvReferenceHeader(
    selected: NrvReference?,
    references: List<NrvReference>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val tokens = LocalAppTokens.current
    val canSwitch = references.size > 1
    Box {
        Row(
            // 整个表头单元格可点，热区补到 48 dp（移动规范 01 §三）。
            // 原来只有「NRV + 箭头」那一条 42×20 dp 的文字可点，手指基本点不中。
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.spacing.hitMin)
                .clickable(enabled = canSwitch, role = androidx.compose.ui.semantics.Role.DropdownList) {
                    expanded = true
                },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppDataTableHeaderText(stringResource(R.string.nutrition_profile_nrv))
            if (canSwitch) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.spacing.s4),
                    tint = LocalAppTokens.current.colors.textMuted,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            references.forEach { reference ->
                DropdownMenuItem(
                    text = { Text(nrvReferenceLabel(reference.id)) },
                    onClick = { onSelected(reference.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun nrvReferenceLabel(id: String): String = when (id) {
    DriNrvRepository.ADULT_REFERENCE_ID -> stringResource(R.string.nutrition_nrv_adult_standard)
    else -> stringResource(R.string.nutrition_nrv_current_user)
}

@Composable
private fun ExerciseExpenditureScreen(request: NutritionExerciseRequest, onBack: () -> Unit) {
    BaseScreen(
        title = stringResource(R.string.nutrition_exercise_consumption),
        onBack = onBack,
        includeStatusBarPadding = false,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = AppSpacingDefault.page, vertical = AppSpacingDefault.s3)) {
            request.servings.forEach { serving ->
                Text(
                    text = stringResource(R.string.nutrition_energy_for_basis, serving.kilocalories, 1.0, serving.label),
                    style = LocalAppTokens.current.type.body,
                    modifier = Modifier.padding(vertical = AppSpacingDefault.s1),
                )
            }
        }
    }
}


@Composable
private fun FoodImagePreview(image: ImageBitmap, onDismiss: () -> Unit) {
    val tokens = LocalAppTokens.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(tokens.radii.container),
            color = tokens.colors.panel,
            border = androidx.compose.foundation.BorderStroke(tokens.radii.stroke, tokens.colors.border),
        ) {
            Column(Modifier.fillMaxSize().padding(tokens.spacing.s3)) {
                // 灯箱必须给出可见的关闭入口（§5.5.7、§7.3），不只靠点按背景与返回键。
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.nutrition_image_preview_close_hint),
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(tokens.spacing.hitMin)) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.nutrition_image_preview_close), tint = tokens.colors.text)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    FoodImage(image, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
internal fun FoodImage(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    androidx.compose.foundation.Image(
        bitmap = image,
        contentDescription = stringResource(R.string.nutrition_food_image),
        modifier = modifier.clip(RoundedCornerShape(AppRadiiDefault.label)),
        contentScale = contentScale,
        alignment = Alignment.Center,
    )
}

@Composable
private fun NutritionActionButton(iconRes: Int, text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalAppTokens.current.colors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(iconRes), null, modifier = Modifier.size(16.dp), tint = LocalAppTokens.current.colors.textMuted)
            Spacer(Modifier.width(4.dp))
            Text(text, style = TextStyle(fontSize = FontTokens.body), color = LocalAppTokens.current.colors.textMuted)
        }
    }
}

@Composable
private fun AddTagDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) { var label by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.nutrition_add_tag)) }, text = { OutlinedTextField(label, { label = it }, label = { Text(stringResource(R.string.nutrition_tag_name)) }) }, confirmButton = { androidx.compose.material3.TextButton({ onAdd(label) }) { Text(stringResource(R.string.body_record_save)) } }, dismissButton = { androidx.compose.material3.TextButton(onDismiss) { Text(stringResource(R.string.body_record_cancel)) } }) }

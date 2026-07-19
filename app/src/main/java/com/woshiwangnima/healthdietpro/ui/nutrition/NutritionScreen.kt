package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
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
import com.woshiwangnima.healthdietpro.common.ui.AppInfoSection
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.food.Food
import com.woshiwangnima.healthdietpro.model.food.FoodCategories
import com.woshiwangnima.healthdietpro.model.food.FoodServing

@Composable
internal fun NutritionScreen(viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when {
        state.managementScreen != null -> NutritionManagementPlaceholder(requireNotNull(state.managementScreen), viewModel::closeManagement)
        state.comparisonReturnTarget != null -> ComparisonPlaceholder(onBack = viewModel::closeComparison)
        state.selectedFood != null -> FoodDetailScreen(requireNotNull(state.selectedFood), viewModel.foodImages, viewModel::closeFood) { viewModel.openComparison(NutritionDestination.FoodDetail) }
        else -> FoodBrowseScreen(state, viewModel, modifier)
    }
}

@Composable
private fun FoodBrowseScreen(state: NutritionUiState, viewModel: NutritionViewModel, modifier: Modifier) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    val foods = viewModel.filteredFoods(language)
    var addingTag by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.keyword,
            onValueChange = viewModel::setKeyword,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
            placeholder = { Text(stringResource(R.string.nutrition_search_food), style = TextStyle(fontSize = FontTokens.caption), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)) },
            textStyle = TextStyle(fontSize = FontTokens.caption),
        )
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutritionActionButton(R.drawable.ic_add, stringResource(R.string.nutrition_add_custom_food)) { viewModel.openManagement(NutritionManagementScreen.CustomFood) }
            NutritionActionButton(R.drawable.ic_chart, stringResource(R.string.nutrition_add_custom_meal_set)) { viewModel.openManagement(NutritionManagementScreen.MealSet) }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CategorySidebar(state, viewModel)
            Column(modifier = Modifier.weight(1f)) {
                TagRow(stringResource(R.string.nutrition_system_tags), listOf("common" to stringResource(R.string.nutrition_tag_common), "favorite" to stringResource(R.string.nutrition_tag_favorite), "recent" to stringResource(R.string.nutrition_tag_recent)), state.selectedSystemTag?.let(::setOf).orEmpty(), viewModel::toggleSystemTag)
                TagRow(stringResource(R.string.nutrition_user_tags), state.userTags.map { it.id to it.label }, state.selectedUserTags, viewModel::toggleUserTag, { addingTag = true })
                if (foods.isEmpty()) Text(stringResource(R.string.nutrition_no_foods), modifier = Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { items(foods, key = { it.id }) { FoodRow(it, language, viewModel.foodImages, viewModel::openFood) } }
            }
        }
    }
    if (addingTag) AddTagDialog({ addingTag = false }) { viewModel.addUserTag(it); addingTag = false }
}

@Composable
private fun CategorySidebar(state: NutritionUiState, viewModel: NutritionViewModel) {
    val children = state.selectedRoot?.let { root -> FoodCategories.children.filter { it.parentTag == root } }.orEmpty()
    Surface(
        modifier = Modifier.width(80.dp).fillMaxHeight(),
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
        item { CompactFilterChip(state.selectedCustomFilter == CustomFoodFilter.Food, { viewModel.selectCustomFilter(CustomFoodFilter.Food) }, Modifier.fillMaxWidth()) { CategoryLabel(stringResource(R.string.nutrition_custom_food)) } }
        item { Spacer(Modifier.height(10.dp)) }
        item { CompactFilterChip(state.selectedCustomFilter == CustomFoodFilter.MealSet, { viewModel.selectCustomFilter(CustomFoodFilter.MealSet) }, Modifier.fillMaxWidth()) { CategoryLabel(stringResource(R.string.nutrition_custom_meal_set)) } }
        item { Spacer(Modifier.height(10.dp)) }
        items(FoodCategories.roots, key = { it.tag }) { category ->
            CompactFilterChip(category.tag == state.selectedRoot, { viewModel.selectRoot(category.tag) }, Modifier.fillMaxWidth()) { CategoryLabel(stringResource(category.labelRes)) }
        }
    }
}

@Composable
private fun CategoryLabel(text: String) {
    TextOverflowText(text = text, style = TextStyle(fontSize = FontTokens.body), maxLines = 1)
}

@Composable
private fun TagRow(title: String, tags: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit, onAdd: (() -> Unit)? = null) {
    Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { tags.forEach { (id, label) -> CompactFilterChip(id in selected, { onToggle(id) }) { Text(label, style = TextStyle(fontSize = FontTokens.body)) } } }
            onAdd?.let { IconButton(onClick = it) { Icon(Icons.Filled.Add, stringResource(R.string.nutrition_add_tag)) } }
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

@Composable
private fun FoodRow(food: Food, language: String, imageStore: FoodImageStore, onClick: (Food) -> Unit) {
    val table = food.nutritionTables[food.servingsOrDefault().first().nutritionTableKey] ?: food.nutrientTable("standard.100g")
    val energy = table.nutrients["ENERGY"]?.value ?: 0.0
    val basis = table.basis
    val image = imageStore.image(food.image?.localKey)
    var previewing by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(food) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        FoodImage(image, Modifier.size(64.dp).clickable { previewing = true })
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(food.displayName(language), style = TextStyle(fontSize = FontTokens.subtitle))
            Text(stringResource(R.string.nutrition_energy_for_basis, energy, basis.value, basis.unitId), style = MaterialTheme.typography.bodyMedium)
            TextOverflowText(
                text = food.categoryTags.joinToString(" / "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (previewing) FoodImagePreview(image, onDismiss = { previewing = false })
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
private fun FoodDetailScreen(food: Food, imageStore: FoodImageStore, onBack: () -> Unit, onCompare: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var selectedServingId by remember(food.id) { mutableStateOf(food.servingsOrDefault().first().id) }
    var previewing by remember { mutableStateOf(false) }
    var showHealthMetricsHelp by remember { mutableStateOf(false) }
    BaseScreen(title = stringResource(R.string.nutrition_food_detail_title), onBack = onBack) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FoodImage(imageStore.image(food.image?.localKey), Modifier.size(96.dp).clickable { previewing = true })
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                val language = LocalConfiguration.current.locales[0]?.language ?: "en"
                Text(food.displayName(language), style = MaterialTheme.typography.headlineSmall)
                food.allNames(language).drop(1).takeIf { it.isNotEmpty() }?.let { aliases ->
                    Text(aliases.joinToString(" / "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                food.displayDescription(language).takeIf { it.isNotBlank() }?.let { description ->
                    Text(description, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = { }) { Icon(Icons.Filled.FavoriteBorder, stringResource(R.string.nutrition_favorite)) }
            IconButton(onClick = onCompare) { Icon(painterResource(R.drawable.ic_chart), stringResource(R.string.nutrition_compare)) }
        }
        EqualWidthSegmentedTabs(
            tabs = listOf(EqualWidthTab(R.string.nutrition_tab_profile), EqualWidthTab(R.string.nutrition_tab_ranking), EqualWidthTab(R.string.nutrition_tab_estimate)),
            selectedIndex = tab,
            onSelected = { tab = it },
            modifier = Modifier.padding(top = 16.dp),
        )
        if (tab == 0) Column(Modifier.weight(1f).padding(top = 12.dp)) {
            DetailSectionTitle(R.drawable.ic_chart, stringResource(R.string.nutrition_health_metrics)) {
                IconButton(onClick = { showHealthMetricsHelp = true }) {
                    Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.nutrition_health_metrics_help))
                }
            }
            AppDataTable(
                rows = food.healthMetricRows(),
                columns = listOf(
                    AppDataTableColumn("nutrient", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_item)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(stringResource(it.labelRes)) },
                    AppDataTableColumn("amount", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_amount)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(it.value) },
                ),
                rowKey = { _, row -> row.key },
                modifier = Modifier.fillMaxWidth().height(154.dp),
            )
            DetailSectionTitle(R.drawable.ic_list, stringResource(R.string.nutrition_nutrients), Modifier.padding(top = 12.dp))
            FoodServingSelector(
                servings = food.servingsOrDefault(),
                selectedServingId = selectedServingId,
                onSelected = { selectedServingId = it },
            )
            val selectedServing = food.servingsOrDefault().first { it.id == selectedServingId }
            AppDataTable(
                rows = food.nutrientRows(selectedServing),
                columns = listOf(
                    AppDataTableColumn("nutrient", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_item)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(stringResource(it.labelRes)) },
                    AppDataTableColumn("amount", { AppDataTableHeaderText(stringResource(R.string.nutrition_profile_amount)) }, ColumnWidth.Flex(1f, 110.dp)) { AppDataTableText(it.value) },
                ),
                rowKey = { _, row -> row.key },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        else Text(stringResource(R.string.nutrition_detail_placeholder), modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (previewing) FoodImagePreview(imageStore.image(food.image?.localKey), onDismiss = { previewing = false })
    if (showHealthMetricsHelp) {
        AppInfoDialog(
            title = stringResource(R.string.nutrition_health_metrics_help),
            sections = listOf(
                AppInfoSection(stringResource(R.string.nutrition_metric_gi), stringResource(R.string.nutrition_metric_gi_help)),
                AppInfoSection(stringResource(R.string.nutrition_metric_gl), stringResource(R.string.nutrition_metric_gl_help)),
                AppInfoSection(stringResource(R.string.nutrition_metric_inflammatory_potential), stringResource(R.string.nutrition_metric_inflammatory_potential_help)),
            ),
            onDismiss = { showHealthMetricsHelp = false },
        )
    }
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
)

private fun Food.healthMetricRows(): List<FoodProfileRow> = listOfNotNull(
        healthMetrics.glycemicIndex?.let { FoodProfileRow("gi", R.string.nutrition_metric_gi, "${it.value} ${it.unit}") },
        healthMetrics.glycemicLoadPer100g?.let { FoodProfileRow("gl", R.string.nutrition_metric_gl, "${it.value} ${it.unit}") },
        healthMetrics.inflammatoryPotential?.let { FoodProfileRow("inflammatory", R.string.nutrition_metric_inflammatory_potential, "${it.value} ${it.unit}") },
    )

private fun Food.nutrientRows(serving: FoodServing): List<FoodProfileRow> {
    return nutrientTable(serving.nutritionTableKey).nutrients.entries.map { (code, amount) ->
        val multiplier = serving.ratioToTable
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
private fun FoodImage(image: ImageBitmap, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        bitmap = image,
        contentDescription = stringResource(R.string.nutrition_food_image),
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun ComparisonPlaceholder(onBack: () -> Unit) {
    BaseScreen(title = stringResource(R.string.nutrition_comparison_title), onBack = onBack) { padding ->
        Text(stringResource(R.string.nutrition_detail_placeholder), modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp))
    }
}

@Composable
private fun NutritionManagementPlaceholder(screen: NutritionManagementScreen, onBack: () -> Unit) {
    val title = when (screen) {
        NutritionManagementScreen.CustomFood -> stringResource(R.string.nutrition_custom_food)
        NutritionManagementScreen.MealSet -> stringResource(R.string.nutrition_custom_meal_set)
    }
    BaseScreen(title = title, onBack = onBack) { padding ->
        Text(stringResource(R.string.nutrition_detail_placeholder), modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp))
    }
}

@Composable
private fun AddTagDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) { var label by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.nutrition_add_tag)) }, text = { OutlinedTextField(label, { label = it }, label = { Text(stringResource(R.string.nutrition_tag_name)) }) }, confirmButton = { androidx.compose.material3.TextButton({ onAdd(label) }) { Text(stringResource(R.string.body_record_save)) } }, dismissButton = { androidx.compose.material3.TextButton(onDismiss) { Text(stringResource(R.string.body_record_cancel)) } }) }

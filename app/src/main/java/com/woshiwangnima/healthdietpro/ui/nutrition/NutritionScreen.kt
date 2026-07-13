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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.FontTokens
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.food.Food
import com.woshiwangnima.healthdietpro.model.food.FoodCategories

@Composable
internal fun NutritionScreen(viewModel: NutritionViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when {
        state.managementScreen != null -> NutritionManagementPlaceholder(requireNotNull(state.managementScreen), viewModel::closeManagement)
        state.showComparison -> ComparisonPlaceholder(onBack = viewModel::closeComparison)
        state.selectedFood != null -> FoodDetailScreen(requireNotNull(state.selectedFood), viewModel::closeFood, viewModel::openComparison)
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
                else LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { items(foods, key = { it.id }) { FoodRow(it, language, viewModel::openFood) } }
            }
        }
    }
    if (addingTag) AddTagDialog({ addingTag = false }) { viewModel.addUserTag(it); addingTag = false }
}

@Composable
private fun CategorySidebar(state: NutritionUiState, viewModel: NutritionViewModel) {
    val children = state.selectedRoot?.let { root -> FoodCategories.children.filter { it.parentTag == root } }.orEmpty()
    Row(modifier = Modifier.width(80.dp).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LazyColumn(modifier = Modifier.width(if (children.isEmpty()) 80.dp else 38.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                CompactFilterChip(state.selectedCustomFilter == CustomFoodFilter.Food, { viewModel.selectCustomFilter(CustomFoodFilter.Food) }, Modifier.fillMaxWidth()) {
                    CategoryLabel(stringResource(R.string.nutrition_custom_food))
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                CompactFilterChip(state.selectedCustomFilter == CustomFoodFilter.MealSet, { viewModel.selectCustomFilter(CustomFoodFilter.MealSet) }, Modifier.fillMaxWidth()) {
                    CategoryLabel(stringResource(R.string.nutrition_custom_meal_set))
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
            items(FoodCategories.roots, key = { it.tag }) { category -> CompactFilterChip(category.tag == state.selectedRoot, { viewModel.selectRoot(category.tag) }, Modifier.fillMaxWidth()) { CategoryLabel(stringResource(category.labelRes)) } }
        }
        if (children.isNotEmpty()) LazyColumn(modifier = Modifier.width(36.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(children, key = { it.tag }) { category -> CompactFilterChip(category.tag in state.selectedChildren, { viewModel.toggleChild(category.tag) }, Modifier.fillMaxWidth()) { CategoryLabel(stringResource(category.labelRes)) } }
        }
    }
}

@Composable
private fun CategoryLabel(text: String) {
    TextOverflowText(text = text, style = TextStyle(fontSize = FontTokens.body), maxLines = 1, overflowMode = "marquee", autoShrinkEnabled = false)
}

@Composable
private fun TagRow(title: String, tags: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit, onAdd: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { tags.forEach { (id, label) -> CompactFilterChip(id in selected, { onToggle(id) }) { Text(label, style = TextStyle(fontSize = FontTokens.body)) } } }
        onAdd?.let { IconButton(onClick = it) { Icon(Icons.Filled.Add, stringResource(R.string.nutrition_add_tag)) } }
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
private fun FoodRow(food: Food, language: String, onClick: (Food) -> Unit) {
    val energy = food.nutrientsPer100g["ENERGY"] ?: 0.0
    var previewing by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick(food) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondaryContainer).clickable { previewing = true }, contentAlignment = Alignment.Center) { Text(stringResource(R.string.nutrition_food_image), style = MaterialTheme.typography.labelSmall) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(food.displayName(language), style = TextStyle(fontSize = FontTokens.subtitle))
            Text(stringResource(R.string.nutrition_energy_per_100g, energy), style = MaterialTheme.typography.bodyMedium)
            Text(food.categoryTag.removePrefix("food."), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (previewing) FoodImagePreview(onDismiss = { previewing = false })
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
private fun FoodDetailScreen(food: Food, onBack: () -> Unit, onCompare: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var previewing by remember { mutableStateOf(false) }
    BaseScreen(title = stringResource(R.string.nutrition_food_detail_title), onBack = onBack) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer).clickable { previewing = true }, contentAlignment = Alignment.Center) { Text(stringResource(R.string.nutrition_food_image)) }
            Spacer(Modifier.width(12.dp)); Text(food.displayName(LocalConfiguration.current.locales[0]?.language ?: "en"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { }) { Icon(Icons.Filled.FavoriteBorder, stringResource(R.string.nutrition_favorite)) }
            IconButton(onClick = onCompare) { Icon(painterResource(R.drawable.ic_chart), stringResource(R.string.nutrition_compare)) }
        }
        Row(modifier = Modifier.padding(top = 16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(R.string.nutrition_tab_profile, R.string.nutrition_tab_ranking, R.string.nutrition_tab_estimate).forEachIndexed { index, label -> FilterChip(tab == index, { tab = index }, label = { Text(stringResource(label)) }) }
        }
        if (tab == 0) LazyColumn(modifier = Modifier.padding(top = 12.dp)) { items(food.nutrientsPer100g.entries.toList()) { (code, value) -> Text("$code  $value", modifier = Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyLarge) } }
        else Text(stringResource(R.string.nutrition_detail_placeholder), modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (previewing) FoodImagePreview(onDismiss = { previewing = false })
    }
}

@Composable
private fun FoodImagePreview(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.body_record_cancel)) } },
        text = {
            Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.nutrition_food_image), style = MaterialTheme.typography.titleMedium)
            }
        },
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

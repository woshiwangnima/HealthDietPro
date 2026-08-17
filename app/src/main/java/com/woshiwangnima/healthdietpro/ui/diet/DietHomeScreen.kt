package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangePreset
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.resolve
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.RecordTimeRangeFilter
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.coroutines.delay

@Composable
internal fun DietHomeScreen(
    uiState: DietUiState,
    onAdd: () -> Unit,
    onEdit: (DietRecord) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            DetailTabItem("statistics", R.string.diet_tab_statistics, R.drawable.ic_chart),
            DetailTabItem("records", R.string.diet_tab_records, R.drawable.ic_list),
        )
    }
    val context = LocalContext.current
    var selectedTab by remember {
        mutableIntStateOf(UserPrefs.current(context).getInt(DIET_SELECTED_TAB_KEY, 1))
    }
    LaunchedEffect(selectedTab) {
        UserPrefs.current(context).putInt(DIET_SELECTED_TAB_KEY, selectedTab)
    }
    BaseScreen(
        title = stringResource(R.string.diet_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
        actions = {
            androidx.compose.material3.IconButton(onClick = onSettings) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.diet_settings_title),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            AnimatedPageContent(selectedTab, Modifier.weight(1f), direction = { initial, target -> target - initial }) { tab ->
                if (tab == 0) {
                    DietStatisticsTab(uiState.records, uiState.goals)
                } else {
                    DietRecordsTab(uiState, onAdd, onEdit, onDelete)
                }
            }
            DetailTabBar(tabs, tabs[selectedTab].id) { item -> selectedTab = tabs.indexOf(item) }
        }
    }
}

@Composable
private fun DietRecordsTab(
    uiState: DietUiState,
    onAdd: () -> Unit,
    onEdit: (DietRecord) -> Unit,
    onDelete: (String) -> Unit,
) {
    var filterPeriod by remember { mutableStateOf<MealPeriod?>(null) }
    var deleting by remember { mutableStateOf<DietRecord?>(null) }
    var timeSelection: RecordTimeRangeSelection by remember {
        mutableStateOf(RecordTimeRangeSelection.Preset(RecordTimeRangePreset.TODAY))
    }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timeSelection is RecordTimeRangeSelection.Preset) {
        if (timeSelection !is RecordTimeRangeSelection.Preset) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000L - nowMillis % 60_000L)
        }
    }
    val range = timeSelection.resolve(nowMillis)
    val filtered = uiState.records.filter { record ->
        range.contains(record.mealStartAt) && (filterPeriod == null || record.mealPeriod == filterPeriod)
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppIconTextButton(stringResource(R.string.diet_add), R.drawable.ic_add, onAdd, Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { RecordTimeRangeFilter(timeSelection, { timeSelection = it }) }
            item { DietPeriodFilter(filterPeriod, onFilterPeriodChange = { filterPeriod = it }) }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.diet_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                }
            }
            items(filtered.sortedByDescending(DietRecord::mealStartAt), key = DietRecord::id) { record ->
                DietCard(
                    record = record,
                    onEdit = { onEdit(record) },
                    onDelete = { deleting = record },
                )
            }
        }
    }
    deleting?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.diet_delete_message, formatRecordTimestamp(record.mealStartAt, RecordTimePrecision.MINUTE))) },
            confirmButton = {
                TextButton(onClick = { onDelete(record.id); deleting = null }) { Text(stringResource(R.string.body_record_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun DietPeriodFilter(selected: MealPeriod?, onFilterPeriodChange: (MealPeriod?) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DietPeriodTag(
            text = stringResource(R.string.diet_filter_all),
            selected = selected == null,
            onClick = { onFilterPeriodChange(null) },
        )
        MealPeriod.entries.forEach { period ->
            DietPeriodTag(
                text = stringResource(period.displayRes()),
                selected = selected == period,
                onClick = { onFilterPeriodChange(if (selected == period) null else period) },
            )
        }
    }
}

@Composable
private fun DietPeriodTag(text: String, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        modifier = Modifier.width(PeriodTagWidth),
    ) {
        TextOverflowText(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

private val PeriodTagWidth = 96.dp

@Composable
private fun DietCard(
    record: DietRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val totalCalories = record.entries.sumOf { entry ->
        entry.resolvedNutrients["ENERGY"]?.value ?: 0.0
    }
    Surface(
        onClick = onEdit,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(record.mealPeriod.displayRes()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = stringResource(R.string.diet_summary_calories, formatCalories(totalCalories)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                androidx.compose.material3.IconButton(onClick = onEdit) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.diet_edit),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                androidx.compose.material3.IconButton(onClick = onDelete) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.diet_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(
                text = stringResource(
                    R.string.diet_card_summary,
                    formatRecordTimestamp(record.mealStartAt, RecordTimePrecision.MINUTE),
                    formatRecordTimestamp(record.mealEndAt, RecordTimePrecision.MINUTE),
                    record.entries.size,
                    formatGrams(record.entries.sumOf(DietFoodEntry::netWeightGrams)),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (record.note.isNotBlank()) {
                Text(
                    text = record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun MealPeriod.displayRes(): Int = when (this) {
    MealPeriod.PRE_BREAKFAST_SNACK -> R.string.diet_meal_period_pre_breakfast_snack
    MealPeriod.BREAKFAST -> R.string.diet_meal_period_breakfast
    MealPeriod.MID_MORNING_SNACK -> R.string.diet_meal_period_mid_morning_snack
    MealPeriod.LUNCH -> R.string.diet_meal_period_lunch
    MealPeriod.MID_AFTERNOON_SNACK -> R.string.diet_meal_period_mid_afternoon_snack
    MealPeriod.DINNER -> R.string.diet_meal_period_dinner
    MealPeriod.POST_DINNER_SNACK -> R.string.diet_meal_period_post_dinner_snack
}

internal fun formatCalories(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

internal fun formatGrams(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()} g" else "%.1f g".format(value)

internal fun FoodKind.displayRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.nutrition_kind_ingredient
    FoodKind.FOOD -> R.string.nutrition_kind_food
    FoodKind.DISH -> R.string.nutrition_kind_dish
}

private const val DIET_SELECTED_TAB_KEY = "diet_selected_tab_v1"
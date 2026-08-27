package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
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
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.RecordTimeRangeFilter
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.delay

@Composable
internal fun DietHomeScreen(
    uiState: DietUiState,
    onAdd: () -> Unit,
    onOpen: (DietRecord) -> Unit,
    onEdit: (DietRecord) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    recordFilterPeriod: MutableState<MealPeriod?>,
    recordTimeSelection: MutableState<RecordTimeRangeSelection>,
    recordListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            DetailTabItem("trend", R.string.diet_tab_trend, R.drawable.ic_chart),
            DetailTabItem("statistics", R.string.diet_tab_statistics, R.drawable.ic_diet),
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
                    DietStatisticsTab(
                        records = uiState.records,
                        goals = uiState.goals,
                        nutrientMetas = uiState.nutrientMetas,
                        onOpenMeal = onOpen,
                        trendOnly = true,
                    )
                } else if (tab == 1) {
                    DietStatisticsTab(
                        records = uiState.records,
                        goals = uiState.goals,
                        nutrientMetas = uiState.nutrientMetas,
                        onOpenMeal = onOpen,
                    )
                } else {
                    DietRecordsTab(uiState, onAdd, onOpen, onEdit, onDelete, recordFilterPeriod, recordTimeSelection, recordListState)
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
    onOpen: (DietRecord) -> Unit,
    onEdit: (DietRecord) -> Unit,
    onDelete: (String) -> Unit,
    recordFilterPeriod: MutableState<MealPeriod?>,
    recordTimeSelection: MutableState<RecordTimeRangeSelection>,
    recordListState: LazyListState,
) {
    var filterPeriod by recordFilterPeriod
    var deleting by remember { mutableStateOf<DietRecord?>(null) }
    var timeSelection: RecordTimeRangeSelection by recordTimeSelection
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
    val grouped = remember(filtered) {
        filtered.sortedByDescending(DietRecord::mealStartAt).groupBy { record ->
            Instant.ofEpochMilli(record.mealStartAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppIconTextButton(stringResource(R.string.diet_add), R.drawable.ic_add, onAdd, Modifier.fillMaxWidth())
        LazyColumn(
            state = recordListState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { RecordTimeRangeFilter(timeSelection, { timeSelection = it }) }
            item {
                MealPeriodSelectorBar(
                    selected = filterPeriod,
                    onPeriodSelected = { filterPeriod = it },
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.diet_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                }
            } else {
                grouped.forEach { (date, records) ->
                    item(key = "date_$date") { DietDateHeader(date) }
                    items(records, key = DietRecord::id) { record ->
                        DietCard(
                            record = record,
                            onOpen = { onOpen(record) },
                            onEdit = { onEdit(record) },
                            onDelete = { deleting = record },
                        )
                    }
                }
            }
        }
    }
    deleting?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.diet_delete_message, formatRecordTimestamp(record.mealStartAt, RecordTimePrecision.MINUTE))) },
            confirmButton = {
                TextButton(onClick = { onDelete(record.id); deleting = null }) {
                    Text(stringResource(R.string.body_record_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun DietDateHeader(date: LocalDate) {
    Text(
        text = date.toString(),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
internal fun DietCard(
    record: DietRecord,
    onOpen: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val totalCalories = record.entries.sumOf { entry ->
        entry.resolvedNutrients["ENERGY"]?.value ?: 0.0
    }
    ActionSectionCard(
        title = stringResource(record.mealPeriod.displayRes()),
        titleIcon = { MealPeriodIcon(record.mealPeriod) },
        onClick = onOpen,
        headerActions = {
            onEdit?.let { edit ->
                androidx.compose.material3.IconButton(onClick = edit) {
                    androidx.compose.material3.Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.diet_edit), tint = MaterialTheme.colorScheme.primary)
                }
            }
            onDelete?.let { delete ->
                androidx.compose.material3.IconButton(onClick = delete) {
                    androidx.compose.material3.Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.diet_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
        Text(
            text = stringResource(R.string.diet_summary_calories, formatCalories(totalCalories)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider()
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
            HorizontalDivider()
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

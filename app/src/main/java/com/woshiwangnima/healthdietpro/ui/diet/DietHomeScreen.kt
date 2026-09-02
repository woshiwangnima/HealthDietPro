package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangePreset
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.resolve
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.GaugeMetricGroup
import com.woshiwangnima.healthdietpro.common.ui.GaugeReferenceValue
import com.woshiwangnima.healthdietpro.common.ui.MappedValueGauge
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.RecordTimeRangeFilter
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.common.range.UnitRange
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
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
    val selectedStatisticsDay = remember { mutableStateOf(LocalDate.now(ZoneId.systemDefault())) }
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
                        selectedDayState = selectedStatisticsDay,
                        onOpenStatistics = { date -> selectedStatisticsDay.value = date; selectedTab = 1 },
                        trendOnly = true,
                    )
                } else if (tab == 1) {
                    DietStatisticsTab(
                        records = uiState.records,
                        goals = uiState.goals,
                        nutrientMetas = uiState.nutrientMetas,
                        onOpenMeal = onOpen,
                        selectedDayState = selectedStatisticsDay,
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
                            allRecords = uiState.records,
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
    allRecords: List<DietRecord> = listOf(record),
    onOpen: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val totalCalories = record.entries.sumOf { entry ->
        entry.resolvedNutrients["ENERGY"]?.value ?: 0.0
    }
    val sameMeals = allRecords.filter { it.mealPeriod == record.mealPeriod }
    val currentWeight = record.entries.sumOf(DietFoodEntry::netWeightGrams)
    val mealWeights = sameMeals.map { it.entries.sumOf(DietFoodEntry::netWeightGrams) }
    val mealCalories = sameMeals.map { meal -> meal.entries.sumOf { it.resolvedNutrients["ENERGY"]?.value ?: 0.0 } }
    val weightGauge = mealGauge(stringResource(R.string.diet_metric_net_weight), currentWeight, mealWeights, "g", Color(0xFF00897B), stringResource(R.string.diet_history_low), stringResource(R.string.diet_history_high), stringResource(R.string.diet_history_average))
    val calorieGauge = mealGauge(stringResource(R.string.diet_summary_energy), totalCalories, mealCalories, "kcal", Color(0xFFE65100), stringResource(R.string.diet_history_low), stringResource(R.string.diet_history_high), stringResource(R.string.diet_history_average))
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Top) {
            Card(modifier = Modifier.weight(1f).then(if (onOpen != null) Modifier.clickable(onClick = requireNotNull(onOpen)) else Modifier)) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.width(72.dp).height(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            MealPeriodIcon(record.mealPeriod, modifier = Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(record.mealPeriod.displayRes()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.diet_item_count, record.entries.size), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Top,
                    ) {
                        MealGauge(weightGauge, Modifier.weight(1f))
                        MealGauge(calorieGauge, Modifier.weight(1f))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextOverflowText(
                            formatRecordTimestamp(record.mealStartAt, RecordTimePrecision.MINUTE),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        if (record.note.isNotBlank()) {
                            TextOverflowText(
                                record.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                onEdit?.let { IconButton(onClick = it) { Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.diet_edit), tint = MaterialTheme.colorScheme.primary) } }
                onDelete?.let { IconButton(onClick = it) { Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.diet_delete), tint = MaterialTheme.colorScheme.error) } }
            }
        }
    }
}

private fun mealGauge(label: String, current: Double, values: List<Double>, unit: String, color: Color, lowLabel: String, highLabel: String, averageLabel: String): GaugeMetricGroup {
    val all = values + current
    val rawMin = all.minOrNull() ?: current
    val rawMax = all.maxOrNull() ?: current
    val padding = (kotlin.math.abs(rawMax).coerceAtLeast(1.0) * 0.1).coerceAtLeast(1.0)
    val min = if (rawMin == rawMax) rawMin - padding else rawMin
    val max = if (rawMin == rawMax) rawMax + padding else rawMax
    val references = listOfNotNull(
        values.minOrNull()?.let { GaugeReferenceValue("min", lowLabel, it, Color(0xFF00897B)) },
        values.maxOrNull()?.let { GaugeReferenceValue("max", highLabel, it, Color(0xFFC62828)) },
        values.takeIf { it.isNotEmpty() }?.average()?.let { GaugeReferenceValue("avg", averageLabel, it, Color(0xFFF9A825)) },
    )
    return GaugeMetricGroup(label, label, current, UnitRange(min, true, max, true, if (unit == "g") "weight" else "energy", unit), color, references, unit)
}

@Composable
private fun MealGauge(group: GaugeMetricGroup, modifier: Modifier = Modifier) {
    Box(
        modifier
            .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val scale = 0.7f
                    val compactHeight = (placeable.height * scale).toInt()
                    layout(placeable.width, compactHeight) {
                        val y = ((compactHeight - placeable.height) / 2f).toInt()
                        // The layer scales around its own center; adding an x offset here would shift it right.
                        placeable.placeWithLayer(0, y) {
                            scaleX = scale
                            scaleY = scale
                        }
                }
            },
    ) {
        MappedValueGauge(
            listOf(group),
            Modifier.fillMaxWidth(),
            showTooltip = false,
            showProgressDial = true,
        )
        Text(
            "${formatDietGaugeValue(group.currentValue)} ${group.unit}",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 26.sp,
                lineHeight = 26.sp,
            ),
            color = group.color,
            modifier = Modifier.align(Alignment.Center).offset(y = (-4).dp),
        )
    }
}

private fun formatDietGaugeValue(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

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

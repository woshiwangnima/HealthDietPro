package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.LinearProgressWithPercent
import com.woshiwangnima.healthdietpro.common.ui.SortBar
import com.woshiwangnima.healthdietpro.common.ui.SortOption
import com.woshiwangnima.healthdietpro.common.ui.SortOrder
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import kotlin.math.roundToInt

/** 单次用餐统计详情：点击记录卡片进入，展示这一餐的营养汇总与目标达成情况。 */
@Composable
internal fun DietMealDetailScreen(
    record: DietRecord,
    goals: DietGoalsPrefs,
    dayTotals: DayNutrients,
    onEdit: () -> Unit,
    onOpenFood: (DietFoodEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mealTotals = remember(record) {
        DayNutrients(
            energy = record.entries.sumOf { it.resolvedNutrients["ENERGY"]?.value ?: 0.0 },
            protein = record.entries.sumOf { it.resolvedNutrients["PROTEIN"]?.value ?: 0.0 },
            fat = record.entries.sumOf { it.resolvedNutrients["FAT"]?.value ?: 0.0 },
            carbs = record.entries.sumOf { it.resolvedNutrients["CHO"]?.value ?: 0.0 },
        )
    }
    var activeSorts by remember { mutableStateOf(emptyList<Pair<String, SortOrder>>()) }
    val sortOptions = listOf(
        SortOption("KIND", stringResource(R.string.diet_sort_kind)),
        SortOption("WEIGHT", stringResource(R.string.diet_entry_weight)),
    ) + NutrientMetric.entries.map { SortOption(it.id, stringResource(it.labelRes)) }
    val sortedEntries = remember(record.entries, activeSorts) { applyEntrySort(record.entries, activeSorts) }
    BaseScreen(
        title = stringResource(R.string.diet_meal_detail_title, stringResource(record.mealPeriod.displayRes())),
        onBack = onBack,
        actions = {
            androidx.compose.material3.IconButton(onClick = onEdit) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.diet_edit),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { MealTimeCard(record) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(R.drawable.ic_diet, stringResource(R.string.diet_meal_summary))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NutrientMetric.entries.forEach { metric ->
                            DietStatCard(
                                label = stringResource(metric.labelRes),
                                unit = metric.unit,
                                value = formatCalories(mealTotals.value(metric)),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(R.drawable.ic_health_metrics, stringResource(R.string.diet_meal_goal_progress))
                    NutrientProgressRow(NutrientMetric.ENERGY, mealTotals.energy, goals.energyKcal.toDouble())
                    NutrientProgressRow(NutrientMetric.CARBS, mealTotals.carbs, goals.carbsGrams.toDouble())
                    NutrientProgressRow(NutrientMetric.PROTEIN, mealTotals.protein, goals.proteinGrams.toDouble())
                    NutrientProgressRow(NutrientMetric.FAT, mealTotals.fat, goals.fatGrams.toDouble())
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(R.drawable.ic_nutrients, stringResource(R.string.diet_meal_nutrient_list))
                    MealNutrientTable(
                        perMealTotals = mapOf(record.mealPeriod to mealTotals),
                        dayTotals = dayTotals,
                        periodColors = mapOf(record.mealPeriod to periodColor(record.mealPeriod)),
                    )
                }
            }
            item {
                SectionTitle(R.drawable.ic_list, stringResource(R.string.diet_entries))
            }
            item {
                SortBar(
                    options = sortOptions,
                    onSortChange = { activeSorts = it },
                )
            }
            itemsIndexed(sortedEntries, key = { index, entry -> entry.foodName + entry.netWeightGrams + index }) { _, entry ->
                MealEntryRow(entry, mealTotals, onOpenFood)
            }
            if (record.note.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.diet_note), style = MaterialTheme.typography.titleMedium)
                        Text(record.note, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MealTimeCard(record: DietRecord) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    R.string.diet_meal_time_range,
                    formatRecordTimestamp(record.mealStartAt, RecordTimePrecision.MINUTE),
                    formatRecordTimestamp(record.mealEndAt, RecordTimePrecision.MINUTE),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.diet_meal_detail_meta, record.entries.size, formatGrams(record.entries.sumOf(DietFoodEntry::netWeightGrams))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MealEntryRow(entry: DietFoodEntry, mealTotals: DayNutrients, onOpenFood: (DietFoodEntry) -> Unit) {
    val (container, onContainer) = foodKindColors(entry.foodKind)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = { onOpenFood(entry) },
                enabled = entry.foodId != null,
                color = container,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                modifier = Modifier.width(MealFoodLabelWidth),
            ) {
                Column(
                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = entry.foodName,
                        color = onContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatGrams(entry.netWeightGrams),
                        color = onContainer.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                NutrientMetric.entries.forEach { metric ->
                    FoodNutrientBarRow(
                        metric = metric,
                        value = entry.resolvedNutrients[metric.id]?.value ?: 0.0,
                        mealTotal = mealTotals.value(metric),
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodNutrientBarRow(metric: NutrientMetric, value: Double, mealTotal: Double) {
    val fraction = if (mealTotal > 0.0) (value / mealTotal).toFloat().coerceIn(0f, 1f) else 0f
    val percent = if (mealTotal > 0.0) (value / mealTotal * 100.0).roundToInt() else 0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextOverflowText(
            text = stringResource(metric.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(FoodNutrientNameWidth),
        )
        LinearProgressWithPercent(
            progress = { fraction },
            color = MetricColor(metric),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            percentText = "$percent%",
            modifier = Modifier.weight(1f),
        )
    }
}

private fun foodKindColors(kind: FoodKind?): Pair<Color, Color> = when (kind) {
    FoodKind.INGREDIENT -> Color(0xFF43A047) to Color(0xFFFFFFFF)
    FoodKind.FOOD -> Color(0xFFF57C00) to Color(0xFFFFFFFF)
    FoodKind.DISH -> Color(0xFFE53935) to Color(0xFFFFFFFF)
    null -> Color(0xFF607D8B) to Color(0xFFFFFFFF)
}

private val MealFoodLabelWidth = 90.dp
private val FoodNutrientNameWidth = 40.dp

private fun applyEntrySort(entries: List<DietFoodEntry>, active: List<Pair<String, SortOrder>>): List<DietFoodEntry> {
    if (active.isEmpty()) return entries
    var comparator: Comparator<DietFoodEntry>? = null
    for ((id, order) in active) {
        val base: Comparator<DietFoodEntry> = when (id) {
            "KIND" -> compareBy { kindRank(it.foodKind) }
            "WEIGHT" -> compareBy { it.netWeightGrams }
            "ENERGY" -> compareBy { it.resolvedNutrients["ENERGY"]?.value ?: 0.0 }
            "CHO" -> compareBy { it.resolvedNutrients["CHO"]?.value ?: 0.0 }
            "PROTEIN" -> compareBy { it.resolvedNutrients["PROTEIN"]?.value ?: 0.0 }
            "FAT" -> compareBy { it.resolvedNutrients["FAT"]?.value ?: 0.0 }
            else -> continue
        }
        val c = if (order == SortOrder.DESCENDING) base.reversed() else base
        comparator = if (comparator == null) c else comparator.then(c)
    }
    return if (comparator == null) entries else entries.sortedWith(comparator)
}

private fun kindRank(kind: FoodKind?): Int = when (kind) {
    FoodKind.INGREDIENT -> 0
    FoodKind.FOOD -> 1
    FoodKind.DISH -> 2
    null -> 3
}
package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.woshiwangnima.healthdietpro.common.ui.NutrientCarbsColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientEnergyColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientFatColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientProteinColor
import com.woshiwangnima.healthdietpro.model.diet.DietFoodEntry
import com.woshiwangnima.healthdietpro.model.diet.DietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRecord

/** 单次用餐统计详情：点击记录卡片进入，展示这一餐的营养汇总与目标达成情况。 */
@Composable
internal fun DietMealDetailScreen(
    record: DietRecord,
    goals: DietGoalsPrefs,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val energy = record.entries.sumOf { it.resolvedNutrients["ENERGY"]?.value ?: 0.0 }
    val protein = record.entries.sumOf { it.resolvedNutrients["PROTEIN"]?.value ?: 0.0 }
    val fat = record.entries.sumOf { it.resolvedNutrients["FAT"]?.value ?: 0.0 }
    val carbs = record.entries.sumOf { it.resolvedNutrients["CHO"]?.value ?: 0.0 }
    BaseScreen(
        title = stringResource(record.mealPeriod.displayRes()),
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
                    Text(stringResource(R.string.diet_meal_summary), style = MaterialTheme.typography.titleMedium)
                    StatRow {
                        MealStatAmount(stringResource(R.string.diet_summary_energy), formatCalories(energy) + " kcal")
                        MealStatAmount(stringResource(R.string.diet_summary_protein), formatGrams(protein))
                        MealStatAmount(stringResource(R.string.diet_summary_fat), formatGrams(fat))
                        MealStatAmount(stringResource(R.string.diet_summary_carbs), formatGrams(carbs))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.diet_meal_goal_progress), style = MaterialTheme.typography.titleMedium)
                    MealGoalRow(stringResource(R.string.diet_summary_energy), energy, goals.energyKcal.toDouble(), "kcal", NutrientEnergyColor)
                    MealGoalRow(stringResource(R.string.diet_summary_carbs), carbs, goals.carbsGrams.toDouble(), "g", NutrientCarbsColor)
                    MealGoalRow(stringResource(R.string.diet_summary_protein), protein, goals.proteinGrams.toDouble(), "g", NutrientProteinColor)
                    MealGoalRow(stringResource(R.string.diet_summary_fat), fat, goals.fatGrams.toDouble(), "g", NutrientFatColor)
                }
            }
            item {
                Text(stringResource(R.string.diet_entries), style = MaterialTheme.typography.titleMedium)
            }
            items(record.entries, key = { it.foodName + it.netWeightGrams }) { entry ->
                MealEntryRow(entry)
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
private fun MealGoalRow(label: String, value: Double, goal: Double, unit: String, color: Color) {
    val fraction = if (goal > 0.0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f
    val overflow = goal > 0.0 && value > goal
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    if (overflow) R.string.diet_statistics_goal_exceeded else R.string.diet_statistics_goal_fraction,
                    formatCalories(value),
                    formatCalories(goal),
                    unit,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (overflow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            color = if (overflow) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MealEntryRow(entry: DietFoodEntry) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.foodName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatGrams(entry.netWeightGrams), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val energy = entry.resolvedNutrients["ENERGY"]?.value
            if (energy != null && energy > 0.0) {
                Text(
                    text = stringResource(R.string.diet_entry_nutrition_summary, formatCalories(energy)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.diet_no_nutrition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        content()
    }
}

@Composable
private fun RowScope.MealStatAmount(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
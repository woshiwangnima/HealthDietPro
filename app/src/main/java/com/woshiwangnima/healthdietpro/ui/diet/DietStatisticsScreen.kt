package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.recordDateStartMillis
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.NutrientCarbsColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientEnergyColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientFatColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientProteinColor
import com.woshiwangnima.healthdietpro.common.ui.nutrientColor
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarChart
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarEntry
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarReferenceLine
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarSegment
import com.woshiwangnima.healthdietpro.model.diet.DietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private data class DayNutrients(val energy: Double = 0.0, val protein: Double = 0.0, val fat: Double = 0.0, val carbs: Double = 0.0)

private enum class NutrientMetric(val id: String, val labelRes: Int, val unit: String) {
    ENERGY("ENERGY", R.string.diet_summary_energy, "kcal"),
    PROTEIN("PROTEIN", R.string.diet_summary_protein, "g"),
    FAT("FAT", R.string.diet_summary_fat, "g"),
    CARBS("CHO", R.string.diet_summary_carbs, "g"),
}

private fun DayNutrients.value(metric: NutrientMetric): Double = when (metric) {
    NutrientMetric.ENERGY -> energy
    NutrientMetric.PROTEIN -> protein
    NutrientMetric.FAT -> fat
    NutrientMetric.CARBS -> carbs
}

@Composable
internal fun DietStatisticsTab(
    records: List<DietRecord>,
    goals: DietGoalsPrefs,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    var trendDays by rememberSaveable { mutableIntStateOf(7) }
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now(zone)) }
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    var donutMetric by rememberSaveable { mutableStateOf(NutrientMetric.ENERGY) }
    var trendMetric by rememberSaveable { mutableStateOf(NutrientMetric.ENERGY) }
    val selectedStart = remember(selectedDay, zone) { recordDateStartMillis(selectedDay, zone) }
    val selectedEnd = remember(selectedDay, zone) { selectedDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
    val dayRecords = remember(records, selectedStart, selectedEnd) {
        records.filter { it.mealStartAt in selectedStart until selectedEnd }
    }
    val dayTotals = remember(dayRecords) { sumNutrients(dayRecords) }
    val perMealTotals = remember(dayRecords) {
        MealPeriod.entries.associateWith { period ->
            sumNutrients(dayRecords.filter { it.mealPeriod == period })
        }
    }
    val endDate = remember(trendDays, zone) { LocalDate.now(zone) }
    val dates = remember(endDate, trendDays) { (0 until trendDays).map { offset -> endDate.minusDays(offset.toLong()) } }
    val startMillis = remember(endDate, trendDays, zone) { endDate.minusDays((trendDays - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli() }
    val endMillis = remember(endDate, zone) { endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
    val rangeRecords = remember(records, startMillis, endMillis) {
        records.filter { it.mealStartAt in startMillis until endMillis }
    }
    val dayTrendTotals = remember(rangeRecords, zone) {
        rangeRecords.groupBy { Instant.ofEpochMilli(it.mealStartAt).atZone(zone).toLocalDate() }
            .mapValues { (_, dayRecords) -> sumNutrients(dayRecords) }
    }
    val periodColors = remember {
        MealPeriod.entries.mapIndexed { index, period ->
            period to listOf(
                NutrientProteinColor, NutrientCarbsColor, Color(0xFF8E24AA),
                Color(0xFF00897B), Color(0xFF039BE5), NutrientEnergyColor, NutrientFatColor,
            )[index % 7]
        }.toMap()
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val entries = remember(dates, dayTrendTotals, trendMetric, primaryColor) {
        dates.map { date ->
            val totals = dayTrendTotals[date] ?: DayNutrients()
            val value = totals.value(trendMetric)
            DateStackedBarEntry(
                date = date,
                label = "%02d-%02d".format(date.monthValue, date.dayOfMonth),
                segments = buildList {
                    if (value > 0.0) add(DateStackedBarSegment("value", value, nutrientColor(trendMetric.id)))
                },
            )
        }
    }
    var selectedTrendDate by rememberSaveable(trendDays) { mutableStateOf(dates.firstOrNull()) }
    val selectedEntry = entries.firstOrNull { it.date == selectedTrendDate }
    val periodLabels = MealPeriod.entries.associateWith { period -> stringResource(period.displayRes()) }
    val donutSegments = remember(perMealTotals, periodColors, donutMetric, periodLabels) {
        MealPeriod.entries.mapNotNull { period ->
            val value = perMealTotals.getValue(period).value(donutMetric)
            if (value <= 0.0) null
            else DonutChartSegment(period.name, periodLabels.getValue(period), value.toFloat(), periodColors.getValue(period))
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DayDatePicker(selectedDay, onPick = { pickingDay = true })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.diet_today_summary), style = MaterialTheme.typography.titleMedium)
                StatRow {
                    DietStatAmount(stringResource(R.string.diet_summary_energy), formatCalories(dayTotals.energy) + " kcal")
                    DietStatAmount(stringResource(R.string.diet_summary_protein), formatGrams(dayTotals.protein))
                    DietStatAmount(stringResource(R.string.diet_summary_fat), formatGrams(dayTotals.fat))
                    DietStatAmount(stringResource(R.string.diet_summary_carbs), formatGrams(dayTotals.carbs))
                }
                listOf(
                    NutrientMetric.ENERGY to goals.energyKcal.toDouble(),
                    NutrientMetric.CARBS to goals.carbsGrams.toDouble(),
                    NutrientMetric.PROTEIN to goals.proteinGrams.toDouble(),
                    NutrientMetric.FAT to goals.fatGrams.toDouble(),
                ).forEach { (metric, goal) ->
                    GoalProgressRow(
                        label = stringResource(metric.labelRes),
                        value = dayTotals.value(metric),
                        goal = goal,
                        unit = metric.unit,
                        color = MetricColor(metric),
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.diet_meal_distribution), style = MaterialTheme.typography.titleMedium)
                NutrientToggleRow(donutMetric) { donutMetric = it }
                AnimatedDonutChart(
                    segments = donutSegments,
                    centerValue = formatMetricValue(dayTotals.value(donutMetric), donutMetric.unit),
                    centerLabel = stringResource(R.string.diet_meal_distribution_center, stringResource(donutMetric.labelRes)),
                )
                if (perMealTotals.all { it.value.energy == 0.0 }) {
                    Text(stringResource(R.string.diet_statistics_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    MealPeriod.entries.forEach { period ->
                        val totals = perMealTotals.getValue(period)
                        if (totals.value(donutMetric) > 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.foundation.Canvas(Modifier.size(10.dp).padding(end = 6.dp)) { drawCircle(periodColors.getValue(period)) }
                                Text(stringResource(period.displayRes()), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(
                                    text = stringResource(
                                        R.string.diet_meal_subtotal_metric,
                                        stringResource(donutMetric.labelRes),
                                        formatMetricValue(totals.value(donutMetric), donutMetric.unit),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.diet_trend), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(7, 30).forEachIndexed { index, optionDays ->
                        SegmentedButton(
                            selected = trendDays == optionDays,
                            onClick = { trendDays = optionDays },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            label = { Text(stringResource(if (optionDays == 7) R.string.diet_statistics_7_days else R.string.diet_statistics_30_days)) },
                        )
                    }
                }
                NutrientToggleRow(trendMetric) { trendMetric = it }
                val nonZeroTotals = entries.map { entry -> entry.segments.sumOf(DateStackedBarSegment::value) }.filter { it > 0.0 }
                val averageValue = remember(nonZeroTotals) {
                    if (nonZeroTotals.isEmpty()) null else nonZeroTotals.average()
                }
                DateStackedBarChart(
                    entries = entries,
                    yAxisTitle = stringResource(R.string.diet_statistics_daily_metric, stringResource(trendMetric.labelRes), trendMetric.unit),
                    formatValue = { formatCalories(it) },
                    labelEvery = 1,
                    selectedEntry = selectedEntry,
                    onEntrySelected = { selectedTrendDate = it.date },
                    referenceLine = averageValue?.let { average ->
                        DateStackedBarReferenceLine(
                            value = average,
                            label = stringResource(R.string.diet_statistics_average, formatMetricValue(average, trendMetric.unit)),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    },
                )
                selectedTrendDate?.let { trendDate ->
                    val trendDateStart = remember(trendDate, zone) { recordDateStartMillis(trendDate, zone) }
                    val trendDateEnd = remember(trendDate, zone) { trendDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
                    val trendDayTotals = remember(rangeRecords, trendDateStart, trendDateEnd) {
                        sumNutrients(rangeRecords.filter { it.mealStartAt in trendDateStart until trendDateEnd })
                    }
                    if (trendDayTotals.value(trendMetric) > 0.0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.diet_statistics_selected_day_breakdown, trendDate.toString(), stringResource(trendMetric.labelRes)),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            val breakdown = remember(rangeRecords, trendDateStart, trendDateEnd) {
                                MealPeriod.entries.associateWith { period ->
                                    sumNutrients(rangeRecords.filter { it.mealPeriod == period && it.mealStartAt in trendDateStart until trendDateEnd })
                                }
                            }
                            MealPeriod.entries.forEach { period ->
                                val periodTotals = breakdown.getValue(period)
                                if (periodTotals.value(trendMetric) > 0.0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.foundation.Canvas(Modifier.size(10.dp).padding(end = 6.dp)) { drawCircle(periodColors.getValue(period)) }
                                        Text(stringResource(period.displayRes()), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Text(
                                            formatMetricValue(periodTotals.value(trendMetric), trendMetric.unit),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (pickingDay) {
        ComposeDatePickerDialog(
            initialMillis = selectedStart,
            onDismiss = { pickingDay = false },
            onDatePicked = { selectedDay = it; pickingDay = false },
        )
    }
}

@Composable
private fun DayDatePicker(selectedDay: LocalDate, onPick: () -> Unit) {
    Surface(
        onClick = onPick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.ic_event),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = selectedDay.toString(),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalProgressRow(label: String, value: Double, goal: Double, unit: String, color: Color) {
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
private fun NutrientToggleRow(selected: NutrientMetric, onMetricChanged: (NutrientMetric) -> Unit) {
    val metrics = NutrientMetric.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        metrics.forEachIndexed { index, metric ->
            SegmentedButton(
                selected = selected == metric,
                onClick = { onMetricChanged(metric) },
                shape = SegmentedButtonDefaults.itemShape(index, metrics.size),
                label = { Text(stringResource(metric.labelRes)) },
            )
        }
    }
}

private fun formatMetricValue(value: Double, unit: String): String =
    "${formatCalories(value)} $unit"

private fun MetricColor(metric: NutrientMetric): Color = nutrientColor(metric.id)

private fun sumNutrients(records: List<DietRecord>): DayNutrients = records.fold(DayNutrients()) { totals, record ->
    record.entries.fold(totals) { acc, entry ->
        val nutrients = entry.resolvedNutrients
        acc.copy(
            energy = acc.energy + (nutrients["ENERGY"]?.value ?: 0.0),
            protein = acc.protein + (nutrients["PROTEIN"]?.value ?: 0.0),
            fat = acc.fat + (nutrients["FAT"]?.value ?: 0.0),
            carbs = acc.carbs + (nutrients["CHO"]?.value ?: 0.0),
        )
    }
}

@Composable
private fun StatRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        content()
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DietStatAmount(label: String, value: String) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
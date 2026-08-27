package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.recordDateStartMillis
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.LinearProgressWithPercent
import com.woshiwangnima.healthdietpro.common.ui.NutrientCarbsColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientEnergyColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientFatColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientProteinColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientOtherColor
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.WaterGlassProgress
import com.woshiwangnima.healthdietpro.common.ui.rememberAttentionShakeOffset
import com.woshiwangnima.healthdietpro.common.ui.SingleChoiceSegmentedOption
import com.woshiwangnima.healthdietpro.common.ui.SingleChoiceSegmentedSelector
import com.woshiwangnima.healthdietpro.common.ui.nutrientColor
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarChart
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarEntry
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarReferenceLine
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarSegment
import com.woshiwangnima.healthdietpro.model.diet.DietGoalsPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietNutrientAmount
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

internal data class DayNutrients(
    val energy: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbs: Double = 0.0,
    val energyByNutrient: Map<String, Double> = emptyMap(),
    val nutrients: Map<String, DietNutrientAmount> = emptyMap(),
)

private fun DayNutrients.amount(code: String): DietNutrientAmount? = nutrients[code]

internal enum class NutrientMetric(val id: String, val labelRes: Int, val unit: String) {
    ENERGY("ENERGY", R.string.diet_summary_energy, "kcal"),
    CARBS("CHO", R.string.diet_summary_carbs, "g"),
    PROTEIN("PROTEIN", R.string.diet_summary_protein, "g"),
    FAT("FAT", R.string.diet_summary_fat, "g"),
}

internal fun DayNutrients.value(metric: NutrientMetric): Double = when (metric) {
    NutrientMetric.ENERGY -> energy
    NutrientMetric.CARBS -> carbs
    NutrientMetric.PROTEIN -> protein
    NutrientMetric.FAT -> fat
}

@Composable
internal fun DietStatisticsTab(
    records: List<DietRecord>,
    goals: DietGoalsPrefs,
    nutrientMetas: List<NutrientMeta>,
    onOpenMeal: (DietRecord) -> Unit,
    trendOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    var trendDays by rememberSaveable { mutableIntStateOf(7) }
    var selectedDay by rememberSaveable { mutableStateOf(LocalDate.now(zone)) }
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    var donutMetric by rememberSaveable { mutableStateOf(NutrientMetric.ENERGY) }
    var trendMetric by rememberSaveable { mutableStateOf(NutrientMetric.ENERGY) }
    var showNutrientDetails by rememberSaveable { mutableStateOf(false) }
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
        MealPeriod.entries.associateWith(::periodColor)
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
    val donutTotal = remember(perMealTotals, donutMetric) { perMealTotals.values.sumOf { it.value(donutMetric) } }
    val donutSegmentLabels = MealPeriod.entries.associateWith { period ->
        val value = perMealTotals.getValue(period).value(donutMetric)
        if (value <= 0.0) ""
        else {
            val percent = if (donutTotal > 0.0) (value / donutTotal * 100.0).roundToInt() else 0
            stringResource(
                R.string.diet_donut_segment_label,
                periodLabels.getValue(period),
                formatMetricValue(value, donutMetric.unit),
                percent,
            )
        }
    }
    val donutSegments = remember(perMealTotals, periodColors, donutMetric, donutSegmentLabels) {
        MealPeriod.entries.mapNotNull { period ->
            val value = perMealTotals.getValue(period).value(donutMetric)
            if (value <= 0.0) null
            else DonutChartSegment(
                id = period.name,
                label = donutSegmentLabels.getValue(period),
                value = value.toFloat(),
                color = periodColors.getValue(period),
            )
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!trendOnly) item {
            DayDatePicker(selectedDay, onPick = { pickingDay = true })
        }
        if (!trendOnly) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SectionTitle(R.drawable.ic_diet, stringResource(R.string.diet_today_summary))
                NutrientSummaryVisuals(dayTotals, goals)
                OutlinedButton(
                    onClick = { showNutrientDetails = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.diet_show_nutrient_details))
                }
            }
        }
        if (!trendOnly) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SectionTitle(R.drawable.ic_energy_distribution, stringResource(R.string.diet_meal_distribution))
                NutrientToggleRow(donutMetric) { donutMetric = it }
                AnimatedDonutChart(
                    segments = donutSegments,
                    centerValue = formatMetricValue(dayTotals.value(donutMetric), donutMetric.unit),
                    centerLabel = stringResource(R.string.diet_meal_distribution_center, stringResource(donutMetric.labelRes)),
                    showLegend = false,
                    labelMaxLines = 2,
                    centerAction = {
                        AppOutlinedIconTextButton(
                            text = stringResource(R.string.diet_meal_distribution_switch),
                            iconRes = R.drawable.ic_switch_user,
                            onClick = {
                                donutMetric = NutrientMetric.entries[
                                    (NutrientMetric.entries.indexOf(donutMetric) + 1) % NutrientMetric.entries.size
                                ]
                            },
                            modifier = Modifier.width(88.dp),
                        )
                    },
                )
                if (donutSegments.isEmpty()) {
                    Text(stringResource(R.string.diet_statistics_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (trendOnly) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SectionTitle(R.drawable.ic_nutrients, stringResource(R.string.diet_trend))
                SingleChoiceSegmentedSelector(
                    options = listOf(7, 30, 90).map { optionDays ->
                        SingleChoiceSegmentedOption(
                            id = optionDays.toString(),
                            labelRes = dayRangeLabelRes(optionDays),
                        )
                    },
                    selectedId = trendDays.toString(),
                    onOptionSelected = { option -> trendDays = option.id.toInt() },
                )
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
                    if (NutrientMetric.entries.any { trendDayTotals.value(it) > 0.0 }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.diet_statistics_selected_day_breakdown_title, trendDate.toString()),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            val dayMeals = remember(rangeRecords, trendDateStart, trendDateEnd) {
                                rangeRecords.filter { it.mealStartAt in trendDateStart until trendDateEnd }
                            }
                            MealNutrientTable(
                                records = dayMeals,
                                dayTotals = trendDayTotals,
                                onOpenMeal = onOpenMeal,
                            )
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
            datesWithData = remember(records, zone) {
                records.map { Instant.ofEpochMilli(it.mealStartAt).atZone(zone).toLocalDate() }.toSet()
            },
            allowNoDataSelection = true,
        )
    }
    if (!trendOnly && showNutrientDetails) NutrientDetailsDialog(dayTotals, nutrientMetas, language) { showNutrientDetails = false }
}

@Composable
private fun NutrientDetailsDialog(
    totals: DayNutrients,
    nutrientMetas: List<NutrientMeta>,
    language: String,
    onDismiss: () -> Unit,
) {
    val groups = remember(nutrientMetas) {
        nutrientMetas.groupBy { meta ->
            when (meta.category.substringBefore('.')) {
                "fiber" -> "carbohydrate"
                else -> meta.category.substringBefore('.')
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.diet_show_nutrient_details)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                groups.forEach { (category, metas) ->
                    item(key = category) {
                        Text(
                            text = nutrientCategoryName(category),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        )
                    }
                    items(metas, key = NutrientMeta::code) { meta ->
                        val rowIndex = metas.indexOf(meta)
                        Surface(
                            color = if (rowIndex % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(meta.displayName(language), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(totals.amount(meta.code)?.let { "${formatCalories(it.value)} ${it.unitId}" } ?: "-", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } },
    )
}

@Composable
private fun nutrientCategoryName(category: String): String = stringResource(
    when (category.substringBefore('.')) {
        "energy" -> R.string.diet_nutrient_category_energy
        "protein" -> R.string.diet_nutrient_category_protein
        "fat" -> R.string.diet_nutrient_category_fat
        "carbohydrate", "fiber" -> R.string.diet_nutrient_category_carbohydrate
        "vitamin" -> R.string.diet_nutrient_category_vitamin
        "mineral" -> R.string.diet_nutrient_category_mineral
        "water" -> R.string.diet_nutrient_category_water
        else -> R.string.diet_nutrient_category_other
    },
)

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
internal fun SectionTitle(iconRes: Int, text: String) {
    val iconSize = with(LocalDensity.current) { MaterialTheme.typography.titleMedium.fontSize.toDp() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)    }
}

private fun dayRangeLabelRes(days: Int): Int = when (days) {
    7 -> R.string.diet_statistics_7_days
    30 -> R.string.diet_statistics_30_days
    else -> R.string.diet_statistics_90_days
}

internal fun periodColor(period: MealPeriod): Color {
    val palette = listOf(
        NutrientProteinColor, NutrientCarbsColor, Color(0xFF8E24AA),
        Color(0xFF00897B), Color(0xFF039BE5), NutrientEnergyColor, NutrientFatColor,
    )
    return palette[MealPeriod.entries.indexOf(period) % palette.size]
}

private fun metricGoal(metric: NutrientMetric, goals: DietGoalsPrefs): Double = when (metric) {
    NutrientMetric.ENERGY -> goals.energyKcal.toDouble()
    NutrientMetric.CARBS -> goals.carbsGrams.toDouble()
    NutrientMetric.PROTEIN -> goals.proteinGrams.toDouble()
    NutrientMetric.FAT -> goals.fatGrams.toDouble()
}

@Composable
internal fun NutrientSummaryVisuals(totals: DayNutrients, goals: DietGoalsPrefs) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NutrientMetric.entries.forEach { metric ->
            val goal = metricGoal(metric, goals)
            val overflow = goal > 0.0 && totals.value(metric) > goal
            val shakeOffset = rememberAttentionShakeOffset(overflow, "dietGoalShake_${metric.id}")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextOverflowText(
                    text = "${formatCalories(goal)} ${metric.unit}" + if (overflow) " ${stringResource(R.string.diet_statistics_goal_exceeded_short)}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetricColor(metric),
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().offset { IntOffset(0, shakeOffset.roundToPx()) },
                )
                DietStatCard(
                    label = stringResource(metric.labelRes),
                    unit = metric.unit,
                    value = formatCalories(totals.value(metric)),
                    rawValue = totals.value(metric),
                    goal = goal,
                    color = MetricColor(metric),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    NutrientEnergyDonut(totals)
}

@Composable
internal fun CompleteNutrientSummary(
    totals: DayNutrients,
    nutrientMetas: List<NutrientMeta>,
    language: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        nutrientMetas.forEach { meta ->
            val amount = totals.amount(meta.code)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextOverflowText(
                    text = meta.displayName(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                TextOverflowText(
                    text = amount?.let { "${formatCalories(it.value)} ${it.unitId}" } ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun DietStatCard(
    label: String,
    unit: String,
    value: String,
    rawValue: Double = value.toDoubleOrNull() ?: 0.0,
    goal: Double = 0.0,
    color: Color? = null,
    modifier: Modifier = Modifier,
) {
    val numericValue = rawValue
    val fillColor = color ?: MaterialTheme.colorScheme.primary
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = modifier.height(100.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            WaterGlassProgress(
                progress = if (goal > 0.0) (numericValue / goal).toFloat() else 0f,
                valueLabel = "",
                supportingLabel = "",
                liquidColor = fillColor,
                modifier = Modifier.fillMaxSize(),
                fillContainer = true,
                showLabels = false,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                TextOverflowText(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                TextOverflowText(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            TextOverflowText(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center).padding(top = 12.dp),
            )
            TextOverflowText(
                text = if (goal > 0.0) "${((numericValue / goal) * 100.0).roundToInt()}%" else "-",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(top = 52.dp).fillMaxWidth(0.6f),
            )
        }
    }
}

@Composable
internal fun MealNutrientTable(
    records: List<DietRecord>,
    dayTotals: DayNutrients,
    onOpenMeal: ((DietRecord) -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        records.sortedBy(DietRecord::mealStartAt).forEach { record ->
            val totals = remember(record) { sumNutrients(listOf(record)) }
            if (NutrientMetric.entries.any { totals.value(it) > 0.0 }) {
                Surface(
                    onClick = { onOpenMeal?.invoke(record) },
                    enabled = onOpenMeal != null,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(MealLabelWidth),
                        ) {
                            androidx.compose.foundation.Canvas(Modifier.size(10.dp).padding(end = 6.dp)) { drawCircle(periodColor(record.mealPeriod)) }
                            TextOverflowText(
                                text = stringResource(record.mealPeriod.displayRes()),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            NutrientMetric.entries.forEach { metric ->
                                NutrientProgressRow(
                                    metric = metric,
                                    value = totals.value(metric),
                                    total = dayTotals.value(metric),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NutrientProgressRow(metric: NutrientMetric, value: Double, total: Double) {
    val fraction = if (total > 0.0) (value / total).toFloat().coerceIn(0f, 1f) else 0f
    val percent = if (total > 0.0) (value / total * 100.0).roundToInt() else 0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextOverflowText(
            text = stringResource(metric.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(NutrientNameWidth),
        )
        LinearProgressWithPercent(
            progress = { fraction },
            color = MetricColor(metric),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            percentText = "$percent%",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        TextOverflowText(
            text = if (total > 0.0) {
                stringResource(
                    R.string.diet_meal_nutrient_values,
                    formatCalories(value),
                    formatCalories(total),
                    metric.unit,
                )
            } else {
                stringResource(R.string.diet_meal_nutrient_values_no_total, formatCalories(value), metric.unit)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(NutrientValueWidth),
        )
    }
}

@Composable
private fun NutrientToggleRow(selected: NutrientMetric, onMetricChanged: (NutrientMetric) -> Unit) {
    SingleChoiceSegmentedSelector(
        options = NutrientMetric.entries.map { metric ->
            SingleChoiceSegmentedOption(id = metric.id, labelRes = metric.labelRes)
        },
        selectedId = selected.id,
        onOptionSelected = { option -> onMetricChanged(NutrientMetric.entries.first { it.id == option.id }) },
    )
}

private fun formatMetricValue(value: Double, unit: String): String =
    "${formatCalories(value)} $unit"

@Composable
private fun NutrientEnergyDonut(totals: DayNutrients) {
    val ranked = totals.energyByNutrient.entries
        .filter { it.value > 0.0 }
        .sortedByDescending { it.value }
    val top = ranked.take(4)
    val identifiedEnergy = ranked.sumOf { it.value }
    val residualEnergy = (totals.energy - identifiedEnergy).coerceAtLeast(0.0)
    val other = ranked.drop(4).sumOf { it.value } + residualEnergy
    if (top.isEmpty() && other <= 0.0) return
    val items = buildList {
        top.forEach { (id, kcal) -> add(id to kcal) }
        if (other > 0.0) add("OTHER" to other)
    }
    val total = items.sumOf { it.second }
    AnimatedDonutChart(
        segments = items.map { (id, kcal) ->
            val percent = kcal / total * 100.0
            DonutChartSegment(
                id = id,
                label = "${nutrientEnergyName(id)} ${"%.1f".format(percent)}%",
                value = kcal.toFloat(),
                color = if (id == "OTHER") NutrientOtherColor else nutrientColor(id),
            )
        },
        centerValue = stringResource(R.string.nutrition_energy_kcal_value, total),
        centerLabel = stringResource(R.string.nutrition_macronutrient_energy_center),
        modifier = Modifier.fillMaxWidth(),
        showLegend = false,
    )
}

@Composable
private fun nutrientEnergyName(id: String): String = when (id.uppercase()) {
    "CHO" -> stringResource(R.string.diet_summary_carbs)
    "PROTEIN" -> stringResource(R.string.diet_summary_protein)
    "FAT" -> stringResource(R.string.diet_summary_fat)
    "FIBER" -> stringResource(R.string.nutrition_nutrient_fiber)
    else -> stringResource(R.string.nutrition_nutrient_other)
}

internal fun MetricColor(metric: NutrientMetric): Color = nutrientColor(metric.id)

internal fun sumNutrients(records: List<DietRecord>): DayNutrients = records.fold(DayNutrients()) { totals, record ->
    record.entries.fold(totals) { acc, entry ->
        val nutrients = entry.resolvedNutrients
        val energyByNutrient = nutrients.entries.fold(acc.energyByNutrient) { map, (id, amount) ->
            val factor = energyKcalPerGram[id]
            if (factor != null && amount.unitId.equals("g", ignoreCase = true)) {
                map + (id to ((map[id] ?: 0.0) + amount.value * factor))
            } else {
                map
            }
        }
        acc.copy(
            energy = acc.energy + (nutrients["ENERGY"]?.value ?: 0.0),
            protein = acc.protein + (nutrients["PROTEIN"]?.value ?: 0.0),
            fat = acc.fat + (nutrients["FAT"]?.value ?: 0.0),
            carbs = acc.carbs + (nutrients["CHO"]?.value ?: 0.0),
            energyByNutrient = energyByNutrient,
            nutrients = nutrients.entries.fold(acc.nutrients) { map, (code, amount) ->
                val previous = map[code]
                map + (code to if (previous == null) {
                    DietNutrientAmount(amount.value, amount.unitCategory, amount.unitId)
                } else {
                    previous.copy(value = previous.value + amount.value)
                })
            },
        )
    }
}

private val energyKcalPerGram = mapOf(
    "PROTEIN" to 4.0,
    "CHO" to 4.0,
    "FAT" to 9.0,
    "FIBER" to 2.0,
    "ALCOHOL" to 7.0,
    "ORGANIC_ACID" to 3.0,
)

private val MealLabelWidth = 62.dp
private val NutrientNameWidth = 52.dp
private val NutrientValueWidth = 104.dp

package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.recordDateStartMillis
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.LinearProgressWithPercent
import com.woshiwangnima.healthdietpro.common.ui.NutrientCarbsColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientEnergyColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientFatColor
import com.woshiwangnima.healthdietpro.common.ui.NutrientProteinColor
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
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
import kotlin.math.roundToInt

internal data class DayNutrients(val energy: Double = 0.0, val protein: Double = 0.0, val fat: Double = 0.0, val carbs: Double = 0.0)

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
        item {
            DayDatePicker(selectedDay, onPick = { pickingDay = true })
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SectionTitle(R.drawable.ic_diet, stringResource(R.string.diet_today_summary))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NutrientMetric.entries.forEach { metric ->
                        DietStatCard(
                            label = stringResource(metric.labelRes),
                            unit = metric.unit,
                            value = formatCalories(dayTotals.value(metric)),
                            modifier = Modifier.weight(1f),
                        )
                    }
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
                SectionTitle(R.drawable.ic_energy_distribution, stringResource(R.string.diet_meal_distribution))
                NutrientToggleRow(donutMetric) { donutMetric = it }
                AnimatedDonutChart(
                    segments = donutSegments,
                    centerValue = formatMetricValue(dayTotals.value(donutMetric), donutMetric.unit),
                    centerLabel = stringResource(R.string.diet_meal_distribution_center, stringResource(donutMetric.labelRes)),
                    showLegend = false,
                    labelMaxLines = 2,
                )
                if (perMealTotals.all { it.value.energy == 0.0 }) {
                    Text(stringResource(R.string.diet_statistics_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    MealNutrientTable(perMealTotals, dayTotals, periodColors)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SectionTitle(R.drawable.ic_nutrients, stringResource(R.string.diet_trend))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(7, 30, 90).forEachIndexed { index, optionDays ->
                        SegmentedButton(
                            selected = trendDays == optionDays,
                            onClick = { trendDays = optionDays },
                            shape = SegmentedButtonDefaults.itemShape(index, 3),
                            label = { Text(stringResource(dayRangeLabelRes(optionDays))) },
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
                    if (NutrientMetric.entries.any { trendDayTotals.value(it) > 0.0 }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.diet_statistics_selected_day_breakdown_title, trendDate.toString()),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            val breakdown = remember(rangeRecords, trendDateStart, trendDateEnd) {
                                MealPeriod.entries.associateWith { period ->
                                    sumNutrients(rangeRecords.filter { it.mealPeriod == period && it.mealStartAt in trendDateStart until trendDateEnd })
                                }
                            }
                            MealNutrientTable(breakdown, trendDayTotals, periodColors)
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

@Composable
internal fun DietStatCard(label: String, unit: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextOverflowText(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
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
            )
        }
    }
}

@Composable
private fun GoalProgressRow(label: String, value: Double, goal: Double, unit: String, color: Color) {
    val fraction = if (goal > 0.0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f
    val overflow = goal > 0.0 && value > goal
    val density = LocalDensity.current
    val shakeTransition = rememberInfiniteTransition(label = "goalProgressShake")
    val shakeDp by shakeTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                -5f at 50
                4f at 100
                -3f at 150
                2.2f at 195
                -1.4f at 240
                0.8f at 275
                0f at 300
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "goalProgressShakeOffset",
    )
    val shakePx = with(density) { shakeDp.dp.toPx() }
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
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().offset { IntOffset(0, if (overflow) shakePx.toInt() else 0) },
        )
    }
}

@Composable
internal fun MealNutrientTable(
    perMealTotals: Map<MealPeriod, DayNutrients>,
    dayTotals: DayNutrients,
    periodColors: Map<MealPeriod, Color>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        MealPeriod.entries.forEach { period ->
            val totals = perMealTotals[period] ?: DayNutrients()
            if (NutrientMetric.entries.any { totals.value(it) > 0.0 }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(MealLabelWidth),
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.size(10.dp).padding(end = 6.dp)) { drawCircle(periodColors.getValue(period)) }
                        TextOverflowText(
                            text = stringResource(period.displayRes()),
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

internal fun MetricColor(metric: NutrientMetric): Color = nutrientColor(metric.id)

internal fun sumNutrients(records: List<DietRecord>): DayNutrients = records.fold(DayNutrients()) { totals, record ->
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

private val MealLabelWidth = 62.dp
private val NutrientNameWidth = 52.dp
private val NutrientValueWidth = 104.dp
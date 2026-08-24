package com.woshiwangnima.healthdietpro.ui.event

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.range.UnitRange
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordClock
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.DietEventGlucoseAnalysis
import com.woshiwangnima.healthdietpro.model.bloodglucose.analyzeDietEventGlucose
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.unit.formatGlucoseValue
import com.woshiwangnima.healthdietpro.ui.diet.DietCard
import com.woshiwangnima.healthdietpro.ui.diet.displayRes
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil

@Composable
internal fun DietEventGlucoseAnalysisScreen(
    record: DietRecord,
    sameDayRecords: List<DietRecord>,
    bloodGlucoseRecords: List<BloodGlucoseRecord>,
    unitId: String,
    unitLabel: String,
    targetRange: UnitRange<Float>,
    onRecordSelected: (DietRecord) -> Unit,
    onOpenDietDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
    val analysis = remember(record.mealStartAt, bloodGlucoseRecords) {
        analyzeDietEventGlucose(record.mealStartAt, bloodGlucoseRecords)
    }
    val recordsOnSameDay = remember(record, sameDayRecords) {
        val date = Instant.ofEpochMilli(record.mealStartAt).atZone(ZoneId.systemDefault()).toLocalDate()
        (sameDayRecords + record)
            .distinctBy(DietRecord::id)
            .filter { Instant.ofEpochMilli(it.mealStartAt).atZone(ZoneId.systemDefault()).toLocalDate() == date }
            .sortedBy(DietRecord::mealStartAt)
    }
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.blood_glucose_diet_event_analysis_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DietAnalysisHeader(
                    record,
                    analysis,
                    unitId,
                    unitLabel,
                    recordsOnSameDay.filterNot { it.id == record.id },
                    onRecordSelected,
                )
            }
            item { DietEventTimePointMetrics(analysis, record.mealStartAt, unitId, unitLabel) }
            item { DietEventOutcomeMetrics(analysis, unitId, unitLabel) }
            item { DietEventGlucoseChart(analysis, record.mealStartAt, unitId, unitLabel, targetRange) }
            item { DietCard(record = record, onOpen = { onOpenDietDetail(record.id) }) }
        }
    }
}

@Composable
private fun DietAnalysisHeader(
    record: DietRecord,
    analysis: DietEventGlucoseAnalysis,
    unitId: String,
    unitLabel: String,
    sameDayRecords: List<DietRecord>,
    onRecordSelected: (DietRecord) -> Unit,
) {
    var menuExpanded by remember(record.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(2f).padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextOverflowText(
                text = formatRecordTimestamp(record.mealStartAt, RecordTimePrecision.MINUTE),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflowMode = "ellipsis",
                modifier = Modifier.fillMaxWidth(),
            )
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth().then(
                        if (sameDayRecords.isNotEmpty()) Modifier.clickable { menuExpanded = true } else Modifier,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextOverflowText(
                        text = stringResource(record.mealPeriod.displayRes()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflowMode = "ellipsis",
                        modifier = Modifier.weight(1f),
                    )
                    if (sameDayRecords.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.diet_event_analysis_meal_selector),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    sameDayRecords.forEach { candidate ->
                        DropdownMenuItem(
                            text = {
                                TextOverflowText(
                                    text = stringResource(
                                        R.string.diet_event_analysis_meal_option,
                                        stringResource(candidate.mealPeriod.displayRes()),
                                        formatRecordClock(candidate.mealStartAt).take(5),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    overflowMode = "ellipsis",
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onRecordSelected(candidate)
                            },
                        )
                    }
                }
            }
        }
        DietEventMetricCard(
            label = stringResource(R.string.diet_event_analysis_glycemic_rise),
            value = analysis.glycemicRiseMmolPerL?.let { formatGlucoseValue(it, unitId) } ?: "-",
            footer = unitLabel,
            important = true,
            modifier = Modifier.weight(1f),
        )
        DietEventMetricCard(
            label = stringResource(R.string.diet_event_analysis_variability),
            value = analysis.variabilityMmolPerL?.let { formatGlucoseValue(it, unitId) } ?: "-",
            footer = unitLabel,
            important = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DietEventTimePointMetrics(
    analysis: DietEventGlucoseAnalysis,
    mealStartAt: Long,
    unitId: String,
    unitLabel: String,
) {
    val points = listOf(
        -1 to R.string.diet_event_analysis_pre_meal_one_hour,
        0 to R.string.diet_event_analysis_at_meal_time,
        1 to R.string.diet_event_analysis_after_meal_one_hour,
        2 to R.string.diet_event_analysis_after_meal_two_hours,
        3 to R.string.diet_event_analysis_after_meal_three_hours,
        4 to R.string.diet_event_analysis_after_meal_four_hours,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        points.forEach { (offsetHours, labelRes) ->
            val sample = analysis.observedAtOffsetHours(offsetHours)
            val targetTime = mealStartAt + offsetHours * HOUR_MILLIS
            DietEventMetricCard(
                label = stringResource(labelRes),
                value = sample?.let { formatGlucoseValue(it.valueMmolPerL, unitId) } ?: "-",
                footer = stringResource(
                    R.string.diet_event_analysis_measurement_meta,
                    formatRecordClock(sample?.timestamp ?: targetTime).take(5),
                    unitLabel,
                ),
                important = offsetHours == 0 || offsetHours == 2,
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DietEventMetricCard(
    label: String,
    value: String,
    footer: String,
    important: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (important) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    }
    val contentColor = if (important) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = if (important) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = modifier.height(if (compact) 72.dp else 96.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (compact) 5.dp else 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TextOverflowText(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryContentColor,
                maxLines = 2,
                overflowMode = "ellipsis",
                modifier = Modifier.fillMaxWidth(),
            )
            TextOverflowText(
                text = value,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = contentColor,
                modifier = Modifier.fillMaxWidth(),
            )
            TextOverflowText(footer, style = MaterialTheme.typography.labelSmall, color = secondaryContentColor, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DietEventOutcomeMetrics(
    analysis: DietEventGlucoseAnalysis,
    unitId: String,
    unitLabel: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DietEventMetricCard(
            label = stringResource(R.string.diet_event_analysis_post_meal_peak),
            value = analysis.postMealPeak?.let { formatGlucoseValue(it.valueMmolPerL, unitId) } ?: "-",
            footer = analysis.postMealPeak?.let {
                stringResource(R.string.diet_event_analysis_measurement_meta, formatRecordClock(it.timestamp).take(5), unitLabel)
            } ?: unitLabel,
            important = true,
            modifier = Modifier.weight(1f),
        )
        DietEventMetricCard(
            label = stringResource(R.string.diet_event_analysis_time_to_peak),
            value = analysis.timeToPostMealPeakMillis?.let { formatDietEventDuration(it) } ?: "-",
            footer = analysis.postMealPeak?.let { formatRecordClock(it.timestamp).take(5) } ?: "-",
            important = true,
            modifier = Modifier.weight(1f),
        )
        DietEventMetricCard(
            label = stringResource(R.string.diet_event_analysis_time_to_recovery),
            value = analysis.timeToPreMealRecoveryMillis?.let { formatDietEventDuration(it) } ?: "-",
            footer = analysis.recoveryToPreMeal?.let { formatRecordClock(it.timestamp).take(5) } ?: "-",
            important = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun formatDietEventDuration(millis: Long): String {
    val minutes = (millis / 60_000L).toInt()
    val hours = minutes / 60
    return if (hours > 0) {
        stringResource(R.string.blood_glucose_chart_target_time_hours_minutes, hours, minutes % 60)
    } else {
        stringResource(R.string.blood_glucose_chart_target_time_minutes, minutes)
    }
}

@Composable
private fun DietEventGlucoseChart(
    analysis: DietEventGlucoseAnalysis,
    mealStartAt: Long,
    unitId: String,
    unitLabel: String,
    targetRange: UnitRange<Float>,
) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface
    val axisLabels = listOf(
        stringResource(R.string.diet_event_analysis_axis_before_meal),
        stringResource(R.string.diet_event_analysis_axis_meal_time, formatRecordClock(mealStartAt).take(5)),
        stringResource(R.string.diet_event_analysis_axis_after_meal, 1),
        stringResource(R.string.diet_event_analysis_axis_after_meal, 2),
        stringResource(R.string.diet_event_analysis_axis_after_meal, 3),
        stringResource(R.string.diet_event_analysis_axis_after_meal, 4),
    )
    var crosshair by remember(analysis.records) { mutableStateOf<DietEventGlucoseCrosshair?>(null) }
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(top = 10.dp, bottom = 8.dp)) {
            Box(Modifier.fillMaxWidth().height(210.dp)) {
                Canvas(
                    Modifier.fillMaxSize().onSizeChanged { chartSize = it }
                        .pointerInput(analysis.records, analysis.windowStart, analysis.windowEnd) {
                            detectTapGestures { tap -> crosshair = dietEventCrosshairAt(tap, chartSize, analysis) }
                        },
                ) {
                    drawDietEventChart(
                        analysis, targetRange, unitId, unitLabel, axisColor, gridColor, primaryColor,
                        targetColor, surfaceColor, crosshair,
                    )
                }
                crosshair?.let { selected ->
                    Surface(
                        color = surfaceColor.copy(alpha = 0.92f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    ) {
                        TextOverflowText(
                            text = stringResource(
                                R.string.diet_event_analysis_crosshair_value,
                                formatRecordTimestamp(selected.record.timestamp, RecordTimePrecision.MINUTE),
                                formatGlucoseValue(selected.record.valueMmolPerL, unitId),
                                unitLabel,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflowMode = "ellipsis",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                axisLabels.forEach { label ->
                    TextOverflowText(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflowMode = "ellipsis",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun dietEventCrosshairAt(tap: Offset, chartSize: IntSize, analysis: DietEventGlucoseAnalysis): DietEventGlucoseCrosshair? {
    if (analysis.records.isEmpty() || chartSize.width <= 0) return null
    val left = 14f
    val right = chartSize.width - 48f
    if (tap.x !in left..right) return null
    val fraction = ((tap.x - left) / (right - left)).coerceIn(0f, 1f)
    val timestamp = analysis.windowStart + ((analysis.windowEnd - analysis.windowStart) * fraction).toLong()
    return analysis.records.minByOrNull { abs(it.timestamp - timestamp) }?.let(::DietEventGlucoseCrosshair)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDietEventChart(
    analysis: DietEventGlucoseAnalysis,
    targetRange: UnitRange<Float>,
    unitId: String,
    unitLabel: String,
    axisColor: Color,
    gridColor: Color,
    primaryColor: Color,
    targetColor: Color,
    surfaceColor: Color,
    crosshair: DietEventGlucoseCrosshair?,
) {
    val left = 14f
    val right = size.width - 48f
    val top = 10f
    val bottom = size.height - 12f
    val width = right - left
    val height = bottom - top
    if (width <= 0f || height <= 0f) return
    val maxObserved = analysis.records.maxOfOrNull(BloodGlucoseRecord::valueMmolPerL) ?: 0.0
    val targetMaximum = targetRange.max?.toDouble() ?: 0.0
    val maximum = maxOf(8.0, ceil(maxOf(maxObserved, targetMaximum) / 2.0) * 2.0)
    fun x(timestamp: Long): Float = left + ((timestamp - analysis.windowStart).toFloat() / (analysis.windowEnd - analysis.windowStart)) * width
    fun y(value: Double): Float = bottom - (value / maximum * height).toFloat()
    targetRange.min?.let { min -> targetRange.max?.let { max ->
        drawRect(targetColor.copy(alpha = 0.48f), Offset(left, y(max.toDouble())), Size(width, y(min.toDouble()) - y(max.toDouble())))
    } }
    generateSequence(0.0) { it + 2.0 }.takeWhile { it <= maximum }.forEach { value ->
        val lineY = y(value)
        drawLine(gridColor, Offset(left, lineY), Offset(right, lineY))
        drawContext.canvas.nativeCanvas.drawText(formatGlucoseValue(value, unitId), right + 5f, lineY + 4f, dietAnalysisAxisPaint(axisColor))
    }
    (0..5).forEach { index -> drawLine(gridColor, Offset(left + width * index / 5f, top), Offset(left + width * index / 5f, bottom)) }
    drawLine(axisColor, Offset(right, top), Offset(right, bottom), 2f)
    drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), 2f)
    drawContext.canvas.nativeCanvas.drawText(unitLabel, right - 10f, top + 10f, dietAnalysisAxisPaint(axisColor))
    val path = Path()
    analysis.records.zipWithNext().forEach { (current, next) ->
        if (next.timestamp - current.timestamp <= HOUR_MILLIS) {
            path.moveTo(x(current.timestamp), y(current.valueMmolPerL))
            path.lineTo(x(next.timestamp), y(next.valueMmolPerL))
        }
    }
    drawPath(path, primaryColor, style = Stroke(3f, cap = StrokeCap.Round))
    analysis.records.forEach { record -> drawCircle(primaryColor, 4.5f, Offset(x(record.timestamp), y(record.valueMmolPerL))) }
    crosshair?.let { selected ->
        val center = Offset(x(selected.record.timestamp), y(selected.record.valueMmolPerL))
        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        drawLine(axisColor.copy(alpha = 0.72f), Offset(center.x, top), Offset(center.x, bottom), 1.6f, pathEffect = dash)
        drawLine(axisColor.copy(alpha = 0.72f), Offset(left, center.y), Offset(right, center.y), 1.6f, pathEffect = dash)
        drawCircle(surfaceColor, 6.5f, center)
        drawCircle(primaryColor, 4.2f, center)
    }
}

private fun dietAnalysisAxisPaint(color: Color) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    this.color = android.graphics.Color.argb((color.alpha * 220).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
    textSize = 20f
}

private data class DietEventGlucoseCrosshair(val record: BloodGlucoseRecord)

private const val HOUR_MILLIS = 60 * 60 * 1_000L

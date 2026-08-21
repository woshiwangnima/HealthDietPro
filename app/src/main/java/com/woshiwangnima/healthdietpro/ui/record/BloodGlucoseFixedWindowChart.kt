package com.woshiwangnima.healthdietpro.ui.record

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartIndex
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartSlice
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartWindow
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseDiabetesType
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.scopedSlice
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Blood-glucose-only fixed-duration chart. It intentionally does not use ComposeBaseChart. */
@Composable
internal fun BloodGlucoseFixedWindowChart(
    records: List<BloodGlucoseRecord>,
    scopeStart: Long,
    scopeEnd: Long,
    window: BloodGlucoseChartWindow,
    diabetesType: BloodGlucoseDiabetesType,
    modifier: Modifier = Modifier,
    sessionWindowEnd: Long? = null,
    onSessionWindowEndChanged: (Long?) -> Unit = {},
) {
    val index = remember(records) { BloodGlucoseChartIndex(records) }
    val initialEnd = remember(index, scopeStart, scopeEnd) {
        index.slice(scopeStart, scopeEnd).lastOrNull()?.timestamp ?: scopeEnd
    }
    var ownedWindowEnd by remember(scopeStart, scopeEnd, window) { mutableLongStateOf(initialEnd) }
    val windowEnd = sessionWindowEnd ?: ownedWindowEnd
    var primaryStyle by remember { mutableStateOf(SeriesStyle(Color(0xFF1976D2), lineStyle = GlucoseLineStyle.Spline)) }
    var delayedStyle by remember { mutableStateOf(SeriesStyle(Color(0xFFF57C00), lineStyle = GlucoseLineStyle.Spline, linePattern = GlucoseLinePattern.Dotted, pointShape = GlucosePointShape.Cross)) }
    var selectedSeries by remember { mutableStateOf(SeriesKind.Primary) }
    var styleDialogSeries by remember { mutableStateOf<SeriesKind?>(null) }
    var fullscreen by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTargetRateHelp by remember { mutableStateOf(false) }
    LaunchedEffect(initialEnd, scopeStart, scopeEnd, window) {
        if (sessionWindowEnd == null) ownedWindowEnd = initialEnd
    }
    val slice = remember(index, scopeStart, scopeEnd, windowEnd, window) {
        index.scopedSlice(scopeStart, scopeEnd, windowEnd, window)
    }
    val earliest = slice.scoped.firstOrNull()?.timestamp ?: scopeStart
    val latest = slice.scoped.lastOrNull()?.timestamp ?: scopeEnd
    val setWindowEnd: (Long) -> Unit = { timestamp ->
        val clamped = timestamp.coerceIn(earliest, latest)
        ownedWindowEnd = clamped
        onSessionWindowEndChanged(clamped)
    }

    GlucoseChartSurface(
        slice = slice,
        window = window,
        diabetesType = diabetesType,
        primaryStyle = primaryStyle,
        delayedStyle = delayedStyle,
        selectedSeries = selectedSeries,
        onSelectedSeries = { series ->
            if (series == selectedSeries) styleDialogSeries = series else selectedSeries = series
        },
        onWindowEndChanged = setWindowEnd,
        onDateClick = { showDatePicker = true },
        onFullscreen = { fullscreen = true },
        onTargetRateHelp = { showTargetRateHelp = true },
        modifier = modifier,
    )
    styleDialogSeries?.let { series ->
        SeriesStyleDialog(
            label = if (series == SeriesKind.Primary) stringResource(R.string.blood_glucose_chart_primary_series)
            else stringResource(R.string.blood_glucose_delayed_series, window.durationMillis / HOUR_MILLIS),
            style = if (series == SeriesKind.Primary) primaryStyle else delayedStyle,
            onStyleChanged = { if (series == SeriesKind.Primary) primaryStyle = it else delayedStyle = it },
            onDismiss = { styleDialogSeries = null },
        )
    }
    if (showDatePicker) {
        ComposeDatePickerDialog(
            initialMillis = slice.windowStart,
            onDismiss = { showDatePicker = false },
            onDatePicked = { date ->
                val selectedStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                setWindowEnd(selectedStart + window.durationMillis)
                showDatePicker = false
            },
        )
    }
    if (fullscreen) {
        Dialog(onDismissRequest = { fullscreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                FixedLandscapeBox {
                    GlucoseChartSurface(
                        slice = slice,
                        window = window,
                        diabetesType = diabetesType,
                        primaryStyle = primaryStyle,
                        delayedStyle = delayedStyle,
                        selectedSeries = selectedSeries,
                        onSelectedSeries = { series ->
                            if (series == selectedSeries) styleDialogSeries = series else selectedSeries = series
                        },
                        onWindowEndChanged = setWindowEnd,
                        onDateClick = {
                            fullscreen = false
                            showDatePicker = true
                        },
                        onFullscreen = { },
                        onTargetRateHelp = { showTargetRateHelp = true },
                        onExitFullscreen = { fullscreen = false },
                        fullscreen = true,
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                    )
                }
            }
        }
    }
    if (showTargetRateHelp) {
        AlertDialog(
            onDismissRequest = { showTargetRateHelp = false },
            title = { Text(stringResource(R.string.blood_glucose_chart_target_rate_title)) },
            text = { Text(stringResource(R.string.blood_glucose_chart_target_rate_help, glucoseRangeText(diabetesType))) },
            confirmButton = { TextButton(onClick = { showTargetRateHelp = false }) { Text(stringResource(R.string.compose_confirm_dialog_ok)) } },
        )
    }
}

@Composable
private fun FixedLandscapeBox(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val modifier = if (maxWidth < maxHeight) {
            Modifier.requiredWidth(maxHeight).requiredHeight(maxWidth).graphicsLayer(rotationZ = 90f)
        } else {
            Modifier.fillMaxSize()
        }
        Box(modifier) { content() }
    }
}

@Composable
private fun GlucoseChartSurface(
    slice: BloodGlucoseChartSlice,
    window: BloodGlucoseChartWindow,
    diabetesType: BloodGlucoseDiabetesType,
    primaryStyle: SeriesStyle,
    delayedStyle: SeriesStyle,
    selectedSeries: SeriesKind,
    onSelectedSeries: (SeriesKind) -> Unit,
    onWindowEndChanged: (Long) -> Unit,
    onDateClick: () -> Unit,
    onFullscreen: () -> Unit,
    onTargetRateHelp: () -> Unit,
    modifier: Modifier,
    fullscreen: Boolean = false,
    onExitFullscreen: (() -> Unit)? = null,
) {
    val primary = remember(slice.primary) { slice.primary.map { RenderedPoint(it, it.timestamp, false) } }
    val delayed = remember(slice.delayed, window) { slice.delayed.map { RenderedPoint(it, it.timestamp + window.durationMillis, true) } }
    val earliest = slice.scoped.firstOrNull()?.timestamp ?: slice.windowStart
    val latest = slice.scoped.lastOrNull()?.timestamp ?: slice.windowEnd
    val palette = ChartPalette(
        primary = primaryStyle.color.copy(alpha = primaryStyle.alpha),
        delayed = delayedStyle.color.copy(alpha = delayedStyle.alpha),
        axis = MaterialTheme.colorScheme.onSurfaceVariant,
        grid = MaterialTheme.colorScheme.outlineVariant,
        target = MaterialTheme.colorScheme.secondaryContainer,
        surface = MaterialTheme.colorScheme.surface,
    )
    var crosshair by remember { mutableStateOf<GlucoseCrosshair?>(null) }
    val currentSlice by rememberUpdatedState(slice)
    val currentPrimary by rememberUpdatedState(primary)
    val currentDelayed by rememberUpdatedState(delayed)
    val currentSelectedSeries by rememberUpdatedState(selectedSeries)
    val currentPrimaryStyle by rememberUpdatedState(primaryStyle)
    val currentDelayedStyle by rememberUpdatedState(delayedStyle)
    val delayedLabel = stringResource(R.string.blood_glucose_delayed_series, window.durationMillis / HOUR_MILLIS)
    val xAxisUnit = stringResource(R.string.blood_glucose_chart_x_axis_unit)
    val yAxisUnit = stringResource(R.string.blood_glucose_chart_y_axis_unit)

    Column(modifier) {
        if (slice.scoped.isEmpty()) {
            Text(stringResource(R.string.blood_glucose_chart_no_data_in_scope), modifier = Modifier.align(Alignment.CenterHorizontally), color = palette.axis)
            return@Column
        }
        val drawingModifier = if (fullscreen) {
            Modifier.fillMaxWidth().weight(1f)
        } else {
            Modifier.fillMaxWidth().height(210.dp)
        }
        BoxWithConstraints(drawingModifier) {
            val currentMidnight = remember(slice.windowStart) {
                Instant.ofEpochMilli(slice.windowStart).atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            val nextMidnight = currentMidnight + 24 * HOUR_MILLIS
            val dateMarker = when {
                currentMidnight == slice.windowStart -> currentMidnight
                nextMidnight <= slice.windowEnd -> nextMidnight
                else -> slice.windowStart
            }
            val dateMarkerFraction = (dateMarker - slice.windowStart).toFloat() /
                (slice.windowEnd - slice.windowStart).coerceAtLeast(1L)
            val dateMarkerOffset = (maxWidth - 132.dp) * dateMarkerFraction
            val markerLabel = remember(dateMarker) {
                Instant.ofEpochMilli(dateMarker).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_LABEL_FORMATTER)
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput("glucose-crosshair") {
                        detectTapGestures { offset ->
                            crosshair = crosshairAt(offset, size.width, size.height, currentSlice, if (currentSelectedSeries == SeriesKind.Primary) currentPrimary else currentDelayed, if (currentSelectedSeries == SeriesKind.Primary) currentPrimaryStyle else currentDelayedStyle)
                        }
                    },
            ) {
                drawGlucoseChart(slice, window, diabetesType, primary, delayed, palette, primaryStyle, delayedStyle, crosshair, xAxisUnit, yAxisUnit)
            }
            if (fullscreen) {
                IconButton(onClick = { onExitFullscreen?.invoke() }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, stringResource(R.string.view_chart_exit_fullscreen))
                }
            } else {
                AppOutlinedIconTextButton(
                    text = stringResource(R.string.view_chart_fullscreen),
                    iconRes = R.drawable.ic_fullscreen,
                    onClick = onFullscreen,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            AppOutlinedIconTextButton(
                text = markerLabel,
                iconRes = R.drawable.ic_birthday,
                onClick = onDateClick,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp + dateMarkerOffset, bottom = 3.dp).width(120.dp),
            )
            crosshair?.let { value ->
                CrosshairInfo(value, palette, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            }
        }
        HorizontalPanArea(
            window = window,
            windowEnd = slice.windowEnd,
            earliest = earliest,
            latest = latest,
            primaryActionColor = MaterialTheme.colorScheme.primary,
            onWindowEndChanged = onWindowEndChanged,
        )
        ChartLegend(primaryStyle, stringResource(R.string.blood_glucose_chart_primary_series), delayedStyle, delayedLabel, selectedSeries, onSelectedSeries)
        if (!fullscreen) {
            GlucoseStatisticsCard(primary, diabetesType, window, onTargetRateHelp)
        }
    }
}

@Composable
private fun CrosshairInfo(value: GlucoseCrosshair, palette: ChartPalette, modifier: Modifier) {
    val originalTimestamp = if (value.series == SeriesKind.Delayed) value.drawingTimestamp - value.delayMillis else value.drawingTimestamp
    val time = Instant.ofEpochMilli(originalTimestamp).atZone(ZoneId.systemDefault()).format(CROSSHAIR_TIME_FORMATTER)
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f), shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = buildString {
                append(if (value.series == SeriesKind.Primary) stringResource(R.string.blood_glucose_chart_primary_series) else stringResource(R.string.blood_glucose_delayed_series, value.delayMillis / HOUR_MILLIS)).append('\n')
                append("X: ").append(time).append('\n')
                append("Y: ").append(String.format(Locale.getDefault(), "%.2f mmol/L", value.value))
            },
            color = palette.axis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun GlucoseStatisticsCard(
    primary: List<RenderedPoint>,
    diabetesType: BloodGlucoseDiabetesType,
    window: BloodGlucoseChartWindow,
    onTargetRateHelp: () -> Unit,
) {
    val records = primary.map(RenderedPoint::record)
    val target = diabetesType.glucoseReferenceRangeMmolPerL
    val inRange = records.count { record ->
        (target.min == null || record.valueMmolPerL >= target.min.toDouble()) && (target.max == null || record.valueMmolPerL <= target.max.toDouble())
    }
    val rate = records.takeIf { it.isNotEmpty() }?.let { inRange.toDouble() / it.size * 100.0 }
    val highest = records.maxByOrNull(BloodGlucoseRecord::valueMmolPerL)
    val lowest = records.minByOrNull(BloodGlucoseRecord::valueMmolPerL)
    val fluctuation = if (highest != null && lowest != null) highest.valueMmolPerL - lowest.valueMmolPerL else null
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TargetRateCard(
                label = stringResource(R.string.blood_glucose_chart_target_rate, window.durationMillis / HOUR_MILLIS),
                value = rate?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "?",
                onHelp = onTargetRateHelp,
            )
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricCard(
                    label = stringResource(R.string.blood_glucose_chart_fluctuation),
                    value = fluctuation?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "?",
                    unit = "mmol/L",
                    timestamp = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                MetricCard(
                    label = stringResource(R.string.blood_glucose_chart_highest),
                    value = highest?.let { String.format(Locale.getDefault(), "%.2f", it.valueMmolPerL) } ?: "?",
                    unit = "mmol/L",
                    timestamp = highest?.timestamp?.let(::formatStatisticTimestamp),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                MetricCard(
                    label = stringResource(R.string.blood_glucose_chart_lowest),
                    value = lowest?.let { String.format(Locale.getDefault(), "%.2f", it.valueMmolPerL) } ?: "?",
                    unit = "mmol/L",
                    timestamp = lowest?.timestamp?.let(::formatStatisticTimestamp),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun TargetRateCard(label: String, value: String, onHelp: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            TooltipLabel(label, Modifier.weight(1f))
            IconButton(onClick = onHelp, modifier = Modifier.size(22.dp)) {
                Icon(painterResource(R.drawable.ic_help), stringResource(R.string.blood_glucose_chart_target_rate_title), modifier = Modifier.size(16.dp))
            }
        }
            StatisticValue(value, Modifier.weight(1f), TextAlign.End)
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, unit: String, timestamp: String?, modifier: Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                TooltipLabel(label, Modifier.weight(1f))
                TooltipLabel(unit, Modifier.weight(1f), textAlign = TextAlign.End)
            }
            StatisticValue(value)
            if (timestamp != null) TooltipLabel(timestamp)
        }
    }
}

@Composable
private fun TooltipLabel(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    TextOverflowText(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
        textAlign = textAlign,
    )
}

@Composable
private fun StatisticValue(value: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    TextOverflowText(
        text = value,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth(),
        textAlign = textAlign,
    )
}

private fun formatStatisticTimestamp(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(RECORD_TIME_FORMATTER)

private fun glucoseRangeText(diabetesType: BloodGlucoseDiabetesType): String {
    val range = diabetesType.glucoseReferenceRangeMmolPerL
    val min = range.min?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-∞"
    val max = range.max?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "∞"
    return "$min-$max mmol/L"
}

@Composable
private fun ChartLegend(
    primaryStyle: SeriesStyle,
    primaryLabel: String,
    delayedStyle: SeriesStyle,
    delayedLabel: String,
    selected: SeriesKind,
    onSelected: (SeriesKind) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.Center) {
        LegendItem(SeriesKind.Primary, primaryStyle, primaryLabel, selected == SeriesKind.Primary, onSelected)
        LegendItem(SeriesKind.Delayed, delayedStyle, delayedLabel, selected == SeriesKind.Delayed, onSelected)
    }
}

@Composable
private fun HorizontalPanArea(
    window: BloodGlucoseChartWindow,
    windowEnd: Long,
    earliest: Long,
    latest: Long,
    primaryActionColor: Color,
    onWindowEndChanged: (Long) -> Unit,
) {
    val currentWindowEnd by rememberUpdatedState(windowEnd)
    val currentEarliest by rememberUpdatedState(earliest)
    val currentLatest by rememberUpdatedState(latest)
    val currentWindow by rememberUpdatedState(window)
    val onCurrentWindowEndChanged by rememberUpdatedState(onWindowEndChanged)
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.blood_glucose_chart_pan_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Canvas(
            Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f), MaterialTheme.shapes.small)
            .pointerInput("glucose-x-pan") {
                var dragStartEnd = currentWindowEnd
                var totalDragX = 0f
                detectDragGestures(
                    onDragStart = {
                        dragStartEnd = currentWindowEnd
                        totalDragX = 0f
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        totalDragX += drag.x
                        // Time moves with the user's finger: left is older, right is newer.
                        val delta = (totalDragX / size.width.coerceAtLeast(1) * currentWindow.durationMillis).toLong()
                        onCurrentWindowEndChanged((dragStartEnd + delta).coerceIn(currentEarliest, currentLatest))
                    },
                )
            },
        ) {
            val range = (latest - earliest).coerceAtLeast(1L)
            val visible = (window.durationMillis.toFloat() / range).coerceIn(0.16f, 1f)
            val endFraction = ((windowEnd - earliest).toFloat() / range).coerceIn(visible, 1f)
            val indicatorWidth = size.width * visible
            val indicatorLeft = (size.width * endFraction - indicatorWidth).coerceIn(0f, size.width - indicatorWidth)
            drawRoundRect(primaryActionColor.copy(alpha = 0.22f), Offset(0f, (size.height - 4f) / 2f), androidx.compose.ui.geometry.Size(size.width, 4f), CornerRadius(2f, 2f))
            drawRoundRect(primaryActionColor, Offset(indicatorLeft, (size.height - 4f) / 2f), androidx.compose.ui.geometry.Size(indicatorWidth, 4f), CornerRadius(2f, 2f))
        }
    }
}

@Composable
private fun LegendItem(kind: SeriesKind, style: SeriesStyle, label: String, selected: Boolean, onSelected: (SeriesKind) -> Unit) {
    Surface(
        onClick = { onSelected(kind) },
        shape = MaterialTheme.shapes.extraSmall,
        color = if (selected) style.color.copy(alpha = 0.12f) else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, style.color.copy(alpha = 0.42f)) else null,
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(28.dp, 12.dp)) {
                drawLine(style.color.copy(alpha = style.alpha), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3f, StrokeCap.Round, style.linePattern.effect())
                drawLegendPoint(style, Offset(size.width / 2, size.height / 2))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
            if (selected) Text(stringResource(R.string.compose_chart_selected_series), style = MaterialTheme.typography.labelSmall, color = style.color, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun SeriesStyleDialog(label: String, style: SeriesStyle, onStyleChanged: (SeriesStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.blood_glucose_chart_series_style, label)) },
        text = {
            Column {
                Text(stringResource(R.string.blood_glucose_chart_color))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CHART_COLORS.forEach { color ->
                        Surface(onClick = { onStyleChanged(style.copy(color = color)) }, color = color, shape = MaterialTheme.shapes.small, modifier = Modifier.size(28.dp), border = if (color == style.color) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null) {}
                    }
                }
                Text(stringResource(R.string.blood_glucose_chart_opacity, (style.alpha * 100).toInt()))
                Slider(style.alpha, { onStyleChanged(style.copy(alpha = it)) }, valueRange = 0.1f..1f)
                AppDropdownField(
                    label = stringResource(R.string.view_chart_line_style),
                    value = stringResource(style.lineStyle.labelRes()),
                    options = GlucoseLineStyle.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                    onSelect = { option -> onStyleChanged(style.copy(lineStyle = GlucoseLineStyle.valueOf(option.id))) },
                )
                AppDropdownField(
                    label = stringResource(R.string.compose_chart_line_pattern),
                    value = stringResource(style.linePattern.labelRes()),
                    options = GlucoseLinePattern.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                    onSelect = { option -> onStyleChanged(style.copy(linePattern = GlucoseLinePattern.valueOf(option.id))) },
                )
                AppDropdownField(
                    label = stringResource(R.string.compose_chart_point_shape),
                    value = stringResource(style.pointShape.labelRes()),
                    options = GlucosePointShape.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                    onSelect = { option -> onStyleChanged(style.copy(pointShape = GlucosePointShape.valueOf(option.id))) },
                )
                AppDropdownField(
                    label = stringResource(R.string.compose_chart_point_fill),
                    value = stringResource(style.pointFill.labelRes()),
                    options = GlucosePointFill.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                    onSelect = { option -> onStyleChanged(style.copy(pointFill = GlucosePointFill.valueOf(option.id))) },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_ok)) } },
    )
}

private fun crosshairAt(touch: Offset, width: Int, height: Int, slice: BloodGlucoseChartSlice, points: List<RenderedPoint>, style: SeriesStyle): GlucoseCrosshair? {
    val bounds = ChartBounds(width.toFloat(), height.toFloat())
    if (touch.x !in bounds.left..bounds.right || touch.y !in bounds.top..bounds.bottom) return null
    val rawX = slice.windowStart + ((touch.x - bounds.left) / bounds.width * (slice.windowEnd - slice.windowStart)).toLong()
    val nextIndex = points.binarySearchBy(rawX) { it.drawingTimestamp }.let { if (it < 0) -it - 1 else it }
    val pair = points.getOrNull(nextIndex - 1)?.let { start ->
        points.getOrNull(nextIndex)?.let { end -> start to end }
    }?.takeIf { (start, end) ->
        rawX in start.drawingTimestamp..end.drawingTimestamp && end.drawingTimestamp - start.drawingTimestamp <= MAX_CONNECTED_GAP_MILLIS
    } ?: return null
    val fraction = (rawX - pair.first.drawingTimestamp).toDouble() / (pair.second.drawingTimestamp - pair.first.drawingTimestamp)
    val value = when (style.lineStyle) {
        GlucoseLineStyle.SteppedFront -> pair.first.record.valueMmolPerL
        GlucoseLineStyle.SteppedBack -> pair.second.record.valueMmolPerL
        GlucoseLineStyle.Bezier, GlucoseLineStyle.Spline, GlucoseLineStyle.Monotone -> {
            val eased = fraction * fraction * (3.0 - 2.0 * fraction)
            pair.first.record.valueMmolPerL + (pair.second.record.valueMmolPerL - pair.first.record.valueMmolPerL) * eased
        }
        else -> pair.first.record.valueMmolPerL + (pair.second.record.valueMmolPerL - pair.first.record.valueMmolPerL) * fraction
    }
    return GlucoseCrosshair(rawX, value, if (pair.first.delayed) SeriesKind.Delayed else SeriesKind.Primary, if (pair.first.delayed) slice.windowEnd - slice.windowStart else 0L)
}

private fun DrawScope.drawGlucoseChart(
    slice: BloodGlucoseChartSlice,
    window: BloodGlucoseChartWindow,
    diabetesType: BloodGlucoseDiabetesType,
    primary: List<RenderedPoint>,
    delayed: List<RenderedPoint>,
    palette: ChartPalette,
    primaryStyle: SeriesStyle,
    delayedStyle: SeriesStyle,
    crosshair: GlucoseCrosshair?,
    xAxisUnit: String,
    yAxisUnit: String,
) {
    val bounds = ChartBounds(size.width, size.height)
    if (bounds.width <= 0f || bounds.height <= 0f) return
    fun x(timestamp: Long) = bounds.left + (timestamp - slice.windowStart).toFloat() / (slice.windowEnd - slice.windowStart) * bounds.width
    fun y(value: Double) = bounds.bottom - (value / slice.historicalMaximum * bounds.height).toFloat()
    diabetesType.glucoseReferenceRangeMmolPerL.min?.let { min -> diabetesType.glucoseReferenceRangeMmolPerL.max?.let { max ->
        drawRect(palette.target.copy(alpha = 0.48f), Offset(bounds.left, y(max.toDouble())), androidx.compose.ui.geometry.Size(bounds.width, y(min.toDouble()) - y(max.toDouble())))
    } }
    val highestTick = kotlin.math.floor(slice.historicalMaximum / Y_TICK_INTERVAL) * Y_TICK_INTERVAL
    generateSequence(0.0) { it + Y_TICK_INTERVAL }.takeWhile { it <= highestTick }.forEach { value ->
        val lineY = y(value)
        drawLine(palette.grid, Offset(bounds.left, lineY), Offset(bounds.right, lineY))
        drawContext.canvas.nativeCanvas.drawText(String.format(Locale.getDefault(), "%.1f", value), bounds.right + 6f, lineY + 4f, axisPaint(palette.axis))
    }
    generateSequence(slice.windowStart) { it + window.tickMillis }.takeWhile { it <= slice.windowEnd }.forEach { tick ->
        val tickX = x(tick)
        drawLine(palette.grid, Offset(tickX, bounds.top), Offset(tickX, bounds.bottom))
        drawContext.canvas.nativeCanvas.drawText(Instant.ofEpochMilli(tick).atZone(ZoneId.systemDefault()).format(TIME_LABEL_FORMATTER), tickX - 14f, bounds.bottom + 18f, axisPaint(palette.axis))
    }
    drawLine(palette.axis, Offset(bounds.right, bounds.top), Offset(bounds.right, bounds.bottom), 2f)
    drawLine(palette.axis, Offset(bounds.left, bounds.bottom), Offset(bounds.right, bounds.bottom), 2f)
    drawContext.canvas.nativeCanvas.drawText(yAxisUnit, bounds.right - 8f, bounds.top + 10f, axisPaint(palette.axis))
    drawContext.canvas.nativeCanvas.drawText(xAxisUnit, bounds.left, bounds.bottom + 36f, axisPaint(palette.axis))
    val targetRange = diabetesType.glucoseReferenceRangeMmolPerL
    targetRange.min?.let { drawContext.canvas.nativeCanvas.drawText(String.format(Locale.getDefault(), "%.1f", it), 2f, y(it.toDouble()) + 4f, axisPaint(Color(0xFFF57C00))) }
    targetRange.max?.let { drawContext.canvas.nativeCanvas.drawText(String.format(Locale.getDefault(), "%.1f", it), 2f, y(it.toDouble()) + 4f, axisPaint(Color(0xFFF57C00))) }
    val visibleValues = primary.map { it.record.valueMmolPerL }
    visibleValues.maxOrNull()?.let { drawContext.canvas.nativeCanvas.drawText(String.format(Locale.getDefault(), "%.2f", it), 2f, y(it) + 4f, axisPaint(palette.primary)) }
    visibleValues.minOrNull()?.takeIf { it != visibleValues.maxOrNull() }?.let { drawContext.canvas.nativeCanvas.drawText(String.format(Locale.getDefault(), "%.2f", it), 2f, y(it) + 4f, axisPaint(palette.primary)) }
    drawSeries(primary, ::x, ::y, palette.primary, primaryStyle)
    drawSeries(delayed, ::x, ::y, palette.delayed, delayedStyle)
    crosshair?.let { value ->
        if (value.drawingTimestamp in slice.windowStart..slice.windowEnd && value.value in 0.0..slice.historicalMaximum) {
            val pointX = x(value.drawingTimestamp)
            val pointY = y(value.value)
            val cross = palette.axis.copy(alpha = 0.72f)
            val selectedColor = if (value.series == SeriesKind.Delayed) palette.delayed else palette.primary
            drawLine(cross, Offset(pointX, bounds.top), Offset(pointX, bounds.bottom), 1.6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawLine(cross, Offset(bounds.left, pointY), Offset(bounds.right, pointY), 1.6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawCircle(palette.surface, 6f, Offset(pointX, pointY))
            drawCircle(selectedColor, 4f, Offset(pointX, pointY))
        }
    }
}

private fun DrawScope.drawSeries(points: List<RenderedPoint>, x: (Long) -> Float, y: (Double) -> Float, color: Color, style: SeriesStyle) {
    val path = Path()
    points.zipWithNext().forEach { (current, next) ->
        if (next.drawingTimestamp - current.drawingTimestamp <= MAX_CONNECTED_GAP_MILLIS) {
            val start = Offset(x(current.drawingTimestamp), y(current.record.valueMmolPerL))
            val end = Offset(x(next.drawingTimestamp), y(next.record.valueMmolPerL))
            path.moveTo(start.x, start.y)
            when (style.lineStyle) {
                GlucoseLineStyle.SteppedFront -> { path.lineTo(end.x, start.y); path.lineTo(end.x, end.y) }
                GlucoseLineStyle.SteppedBack -> { path.lineTo(start.x, end.y); path.lineTo(end.x, end.y) }
                GlucoseLineStyle.Bezier, GlucoseLineStyle.Spline, GlucoseLineStyle.Monotone -> path.quadraticTo((start.x + end.x) / 2f, start.y, end.x, end.y)
                else -> path.lineTo(end.x, end.y)
            }
        }
    }
    drawPath(path, color, style = Stroke(3f, cap = StrokeCap.Round, pathEffect = style.linePattern.effect()))
    points.forEach { drawPointShape(Offset(x(it.drawingTimestamp), y(it.record.valueMmolPerL)), color, style) }
}

private fun GlucoseLinePattern.effect(): androidx.compose.ui.graphics.PathEffect? = when (this) {
    GlucoseLinePattern.Solid -> null
    GlucoseLinePattern.Dashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(9f, 7f))
    GlucoseLinePattern.Dotted -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 6f))
    GlucoseLinePattern.DotDashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 5f, 9f, 5f))
}

private fun DrawScope.drawPointShape(center: Offset, color: Color, style: SeriesStyle) {
    val radius = 4.5f
    val fill = style.pointFill == GlucosePointFill.Filled
    val drawStyle = if (fill) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.8f)
    when (style.pointShape) {
        GlucosePointShape.Circle -> drawCircle(color, radius, center, style = drawStyle)
        GlucosePointShape.Square -> drawRect(color, Offset(center.x - radius, center.y - radius), androidx.compose.ui.geometry.Size(radius * 2, radius * 2), style = drawStyle)
        GlucosePointShape.Triangle -> drawPath(Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x - radius, center.y + radius)
            lineTo(center.x + radius, center.y + radius)
            close()
        }, color, style = drawStyle)
        GlucosePointShape.Diamond -> drawPath(Path().apply {
            moveTo(center.x, center.y - radius)
            lineTo(center.x - radius, center.y)
            lineTo(center.x, center.y + radius)
            lineTo(center.x + radius, center.y)
            close()
        }, color, style = drawStyle)
        GlucosePointShape.Cross -> {
            drawLine(color, Offset(center.x - radius, center.y - radius), Offset(center.x + radius, center.y + radius), 1.8f)
            drawLine(color, Offset(center.x - radius, center.y + radius), Offset(center.x + radius, center.y - radius), 1.8f)
        }
    }
}

private fun DrawScope.drawLegendPoint(style: SeriesStyle, center: Offset) = drawPointShape(center, style.color.copy(alpha = style.alpha), style)

private fun axisPaint(color: Color) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    this.color = android.graphics.Color.argb((color.alpha * 220).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
    textSize = 20f
}

private data class ChartBounds(val widthPx: Float, val heightPx: Float) {
    val left = 12f
    val right = widthPx - 52f
    val top = 12f
    val bottom = heightPx - 42f
    val width = right - left
    val height = bottom - top
}
private data class RenderedPoint(val record: BloodGlucoseRecord, val drawingTimestamp: Long, val delayed: Boolean)
private data class GlucoseCrosshair(val drawingTimestamp: Long, val value: Double, val series: SeriesKind, val delayMillis: Long)
private data class ChartPalette(val primary: Color, val delayed: Color, val axis: Color, val grid: Color, val target: Color, val surface: Color)
internal data class SeriesStyle(
    val color: Color,
    val alpha: Float = 1f,
    val lineStyle: GlucoseLineStyle = GlucoseLineStyle.Linear,
    val linePattern: GlucoseLinePattern = GlucoseLinePattern.Solid,
    val pointShape: GlucosePointShape = GlucosePointShape.Circle,
    val pointFill: GlucosePointFill = GlucosePointFill.Filled,
)
private enum class SeriesKind { Primary, Delayed }
internal enum class GlucoseLineStyle { Linear, Bezier, Spline, CatmullRom, Monotone, SteppedFront, SteppedBack }
internal enum class GlucoseLinePattern { Solid, Dashed, Dotted, DotDashed }
internal enum class GlucosePointShape { Circle, Triangle, Square, Diamond, Cross }
internal enum class GlucosePointFill { Filled, Hollow }

private fun GlucoseLineStyle.labelRes(): Int = when (this) {
    GlucoseLineStyle.Linear -> R.string.compose_chart_line_linear
    GlucoseLineStyle.Bezier -> R.string.compose_chart_line_bezier
    GlucoseLineStyle.Spline -> R.string.compose_chart_line_spline
    GlucoseLineStyle.CatmullRom -> R.string.compose_chart_line_catmull_rom
    GlucoseLineStyle.Monotone -> R.string.compose_chart_line_monotone
    GlucoseLineStyle.SteppedFront -> R.string.compose_chart_line_step_front
    GlucoseLineStyle.SteppedBack -> R.string.compose_chart_line_step_back
}
private fun GlucoseLinePattern.labelRes(): Int = when (this) {
    GlucoseLinePattern.Solid -> R.string.compose_chart_grid_solid
    GlucoseLinePattern.Dashed -> R.string.compose_chart_grid_dashed
    GlucoseLinePattern.Dotted -> R.string.compose_chart_grid_dotted
    GlucoseLinePattern.DotDashed -> R.string.compose_chart_grid_dot_dashed
}
private fun GlucosePointShape.labelRes(): Int = when (this) {
    GlucosePointShape.Circle -> R.string.compose_chart_point_circle
    GlucosePointShape.Triangle -> R.string.compose_chart_point_triangle
    GlucosePointShape.Square -> R.string.compose_chart_point_square
    GlucosePointShape.Diamond -> R.string.compose_chart_point_diamond
    GlucosePointShape.Cross -> R.string.compose_chart_point_cross
}
private fun GlucosePointFill.labelRes(): Int = when (this) {
    GlucosePointFill.Filled -> R.string.compose_chart_point_filled
    GlucosePointFill.Hollow -> R.string.compose_chart_point_hollow
}

private const val HOUR_MILLIS = 3_600_000L
private const val MAX_CONNECTED_GAP_MILLIS = 15 * 60_000L
private const val Y_TICK_INTERVAL = 3.0
private val TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault())
private val CROSSHAIR_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
private val RECORD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
private val CHART_COLORS = listOf(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00), Color(0xFFD32F2F), Color(0xFF7B1FA2))

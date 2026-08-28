package com.woshiwangnima.healthdietpro.ui.record

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.rememberAttentionShakeOffset
import com.woshiwangnima.healthdietpro.common.range.Range
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartIndex
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartSlice
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartWindow
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartStylePrefs
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseSeriesStylePrefs
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseBarStylePrefs
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseDiabetesType
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTimingAnchor
import com.woshiwangnima.healthdietpro.model.bloodglucose.GlucoseTimeRangeBand
import com.woshiwangnima.healthdietpro.model.bloodglucose.GlucoseTimeRangeDistribution
import com.woshiwangnima.healthdietpro.model.bloodglucose.calculateGlucoseTimeRangeDistribution
import com.woshiwangnima.healthdietpro.model.bloodglucose.classifyBloodGlucoseValue
import com.woshiwangnima.healthdietpro.model.bloodglucose.glucoseTimeReferenceRanges
import com.woshiwangnima.healthdietpro.model.bloodglucose.scopedSlice
import com.woshiwangnima.healthdietpro.model.diet.DietRepository
import com.woshiwangnima.healthdietpro.model.diet.DietPrefs
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod
import com.woshiwangnima.healthdietpro.model.diet.loadDietPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.sleep.SleepKind
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.SleepRepository
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.disease.DiseaseReference
import com.woshiwangnima.healthdietpro.model.disease.curatedId
import com.woshiwangnima.healthdietpro.model.disease.hasCurrentUserDiabetesRisk
import com.woshiwangnima.healthdietpro.model.disease.diabetesReferenceIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    chartStyle: BloodGlucoseChartStylePrefs,
    onChartStyleChanged: (BloodGlucoseChartStylePrefs) -> Unit,
    modifier: Modifier = Modifier,
    sessionWindowEnd: Long? = null,
    onSessionWindowEndChanged: (Long?) -> Unit = {},
) {
    val index = remember(records) { BloodGlucoseChartIndex(records) }
    val currentScopeEnd = remember(scopeStart, scopeEnd) {
        minOf(scopeEnd, System.currentTimeMillis()).coerceAtLeast(scopeStart)
    }
    val initialEnd = remember(scopeStart, currentScopeEnd) { currentScopeEnd }
    var ownedWindowEnd by remember(scopeStart, currentScopeEnd, window) { mutableLongStateOf(initialEnd) }
    val windowEnd = sessionWindowEnd ?: ownedWindowEnd
    val primaryStyle = chartStyle.primary.toUiStyle(defaultPrimaryStyle())
    val delayedStyle = chartStyle.delayed.toUiStyle(defaultDelayedStyle())
    var selectedSeries by remember { mutableStateOf(SeriesKind.Primary) }
    var selectedBar by remember { mutableStateOf<BarKind?>(BarKind.Diet) }
    var styleDialogSeries by remember { mutableStateOf<SeriesKind?>(null) }
    var barStyleDialog by remember { mutableStateOf<BarKind?>(null) }
    val barStyles = BarKind.entries.associateWith { kind ->
        chartStyle.bars[kind.name].toUiStyle(defaultBarStyles().getValue(kind))
    }
    var eventBars by remember { mutableStateOf(emptyList<BarSample>()) }
    var nightSleepRecords by remember { mutableStateOf(emptyList<SleepRecord>()) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dietPrefs = remember(context) { loadDietPrefs(context) }
    var fullscreen by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTargetRateHelp by remember { mutableStateOf(false) }
    LaunchedEffect(initialEnd, scopeStart, currentScopeEnd, window, records) {
        if (sessionWindowEnd == null) ownedWindowEnd = initialEnd
    }
    LaunchedEffect(lifecycleOwner, scopeStart, currentScopeEnd, window, records) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val loaded = withContext(Dispatchers.IO) {
                loadEventBars(context, scopeStart, currentScopeEnd) to SleepRepository.fromContext(context).load().records
                    .filter { it.kind == SleepKind.NIGHT_SLEEP && it.wakeUpAt != null }
            }
            eventBars = loaded.first
            nightSleepRecords = loaded.second
        }
    }
    val slice = remember(index, scopeStart, currentScopeEnd, windowEnd, window) {
        index.scopedSlice(scopeStart, currentScopeEnd, windowEnd, window)
    }
    val earliest = scopeStart
    val latest = currentScopeEnd
    val setWindowEnd: (Long) -> Unit = { timestamp ->
        val clamped = timestamp.coerceIn(earliest, latest)
        ownedWindowEnd = clamped
        onSessionWindowEndChanged(clamped)
    }

    GlucoseChartSurface(
        slice = slice,
        allRecords = records,
        dietPrefs = dietPrefs,
        nightSleepRecords = nightSleepRecords,
        window = window,
        diabetesType = diabetesType,
        primaryStyle = primaryStyle,
        delayedStyle = delayedStyle,
        barStyles = barStyles,
        eventBars = eventBars,
        panEarliest = earliest,
        panLatest = latest,
        selectedSeries = selectedSeries,
        selectedBar = selectedBar,
        onSelectedSeries = { series ->
            if (series == selectedSeries) styleDialogSeries = series else selectedSeries = series
        },
        onBarSelected = { kind ->
            if (kind == selectedBar) barStyleDialog = kind else selectedBar = kind
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
            onStyleChanged = { updated ->
                onChartStyleChanged(
                    if (series == SeriesKind.Primary) chartStyle.copy(primary = updated.toPrefs())
                    else chartStyle.copy(delayed = updated.toPrefs())
                )
            },
            onDismiss = { styleDialogSeries = null },
        )
    }
    barStyleDialog?.let { kind ->
        BarStyleDialog(
            kind,
            barStyles.getValue(kind),
            { updated -> onChartStyleChanged(chartStyle.copy(bars = chartStyle.bars + (kind.name to updated.toPrefs()))) },
            { barStyleDialog = null },
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
            datesWithData = remember(records) {
                records.map { Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
            },
            allowNoDataSelection = true,
        )
    }
    if (fullscreen) {
        Dialog(onDismissRequest = { fullscreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                FixedLandscapeBox {
                    GlucoseChartSurface(
                        slice = slice,
                        allRecords = records,
                        dietPrefs = dietPrefs,
                        nightSleepRecords = nightSleepRecords,
                        window = window,
                        diabetesType = diabetesType,
                        primaryStyle = primaryStyle,
                        delayedStyle = delayedStyle,
                        barStyles = barStyles,
                        eventBars = eventBars,
                        panEarliest = earliest,
                        panLatest = latest,
                        selectedSeries = selectedSeries,
                        selectedBar = selectedBar,
                        onSelectedSeries = { series ->
                            if (series == selectedSeries) styleDialogSeries = series else selectedSeries = series
                        },
                        onBarSelected = { kind ->
                            if (kind == selectedBar) barStyleDialog = kind else selectedBar = kind
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
            text = { TargetTimeRangeHelp(glucoseRangeText(diabetesType)) },
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
    allRecords: List<BloodGlucoseRecord>,
    dietPrefs: DietPrefs,
    nightSleepRecords: List<SleepRecord>,
    window: BloodGlucoseChartWindow,
    diabetesType: BloodGlucoseDiabetesType,
    primaryStyle: SeriesStyle,
    delayedStyle: SeriesStyle,
    barStyles: Map<BarKind, BarStyle>,
    eventBars: List<BarSample>,
    panEarliest: Long,
    panLatest: Long,
    selectedSeries: SeriesKind,
    selectedBar: BarKind?,
    onSelectedSeries: (SeriesKind) -> Unit,
    onBarSelected: (BarKind) -> Unit,
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
    val bars = remember(eventBars, slice.windowStart, slice.windowEnd) {
        eventBars.filter { it.endTimestamp > slice.windowStart && it.startTimestamp < slice.windowEnd }
    }
    val earliest = panEarliest
    val latest = panLatest
    val palette = ChartPalette(
        primary = primaryStyle.color.copy(alpha = primaryStyle.alpha),
        delayed = delayedStyle.color.copy(alpha = delayedStyle.alpha),
        axis = MaterialTheme.colorScheme.onSurfaceVariant,
        grid = MaterialTheme.colorScheme.outlineVariant,
        target = MaterialTheme.colorScheme.secondaryContainer,
        surface = MaterialTheme.colorScheme.surface,
    )
    var crosshair by remember { mutableStateOf<GlucoseCrosshair?>(null) }
    var barCrosshair by remember { mutableStateOf<BarCrosshair?>(null) }
    LaunchedEffect(selectedBar) { barCrosshair = null }
    val currentSlice by rememberUpdatedState(slice)
    val currentPrimary by rememberUpdatedState(primary)
    val currentDelayed by rememberUpdatedState(delayed)
    val currentSelectedSeries by rememberUpdatedState(selectedSeries)
    val currentPrimaryStyle by rememberUpdatedState(primaryStyle)
    val currentDelayedStyle by rememberUpdatedState(delayedStyle)
    val currentSelectedBar by rememberUpdatedState(selectedBar)
    val currentBars by rememberUpdatedState(bars)
    val delayedLabel = stringResource(R.string.blood_glucose_delayed_series, window.durationMillis / HOUR_MILLIS)
    val xAxisUnit = stringResource(R.string.blood_glucose_chart_x_axis_unit)
    val yAxisUnit = stringResource(R.string.blood_glucose_chart_y_axis_unit)

    Column(if (fullscreen) modifier else modifier.verticalScroll(rememberScrollState())) {
        if (slice.scoped.isEmpty()) {
            Text(stringResource(R.string.blood_glucose_chart_no_data_in_scope), modifier = Modifier.align(Alignment.CenterHorizontally), color = palette.axis)
            if (!fullscreen) {
                ChartLegend(primaryStyle, stringResource(R.string.blood_glucose_chart_primary_series), delayedStyle, delayedLabel, selectedSeries, onSelectedSeries)
                BarLegend(barStyles, selectedBar, onBarSelected)
                EightPointGlucoseCard(
                    records = allRecords,
                    selectedDate = Instant.ofEpochMilli(slice.windowStart).atZone(ZoneId.systemDefault()).toLocalDate(),
                    targetRange = diabetesType.glucoseReferenceRangeMmolPerL,
                    dietPrefs = dietPrefs,
                    nightSleepRecords = nightSleepRecords,
                )
            }
            return@Column
        }
        val drawingModifier = if (fullscreen) {
            Modifier.fillMaxWidth().weight(1f)
        } else {
            Modifier.fillMaxWidth().height(210.dp)
        }
        BoxWithConstraints(drawingModifier) {
            var chartSize by remember { mutableStateOf(IntSize.Zero) }
            var lineInfoOffset by remember { mutableStateOf(IntOffset.Zero) }
            var barInfoOffset by remember { mutableStateOf(IntOffset.Zero) }
            val currentLineInfoOffset by rememberUpdatedState(lineInfoOffset)
            val currentBarInfoOffset by rememberUpdatedState(barInfoOffset)
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
            fun handleChartTap(offset: Offset) {
                val timestamp = chartTimestampAt(offset, chartSize.width, chartSize.height, currentSlice)
                if (timestamp == null) {
                    crosshair = null
                    barCrosshair = null
                } else {
                    crosshair = crosshairAt(timestamp, currentSlice, if (currentSelectedSeries == SeriesKind.Primary) currentPrimary else currentDelayed, if (currentSelectedSeries == SeriesKind.Primary) currentPrimaryStyle else currentDelayedStyle)
                    barCrosshair = currentSelectedBar?.let { kind -> barCrosshairAt(timestamp, currentSlice, currentBars, kind) }
                }
            }
            Canvas(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { chartSize = it }
                    .pointerInput("glucose-crosshair") {
                        detectTapGestures { offset ->
                            handleChartTap(offset)
                        }
                    },
            ) {
                drawGlucoseChart(slice, window, diabetesType, primary, delayed, bars, barStyles, palette, primaryStyle, delayedStyle, crosshair, barCrosshair, xAxisUnit, yAxisUnit)
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
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).graphicsLayer(scaleX = 0.82f, scaleY = 0.82f),
                )
            }
            OutlinedButton(
                onClick = onDateClick,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp + dateMarkerOffset, bottom = 1.dp).width(104.dp).graphicsLayer(scaleX = 0.82f, scaleY = 0.82f),
            ) {
                Icon(painterResource(R.drawable.ic_birthday), null, modifier = Modifier.size(16.dp))
                TextOverflowText(
                    text = markerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 3.dp),
                )
            }
            crosshair?.let { value ->
                CrosshairInfo(value, palette, modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp)
                    .onGloballyPositioned { lineInfoOffset = it.positionInParent().let { p -> IntOffset(p.x.toInt(), p.y.toInt()) } }
                    .pointerInput("line-info-crosshair") { detectTapGestures { handleChartTap(Offset(currentLineInfoOffset.x.toFloat() + it.x, currentLineInfoOffset.y.toFloat() + it.y)) } })
            }
            barCrosshair?.let { value ->
                BarCrosshairInfo(value, modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 82.dp)
                    .onGloballyPositioned { barInfoOffset = it.positionInParent().let { p -> IntOffset(p.x.toInt(), p.y.toInt()) } }
                    .pointerInput("bar-info-crosshair") { detectTapGestures { handleChartTap(Offset(currentBarInfoOffset.x.toFloat() + it.x, currentBarInfoOffset.y.toFloat() + it.y)) } })
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
        if (!fullscreen) {
            ChartLegend(primaryStyle, stringResource(R.string.blood_glucose_chart_primary_series), delayedStyle, delayedLabel, selectedSeries, onSelectedSeries)
            BarLegend(barStyles, selectedBar, onBarSelected)
        }
        if (!fullscreen) {
            GlucoseStatisticsCard(primary, diabetesType, window, onTargetRateHelp)
            EightPointGlucoseCard(
                records = allRecords,
                selectedDate = Instant.ofEpochMilli(slice.windowStart).atZone(ZoneId.systemDefault()).toLocalDate(),
                targetRange = diabetesType.glucoseReferenceRangeMmolPerL,
                dietPrefs = dietPrefs,
                nightSleepRecords = nightSleepRecords,
            )
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
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun BarCrosshairInfo(value: BarCrosshair, modifier: Modifier) {
    val start = Instant.ofEpochMilli(value.startTimestamp).atZone(ZoneId.systemDefault()).format(CROSSHAIR_TIME_FORMATTER)
    val end = Instant.ofEpochMilli(value.endTimestamp).atZone(ZoneId.systemDefault()).format(CROSSHAIR_TIME_FORMATTER)
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = "${stringResource(value.kind.labelRes)}\n$start - $end",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
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
    val timeDistribution = remember(records, target) {
        calculateGlucoseTimeRangeDistribution(records, target)
    }
    val highest = records.maxByOrNull(BloodGlucoseRecord::valueMmolPerL)
    val lowest = records.minByOrNull(BloodGlucoseRecord::valueMmolPerL)
    val average = records.takeIf { it.isNotEmpty() }?.map(BloodGlucoseRecord::valueMmolPerL)?.average()
    val fluctuation = if (highest != null && lowest != null) highest.valueMmolPerL - lowest.valueMmolPerL else null
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TargetTimeRangeCard(
                label = stringResource(R.string.blood_glucose_chart_target_rate, window.durationMillis / HOUR_MILLIS),
                distribution = timeDistribution,
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
                MetricCard(
                    label = stringResource(R.string.blood_glucose_chart_average),
                    value = average?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "?",
                    unit = "mmol/L",
                    timestamp = null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun EightPointGlucoseCard(
    records: List<BloodGlucoseRecord>,
    selectedDate: java.time.LocalDate,
    targetRange: Range<Float>,
    dietPrefs: DietPrefs,
    nightSleepRecords: List<SleepRecord>,
) {
    val zone = remember { ZoneId.systemDefault() }
    val dates = remember(selectedDate) { (6 downTo 0).map { selectedDate.minusDays(it.toLong()) } }
    val mealWindows = remember(dietPrefs) { EightPointMealWindows.from(dietPrefs) }
    var sourceDefinition by remember { mutableStateOf<EightPointSlotDefinition?>(null) }
    val valuesByDate = remember(records, dates, zone, mealWindows, nightSleepRecords) {
        dates.associateWith { date ->
            buildMap<EightPointSlot, EightPointResolvedValue> {
                records.asSequence()
                    .filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == date }
                    .sortedBy(BloodGlucoseRecord::timestamp)
                    .forEach { record ->
                        val resolved = record.eightPointResolution(zone, mealWindows, nightSleepRecords)
                        val existing = get(resolved.slot)
                        if (existing == null || (existing.isFallback && !resolved.isFallback) || existing.isFallback == resolved.isFallback) {
                            put(resolved.slot, resolved)
                        }
                    }
            }
        }
    }
    val unknownColor = MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(top = 6.dp).fillMaxWidth().height(382.dp),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.blood_glucose_eight_point_chart_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EightPointLegendItem(glucoseTimeBandColor(GlucoseTimeRangeBand.HIGH), stringResource(R.string.blood_glucose_chart_target_time_high))
                    EightPointLegendItem(glucoseTimeBandColor(GlucoseTimeRangeBand.IN_RANGE), stringResource(R.string.blood_glucose_chart_target_time_in_range))
                    EightPointLegendItem(glucoseTimeBandColor(GlucoseTimeRangeBand.LOW), stringResource(R.string.blood_glucose_chart_target_time_low))
                    EightPointLegendItem(unknownColor, stringResource(R.string.blood_glucose_eight_point_unknown))
                }
                Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EightPointNumberSample(
                        label = stringResource(R.string.blood_glucose_eight_point_determinate_value),
                        isFallback = false,
                    )
                    EightPointNumberSample(
                        label = stringResource(R.string.blood_glucose_eight_point_fallback_value),
                        isFallback = true,
                    )
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f)) {
                Row(Modifier.fillMaxWidth().height(44.dp)) {
                    EightPointGridHeader(text = "", modifier = Modifier.weight(1f))
                    EIGHT_POINT_SLOT_DEFINITIONS.forEach { definition ->
                        EightPointGridHeader(
                            text = stringResource(definition.labelRes),
                            modifier = Modifier.weight(1f),
                            onClick = { sourceDefinition = definition },
                        )
                    }
                }
                dates.forEach { date ->
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        EightPointGridHeader(date.format(EIGHT_POINT_DATE_FORMATTER), Modifier.weight(1f))
                        EIGHT_POINT_SLOT_DEFINITIONS.forEach { definition ->
                            EightPointGlucoseValue(
                                value = valuesByDate.getValue(date)[definition.slot],
                                targetRange = targetRange,
                                unknownColor = unknownColor,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
    sourceDefinition?.let { definition ->
        AlertDialog(
            onDismissRequest = { sourceDefinition = null },
            title = { Text(stringResource(definition.labelRes)) },
            text = { Text(stringResource(definition.sourceDescriptionRes)) },
            confirmButton = {
                TextButton(onClick = { sourceDefinition = null }) {
                    Text(stringResource(R.string.compose_confirm_dialog_ok))
                }
            },
        )
    }
}

@Composable
private fun EightPointLegendItem(color: Color, label: String) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(9.dp)) { drawCircle(color) }
        Text(
            label,
            modifier = Modifier.padding(start = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EightPointNumberSample(label: String, isFallback: Boolean) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "6.20",
            style = if (isFallback) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleSmall,
            fontWeight = if (isFallback) FontWeight.Normal else FontWeight.Bold,
            color = if (isFallback) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EightPointGridHeader(text: String, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Surface(
        color = Color.Transparent,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxHeight().padding(1.dp).then(
            if (onClick == null) Modifier else Modifier.clickable(onClick = onClick),
        ),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 1.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EightPointGlucoseValue(
    value: EightPointResolvedValue?,
    targetRange: Range<Float>,
    unknownColor: Color,
    modifier: Modifier,
) {
    val record = value?.record
    val isFallback = value?.isFallback ?: true
    val band = record?.let { classifyBloodGlucoseValue(it.valueMmolPerL, targetRange) }
    val color = band?.let(::glucoseTimeBandColor) ?: unknownColor
    Surface(
        color = color.copy(alpha = 0.26f),
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxHeight().padding(1.dp),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 1.dp), contentAlignment = Alignment.Center) {
            Text(
                text = record?.let { String.format(Locale.getDefault(), "%.2f", it.valueMmolPerL) } ?: "-",
                style = if (isFallback) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFallback) FontWeight.Normal else FontWeight.Bold,
                color = if (isFallback) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private enum class EightPointSlot {
    Dawn,
    Breakfast,
    BreakfastAfterTwoHours,
    Lunch,
    LunchAfterTwoHours,
    Dinner,
    DinnerBeforeTwoHours,
    Bedtime,
}

private enum class EightPointSlotSource {
    NightSleepInterval,
    MealTimeRange,
    MealEndPlusTwoHours,
    MealStartMinusTwoHours,
    NightSleepStart,
}

private data class EightPointSlotDefinition(
    val slot: EightPointSlot,
    val labelRes: Int,
    val sourceDescriptionRes: Int,
    val source: EightPointSlotSource,
    val resolutionPriority: Int,
    val mealPeriod: MealPeriod? = null,
)

private val EIGHT_POINT_SLOT_DEFINITIONS = listOf(
    EightPointSlotDefinition(EightPointSlot.Dawn, R.string.blood_glucose_eight_point_dawn, R.string.blood_glucose_eight_point_dawn_source, EightPointSlotSource.NightSleepInterval, 8),
    EightPointSlotDefinition(EightPointSlot.Breakfast, R.string.blood_glucose_eight_point_breakfast, R.string.blood_glucose_eight_point_breakfast_source, EightPointSlotSource.MealTimeRange, 0, MealPeriod.BREAKFAST),
    EightPointSlotDefinition(EightPointSlot.BreakfastAfterTwoHours, R.string.blood_glucose_eight_point_breakfast_after_two_hours, R.string.blood_glucose_eight_point_breakfast_after_two_hours_source, EightPointSlotSource.MealEndPlusTwoHours, 1, MealPeriod.BREAKFAST),
    EightPointSlotDefinition(EightPointSlot.Lunch, R.string.blood_glucose_eight_point_lunch, R.string.blood_glucose_eight_point_lunch_source, EightPointSlotSource.MealTimeRange, 2, MealPeriod.LUNCH),
    EightPointSlotDefinition(EightPointSlot.LunchAfterTwoHours, R.string.blood_glucose_eight_point_lunch_after_two_hours, R.string.blood_glucose_eight_point_lunch_after_two_hours_source, EightPointSlotSource.MealEndPlusTwoHours, 3, MealPeriod.LUNCH),
    EightPointSlotDefinition(EightPointSlot.Dinner, R.string.blood_glucose_eight_point_dinner, R.string.blood_glucose_eight_point_dinner_source, EightPointSlotSource.MealTimeRange, 4, MealPeriod.DINNER),
    EightPointSlotDefinition(EightPointSlot.DinnerBeforeTwoHours, R.string.blood_glucose_eight_point_dinner_before_two_hours, R.string.blood_glucose_eight_point_dinner_before_two_hours_source, EightPointSlotSource.MealStartMinusTwoHours, 5, MealPeriod.DINNER),
    EightPointSlotDefinition(EightPointSlot.Bedtime, R.string.blood_glucose_eight_point_bedtime, R.string.blood_glucose_eight_point_bedtime_source, EightPointSlotSource.NightSleepStart, 7),
)

private data class EightPointResolvedValue(
    val slot: EightPointSlot,
    val record: BloodGlucoseRecord,
    val isFallback: Boolean,
)

private data class EightPointMealWindow(val startMinute: Int, val endMinute: Int) {
    fun contains(minuteOfDay: Int): Boolean =
        if (startMinute <= endMinute) minuteOfDay in startMinute..endMinute
        else minuteOfDay >= startMinute || minuteOfDay <= endMinute

    fun matchesOffset(minuteOfDay: Int, offsetMinutes: Int): Boolean {
        val baseMinute = if (offsetMinutes < 0) startMinute else endMinute
        val target = Math.floorMod(baseMinute + offsetMinutes, MINUTES_PER_DAY)
        val distance = Math.floorMod(minuteOfDay - target, MINUTES_PER_DAY)
        return distance <= 30 || distance >= MINUTES_PER_DAY - 30
    }
}

private data class EightPointMealWindows(
    val breakfast: EightPointMealWindow?,
    val lunch: EightPointMealWindow?,
    val dinner: EightPointMealWindow?,
) {
    companion object {
        fun from(prefs: DietPrefs): EightPointMealWindows = EightPointMealWindows(
            breakfast = prefs.windowFor(MealPeriod.BREAKFAST),
            lunch = prefs.windowFor(MealPeriod.LUNCH),
            dinner = prefs.windowFor(MealPeriod.DINNER),
        )
    }

    fun forPeriod(period: MealPeriod): EightPointMealWindow? = when (period) {
        MealPeriod.BREAKFAST -> breakfast
        MealPeriod.LUNCH -> lunch
        MealPeriod.DINNER -> dinner
        else -> null
    }
}

private fun DietPrefs.windowFor(period: MealPeriod): EightPointMealWindow? {
    val prefs = forPeriod(period)
    return prefs.rangeStartMinute?.let { start -> prefs.rangeEndMinute?.let { end -> EightPointMealWindow(start, end) } }
}

private fun BloodGlucoseRecord.eightPointResolution(
    zone: ZoneId,
    mealWindows: EightPointMealWindows,
    nightSleepRecords: List<SleepRecord>,
): EightPointResolvedValue {
    val resolvedSlot = EIGHT_POINT_SLOT_DEFINITIONS
        .sortedBy(EightPointSlotDefinition::resolutionPriority)
        .firstOrNull { it.matches(this, zone, mealWindows, nightSleepRecords) }
        ?.slot
    return EightPointResolvedValue(
        slot = resolvedSlot ?: fallbackEightPointSlot(zone),
        record = this,
        isFallback = resolvedSlot == null,
    )
}

private fun EightPointSlotDefinition.matches(
    record: BloodGlucoseRecord,
    zone: ZoneId,
    mealWindows: EightPointMealWindows,
    nightSleepRecords: List<SleepRecord>,
): Boolean {
    val minuteOfDay = Instant.ofEpochMilli(record.timestamp).atZone(zone).let { it.hour * 60 + it.minute }
    return when (source) {
        EightPointSlotSource.NightSleepInterval -> nightSleepRecords.any { it.contains(record.timestamp) }
        EightPointSlotSource.NightSleepStart -> nightSleepRecords.any { it.matchesSleepStart(record.timestamp) }
        EightPointSlotSource.MealTimeRange -> mealPeriod?.let(mealWindows::forPeriod)?.contains(minuteOfDay) == true
        EightPointSlotSource.MealEndPlusTwoHours -> mealPeriod?.let(mealWindows::forPeriod)?.matchesOffset(minuteOfDay, 120) == true
        EightPointSlotSource.MealStartMinusTwoHours -> mealPeriod?.let(mealWindows::forPeriod)?.matchesOffset(minuteOfDay, -120) == true
    }
}

private fun SleepRecord.contains(timestamp: Long): Boolean =
    timestamp in sleepStartAt..(wakeUpAt ?: sleepStartAt)

private fun SleepRecord.matchesSleepStart(timestamp: Long): Boolean =
    abs(timestamp - sleepStartAt) <= 30 * MINUTE_MILLIS

private fun BloodGlucoseRecord.fallbackEightPointSlot(zone: ZoneId): EightPointSlot = when (timingAnchor) {
    BloodGlucoseTimingAnchor.WAKE_UP -> EightPointSlot.Dawn
    BloodGlucoseTimingAnchor.BREAKFAST -> if ((relativeMinutes ?: 0) >= 90) EightPointSlot.BreakfastAfterTwoHours else EightPointSlot.Breakfast
    BloodGlucoseTimingAnchor.LUNCH -> if ((relativeMinutes ?: 0) >= 90) EightPointSlot.LunchAfterTwoHours else EightPointSlot.Lunch
    BloodGlucoseTimingAnchor.DINNER -> if ((relativeMinutes ?: 0) <= -90) EightPointSlot.DinnerBeforeTwoHours else EightPointSlot.Dinner
    BloodGlucoseTimingAnchor.BEDTIME -> EightPointSlot.Bedtime
    null -> when (Instant.ofEpochMilli(timestamp).atZone(zone).hour) {
        in 0..5 -> EightPointSlot.Dawn
        in 6..8 -> EightPointSlot.Breakfast
        in 9..10 -> EightPointSlot.BreakfastAfterTwoHours
        in 11..12 -> EightPointSlot.Lunch
        in 13..14 -> EightPointSlot.LunchAfterTwoHours
        in 15..16 -> EightPointSlot.DinnerBeforeTwoHours
        in 17..19 -> EightPointSlot.Dinner
        else -> EightPointSlot.Bedtime
    }
}

@Composable
private fun TargetTimeRangeCard(
    label: String,
    distribution: GlucoseTimeRangeDistribution,
    onHelp: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TooltipLabel(label, Modifier.weight(1f))
                IconButton(onClick = onHelp, modifier = Modifier.size(22.dp)) {
                    Icon(painterResource(R.drawable.ic_help), stringResource(R.string.blood_glucose_chart_target_rate_title), modifier = Modifier.size(16.dp))
                }
            }
            val totalMillis = distribution.coveredMillis
            val allBandsWithinReferences = GlucoseTimeRangeBand.entries.all { band ->
                val percent = if (totalMillis > 0L) {
                    distribution.millisFor(band).toFloat() / totalMillis * 100f
                } else {
                    0f
                }
                glucoseTimeReferenceRanges.first { it.value == band }.contains(percent)
            }
            val centerAttentionShake = rememberAttentionShakeOffset(
                active = totalMillis > 0L && !allBandsWithinReferences,
                label = "glucoseTargetTimeCenter",
            )
            AnimatedDonutChart(
                segments = GlucoseTimeRangeBand.entries.map { band ->
                    val durationMillis = distribution.millisFor(band)
                    val percent = if (totalMillis > 0L) durationMillis.toDouble() / totalMillis * 100.0 else 0.0
                    DonutChartSegment(
                        id = band.name,
                        label = stringResource(
                            R.string.blood_glucose_chart_target_time_segment,
                            stringResource(glucoseTimeBandLabelRes(band)),
                            String.format(Locale.getDefault(), "%.1f", percent),
                            formatTargetTime(durationMillis),
                        ),
                        value = durationMillis.toFloat(),
                        color = glucoseTimeBandColor(band),
                        needsAttention = !glucoseTimeReferenceRanges.first { it.value == band }.contains(percent.toFloat()),
                    )
                },
                centerValue = if (totalMillis > 0L) {
                    String.format(
                        Locale.getDefault(),
                        "%.1f%%",
                        distribution.inRangeMillis.toDouble() / totalMillis * 100.0,
                    )
                } else {
                    "?"
                },
                centerLabel = stringResource(R.string.blood_glucose_chart_target_rate_title),
                showLegend = false,
                labelMaxLines = 2,
                chartHeight = 173.dp,
                centerContentColor = if (totalMillis > 0L && !allBandsWithinReferences) MaterialTheme.colorScheme.tertiary else null,
                centerContentModifier = Modifier.offset { IntOffset(0, centerAttentionShake.roundToPx()) },
            )
        }
    }
}

@Composable
private fun TargetTimeRangeHelp(targetRangeText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.blood_glucose_chart_target_time_help_intro, targetRangeText),
            style = MaterialTheme.typography.bodyMedium,
        )
        GlucoseTimeRangeHelpRow(GlucoseTimeRangeBand.HIGH, R.string.blood_glucose_chart_target_time_high_help)
        GlucoseTimeRangeHelpRow(GlucoseTimeRangeBand.IN_RANGE, R.string.blood_glucose_chart_target_time_in_range_help)
        GlucoseTimeRangeHelpRow(GlucoseTimeRangeBand.LOW, R.string.blood_glucose_chart_target_time_low_help)
    }
}

@Composable
private fun GlucoseTimeRangeHelpRow(band: GlucoseTimeRangeBand, descriptionRes: Int) {
    val referenceRange = glucoseTimeReferenceRanges.first { it.value == band }
    Row(verticalAlignment = Alignment.Top) {
        Canvas(Modifier.size(10.dp).padding(top = 3.dp)) { drawCircle(glucoseTimeBandColor(band)) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(glucoseTimeBandLabelRes(band)), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.blood_glucose_chart_target_time_reference, formatPercentRange(referenceRange)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Composable
private fun formatTargetTime(millis: Long): String {
    val totalMinutes = (millis / 60_000.0).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.blood_glucose_chart_target_time_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.blood_glucose_chart_target_time_minutes, minutes)
    }
}

private fun glucoseTimeBandLabelRes(band: GlucoseTimeRangeBand): Int = when (band) {
    GlucoseTimeRangeBand.HIGH -> R.string.blood_glucose_chart_target_time_high
    GlucoseTimeRangeBand.IN_RANGE -> R.string.blood_glucose_chart_target_time_in_range
    GlucoseTimeRangeBand.LOW -> R.string.blood_glucose_chart_target_time_low
}

private fun glucoseTimeBandColor(band: GlucoseTimeRangeBand): Color = when (band) {
    GlucoseTimeRangeBand.HIGH -> Color(0xFFF57C00)
    GlucoseTimeRangeBand.IN_RANGE -> Color(0xFF43A047)
    GlucoseTimeRangeBand.LOW -> Color(0xFFE53935)
}

private fun formatPercentRange(range: Range<Float>): String {
    val lowerBracket = if (range.minInclusive) "[" else "("
    val upperBracket = if (range.maxInclusive) "]" else ")"
    val lower = range.min?.let { String.format(Locale.getDefault(), "%.0f%%", it) } ?: "-∞"
    val upper = range.max?.let { String.format(Locale.getDefault(), "%.0f%%", it) } ?: "+∞"
    return "$lowerBracket$lower, $upper$upperBracket"
}

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
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 1.dp), horizontalArrangement = Arrangement.Center) {
            LegendItem(SeriesKind.Primary, primaryStyle, primaryLabel, selected == SeriesKind.Primary, onSelected)
            LegendItem(SeriesKind.Delayed, delayedStyle, delayedLabel, selected == SeriesKind.Delayed, onSelected)
        }
    }
}

@Composable
private fun BarLegend(styles: Map<BarKind, BarStyle>, selected: BarKind?, onSelected: (BarKind) -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(Modifier.fillMaxWidth().padding(bottom = 1.dp), horizontalArrangement = Arrangement.Center) {
            BarKind.entries.forEach { kind ->
                val style = styles.getValue(kind)
                val isSelected = kind == selected
                Surface(onClick = { onSelected(kind) }, shape = MaterialTheme.shapes.extraSmall, color = if (isSelected) style.color.copy(alpha = 0.12f) else Color.Transparent, border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, style.color.copy(alpha = 0.42f)) else null) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(14.dp, 12.dp)) { drawRect(style.color.copy(alpha = style.mainAlpha), size = androidx.compose.ui.geometry.Size(size.width, size.height)) }
                        Text(stringResource(kind.labelRes), style = MaterialTheme.typography.labelSmall, textDecoration = if (style.visible) null else TextDecoration.LineThrough, modifier = Modifier.padding(start = 3.dp))
                    }
                }
            }
        }
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
        Row(Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(28.dp, 12.dp)) {
                drawLine(style.color.copy(alpha = style.alpha), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3f, StrokeCap.Round, style.linePattern.effect())
                drawLegendPoint(style, Offset(size.width / 2, size.height / 2))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = if (style.visible) null else TextDecoration.LineThrough, modifier = Modifier.padding(start = 4.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.blood_glucose_chart_show))
                    Checkbox(checked = style.visible, onCheckedChange = { onStyleChanged(style.copy(visible = it)) })
                }
                Text(stringResource(R.string.blood_glucose_chart_color))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CHART_COLORS.forEach { color ->
                        Surface(onClick = { onStyleChanged(style.copy(color = color)) }, color = color, shape = MaterialTheme.shapes.small, modifier = Modifier.size(28.dp), border = if (color == style.color) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null) {}
                    }
                }
                Text(stringResource(R.string.blood_glucose_chart_opacity, (style.alpha * 100).toInt()))
                Slider(style.alpha, { onStyleChanged(style.copy(alpha = it)) }, valueRange = 0f..1f)
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

@Composable
private fun BarStyleDialog(kind: BarKind, style: BarStyle, onStyleChanged: (BarStyle) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.blood_glucose_chart_bar_style, stringResource(kind.labelRes))) }, text = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.blood_glucose_chart_show))
                Checkbox(checked = style.visible, onCheckedChange = { onStyleChanged(style.copy(visible = it)) })
            }
            Text(stringResource(R.string.blood_glucose_chart_color))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { CHART_COLORS.forEach { color -> Surface(onClick = { onStyleChanged(style.copy(color = color)) }, color = color, modifier = Modifier.size(28.dp)) {} } }
            Text(stringResource(R.string.blood_glucose_chart_main_opacity, (style.mainAlpha * 100).toInt()))
            Slider(style.mainAlpha, { onStyleChanged(style.copy(mainAlpha = it)) }, valueRange = 0f..1f)
            Text(stringResource(R.string.blood_glucose_chart_impact_opacity, (style.impactAlpha * 100).toInt()))
            Slider(style.impactAlpha, { onStyleChanged(style.copy(impactAlpha = it)) }, valueRange = 0f..1f)
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_ok)) } })
}

private fun chartTimestampAt(touch: Offset, width: Int, height: Int, slice: BloodGlucoseChartSlice): Long? {
    val bounds = ChartBounds(width.toFloat(), height.toFloat())
    if (bounds.width <= 0f || slice.windowEnd <= slice.windowStart || touch.x !in bounds.left..bounds.right || touch.y !in bounds.top..bounds.bottom) return null
    return slice.windowStart + ((touch.x - bounds.left) / bounds.width * (slice.windowEnd - slice.windowStart)).toLong()
}

private fun crosshairAt(timestamp: Long, slice: BloodGlucoseChartSlice, points: List<RenderedPoint>, style: SeriesStyle): GlucoseCrosshair? {
    val rawX = timestamp
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

private fun barCrosshairAt(timestamp: Long, slice: BloodGlucoseChartSlice, bars: List<BarSample>, selectedKind: BarKind): BarCrosshair? {
    return bars.firstOrNull { it.kind == selectedKind && timestamp in it.impactStartTimestamp()..it.impactEndTimestamp() }
        ?.let { BarCrosshair(timestamp, it.startTimestamp, it.endTimestamp, it.kind) }
}

private fun DrawScope.drawGlucoseChart(
    slice: BloodGlucoseChartSlice,
    window: BloodGlucoseChartWindow,
    diabetesType: BloodGlucoseDiabetesType,
    primary: List<RenderedPoint>,
    delayed: List<RenderedPoint>,
    bars: List<BarSample>,
    barStyles: Map<BarKind, BarStyle>,
    palette: ChartPalette,
    primaryStyle: SeriesStyle,
    delayedStyle: SeriesStyle,
    crosshair: GlucoseCrosshair?,
    barCrosshair: BarCrosshair?,
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
    if (primaryStyle.visible) drawSeries(primary, ::x, ::y, palette.primary, primaryStyle)
    if (delayedStyle.visible) drawSeries(delayed, ::x, ::y, palette.delayed, delayedStyle)
    bars.forEach { bar ->
        val style = barStyles.getValue(bar.kind)
        if (style.visible) {
            val left = x(bar.startTimestamp)
            val right = x(bar.endTimestamp)
            val top = bounds.bottom - bounds.height * bar.height
            val impactLeft = x(bar.impactStartTimestamp()).coerceAtLeast(bounds.left)
            val impactRight = x(bar.impactEndTimestamp()).coerceAtMost(bounds.right)
            drawRect(style.color.copy(alpha = style.impactAlpha), Offset(impactLeft, top), androidx.compose.ui.geometry.Size((left - impactLeft).coerceAtLeast(0f), bounds.bottom - top))
            drawRect(style.color.copy(alpha = style.impactAlpha), Offset(right, top), androidx.compose.ui.geometry.Size((impactRight - right).coerceAtLeast(0f), bounds.bottom - top))
            drawRect(style.color.copy(alpha = style.mainAlpha), Offset(left, top), androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(2f), bounds.bottom - top))
        }
    }
    barCrosshair?.let { value ->
        if (value.timestamp in slice.windowStart..slice.windowEnd) {
            val pointX = x(value.timestamp)
            val bar = bars.firstOrNull { it.kind == value.kind && value.timestamp in it.impactStartTimestamp()..it.impactEndTimestamp() }
            val pointY = bar?.let { bounds.bottom - bounds.height * it.height } ?: bounds.bottom
            val cross = barStyles.getValue(value.kind).color.copy(alpha = 0.82f)
            drawLine(cross, Offset(pointX, bounds.top), Offset(pointX, bounds.bottom), 1.6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawLine(cross, Offset(bounds.left, pointY), Offset(bounds.right, pointY), 1.6f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            drawCircle(palette.surface, 6f, Offset(pointX, pointY))
            drawCircle(cross, 4f, Offset(pointX, pointY))
        }
    }
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
private data class BarCrosshair(val timestamp: Long, val startTimestamp: Long, val endTimestamp: Long, val kind: BarKind)
private data class ChartPalette(val primary: Color, val delayed: Color, val axis: Color, val grid: Color, val target: Color, val surface: Color)
internal data class SeriesStyle(
    val color: Color,
    val alpha: Float = 1f,
    val visible: Boolean = true,
    val lineStyle: GlucoseLineStyle = GlucoseLineStyle.Linear,
    val linePattern: GlucoseLinePattern = GlucoseLinePattern.Solid,
    val pointShape: GlucosePointShape = GlucosePointShape.Circle,
    val pointFill: GlucosePointFill = GlucosePointFill.Filled,
)
private enum class SeriesKind { Primary, Delayed }

private enum class BarKind(val labelRes: Int) { Medication(R.string.blood_glucose_chart_bar_medication), Diet(R.string.blood_glucose_chart_bar_diet), Exercise(R.string.blood_glucose_chart_bar_exercise), Sleep(R.string.blood_glucose_chart_bar_sleep) }
private data class BarStyle(val color: Color, val mainAlpha: Float = 0.5f, val impactAlpha: Float = 0.3f, val visible: Boolean = true)

private fun defaultPrimaryStyle() = SeriesStyle(Color(0xFF1976D2), lineStyle = GlucoseLineStyle.Spline)

private fun defaultDelayedStyle() = SeriesStyle(
    Color(0xFFF57C00),
    lineStyle = GlucoseLineStyle.Spline,
    linePattern = GlucoseLinePattern.Dotted,
    pointShape = GlucosePointShape.Cross,
)

private fun BloodGlucoseSeriesStylePrefs.toUiStyle(fallback: SeriesStyle): SeriesStyle = SeriesStyle(
    color = Color(colorArgb.toInt()),
    alpha = alpha.coerceIn(0f, 1f),
    visible = visible,
    lineStyle = runCatching { GlucoseLineStyle.valueOf(lineStyle) }.getOrDefault(fallback.lineStyle),
    linePattern = runCatching { GlucoseLinePattern.valueOf(linePattern) }.getOrDefault(fallback.linePattern),
    pointShape = runCatching { GlucosePointShape.valueOf(pointShape) }.getOrDefault(fallback.pointShape),
    pointFill = runCatching { GlucosePointFill.valueOf(pointFill) }.getOrDefault(fallback.pointFill),
)

private fun SeriesStyle.toPrefs() = BloodGlucoseSeriesStylePrefs(
    colorArgb = color.toArgb().toLong(),
    alpha = alpha,
    visible = visible,
    lineStyle = lineStyle.name,
    linePattern = linePattern.name,
    pointShape = pointShape.name,
    pointFill = pointFill.name,
)

private fun BloodGlucoseBarStylePrefs?.toUiStyle(fallback: BarStyle): BarStyle = BarStyle(
    color = this?.let { Color(colorArgb.toInt()) } ?: fallback.color,
    mainAlpha = this?.mainAlpha?.coerceIn(0f, 1f) ?: fallback.mainAlpha,
    impactAlpha = this?.impactAlpha?.coerceIn(0f, 1f) ?: fallback.impactAlpha,
    visible = this?.visible ?: fallback.visible,
)

private fun BarStyle.toPrefs() = BloodGlucoseBarStylePrefs(
    colorArgb = color.toArgb().toLong(),
    mainAlpha = mainAlpha,
    impactAlpha = impactAlpha,
    visible = visible,
)

private data class BarSample(val startTimestamp: Long, val endTimestamp: Long, val kind: BarKind, val height: Float)
private fun BarSample.impactStartTimestamp(): Long = when (kind) {
    BarKind.Medication, BarKind.Exercise, BarKind.Sleep -> startTimestamp - 30 * MINUTE_MILLIS
    BarKind.Diet -> startTimestamp - HOUR_MILLIS
}
private fun BarSample.impactEndTimestamp(): Long = when (kind) {
    BarKind.Medication -> startTimestamp + 12 * HOUR_MILLIS
    BarKind.Diet -> endTimestamp + 4 * HOUR_MILLIS
    BarKind.Exercise, BarKind.Sleep -> endTimestamp + 30 * MINUTE_MILLIS
}
private fun defaultBarStyles() = mapOf(BarKind.Medication to BarStyle(Color(0xFFE53935)), BarKind.Diet to BarStyle(Color(0xFFF57C00)), BarKind.Exercise to BarStyle(Color(0xFF43A047)), BarKind.Sleep to BarStyle(Color(0xFF7E57C2)))
private fun loadEventBars(context: android.content.Context, start: Long, end: Long): List<BarSample> {
    val hasDiabetesRisk = hasCurrentUserDiabetesRisk(context)
    val diseaseRepository = DiseaseRepository.fromContext(context)
    val diabetesIds = diseaseRepository.diabetesReferenceIds()
    val medicationBars = MedicationPrefs.getRecords(context).asSequence()
        .filter { medication ->
            hasDiabetesRisk && medication.timestamp in start..end && (
                medication.indicationReferences.any { it.curatedId() in diabetesIds }
                )
        }
        .map { BarSample(it.timestamp, it.timestamp + 10 * MINUTE_MILLIS, BarKind.Medication, 0.30f) }
    val dietBars = DietRepository.fromContext(context).load().records.asSequence()
        .filter { it.mealStartAt < end && it.mealEndAt > start }
        .map { BarSample(it.mealStartAt, it.mealEndAt.coerceAtLeast(it.mealStartAt + MINUTE_MILLIS), BarKind.Diet, 0.24f) }
    val sleepBars = SleepRepository.fromContext(context).load().records.asSequence()
        .filter { it.sleepStartAt < end && (it.wakeUpAt ?: end) > start }
        .map { BarSample(it.sleepStartAt, (it.wakeUpAt ?: end).coerceAtLeast(it.sleepStartAt + MINUTE_MILLIS), BarKind.Sleep, 0.12f) }
    return (medicationBars + dietBars + sleepBars).toList()
}
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
private const val MINUTE_MILLIS = 60_000L
private const val MINUTES_PER_DAY = 24 * 60
private const val MAX_CONNECTED_GAP_MILLIS = 15 * 60_000L
private const val Y_TICK_INTERVAL = 3.0
private val TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd", Locale.getDefault())
private val EIGHT_POINT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd", Locale.getDefault())
private val CROSSHAIR_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
private val RECORD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
private val CHART_COLORS = listOf(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00), Color(0xFFD32F2F), Color(0xFF7B1FA2))

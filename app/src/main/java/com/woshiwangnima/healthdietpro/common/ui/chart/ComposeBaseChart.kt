package com.woshiwangnima.healthdietpro.common.ui.chart

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppStepperDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppRepeatAdjustButton
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.adaptiveNavigationBarsWindowInsets
import com.woshiwangnima.healthdietpro.model.chart.ComposeChartSeriesState
import com.woshiwangnima.healthdietpro.model.chart.ComposeChartState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Immutable
internal data class ComposeChartPoint(
    val x: Double,
    val y: Double,
    val label: String = "",
)

@Immutable
internal data class ComposeChartSeries(
    val id: String,
    val label: String,
    val points: List<ComposeChartPoint>,
    val color: Color,
    val lineStyle: ComposeChartLineStyle = ComposeChartLineStyle.Linear,
    val linePattern: ComposeChartLinePattern = ComposeChartLinePattern.Solid,
    val pointShape: ComposeChartPointShape = ComposeChartPointShape.Circle,
    val pointFill: ComposeChartPointFill = ComposeChartPointFill.Filled,
)

internal enum class ComposeChartAxisSide {
    Start,
    End,
    Top,
    Bottom,
}

internal enum class ComposeChartLinePattern {
    Solid,
    Dashed,
    Dotted,
    DotDashed,
}

internal enum class ComposeChartLineStyle {
    Linear,
    Bezier,
    Spline,
    CatmullRom,
    Monotone,
    SteppedFront,
    SteppedBack,
}

internal enum class ComposeChartPointShape {
    Circle,
    Triangle,
    Square,
    Diamond,
    Cross,
}

internal enum class ComposeChartPointFill {
    Filled,
    Hollow,
}

internal enum class ComposeChartCrosshairBasis {
    PerpendicularToXAxis,
    PerpendicularToYAxis,
}

internal enum class ComposeChartFullscreenArea {
    ChartSettings,
    Title,
    ChartDrawing,
    DataGroupLegend,
}

internal enum class ComposeChartAxisWindowMode {
    PercentRange,
    PercentBounds,
}

@Immutable
internal data class ComposeChartSeriesStyle(
    val color: Color,
    val lineStyle: ComposeChartLineStyle = ComposeChartLineStyle.Linear,
    val linePattern: ComposeChartLinePattern = ComposeChartLinePattern.Solid,
    val pointShape: ComposeChartPointShape = ComposeChartPointShape.Circle,
    val pointFill: ComposeChartPointFill = ComposeChartPointFill.Filled,
)

@Immutable
internal data class ComposeChartRangeBand(
    val min: Double,
    val max: Double,
    val color: Color,
)

@Immutable
internal data class ComposeChartControlLabels(
    val xAxisRange: String? = null,
    val xAxisInterval: String? = null,
    val yAxisInterval: String? = null,
    val yAxisBounds: String? = null,
    val fullscreen: String? = null,
    val moreStyles: String? = null,
)

@Immutable
internal data class ComposeChartAxisSpec(
    val label: String,
    val position: ComposeChartAxisSide,
    val tickCount: Int = 5,
    val interval: Double? = null,
    val tickPolicy: ChartTickPolicy = NumericChartTickPolicy,
    val tickOptionId: String? = null,
    val gridLinePatterns: List<ComposeChartLinePattern> = listOf(ComposeChartLinePattern.Dotted),
    val valueFormatter: (Double, Double) -> String = { value, _ -> "%.0f".format(value) },
    val minLabelGapPx: Float = 8f,
)

@Immutable
internal data class ComposeBaseChartSpec(
    val title: String,
    val chartStateKey: String? = null,
    val series: List<ComposeChartSeries>,
    val xAxis: ComposeChartAxisSpec,
    val yAxis: ComposeChartAxisSpec,
    val xVisibleRange: Double? = null,
    val yVisibleRange: Double? = null,
    val xBands: List<ComposeChartRangeBand> = emptyList(),
    val yBands: List<ComposeChartRangeBand> = emptyList(),
    val controls: ComposeChartControls = ComposeChartControls(),
)

@Immutable
internal data class ComposeChartControls(
    val showControls: Boolean = true,
    val showLegend: Boolean = true,
    val allowFullscreen: Boolean = true,
    val allowMoreStyles: Boolean = true,
    val labels: ComposeChartControlLabels = ComposeChartControlLabels(),
    val axisRangePercentOptions: List<Int> = listOf(5, 10, 25, 50, 75, 90, 100),
    val axisBoundPercentOptions: List<Int> = listOf(-200, -150, -100, -50, -25, -10, 0, 10, 25, 50, 75, 90, 100, 110, 125, 150, 200),
    val defaultXAxisWindowMode: ComposeChartAxisWindowMode = ComposeChartAxisWindowMode.PercentRange,
    val defaultYAxisWindowMode: ComposeChartAxisWindowMode = ComposeChartAxisWindowMode.PercentBounds,
    val defaultXMinPercent: Int = 0,
    val defaultXMaxPercent: Int? = null,
    val defaultYMinPercent: Int = 0,
    val defaultYMaxPercent: Int? = null,
    val defaultCrosshairBasis: ComposeChartCrosshairBasis = ComposeChartCrosshairBasis.PerpendicularToXAxis,
    val defaultFullscreenAreas: Set<ComposeChartFullscreenArea> = setOf(
        ComposeChartFullscreenArea.ChartDrawing,
    ),
)

private data class CrosshairState(
    val x: Double,
    val y: Double,
    val rawX: Double,
    val rawY: Double,
    val xLabel: String,
    val yLabel: String,
)

@Composable
internal fun ComposeBaseChart(
    spec: ComposeBaseChartSpec,
    savedState: ComposeChartState? = null,
    onChartStateChanged: (ComposeChartState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var liveState by remember(spec.chartStateKey) { mutableStateOf(savedState) }
    LaunchedEffect(savedState) {
        if (savedState != null) liveState = savedState
    }
    val seriesStyleKey = remember(spec.series) { spec.series.joinToString("|") { it.id } }
    val seriesStyles = remember(spec.chartStateKey, savedState, seriesStyleKey) {
        mutableStateMapOf<String, ComposeChartSeriesStyle>().apply {
            spec.series.forEach { series ->
                put(series.id, savedState?.seriesStyles?.get(series.id)?.toComposeStyle() ?: series.toSeriesStyle())
            }
        }
    }
    var selectedSeriesId by remember(spec.chartStateKey, seriesStyleKey) {
        mutableStateOf(spec.series.firstOrNull()?.id)
    }
    var fullscreenAreas by remember(spec.chartStateKey, savedState) {
        mutableStateOf(savedState?.fullscreenAreas?.mapNotNull { it.toFullscreenAreaOrNull() }?.toSet() ?: spec.controls.defaultFullscreenAreas)
    }
    var fullscreen by remember { mutableStateOf(false) }
    ChartSurface(
        spec = spec,
        savedState = liveState,
        fullscreen = false,
        fullscreenAreas = fullscreenAreas,
        seriesStyles = seriesStyles,
        selectedSeriesId = selectedSeriesId,
        onSelectedSeries = { selectedSeriesId = it },
        onSeriesStyleChange = { id, style -> seriesStyles[id] = style },
        onFullscreenAreas = { fullscreenAreas = it },
        onFullscreen = { fullscreen = true },
        onChartStateChanged = { state ->
            liveState = state
            onChartStateChanged(state)
        },
        modifier = modifier,
    )
    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(adaptiveNavigationBarsWindowInsets()),
                color = MaterialTheme.colorScheme.background,
            ) {
                FixedLandscapeBox {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                    ) {
                        ChartSurface(
                            spec = spec,
                            savedState = liveState,
                            fullscreen = true,
                            fullscreenAreas = fullscreenAreas,
                            seriesStyles = seriesStyles,
                            selectedSeriesId = selectedSeriesId,
                            onSelectedSeries = { selectedSeriesId = it },
                            onSeriesStyleChange = { id, style -> seriesStyles[id] = style },
                            onFullscreenAreas = { fullscreenAreas = it },
                            onFullscreen = { },
                            onExitFullscreen = { fullscreen = false },
                            onChartStateChanged = { state ->
                                liveState = state
                                onChartStateChanged(state)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedLandscapeBox(content: @Composable () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val landscapeModifier = if (maxWidth < maxHeight) {
            Modifier
                .requiredWidth(maxHeight)
                .requiredHeight(maxWidth)
                .graphicsLayer(rotationZ = 90f)
        } else {
            Modifier.fillMaxSize()
        }
        Box(modifier = landscapeModifier) {
            content()
        }
    }
}

@Composable
private fun ChartSurface(
    spec: ComposeBaseChartSpec,
    savedState: ComposeChartState?,
    fullscreen: Boolean,
    fullscreenAreas: Set<ComposeChartFullscreenArea>,
    seriesStyles: Map<String, ComposeChartSeriesStyle>,
    selectedSeriesId: String?,
    onSelectedSeries: (String) -> Unit,
    onSeriesStyleChange: (String, ComposeChartSeriesStyle) -> Unit,
    onFullscreenAreas: (Set<ComposeChartFullscreenArea>) -> Unit,
    onFullscreen: () -> Unit,
    onExitFullscreen: (() -> Unit)? = null,
    onChartStateChanged: (ComposeChartState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allPoints = remember(spec.series) { spec.series.flatMap { it.points } }
    val dataMinX = allPoints.minOfOrNull { it.x } ?: 0.0
    val dataMaxX = allPoints.maxOfOrNull { it.x } ?: 1.0
    val dataMinY = allPoints.minOfOrNull { it.y } ?: 0.0
    val dataMaxY = allPoints.maxOfOrNull { it.y } ?: 1.0
    val fullXRange = (dataMaxX - dataMinX).takeIf { it > 0.0 } ?: 1.0
    val fullYRange = (dataMaxY - dataMinY).takeIf { it > 0.0 } ?: 1.0
    val initialXRange = (spec.xVisibleRange ?: fullXRange).coerceIn(fullXRange * 0.05, fullXRange)
    val initialYRange = (spec.yVisibleRange ?: fullYRange).coerceIn(fullYRange * 0.05, fullYRange)

    val initialXRangePercent = initialRangePercent(
        initialRange = initialXRange,
        fullRange = fullXRange,
    )
    val initialYRangePercent = initialRangePercent(
        initialRange = initialYRange,
        fullRange = fullYRange,
    )
    val initialXMaxPercent = initialMaxPercent(
        minPercent = spec.controls.defaultXMinPercent,
        maxPercent = spec.controls.defaultXMaxPercent,
        rangePercent = initialXRangePercent,
    )
    val initialYMaxPercent = initialMaxPercent(
        minPercent = spec.controls.defaultYMinPercent,
        maxPercent = spec.controls.defaultYMaxPercent,
        rangePercent = initialYRangePercent,
    )
    var xWindowMode by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.xWindowMode.toAxisWindowModeOrNull() ?: spec.controls.defaultXAxisWindowMode) }
    var yWindowMode by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.yWindowMode.toAxisWindowModeOrNull() ?: spec.controls.defaultYAxisWindowMode) }
    var xRangePercent by remember(spec.chartStateKey, savedState) { mutableDoubleStateOf(savedState?.xRangePercent ?: initialXRangePercent) }
    var yRangePercent by remember(spec.chartStateKey, savedState) { mutableDoubleStateOf(savedState?.yRangePercent ?: initialYRangePercent) }
    var xMinPercent by remember(spec.chartStateKey, savedState) { mutableDoubleStateOf(savedState?.xMinPercent ?: spec.controls.defaultXMinPercent.toDouble()) }
    var xMaxPercent by remember(spec.chartStateKey, savedState) { mutableDoubleStateOf(savedState?.xMaxPercent ?: initialXMaxPercent) }
    var xTickOptionId by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.xTickOptionId) }
    var yTickOptionId by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.yTickOptionId) }
    var yMinPercent by remember(spec.chartStateKey, savedState) { mutableDoubleStateOf(savedState?.yMinPercent ?: spec.controls.defaultYMinPercent.toDouble()) }
    var yMaxPercent by remember(spec.chartStateKey, savedState) { mutableDoubleStateOf(savedState?.yMaxPercent ?: initialYMaxPercent) }
    var xAxisSide by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.xAxisSide.toAxisSideOrNull() ?: spec.xAxis.position) }
    var yAxisSide by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.yAxisSide.toAxisSideOrNull() ?: spec.yAxis.position) }
    var xGridPattern by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.xGridPattern.toLinePatternOrNull() ?: spec.xAxis.gridLinePatterns.firstOrNull() ?: ComposeChartLinePattern.Dotted) }
    var yGridPattern by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.yGridPattern.toLinePatternOrNull() ?: spec.yAxis.gridLinePatterns.firstOrNull() ?: ComposeChartLinePattern.Dotted) }
    var crosshairBasis by remember(spec.chartStateKey, savedState) { mutableStateOf(savedState?.crosshairBasis.toCrosshairBasisOrNull() ?: spec.controls.defaultCrosshairBasis) }
    var chartStyleDialog by remember { mutableStateOf(false) }
    var controlsExpanded by remember(spec.chartStateKey) { mutableStateOf(false) }
    var seriesStyleDialogId by remember { mutableStateOf<String?>(null) }
    var crosshair by remember { mutableStateOf<CrosshairState?>(null) }
    var xStart by remember(spec.title) { mutableDoubleStateOf(dataMinX) }
    var yMin by remember(spec.title) { mutableDoubleStateOf(dataMinY) }

    val effectiveXMinPercent = minOf(xMinPercent, xMaxPercent - 0.5)
    val effectiveXMaxPercent = maxOf(xMaxPercent, effectiveXMinPercent + 0.5)
    val effectiveXRangePercent = when (xWindowMode) {
        ComposeChartAxisWindowMode.PercentRange -> xRangePercent.coerceIn(0.5, 100.0)
        ComposeChartAxisWindowMode.PercentBounds -> (effectiveXMaxPercent - effectiveXMinPercent).coerceAtLeast(0.5)
    }
    val xWindowMin = when (xWindowMode) {
        ComposeChartAxisWindowMode.PercentRange -> xStart
        ComposeChartAxisWindowMode.PercentBounds -> dataMinX + fullXRange * effectiveXMinPercent / 100.0
    }
    val xVisibleRange = fullXRange * effectiveXRangePercent / 100.0
    val effectiveYMinPercent = minOf(yMinPercent, yMaxPercent - 0.5)
    val effectiveYMaxPercent = maxOf(yMaxPercent, effectiveYMinPercent + 0.5)
    val effectiveYRangePercent = when (yWindowMode) {
        ComposeChartAxisWindowMode.PercentRange -> yRangePercent.coerceIn(0.5, 100.0)
        ComposeChartAxisWindowMode.PercentBounds -> (effectiveYMaxPercent - effectiveYMinPercent).coerceAtLeast(0.5)
    }
    val yWindowMin = when (yWindowMode) {
        ComposeChartAxisWindowMode.PercentRange -> yMin
        ComposeChartAxisWindowMode.PercentBounds -> dataMinY + fullYRange * effectiveYMinPercent / 100.0
    }
    val yVisibleRange = fullYRange * effectiveYRangePercent / 100.0
    val xTickResolution = spec.xAxis.tickPolicy.resolve(xVisibleRange, xTickOptionId)
    val yTickResolution = spec.yAxis.tickPolicy.resolve(yVisibleRange, yTickOptionId)
    LaunchedEffect(
        xWindowMode, yWindowMode, xRangePercent, yRangePercent, xMinPercent, xMaxPercent,
        yMinPercent, yMaxPercent, xTickResolution, yTickResolution, xAxisSide, yAxisSide,
        xGridPattern, yGridPattern, crosshairBasis, fullscreenAreas, seriesStyles.toMap(),
    ) {
        onChartStateChanged(
            ComposeChartState(
                xWindowMode = xWindowMode.name,
                yWindowMode = yWindowMode.name,
                xRangePercent = xRangePercent,
                yRangePercent = yRangePercent,
                xMinPercent = xMinPercent,
                xMaxPercent = xMaxPercent,
                yMinPercent = yMinPercent,
                yMaxPercent = yMaxPercent,
                xInterval = xTickResolution.interval,
                yInterval = yTickResolution.interval,
                xTickOptionId = xTickResolution.selectedId,
                yTickOptionId = yTickResolution.selectedId,
                xAxisSide = xAxisSide.name,
                yAxisSide = yAxisSide.name,
                xGridPattern = xGridPattern.name,
                yGridPattern = yGridPattern.name,
                crosshairBasis = crosshairBasis.name,
                fullscreenAreas = fullscreenAreas.map { it.name }.toSet(),
                seriesStyles = seriesStyles.mapValues { (_, style) ->
                    ComposeChartSeriesState(
                        colorArgb = style.color.value.toLong(),
                        lineStyle = style.lineStyle.name,
                        linePattern = style.linePattern.name,
                        pointShape = style.pointShape.name,
                        pointFill = style.pointFill.name,
                    )
                },
            ),
        )
    }
    val effectiveSpec = spec.copy(
        series = spec.series.map {
            val style = seriesStyles[it.id] ?: it.toSeriesStyle()
            it.copy(
                color = style.color,
                lineStyle = style.lineStyle,
                linePattern = style.linePattern,
                pointShape = style.pointShape,
                pointFill = style.pointFill,
            )
        },
        xVisibleRange = xVisibleRange,
        yVisibleRange = yVisibleRange,
        xAxis = spec.xAxis.copy(
            position = xAxisSide,
            interval = xTickResolution.interval,
            tickOptionId = xTickResolution.selectedId,
            gridLinePatterns = listOf(xGridPattern),
        ),
        yAxis = spec.yAxis.copy(
            position = yAxisSide,
            interval = yTickResolution.interval,
            tickOptionId = yTickResolution.selectedId,
            gridLinePatterns = listOf(yGridPattern),
        ),
    )
    val resolvedCrosshair = crosshair?.let { previous ->
        resolveCrosshair(
            spec = effectiveSpec,
            selectedSeriesId = selectedSeriesId,
            crosshairBasis = crosshairBasis,
            rawX = previous.rawX,
            rawY = previous.rawY,
            xMin = xStart,
            xMax = xStart + xVisibleRange,
            yMin = yMin,
            yMax = yMin + yVisibleRange,
        )
    }

    LaunchedEffect(spec.title, dataMinX, dataMaxX, dataMinY, dataMaxY, xVisibleRange, yVisibleRange, xWindowMin, yWindowMin) {
        val ranges = listOf(dataMinX, dataMaxX, dataMinY, dataMaxY, xVisibleRange, yVisibleRange)
        if (ranges.any { !it.isFiniteValue() }) {
            Log.e(
                TAG,
                "Invalid chart range for ${spec.title}: x=[$dataMinX,$dataMaxX] " +
                    "y=[$dataMinY,$dataMaxY] visible=[$xVisibleRange,$yVisibleRange]",
            )
        }
        xStart = if (xWindowMode == ComposeChartAxisWindowMode.PercentBounds) {
            xWindowMin
        } else {
            clampWindowStart(xWindowMin, dataMinX, dataMaxX, xVisibleRange)
        }
        yMin = yWindowMin
    }

    if (chartStyleDialog) {
        ChartStyleDialog(
            xAxisSide = xAxisSide,
            yAxisSide = yAxisSide,
            xGridPattern = xGridPattern,
            yGridPattern = yGridPattern,
            crosshairBasis = crosshairBasis,
            fullscreenAreas = fullscreenAreas,
            onXAxisSide = { xAxisSide = it },
            onYAxisSide = { yAxisSide = it },
            onXGridPattern = { xGridPattern = it },
            onYGridPattern = { yGridPattern = it },
            onCrosshairBasis = { crosshairBasis = it },
            onFullscreenAreas = onFullscreenAreas,
            onDismiss = { chartStyleDialog = false },
        )
    }
    seriesStyleDialogId?.let { seriesId ->
        val series = effectiveSpec.series.firstOrNull { it.id == seriesId }
        val style = seriesStyles[seriesId]
        if (series != null && style != null) {
            SeriesStyleDialog(
                seriesLabel = series.label,
                style = style,
                onStyleChange = { onSeriesStyleChange(seriesId, it) },
                onDismiss = { seriesStyleDialogId = null },
            )
        } else {
            seriesStyleDialogId = null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (fullscreen) 0.dp else 460.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (fullscreen) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier
                .then(if (fullscreen) Modifier.fillMaxSize() else Modifier)
                .padding(if (fullscreen) 4.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (fullscreen) 4.dp else 10.dp),
        ) {
            if (spec.controls.showControls && !fullscreen) {
                AppOutlinedIconTextButton(
                    text = stringResource(R.string.compose_chart_style_settings),
                    iconRes = R.drawable.ic_settings,
                    onClick = { controlsExpanded = !controlsExpanded },
                )
            }
            if (spec.controls.showControls && !fullscreen && controlsExpanded) {
                ChartControls(
                    xWindowMode = xWindowMode,
                    yWindowMode = yWindowMode,
                    xRangePercent = xRangePercent,
                    xMinPercent = xMinPercent,
                    xMaxPercent = xMaxPercent,
                    xTickOptions = xTickResolution.options,
                    yTickOptions = yTickResolution.options,
                    xTickOptionId = xTickResolution.selectedId,
                    yTickOptionId = yTickResolution.selectedId,
                    yRangePercent = yRangePercent,
                    yMinPercent = yMinPercent,
                    yMaxPercent = yMaxPercent,
                    controls = spec.controls,
                    onXWindowMode = {
                        xWindowMode = it
                        crosshair = null
                    },
                    onYWindowMode = {
                        yWindowMode = it
                        crosshair = null
                    },
                    onXRangePercent = {
                        val rangePercent = it.coerceIn(0.5, 100.0)
                        xRangePercent = rangePercent
                        xStart = clampWindowStart(xStart, dataMinX, dataMaxX, fullXRange * rangePercent / 100.0)
                        crosshair = null
                    },
                    onXMinPercent = {
                        val minPercent = it.coerceIn(-200.0, 199.5)
                        xMinPercent = minPercent
                        if (xMaxPercent <= minPercent) xMaxPercent = minPercent + 0.5
                        crosshair = null
                    },
                    onXMaxPercent = {
                        val maxPercent = it.coerceIn(-199.5, 200.0)
                        xMaxPercent = maxPercent
                        if (xMinPercent >= maxPercent) xMinPercent = maxPercent - 0.5
                        crosshair = null
                    },
                    onXInterval = { xTickOptionId = it; crosshair = null },
                    onYInterval = { yTickOptionId = it; crosshair = null },
                    onYRangePercent = {
                        val rangePercent = it.coerceIn(0.5, 100.0)
                        yRangePercent = rangePercent
                        yMin = clampWindowStart(yMin, dataMinY, dataMaxY, fullYRange * rangePercent / 100.0)
                        crosshair = null
                    },
                    onYMinPercent = {
                        val minPercent = it.coerceIn(-200.0, 199.5)
                        yMinPercent = minPercent
                        if (yMaxPercent <= minPercent) yMaxPercent = minPercent + 0.5
                        crosshair = null
                    },
                    onYMaxPercent = {
                        val maxPercent = it.coerceIn(-199.5, 200.0)
                        yMaxPercent = maxPercent
                        if (yMinPercent >= maxPercent) yMinPercent = maxPercent - 0.5
                        crosshair = null
                    },
                    onMoreStyles = { chartStyleDialog = true },
                    allowMoreStyles = spec.controls.allowMoreStyles,
                )
            }
            if (!fullscreen || ComposeChartFullscreenArea.Title in fullscreenAreas || onExitFullscreen != null) {
                ChartTitleArea(
                    title = if (!fullscreen || ComposeChartFullscreenArea.Title in fullscreenAreas) spec.title else String(CharArray(0)),
                    fullscreen = fullscreen,
                    allowFullscreen = spec.controls.allowFullscreen && !fullscreen,
                    fullscreenLabel = spec.controls.labels.fullscreen ?: stringResource(R.string.view_chart_fullscreen),
                    onFullscreen = onFullscreen,
                    onExitFullscreen = onExitFullscreen,
                )
            }
            ChartCanvasArea(
                spec = effectiveSpec,
                dataMinX = dataMinX,
                dataMaxX = dataMaxX,
                dataMinY = dataMinY,
                dataMaxY = dataMaxY,
                xStart = xStart,
                yMin = yMin,
                xVisibleRange = xVisibleRange,
                yVisibleRange = yVisibleRange,
                selectedSeriesId = selectedSeriesId,
                crosshairBasis = crosshairBasis,
                crosshair = resolvedCrosshair,
                onXStart = {
                        val spanPercent = effectiveXRangePercent.coerceAtLeast(0.5)
                        val startPercent = percentInRange(it, dataMinX, fullXRange)
                            .coerceIn(
                                if (xWindowMode == ComposeChartAxisWindowMode.PercentBounds) -200.0 else 0.0,
                                if (xWindowMode == ComposeChartAxisWindowMode.PercentBounds) 200.0 - spanPercent else 100.0 - spanPercent,
                            )
                        xMinPercent = startPercent
                        xMaxPercent = startPercent + spanPercent
                        xStart = it
                },
                onYMin = {
                        val spanPercent = effectiveYRangePercent.coerceAtLeast(0.5)
                        val startPercent = percentInRange(it, dataMinY, fullYRange)
                            .coerceIn(
                                if (yWindowMode == ComposeChartAxisWindowMode.PercentBounds) -200.0 else 0.0,
                                if (yWindowMode == ComposeChartAxisWindowMode.PercentBounds) 200.0 - spanPercent else 100.0 - spanPercent,
                            )
                        yMinPercent = startPercent
                        yMaxPercent = startPercent + spanPercent
                        yMin = it
                },
                onCrosshair = { crosshair = it },
                fullscreen = fullscreen,
                modifier = if (fullscreen) Modifier.weight(1f) else Modifier,
            )
            if (spec.controls.showLegend && (!fullscreen || ComposeChartFullscreenArea.DataGroupLegend in fullscreenAreas)) {
                ChartLegend(
                    series = effectiveSpec.series,
                    selectedSeriesId = selectedSeriesId,
                    onSeriesClick = {
                        if (it == selectedSeriesId) {
                            seriesStyleDialogId = it
                        } else {
                            onSelectedSeries(it)
                        }
                        crosshair = null
                    },
                )
            }
        }
    }
}

@Composable
private fun ChartControls(
    xWindowMode: ComposeChartAxisWindowMode,
    yWindowMode: ComposeChartAxisWindowMode,
    xRangePercent: Double,
    xMinPercent: Double,
    xMaxPercent: Double,
    xTickOptions: List<ChartTickOption>,
    yTickOptions: List<ChartTickOption>,
    xTickOptionId: String,
    yTickOptionId: String,
    yRangePercent: Double,
    yMinPercent: Double,
    yMaxPercent: Double,
    controls: ComposeChartControls,
    onXWindowMode: (ComposeChartAxisWindowMode) -> Unit,
    onYWindowMode: (ComposeChartAxisWindowMode) -> Unit,
    onXRangePercent: (Double) -> Unit,
    onXMinPercent: (Double) -> Unit,
    onXMaxPercent: (Double) -> Unit,
    onXInterval: (String) -> Unit,
    onYInterval: (String) -> Unit,
    onYRangePercent: (Double) -> Unit,
    onYMinPercent: (Double) -> Unit,
    onYMaxPercent: (Double) -> Unit,
    onMoreStyles: () -> Unit,
    allowMoreStyles: Boolean,
) {
    Column(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AxisWindowSettingsGroup(
                modeLabel = stringResource(R.string.compose_chart_x_window_mode),
                boundsLabel = controls.labels.xAxisRange ?: stringResource(R.string.compose_chart_x_axis_bounds),
                mode = xWindowMode,
                rangePercent = xRangePercent,
                minPercent = xMinPercent,
                maxPercent = xMaxPercent,
                rangeOptions = controls.axisRangePercentOptions,
                boundOptions = controls.axisBoundPercentOptions,
                onMode = onXWindowMode,
                onRangePercent = onXRangePercent,
                onMinPercent = onXMinPercent,
                onMaxPercent = onXMaxPercent,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AxisWindowSettingsGroup(
                modeLabel = stringResource(R.string.compose_chart_y_window_mode),
                boundsLabel = controls.labels.yAxisBounds ?: stringResource(R.string.compose_chart_y_axis_bounds),
                mode = yWindowMode,
                rangePercent = yRangePercent,
                minPercent = yMinPercent,
                maxPercent = yMaxPercent,
                rangeOptions = controls.axisRangePercentOptions,
                boundOptions = controls.axisBoundPercentOptions,
                onMode = onYWindowMode,
                onRangePercent = onYRangePercent,
                onMinPercent = onYMinPercent,
                onMaxPercent = onYMaxPercent,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ChartDropdown(
                label = controls.labels.xAxisInterval ?: stringResource(R.string.view_chart_x_axis_interval),
                value = xTickOptions.firstOrNull { it.id == xTickOptionId }?.label.orEmpty(),
                options = xTickOptions,
                optionId = { it.id },
                optionLabel = { it.label },
                onSelected = { onXInterval(it.id) },
            )
            ChartDropdown(
                label = controls.labels.yAxisInterval ?: stringResource(R.string.compose_chart_y_axis_interval),
                value = yTickOptions.firstOrNull { it.id == yTickOptionId }?.label.orEmpty(),
                options = yTickOptions,
                optionId = { it.id },
                optionLabel = { it.label },
                onSelected = { onYInterval(it.id) },
            )
        }
        if (allowMoreStyles) {
            AppOutlinedIconTextButton(
                text = controls.labels.moreStyles ?: stringResource(R.string.compose_chart_more_styles),
                iconRes = R.drawable.ic_settings,
                onClick = onMoreStyles,
            )
        }
    }
}

@Composable
private fun AxisWindowSettingsGroup(
    modeLabel: String,
    boundsLabel: String,
    mode: ComposeChartAxisWindowMode,
    rangePercent: Double,
    minPercent: Double,
    maxPercent: Double,
    rangeOptions: List<Int>,
    boundOptions: List<Int>,
    onMode: (ComposeChartAxisWindowMode) -> Unit,
    onRangePercent: (Double) -> Unit,
    onMinPercent: (Double) -> Unit,
    onMaxPercent: (Double) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChartDropdown(
                label = modeLabel,
                value = mode.windowModeLabel(),
                options = ComposeChartAxisWindowMode.entries,
                optionLabel = { it.windowModeLabel() },
                onSelected = onMode,
            )
            when (mode) {
                ComposeChartAxisWindowMode.PercentRange -> AppStepperDropdownField(
                    label = boundsLabel,
                    value = percentLabel(rangePercent),
                    options = rangeOptions.map { AppDropdownOption(it.toString(), "$it%") },
                    onSelect = { onRangePercent(it.id.toDouble()) },
                    numericValue = rangePercent,
                    minimum = rangeOptions.minOrNull()?.toDouble() ?: rangePercent,
                    maximum = rangeOptions.maxOrNull()?.toDouble() ?: rangePercent,
                    step = 0.5,
                    onValueChange = onRangePercent,
                    modifier = Modifier.requiredWidth(210.dp),
                )
                ComposeChartAxisWindowMode.PercentBounds -> {
                    AppStepperDropdownField(
                        label = boundsLabel,
                        value = percentLabel(minPercent),
                        options = boundOptions.map { AppDropdownOption(it.toString(), "$it%") },
                        onSelect = { onMinPercent(it.id.toDouble()) },
                        numericValue = minPercent,
                        minimum = boundOptions.minOrNull()?.toDouble() ?: minPercent,
                        maximum = minOf(
                            boundOptions.maxOrNull()?.toDouble() ?: minPercent,
                            maxPercent - 0.5,
                        ),
                        step = 0.5,
                        onValueChange = onMinPercent,
                        modifier = Modifier.requiredWidth(210.dp),
                    )
                    Text("~", style = MaterialTheme.typography.bodyMedium)
                    AppStepperDropdownField(
                        label = "",
                        value = percentLabel(maxPercent),
                        options = boundOptions.map { AppDropdownOption(it.toString(), "$it%") },
                        onSelect = { onMaxPercent(it.id.toDouble()) },
                        numericValue = maxPercent,
                        minimum = maxOf(
                            boundOptions.minOrNull()?.toDouble() ?: maxPercent,
                            minPercent + 0.5,
                        ),
                        maximum = boundOptions.maxOrNull()?.toDouble() ?: maxPercent,
                        step = 0.5,
                        onValueChange = onMaxPercent,
                        modifier = Modifier.requiredWidth(210.dp),
                    )
                }
            }
        }
    }
}

private fun percentLabel(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()}%" else "%.1f%%".format(value)


@Composable
private fun ChartTitleArea(
    title: String,
    fullscreen: Boolean,
    allowFullscreen: Boolean,
    fullscreenLabel: String,
    onFullscreen: () -> Unit,
    onExitFullscreen: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (allowFullscreen) {
            Button(onClick = onFullscreen) {
                Icon(
                    painter = painterResource(R.drawable.ic_fullscreen),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(fullscreenLabel)
            }
        }
        onExitFullscreen?.let { exit ->
            TextButton(onClick = exit) {
                Icon(
                    painter = painterResource(R.drawable.ic_fullscreen),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.view_chart_exit_fullscreen))
            }
        }
    }
}

@Composable
private fun <T> ChartDropdown(
    label: String,
    value: String,
    options: List<T>,
    optionId: (T) -> String = { it.toString() },
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    val dropdownOptions = options.map { option ->
        AppDropdownOption(id = optionId(option), label = optionLabel(option))
    }
    AppDropdownField(
        label = label,
        value = value,
        options = dropdownOptions,
        onSelect = { selected -> options.firstOrNull { optionId(it) == selected.id }?.let(onSelected) },
        modifier = Modifier.requiredWidth(180.dp),
    )
}

@Composable
private fun ChartStyleDialog(
    xAxisSide: ComposeChartAxisSide,
    yAxisSide: ComposeChartAxisSide,
    xGridPattern: ComposeChartLinePattern,
    yGridPattern: ComposeChartLinePattern,
    crosshairBasis: ComposeChartCrosshairBasis,
    fullscreenAreas: Set<ComposeChartFullscreenArea>,
    onXAxisSide: (ComposeChartAxisSide) -> Unit,
    onYAxisSide: (ComposeChartAxisSide) -> Unit,
    onXGridPattern: (ComposeChartLinePattern) -> Unit,
    onYGridPattern: (ComposeChartLinePattern) -> Unit,
    onCrosshairBasis: (ComposeChartCrosshairBasis) -> Unit,
    onFullscreenAreas: (Set<ComposeChartFullscreenArea>) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.compose_chart_style_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_x_axis_side),
                    value = xAxisSide.axisLabel(),
                    options = listOf(ComposeChartAxisSide.Top, ComposeChartAxisSide.Bottom),
                    optionLabel = { it.axisLabel() },
                    onSelected = onXAxisSide,
                )
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_y_axis_side),
                    value = yAxisSide.axisLabel(),
                    options = listOf(ComposeChartAxisSide.Start, ComposeChartAxisSide.End),
                    optionLabel = { it.axisLabel() },
                    onSelected = onYAxisSide,
                )
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_x_grid),
                    value = xGridPattern.patternLabel(),
                    options = ComposeChartLinePattern.entries,
                    optionLabel = { it.patternLabel() },
                    onSelected = onXGridPattern,
                )
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_y_grid),
                    value = yGridPattern.patternLabel(),
                    options = ComposeChartLinePattern.entries,
                    optionLabel = { it.patternLabel() },
                    onSelected = onYGridPattern,
                )
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_crosshair_basis),
                    value = crosshairBasis.basisLabel(),
                    options = ComposeChartCrosshairBasis.entries,
                    optionLabel = { it.basisLabel() },
                    onSelected = onCrosshairBasis,
                )
                Text(
                    text = stringResource(R.string.compose_chart_fullscreen_areas),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                listOf(
                    ComposeChartFullscreenArea.Title,
                    ComposeChartFullscreenArea.DataGroupLegend,
                )
                    .forEach { area ->
                    val checked = area in fullscreenAreas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = if (checked) {
                                    fullscreenAreas - area
                                } else {
                                    fullscreenAreas + area
                                }.let {
                                    if (ComposeChartFullscreenArea.ChartDrawing in it) it else it + ComposeChartFullscreenArea.ChartDrawing
                                }
                                onFullscreenAreas(next)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                        )
                        Text(
                            text = area.areaLabel(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.compose_confirm_dialog_ok))
            }
        },
    )
}

@Composable
private fun SeriesStyleDialog(
    seriesLabel: String,
    style: ComposeChartSeriesStyle,
    onStyleChange: (ComposeChartSeriesStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.compose_chart_series_style_settings, seriesLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.compose_chart_series_color))
                chartSeriesColorPalette.chunked(4).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowColors.forEach { color ->
                            Surface(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { onStyleChange(style.copy(color = color)) },
                                shape = CircleShape,
                                color = color,
                                border = if (color == style.color) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                },
                            ) {}
                        }
                    }
                }
                ChartDropdown(
                    label = stringResource(R.string.view_chart_line_style),
                    value = style.lineStyle.styleLabel(),
                    options = ComposeChartLineStyle.entries,
                    optionLabel = { it.styleLabel() },
                    onSelected = { onStyleChange(style.copy(lineStyle = it)) },
                )
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_line_pattern),
                    value = style.linePattern.patternLabel(),
                    options = ComposeChartLinePattern.entries,
                    optionLabel = { it.patternLabel() },
                    onSelected = { onStyleChange(style.copy(linePattern = it)) },
                )
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_point_shape),
                    value = style.pointShape.shapeLabel(),
                    options = ComposeChartPointShape.entries,
                    optionLabel = { it.shapeLabel() },
                    onSelected = { onStyleChange(style.copy(pointShape = it)) },
                )
                ChartDropdown(
                    label = stringResource(R.string.compose_chart_point_fill),
                    value = style.pointFill.fillLabel(),
                    options = ComposeChartPointFill.entries,
                    optionLabel = { it.fillLabel() },
                    onSelected = { onStyleChange(style.copy(pointFill = it)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.compose_confirm_dialog_ok))
            }
        },
    )
}

@Composable
private fun ChartCanvasArea(
    spec: ComposeBaseChartSpec,
    dataMinX: Double,
    dataMaxX: Double,
    dataMinY: Double,
    dataMaxY: Double,
    xStart: Double,
    yMin: Double,
    xVisibleRange: Double,
    yVisibleRange: Double,
    selectedSeriesId: String?,
    crosshairBasis: ComposeChartCrosshairBasis,
    crosshair: CrosshairState?,
    onXStart: (Double) -> Unit,
    onYMin: (Double) -> Unit,
    onCrosshair: (CrosshairState?) -> Unit,
    fullscreen: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    var marqueeTimeNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { marqueeTimeNanos = it }
        }
    }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textStyle = TextStyle(color = onSurfaceVariant, fontSize = 12.sp)
    val canPanX = dataMaxX - dataMinX > xVisibleRange
    val canPanY = dataMaxY - dataMinY > yVisibleRange
    fun crosshairFromOffset(offset: Offset, width: Float, height: Float): CrosshairState? {
        val chart = chartRect(
            size = Size(width, height),
            spec = spec,
            xMin = xStart,
            xMax = xStart + xVisibleRange,
            yMin = yMin,
            yMax = yMin + yVisibleRange,
            textMeasurer = textMeasurer,
            textStyle = textStyle,
        )
        if (chart.width <= 0f || chart.height <= 0f || offset.x !in chart.left..chart.right || offset.y !in chart.top..chart.bottom) {
            return null
        }
        val selectedSeries = spec.series.firstOrNull { it.id == selectedSeriesId } ?: spec.series.firstOrNull() ?: return null
        val visiblePoints = visibleSegmentPoints(selectedSeries.points, xStart, xStart + xVisibleRange, selectedSeries.lineStyle)
        if (visiblePoints.size < 2) return null
        val xFraction = ((offset.x - chart.left) / chart.width).toDouble().coerceIn(0.0, 1.0)
        val yFraction = ((offset.y - chart.top) / chart.height).toDouble().coerceIn(0.0, 1.0)
        val rawX = xStart + if (spec.yAxis.position == ComposeChartAxisSide.Start) xFraction * xVisibleRange else (1.0 - xFraction) * xVisibleRange
        val rawY = yMin + if (spec.xAxis.position == ComposeChartAxisSide.Bottom) (1.0 - yFraction) * yVisibleRange else yFraction * yVisibleRange
        val selectedPoint = ChartCrosshairResolver.resolve(
            points = visiblePoints,
            lineStyle = selectedSeries.lineStyle,
            basis = crosshairBasis,
            rawX = rawX,
            rawY = rawY,
        ) ?: return null
        val xTicks = axisTicks(xStart, xStart + xVisibleRange, spec.xAxis)
        val yTicks = axisTicks(yMin, yMin + yVisibleRange, spec.yAxis)
        return CrosshairState(
            x = selectedPoint.x,
            y = selectedPoint.y,
            rawX = rawX,
            rawY = rawY,
            xLabel = spec.xAxis.valueFormatter(selectedPoint.x, xTicks.step),
            yLabel = spec.yAxis.valueFormatter(selectedPoint.y, yTicks.step),
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val chartModifier = Modifier
            .fillMaxWidth()
            .then(if (fullscreen) Modifier.fillMaxHeight() else Modifier.height(320.dp))
            .background(surfaceVariant.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .pointerInput(spec, selectedSeriesId, crosshairBasis, xStart, yMin, xVisibleRange, yVisibleRange) {
                detectTapGestures { offset ->
                    onCrosshair(crosshairFromOffset(offset, size.width.toFloat(), size.height.toFloat()))
                }
            }
            .pointerInput(spec, selectedSeriesId, crosshairBasis, xStart, yMin, xVisibleRange, yVisibleRange) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onCrosshair(crosshairFromOffset(offset, size.width.toFloat(), size.height.toFloat()))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onCrosshair(crosshairFromOffset(change.position, size.width.toFloat(), size.height.toFloat()))
                    },
                )
            }
        val yPanModifier = Modifier
            .width(42.dp)
            .then(if (fullscreen) Modifier.fillMaxHeight() else Modifier.height(320.dp))

        if (canPanX && spec.xAxis.position == ComposeChartAxisSide.Top) {
            HorizontalPanControl(
                dataMin = dataMinX,
                dataMax = dataMaxX,
                windowStart = xStart,
                visibleRange = xVisibleRange,
                onWindowStart = onXStart,
                color = onSurfaceVariant,
                containerColor = surfaceVariant,
                positiveXOnRight = spec.yAxis.position == ComposeChartAxisSide.Start,
            )
        }
        Row(
            modifier = if (fullscreen) Modifier.weight(1f) else Modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
        if (canPanY && spec.yAxis.position == ComposeChartAxisSide.Start) {
            VerticalPanControl(
                dataMin = dataMinY,
                dataMax = dataMaxY,
                windowMin = yMin,
                visibleRange = yVisibleRange,
                onWindowMin = onYMin,
                color = onSurfaceVariant,
                containerColor = surfaceVariant,
                positiveYAtTop = spec.xAxis.position == ComposeChartAxisSide.Bottom,
                modifier = yPanModifier,
            )
        }
        Canvas(modifier = chartModifier.weight(1f)) {
            drawComposeChart(
                spec = spec,
                xMin = xStart,
                xMax = xStart + xVisibleRange,
                yMin = yMin,
                yMax = yMin + yVisibleRange,
                crosshair = crosshair,
                selectedSeriesId = selectedSeriesId,
                axisColor = onSurfaceVariant,
                gridColor = outlineVariant,
                bubbleColor = surfaceColor,
                textMeasurer = textMeasurer,
                textStyle = textStyle,
                marqueeTimeNanos = marqueeTimeNanos,
            )
        }
        if (canPanY && spec.yAxis.position == ComposeChartAxisSide.End) {
            VerticalPanControl(
                dataMin = dataMinY,
                dataMax = dataMaxY,
                windowMin = yMin,
                visibleRange = yVisibleRange,
                onWindowMin = onYMin,
                color = onSurfaceVariant,
                containerColor = surfaceVariant,
                positiveYAtTop = spec.xAxis.position == ComposeChartAxisSide.Bottom,
                modifier = yPanModifier,
            )
        }
        }
        if (canPanX && spec.xAxis.position == ComposeChartAxisSide.Bottom) {
            HorizontalPanControl(
                dataMin = dataMinX,
                dataMax = dataMaxX,
                windowStart = xStart,
                visibleRange = xVisibleRange,
                onWindowStart = onXStart,
                color = onSurfaceVariant,
                containerColor = surfaceVariant,
                positiveXOnRight = spec.yAxis.position == ComposeChartAxisSide.Start,
            )
        }
    }
}

@Composable
private fun HorizontalPanControl(
    dataMin: Double,
    dataMax: Double,
    windowStart: Double,
    visibleRange: Double,
    onWindowStart: (Double) -> Unit,
    color: Color,
    containerColor: Color,
    positiveXOnRight: Boolean,
) {
    val direction = if (positiveXOnRight) 1.0 else -1.0
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        AppRepeatAdjustButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft, onAdjust = {
            onWindowStart(clampWindowStart(windowStart - ChartLayoutMath.percentStep(dataMax - dataMin) * direction, dataMin, dataMax, visibleRange))
        })
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(containerColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        ) {
            drawHorizontalPanIndicator(dataMin, dataMax, windowStart, visibleRange, color)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(containerColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                .pointerInput(dataMin, dataMax, visibleRange, windowStart, positiveXOnRight) {
                    var dragStartWindow = windowStart
                    var totalDragX = 0f
                    detectDragGestures(
                        onDragStart = {
                            dragStartWindow = windowStart
                            totalDragX = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            val widthPx = size.width.toDouble().coerceAtLeast(1.0)
                            val delta = ChartLayoutMath.panDelta(totalDragX, widthPx.toFloat(), dataMax - dataMin, visibleRange, direction)
                            onWindowStart(clampWindowStart(dragStartWindow + delta, dataMin, dataMax, visibleRange))
                        },
                    )
                },
        ) {}
        }
        AppRepeatAdjustButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight, onAdjust = {
            onWindowStart(clampWindowStart(windowStart + ChartLayoutMath.percentStep(dataMax - dataMin) * direction, dataMin, dataMax, visibleRange))
        })
    }
}

@Composable
private fun VerticalPanControl(
    dataMin: Double,
    dataMax: Double,
    windowMin: Double,
    visibleRange: Double,
    onWindowMin: (Double) -> Unit,
    color: Color,
    containerColor: Color,
    positiveYAtTop: Boolean,
    modifier: Modifier = Modifier,
) {
    val direction = if (positiveYAtTop) -1.0 else 1.0
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppRepeatAdjustButton(icon = Icons.Filled.KeyboardArrowUp, onAdjust = {
            onWindowMin(clampWindowStart(windowMin + ChartLayoutMath.percentStep(dataMax - dataMin) * direction, dataMin, dataMax, visibleRange))
        })
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            modifier = Modifier
                .width(12.dp)
                .fillMaxSize()
                .background(containerColor.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
        ) {
            drawVerticalPanIndicator(dataMin, dataMax, windowMin, visibleRange, color)
        }
        Canvas(
            modifier = Modifier
                .width(16.dp)
                .fillMaxSize()
                .background(containerColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                .pointerInput(dataMin, dataMax, visibleRange, windowMin, positiveYAtTop) {
                    var dragStartWindow = windowMin
                    var totalDragY = 0f
                    detectDragGestures(
                        onDragStart = {
                            dragStartWindow = windowMin
                            totalDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragY += dragAmount.y
                            val heightPx = size.height.toDouble().coerceAtLeast(1.0)
                            val delta = ChartLayoutMath.panDelta(totalDragY, heightPx.toFloat(), dataMax - dataMin, visibleRange, direction)
                            onWindowMin(clampWindowStart(dragStartWindow + delta, dataMin, dataMax, visibleRange))
                        },
                    )
                },
        ) {}
        }
        AppRepeatAdjustButton(icon = Icons.Filled.KeyboardArrowDown, onAdjust = {
            onWindowMin(clampWindowStart(windowMin - ChartLayoutMath.percentStep(dataMax - dataMin) * direction, dataMin, dataMax, visibleRange))
        })
    }
}

@Composable
private fun ChartLegend(
    series: List<ComposeChartSeries>,
    selectedSeriesId: String?,
    onSeriesClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        series.forEach { item ->
            val selected = item.id == selectedSeriesId
            Surface(
                modifier = Modifier.clickable { onSeriesClick(item.id) },
                shape = RoundedCornerShape(6.dp),
                color = if (selected) {
                    item.color.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(1.dp, item.color.copy(alpha = 0.42f))
                } else {
                    null
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(28.dp, 12.dp)) {
                        drawLine(
                            color = item.color,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round,
                            pathEffect = patternEffect(listOf(item.linePattern), 0),
                        )
                        drawPointShape(
                            center = Offset(size.width / 2f, size.height / 2f),
                            color = item.color,
                            shape = item.pointShape,
                            fill = item.pointFill,
                            radius = 4f,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (selected) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.compose_chart_selected_series),
                            style = MaterialTheme.typography.labelSmall,
                            color = item.color,
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawComposeChart(
    spec: ComposeBaseChartSpec,
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    crosshair: CrosshairState?,
    selectedSeriesId: String?,
    axisColor: Color,
    gridColor: Color,
    bubbleColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    marqueeTimeNanos: Long,
) {
    val chart = chartRect(
        size = size,
        spec = spec,
        xMin = xMin,
        xMax = xMax,
        yMin = yMin,
        yMax = yMax,
        textMeasurer = textMeasurer,
        textStyle = textStyle,
    )
    if (chart.width <= 0f || chart.height <= 0f) return
    if (!xMin.isFiniteValue() || !xMax.isFiniteValue() || !yMin.isFiniteValue() || !yMax.isFiniteValue() || xMax <= xMin || yMax <= yMin) {
        Log.e(TAG, "Skip invalid draw range for ${spec.title}: x=[$xMin,$xMax] y=[$yMin,$yMax]")
        return
    }

    fun sx(x: Double): Float {
        val fraction = ((x - xMin) / (xMax - xMin)).toFloat()
        return if (spec.yAxis.position == ComposeChartAxisSide.Start) {
            chart.left + fraction * chart.width
        } else {
            chart.right - fraction * chart.width
        }
    }
    fun sy(y: Double): Float {
        val fraction = ((y - yMin) / (yMax - yMin)).toFloat()
        return if (spec.xAxis.position == ComposeChartAxisSide.Bottom) {
            chart.bottom - fraction * chart.height
        } else {
            chart.top + fraction * chart.height
        }
    }
    fun drawMeasuredText(
        text: String,
        measured: androidx.compose.ui.text.TextLayoutResult,
        requestedTopLeft: Offset,
        color: Color = textStyle.color,
        safeArea: Rect = Rect(0f, 0f, size.width, size.height),
    ) {
        val measuredWidth = measured.size.width.toFloat()
        val measuredHeight = measured.size.height.toFloat()
        if (
            safeArea.width <= 0f || safeArea.height <= 0f ||
            !requestedTopLeft.x.isFiniteValue() || !requestedTopLeft.y.isFiniteValue()
        ) {
            return
        }
        val shrinkRatio = minOf(
            1f,
            safeArea.width / measuredWidth,
            safeArea.height / measuredHeight,
        )
        val fittedStyle = if (shrinkRatio < 1f && textStyle.fontSize.value > 8f) {
            textStyle.copy(fontSize = (textStyle.fontSize.value * shrinkRatio).coerceAtLeast(8f).sp)
        } else {
            textStyle
        }
        val fitted = if (fittedStyle == textStyle) measured else textMeasurer.measure(text, fittedStyle)
        val fittedWidth = fitted.size.width.toFloat()
        val fittedHeight = fitted.size.height.toFloat()
        val canScrollHorizontally = fittedWidth > safeArea.width
        val canScrollVertically = fittedHeight > safeArea.height
        clipRect(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom) {
            val left = if (canScrollHorizontally) {
                marqueeOffset(marqueeTimeNanos, fittedWidth + 12f, safeArea.width) + safeArea.left
            } else {
                requestedTopLeft.x.coerceIn(safeArea.left, safeArea.right - fittedWidth)
            }
            val top = if (canScrollVertically) {
                marqueeOffset(marqueeTimeNanos, fittedHeight + 8f, safeArea.height) + safeArea.top
            } else {
                requestedTopLeft.y.coerceIn(safeArea.top, safeArea.bottom - fittedHeight)
            }
            drawText(
                textMeasurer = textMeasurer,
                text = text,
                topLeft = Offset(left, top),
                style = fittedStyle.copy(color = color),
            )
            if (canScrollHorizontally) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(left + fittedWidth + 12f, top),
                    style = fittedStyle.copy(color = color),
                )
            }
        }
    }

    val yTickArea = if (spec.yAxis.position == ComposeChartAxisSide.Start) {
        Rect(0f, chart.top, chart.left - 6f, chart.bottom)
    } else {
        Rect(chart.right + 6f, chart.top, size.width, chart.bottom)
    }
    val xTickArea = if (spec.xAxis.position == ComposeChartAxisSide.Top) {
        Rect(chart.left, 0f, chart.right, chart.top - 8f)
    } else {
        Rect(chart.left, chart.bottom + 8f, chart.right, size.height)
    }
    val yBandArea = if (spec.yAxis.position == ComposeChartAxisSide.Start) {
        Rect(chart.right + 6f, chart.top, size.width, chart.bottom)
    } else {
        Rect(0f, chart.top, chart.left - 6f, chart.bottom)
    }
    val xBandArea = if (spec.xAxis.position == ComposeChartAxisSide.Top) {
        Rect(chart.left, chart.bottom + 8f, chart.right, size.height)
    } else {
        Rect(chart.left, 0f, chart.right, chart.top - 8f)
    }

    spec.xBands.forEach { band ->
        val start = band.min.coerceAtLeast(xMin)
        val end = band.max.coerceAtMost(xMax)
        if (end > start) drawRect(
            color = band.color,
            topLeft = Offset(sx(start), chart.top),
            size = Size(sx(end) - sx(start), chart.height),
        )
    }
    spec.yBands.forEach { band ->
        val start = band.min.coerceAtLeast(yMin)
        val end = band.max.coerceAtMost(yMax)
        if (end > start) drawRect(
            color = band.color,
            topLeft = Offset(chart.left, sy(end)),
            size = Size(chart.width, sy(start) - sy(end)),
        )
    }

    val yTicks = axisTicks(yMin, yMax, spec.yAxis)
    val yLabelTexts = yTicks.map { spec.yAxis.valueFormatter(it, yTicks.step) }
    val yLabelSizes = yLabelTexts.map { textMeasurer.measure(it, textStyle).size.height.toFloat() }
    val yLabelIndexes = ChartLayoutMath.selectNonOverlappingLabelIndexes(
        centers = yTicks.map(::sy),
        sizes = yLabelSizes,
        gap = spec.yAxis.minLabelGapPx,
    ).toSet()
    yTicks.forEachIndexed { index, tick ->
        val y = sy(tick)
        drawLine(
            color = gridColor,
            start = Offset(chart.left, y),
            end = Offset(chart.right, y),
            strokeWidth = 1.2f,
            pathEffect = patternEffect(spec.yAxis.gridLinePatterns, yTicks.indexOf(tick)),
        )
        if (index !in yLabelIndexes) return@forEachIndexed
        val label = yLabelTexts[index]
        val measured = textMeasurer.measure(label, textStyle)
        val labelX = if (spec.yAxis.position == ComposeChartAxisSide.Start) {
            chart.left - measured.size.width - 6f
        } else {
            chart.right + 6f
        }
        drawMeasuredText(
            text = label,
            measured = measured,
            requestedTopLeft = Offset(labelX, y - measured.size.height / 2f),
            safeArea = yTickArea,
        )
    }

    val xTicks = axisTicks(xMin, xMax, spec.xAxis)
    val xLabelTexts = xTicks.map { spec.xAxis.valueFormatter(it, xTicks.step) }
    val xLabelSizes = xLabelTexts.map { textMeasurer.measure(it, textStyle).size.width.toFloat() }
    val xLabelIndexes = ChartLayoutMath.selectNonOverlappingLabelIndexes(
        centers = xTicks.map(::sx),
        sizes = xLabelSizes,
        gap = spec.xAxis.minLabelGapPx,
    ).toSet()
    xTicks.forEachIndexed { index, tick ->
        val x = sx(tick)
        drawLine(
            color = gridColor,
            start = Offset(x, chart.top),
            end = Offset(x, chart.bottom),
            strokeWidth = 1.2f,
            pathEffect = patternEffect(spec.xAxis.gridLinePatterns, xTicks.indexOf(tick)),
        )
        if (index !in xLabelIndexes) return@forEachIndexed
        val label = xLabelTexts[index]
        val measured = textMeasurer.measure(label, textStyle)
        val labelY = if (spec.xAxis.position == ComposeChartAxisSide.Top) {
            chart.top - measured.size.height - 8f
        } else {
            chart.bottom + 8f
        }
        drawMeasuredText(
            text = label,
            measured = measured,
            requestedTopLeft = Offset(x - measured.size.width / 2f, labelY),
            safeArea = xTickArea,
        )
    }

    val yBandBoundaries = spec.yBands.flatMap { band ->
        listOf(band.min.coerceIn(yMin, yMax), band.max.coerceIn(yMin, yMax)).map { value -> value to band.color }
    }.distinctBy { it.first }
    val yBandBoundarySizes = yBandBoundaries.map { (value, _) ->
        textMeasurer.measure(spec.yAxis.valueFormatter(value, yTicks.step), textStyle).size.height.toFloat()
    }
    val yBandBoundaryIndexes = ChartLayoutMath.selectNonOverlappingLabelIndexes(
        centers = yBandBoundaries.map { (value, _) -> sy(value) },
        sizes = yBandBoundarySizes,
        gap = spec.yAxis.minLabelGapPx,
    ).toSet()
    yBandBoundaries.forEachIndexed { index, (value, color) ->
        if (index !in yBandBoundaryIndexes) return@forEachIndexed
        val label = spec.yAxis.valueFormatter(value, yTicks.step)
        val measured = textMeasurer.measure(label, textStyle)
        val labelX = if (spec.yAxis.position == ComposeChartAxisSide.Start) chart.right + 6f else chart.left - measured.size.width - 6f
        drawMeasuredText(
            text = label,
            measured = measured,
            requestedTopLeft = Offset(labelX, sy(value) - measured.size.height / 2f),
            color = color,
            safeArea = yBandArea,
        )
    }

    val xBandBoundaries = spec.xBands.flatMap { band ->
        listOf(band.min.coerceIn(xMin, xMax), band.max.coerceIn(xMin, xMax)).map { value -> value to band.color }
    }.distinctBy { it.first }
    val xBandBoundarySizes = xBandBoundaries.map { (value, _) ->
        textMeasurer.measure(spec.xAxis.valueFormatter(value, xTicks.step), textStyle).size.width.toFloat()
    }
    val xBandBoundaryIndexes = ChartLayoutMath.selectNonOverlappingLabelIndexes(
        centers = xBandBoundaries.map { (value, _) -> sx(value) },
        sizes = xBandBoundarySizes,
        gap = spec.xAxis.minLabelGapPx,
    ).toSet()
    xBandBoundaries.forEachIndexed { index, (value, color) ->
        if (index !in xBandBoundaryIndexes) return@forEachIndexed
        val label = spec.xAxis.valueFormatter(value, xTicks.step)
        val measured = textMeasurer.measure(label, textStyle)
        val labelY = if (spec.xAxis.position == ComposeChartAxisSide.Top) chart.bottom + 8f else chart.top - measured.size.height - 8f
        drawMeasuredText(
            text = label,
            measured = measured,
            requestedTopLeft = Offset(sx(value) - measured.size.width / 2f, labelY),
            color = color,
            safeArea = xBandArea,
        )
    }

    val xAxisY = if (spec.xAxis.position == ComposeChartAxisSide.Top) chart.top else chart.bottom
    val yAxisX = if (spec.yAxis.position == ComposeChartAxisSide.Start) chart.left else chart.right
    drawLine(axisColor, Offset(chart.left, xAxisY), Offset(chart.right, xAxisY), strokeWidth = 2f)
    drawLine(axisColor, Offset(yAxisX, chart.top), Offset(yAxisX, chart.bottom), strokeWidth = 2f)
    val xPositiveEnd = if (spec.yAxis.position == ComposeChartAxisSide.Start) chart.right else chart.left
    drawAxisArrow(
        tip = Offset(xPositiveEnd, xAxisY),
        direction = if (spec.yAxis.position == ComposeChartAxisSide.Start) Offset(1f, 0f) else Offset(-1f, 0f),
        color = axisColor,
    )
    val yPositiveEnd = if (spec.xAxis.position == ComposeChartAxisSide.Bottom) chart.top else chart.bottom
    drawAxisArrow(
        tip = Offset(yAxisX, yPositiveEnd),
        direction = if (spec.xAxis.position == ComposeChartAxisSide.Bottom) Offset(0f, -1f) else Offset(0f, 1f),
        color = axisColor,
    )
    val xUnitArea = if (spec.yAxis.position == ComposeChartAxisSide.Start) {
        Rect(chart.right, xTickArea.top, size.width, xTickArea.bottom)
    } else {
        Rect(0f, xTickArea.top, chart.left, xTickArea.bottom)
    }
    val yUnitArea = if (spec.xAxis.position == ComposeChartAxisSide.Bottom) {
        Rect(yTickArea.left, 0f, yTickArea.right, chart.top)
    } else {
        Rect(yTickArea.left, chart.bottom, yTickArea.right, size.height)
    }
    val xUnit = textMeasurer.measure(spec.xAxis.label, textStyle)
    val yUnit = textMeasurer.measure(spec.yAxis.label, textStyle)
    drawMeasuredText(
        text = spec.xAxis.label,
        measured = xUnit,
        requestedTopLeft = Offset(
            x = if (spec.yAxis.position == ComposeChartAxisSide.Start) chart.right + 8f else chart.left - xUnit.size.width - 8f,
            y = xTickArea.top + (xTickArea.height - xUnit.size.height) / 2f,
        ),
        safeArea = xUnitArea,
    )
    drawMeasuredText(
        text = spec.yAxis.label,
        measured = yUnit,
        requestedTopLeft = Offset(
            x = yTickArea.left + (yTickArea.width - yUnit.size.width) / 2f,
            y = if (spec.xAxis.position == ComposeChartAxisSide.Bottom) chart.top - yUnit.size.height - 8f else chart.bottom + 8f,
        ),
        safeArea = yUnitArea,
    )
    val xTickEnd = if (spec.xAxis.position == ComposeChartAxisSide.Top) xAxisY - 6f else xAxisY + 6f
    xTicks.forEach { tick ->
        val x = sx(tick)
        drawLine(axisColor, Offset(x, xAxisY), Offset(x, xTickEnd), strokeWidth = 1.5f)
    }
    val yTickEnd = if (spec.yAxis.position == ComposeChartAxisSide.Start) yAxisX - 6f else yAxisX + 6f
    yTicks.forEach { tick ->
        val y = sy(tick)
        drawLine(axisColor, Offset(yAxisX, y), Offset(yTickEnd, y), strokeWidth = 1.5f)
    }
    clipRect(chart.left, chart.top, chart.right, chart.bottom) {
        spec.series.forEach { series ->
            val visible = visibleSegmentPoints(series.points, xMin, xMax, series.lineStyle)
            if (visible.size >= 2) {
                val path = seriesPath(visible, series.lineStyle, ::sx, ::sy)
                drawPath(
                    path = path,
                    color = series.color,
                    style = Stroke(
                        width = 3.4f,
                        cap = StrokeCap.Round,
                        pathEffect = patternEffect(listOf(series.linePattern), 0),
                    ),
                )
            }
            visible.forEach {
                val y = sy(it.y)
                if (y in chart.top..chart.bottom) {
                    drawPointShape(
                        center = Offset(sx(it.x), y),
                        color = series.color,
                        shape = series.pointShape,
                        fill = series.pointFill,
                        radius = 4.8f,
                    )
                }
            }
        }
    }

    crosshair?.let {
        if (it.x in xMin..xMax && it.y in yMin..yMax) {
            val x = sx(it.x)
            val y = sy(it.y)
            val crossColor = axisColor.copy(alpha = 0.72f)
            val selectedColor = spec.series.firstOrNull { series -> series.id == selectedSeriesId }?.color
                ?: spec.series.firstOrNull()?.color
                ?: axisColor
            drawLine(crossColor, Offset(x, chart.top), Offset(x, chart.bottom), strokeWidth = 1.6f, pathEffect = patternEffect(listOf(ComposeChartLinePattern.Dashed), 0))
            drawLine(crossColor, Offset(chart.left, y), Offset(chart.right, y), strokeWidth = 1.6f, pathEffect = patternEffect(listOf(ComposeChartLinePattern.Dashed), 0))
            drawCircle(bubbleColor, radius = 6f, center = Offset(x, y))
            drawCircle(selectedColor, radius = 4f, center = Offset(x, y))
            drawCrosshairBubble(
                x = x,
                y = y,
                dateText = it.xLabel,
                valueText = it.yLabel,
                chart = chart,
                textMeasurer = textMeasurer,
                textStyle = textStyle,
                bubbleColor = bubbleColor,
                borderColor = axisColor,
            )
        }
    }
}

private fun DrawScope.drawAxisArrow(tip: Offset, direction: Offset, color: Color) {
    val perpendicular = Offset(-direction.y, direction.x)
    val base = tip - direction * 8f
    drawLine(color, tip, base + perpendicular * 4f, strokeWidth = 2f)
    drawLine(color, tip, base - perpendicular * 4f, strokeWidth = 2f)
}

private fun DrawScope.drawCrosshairBubble(
    x: Float,
    y: Float,
    dateText: String,
    valueText: String,
    chart: Rect,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    bubbleColor: Color,
    borderColor: Color,
) {
    val xText = 'X'.toString() + 0xFF1A.toChar() + dateText
    val yText = 'Y'.toString() + 0xFF1A.toChar() + valueText
    val date = textMeasurer.measure(xText, textStyle)
    val value = textMeasurer.measure(yText, textStyle)
    val width = maxOf(date.size.width, value.size.width).toFloat() + 20f
    val height = (date.size.height + value.size.height).toFloat() + 18f
    var left = x + 12f
    var top = y - height - 12f
    if (left + width > chart.right) left = x - width - 12f
    if (top < chart.top) top = y + 12f
    left = left.coerceIn(chart.left + 4f, chart.right - width - 4f)
    top = top.coerceIn(chart.top + 4f, chart.bottom - height - 4f)
    drawRoundRect(
        color = bubbleColor.copy(alpha = 0.94f),
        topLeft = Offset(left, top),
        size = Size(width, height),
    )
    drawRoundRect(
        color = borderColor.copy(alpha = 0.56f),
        topLeft = Offset(left, top),
        size = Size(width, height),
        style = Stroke(width = 1f),
    )
    drawText(textMeasurer, xText, Offset(left + 10f, top + 8f), style = textStyle)
    drawText(textMeasurer, yText, Offset(left + 10f, top + 10f + date.size.height), style = textStyle)
}

private fun DrawScope.drawPointShape(
    center: Offset,
    color: Color,
    shape: ComposeChartPointShape,
    fill: ComposeChartPointFill,
    radius: Float,
) {
    val style = if (fill == ComposeChartPointFill.Filled) {
        androidx.compose.ui.graphics.drawscope.Fill
    } else {
        Stroke(width = 2f)
    }
    when (shape) {
        ComposeChartPointShape.Circle -> drawCircle(color, radius = radius, center = center, style = style)
        ComposeChartPointShape.Triangle -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x - radius * 0.866f, center.y + radius * 0.5f)
                lineTo(center.x + radius * 0.866f, center.y + radius * 0.5f)
                close()
            }
            drawPath(path, color, style = style)
        }
        ComposeChartPointShape.Square -> drawRect(
            color = color,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = style,
        )
        ComposeChartPointShape.Diamond -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius, center.y)
                close()
            }
            drawPath(path, color, style = style)
        }
        ComposeChartPointShape.Cross -> {
            drawLine(color, Offset(center.x - radius, center.y - radius), Offset(center.x + radius, center.y + radius), strokeWidth = 2f)
            drawLine(color, Offset(center.x + radius, center.y - radius), Offset(center.x - radius, center.y + radius), strokeWidth = 2f)
        }
    }
}

private data class TickSet(
    val values: List<Double>,
    val step: Double,
) : List<Double> by values

private fun niceTicks(
    min: Double,
    max: Double,
    count: Int,
): TickSet {
    val safeCount = count.coerceAtLeast(2)
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0
    val rawStep = range / (safeCount - 1)
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val normalized = rawStep / magnitude
    val niceNormalized = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    val step = niceNormalized * magnitude
    val first = ceil(min / step) * step
    val values = buildList {
        var value = first
        while (value <= max + TICK_EPSILON && size < 1000) {
            add(value)
            value += step
        }
    }.withVisibleEndpoints(min, max)
    return TickSet(values, step)
}

private fun axisTicks(
    min: Double,
    max: Double,
    axis: ComposeChartAxisSpec,
): TickSet {
    (axis.tickPolicy as? TimeChartTickPolicy)
        ?.calendarTicks(min, max, axis.tickOptionId)
        ?.let { calendarValues ->
            val interval = axis.interval?.takeIf { it > 0.0 && it.isFiniteValue() } ?: return niceTicks(min, max, axis.tickCount)
            return TickSet(calendarValues.withVisibleEndpoints(min, max), interval)
        }
    val interval = axis.interval?.takeIf { it > 0.0 && it.isFiniteValue() }
        ?: return niceTicks(min, max, axis.tickCount)
    val values = buildList {
        var value = ceil(min / interval) * interval
        while (value <= max + TICK_EPSILON && size < 1000) {
            add(value)
            value += interval
        }
    }.withVisibleEndpoints(min, max)
    return TickSet(values, interval)
}

private fun List<Double>.withVisibleEndpoints(min: Double, max: Double): List<Double> =
    (listOf(min) + filter { it in min..max } + listOf(max))
        .sorted()
        .distinctBy { "%.9f".format(it) }

private fun intervalOptionsAround(current: Double): List<Double> {
    val safe = current.takeIf { it > 0.0 && it.isFiniteValue() } ?: 1.0
    return listOf(safe / 4.0, safe / 2.0, safe, safe * 2.0, safe * 4.0)
        .filter { it > 0.0 && it.isFiniteValue() }
        .distinctBy { "%.8f".format(it) }
}

private fun initialRangePercent(
    initialRange: Double,
    fullRange: Double,
): Double =
    ((initialRange / fullRange) * 100.0)
        .coerceIn(0.5, 100.0)

private fun initialMaxPercent(
    minPercent: Int,
    maxPercent: Int?,
    rangePercent: Double,
): Double {
    return (maxPercent?.toDouble() ?: (minPercent + rangePercent)).coerceAtLeast(minPercent + 0.5)
}

private fun resolveCrosshair(
    spec: ComposeBaseChartSpec,
    selectedSeriesId: String?,
    crosshairBasis: ComposeChartCrosshairBasis,
    rawX: Double,
    rawY: Double,
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
): CrosshairState? {
    val selectedSeries = spec.series.firstOrNull { it.id == selectedSeriesId } ?: spec.series.firstOrNull() ?: return null
    val visiblePoints = visibleSegmentPoints(selectedSeries.points, xMin, xMax, selectedSeries.lineStyle)
    val point = ChartCrosshairResolver.resolve(
        points = visiblePoints,
        lineStyle = selectedSeries.lineStyle,
        basis = crosshairBasis,
        rawX = rawX,
        rawY = rawY,
    ) ?: return null
    if (point.x !in xMin..xMax || point.y !in yMin..yMax) return null
    val xTicks = axisTicks(xMin, xMax, spec.xAxis)
    val yTicks = axisTicks(yMin, yMax, spec.yAxis)
    return CrosshairState(
        x = point.x,
        y = point.y,
        rawX = rawX,
        rawY = rawY,
        xLabel = spec.xAxis.valueFormatter(point.x, xTicks.step),
        yLabel = spec.yAxis.valueFormatter(point.y, yTicks.step),
    )
}

private fun visibleSegmentPoints(
    points: List<ComposeChartPoint>,
    xMin: Double,
    xMax: Double,
    lineStyle: ComposeChartLineStyle,
): List<ComposeChartPoint> {
    val sorted = points.sortedBy { it.x }
    if (sorted.isEmpty()) return emptyList()
    val result = mutableListOf<ComposeChartPoint>()
    pointOnSeriesByX(sorted, xMin, lineStyle)?.let(result::add)
    result.addAll(sorted.filter { it.x in xMin..xMax })
    pointOnSeriesByX(sorted, xMax, lineStyle)?.let(result::add)
    return result
        .sortedBy { it.x }
        .distinctBy { "%.8f".format(it.x) }
}

private fun patternEffect(
    patterns: List<ComposeChartLinePattern>,
    index: Int,
): androidx.compose.ui.graphics.PathEffect? {
    if (patterns.isEmpty()) return null
    val pattern = patterns.getOrNull(index % patterns.size) ?: return null
    return when (pattern) {
        ComposeChartLinePattern.Solid -> null
        ComposeChartLinePattern.Dashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 7f))
        ComposeChartLinePattern.Dotted -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 7f))
        ComposeChartLinePattern.DotDashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 6f, 2f, 6f))
    }
}

private fun seriesPath(
    points: List<ComposeChartPoint>,
    lineStyle: ComposeChartLineStyle,
    sx: (Double) -> Float,
    sy: (Double) -> Float,
): Path {
    if (
        lineStyle == ComposeChartLineStyle.Bezier ||
        lineStyle == ComposeChartLineStyle.Spline ||
        lineStyle == ComposeChartLineStyle.CatmullRom ||
        lineStyle == ComposeChartLineStyle.Monotone
    ) {
        return smoothPath(points, lineStyle, sx, sy)
    }
    return Path().apply {
        moveTo(sx(points.first().x), sy(points.first().y))
        when (lineStyle) {
            ComposeChartLineStyle.Linear -> {
                points.drop(1).forEach { lineTo(sx(it.x), sy(it.y)) }
            }
            ComposeChartLineStyle.SteppedFront -> {
                points.zipWithNext { current, next ->
                    lineTo(sx(next.x), sy(current.y))
                    lineTo(sx(next.x), sy(next.y))
                }
            }
            ComposeChartLineStyle.SteppedBack -> {
                points.zipWithNext { current, next ->
                    lineTo(sx(current.x), sy(next.y))
                    lineTo(sx(next.x), sy(next.y))
                }
            }
            ComposeChartLineStyle.Bezier,
            ComposeChartLineStyle.Spline,
            ComposeChartLineStyle.CatmullRom,
            ComposeChartLineStyle.Monotone -> Unit
        }
    }
}

private fun smoothPath(
    points: List<ComposeChartPoint>,
    lineStyle: ComposeChartLineStyle,
    sx: (Double) -> Float,
    sy: (Double) -> Float,
): Path {
    val sorted = points.sortedBy { it.x }
    val minX = sorted.first().x
    val maxX = sorted.last().x
    val samples = (sorted.size * 28).coerceIn(48, 280)
    return Path().apply {
        moveTo(sx(minX), sy(sorted.first().y))
        for (index in 1..samples) {
            val x = minX + (maxX - minX) * index / samples
            lineTo(sx(x), sy(interpolateSmoothY(sorted, x, lineStyle)))
        }
    }
}

private fun interpolateSmoothY(
    points: List<ComposeChartPoint>,
    x: Double,
    lineStyle: ComposeChartLineStyle,
): Double {
    if (points.size < 2) return points.firstOrNull()?.y ?: 0.0
    val nextIndex = points.indexOfFirst { it.x >= x }.let { if (it == -1) points.lastIndex else it }
    val currentIndex = (nextIndex - 1).coerceAtLeast(0)
    val current = points[currentIndex]
    val next = points[nextIndex.coerceAtLeast(1)]
    val span = (next.x - current.x).takeIf { abs(it) > 0.000001 } ?: return current.y
    val t = ((x - current.x) / span).coerceIn(0.0, 1.0)
    val y0 = points.getOrNull(currentIndex - 1)?.y ?: current.y
    val y1 = current.y
    val y2 = next.y
    val y3 = points.getOrNull(nextIndex + 1)?.y ?: next.y
    val value = when (lineStyle) {
        ComposeChartLineStyle.Bezier,
        ComposeChartLineStyle.Spline -> {
            val eased = t * t * (3.0 - 2.0 * t)
            y1 + (y2 - y1) * eased
        }
        ComposeChartLineStyle.CatmullRom -> {
            val t2 = t * t
            val t3 = t2 * t
            0.5 * (
                2.0 * y1 +
                    (-y0 + y2) * t +
                    (2.0 * y0 - 5.0 * y1 + 4.0 * y2 - y3) * t2 +
                    (-y0 + 3.0 * y1 - 3.0 * y2 + y3) * t3
                )
        }
        ComposeChartLineStyle.Monotone -> {
            val eased = t * t * (3.0 - 2.0 * t)
            y1 + (y2 - y1) * eased
        }
        ComposeChartLineStyle.Linear,
        ComposeChartLineStyle.SteppedFront,
        ComposeChartLineStyle.SteppedBack -> y1 + (y2 - y1) * t
    }
    return if (lineStyle == ComposeChartLineStyle.Monotone || lineStyle == ComposeChartLineStyle.CatmullRom) {
        value.coerceIn(minOf(y1, y2), maxOf(y1, y2))
    } else {
        value
    }
}

private fun pointOnSeriesByX(
    points: List<ComposeChartPoint>,
    x: Double,
    lineStyle: ComposeChartLineStyle,
): ComposeChartPoint? {
    val sorted = points.sortedBy { it.x }
    if (sorted.size < 2 || x < sorted.first().x || x > sorted.last().x) return null
    return ComposeChartPoint(
        x = x,
        y = interpolateSeriesY(sorted, x, lineStyle),
    )
}

private fun pointOnSeriesByY(
    points: List<ComposeChartPoint>,
    y: Double,
    lineStyle: ComposeChartLineStyle,
): ComposeChartPoint? {
    val sorted = points.sortedBy { it.x }
    if (sorted.size < 2) return null
    val samples = (sorted.size * 48).coerceIn(96, 600)
    val minX = sorted.first().x
    val maxX = sorted.last().x
    return (0..samples)
        .mapNotNull { index ->
            val x = minX + (maxX - minX) * index / samples
            pointOnSeriesByX(sorted, x, lineStyle)
        }
        .minByOrNull { abs(it.y - y) }
}

private fun interpolateSeriesY(
    points: List<ComposeChartPoint>,
    x: Double,
    lineStyle: ComposeChartLineStyle,
): Double {
    val nextIndex = points.indexOfFirst { it.x >= x }.let { if (it == -1) points.lastIndex else it }
    val currentIndex = (nextIndex - 1).coerceAtLeast(0)
    val current = points[currentIndex]
    val next = points[nextIndex.coerceAtLeast(1)]
    val span = (next.x - current.x).takeIf { abs(it) > 0.000001 } ?: return current.y
    val t = ((x - current.x) / span).coerceIn(0.0, 1.0)
    return when (lineStyle) {
        ComposeChartLineStyle.Linear -> current.y + (next.y - current.y) * t
        ComposeChartLineStyle.SteppedFront -> current.y
        ComposeChartLineStyle.SteppedBack -> next.y
        ComposeChartLineStyle.Bezier,
        ComposeChartLineStyle.Spline,
        ComposeChartLineStyle.CatmullRom,
        ComposeChartLineStyle.Monotone -> interpolateSmoothY(points, x, lineStyle)
    }
}

private fun ComposeChartSeries.toSeriesStyle(): ComposeChartSeriesStyle =
    ComposeChartSeriesStyle(
        color = color,
        lineStyle = lineStyle,
        linePattern = linePattern,
        pointShape = pointShape,
        pointFill = pointFill,
    )

private val chartSeriesColorPalette = listOf(
    Color(0xFF1976D2),
    Color(0xFF388E3C),
    Color(0xFFF57C00),
    Color(0xFFD32F2F),
    Color(0xFF7B1FA2),
    Color(0xFF00796B),
    Color(0xFFC2185B),
    Color(0xFF5D4037),
)

private fun chartRect(
    size: Size,
    spec: ComposeBaseChartSpec,
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
): Rect {
    val xTicks = axisTicks(xMin, xMax, spec.xAxis)
    val yTicks = axisTicks(yMin, yMax, spec.yAxis)
    val xHeight = xTicks.maxOfOrNull { textMeasurer.measure(spec.xAxis.valueFormatter(it, xTicks.step), textStyle).size.height } ?: 0
    val yWidth = yTicks.maxOfOrNull { textMeasurer.measure(spec.yAxis.valueFormatter(it, yTicks.step), textStyle).size.width } ?: 0
    val xBandHeight = spec.xBands
        .flatMap { listOf(it.min.coerceIn(xMin, xMax), it.max.coerceIn(xMin, xMax)) }
        .maxOfOrNull { textMeasurer.measure(spec.xAxis.valueFormatter(it, xTicks.step), textStyle).size.height }
        ?: 0
    val yBandWidth = spec.yBands
        .flatMap { listOf(it.min.coerceIn(yMin, yMax), it.max.coerceIn(yMin, yMax)) }
        .maxOfOrNull { textMeasurer.measure(spec.yAxis.valueFormatter(it, yTicks.step), textStyle).size.width }
        ?: 0
    val xUnitWidth = textMeasurer.measure(spec.xAxis.label, textStyle).size.width
    val yUnitHeight = textMeasurer.measure(spec.yAxis.label, textStyle).size.height
    val horizontalUnitReserve = maxOf(36f, xUnitWidth + 20f, yBandWidth + 12f)
    val verticalUnitReserve = maxOf(28f, yUnitHeight + 16f, xBandHeight + 12f)
    val leftPad = if (spec.yAxis.position == ComposeChartAxisSide.Start) yWidth + horizontalUnitReserve + 18f else horizontalUnitReserve
    val rightPad = if (spec.yAxis.position == ComposeChartAxisSide.End) yWidth + horizontalUnitReserve + 18f else horizontalUnitReserve
    val topPad = if (spec.xAxis.position == ComposeChartAxisSide.Top) xHeight + verticalUnitReserve + 16f else verticalUnitReserve
    val bottomPad = if (spec.xAxis.position == ComposeChartAxisSide.Bottom) xHeight + verticalUnitReserve + 16f else verticalUnitReserve
    return Rect(leftPad, topPad, size.width - rightPad, size.height - bottomPad)
}

private fun marqueeOffset(
    timeNanos: Long,
    cycleWidth: Float,
    viewportWidth: Float,
): Float {
    if (cycleWidth <= viewportWidth) return 0f
    val distance = cycleWidth + viewportWidth
    val offset = (timeNanos / 1_000_000_000.0 * 24.0 % distance).toFloat()
    return viewportWidth - offset
}

private fun clampWindowStart(
    value: Double,
    dataMin: Double,
    dataMax: Double,
    visibleRange: Double,
): Double {
    val maxStart = dataMax - visibleRange
    if (maxStart <= dataMin) return dataMin
    return value.coerceIn(dataMin, maxStart)
}

private fun percentInRange(
    value: Double,
    dataMin: Double,
    dataRange: Double,
): Double {
    if (dataRange <= 0.0 || !dataRange.isFiniteValue()) return 0.0
    return (((value - dataMin) / dataRange) * 100.0 * 2.0).roundToInt() / 2.0
}

private fun Double.isFiniteValue(): Boolean = !isNaN() && !isInfinite()

private fun ComposeChartSeriesState.toComposeStyle(): ComposeChartSeriesStyle =
    ComposeChartSeriesStyle(
        color = Color(colorArgb.toULong()),
        lineStyle = ComposeChartLineStyle.entries.firstOrNull { it.name == lineStyle } ?: ComposeChartLineStyle.Linear,
        linePattern = ComposeChartLinePattern.entries.firstOrNull { it.name == linePattern } ?: ComposeChartLinePattern.Solid,
        pointShape = ComposeChartPointShape.entries.firstOrNull { it.name == pointShape } ?: ComposeChartPointShape.Circle,
        pointFill = ComposeChartPointFill.entries.firstOrNull { it.name == pointFill } ?: ComposeChartPointFill.Filled,
    )

private fun String?.toAxisWindowModeOrNull(): ComposeChartAxisWindowMode? =
    ComposeChartAxisWindowMode.entries.firstOrNull { it.name == this }

private fun String?.toAxisSideOrNull(): ComposeChartAxisSide? =
    ComposeChartAxisSide.entries.firstOrNull { it.name == this }

private fun String?.toLinePatternOrNull(): ComposeChartLinePattern? =
    ComposeChartLinePattern.entries.firstOrNull { it.name == this }

private fun String?.toCrosshairBasisOrNull(): ComposeChartCrosshairBasis? =
    ComposeChartCrosshairBasis.entries.firstOrNull { it.name == this }

private fun String.toFullscreenAreaOrNull(): ComposeChartFullscreenArea? =
    ComposeChartFullscreenArea.entries.firstOrNull { it.name == this }

private fun Float.isFiniteValue(): Boolean = !isNaN() && !isInfinite()

@Composable
private fun ComposeChartLineStyle.styleLabel(): String = when (this) {
    ComposeChartLineStyle.Linear -> stringResource(R.string.compose_chart_line_linear)
    ComposeChartLineStyle.Bezier -> stringResource(R.string.compose_chart_line_bezier)
    ComposeChartLineStyle.Spline -> stringResource(R.string.compose_chart_line_spline)
    ComposeChartLineStyle.CatmullRom -> stringResource(R.string.compose_chart_line_catmull_rom)
    ComposeChartLineStyle.Monotone -> stringResource(R.string.compose_chart_line_monotone)
    ComposeChartLineStyle.SteppedFront -> stringResource(R.string.compose_chart_line_step_front)
    ComposeChartLineStyle.SteppedBack -> stringResource(R.string.compose_chart_line_step_back)
}

@Composable
private fun ComposeChartAxisSide.axisLabel(): String = when (this) {
    ComposeChartAxisSide.Start -> stringResource(R.string.compose_chart_axis_left)
    ComposeChartAxisSide.End -> stringResource(R.string.compose_chart_axis_right)
    ComposeChartAxisSide.Top -> stringResource(R.string.compose_chart_axis_top)
    ComposeChartAxisSide.Bottom -> stringResource(R.string.compose_chart_axis_bottom)
}

@Composable
private fun ComposeChartLinePattern.patternLabel(): String = when (this) {
    ComposeChartLinePattern.Solid -> stringResource(R.string.compose_chart_grid_solid)
    ComposeChartLinePattern.Dashed -> stringResource(R.string.compose_chart_grid_dashed)
    ComposeChartLinePattern.Dotted -> stringResource(R.string.compose_chart_grid_dotted)
    ComposeChartLinePattern.DotDashed -> stringResource(R.string.compose_chart_grid_dot_dashed)
}

@Composable
private fun ComposeChartPointShape.shapeLabel(): String = when (this) {
    ComposeChartPointShape.Circle -> stringResource(R.string.compose_chart_point_circle)
    ComposeChartPointShape.Triangle -> stringResource(R.string.compose_chart_point_triangle)
    ComposeChartPointShape.Square -> stringResource(R.string.compose_chart_point_square)
    ComposeChartPointShape.Diamond -> stringResource(R.string.compose_chart_point_diamond)
    ComposeChartPointShape.Cross -> stringResource(R.string.compose_chart_point_cross)
}

@Composable
private fun ComposeChartPointFill.fillLabel(): String = when (this) {
    ComposeChartPointFill.Filled -> stringResource(R.string.compose_chart_point_filled)
    ComposeChartPointFill.Hollow -> stringResource(R.string.compose_chart_point_hollow)
}

@Composable
private fun ComposeChartCrosshairBasis.basisLabel(): String = when (this) {
    ComposeChartCrosshairBasis.PerpendicularToXAxis -> stringResource(R.string.compose_chart_crosshair_basis_perpendicular_x)
    ComposeChartCrosshairBasis.PerpendicularToYAxis -> stringResource(R.string.compose_chart_crosshair_basis_perpendicular_y)
}

@Composable
private fun ComposeChartFullscreenArea.areaLabel(): String = when (this) {
    ComposeChartFullscreenArea.ChartSettings -> stringResource(R.string.compose_chart_fullscreen_area_settings)
    ComposeChartFullscreenArea.Title -> stringResource(R.string.compose_chart_fullscreen_area_title)
    ComposeChartFullscreenArea.ChartDrawing -> stringResource(R.string.compose_chart_fullscreen_area_chart_drawing)
    ComposeChartFullscreenArea.DataGroupLegend -> stringResource(R.string.compose_chart_fullscreen_area_data_legend)
}

@Composable
private fun ComposeChartAxisWindowMode.windowModeLabel(): String = when (this) {
    ComposeChartAxisWindowMode.PercentRange -> stringResource(R.string.compose_chart_axis_window_percent_range)
    ComposeChartAxisWindowMode.PercentBounds -> stringResource(R.string.compose_chart_axis_window_percent_bounds)
}

private fun DrawScope.drawHorizontalPanIndicator(
    dataMin: Double,
    dataMax: Double,
    windowStart: Double,
    visibleRange: Double,
    color: Color,
) {
    val range = (dataMax - dataMin).takeIf { it > 0.0 } ?: return
    val widthFraction = (visibleRange / range).coerceIn(0.05, 1.0).toFloat()
    val leftFraction = ((windowStart - dataMin) / range).coerceIn(0.0, 1.0).toFloat()
    drawRoundRect(
        color = color.copy(alpha = 0.42f),
        topLeft = Offset(size.width * leftFraction, size.height * 0.32f),
        size = Size(size.width * widthFraction, size.height * 0.36f),
    )
}

private fun DrawScope.drawVerticalPanIndicator(
    dataMin: Double,
    dataMax: Double,
    windowMin: Double,
    visibleRange: Double,
    color: Color,
) {
    val range = (dataMax - dataMin).takeIf { it > 0.0 } ?: return
    val heightFraction = (visibleRange / range).coerceIn(0.05, 1.0).toFloat()
    val topFraction = (1.0 - ((windowMin - dataMin + visibleRange) / range)).coerceIn(0.0, 1.0).toFloat()
    drawRoundRect(
        color = color.copy(alpha = 0.42f),
        topLeft = Offset(size.width * 0.32f, size.height * topFraction),
        size = Size(size.width * 0.36f, size.height * heightFraction),
    )
}

private const val TAG = "ComposeBaseChart"
private const val TICK_EPSILON = 0.0000001

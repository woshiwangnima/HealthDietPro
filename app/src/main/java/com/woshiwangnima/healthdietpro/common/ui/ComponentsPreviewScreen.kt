package com.woshiwangnima.healthdietpro.common.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeBaseChart
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeBaseChartSpec
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartAxisSide
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartAxisSpec
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartControls
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartLinePattern
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartLineStyle
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartPoint
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartPointFill
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartPointShape
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartRangeBand
import com.woshiwangnima.healthdietpro.common.ui.chart.ComposeChartSeries
import com.woshiwangnima.healthdietpro.common.ui.chart.ChartDataPoint
import com.woshiwangnima.healthdietpro.common.ui.chart.ChartDataSeries
import com.woshiwangnima.healthdietpro.common.ui.chart.ChartValueExpression
import com.woshiwangnima.healthdietpro.common.ui.chart.deriveChartDataSeries

@Composable
fun ComponentsPreviewScreen(
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        Log.d(TAG, "ComponentsPreviewScreen composed")
    }
    var showConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fontScale by AppFontScaleState.scale.collectAsState()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val alphaMid = rememberFontStyleAlphaMid()

    if (showConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.compose_confirm_dialog_title),
            message = stringResource(R.string.compose_confirm_dialog_message),
            confirmText = stringResource(R.string.compose_confirm_dialog_ok),
            cancelText = stringResource(R.string.compose_confirm_dialog_cancel),
            onConfirm = { showConfirm = false },
            onDismiss = { showConfirm = false },
        )
    }

    BaseScreen(
        title = stringResource(R.string.compose_components_preview_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.compose_font_scale_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FontScaleSlider(
                currentScale = fontScale,
                onScaleChangeStopped = { AppFontScaleState.update(context, it) },
            )
            TypographyPreview()

            HorizontalDivider()

            Text(
                text = stringResource(R.string.compose_confirm_dialog_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(onClick = { showConfirm = true }) {
                Text(stringResource(R.string.compose_show_confirm_dialog))
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.compose_text_overflow_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.compose_theme_preview_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.text_overflow_preview_sample),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.compose_theme_preview_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(alphaMid),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceVariant.copy(alpha = 0.3f))
                    .border(1.dp, onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                TextOverflowText(
                    text = stringResource(R.string.text_overflow_preview_sample),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.text_overflow_marquee_speed_desc),
                style = TextStyle(fontSize = FontTokens.caption),
                color = onSurfaceVariant,
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.compose_chart_section),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ComposeChartPreviewSamples()
        }
    }
}

@Composable
private fun TypographyPreview() {
    val styles = listOf(
        "Headline" to MaterialTheme.typography.headlineLarge,
        "Title" to MaterialTheme.typography.titleLarge,
        "Subtitle" to MaterialTheme.typography.titleSmall,
        "Body" to MaterialTheme.typography.bodyLarge,
        "Label" to MaterialTheme.typography.labelLarge,
        "Caption" to MaterialTheme.typography.bodySmall,
        "Micro" to MaterialTheme.typography.labelSmall,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        styles.forEach { (name, style) ->
            Text(text = "$name  AaBbCc 0123", style = style, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ComposeChartPreviewSamples() {
    val baseSeries = remember {
        listOf(
            ComposeChartSeries(
                id = "primary",
                label = "primary",
                color = Color(0xFF1976D2),
                lineStyle = ComposeChartLineStyle.Monotone,
                points = listOf(
                    ComposeChartPoint(0.0, 169.4),
                    ComposeChartPoint(1.0, 169.8),
                    ComposeChartPoint(2.0, 170.2),
                    ComposeChartPoint(3.0, 170.0),
                    ComposeChartPoint(4.0, 170.6),
                    ComposeChartPoint(5.0, 171.1),
                    ComposeChartPoint(6.0, 171.0),
                    ComposeChartPoint(7.0, 171.3),
                ),
            ),
            ComposeChartSeries(
                id = "secondary",
                label = "secondary",
                color = Color(0xFFE91E63),
                linePattern = ComposeChartLinePattern.Dashed,
                pointShape = ComposeChartPointShape.Diamond,
                pointFill = ComposeChartPointFill.Hollow,
                points = listOf(
                    ComposeChartPoint(0.0, 170.0),
                    ComposeChartPoint(7.0, 171.0),
                ),
            ),
        )
    }
    val xAxisLabel = stringResource(R.string.compose_chart_x_axis)
    val yAxisLabel = stringResource(R.string.compose_chart_y_axis)
    val previewControls = ComposeChartControls(
        defaultYMinPercent = -10,
        defaultYMaxPercent = 110,
    )

    ChartPreviewCard(title = stringResource(R.string.compose_chart_time_value_demo)) {
        ComposeBaseChart(
            spec = ComposeBaseChartSpec(
                title = stringResource(R.string.compose_chart_time_value_demo),
                series = baseSeries,
                xVisibleRange = 4.0,
                xAxis = ComposeChartAxisSpec(
                    label = xAxisLabel,
                    position = ComposeChartAxisSide.Bottom,
                    valueFormatter = { value, _ -> "D%.0f".format(value) },
                ),
                yAxis = ComposeChartAxisSpec(
                    label = yAxisLabel,
                    position = ComposeChartAxisSide.End,
                    valueFormatter = { value, _ -> "%.1f".format(value) },
                ),
                controls = previewControls,
            ),
        )
    }

    ChartPreviewCard(title = stringResource(R.string.compose_chart_numeric_xy_demo)) {
        ComposeBaseChart(
            spec = ComposeBaseChartSpec(
                title = stringResource(R.string.compose_chart_numeric_xy_demo),
                series = listOf(
                    ComposeChartSeries(
                        id = "numeric",
                        label = "numeric",
                        color = Color(0xFF00897B),
                        lineStyle = ComposeChartLineStyle.CatmullRom,
                        pointShape = ComposeChartPointShape.Square,
                        points = listOf(
                            ComposeChartPoint(-4.0, 16.0),
                            ComposeChartPoint(-2.0, 4.0),
                            ComposeChartPoint(0.0, 0.0),
                            ComposeChartPoint(1.5, 2.25),
                            ComposeChartPoint(3.0, 9.0),
                            ComposeChartPoint(4.5, 20.25),
                        ),
                    ),
                ),
                xVisibleRange = 6.0,
                xAxis = ComposeChartAxisSpec(
                    label = xAxisLabel,
                    position = ComposeChartAxisSide.Bottom,
                    valueFormatter = { value, _ -> "%.1f".format(value) },
                ),
                yAxis = ComposeChartAxisSpec(
                    label = yAxisLabel,
                    position = ComposeChartAxisSide.End,
                    valueFormatter = { value, _ -> "%.1f".format(value) },
                ),
                controls = previewControls,
            ),
        )
    }

    ChartPreviewCard(title = stringResource(R.string.compose_chart_category_demo)) {
        val categories = listOf("A", "B", "C", "D", "E", "F")
        ComposeBaseChart(
            spec = ComposeBaseChartSpec(
                title = stringResource(R.string.compose_chart_category_demo),
                series = listOf(
                    ComposeChartSeries(
                        id = "category",
                        label = "category",
                        color = Color(0xFF7B1FA2),
                        lineStyle = ComposeChartLineStyle.SteppedFront,
                        pointShape = ComposeChartPointShape.Triangle,
                        points = categories.mapIndexed { index, _ ->
                            ComposeChartPoint(index.toDouble(), listOf(2.0, 5.0, 3.0, 8.0, 6.0, 9.0)[index])
                        },
                    ),
                ),
                xVisibleRange = 4.0,
                xAxis = ComposeChartAxisSpec(
                    label = xAxisLabel,
                    position = ComposeChartAxisSide.Top,
                    valueFormatter = { value, _ -> categories.getOrNull(value.toInt()).orEmpty() },
                ),
                yAxis = ComposeChartAxisSpec(
                    label = yAxisLabel,
                    position = ComposeChartAxisSide.End,
                    valueFormatter = { value, _ -> "%.0f".format(value) },
                ),
                controls = previewControls,
            ),
        )
    }

    ChartPreviewCard(title = stringResource(R.string.compose_chart_percent_demo)) {
        ComposeBaseChart(
            spec = ComposeBaseChartSpec(
                title = stringResource(R.string.compose_chart_percent_demo),
                series = listOf(
                    ComposeChartSeries(
                        id = "percent",
                        label = "percent",
                        color = Color(0xFFF57C00),
                        lineStyle = ComposeChartLineStyle.Bezier,
                        pointFill = ComposeChartPointFill.Hollow,
                        points = listOf(
                            ComposeChartPoint(0.0, -12.0),
                            ComposeChartPoint(1.0, 8.0),
                            ComposeChartPoint(2.0, 24.0),
                            ComposeChartPoint(3.0, -5.0),
                            ComposeChartPoint(4.0, 36.0),
                            ComposeChartPoint(5.0, 52.0),
                        ),
                    ),
                ),
                yBands = listOf(
                    ComposeChartRangeBand(-20.0, 0.0, Color(0x33E53935)),
                    ComposeChartRangeBand(25.0, 60.0, Color(0x332196F3)),
                ),
                xVisibleRange = 4.0,
                xAxis = ComposeChartAxisSpec(
                    label = xAxisLabel,
                    position = ComposeChartAxisSide.Bottom,
                    valueFormatter = { value, _ -> "P%.0f".format(value) },
                ),
                yAxis = ComposeChartAxisSpec(
                    label = yAxisLabel,
                    position = ComposeChartAxisSide.Start,
                    valueFormatter = { value, _ -> "%.0f%%".format(value) },
                ),
                controls = previewControls,
            ),
        )
    }

    ChartPreviewCard(title = stringResource(R.string.compose_chart_axis_position_demo)) {
        ComposeBaseChart(
            spec = ComposeBaseChartSpec(
                title = stringResource(R.string.compose_chart_axis_position_demo),
                series = baseSeries,
                xVisibleRange = 3.0,
                yVisibleRange = 1.4,
                xAxis = ComposeChartAxisSpec(
                    label = xAxisLabel,
                    position = ComposeChartAxisSide.Top,
                    valueFormatter = { value, _ -> "W%.0f".format(value) },
                ),
                yAxis = ComposeChartAxisSpec(
                    label = yAxisLabel,
                    position = ComposeChartAxisSide.Start,
                    valueFormatter = { value, _ -> "%.1f".format(value) },
                ),
                controls = previewControls,
            ),
        )
    }

    ChartPreviewCard(title = stringResource(R.string.compose_chart_range_band_demo)) {
        ComposeBaseChart(
            spec = ComposeBaseChartSpec(
                title = stringResource(R.string.compose_chart_range_band_demo),
                series = baseSeries,
                xVisibleRange = 5.0,
                xBands = listOf(
                    ComposeChartRangeBand(1.0, 2.5, Color(0x332196F3)),
                    ComposeChartRangeBand(4.0, 5.5, Color(0x33E91E63)),
                ),
                yBands = listOf(
                    ComposeChartRangeBand(169.8, 170.6, Color(0x334CAF50)),
                    ComposeChartRangeBand(170.9, 171.4, Color(0x33FF9800)),
                ),
                xAxis = ComposeChartAxisSpec(
                    label = xAxisLabel,
                    position = ComposeChartAxisSide.Bottom,
                    gridLinePatterns = listOf(
                        ComposeChartLinePattern.Solid,
                        ComposeChartLinePattern.Dashed,
                        ComposeChartLinePattern.Dotted,
                        ComposeChartLinePattern.DotDashed,
                    ),
                    valueFormatter = { value, step ->
                        if (step >= 2.0) "%.0f".format(value) else "%.1f".format(value)
                    },
                ),
                yAxis = ComposeChartAxisSpec(
                    label = yAxisLabel,
                    position = ComposeChartAxisSide.Start,
                    gridLinePatterns = listOf(
                        ComposeChartLinePattern.Dashed,
                        ComposeChartLinePattern.Dotted,
                    ),
                    valueFormatter = { value, _ -> "%.1f".format(value) },
                ),
                controls = previewControls,
            ),
        )
    }

    ChartPreviewCard(title = stringResource(R.string.compose_chart_derived_demo)) {
        val intake = ChartDataSeries(
            id = "intake",
            label = "intake",
            points = listOf(
                ChartDataPoint(0, 1200f),
                ChartDataPoint(1, 1500f),
                ChartDataPoint(2, 1380f),
                ChartDataPoint(3, 1800f),
                ChartDataPoint(4, 1640f),
                ChartDataPoint(5, 1720f),
            ),
        )
        val target = ChartDataSeries(
            id = "target",
            label = "target",
            points = List(6) { index -> ChartDataPoint(index.toLong(), 2000f) },
        )
        val derived = deriveChartDataSeries(
            id = "completion",
            label = "completion",
            sources = listOf(intake, target),
            expression = ChartValueExpression.Percent(
                part = ChartValueExpression.Source("intake"),
                total = ChartValueExpression.Source("target"),
            ),
        )
        ComposeBaseChart(
            spec = ComposeBaseChartSpec(
                title = stringResource(R.string.compose_chart_derived_demo),
                series = listOf(
                    ComposeChartSeries(
                        id = derived.id,
                        label = derived.label,
                        color = Color(0xFF455A64),
                        lineStyle = ComposeChartLineStyle.Spline,
                        linePattern = ComposeChartLinePattern.DotDashed,
                        pointShape = ComposeChartPointShape.Cross,
                        points = derived.points.map { ComposeChartPoint(it.x.toDouble(), it.y.toDouble()) },
                    ),
                ),
                yBands = listOf(
                    ComposeChartRangeBand(60.0, 80.0, Color(0x332196F3)),
                    ComposeChartRangeBand(80.0, 100.0, Color(0x334CAF50)),
                ),
                xVisibleRange = 4.0,
                xAxis = ComposeChartAxisSpec(
                    label = xAxisLabel,
                    position = ComposeChartAxisSide.Bottom,
                    valueFormatter = { value, _ -> "D%.0f".format(value) },
                ),
                yAxis = ComposeChartAxisSpec(
                    label = yAxisLabel,
                    position = ComposeChartAxisSide.End,
                    valueFormatter = { value, _ -> "%.0f%%".format(value) },
                ),
                controls = previewControls,
            ),
        )
    }
}

@Composable
private fun ChartPreviewCard(
    title: String,
    content: @Composable () -> Unit,
) {
    LaunchedEffect(title) {
        Log.d(TAG, "Rendering chart preview card: $title")
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

private const val TAG = "ComponentsPreview"

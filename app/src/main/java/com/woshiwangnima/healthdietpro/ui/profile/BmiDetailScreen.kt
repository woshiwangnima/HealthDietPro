package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartCanvasStyle
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartControlLabels
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSeries
import com.woshiwangnima.healthdietpro.ui.profile.chart.ComposeChart
import com.woshiwangnima.healthdietpro.ui.profile.chart.ComposeChartSpec
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyle
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineType
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointFill
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointShape
import com.woshiwangnima.healthdietpro.ui.profile.chart.YAxisBand
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun BmiDetailScreen(
    bmiData: List<DataPoint>,
    initialTab: Int,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val tabs = remember {
        listOf(
            DetailTabItem("0", R.string.detail_tab_chart, R.drawable.ic_chart),
            DetailTabItem("1", R.string.detail_tab_data, R.drawable.ic_list),
        )
    }

    BaseScreen(
        title = stringResource(R.string.bmi_history_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> BmiChartPage(bmiData = bmiData)
                    1 -> BmiDataPage(bmiData = bmiData)
                }
            }
            DetailTabBar(
                items = tabs,
                selectedId = selectedTab.toString(),
                onSelected = { item ->
                    selectedTab = item.id.toInt()
                    onTabSelected(selectedTab)
                },
            )
        }
    }
}

@Composable
private fun BmiChartPage(
    bmiData: List<DataPoint>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BmiCardTitle(text = stringResource(R.string.bmi_chart_title))
                    BmiChartAndroidView(
                        bmiData = bmiData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(336.dp),
                    )
                }
            }
        }
        item { BmiReferenceCard() }
        item { BmiCalculatorCard() }
    }
}

@Composable
private fun BmiChartAndroidView(
    bmiData: List<DataPoint>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val title = stringResource(R.string.bmi_chart_title)
    val unitLabel = stringResource(R.string.bmi_unit_label)
    val seriesLabel = stringResource(R.string.bmi_title)
    val controlLabels = ChartControlLabels(
        lineStyle = stringResource(R.string.view_chart_line_style),
        xAxisRange = stringResource(R.string.view_chart_time_range),
        xAxisInterval = stringResource(R.string.view_chart_time_interval),
        yAxisBounds = stringResource(R.string.view_chart_bmi_bounds),
        fullscreen = stringResource(R.string.view_chart_fullscreen),
    )
    val color = remember(context) { ContextCompat.getColor(context, R.color.primary) }
    val bands = remember {
        BmiUtil.loadBmiBands().map { band ->
            YAxisBand(band.min.coerceAtLeast(0f), band.max, band.color)
        }
    }
    val series = remember(bmiData, seriesLabel, color) {
        ChartSeries(
            points = bmiData,
            label = seriesLabel,
            color = color,
            lineStyle = LineStyle.LINEAR,
            lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE,
            pointFill = PointFill.FILLED,
        )
    }

    ComposeChart(
        spec = ComposeChartSpec(
            title = title,
            chartStateKey = ProfilePrefs.makeChartStateKey(context, "bmi_history"),
            canvasStyle = ChartCanvasStyle(
                yAxisBands = bands,
                yValueFormatter = { value -> "%.1f".format(value) },
                xValueFormatter = ::formatProfileChartTimeAxis,
                crosshairValueFormatter = { value, label -> "%.1f %s".format(value, label) },
                crosshairTimeFormatter = { timestamp ->
                    formatProfileChartTimeAxis(timestamp, 60_000L)
                },
            ),
            controlLabels = controlLabels,
            series = listOf(series),
            unitLabel = unitLabel,
            titleGravity = android.view.Gravity.START,
            titleVisible = false,
        ),
        modifier = modifier,
    )
}

private fun formatProfileChartTimeAxis(
    timestamp: Long,
    intervalMs: Long,
): String {
    val pattern = when {
        intervalMs < 60_000L -> "HH:mm:ss"
        intervalMs < 3_600_000L -> "HH:mm"
        intervalMs < 86_400_000L -> "MM-dd HH:mm"
        intervalMs < 604_800_000L -> "MM-dd"
        intervalMs < 31L * 86_400_000L -> "MM-dd"
        else -> "yyyy-MM"
    }
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.systemDefault(),
    ).format(DateTimeFormatter.ofPattern(pattern))
}

@Composable
private fun BmiReferenceCard() {
    val bands = remember { BmiUtil.loadBmiBands() }
    val labels = listOf(
        stringResource(R.string.bmi_underweight),
        stringResource(R.string.bmi_normal),
        stringResource(R.string.bmi_overweight),
        stringResource(R.string.bmi_obese),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BmiCardTitle(text = stringResource(R.string.bmi_reference_title))
            Spacer(Modifier.height(12.dp))
            BmiReferenceRow(
                left = stringResource(R.string.bmi_reference_range),
                right = stringResource(R.string.bmi_reference_category),
                header = true,
            )
            bands.forEachIndexed { index, band ->
                BmiReferenceRow(
                    left = band.rangeText(),
                    right = labels.getOrNull(index).orEmpty(),
                    background = Color(band.color).copy(alpha = 0.16f),
                )
            }
        }
    }
}

@Composable
private fun BmiReferenceRow(
    left: String,
    right: String,
    header: Boolean = false,
    background: Color = Color.Transparent,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
        Text(
            text = left,
            style = style,
            fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = right,
            style = style,
            fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BmiCalculatorCard() {
    val labels = listOf(
        stringResource(R.string.bmi_underweight),
        stringResource(R.string.bmi_normal),
        stringResource(R.string.bmi_overweight),
        stringResource(R.string.bmi_obese),
    )
    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    val height = heightText.toFloatOrNull()
    val weight = weightText.toFloatOrNull()
    val bmi = if (height != null && weight != null && height > 0f && weight > 0f) {
        BmiUtil.computeBmi(weight, height)
    } else {
        0f
    }
    val resultLabel = if (bmi > 0f) {
        stringResource(R.string.bmi_result_colored_value, bmi, bmiLabel(bmi, labels))
    } else {
        ""
    }
    val resultColor = if (bmi > 0f) {
        Color(BmiUtil.getBmiColor(bmi)).copy(alpha = 1f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BmiCardTitle(text = stringResource(R.string.bmi_calculator_title))
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.bmi_height_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.bmi_weight_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            if (bmi > 0f) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.bmi_result_prefix),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = resultLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = resultColor,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.bmi_result_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BmiCardTitle(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BmiDataPage(
    bmiData: List<DataPoint>,
) {
    val labels = listOf(
        stringResource(R.string.bmi_underweight),
        stringResource(R.string.bmi_normal),
        stringResource(R.string.bmi_overweight),
        stringResource(R.string.bmi_obese),
    )
    val rows = remember(bmiData) {
        bmiData.sortedByDescending { it.timestamp }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.bmi_record_count, rows.size),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.bmi_no_records),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppDataTable(
            rows = rows,
            rowKey = { index, point -> "${point.timestamp}_$index" },
            columns = listOf(
                AppDataTableColumn<DataPoint>(
                    key = "date",
                    header = { AppDataTableHeaderText(stringResource(R.string.body_record_date)) },
                    width = ColumnWidth.Fixed(120.dp),
                ) { point ->
                    AppDataTableText(point.dateLabel)
                },
                AppDataTableColumn<DataPoint>(
                    key = "bmi",
                    header = { AppDataTableHeaderText(stringResource(R.string.bmi_title)) },
                    width = ColumnWidth.Fixed(112.dp),
                ) { point ->
                    AppDataTableText("%.1f".format(point.value))
                },
                AppDataTableColumn<DataPoint>(
                    key = "category",
                    header = { AppDataTableHeaderText(stringResource(R.string.bmi_reference_category)) },
                    width = ColumnWidth.Flex(weight = 1f, min = 120.dp),
                ) { point ->
                    AppDataTableText(bmiLabel(point.value, labels))
                },
            ),
        )
    }
}

private fun BmiUtil.BmiBand.rangeText(): String = when {
    min < 0f -> "< ${max.formatBmiBoundary()}"
    max == Float.MAX_VALUE -> ">= ${min.formatBmiBoundary()}"
    else -> "${min.formatBmiBoundary()} - ${max.formatBmiBoundary()}"
}

private fun Float.formatBmiBoundary(): String {
    return if (this % 1f == 0f) "%.0f".format(this) else "%.1f".format(this)
}

private fun bmiLabel(
    bmi: Float,
    labels: List<String>,
): String {
    val bands = BmiUtil.loadBmiBands()
    val index = bands.indexOfFirst { band ->
        (band.min < 0f || bmi >= band.min) && (band.max == Float.MAX_VALUE || bmi < band.max)
    }
    return labels.getOrNull(index).orEmpty()
}

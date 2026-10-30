package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppInfoDialog
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartEvent
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppColorTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.model.chart.ComposeChartState
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartAxisKind
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
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * BMI 历史：与【食物 / 食材 / 菜肴详情】【记物价】同一套语法——页面只留 `bg`，
 * 内容全部装 `AppSectionCard`（`panelRaised` 底 + `radii.container` + `radii.stroke` 描边），
 * 卡内可点项用「行 + 发丝分隔线」，尺寸 / 颜色 / 字号全部走令牌。
 *
 * 分级用语义色而不是资产里的 `#26xxxxxx`：那几个色自带 15% 透明度，既拿来做文字色会掉到
 * 2:1 以下（§6 对比度），也不随主题切换。这里改用 tokens 的 info / success / warning / danger，
 * 底衬统一 14% 透明度，深浅两套主题都对得上。
 */
@Composable
internal fun BmiDetailScreen(
    viewModel: BmiDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chartState by viewModel.chartState.collectAsStateWithLifecycle()
    val tabs = remember {
        listOf(
            DetailTabItem("0", R.string.detail_tab_chart, R.drawable.ic_chart),
            DetailTabItem("1", R.string.detail_tab_data, R.drawable.ic_list),
        )
    }

    AppTheme(darkTheme = rememberAppDarkTheme()) {
        val tokens = LocalAppTokens.current
        BaseScreen(
            title = stringResource(R.string.bmi_history_title),
            onBack = onBack,
            includeNavigationBarPadding = false,
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(tokens.colors.bg)
                    .padding(padding),
            ) {
                Box(Modifier.weight(1f)) {
                    AnimatedPageContent(
                        targetState = uiState.selectedTab,
                        direction = { from, to -> if (to > from) 1 else -1 },
                    ) { tab ->
                        when (tab) {
                            0 -> BmiChartPage(
                                bmiData = uiState.bmiData,
                                chartState = chartState,
                                chartStateKey = viewModel.chartStateKey,
                                onChartStateChanged = { viewModel.onChartEvent(BaseChartEvent.StateChanged(it)) },
                            )

                            else -> BmiDataPage(bmiData = uiState.bmiData)
                        }
                    }
                }
                DetailTabBar(
                    items = tabs,
                    selectedId = uiState.selectedTab.toString(),
                    onSelected = { item ->
                        viewModel.onEvent(BmiDetailEvent.TabSelected(item.id.toInt()))
                    },
                )
            }
        }
    }
}

@Composable
private fun BmiChartPage(
    bmiData: List<BmiUtil.BmiDataPoint>,
    chartState: ComposeChartState?,
    chartStateKey: String,
    onChartStateChanged: (ComposeChartState) -> Unit,
) {
    val spacing = LocalAppTokens.current.spacing
    // 先换算成图表点再 memo：直接写 map{} 每次重组都会产出新 List，remember 失效、曲线每帧重建。
    val chartPoints = remember(bmiData) { bmiData.map { it.toChartPoint() } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.page,
            end = spacing.page,
            top = spacing.s3,
            bottom = spacing.s4,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        if (bmiData.isEmpty()) {
            item { BmiEmptyCard() }
        } else {
            item { BmiLatestCard(latest = bmiData.last(), previous = bmiData.getOrNull(bmiData.size - 2)) }
            item {
                BmiChartCard(
                    bmiData = chartPoints,
                    chartState = chartState,
                    chartStateKey = chartStateKey,
                    onChartStateChanged = onChartStateChanged,
                )
            }
        }
        item { BmiReferenceCard() }
        item { BmiCalculatorCard() }
    }
}

/** 最新一次 BMI：大数字 + 分级 + 测量时间与来源数据，给图表一个可读的锚点。 */
@Composable
private fun BmiLatestCard(
    latest: BmiUtil.BmiDataPoint,
    previous: BmiUtil.BmiDataPoint?,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val locale = LocalContext.current.resources.configuration.locales[0].toLanguageTag()
    val labels = bmiLabels()
    val bandIndex = bmiBandIndex(latest.bmi)
    val bandColor = bmiBandColor(bandIndex, colors)

    AppSectionCard(
        iconRes = R.drawable.ic_health_metrics,
        title = stringResource(R.string.bmi_latest_section),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "%.1f".format(latest.bmi),
                style = tokens.type.titleL.copy(fontFamily = FontFamily.Monospace),
                color = bandColor,
            )
            Spacer(Modifier.width(spacing.s2))
            Text(
                text = stringResource(R.string.bmi_unit_label),
                style = tokens.type.caption,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = spacing.s1),
            )
            Spacer(Modifier.weight(1f))
            BmiCategoryChip(
                label = labels.getOrNull(bandIndex).orEmpty(),
                color = bandColor,
                modifier = Modifier.padding(bottom = spacing.s1),
            )
        }
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = latest.dateLabel,
            style = tokens.type.caption,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = stringResource(
                R.string.bmi_latest_source,
                formatMetric(latest.heightCm, latest.heightUnitId, locale),
                formatMetric(latest.weightKg, latest.weightUnitId, locale),
            ),
            style = tokens.type.caption,
            color = colors.textMuted,
        )
        if (previous != null) {
            val delta = latest.bmi - previous.bmi
            if (kotlin.math.abs(delta) >= 0.05f) {
                Spacer(Modifier.height(spacing.s1))
                Text(
                    text = stringResource(
                        R.string.bmi_latest_change,
                        "%s%.1f".format(if (delta > 0f) "+" else "-", kotlin.math.abs(delta)),
                    ),
                    style = tokens.type.caption,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun BmiChartCard(
    bmiData: List<DataPoint>,
    chartState: ComposeChartState?,
    chartStateKey: String,
    onChartStateChanged: (ComposeChartState) -> Unit,
) {
    val tokens = LocalAppTokens.current
    AppSectionCard(
        iconRes = R.drawable.ic_chart,
        title = stringResource(R.string.bmi_chart_section),
        // 图表自身是 `surface` 色的 Surface：卡底用同一个 `panel`，两者才接得上，
        // 否则 panelRaised 里嵌一块更暗的面板，会读成「卡片里凹进去一块」。
        containerColor = tokens.colors.panel,
        contentPadding = PaddingValues(0.dp),
    ) {
        BmiChart(
            bmiData = bmiData,
            chartState = chartState,
            chartStateKey = chartStateKey,
            onChartStateChanged = onChartStateChanged,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BmiChart(
    bmiData: List<DataPoint>,
    chartState: ComposeChartState?,
    chartStateKey: String,
    onChartStateChanged: (ComposeChartState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val title = stringResource(R.string.bmi_chart_title)
    val timeUnitLabel = stringResource(R.string.chart_axis_time_unit)
    val unitLabel = stringResource(R.string.bmi_unit_label)
    val seriesLabel = stringResource(R.string.bmi_title)
    val controlLabels = ChartControlLabels(
        lineStyle = stringResource(R.string.view_chart_line_style),
        xAxisRange = stringResource(R.string.view_chart_time_range),
        xAxisInterval = stringResource(R.string.view_chart_time_interval),
        yAxisBounds = stringResource(R.string.view_chart_bmi_bounds),
        fullscreen = stringResource(R.string.view_chart_fullscreen),
    )
    val lineColor = tokens.colors.primary.toArgb()
    val bands = remember(tokens) {
        BmiUtil.loadBmiBands().mapIndexed { index, band ->
            YAxisBand(
                minValue = band.min.coerceAtLeast(0f),
                maxValue = band.max,
                color = bmiBandColor(index, tokens.colors).copy(alpha = 0.14f).toArgb(),
            )
        }
    }
    val series = remember(bmiData, seriesLabel, lineColor) {
        ChartSeries(
            points = bmiData,
            label = seriesLabel,
            color = lineColor,
            lineStyle = LineStyle.LINEAR,
            lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE,
            pointFill = PointFill.FILLED,
        )
    }

    ComposeChart(
        spec = ComposeChartSpec(
            title = title,
            chartStateKey = chartStateKey,
            canvasStyle = ChartCanvasStyle(
                xAxisKind = ChartAxisKind.TimestampMs,
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
            xAxisLabel = timeUnitLabel,
            yAxisLabel = unitLabel,
            titleGravity = android.view.Gravity.START,
            titleVisible = false,
        ),
        chartState = chartState,
        onChartStateChanged = onChartStateChanged,
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

/** 分级对照：范围 + 分类，色点与曲线里的色带同源（都是 tokens 语义色）。 */
@Composable
private fun BmiReferenceCard() {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val bands = remember { BmiUtil.loadBmiBands() }
    val labels = bmiLabels()

    AppSectionCard(
        iconRes = R.drawable.ic_vs,
        title = stringResource(R.string.bmi_reference_section),
        contentPadding = PaddingValues(bottom = spacing.s2),
    ) {
        BmiReferenceHeader(
            left = stringResource(R.string.bmi_reference_range),
            right = stringResource(R.string.bmi_reference_category),
        )
        bands.forEachIndexed { index, band ->
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = spacing.s3),
                thickness = tokens.radii.highlight,
                color = colors.border,
            )
            BmiReferenceRow(
                range = band.rangeText(),
                label = labels.getOrNull(index).orEmpty(),
                color = bmiBandColor(index, colors),
            )
        }
        Text(
            text = stringResource(R.string.bmi_reference_note),
            style = tokens.type.caption,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = spacing.s3, vertical = spacing.s1),
        )
    }
}

@Composable
private fun BmiReferenceHeader(left: String, right: String) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s3, vertical = spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = left,
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = right,
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun BmiReferenceRow(range: String, label: String, color: Color) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.hitMin)
            .padding(horizontal = spacing.s3, vertical = spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = range,
            style = tokens.type.bodyS.copy(fontFamily = FontFamily.Monospace),
            color = tokens.colors.text,
            modifier = Modifier.weight(1f),
        )
        BmiCategoryChip(label = label, color = color)
    }
}

@Composable
private fun BmiCalculatorCard() {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var heightText by rememberSaveable { mutableStateOf("") }
    var weightText by rememberSaveable { mutableStateOf("") }
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val labels = bmiLabels()

    val height = heightText.trim().takeIf(String::isNotBlank)?.toFloatOrNull()
    val weight = weightText.trim().takeIf(String::isNotBlank)?.toFloatOrNull()
    val bmi = if (height != null && weight != null && height > 0f && weight > 0f) {
        BmiUtil.computeBmi(weight, height)
    } else {
        0f
    }
    val bandColor = if (bmi > 0f) bmiBandColor(bmiBandIndex(bmi), colors) else colors.textMuted

    AppSectionCard(
        iconRes = R.drawable.ic_edit,
        title = stringResource(R.string.bmi_calculator_title),
        action = { HelpIconButton(onClick = { showHelp = true }) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            verticalAlignment = Alignment.Top,
        ) {
            EditorTextField(
                label = stringResource(R.string.bmi_height_label),
                value = heightText,
                onValueChange = { heightText = it },
                required = false,
                showRequirementMarker = false,
                numeric = true,
                modifier = Modifier.weight(1f),
            )
            EditorTextField(
                label = stringResource(R.string.bmi_weight_label),
                value = weightText,
                onValueChange = { weightText = it },
                required = false,
                showRequirementMarker = false,
                numeric = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(spacing.s3))
        if (bmi > 0f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.bmi_title),
                    style = tokens.type.caption,
                    color = colors.textMuted,
                )
                Spacer(Modifier.width(spacing.s2))
                Text(
                    text = "%.1f".format(bmi),
                    style = tokens.type.titleS.copy(fontFamily = FontFamily.Monospace),
                    color = bandColor,
                )
                Spacer(Modifier.width(spacing.s2))
                BmiCategoryChip(label = labels.getOrNull(bmiBandIndex(bmi)).orEmpty(), color = bandColor)
            }
        } else {
            Text(
                text = stringResource(R.string.bmi_calculator_hint),
                style = tokens.type.bodyS,
                color = colors.textMuted,
            )
        }
    }

    if (showHelp) {
        AppInfoDialog(stringResource(R.string.bmi_calculator_title), { showHelp = false }) {
            Text(
                text = stringResource(R.string.bmi_calculator_help),
                style = LocalAppTokens.current.type.bodyS,
                color = LocalAppTokens.current.colors.text,
            )
        }
    }
}

/** 标题侧问号：视觉 20 dp，外层补到 48 dp 热区（§4.6-3）。 */
@Composable
private fun HelpIconButton(onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    Box(
        modifier = Modifier
            .size(tokens.spacing.hitMin)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_help),
            contentDescription = stringResource(R.string.bmi_calculator_title),
            modifier = Modifier.size(tokens.spacing.s5),
            tint = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun BmiEmptyCard() {
    AppSectionCard(
        iconRes = R.drawable.ic_chart,
        title = stringResource(R.string.bmi_chart_section),
    ) {
        BmiEmptyMessage()
    }
}

/** 空态：说明「为什么空、下一步做什么」，不留一片空白（§6.2）。 */
@Composable
private fun BmiEmptyMessage() {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.s4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_health_metrics),
            contentDescription = null,
            modifier = Modifier.size(spacing.s6),
            tint = tokens.colors.disabled,
        )
        Text(
            text = stringResource(R.string.bmi_no_records),
            style = tokens.type.body,
            color = tokens.colors.text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.bmi_empty_hint),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BmiDataPage(bmiData: List<BmiUtil.BmiDataPoint>) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0].toLanguageTag()
    val labels = bmiLabels()
    val rows = remember(bmiData) { bmiData.sortedByDescending { it.timestamp } }
    // 条数做标题栏右侧注记；空表不挂「共 0 条」，避免一屏两个「空」的说法。
    val countLabel: (@Composable () -> Unit)? = if (rows.isEmpty()) {
        null
    } else {
        {
            Text(
                text = stringResource(R.string.bmi_record_count, rows.size),
                style = tokens.type.caption,
                color = colors.textMuted,
                modifier = Modifier.padding(end = spacing.s2),
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.page,
            end = spacing.page,
            top = spacing.s3,
            bottom = spacing.s4,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        item {
            AppSectionCard(
                iconRes = R.drawable.ic_list,
                title = stringResource(R.string.bmi_records_section),
                action = countLabel,
                contentPadding = PaddingValues(0.dp),
            ) {
                if (rows.isEmpty()) {
                    BmiEmptyMessage()
                } else {
                    // 表格在 LazyColumn 的 item 里，必须给 maxBodyHeight：否则表体是无限高约束，
                    // 内层可滚动列表会直接崩（§Q-6）。这里取 480 dp，行数少时按行数自适应。
                    AppDataTable(
                        rows = rows,
                        rowKey = { index, point -> "${point.timestamp}_$index" },
                        maxBodyHeight = 480.dp,
                        // 渐隐底色必须跟卡片一致，默认取 surface（panel），会在卡内留下一条暗带。
                        fadeColor = colors.panelRaised,
                        columns = listOf(
                            AppDataTableColumn<BmiUtil.BmiDataPoint>(
                                key = "date",
                                header = { AppDataTableHeaderText(stringResource(R.string.body_record_table_time)) },
                                width = ColumnWidth.Fixed(128.dp),
                            ) { point ->
                                AppDataTableText(formatBmiRecordDate(point.timestamp), numeric = true)
                            },
                            AppDataTableColumn<BmiUtil.BmiDataPoint>(
                                key = "bmi",
                                header = { AppDataTableHeaderText(stringResource(R.string.bmi_title)) },
                                width = ColumnWidth.Fixed(72.dp),
                            ) { point ->
                                Text(
                                    text = "%.1f".format(point.bmi),
                                    style = tokens.type.numeric,
                                    color = bmiBandColor(bmiBandIndex(point.bmi), colors),
                                    maxLines = 1,
                                )
                            },
                            AppDataTableColumn<BmiUtil.BmiDataPoint>(
                                key = "height",
                                header = { AppDataTableHeaderText(stringResource(R.string.bmi_table_height)) },
                                width = ColumnWidth.Fixed(84.dp),
                            ) { point ->
                                AppDataTableText(
                                    text = formatMetric(point.heightCm, point.heightUnitId, locale),
                                    numeric = true,
                                )
                            },
                            AppDataTableColumn<BmiUtil.BmiDataPoint>(
                                key = "weight",
                                header = { AppDataTableHeaderText(stringResource(R.string.bmi_table_weight)) },
                                width = ColumnWidth.Fixed(84.dp),
                            ) { point ->
                                AppDataTableText(
                                    text = formatMetric(point.weightKg, point.weightUnitId, locale),
                                    numeric = true,
                                )
                            },
                            AppDataTableColumn<BmiUtil.BmiDataPoint>(
                                key = "category",
                                header = { AppDataTableHeaderText(stringResource(R.string.bmi_reference_category)) },
                                width = ColumnWidth.Flex(weight = 1f, min = 96.dp),
                            ) { point ->
                                BmiCategoryChip(
                                    label = labels.getOrNull(bmiBandIndex(point.bmi)).orEmpty(),
                                    color = bmiBandColor(bmiBandIndex(point.bmi), colors),
                                )
                            },
                        ),
                    )
                }
            }
        }
    }
}

private fun formatMetric(baseValue: Float, unitId: String, locale: String): String =
    UnitConverter.formatWithUnit(
        category = if (unitId in LENGTH_UNIT_IDS) UnitCategoryType.Length.id else UnitCategoryType.Weight.id,
        baseValue = baseValue,
        unitId = unitId,
        locale = locale,
        precision = if (unitId in LENGTH_UNIT_IDS) 1 else 2,
    )

private val LENGTH_UNIT_IDS = setOf("cm", "m", "ft", "in")

/** 分级标签：颜色取 tokens 语义色，底衬统一 14%，文字用实色保证对比度（§6）。 */
@Composable
private fun BmiCategoryChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    if (label.isBlank()) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radii.label),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(tokens.radii.highlight, color.copy(alpha = 0.45f)),
    ) {
        Text(
            text = label,
            style = tokens.type.caption,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = tokens.spacing.s2, vertical = 2.dp),
        )
    }
}

@Composable
private fun bmiLabels(): List<String> = listOf(
    stringResource(R.string.bmi_underweight),
    stringResource(R.string.bmi_normal),
    stringResource(R.string.bmi_overweight),
    stringResource(R.string.bmi_obese),
)

/** 与 `BmiUtil.loadBmiBands()` 的下标一一对应：0 偏低 / 1 正常 / 2 超重 / 3 肥胖。 */
private fun bmiBandColor(index: Int, colors: AppColorTokens): Color = when (index) {
    0 -> colors.info
    1 -> colors.success
    2 -> colors.warning
    else -> colors.danger
}

private fun bmiBandIndex(bmi: Float): Int {
    val bands = BmiUtil.loadBmiBands()
    val index = bands.indexOfFirst { band ->
        (band.min < 0f || bmi >= band.min) && (band.max == Float.MAX_VALUE || bmi < band.max)
    }
    return if (index >= 0) index else 1
}

/**
 * 表格里的时间列只到日：BMI 是按「当天量了什么」记录的，时分秒既不参与计算，
 * 放进来还会把列宽撑到需要横向滚动（16 字符在 16 sp 下约 154 dp）。
 */
private fun formatBmiRecordDate(timestamp: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

private fun BmiUtil.BmiBand.rangeText(): String = when {
    min < 0f -> "< ${max.formatBmiBoundary()}"
    max == Float.MAX_VALUE -> "≥ ${min.formatBmiBoundary()}"
    else -> "${min.formatBmiBoundary()} – ${max.formatBmiBoundary()}"
}

private fun Float.formatBmiBoundary(): String =
    if (this % 1f == 0f) "%.0f".format(this) else "%.1f".format(this)

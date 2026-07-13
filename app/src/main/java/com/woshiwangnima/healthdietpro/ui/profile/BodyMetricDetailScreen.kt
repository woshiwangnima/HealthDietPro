package com.woshiwangnima.healthdietpro.ui.profile

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.formatDateTime
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartEvent
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartViewModel
import com.woshiwangnima.healthdietpro.model.chart.ComposeChartState
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.bodyRecordEpochMillis
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDateTime
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDisplayDateTime
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
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun BodyMetricDetailScreen(
    title: String,
    isHeight: Boolean,
    unitId: String,
    category: String,
    records: List<BodyRecord>,
    initialTab: Int,
    chartViewModel: BaseChartViewModel,
    onTabSelected: (Int) -> Unit,
    onRecordsChanged: (List<BodyRecord>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val chartState by chartViewModel.chartState.collectAsStateWithLifecycle()
    val tabs = remember {
        listOf(
            DetailTabItem("0", R.string.detail_tab_chart, R.drawable.ic_chart),
            DetailTabItem("1", R.string.detail_tab_data, R.drawable.ic_list),
        )
    }
    BaseScreen(title = title, onBack = onBack, includeNavigationBarPadding = false) { padding ->
        Column(
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> BodyMetricChart(
                        title = title,
                        unitId = unitId,
                        category = category,
                        records = records,
                        chartState = chartState,
                        chartStateKey = chartViewModel.chartStateKey,
                        onChartStateChanged = { chartViewModel.onChartEvent(BaseChartEvent.StateChanged(it)) },
                    )
                    1 -> BodyMetricDataPage(
                        records = records,
                        isHeight = isHeight,
                        unitId = unitId,
                        category = category,
                        onRecordsChanged = onRecordsChanged,
                    )
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
private fun BodyMetricChart(
    title: String,
    unitId: String,
    category: String,
    records: List<BodyRecord>,
    chartState: ComposeChartState?,
    chartStateKey: String,
    onChartStateChanged: (ComposeChartState) -> Unit,
) {
    val context = LocalContext.current
    val seriesLabel = stringResource(R.string.body_record_value)
    val controlLabels = ChartControlLabels(
        lineStyle = stringResource(R.string.view_chart_line_style),
        xAxisRange = stringResource(R.string.view_chart_time_range),
        xAxisInterval = stringResource(R.string.view_chart_time_interval),
        yAxisBounds = stringResource(R.string.view_chart_bmi_bounds),
        fullscreen = stringResource(R.string.view_chart_fullscreen),
    )
    val data = remember(records, category, unitId) {
        records.sortedBy { bodyRecordEpochMillis(it.date) }.map { record ->
            DataPoint(
                timestamp = bodyRecordEpochMillis(record.date),
                value = UnitConverter.fromBase(category, record.value, unitId),
                dateLabel = formatBodyRecordDisplayDateTime(record.date),
            )
        }
    }
    val series = remember(data, seriesLabel, context) {
        ChartSeries(
            points = data,
            label = seriesLabel,
            color = ContextCompat.getColor(context, R.color.primary),
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
                yValueFormatter = { "%.1f".format(it) },
                xValueFormatter = ::formatBodyMetricChartTimeAxis,
                crosshairValueFormatter = { value, label -> "%.1f %s".format(value, label) },
                crosshairTimeFormatter = { timestamp -> formatBodyMetricChartTimeAxis(timestamp, 60_000L) },
            ),
            controlLabels = controlLabels,
            series = listOf(series),
            xAxisLabel = stringResource(R.string.chart_axis_time_unit),
            yAxisLabel = unitId,
            titleVisible = false,
        ),
        chartState = chartState,
        onChartStateChanged = onChartStateChanged,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun BodyMetricDataPage(
    records: List<BodyRecord>,
    isHeight: Boolean,
    unitId: String,
    category: String,
    onRecordsChanged: (List<BodyRecord>) -> Unit,
) {
    val context = LocalContext.current
    var editPosition by remember { mutableStateOf<Int?>(null) }
    var deletePosition by remember { mutableStateOf<Int?>(null) }
    val rows = remember(records) { records.sortedByDescending { bodyRecordEpochMillis(it.date) } }
    BodyMetricRecordTable(
        rows = rows,
        unitId = unitId,
        category = category,
        onAdd = { editPosition = -1 },
        onEdit = { record -> editPosition = records.indexOf(record).takeIf { it >= 0 } },
        onDelete = { record -> deletePosition = records.indexOf(record).takeIf { it >= 0 } },
    )
    deletePosition?.let { position ->
        AlertDialog(
            onDismissRequest = { deletePosition = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.body_record_delete_confirm_message)) },
            confirmButton = {
                AppIconTextButton(
                    text = stringResource(R.string.body_record_delete),
                    iconRes = R.drawable.ic_delete,
                    onClick = {
                        onRecordsChanged(records.filterIndexed { index, _ -> index != position })
                        deletePosition = null
                    },
                )
            },
            dismissButton = {
                AppOutlinedIconTextButton(
                    text = stringResource(R.string.body_record_cancel),
                    iconRes = R.drawable.ic_cancel,
                    onClick = { deletePosition = null },
                )
            },
        )
    }
    editPosition?.let { position ->
        BodyMetricRecordEditSheet(
            editing = records.getOrNull(position),
            isHeight = isHeight,
            unitId = unitId,
            category = category,
            onDismiss = { editPosition = null },
            onSave = { date, valueText ->
                val value = valueText.toFloatOrNull()
                when {
                    valueText.isBlank() -> Toast.makeText(context, R.string.body_record_value_required, Toast.LENGTH_SHORT).show()
                    value == null || value <= 0f -> Toast.makeText(context, R.string.body_record_value_invalid, Toast.LENGTH_SHORT).show()
                    else -> {
                        val record = BodyRecord(date, UnitConverter.toBase(category, value, unitId), unitId)
                        onRecordsChanged(if (position >= 0) records.mapIndexed { index, old -> if (index == position) record else old } else records + record)
                        editPosition = null
                    }
                }
            },
        )
    }
}

@Composable
private fun BodyMetricRecordTable(
    rows: List<BodyRecord>, unitId: String, category: String,
    onAdd: () -> Unit, onEdit: (BodyRecord) -> Unit, onDelete: (BodyRecord) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.body_record_count, rows.size), style = MaterialTheme.typography.titleMedium)
            AppIconTextButton(stringResource(R.string.body_record_add), R.drawable.ic_add, onAdd)
        }
        AppDataTable(
            rows = rows,
            rowKey = { index, record -> "${record.date}_${record.value}_${record.unit}_$index" },
            columns = listOf(
                AppDataTableColumn("time", { AppDataTableHeaderText(stringResource(R.string.body_record_time)) }, ColumnWidth.Fixed(156.dp)) { AppDataTableText(formatBodyRecordDisplayDateTime(it.date)) },
                AppDataTableColumn("value", { AppDataTableHeaderText(stringResource(R.string.body_record_value)) }, ColumnWidth.Fixed(120.dp)) { AppDataTableText("%.1f".format(UnitConverter.fromBase(category, it.value, unitId))) },
                AppDataTableColumn("unit", { AppDataTableHeaderText(stringResource(R.string.body_record_unit)) }, ColumnWidth.Fixed(96.dp)) { AppDataTableText(unitId) },
            ),
            actionsWidth = 104.dp,
            actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
            rowActions = { AppDataTableDeleteAction(stringResource(R.string.body_record_delete), onClick = { onDelete(it) }) },
            onRowClick = onEdit,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyMetricRecordEditSheet(
    editing: BodyRecord?, isHeight: Boolean, unitId: String, category: String,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var date by rememberSaveable(editing?.date) { mutableStateOf(editing?.date ?: formatBodyRecordDateTime(LocalDateTime.now())) }
        var value by rememberSaveable(editing?.value) { mutableStateOf(editing?.let { "%.1f".format(UnitConverter.fromBase(category, it.value, unitId)) }.orEmpty()) }
        val title = stringResource(when {
            editing != null && isHeight -> R.string.body_record_edit_height_title
            editing != null -> R.string.body_record_edit_weight_title
            isHeight -> R.string.body_record_add_height_title
            else -> R.string.body_record_add_weight_title
        })
        val context = LocalContext.current
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            AppFormSubtitle(stringResource(if (isHeight) R.string.body_record_height_help else R.string.body_record_weight_help, unitId))
            Text(stringResource(R.string.body_record_time), style = MaterialTheme.typography.titleSmall)
            var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)).clickable {
                showDateTimePicker = true
            }.padding(horizontal = 12.dp, vertical = 14.dp)) { Text(date, style = MaterialTheme.typography.bodyLarge) }
            if (showDateTimePicker) {
                ComposeDateTimePickerDialog(
                    initialMillis = bodyRecordEpochMillis(date),
                    onDismiss = { showDateTimePicker = false },
                    onDateTimePicked = {
                        date = formatDateTime(it)
                        showDateTimePicker = false
                    },
                )
            }
            OutlinedTextField(value, { value = it }, label = { AppInputLabel(stringResource(R.string.body_record_value)) }, colors = AppInputTextFieldColors(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                AppOutlinedIconTextButton(stringResource(R.string.body_record_cancel), R.drawable.ic_cancel, onDismiss)
                AppIconTextButton(stringResource(if (editing != null) R.string.body_record_save else R.string.body_record_add), if (editing != null) R.drawable.ic_save else R.drawable.ic_add, { onSave(date, value) }, Modifier.padding(start = 8.dp))
            }
        }
    }
}

private fun formatBodyMetricChartTimeAxis(timestamp: Long, intervalMs: Long): String {
    val pattern = when {
        intervalMs < 60_000L -> "HH:mm:ss"
        intervalMs < 3_600_000L -> "HH:mm"
        intervalMs < 86_400_000L -> "MM-dd HH:mm"
        intervalMs < 31L * 86_400_000L -> "MM-dd"
        else -> "yyyy-MM"
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))
}

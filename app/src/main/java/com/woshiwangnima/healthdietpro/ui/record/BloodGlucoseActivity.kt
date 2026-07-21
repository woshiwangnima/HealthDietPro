package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartEvent
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTimingAnchor
import com.woshiwangnima.healthdietpro.model.bloodglucose.isValidBloodGlucoseValue
import com.woshiwangnima.healthdietpro.model.bloodglucose.normalizeBloodGlucoseTimestamp
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.Locale

class BloodGlucoseActivity : BaseActivity() {
    private val viewModel: BloodGlucoseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                BloodGlucoseScreen(viewModel = viewModel, onBack = ::finish)
            }
        }
    }
}

@Composable
private fun BloodGlucoseScreen(viewModel: BloodGlucoseViewModel, onBack: () -> Unit) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val chartState by viewModel.chartState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingRecord by remember { mutableStateOf<BloodGlucoseRecord?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    val tabs = remember {
        listOf(
            DetailTabItem("chart", R.string.detail_tab_chart, R.drawable.ic_chart),
            DetailTabItem("data", R.string.detail_tab_data, R.drawable.ic_list),
        )
    }

    if (showEditor) {
        BloodGlucoseEditorScreen(
            record = editingRecord,
            onBack = { showEditor = false },
            onSave = { record ->
                viewModel.upsert(record)
                showEditor = false
            },
        )
        return
    }

    BaseScreen(title = stringResource(R.string.blood_glucose_title), onBack = onBack, includeNavigationBarPadding = false) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            AnimatedPageContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                direction = { initialTab, targetTab -> targetTab - initialTab },
            ) { tab ->
                if (tab == 0) {
                    BloodGlucoseChart(records, chartState, viewModel.chartStateKey) {
                        viewModel.onChartEvent(BaseChartEvent.StateChanged(it))
                    }
                } else {
                    BloodGlucoseDataPage(
                        records = records,
                        onAdd = { editingRecord = null; showEditor = true },
                        onEdit = { editingRecord = it; showEditor = true },
                        onDelete = viewModel::delete,
                    )
                }
            }
            DetailTabBar(items = tabs, selectedId = tabs[selectedTab].id) { item ->
                selectedTab = tabs.indexOf(item)
            }
        }
    }
}

@Composable
private fun BloodGlucoseChart(
    records: List<BloodGlucoseRecord>,
    chartState: com.woshiwangnima.healthdietpro.model.chart.ComposeChartState?,
    chartStateKey: String,
    onChartStateChanged: (com.woshiwangnima.healthdietpro.model.chart.ComposeChartState) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val seriesLabel = stringResource(R.string.blood_glucose_value)
    val unit = stringResource(R.string.blood_glucose_unit)
    val valueWithUnitFormat = stringResource(R.string.blood_glucose_value_with_unit)
    val data = remember(records) {
        records.sortedBy { it.timestamp }.map { record ->
            DataPoint(record.timestamp, record.valueMmolPerL.toFloat(), formatBloodGlucoseTime(record.timestamp))
        }
    }
    val series = remember(data, context, seriesLabel) {
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
            title = stringResource(R.string.blood_glucose_title),
            chartStateKey = chartStateKey,
            canvasStyle = ChartCanvasStyle(
                xAxisKind = ChartAxisKind.TimestampMs,
                yValueFormatter = { "%.1f".format(it) },
                xValueFormatter = ::formatBloodGlucoseAxisTime,
                crosshairValueFormatter = { value, _ -> String.format(Locale.getDefault(), valueWithUnitFormat, value, unit) },
                crosshairTimeFormatter = { timestamp -> formatBloodGlucoseTime(timestamp) },
            ),
            controlLabels = ChartControlLabels(
                lineStyle = stringResource(R.string.view_chart_line_style),
                xAxisRange = stringResource(R.string.view_chart_time_range),
                xAxisInterval = stringResource(R.string.view_chart_time_interval),
                yAxisBounds = stringResource(R.string.view_chart_bmi_bounds),
                fullscreen = stringResource(R.string.view_chart_fullscreen),
            ),
            series = listOf(series),
            xAxisLabel = stringResource(R.string.chart_axis_time_unit),
            yAxisLabel = stringResource(R.string.blood_glucose_unit),
            titleVisible = false,
        ),
        chartState = chartState,
        onChartStateChanged = onChartStateChanged,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun BloodGlucoseDataPage(
    records: List<BloodGlucoseRecord>,
    onAdd: () -> Unit,
    onEdit: (BloodGlucoseRecord) -> Unit,
    onDelete: (String) -> Unit,
) {
    var deletingRecord by remember { mutableStateOf<BloodGlucoseRecord?>(null) }
    val unit = stringResource(R.string.blood_glucose_unit)
    val valueWithUnitFormat = stringResource(R.string.blood_glucose_value_with_unit)
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.body_record_count, records.size), style = MaterialTheme.typography.titleMedium)
            AppIconTextButton(stringResource(R.string.body_record_add), R.drawable.ic_add, onAdd)
        }
        if (records.isEmpty()) {
            Text(stringResource(R.string.blood_glucose_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            AppDataTable(
                rows = records,
                rowKey = { _, record -> record.id },
                columns = listOf(
                    AppDataTableColumn("time", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_time)) }, ColumnWidth.Fixed(150.dp)) { AppDataTableText(formatBloodGlucoseTime(it.timestamp)) },
                    AppDataTableColumn("period", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_period)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(it.periodLabel()) },
                    AppDataTableColumn("value", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_value)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(String.format(Locale.getDefault(), valueWithUnitFormat, it.valueMmolPerL, unit)) },
                    AppDataTableColumn("note", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_note)) }, ColumnWidth.Flex(1f, 120.dp)) { AppDataTableText(it.note) },
                ),
                actionsWidth = 104.dp,
                actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
                rowActions = { AppDataTableDeleteAction(stringResource(R.string.body_record_delete), onClick = { deletingRecord = it }) },
                onRowClick = onEdit,
            )
        }
    }
    deletingRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deletingRecord = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.body_record_delete_confirm_message)) },
            confirmButton = { TextButton(onClick = { onDelete(record.id); deletingRecord = null }) { Text(stringResource(R.string.body_record_delete)) } },
            dismissButton = { TextButton(onClick = { deletingRecord = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } },
        )
    }
}

@Composable
private fun BloodGlucoseEditorScreen(
    record: BloodGlucoseRecord?,
    onBack: () -> Unit,
    onSave: (BloodGlucoseRecord) -> Unit,
) {
    var timestamp by rememberSaveable(record?.id) { mutableStateOf(record?.timestamp ?: normalizeBloodGlucoseTimestamp(System.currentTimeMillis())) }
    var value by rememberSaveable(record?.id) { mutableStateOf(record?.valueMmolPerL?.toString().orEmpty()) }
    var anchor by rememberSaveable(record?.id) { mutableStateOf(record?.timingAnchor) }
    var relativeMinutes by rememberSaveable(record?.id) { mutableStateOf(record?.relativeMinutes?.toString().orEmpty()) }
    var note by rememberSaveable(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    val validValue = value.toDoubleOrNull()?.takeIf(::isValidBloodGlucoseValue)
    val invalidValue = value.isNotBlank() && validValue == null
    val invalidRelativeMinutes = relativeMinutes.isNotBlank() && relativeMinutes.toIntOrNull() == null

    BaseScreen(title = stringResource(R.string.blood_glucose_add_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { AppFormSubtitle(stringResource(R.string.blood_glucose_note_hint)) }
                item { Text(stringResource(R.string.blood_glucose_time), style = MaterialTheme.typography.titleSmall) }
                item {
                    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)).clickable { showDateTimePicker = true }.padding(horizontal = 12.dp, vertical = 14.dp)) {
                        Text(com.woshiwangnima.healthdietpro.common.ui.formatDateTime(timestamp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(stringResource(R.string.blood_glucose_value)) },
                        suffix = { Text(stringResource(R.string.blood_glucose_unit)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = invalidValue,
                        supportingText = if (invalidValue) {
                            { Text(stringResource(R.string.blood_glucose_value_invalid)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item { Text(stringResource(R.string.blood_glucose_period), style = MaterialTheme.typography.titleSmall) }
                item {
                    Text(stringResource(R.string.blood_glucose_default_time), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BloodGlucoseTimingAnchor.entries.forEach { item ->
                            FilterChip(selected = anchor == item, onClick = { anchor = if (anchor == item) null else item }, label = { Text(stringResource(item.labelRes())) })
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = relativeMinutes,
                        onValueChange = { relativeMinutes = it },
                        label = { Text(stringResource(R.string.blood_glucose_relative_minutes)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = invalidRelativeMinutes,
                        supportingText = if (invalidRelativeMinutes) {
                            { Text(stringResource(R.string.blood_glucose_relative_minutes_invalid)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.blood_glucose_note)) },
                        placeholder = { Text(stringResource(R.string.blood_glucose_note_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
            }
            FormSaveBar(
                text = stringResource(R.string.blood_glucose_save),
                enabled = validValue != null && !invalidRelativeMinutes,
                onSave = {
                    onSave(BloodGlucoseRecord(record?.id ?: UUID.randomUUID().toString(), timestamp, requireNotNull(validValue), anchor, relativeMinutes.toIntOrNull(), note.trim()))
                },
            )
        }
    }
    if (showDateTimePicker) {
        ComposeDateTimePickerDialog(timestamp, { showDateTimePicker = false }) {
            timestamp = it
            showDateTimePicker = false
        }
    }
}

@Composable
private fun BloodGlucoseRecord.periodLabel(): String {
    val anchorText = timingAnchor?.let { stringResource(it.labelRes()) }.orEmpty()
    val offsetText = relativeMinutes?.let { stringResource(R.string.blood_glucose_relative_minutes_value, it) }.orEmpty()
    return listOf(anchorText, offsetText)
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifEmpty { stringResource(R.string.blood_glucose_period_none) }
}

private fun BloodGlucoseTimingAnchor.labelRes(): Int = when (this) {
    BloodGlucoseTimingAnchor.BREAKFAST -> R.string.blood_glucose_anchor_breakfast
    BloodGlucoseTimingAnchor.LUNCH -> R.string.blood_glucose_anchor_lunch
    BloodGlucoseTimingAnchor.DINNER -> R.string.blood_glucose_anchor_dinner
    BloodGlucoseTimingAnchor.WAKE_UP -> R.string.blood_glucose_anchor_wake_up
    BloodGlucoseTimingAnchor.BEDTIME -> R.string.blood_glucose_anchor_bedtime
}

private fun formatBloodGlucoseTime(timestamp: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

private fun formatBloodGlucoseAxisTime(timestamp: Long, intervalMs: Long): String {
    val pattern = if (intervalMs < 86_400_000L) "MM-dd HH:mm" else "MM-dd"
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))
}

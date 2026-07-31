package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartEvent
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureCategory
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureClassificationRule
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureRecord
import com.woshiwangnima.healthdietpro.model.bloodpressure.bloodPressureClassificationRules
import com.woshiwangnima.healthdietpro.model.bloodpressure.category
import com.woshiwangnima.healthdietpro.model.bloodpressure.bloodPressureCategory
import com.woshiwangnima.healthdietpro.model.bloodpressure.isValidBloodPressure
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.common.range.CriterionOperator
import com.woshiwangnima.healthdietpro.common.range.Range
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
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
import java.util.Locale
import java.util.UUID

class BloodPressureActivity : BaseActivity() {
    companion object {
        const val EXTRA_OPEN_EDITOR = "open_editor"
    }

    private val viewModel: BloodPressureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                BloodPressureScreen(
                    viewModel = viewModel,
                    onBack = ::finish,
                    openEditorInitially = intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false),
                )
            }
        }
    }
}

@Composable
private fun BloodPressureScreen(
    viewModel: BloodPressureViewModel,
    onBack: () -> Unit,
    openEditorInitially: Boolean,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val chartState by viewModel.chartState.collectAsStateWithLifecycle()
    var editingRecord by remember { mutableStateOf<BloodPressureRecord?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(openEditorInitially) }
    var showReference by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = showEditor) {
        if (openEditorInitially) onBack() else showEditor = false
    }
    BackHandler(enabled = !showEditor && showReference) { showReference = false }
    when {
        showEditor -> {
        BloodPressureEditorScreen(editingRecord, { if (openEditorInitially) onBack() else showEditor = false }) { record -> viewModel.upsert(record); showEditor = false }
        }
        showReference -> BloodPressureReferenceScreen(onBack = { showReference = false })
        else -> BloodPressureHomeScreen(
            records = records,
            chartState = chartState,
            chartStateKey = viewModel.chartStateKey,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onChartStateChanged = { viewModel.onChartEvent(BaseChartEvent.StateChanged(it)) },
            onBack = onBack,
            onOpenReference = { showReference = true },
            onAdd = { editingRecord = null; showEditor = true },
            onEdit = { editingRecord = it; showEditor = true },
            onDelete = viewModel::delete,
        )
    }
}

@Composable
private fun BloodPressureHomeScreen(
    records: List<BloodPressureRecord>,
    chartState: com.woshiwangnima.healthdietpro.model.chart.ComposeChartState?,
    chartStateKey: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onChartStateChanged: (com.woshiwangnima.healthdietpro.model.chart.ComposeChartState) -> Unit,
    onBack: () -> Unit,
    onOpenReference: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (BloodPressureRecord) -> Unit,
    onDelete: (String) -> Unit,
) {
    val tabs = remember { listOf(DetailTabItem("chart", R.string.detail_tab_chart, R.drawable.ic_chart), DetailTabItem("data", R.string.detail_tab_data, R.drawable.ic_list)) }
    BaseScreen(
        title = stringResource(R.string.blood_pressure_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
        actions = { IconButton(onClick = onOpenReference) { Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.blood_pressure_reference_title)) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            AnimatedPageContent(selectedTab, Modifier.weight(1f), direction = { initial, target -> target - initial }) { tab ->
                if (tab == 0) BloodPressureChart(records, chartState, chartStateKey, onChartStateChanged)
                else BloodPressureDataContent(records, onAdd, onEdit, onDelete)
            }
            DetailTabBar(tabs, tabs[selectedTab].id) { onTabSelected(tabs.indexOf(it)) }
        }
    }
}

@Composable
private fun BloodPressureDataContent(
    records: List<BloodPressureRecord>, onAdd: () -> Unit,
    onEdit: (BloodPressureRecord) -> Unit, onDelete: (String) -> Unit,
) {
    var deletingRecord by remember { mutableStateOf<BloodPressureRecord?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val unitId = AppPrefs.getUnit(context, UnitCategoryType.Pressure.id, UnitCategoryType.Pressure.defaultUnitId)
    val unit = pressureUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.body_record_count, records.size), style = MaterialTheme.typography.titleMedium)
                AppIconTextButton(stringResource(R.string.body_record_add), R.drawable.ic_add, onAdd)
            }
            if (records.isEmpty()) Text(stringResource(R.string.blood_pressure_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else AppDataTable(
                rows = records,
                rowKey = { _, record -> record.id },
                columns = listOf(
                    AppDataTableColumn("time", { AppDataTableHeaderText(stringResource(R.string.blood_pressure_time)) }, ColumnWidth.Fixed(150.dp)) { AppDataTableText(formatBloodPressureTime(it.timestamp)) },
                    AppDataTableColumn("systolic", { AppDataTableHeaderText(stringResource(R.string.blood_pressure_systolic)) }, ColumnWidth.Fixed(100.dp)) { AppDataTableText("${pressureValue(it.systolicMmhg, unitId)} $unit") },
                    AppDataTableColumn("diastolic", { AppDataTableHeaderText(stringResource(R.string.blood_pressure_diastolic)) }, ColumnWidth.Fixed(100.dp)) { AppDataTableText("${pressureValue(it.diastolicMmhg, unitId)} $unit") },
                    AppDataTableColumn("pulse", { AppDataTableHeaderText(stringResource(R.string.blood_pressure_pulse_pressure)) }, ColumnWidth.Fixed(100.dp)) { AppDataTableText(it.pulsePressureText(unitId, unit)) },
                    AppDataTableColumn("category", { AppDataTableHeaderText(stringResource(R.string.blood_pressure_category)) }, ColumnWidth.Fixed(130.dp)) { AppDataTableText(stringResource(it.category().labelRes()), color = it.category().color()) },
                    AppDataTableColumn("note", { AppDataTableHeaderText(stringResource(R.string.blood_pressure_note)) }, ColumnWidth.Flex(1f, 120.dp)) { AppDataTableText(it.note) },
                ),
                actionsWidth = 104.dp,
                actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
                rowActions = { AppDataTableDeleteAction(stringResource(R.string.body_record_delete), onClick = { deletingRecord = it }) },
                onRowClick = onEdit,
                modifier = Modifier.weight(1f),
            )
    }
    deletingRecord?.let { record ->
        AlertDialog(onDismissRequest = { deletingRecord = null }, title = { Text(stringResource(R.string.body_record_delete_confirm_title)) }, text = { Text(stringResource(R.string.body_record_delete_confirm_message)) }, confirmButton = { TextButton(onClick = { onDelete(record.id); deletingRecord = null }) { Text(stringResource(R.string.body_record_delete)) } }, dismissButton = { TextButton(onClick = { deletingRecord = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } })
    }
}

@Composable
private fun BloodPressureChart(
    records: List<BloodPressureRecord>,
    chartState: com.woshiwangnima.healthdietpro.model.chart.ComposeChartState?,
    chartStateKey: String,
    onChartStateChanged: (com.woshiwangnima.healthdietpro.model.chart.ComposeChartState) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val unitId = AppPrefs.getUnit(context, UnitCategoryType.Pressure.id, UnitCategoryType.Pressure.defaultUnitId)
    val unit = pressureUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId
    val systolicData = remember(records, unitId) { records.sortedBy { it.timestamp }.map { DataPoint(it.timestamp, UnitConverter.fromBase(UnitCategoryType.Pressure.id, it.systolicMmhg, unitId), formatBloodPressureTime(it.timestamp)) } }
    val diastolicData = remember(records, unitId) { records.sortedBy { it.timestamp }.map { DataPoint(it.timestamp, UnitConverter.fromBase(UnitCategoryType.Pressure.id, it.diastolicMmhg, unitId), formatBloodPressureTime(it.timestamp)) } }
    val systolicLabel = stringResource(R.string.blood_pressure_systolic)
    val diastolicLabel = stringResource(R.string.blood_pressure_diastolic)
    val controls = ChartControlLabels(
        lineStyle = stringResource(R.string.view_chart_line_style),
        xAxisRange = stringResource(R.string.view_chart_time_range),
        xAxisInterval = stringResource(R.string.view_chart_time_interval),
        yAxisBounds = stringResource(R.string.view_chart_bmi_bounds),
        fullscreen = stringResource(R.string.view_chart_fullscreen),
    )
    val series = remember(systolicData, diastolicData, systolicLabel, diastolicLabel) {
        listOf(
            ChartSeries(systolicData, systolicLabel, android.graphics.Color.rgb(229, 57, 53), LineStyle.LINEAR, LineType.SOLID, PointShape.CIRCLE, PointFill.FILLED),
            ChartSeries(diastolicData, diastolicLabel, android.graphics.Color.rgb(30, 136, 229), LineStyle.LINEAR, LineType.SOLID, PointShape.DIAMOND, PointFill.FILLED),
        )
    }
    ComposeChart(
        spec = ComposeChartSpec(
            title = stringResource(R.string.blood_pressure_title),
            chartStateKey = chartStateKey,
            series = series,
            xAxisLabel = stringResource(R.string.chart_axis_time_unit),
            yAxisLabel = unit,
            titleVisible = false,
            canvasStyle = ChartCanvasStyle(
                xAxisKind = ChartAxisKind.TimestampMs,
                yValueFormatter = { value -> if (unitId == "kpa") "%.1f".format(value) else "%.0f".format(value) },
                xValueFormatter = ::formatBloodPressureAxisTime,
                crosshairValueFormatter = { value, _ -> "${if (unitId == "kpa") "%.1f".format(value) else "%.0f".format(value)} $unit" },
                crosshairTimeFormatter = ::formatBloodPressureTime,
            ),
            controlLabels = controls,
        ),
        chartState = chartState,
        onChartStateChanged = onChartStateChanged,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun BloodPressureReferenceScreen(onBack: () -> Unit) {
    val rows = bloodPressureClassificationRules.reversed()
    BaseScreen(title = stringResource(R.string.blood_pressure_reference_title), onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { AppFormSubtitle(stringResource(R.string.blood_pressure_reference_disclaimer)) }
            item {
                Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        BloodPressureReferenceRow(stringResource(R.string.blood_pressure_category), stringResource(R.string.blood_pressure_reference_standard), header = true)
                        rows.forEach { rule ->
                            BloodPressureReferenceRow(
                                label = stringResource(rule.category.labelRes()),
                                standard = bloodPressureRuleText(rule),
                                background = rule.category.color().copy(alpha = .16f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun bloodPressureRuleText(rule: BloodPressureClassificationRule): String {
    val connector = stringResource(
        if (rule.operator == CriterionOperator.All) R.string.blood_pressure_reference_and else R.string.blood_pressure_reference_or,
    )
    return listOfNotNull(
        rule.systolicRange?.let { it.bloodPressureRangeText(stringResource(R.string.blood_pressure_systolic)) },
        rule.diastolicRange?.let { it.bloodPressureRangeText(stringResource(R.string.blood_pressure_diastolic)) },
    ).joinToString(" $connector ")
}

private fun Range<Float>.bloodPressureRangeText(label: String): String {
    val lower = min
    val upper = max
    return when {
        lower != null && upper != null -> "${lower.formatPressureThreshold()} mmHg ${if (minInclusive) "≤" else "<"} $label ${if (maxInclusive) "≤" else "<"} ${upper.formatPressureThreshold()} mmHg"
        lower != null -> "$label ${if (minInclusive) "≥" else ">"} ${lower.formatPressureThreshold()} mmHg"
        upper != null -> "$label ${if (maxInclusive) "≤" else "<"} ${upper.formatPressureThreshold()} mmHg"
        else -> ""
    }
}

private fun Float.formatPressureThreshold(): String = "%.0f".format(this)

@Composable
private fun BloodPressureReferenceRow(label: String, standard: String, background: Color = Color.Transparent, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().background(background).padding(horizontal = 8.dp, vertical = 10.dp)) {
        Text(label, style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(standard, style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.5f))
    }
}

@Composable
private fun BloodPressureEditorScreen(record: BloodPressureRecord?, onBack: () -> Unit, onSave: (BloodPressureRecord) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var timestamp by rememberSaveable(record?.id) { mutableStateOf(record?.timestamp ?: System.currentTimeMillis() / 60_000L * 60_000L) }
    var unitId by rememberSaveable { mutableStateOf(AppPrefs.getUnit(context, UnitCategoryType.Pressure.id, UnitCategoryType.Pressure.defaultUnitId)) }
    var systolic by rememberSaveable(record?.id) { mutableStateOf(record?.systolicMmhg?.let { pressureValue(it, unitId) }.orEmpty()) }
    var diastolic by rememberSaveable(record?.id) { mutableStateOf(record?.diastolicMmhg?.let { pressureValue(it, unitId) }.orEmpty()) }
    var note by rememberSaveable(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    val systolicBase = systolic.toFloatOrNull()?.let { UnitConverter.toBase(UnitCategoryType.Pressure.id, it, unitId) }
    val diastolicBase = diastolic.toFloatOrNull()?.let { UnitConverter.toBase(UnitCategoryType.Pressure.id, it, unitId) }
    val valid = isValidBloodPressure(systolicBase, diastolicBase)
    val unit = pressureUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId
    BaseScreen(title = stringResource(R.string.blood_pressure_add_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { AppFormSubtitle(stringResource(R.string.blood_pressure_editor_hint)) }
                item { Text(stringResource(R.string.blood_pressure_time), style = MaterialTheme.typography.titleSmall) }
                item { Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .36f)).clickable { showDateTimePicker = true }.padding(horizontal = 12.dp, vertical = 14.dp)) { Text(com.woshiwangnima.healthdietpro.common.ui.formatDateTime(timestamp)) } }
                item { EditorTextField(label = stringResource(R.string.blood_pressure_systolic), value = systolic, onValueChange = { systolic = it }, required = true, numeric = true, suffix = { Text(unit) }) }
                item { EditorTextField(label = stringResource(R.string.blood_pressure_diastolic), value = diastolic, onValueChange = { diastolic = it }, required = true, numeric = true, suffix = { Text(unit) }, supportingTextOverride = if (!valid && systolic.isNotBlank() && diastolic.isNotBlank()) ({ Text(stringResource(R.string.blood_pressure_values_invalid)) }) else null) }
                if (valid && systolicBase != null && diastolicBase != null) item {
                    val pulsePressure = systolicBase - diastolicBase
                    Text(stringResource(R.string.blood_pressure_pulse_pressure_value, pressureValue(pulsePressure, unitId), unit), style = MaterialTheme.typography.titleMedium)
                    val category = bloodPressureCategory(systolicBase, diastolicBase)
                    Text(stringResource(category.labelRes()), color = category.color())
                }
                item { AppDropdownField(label = stringResource(R.string.blood_pressure_unit), value = unit, options = pressureUnitOptions(), onSelect = { selected ->
                    systolic.toFloatOrNull()?.let { systolic = pressureValue(UnitConverter.toBase(UnitCategoryType.Pressure.id, it, unitId), selected.id) }
                    diastolic.toFloatOrNull()?.let { diastolic = pressureValue(UnitConverter.toBase(UnitCategoryType.Pressure.id, it, unitId), selected.id) }
                    unitId = selected.id; AppPrefs.setUnit(context, UnitCategoryType.Pressure.id, unitId)
                }) }
                item { EditorTextField(label = stringResource(R.string.blood_pressure_note), value = note, onValueChange = { note = it }, numeric = false) }
            }
            FormSaveBar(
                text = stringResource(R.string.blood_pressure_save),
                enabled = valid,
                onSave = { onSave(BloodPressureRecord(record?.id ?: UUID.randomUUID().toString(), timestamp, requireNotNull(systolicBase), requireNotNull(diastolicBase), note.trim())) },
            )
        }
    }
    if (showDateTimePicker) ComposeDateTimePickerDialog(timestamp, { showDateTimePicker = false }) { timestamp = it; showDateTimePicker = false }
}

@Composable private fun BloodPressureCategory.labelRes(): Int = when (this) {
    BloodPressureCategory.Normal -> R.string.blood_pressure_category_normal
    BloodPressureCategory.Elevated -> R.string.blood_pressure_category_elevated
    BloodPressureCategory.HypertensionStage1 -> R.string.blood_pressure_category_stage_1
    BloodPressureCategory.HypertensionStage2 -> R.string.blood_pressure_category_stage_2
    BloodPressureCategory.HypertensiveCrisis -> R.string.blood_pressure_category_crisis
}

private fun BloodPressureCategory.color(): Color = when (this) {
    BloodPressureCategory.Normal -> Color(0xFF43A047)
    BloodPressureCategory.Elevated -> Color(0xFFF9A825)
    BloodPressureCategory.HypertensionStage1 -> Color(0xFFFB8C00)
    BloodPressureCategory.HypertensionStage2 -> Color(0xFFE53935)
    BloodPressureCategory.HypertensiveCrisis -> Color(0xFF8E24AA)
}

private fun pressureUnitOptions(): List<AppDropdownOption> = UnitConverter.getRepository()?.getCategory(UnitCategoryType.Pressure.id)?.units?.filterNot { it.hidden }?.map { AppDropdownOption(it.id, it.symbol(Locale.getDefault())) }.orEmpty()
private fun pressureValue(valueMmhg: Float, unitId: String): String = String.format(Locale.getDefault(), if (unitId == "kpa") "%.1f" else "%.0f", UnitConverter.fromBase(UnitCategoryType.Pressure.id, valueMmhg, unitId))
private fun BloodPressureRecord.pressureText(unitId: String, unit: String) = "${pressureValue(systolicMmhg, unitId)}/${pressureValue(diastolicMmhg, unitId)} $unit"
private fun BloodPressureRecord.pulsePressureText(unitId: String, unit: String) = "${pressureValue(pulsePressureMmhg, unitId)} $unit"
private fun formatBloodPressureTime(timestamp: Long): String = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
private fun formatBloodPressureAxisTime(timestamp: Long, intervalMs: Long): String = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(if (intervalMs < 86_400_000L) "MM-dd HH:mm" else "MM-dd"))

package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import android.Manifest
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppNumericStepperField
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.common.range.UnitRange
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartEvent
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTimingAnchor
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseDiabetesType
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseAlertMode
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseReminderSettings
import com.woshiwangnima.healthdietpro.model.bloodglucose.isValidBloodGlucoseValue
import com.woshiwangnima.healthdietpro.model.bloodglucose.normalizeBloodGlucoseTimestamp
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.model.unit.UnitStepMode
import com.woshiwangnima.healthdietpro.model.unit.stepSpec
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.Locale

class BloodGlucoseActivity : BaseActivity() {
    companion object {
        const val EXTRA_OPEN_EDITOR = "open_editor"
    }

    private val viewModel: BloodGlucoseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                BloodGlucoseScreen(
                    viewModel = viewModel,
                    onBack = ::finish,
                    openEditorInitially = intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false),
                )
            }
        }
    }
}

@Composable
private fun BloodGlucoseScreen(
    viewModel: BloodGlucoseViewModel,
    onBack: () -> Unit,
    openEditorInitially: Boolean,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val chartState by viewModel.chartState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingRecord by remember { mutableStateOf<BloodGlucoseRecord?>(null) }
    var showEditor by remember { mutableStateOf(openEditorInitially) }
    var route by rememberSaveable { mutableStateOf(BloodGlucoseRoute.Records) }
    val tabs = remember {
        listOf(
            DetailTabItem("chart", R.string.detail_tab_chart, R.drawable.ic_chart),
            DetailTabItem("data", R.string.detail_tab_data, R.drawable.ic_list),
        )
    }

    BackHandler(enabled = showEditor) {
        if (openEditorInitially) onBack() else showEditor = false
    }
    if (showEditor) {
        BloodGlucoseEditorScreen(
            record = editingRecord,
            onBack = { if (openEditorInitially) onBack() else showEditor = false },
            onSave = { record ->
                viewModel.upsert(record)
                showEditor = false
            },
        )
        return
    }
    BackHandler(enabled = route != BloodGlucoseRoute.Records) {
        route = when (route) {
            BloodGlucoseRoute.Targets -> BloodGlucoseRoute.Settings
            BloodGlucoseRoute.Reminders -> BloodGlucoseRoute.Settings
            BloodGlucoseRoute.Settings, BloodGlucoseRoute.Records -> BloodGlucoseRoute.Records
        }
    }

    if (route == BloodGlucoseRoute.Settings) {
        BloodGlucoseSettingsScreen(onBack = { route = BloodGlucoseRoute.Records }, onOpenTargets = { route = BloodGlucoseRoute.Targets }, onOpenReminders = { route = BloodGlucoseRoute.Reminders })
        return
    }
    if (route == BloodGlucoseRoute.Targets) {
        val diabetesType by viewModel.diabetesType.collectAsStateWithLifecycle()
        BloodGlucoseTargetRangeScreen(
            diabetesType = diabetesType,
            onBack = { route = BloodGlucoseRoute.Settings },
            onSelectDiabetesType = viewModel::setDiabetesType,
        )
        return
    }
    if (route == BloodGlucoseRoute.Reminders) {
        val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()
        BloodGlucoseReminderSettingsScreen(
            settings = reminderSettings,
            onBack = { route = BloodGlucoseRoute.Settings },
            onSettingsChange = viewModel::setReminderSettings,
        )
        return
    }

    BaseScreen(
        title = stringResource(R.string.blood_glucose_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
        actions = {
            IconButton(onClick = { route = BloodGlucoseRoute.Settings }) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.blood_glucose_settings_title))
            }
        },
    ) { padding ->
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

private enum class BloodGlucoseRoute { Records, Settings, Targets, Reminders }

@Composable
private fun BloodGlucoseSettingsScreen(onBack: () -> Unit, onOpenTargets: () -> Unit, onOpenReminders: () -> Unit) {
    BaseScreen(title = stringResource(R.string.blood_glucose_settings_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SettingRow(
                title = stringResource(R.string.blood_glucose_target_range_settings),
                subtitle = stringResource(R.string.blood_glucose_target_range_settings_desc),
                leadingIconRes = R.drawable.ic_blood_glucose,
                onClick = onOpenTargets,
            )
            HorizontalDivider()
            SettingRow(
                title = stringResource(R.string.blood_glucose_reminder_settings),
                subtitle = stringResource(R.string.blood_glucose_reminder_settings_desc),
                leadingIconRes = R.drawable.ic_notification,
                onClick = onOpenReminders,
            )
            HorizontalDivider()
            SettingRow(
                title = stringResource(R.string.blood_glucose_do_not_disturb_settings),
                subtitle = stringResource(R.string.blood_glucose_do_not_disturb_settings_desc),
                leadingIconRes = R.drawable.ic_do_not_disturb,
                onClick = {},
            )
        }
    }
}

@Composable
private fun BloodGlucoseReminderSettingsScreen(
    settings: BloodGlucoseReminderSettings,
    onBack: () -> Unit,
    onSettingsChange: (BloodGlucoseReminderSettings) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val unitId = AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)
    val unitLabel = glucoseUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId
    val glucoseStep = UnitCategoryType.Glucose.stepSpec(unitId).valueFor(UnitStepMode.Normal)
    fun update(next: BloodGlucoseReminderSettings) = onSettingsChange(next)
    fun requestPermissionWhenEnabled(enabled: Boolean) {
        if (enabled && android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    BaseScreen(title = stringResource(R.string.blood_glucose_reminder_settings), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                BloodGlucoseReminderCard {
                    BloodGlucoseReminderToggle(stringResource(R.string.blood_glucose_alert_high_enabled), settings.highEnabled) {
                        requestPermissionWhenEnabled(it); update(settings.copy(highEnabled = it))
                    }
                    AppNumericStepperField(
                        label = stringResource(R.string.blood_glucose_alert_high_threshold),
                        value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, settings.highThresholdMmolPerL, unitId),
                        unit = unitLabel,
                        step = glucoseStep,
                        onValueChange = { update(settings.copy(highThresholdMmolPerL = UnitConverter.toBase(UnitCategoryType.Glucose.id, it, unitId))) },
                    )
                }
            }
            item {
                BloodGlucoseReminderCard {
                    BloodGlucoseReminderToggle(stringResource(R.string.blood_glucose_alert_low_enabled), settings.lowEnabled) {
                        requestPermissionWhenEnabled(it); update(settings.copy(lowEnabled = it))
                    }
                    AppNumericStepperField(
                        label = stringResource(R.string.blood_glucose_alert_low_threshold),
                        value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, settings.lowThresholdMmolPerL, unitId),
                        unit = unitLabel,
                        step = glucoseStep,
                        onValueChange = { update(settings.copy(lowThresholdMmolPerL = UnitConverter.toBase(UnitCategoryType.Glucose.id, it, unitId))) },
                    )
                }
            }
            item {
                BloodGlucoseReminderCard {
                    BloodGlucoseReminderToggle(stringResource(R.string.blood_glucose_alert_emergency_low_enabled), settings.emergencyLowEnabled) {
                        requestPermissionWhenEnabled(it); update(settings.copy(emergencyLowEnabled = it))
                    }
                    AppNumericStepperField(
                        label = stringResource(R.string.blood_glucose_alert_emergency_low_threshold),
                        value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, settings.emergencyLowThresholdMmolPerL, unitId),
                        unit = unitLabel,
                        step = glucoseStep,
                        onValueChange = { update(settings.copy(emergencyLowThresholdMmolPerL = UnitConverter.toBase(UnitCategoryType.Glucose.id, it, unitId))) },
                    )
                }
            }
            item {
                BloodGlucoseTrendReminderSection(
                    toggleTitle = stringResource(R.string.blood_glucose_alert_rising_title),
                    enabled = settings.risingEnabled,
                    mode = settings.risingMode,
                    intervalMinutes = UnitConverter.fromBase(UnitCategoryType.Time.id, settings.risingReminderIntervalSeconds.toFloat(), "min").toInt(),
                    durationSeconds = settings.risingAlertDurationSeconds,
                    onEnabledChange = { requestPermissionWhenEnabled(it); update(settings.copy(risingEnabled = it)) },
                    onModeChange = { update(settings.copy(risingMode = it)) },
                    onIntervalChange = { update(settings.copy(risingReminderIntervalSeconds = UnitConverter.toBase(UnitCategoryType.Time.id, it.toFloat(), "min").toInt())) },
                    onDurationChange = { update(settings.copy(risingAlertDurationSeconds = it)) },
                )
            }
            item {
                BloodGlucoseTrendReminderSection(
                    toggleTitle = stringResource(R.string.blood_glucose_alert_falling_title),
                    enabled = settings.fallingEnabled,
                    mode = settings.fallingMode,
                    intervalMinutes = UnitConverter.fromBase(UnitCategoryType.Time.id, settings.fallingReminderIntervalSeconds.toFloat(), "min").toInt(),
                    durationSeconds = settings.fallingAlertDurationSeconds,
                    onEnabledChange = { requestPermissionWhenEnabled(it); update(settings.copy(fallingEnabled = it)) },
                    onModeChange = { update(settings.copy(fallingMode = it)) },
                    onIntervalChange = { update(settings.copy(fallingReminderIntervalSeconds = UnitConverter.toBase(UnitCategoryType.Time.id, it.toFloat(), "min").toInt())) },
                    onDurationChange = { update(settings.copy(fallingAlertDurationSeconds = it)) },
                )
            }
        }
    }
}

@Composable
private fun BloodGlucoseReminderCard(content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun BloodGlucoseReminderSection(content: @Composable () -> Unit) {
    BloodGlucoseReminderCard {
        content()
    }
}

@Composable
private fun BloodGlucoseReminderToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BloodGlucoseTrendReminderSection(
    toggleTitle: String,
    enabled: Boolean,
    mode: BloodGlucoseAlertMode,
    intervalMinutes: Int,
    durationSeconds: Int,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (BloodGlucoseAlertMode) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
) {
    BloodGlucoseReminderSection {
        BloodGlucoseReminderToggle(toggleTitle, enabled, onEnabledChange)
        AppDropdownField(
            label = stringResource(R.string.blood_glucose_alert_mode),
            value = stringResource(mode.labelRes()),
            options = BloodGlucoseAlertMode.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
            onSelect = { onModeChange(BloodGlucoseAlertMode.valueOf(it.id)) },
        )
        AppNumericStepperField(stringResource(R.string.blood_glucose_alert_interval), intervalMinutes.toFloat(), stringResource(R.string.blood_glucose_minutes), UnitCategoryType.Time.stepSpec("min").valueFor(UnitStepMode.Normal), onValueChange = { onIntervalChange(it.toInt().coerceAtLeast(1)) })
        AppNumericStepperField(stringResource(R.string.blood_glucose_alert_duration), durationSeconds.toFloat(), stringResource(R.string.blood_glucose_seconds), 1f, onValueChange = { onDurationChange(it.toInt().coerceAtLeast(1)) })
    }
}

@Composable
private fun BloodGlucoseAlertMode.labelRes(): Int = when (this) {
    BloodGlucoseAlertMode.Sound -> R.string.blood_glucose_alert_mode_sound
    BloodGlucoseAlertMode.Vibration -> R.string.blood_glucose_alert_mode_vibration
    BloodGlucoseAlertMode.SoundAndVibration -> R.string.blood_glucose_alert_mode_sound_and_vibration
}

@Composable
private fun BloodGlucoseTargetRangeScreen(
    diabetesType: BloodGlucoseDiabetesType,
    onBack: () -> Unit,
    onSelectDiabetesType: (BloodGlucoseDiabetesType) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val unitId = AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)
    val unitLabel = glucoseUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId
    var showTypePicker by remember { mutableStateOf(false) }
    val targetRange = diabetesType.targetRange(unitId)
    BaseScreen(title = stringResource(R.string.blood_glucose_target_range_settings), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SettingRow(
                title = stringResource(R.string.blood_glucose_diabetes_type),
                subtitle = stringResource(R.string.blood_glucose_diabetes_type_desc),
                leadingIconRes = R.drawable.ic_medical_history,
                trailingValue = stringResource(diabetesType.labelRes()),
                onClick = { showTypePicker = true },
            )
            HorizontalDivider()
            SettingRow(
                title = stringResource(R.string.blood_glucose_target_range),
                subtitle = stringResource(R.string.blood_glucose_target_range_desc),
                leadingIconRes = R.drawable.ic_blood_glucose,
                trailingValue = bloodGlucoseTargetRangeText(targetRange, unitLabel),
                onClick = {},
                clickable = false,
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.blood_glucose_target_range_recommendations),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
            AppDataTable(
                rows = BloodGlucoseDiabetesType.entries.toList(),
                rowKey = { _, type -> type.name },
                columns = listOf(
                    AppDataTableColumn("type", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_diabetes_type)) }, ColumnWidth.Fixed(180.dp)) { AppDataTableText(stringResource(it.labelRes())) },
                    AppDataTableColumn("range", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_target_range)) }, ColumnWidth.Fixed(160.dp)) { AppDataTableText(bloodGlucoseTargetRangeText(it.targetRange(unitId), unitLabel)) },
                ),
                showRowNumber = false,
                showPager = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (showTypePicker) {
        BackHandler { showTypePicker = false }
        AlertDialog(
            onDismissRequest = { showTypePicker = false },
            title = { Text(stringResource(R.string.blood_glucose_diabetes_type)) },
            text = {
                LazyColumn {
                    items(BloodGlucoseDiabetesType.entries.size) { index ->
                        val type = BloodGlucoseDiabetesType.entries[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().then(if (type.available) Modifier.clickable { onSelectDiabetesType(type); showTypePicker = false } else Modifier).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = type == diabetesType, onClick = null, enabled = type.available)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(stringResource(type.labelRes()), color = if (type.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!type.available) Text(stringResource(R.string.blood_glucose_diabetes_type_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTypePicker = false }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } },
        )
    }
}

@Composable
private fun BloodGlucoseDiabetesType.labelRes(): Int = when (this) {
    BloodGlucoseDiabetesType.Normal -> R.string.blood_glucose_diabetes_type_normal
    BloodGlucoseDiabetesType.Type1 -> R.string.blood_glucose_diabetes_type_type_1
    BloodGlucoseDiabetesType.Type2 -> R.string.blood_glucose_diabetes_type_type_2
    BloodGlucoseDiabetesType.Gestational -> R.string.blood_glucose_diabetes_type_gestational
    BloodGlucoseDiabetesType.Other -> R.string.blood_glucose_diabetes_type_other
}

private fun bloodGlucoseTargetRangeText(range: UnitRange<Float>, unitLabel: String): String {
    val min = requireNotNull(range.min)
    val max = requireNotNull(range.max)
    return String.format(Locale.getDefault(), "%.1f-%.1f %s", min, max, unitLabel)
}

@Composable
private fun BloodGlucoseChart(
    records: List<BloodGlucoseRecord>,
    chartState: com.woshiwangnima.healthdietpro.model.chart.ComposeChartState?,
    chartStateKey: String,
    onChartStateChanged: (com.woshiwangnima.healthdietpro.model.chart.ComposeChartState) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedDateMillis by rememberSaveable { mutableStateOf(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedDate = remember(selectedDateMillis) {
        Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val dayStart = remember(selectedDate) { selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val nextDayStart = remember(selectedDate) { selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val seriesLabel = stringResource(R.string.blood_glucose_value)
    val delayedSeriesLabel = stringResource(R.string.blood_glucose_delayed_series)
    val unit = stringResource(R.string.blood_glucose_unit)
    val valueWithUnitFormat = stringResource(R.string.blood_glucose_value_with_unit)
    val data = remember(records, dayStart, nextDayStart) {
        records.filter { it.timestamp in dayStart until nextDayStart }.sortedBy { it.timestamp }.map { record ->
            DataPoint(record.timestamp, record.valueMmolPerL.toFloat(), formatBloodGlucoseTime(record.timestamp))
        }
    }
    val delayedData = remember(records, dayStart) {
        records.filter { it.timestamp in dayStart - 86_400_000L until dayStart }
            .sortedBy { it.timestamp }
            .map { record -> DataPoint(record.timestamp + 86_400_000L, record.valueMmolPerL.toFloat(), formatBloodGlucoseTime(record.timestamp + 86_400_000L)) }
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
    val delayedSeries = remember(delayedData, context, delayedSeriesLabel) {
        ChartSeries(
            points = delayedData,
            label = delayedSeriesLabel,
            color = ContextCompat.getColor(context, R.color.secondary),
            lineStyle = LineStyle.STEPPED_FRONT,
            lineType = LineType.DASHED,
            pointShape = PointShape.CIRCLE,
            pointFill = PointFill.HOLLOW,
        )
    }
    Column(Modifier.fillMaxSize()) {
        AppIconTextButton(
            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            iconRes = R.drawable.ic_birthday,
            onClick = { showDatePicker = true },
        )
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
            series = listOf(series, delayedSeries),
            xAxisLabel = stringResource(R.string.chart_axis_time_unit),
            yAxisLabel = stringResource(R.string.blood_glucose_unit),
            titleVisible = false,
        ),
            chartState = chartState,
            onChartStateChanged = onChartStateChanged,
            modifier = Modifier.weight(1f),
        )
    }
    if (showDatePicker) {
        ComposeDatePickerDialog(selectedDateMillis, { showDatePicker = false }) { selected ->
            selectedDateMillis = selected.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            showDatePicker = false
        }
    }
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var unitId by rememberSaveable { mutableStateOf(AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)) }
    var value by rememberSaveable(record?.id) { mutableStateOf(record?.valueMmolPerL?.let { UnitConverter.fromBase(UnitCategoryType.Glucose.id, it.toFloat(), unitId).toString() }.orEmpty()) }
    var anchor by rememberSaveable(record?.id) { mutableStateOf(record?.timingAnchor) }
    var relativeMinutes by rememberSaveable(record?.id) { mutableStateOf(record?.relativeMinutes?.let { kotlin.math.abs(it).toString() }.orEmpty()) }
    var timingRelation by rememberSaveable(record?.id) { mutableStateOf(record?.relativeMinutes?.timingRelation() ?: BloodGlucoseTimingRelation.At) }
    var note by rememberSaveable(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    val validValue = value.toDoubleOrNull()?.let { UnitConverter.toBase(UnitCategoryType.Glucose.id, it.toFloat(), unitId).toDouble() }?.takeIf(::isValidBloodGlucoseValue)
    val invalidValue = value.isNotBlank() && validValue == null
    val invalidRelativeMinutes = timingRelation != BloodGlucoseTimingRelation.At && (relativeMinutes.toIntOrNull()?.let { it > 0 } != true)
    val glucoseRange = if (unitId == "mg_dl") "20-600 mg/dL" else "1.1-33.3 mmol/L"

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
                    EditorTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = stringResource(R.string.blood_glucose_value),
                        required = true,
                        numeric = true,
                        suffix = { Text(glucoseUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId) },
                        supportingTextOverride = { Text(if (invalidValue) stringResource(R.string.blood_glucose_value_invalid) else stringResource(R.string.blood_glucose_value_range_example, glucoseRange, if (unitId == "mg_dl") "100" else "5.6")) },
                    )
                }
                item {
                    AppDropdownField(
                        label = stringResource(R.string.blood_glucose_unit),
                        value = glucoseUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId,
                        options = glucoseUnitOptions(),
                        onSelect = { selected ->
                            value.toDoubleOrNull()?.let { current -> value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, UnitConverter.toBase(UnitCategoryType.Glucose.id, current.toFloat(), unitId), selected.id).toString() }
                            unitId = selected.id
                            AppPrefs.setUnit(context, UnitCategoryType.Glucose.id, unitId)
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
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BloodGlucoseTimingRelation.entries.forEach { relation ->
                            FilterChip(selected = timingRelation == relation, onClick = { timingRelation = relation }, label = { Text(stringResource(relation.labelRes())) })
                        }
                    }
                }
                if (timingRelation != BloodGlucoseTimingRelation.At) item {
                    OutlinedTextField(
                        value = relativeMinutes,
                        onValueChange = { relativeMinutes = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.blood_glucose_minutes)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = invalidRelativeMinutes,
                        supportingText = if (invalidRelativeMinutes) { { Text(stringResource(R.string.blood_glucose_relative_minutes_invalid)) } } else null,
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
                    val minutes = when (timingRelation) {
                        BloodGlucoseTimingRelation.Before -> -requireNotNull(relativeMinutes.toIntOrNull())
                        BloodGlucoseTimingRelation.After -> requireNotNull(relativeMinutes.toIntOrNull())
                        BloodGlucoseTimingRelation.At -> 0
                    }
                    onSave(BloodGlucoseRecord(record?.id ?: UUID.randomUUID().toString(), timestamp, requireNotNull(validValue), anchor, minutes, note.trim()))
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
private fun glucoseUnitOptions(): List<AppDropdownOption> = UnitConverter.getRepository()?.getCategory(UnitCategoryType.Glucose.id)?.units
    ?.filterNot { it.hidden }
    ?.map { AppDropdownOption(it.id, it.symbol(Locale.getDefault())) }
    .orEmpty()

@Composable
private fun BloodGlucoseRecord.periodLabel(): String {
    val anchorText = timingAnchor?.let { stringResource(it.labelRes()) }.orEmpty()
    val offsetText = relativeMinutes?.let { minutes ->
        when (minutes.timingRelation()) {
            BloodGlucoseTimingRelation.Before -> stringResource(R.string.blood_glucose_period_before, -minutes)
            BloodGlucoseTimingRelation.After -> stringResource(R.string.blood_glucose_period_after, minutes)
            BloodGlucoseTimingRelation.At -> stringResource(R.string.blood_glucose_period_at)
        }
    }.orEmpty()
    return listOf(anchorText, offsetText)
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifEmpty { stringResource(R.string.blood_glucose_period_none) }
}

private enum class BloodGlucoseTimingRelation { Before, After, At }

private fun Int.timingRelation(): BloodGlucoseTimingRelation = when {
    this < 0 -> BloodGlucoseTimingRelation.Before
    this > 0 -> BloodGlucoseTimingRelation.After
    else -> BloodGlucoseTimingRelation.At
}

private fun BloodGlucoseTimingRelation.labelRes(): Int = when (this) {
    BloodGlucoseTimingRelation.Before -> R.string.blood_glucose_relation_before
    BloodGlucoseTimingRelation.After -> R.string.blood_glucose_relation_after
    BloodGlucoseTimingRelation.At -> R.string.blood_glucose_relation_at
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

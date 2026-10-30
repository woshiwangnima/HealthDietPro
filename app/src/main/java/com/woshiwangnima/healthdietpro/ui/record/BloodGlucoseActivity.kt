package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import android.Manifest
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableReorder
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppDestructiveTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppNumericStepperField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputKind
import com.woshiwangnima.healthdietpro.common.ui.NumericInputSpec
import com.woshiwangnima.healthdietpro.common.ui.ParticleValueOrb
import com.woshiwangnima.healthdietpro.common.ui.TrendIndicatorArrowPreview
import com.woshiwangnima.healthdietpro.common.ui.TextInputField
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.RecordTimePickerField
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.SingleChoiceSegmentedOption
import com.woshiwangnima.healthdietpro.common.ui.SingleChoiceSegmentedSelector
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadioRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSwitchRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.common.ui.recordLatestUpdatePrefix
import com.woshiwangnima.healthdietpro.common.range.UnitRange
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodHbA1cRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseSource
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTimingDefaults
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTimingAnchor
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseDiabetesType
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseAlertMode
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseReminderSettings
import com.woshiwangnima.healthdietpro.model.bloodglucose.isValidBloodGlucoseValue
import com.woshiwangnima.healthdietpro.model.bloodglucose.isValidHbA1cValue
import com.woshiwangnima.healthdietpro.model.bloodglucose.normalizeBloodGlucoseTimestamp
import com.woshiwangnima.healthdietpro.model.bloodglucose.bloodGlucoseInputRange
import com.woshiwangnima.healthdietpro.model.bloodglucose.bloodGlucoseParticleLevel
import com.woshiwangnima.healthdietpro.model.bloodglucose.bloodGlucoseTrendRateRanges
import com.woshiwangnima.healthdietpro.model.bloodglucose.hbA1cInputRange
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.formatRelativeTimeOffset
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.model.unit.UnitStepMode
import com.woshiwangnima.healthdietpro.model.unit.stepSpec
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.ui.event.EventScreen
import com.woshiwangnima.healthdietpro.ui.event.EventViewModel
import com.woshiwangnima.healthdietpro.ui.event.EventInfoScreen
import com.woshiwangnima.healthdietpro.ui.event.EventGlucoseAnalysisScreen
import com.woshiwangnima.healthdietpro.ui.event.DietEventGlucoseAnalysisScreen
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.common.time.resolve
import com.woshiwangnima.healthdietpro.model.unit.formatGlucoseValue
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.math.abs
import kotlin.math.round

class BloodGlucoseActivity : BaseActivity() {
    companion object {
        const val EXTRA_OPEN_EDITOR = "open_editor"
    }

    private val viewModel: BloodGlucoseViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels { EventViewModel.Factory(application) }

    private fun openRecordAction(action: RecordActionId) {
        when (action) {
            RecordActionId.Medication -> startActivity(android.content.Intent(this, MedicationListActivity::class.java))
            RecordActionId.Diet -> startActivity(android.content.Intent(this, com.woshiwangnima.healthdietpro.ui.diet.DietRecordActivity::class.java))
            RecordActionId.Sleep -> startActivity(android.content.Intent(this, com.woshiwangnima.healthdietpro.ui.sleep.SleepRecordActivity::class.java))
            else -> return
        }
        finish()
    }

    private fun openDietDetail(recordId: String) {
        startActivity(
            android.content.Intent(this, com.woshiwangnima.healthdietpro.ui.diet.DietRecordActivity::class.java)
                .putExtra(com.woshiwangnima.healthdietpro.ui.diet.DietRecordActivity.EXTRA_OPEN_RECORD_ID, recordId),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                // 本文件下的子屏（设置 / 来源 / 提醒 / 目标范围 / 录入）都已迁到 AppTheme，
                // 在 Activity 里套一层即可，不必每个子屏各套一次。
                AppTheme(darkTheme = rememberAppDarkTheme()) {
                    BloodGlucoseScreen(
                        viewModel = viewModel,
                        eventViewModel = eventViewModel,
                        onBack = ::finish,
                        onOpenRecordAction = ::openRecordAction,
                        onOpenDietDetail = ::openDietDetail,
                        openEditorInitially = intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        eventViewModel.refresh()
    }
}

@Composable
private fun BloodGlucoseScreen(
    viewModel: BloodGlucoseViewModel,
    eventViewModel: EventViewModel,
    onBack: () -> Unit,
    onOpenRecordAction: (RecordActionId) -> Unit,
    onOpenDietDetail: (String) -> Unit,
    openEditorInitially: Boolean,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val records by viewModel.records.collectAsStateWithLifecycle()
    val hbA1cRecords by viewModel.hbA1cRecords.collectAsStateWithLifecycle()
    val diabetesType by viewModel.diabetesType.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val chartScope by viewModel.chartScope.collectAsStateWithLifecycle()
    val chartWindow by viewModel.chartWindow.collectAsStateWithLifecycle()
    val chartWindowEnd by viewModel.chartWindowEnd.collectAsStateWithLifecycle()
    val chartStyle by viewModel.chartStyle.collectAsStateWithLifecycle()
    val predictionPoints by viewModel.predictionPoints.collectAsStateWithLifecycle()
    val predictionConfidence by viewModel.predictionConfidence.collectAsStateWithLifecycle()
    val predictionGenerating by viewModel.predictionGenerating.collectAsStateWithLifecycle()
    val predictionGenerationStatus by viewModel.predictionGenerationStatus.collectAsStateWithLifecycle()
    val predictionEligibility by viewModel.predictionEligibility.collectAsStateWithLifecycle()
    val predictionFormula by viewModel.predictionFormula.collectAsStateWithLifecycle()
    val eventUiState by eventViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedRecordType by rememberSaveable { mutableIntStateOf(0) }
    var editingRecord by remember { mutableStateOf<BloodGlucoseRecord?>(null) }
    var editingHbA1cRecord by remember { mutableStateOf<BloodHbA1cRecord?>(null) }
    var editingSourceId by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(openEditorInitially) }
    var showHbA1cEditor by remember { mutableStateOf(false) }
    var route by rememberSaveable { mutableStateOf(BloodGlucoseRoute.Records) }
    var selectedDietEvent by remember { mutableStateOf<DietRecord?>(null) }
    val glucoseUnitId = AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)
    val glucoseUnitLabel = UnitConverter.getRepository()?.getUnit(UnitCategoryType.Glucose.id, glucoseUnitId)
        ?.symbol(Locale.getDefault()) ?: glucoseUnitId
    val tabs = remember {
        listOf(
            DetailTabItem("chart", R.string.detail_tab_chart, R.drawable.ic_chart),
            DetailTabItem("data", R.string.detail_tab_data, R.drawable.ic_list),
            DetailTabItem("events", R.string.blood_glucose_events_title, R.drawable.ic_event),
        )
    }

    if (showEditor) {
        BloodGlucoseEditorScreen(
            record = editingRecord,
            sources = sources,
            timingDefaults = BloodGlucoseTimingDefaults(),
            referenceRange = diabetesType.targetRange(AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)),
            onBack = { if (openEditorInitially) onBack() else showEditor = false },
            onSave = { record ->
                viewModel.upsert(record)
                showEditor = false
            },
        )
        return
    }
    if (showHbA1cEditor) {
        BloodHbA1cEditorScreen(
            record = editingHbA1cRecord,
            sources = sources,
            timingDefaults = BloodGlucoseTimingDefaults(),
            referenceRange = diabetesType.hbA1cReferenceRange,
            onBack = { showHbA1cEditor = false },
            onSave = { record ->
                viewModel.upsertHbA1c(record)
                showHbA1cEditor = false
            },
        )
        return
    }
    BackHandler(enabled = route != BloodGlucoseRoute.Records) {
        route = when (route) {
            BloodGlucoseRoute.Targets -> BloodGlucoseRoute.Settings
            BloodGlucoseRoute.Reminders -> BloodGlucoseRoute.Settings
            BloodGlucoseRoute.Sources -> BloodGlucoseRoute.Settings
            BloodGlucoseRoute.SourceEditor -> BloodGlucoseRoute.Sources
            BloodGlucoseRoute.EventInfo, BloodGlucoseRoute.DataInfo, BloodGlucoseRoute.DietEventAnalysis, BloodGlucoseRoute.SleepEventAnalysis, BloodGlucoseRoute.Settings, BloodGlucoseRoute.Records -> BloodGlucoseRoute.Records
        }
    }

    if (route == BloodGlucoseRoute.Settings) {
        BloodGlucoseSettingsScreen(onBack = { route = BloodGlucoseRoute.Records }, onOpenTargets = { route = BloodGlucoseRoute.Targets }, onOpenReminders = { route = BloodGlucoseRoute.Reminders }, onOpenSources = { route = BloodGlucoseRoute.Sources })
        return
    }
    if (route == BloodGlucoseRoute.Targets) {
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
    if (route == BloodGlucoseRoute.Sources) {
        BloodGlucoseSourceSettingsScreen(
            sources = sources,
            records = records,
            hbA1cRecords = hbA1cRecords,
            onBack = { route = BloodGlucoseRoute.Settings },
            onAdd = { editingSourceId = null; route = BloodGlucoseRoute.SourceEditor },
            onEdit = { editingSourceId = it.id; route = BloodGlucoseRoute.SourceEditor },
            onDelete = { id -> viewModel.saveSources(sources.filterNot { it.id == id }) },
            onDeleteData = viewModel::deleteDataForSource,
            onReorder = { ordered -> viewModel.reorderSources(ordered.map(BloodGlucoseSource::id)) },
        )
        return
    }
    if (route == BloodGlucoseRoute.SourceEditor) {
        BloodGlucoseSourceEditorScreen(
            source = sources.firstOrNull { it.id == editingSourceId },
            onBack = { route = BloodGlucoseRoute.Sources },
            onSave = { originalId, source ->
                viewModel.saveSources(
                    if (originalId == null) sources + source
                    else sources.map { existing -> if (existing.id == originalId) source else existing },
                )
                route = BloodGlucoseRoute.Sources
            },
        )
        return
    }
    if (route == BloodGlucoseRoute.EventInfo) {
        EventInfoScreen(
            onBack = { route = BloodGlucoseRoute.Records },
            onOpenRecordAction = onOpenRecordAction,
        )
        return
    }
    if (route == BloodGlucoseRoute.DataInfo) {
        BloodGlucoseDataInfoScreen(onBack = { route = BloodGlucoseRoute.Records })
        return
    }
    if (route == BloodGlucoseRoute.DietEventAnalysis) {
        selectedDietEvent?.let { dietEvent ->
            DietEventGlucoseAnalysisScreen(
                record = dietEvent,
                bloodGlucoseRecords = records,
                unitId = glucoseUnitId,
                unitLabel = glucoseUnitLabel,
                targetRange = diabetesType.glucoseReferenceRangeMmolPerL,
                onBack = { route = BloodGlucoseRoute.Records },
                sameDayRecords = eventUiState.dietRecords,
                onRecordSelected = { selectedDietEvent = it },
                onOpenDietDetail = onOpenDietDetail,
            )
        } ?: EventGlucoseAnalysisScreen(
            title = stringResource(R.string.blood_glucose_diet_event_analysis_title),
            onBack = { route = BloodGlucoseRoute.Records },
        )
        return
    }
    if (route == BloodGlucoseRoute.SleepEventAnalysis) {
        EventGlucoseAnalysisScreen(
            title = stringResource(R.string.blood_glucose_sleep_event_analysis_title),
            onBack = { route = BloodGlucoseRoute.Records },
        )
        return
    }

    BaseScreen(
        title = stringResource(R.string.blood_glucose_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
        actions = {
            if (selectedTab == 2) {
                IconButton(onClick = { route = BloodGlucoseRoute.EventInfo }) {
                    Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.blood_glucose_events_info_title))
                }
            }
            if (selectedTab == 1) {
                IconButton(onClick = { route = BloodGlucoseRoute.DataInfo }) {
                    Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.blood_glucose_data_info_title))
                }
            }
            IconButton(onClick = { route = BloodGlucoseRoute.Settings }) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.blood_glucose_settings_title))
            }
        },
    ) { padding ->
        val tokens = LocalAppTokens.current
        Column(Modifier.fillMaxSize().background(tokens.colors.bg).padding(padding)) {
            AnimatedPageContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                direction = { initialTab, targetTab -> targetTab - initialTab },
            ) { tab ->
                if (tab == 0) {
                    BloodGlucoseChart(
                        records = records,
                        diabetesType = diabetesType,
                        scope = chartScope,
                        window = chartWindow,
                        windowEnd = chartWindowEnd,
                        chartStyle = chartStyle,
                        predictionPoints = predictionPoints,
                        predictionGenerating = predictionGenerating,
                        predictionGenerationStatus = predictionGenerationStatus,
                        predictionEligibility = predictionEligibility,
                        predictionConfidence = predictionConfidence,
                        predictionFormula = predictionFormula,
                        onScopeChanged = viewModel::setChartScope,
                        onWindowChanged = viewModel::setChartWindow,
                        onWindowEndChanged = viewModel::setChartWindowEnd,
                        onChartStyleChanged = viewModel::setChartStyle,
                        onGeneratePrediction = viewModel::generatePrediction,
                        onPredictionFormulaDismissed = viewModel::consumePredictionFormula,
                    )
                } else {
                    when (tab) {
                        1 -> BloodGlucoseDataPage(
                            records = records,
                            hbA1cRecords = hbA1cRecords,
                            sources = sources,
                            diabetesType = diabetesType,
                            selectedRecordType = selectedRecordType,
                            onRecordTypeChange = { selectedRecordType = it },
                            onAdd = { editingRecord = null; showEditor = true },
                            onEdit = { editingRecord = it; showEditor = true },
                            onDelete = viewModel::delete,
                            onAddHbA1c = { editingHbA1cRecord = null; showHbA1cEditor = true },
                            onEditHbA1c = { editingHbA1cRecord = it; showHbA1cEditor = true },
                            onDeleteHbA1c = viewModel::deleteHbA1c,
                        )
                        2 -> EventScreen(
                            viewModel = eventViewModel,
                            onOpenDietAnalysis = { dietEvent ->
                                selectedDietEvent = dietEvent
                                route = BloodGlucoseRoute.DietEventAnalysis
                            },
                            onOpenSleepAnalysis = { route = BloodGlucoseRoute.SleepEventAnalysis },
                        )
                    }
                }
            }
            DetailTabBar(items = tabs, selectedId = tabs[selectedTab].id) { item ->
                selectedTab = tabs.indexOf(item)
            }
        }
    }
}

private enum class BloodGlucoseRoute { Records, Settings, Targets, Reminders, Sources, SourceEditor, EventInfo, DataInfo, DietEventAnalysis, SleepEventAnalysis }

@Composable
private fun BloodGlucoseDataInfoScreen(onBack: () -> Unit) {
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.blood_glucose_data_info_title), onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(tokens.colors.bg).padding(padding),
            contentPadding = PaddingValues(
                start = tokens.spacing.page,
                end = tokens.spacing.page,
                top = tokens.spacing.s4,
                bottom = tokens.spacing.s4,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            item { BloodGlucoseDataInfoSection(R.string.blood_glucose_glucose, R.string.blood_glucose_data_info_glucose) }
            item { BloodGlucoseDataInfoSection(R.string.blood_glucose_hb_a1c, R.string.blood_glucose_data_info_hb_a1c) }
            item { BloodGlucoseDataInfoSection(R.string.blood_glucose_ogtt_full, R.string.blood_glucose_data_info_ogtt) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
                    Text(
                        text = stringResource(R.string.blood_glucose_data_info_disclaimer),
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                    )
                    Text(
                        text = stringResource(R.string.blood_glucose_data_info_source),
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun BloodGlucoseDataInfoSection(
    @androidx.annotation.StringRes title: Int,
    @androidx.annotation.StringRes text: Int,
) {
    val tokens = LocalAppTokens.current
    AppSectionGroup {
        Column(
            modifier = Modifier.padding(tokens.spacing.s3),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        ) {
            Text(
                text = stringResource(title),
                style = tokens.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.colors.text,
            )
            Text(
                text = stringResource(text),
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
            )
        }
    }
}

@Composable
private fun BloodGlucoseSettingsScreen(onBack: () -> Unit, onOpenTargets: () -> Unit, onOpenReminders: () -> Unit, onOpenSources: () -> Unit) {
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.blood_glucose_settings_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
        ) {
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.blood_glucose_target_range_settings),
                    tooltip = stringResource(R.string.blood_glucose_target_range_settings_desc),
                    iconRes = R.drawable.ic_blood_glucose,
                    showChevron = true,
                    onClick = onOpenTargets,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.blood_glucose_source_settings),
                    tooltip = stringResource(R.string.blood_glucose_source_settings_desc),
                    iconRes = R.drawable.ic_edit,
                    showChevron = true,
                    onClick = onOpenSources,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.blood_glucose_reminder_settings),
                    tooltip = stringResource(R.string.blood_glucose_reminder_settings_desc),
                    iconRes = R.drawable.ic_notification,
                    showChevron = true,
                    onClick = onOpenReminders,
                )
                AppListRowDivider()
                // 免打扰还没有实现，不给箭头也不接收点击：带箭头却点不动比没有箭头更让人困惑。
                AppListRow(
                    title = stringResource(R.string.blood_glucose_do_not_disturb_settings),
                    tooltip = stringResource(R.string.blood_glucose_do_not_disturb_settings_desc),
                    iconRes = R.drawable.ic_do_not_disturb,
                )
            }
        }
    }
}

@Composable
private fun BloodGlucoseSourceSettingsScreen(
    sources: List<BloodGlucoseSource>,
    records: List<BloodGlucoseRecord>,
    hbA1cRecords: List<BloodHbA1cRecord>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (BloodGlucoseSource) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteData: (String) -> Unit,
    onReorder: (List<BloodGlucoseSource>) -> Unit,
) {
    var deleting by remember { mutableStateOf<BloodGlucoseSource?>(null) }
    var deletingData by remember { mutableStateOf<BloodGlucoseSource?>(null) }
    var orderedSources by remember(sources) { mutableStateOf(sources) }
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.blood_glucose_source_settings), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding)
                .padding(tokens.spacing.page),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                AppIconTextButton(stringResource(R.string.blood_glucose_source_add), R.drawable.ic_add, onAdd)
            }
            if (orderedSources.isEmpty()) {
                AppEmptyState(
                    title = stringResource(R.string.blood_glucose_source_empty),
                    description = stringResource(R.string.blood_glucose_source_settings_desc),
                    iconRes = R.drawable.ic_edit,
                )
            } else {
                AppDataTable(
                    rows = orderedSources,
                    rowKey = { _, source -> source.id },
                    modifier = Modifier.weight(1f),
                    showPager = false,
                    actionsWidth = 220.dp,
                    columns = listOf(
                        AppDataTableColumn("source", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_source_note)) }, ColumnWidth.Fixed(220.dp)) { source -> AppDataTableText(source.note) },
                    ),
                    actionsHeader = { AppDataTableHeaderText(stringResource(R.string.blood_glucose_source_actions)) },
                    rowActions = { source ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppDestructiveTextButton(
                                text = stringResource(R.string.blood_glucose_source_delete_data),
                                onClick = { deletingData = source },
                                modifier = Modifier.weight(1f),
                                iconSize = 16.dp,
                            )
                            AppDestructiveTextButton(
                                text = stringResource(R.string.blood_glucose_source_delete),
                                onClick = { deleting = source },
                                modifier = Modifier.weight(1f),
                                iconSize = 16.dp,
                            )
                        }
                    },
                    onRowClick = onEdit,
                    reorder = AppDataTableReorder(
                        onMove = { from, to -> orderedSources = orderedSources.toMutableList().apply { add(to, removeAt(from)) } },
                        onMoveFinished = { onReorder(orderedSources) },
                    ),
                )
            }
        }
    }
    deleting?.let { source ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.blood_glucose_source_delete_message)) },
            confirmButton = { TextButton(onClick = { onDelete(source.id); deleting = null }) { Text(stringResource(R.string.blood_glucose_source_delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } },
        )
    }
    deletingData?.let { source ->
        val glucoseCount = records.count { it.sourceId == source.id }
        val hbA1cCount = hbA1cRecords.count { it.sourceId == source.id }
        AlertDialog(
            onDismissRequest = { deletingData = null },
            title = { Text(stringResource(R.string.blood_glucose_source_delete_data_title)) },
            text = { Text(stringResource(R.string.blood_glucose_source_delete_data_message, glucoseCount, hbA1cCount, source.note)) },
            confirmButton = { TextButton(onClick = { onDeleteData(source.id); deletingData = null }) { Text(stringResource(R.string.blood_glucose_source_delete_data)) } },
            dismissButton = { TextButton(onClick = { deletingData = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } },
        )
    }
}

@Composable
private fun BloodGlucoseSourceEditorScreen(
    source: BloodGlucoseSource?,
    onBack: () -> Unit,
    onSave: (String?, BloodGlucoseSource) -> Unit,
) {
    var note by rememberSaveable(source?.id) { mutableStateOf(source?.note.orEmpty()) }
    val current = note.trim().takeIf(String::isNotBlank)?.let { BloodGlucoseSource(source?.id ?: UUID.randomUUID().toString(), it) }
    val hasChanges = current != source
    val saveEnabled = current != null && hasChanges
    var showDiscardDialog by rememberSaveable(source?.id) { mutableStateOf(false) }
    fun save() { current?.let { onSave(source?.id, it) } }
    fun requestBack() { if (hasChanges) showDiscardDialog = true else onBack() }
    val tokens = LocalAppTokens.current
    BackHandler(onBack = ::requestBack)
    BaseScreen(title = stringResource(if (source == null) R.string.blood_glucose_source_add else R.string.blood_glucose_source_edit), onBack = ::requestBack) { padding ->
        Column(Modifier.fillMaxSize().background(tokens.colors.bg).padding(padding)) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
            ) {
                item {
                    TextInputField(
                        label = stringResource(R.string.blood_glucose_source_note),
                        value = note,
                        onValueChange = { note = it },
                    )
                }
            }
            FormSaveBar(stringResource(R.string.blood_glucose_save), saveEnabled, ::save)
        }
    }
    if (showDiscardDialog) DiscardChangesDialog(onDiscard = onBack, onSave = ::save, onDismiss = { showDiscardDialog = false }, saveEnabled = saveEnabled)
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
        val tokens = LocalAppTokens.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(tokens.colors.bg).padding(padding),
            contentPadding = PaddingValues(
                start = tokens.spacing.page,
                end = tokens.spacing.page,
                top = tokens.spacing.s4,
                bottom = tokens.spacing.s4,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            item {
                BloodGlucoseReminderCard(
                    title = stringResource(R.string.blood_glucose_alert_high_enabled),
                    checked = settings.highEnabled,
                    onCheckedChange = { requestPermissionWhenEnabled(it); update(settings.copy(highEnabled = it)) },
                ) {
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
                BloodGlucoseReminderCard(
                    title = stringResource(R.string.blood_glucose_alert_low_enabled),
                    checked = settings.lowEnabled,
                    onCheckedChange = { requestPermissionWhenEnabled(it); update(settings.copy(lowEnabled = it)) },
                ) {
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
                BloodGlucoseReminderCard(
                    title = stringResource(R.string.blood_glucose_alert_emergency_low_enabled),
                    checked = settings.emergencyLowEnabled,
                    onCheckedChange = { requestPermissionWhenEnabled(it); update(settings.copy(emergencyLowEnabled = it)) },
                ) {
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
private fun BloodGlucoseReminderCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    // 卡内零内边距：开关行自带 s3 水平内边距，字段区再单独补，否则开关会比输入框多缩进一截。
    AppSectionGroup(contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(vertical = tokens.spacing.s1)) {
            AppSwitchRow(
                title = title,
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                content = content,
            )
        }
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
    BloodGlucoseReminderCard(
        title = toggleTitle,
        checked = enabled,
        onCheckedChange = onEnabledChange,
    ) {
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
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.blood_glucose_target_range_settings), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().background(tokens.colors.bg).padding(padding)) {
            AppSectionGroup(
                modifier = Modifier.padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                ),
            ) {
                AppListRow(
                    title = stringResource(R.string.blood_glucose_diabetes_type),
                    tooltip = stringResource(R.string.blood_glucose_diabetes_type_desc),
                    iconRes = R.drawable.ic_medical_history,
                    trailingValue = stringResource(diabetesType.labelRes()),
                    showChevron = true,
                    onClick = { showTypePicker = true },
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.blood_glucose_glucose_reference_range),
                    tooltip = stringResource(R.string.blood_glucose_target_range_desc),
                    iconRes = R.drawable.ic_blood_glucose,
                    trailingValue = rangeText(targetRange, unitLabel),
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.blood_glucose_hb_a1c_reference_range),
                    tooltip = stringResource(R.string.blood_glucose_target_range_desc),
                    iconRes = R.drawable.ic_blood_glucose,
                    trailingValue = rangeText(diabetesType.hbA1cReferenceRange, "%"),
                )
            }
            Text(
                text = stringResource(R.string.blood_glucose_target_range_recommendations),
                style = tokens.type.bodyS.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.colors.text,
                modifier = Modifier.padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s2,
                ),
            )
            AppDataTable(
                rows = BloodGlucoseDiabetesType.entries.toList(),
                rowKey = { _, type -> type.name },
                columns = listOf(
                    AppDataTableColumn("type", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_diabetes_type)) }, ColumnWidth.Fixed(180.dp)) { AppDataTableText(stringResource(it.labelRes())) },
                    AppDataTableColumn("glucoseRange", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_glucose_reference_range)) }, ColumnWidth.Fixed(180.dp)) { AppDataTableText(rangeText(it.targetRange(unitId), unitLabel), numeric = true) },
                    AppDataTableColumn("hbA1cRange", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_hb_a1c_reference_range)) }, ColumnWidth.Fixed(180.dp)) { AppDataTableText(rangeText(it.hbA1cReferenceRange, "%"), numeric = true) },
                ),
                showRowNumber = false,
                showPager = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (showTypePicker) {
        val dialogTokens = LocalAppTokens.current
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
                                Text(
                                    stringResource(type.labelRes()),
                                    style = dialogTokens.type.body,
                                    color = if (type.available) dialogTokens.colors.text else dialogTokens.colors.textMuted,
                                )
                                if (!type.available) Text(
                                    stringResource(R.string.blood_glucose_diabetes_type_unavailable),
                                    style = dialogTokens.type.caption,
                                    color = dialogTokens.colors.textMuted,
                                )
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

private fun <T> rangeText(range: UnitRange<T>, unitLabel: String): String where T : Number, T : Comparable<T> {
    val min = range.min?.toDouble()?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "-∞"
    val max = range.max?.toDouble()?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "∞"
    return "${if (range.minInclusive) '[' else '('}$min, $max${if (range.maxInclusive) ']' else ')'} $unitLabel"
}

@Composable
private fun BloodGlucoseChart(
    records: List<BloodGlucoseRecord>,
    diabetesType: BloodGlucoseDiabetesType,
    scope: com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection,
    window: com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartWindow,
    windowEnd: Long?,
    chartStyle: com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartStylePrefs,
    predictionPoints: List<com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionPoint>,
    predictionGenerating: Boolean,
    predictionGenerationStatus: com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionGenerationStatus,
    predictionEligibility: com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionEligibility,
    predictionConfidence: com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionConfidence?,
    predictionFormula: com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionFormula?,
    onScopeChanged: (com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection) -> Unit,
    onWindowChanged: (com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartWindow) -> Unit,
    onWindowEndChanged: (Long?) -> Unit,
    onChartStyleChanged: (com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartStylePrefs) -> Unit,
    onGeneratePrediction: () -> Unit,
    onPredictionFormulaDismissed: () -> Unit,
) {
    var displayedPredictionFormula by remember { mutableStateOf<com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionFormula?>(null) }
    val tokens = LocalAppTokens.current
    LaunchedEffect(predictionFormula, predictionGenerating) {
        if (!predictionGenerating && predictionFormula != null) {
            displayedPredictionFormula = predictionFormula
        }
    }
    val resolvedScope = scope.resolve()
    val effectiveScopeEnd = resolvedScope.endMillis +
        com.woshiwangnima.healthdietpro.model.bloodglucose.PREDICTION_DURATION_MILLIS
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().then(if (predictionGenerating) Modifier.blur(8.dp) else Modifier)) {
            com.woshiwangnima.healthdietpro.common.ui.RecordTimeRangeFilter(scope, onScopeChanged)
            val windowOptions = com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartWindow.entries.map { option ->
                SingleChoiceSegmentedOption(
                    id = option.name,
                    labelRes = R.string.blood_glucose_chart_window_hours,
                    labelArgs = listOf(option.durationMillis / 3_600_000L),
                )
            }
            SingleChoiceSegmentedSelector(
                options = windowOptions,
                selectedId = window.name,
                onOptionSelected = { selected ->
                    onWindowChanged(com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartWindow.valueOf(selected.id))
                },
                modifier = Modifier.padding(vertical = 8.dp),
            )
            val predictionStatusRes = when (predictionGenerationStatus) {
            com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionGenerationStatus.INSUFFICIENT_DATA -> R.string.blood_glucose_prediction_status_insufficient_data
            com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionGenerationStatus.FAILED -> R.string.blood_glucose_prediction_status_failed
            else -> null
        }
            predictionStatusRes?.let { statusRes ->
                Text(
                    stringResource(statusRes),
                    style = tokens.type.caption,
                    color = tokens.colors.danger,
                    modifier = Modifier.align(Alignment.End).padding(bottom = tokens.spacing.s1),
                )
            }
            BloodGlucoseFixedWindowChart(
                records = records,
                scopeStart = resolvedScope.startMillis,
                scopeEnd = effectiveScopeEnd,
                window = window,
                sessionWindowEnd = windowEnd,
                onSessionWindowEndChanged = onWindowEndChanged,
                diabetesType = diabetesType,
                chartStyle = chartStyle,
                onChartStyleChanged = onChartStyleChanged,
                predictionPoints = predictionPoints,
                predictionGenerating = predictionGenerating,
                onGeneratePrediction = onGeneratePrediction,
                predictionEligibility = predictionEligibility,
                predictionConfidence = predictionConfidence,
                modifier = Modifier.weight(1f),
            )
        }
        if (predictionGenerating) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(tokens.colors.panel.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    TextOverflowText(
                        text = stringResource(R.string.blood_glucose_prediction_generating),
                        style = tokens.type.body,
                        modifier = Modifier.padding(top = tokens.spacing.s3),
                    )
                }
            }
        }
        displayedPredictionFormula?.let { formula ->
            PredictionFormulaDialog(
                formula = formula,
                onDismiss = {
                    displayedPredictionFormula = null
                    onPredictionFormulaDismissed()
                },
            )
        }
    }
}

@Composable
private fun PredictionFormulaDialog(
    formula: com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionFormula,
    onDismiss: () -> Unit,
) {
    val number = { value: Double -> String.format(Locale.getDefault(), "%.3f", value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.blood_glucose_prediction_formula_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PredictionFormulaSection(
                    title = stringResource(R.string.blood_glucose_prediction_formula_total_title),
                    originalFormula = stringResource(R.string.blood_glucose_prediction_formula_total_original),
                    algebraExplanation = listOf(stringResource(R.string.blood_glucose_prediction_formula_total_algebra)),
                    substitutedFormula = null,
                )
                PredictionFormulaSection(
                    title = stringResource(R.string.blood_glucose_prediction_formula_baseline_title),
                    originalFormula = stringResource(R.string.blood_glucose_prediction_formula_baseline_original),
                    algebraExplanation = listOf(
                        stringResource(R.string.blood_glucose_prediction_formula_baseline_base_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_baseline_start_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_baseline_decay_algebra),
                    ),
                    substitutedFormula = stringResource(
                        R.string.blood_glucose_prediction_formula_baseline_substituted,
                        number(formula.baseMmolPerL),
                        number(formula.startMmolPerL),
                        number(formula.baseMmolPerL),
                        number(formula.baselineDecayPerHour),
                    ),
                )
                PredictionFormulaSection(
                    title = stringResource(R.string.blood_glucose_prediction_formula_meal_title),
                    originalFormula = stringResource(R.string.blood_glucose_prediction_formula_meal_original),
                    algebraExplanation = listOf(
                        stringResource(R.string.blood_glucose_prediction_formula_meal_carbs_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_meal_peak_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_meal_window_algebra),
                    ),
                    substitutedFormula = stringResource(
                        R.string.blood_glucose_prediction_formula_meal_substituted,
                        number(formula.mealResponseScale),
                        number(formula.carbsCoefficientPerGram),
                        number(formula.mealPeakMinutes),
                    ),
                )
                PredictionFormulaSection(
                    title = stringResource(R.string.blood_glucose_prediction_formula_drug_title),
                    originalFormula = stringResource(R.string.blood_glucose_prediction_formula_drug_original),
                    algebraExplanation = listOf(
                        stringResource(R.string.blood_glucose_prediction_formula_drug_coefficient_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_drug_peak_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_drug_duration_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_drug_window_algebra),
                    ),
                    substitutedFormula = formula.drugFormulas.filter { it.enabled }.map { drug ->
                        stringResource(
                            R.string.blood_glucose_prediction_formula_drug_substituted,
                            drug.medicationName,
                            number(drug.responseScale),
                            number(drug.coefficientPerDose),
                            number(drug.peakMinutes),
                            drug.doseValues.distinct().joinToString(", ", prefix = "{", postfix = "}", transform = number),
                            number(drug.durationMinutes),
                        )
                    }.joinToString("\n").ifBlank {
                        stringResource(R.string.blood_glucose_prediction_formula_drug_no_data)
                    },
                )
                PredictionFormulaSection(
                    title = stringResource(R.string.blood_glucose_prediction_formula_sleep_title),
                    originalFormula = stringResource(R.string.blood_glucose_prediction_formula_sleep_original),
                    algebraExplanation = listOf(
                        stringResource(R.string.blood_glucose_prediction_formula_sleep_slow_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_sleep_stress_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_sleep_hours_algebra),
                        stringResource(R.string.blood_glucose_prediction_formula_sleep_decay_algebra),
                    ),
                    substitutedFormula = if (formula.sleepFormula.enabled) stringResource(
                        R.string.blood_glucose_prediction_formula_sleep_substituted,
                        number(formula.sleepFormula.slowMmolPerL),
                        number(formula.sleepFormula.stressCoefficientMmolPerL),
                        number(formula.sleepFormula.stressDecayPerHour),
                    ) else stringResource(
                        R.string.blood_glucose_prediction_formula_sleep_no_data,
                        formula.sleepFormula.sampleCount,
                    ),
                )
                PredictionFormulaSection(
                    title = stringResource(R.string.blood_glucose_prediction_formula_exercise_title),
                    originalFormula = stringResource(R.string.blood_glucose_prediction_formula_exercise_original),
                    algebraExplanation = listOf(stringResource(R.string.blood_glucose_prediction_formula_exercise_algebra)),
                    substitutedFormula = stringResource(R.string.blood_glucose_prediction_formula_exercise_substituted),
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
private fun PredictionFormulaSection(
    title: String,
    originalFormula: String,
    algebraExplanation: List<String>,
    substitutedFormula: String?,
) {
    val tokens = LocalAppTokens.current
    AppSectionGroup {
        Column(
            modifier = Modifier.padding(tokens.spacing.s3),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        ) {
            Text(
                text = title,
                style = tokens.type.bodyS.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.colors.text,
            )
            Text(text = originalFormula, style = tokens.type.caption, color = tokens.colors.textMuted)
            // 推导步骤只缩进、不再套第二层底：两层底在同一张卡里会出现一条多余的分界线。
            Column(
                modifier = Modifier.padding(start = tokens.spacing.s2),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
            ) {
                algebraExplanation.forEach { explanation ->
                    Text(text = explanation, style = tokens.type.caption, color = tokens.colors.textMuted)
                }
            }
            substitutedFormula?.let {
                Text(text = it, style = tokens.type.caption, color = tokens.colors.text)
            }
        }
    }
}

@Composable
private fun BloodGlucoseDataPage(
    records: List<BloodGlucoseRecord>,
    hbA1cRecords: List<BloodHbA1cRecord>,
    sources: List<BloodGlucoseSource>,
    diabetesType: BloodGlucoseDiabetesType,
    selectedRecordType: Int,
    onRecordTypeChange: (Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (BloodGlucoseRecord) -> Unit,
    onDelete: (String) -> Unit,
    onAddHbA1c: () -> Unit,
    onEditHbA1c: (BloodHbA1cRecord) -> Unit,
    onDeleteHbA1c: (String) -> Unit,
) {
    var deletingRecord by remember { mutableStateOf<BloodGlucoseRecord?>(null) }
    var deletingHbA1cRecord by remember { mutableStateOf<BloodHbA1cRecord?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val unitId = AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)
    val unit = glucoseUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId
    val recordTypeLabels = listOf(
        EqualWidthTab.text(stringResource(R.string.blood_glucose_glucose)),
        EqualWidthTab.text(stringResource(R.string.blood_glucose_hb_a1c)),
        EqualWidthTab.text(stringResource(R.string.blood_glucose_ogtt)),
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(LocalAppTokens.current.colors.bg)
            .padding(
                horizontal = LocalAppTokens.current.spacing.page,
                vertical = LocalAppTokens.current.spacing.s3,
            ),
        verticalArrangement = Arrangement.spacedBy(LocalAppTokens.current.spacing.s3),
    ) {
        EqualWidthSegmentedTabs(
            tabs = recordTypeLabels,
            selectedIndex = selectedRecordType,
            onSelected = onRecordTypeChange,
        )
        when (selectedRecordType) {
            0 -> GlucoseRecordList(records, sources, diabetesType, unitId, unit, onAdd, onEdit) { deletingRecord = it }
            1 -> HbA1cRecordList(hbA1cRecords, sources, diabetesType, unitId, unit, onAddHbA1c, onEditHbA1c) { deletingHbA1cRecord = it }
            2 -> OgttRecordPlaceholder()
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
    deletingHbA1cRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deletingHbA1cRecord = null },
            title = { Text(stringResource(R.string.body_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.body_record_delete_confirm_message)) },
            confirmButton = { TextButton(onClick = { onDeleteHbA1c(record.id); deletingHbA1cRecord = null }) { Text(stringResource(R.string.body_record_delete)) } },
            dismissButton = { TextButton(onClick = { deletingHbA1cRecord = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } },
        )
    }
}

@Composable
private fun GlucoseRecordList(
    records: List<BloodGlucoseRecord>,
    sources: List<BloodGlucoseSource>,
    diabetesType: BloodGlucoseDiabetesType,
    unitId: String,
    unit: String,
    onAdd: () -> Unit,
    onEdit: (BloodGlucoseRecord) -> Unit,
    onAskDelete: (BloodGlucoseRecord) -> Unit,
) {
    val sourceNames = remember(sources) { sources.associate { it.id to it.note } }
    val noSource = stringResource(R.string.data_source_none)
    val tokens = LocalAppTokens.current
    val latestRecords = remember(records) { records.sortedByDescending(BloodGlucoseRecord::timestamp).take(2) }
    val latestRecord = latestRecords.firstOrNull()
    val particleLevel = remember(latestRecords) {
        if (latestRecords.size == 2) bloodGlucoseParticleLevel(latestRecords[1], latestRecords[0]) else 0
    }
    val particleColor = bloodGlucoseTrendColor(particleLevel)
    val trendIndicatorContentDescription = stringResource(R.string.blood_glucose_trend_indicator)
    val latestUpdatePrefix = latestRecord?.let { recordLatestUpdatePrefix(it.timestamp) }
        ?: stringResource(R.string.record_no_data)
    var showTrendIndicatorInfo by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ParticleValueOrb(
                valueLabel = latestRecord?.let { formatGlucoseValue(it.valueMmolPerL, unitId) } ?: "-",
                supportingLabel = unit,
                level = particleLevel,
                particleColor = particleColor,
                modifier = Modifier
                    .size(112.dp)
                    .semantics { contentDescription = trendIndicatorContentDescription }
                    .clickable { showTrendIndicatorInfo = true },
            )
            Text(
                latestUpdatePrefix,
                modifier = Modifier.weight(1f).padding(start = tokens.spacing.s2, end = tokens.spacing.s2),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
            )
            AppIconTextButton(stringResource(R.string.body_record_add), R.drawable.ic_add, onAdd)
        }
        if (records.isEmpty()) {
            AppEmptyState(
                title = stringResource(R.string.blood_glucose_empty),
                description = stringResource(R.string.blood_glucose_data_info_glucose),
                iconRes = R.drawable.ic_blood_glucose,
            )
        } else {
            AppDataTable(
                rows = records,
                rowKey = { _, record -> record.id },
                columns = listOf(
                    AppDataTableColumn("time", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_time)) }, ColumnWidth.Fixed(150.dp)) { AppDataTableText(formatBloodGlucoseTime(it.timestamp)) },
                    AppDataTableColumn("period", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_period)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(it.timingAnchor.periodLabel(it.relativeMinutes)) },
                    AppDataTableColumn("value", {
                        ReferenceRangeTableHeader(
                            title = stringResource(R.string.blood_glucose_value),
                            rangeText = rangeText(diabetesType.targetRange(unitId), unit),
                        )
                    }, ColumnWidth.Fixed(180.dp)) { record ->
                        val referenceRange = diabetesType.targetRange(unitId)
                        val value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, record.valueMmolPerL.toFloat(), unitId)
                        ReferenceRangeValue(
                            text = "${formatGlucoseValue(record.valueMmolPerL, unitId)} $unit",
                            value = value,
                            range = referenceRange,
                            aboveDescription = stringResource(R.string.blood_glucose_above_reference),
                            belowDescription = stringResource(R.string.blood_glucose_below_reference),
                        )
                    },
                    AppDataTableColumn("source", { AppDataTableHeaderText(stringResource(R.string.data_source)) }, ColumnWidth.Fixed(130.dp)) {
                        AppDataTableText(it.sourceId?.let(sourceNames::get) ?: noSource)
                    },
                    AppDataTableColumn("note", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_note)) }, ColumnWidth.Flex(1f, 120.dp)) { AppDataTableText(it.note) },
                ),
                actionsWidth = 104.dp,
                actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
                rowActions = { AppDataTableDeleteAction(stringResource(R.string.body_record_delete), onClick = { onAskDelete(it) }) },
                onRowClick = onEdit,
            )
        }
    }
    if (showTrendIndicatorInfo) {
        BloodGlucoseTrendIndicatorInfoDialog(onDismiss = { showTrendIndicatorInfo = false })
    }
}

@Composable
private fun BloodGlucoseTrendIndicatorInfoDialog(onDismiss: () -> Unit) {
    val tokens = LocalAppTokens.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.blood_glucose_trend_indicator_info_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            ) {
                Text(
                    stringResource(R.string.blood_glucose_trend_indicator_info_description),
                    style = tokens.type.body,
                    color = tokens.colors.textMuted,
                )
                listOf(4, 3, 2, 1, 0, -1, -2, -3, -4).forEach { level ->
                    val band = bloodGlucoseTrendRateRanges.first { it.value == abs(level) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TrendIndicatorArrowPreview(
                            level = level,
                            color = bloodGlucoseTrendColor(level),
                            modifier = Modifier.size(44.dp),
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = tokens.spacing.s2)) {
                            Text(
                                stringResource(bloodGlucoseTrendLabelRes(level)),
                                style = tokens.type.body,
                                color = tokens.colors.text,
                            )
                            Text(
                                bloodGlucoseTrendRangeText(band),
                                style = tokens.type.caption,
                                color = tokens.colors.textMuted,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.blood_glucose_trend_indicator_close)) }
        },
    )
}

private fun bloodGlucoseTrendColor(level: Int): Color = when {
    level < 0 -> Color(0xFFE53935)
    level > 0 -> Color(0xFFF57C00)
    else -> Color(0xFF43A047)
}

private fun bloodGlucoseTrendLabelRes(level: Int): Int = when (level) {
    4 -> R.string.blood_glucose_trend_case_positive_4
    3 -> R.string.blood_glucose_trend_case_positive_3
    2 -> R.string.blood_glucose_trend_case_positive_2
    1 -> R.string.blood_glucose_trend_case_positive_1
    0 -> R.string.blood_glucose_trend_case_stable
    -1 -> R.string.blood_glucose_trend_case_negative_1
    -2 -> R.string.blood_glucose_trend_case_negative_2
    -3 -> R.string.blood_glucose_trend_case_negative_3
    else -> R.string.blood_glucose_trend_case_negative_4
}

private fun bloodGlucoseTrendRangeText(band: RangeBand<Double, Int>): String {
    val min = band.min?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "-∞"
    val max = band.max?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "∞"
    return "${if (band.minInclusive) '[' else '('}$min, $max${if (band.maxInclusive) ']' else ')'} mmol/L/min"
}

@Composable
private fun HbA1cRecordList(
    records: List<BloodHbA1cRecord>,
    sources: List<BloodGlucoseSource>,
    diabetesType: BloodGlucoseDiabetesType,
    unitId: String,
    unit: String,
    onAdd: () -> Unit,
    onEdit: (BloodHbA1cRecord) -> Unit,
    onAskDelete: (BloodHbA1cRecord) -> Unit,
) {
    val sourceNames = remember(sources) { sources.associate { it.id to it.note } }
    val noSource = stringResource(R.string.data_source_none)
    val tokens = LocalAppTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.body_record_count, records.size),
                style = tokens.type.bodyS.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.colors.text,
            )
            AppIconTextButton(stringResource(R.string.body_record_add), R.drawable.ic_add, onAdd)
        }
        if (records.isEmpty()) {
            AppEmptyState(
                title = stringResource(R.string.blood_glucose_empty),
                description = stringResource(R.string.blood_glucose_data_info_hb_a1c),
                iconRes = R.drawable.ic_blood_glucose,
            )
        } else {
            AppDataTable(
                rows = records,
                rowKey = { _, record -> record.id },
                columns = listOf(
                    AppDataTableColumn("time", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_time)) }, ColumnWidth.Fixed(150.dp)) { AppDataTableText(formatBloodGlucoseTime(it.timestamp)) },
                    AppDataTableColumn("period", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_period)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(it.timingAnchor.periodLabel(it.relativeMinutes)) },
                    AppDataTableColumn("value", {
                        ReferenceRangeTableHeader(
                            title = stringResource(R.string.blood_glucose_hb_a1c),
                            rangeText = rangeText(diabetesType.hbA1cReferenceRange, "%"),
                        )
                    }, ColumnWidth.Fixed(190.dp)) { record ->
                        ReferenceRangeValue(
                            text = "%.2f%%".format(record.valueHbA1c),
                            value = record.valueHbA1c,
                            range = diabetesType.hbA1cReferenceRange,
                            aboveDescription = stringResource(R.string.hb_a1c_above_reference),
                            belowDescription = stringResource(R.string.hb_a1c_below_reference),
                        )
                    },
                    AppDataTableColumn("source", { AppDataTableHeaderText(stringResource(R.string.data_source)) }, ColumnWidth.Fixed(130.dp)) {
                        AppDataTableText(it.sourceId?.let(sourceNames::get) ?: noSource)
                    },
                    AppDataTableColumn("note", { AppDataTableHeaderText(stringResource(R.string.blood_glucose_note)) }, ColumnWidth.Flex(1f, 120.dp)) { AppDataTableText(it.note) },
                ),
                actionsWidth = 104.dp,
                actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
                rowActions = { AppDataTableDeleteAction(stringResource(R.string.body_record_delete), onClick = { onAskDelete(it) }) },
                onRowClick = onEdit,
            )
        }
    }
}

@Composable
private fun ReferenceRangeTableHeader(title: String, rangeText: String) {
    val tokens = LocalAppTokens.current
    Column {
        AppDataTableHeaderText(title)
        Text(rangeText, style = tokens.type.caption, color = tokens.colors.textMuted)
    }
}

@Composable
private fun <T> ReferenceRangeValue(
    text: String,
    value: T,
    range: UnitRange<T>,
    aboveDescription: String,
    belowDescription: String,
) where T : Comparable<T> {
    val isLow = range.min?.let { minimum -> value < minimum || value == minimum && !range.minInclusive } == true
    val isHigh = range.max?.let { maximum -> value > maximum || value == maximum && !range.maxInclusive } == true
    val tokens = LocalAppTokens.current
    val color = if (isLow || isHigh) tokens.colors.danger else tokens.colors.text
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 数值列统一等宽：血糖 5.60 与 12.30 的小数点必须在同一竖线上，靠右的箭头才不会跳。
        AppDataTableText(text, color = color, numeric = true)
        if (isLow || isHigh) {
            Icon(
                imageVector = if (isHigh) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isHigh) aboveDescription else belowDescription,
                tint = color,
            )
        }
    }
}

@Composable
private fun OgttRecordPlaceholder() {
    AppEmptyState(
        title = stringResource(R.string.blood_glucose_ogtt_unavailable),
        description = stringResource(R.string.blood_glucose_data_info_ogtt),
        iconRes = R.drawable.ic_blood_glucose,
    )
}

@Composable
private fun BloodGlucoseEditorScreen(
    record: BloodGlucoseRecord?,
    sources: List<BloodGlucoseSource>,
    timingDefaults: BloodGlucoseTimingDefaults,
    referenceRange: UnitRange<Float>,
    onBack: () -> Unit,
    onSave: (BloodGlucoseRecord) -> Unit,
) {
    val initialTimestamp = rememberSaveable(record?.id) {
        record?.timestamp ?: normalizeBloodGlucoseTimestamp(System.currentTimeMillis())
    }
    var timestamp by rememberSaveable(record?.id) { mutableStateOf(initialTimestamp) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val initialUnitId = rememberSaveable(record?.id) {
        AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)
    }
    var unitId by rememberSaveable(record?.id) { mutableStateOf(initialUnitId) }
    var value by rememberSaveable(record?.id) { mutableStateOf(record?.valueMmolPerL?.let { UnitConverter.fromBase(UnitCategoryType.Glucose.id, it.toFloat(), unitId).toString() }.orEmpty()) }
    var anchor by rememberSaveable(record?.id) { mutableStateOf(record?.timingAnchor ?: timingDefaults.preferredAnchor()) }
var relativeMinutes by rememberSaveable(record?.id) { mutableStateOf(record?.relativeMinutes?.let { kotlin.math.abs(it).toString() }.orEmpty()) }
    var timingRelation by rememberSaveable(record?.id) { mutableStateOf(record?.relativeMinutes?.timingRelation() ?: BloodGlucoseTimingRelation.At) }
    var sourceId by rememberSaveable(record?.id) { mutableStateOf(record?.sourceId) }
    var note by rememberSaveable(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    var showTimeResetDialog by rememberSaveable { mutableStateOf(false) }
    val timeResetOffset = ZERO
    val timeResetLabel = formatRelativeTimeOffset(
        offset = timeResetOffset,
        zeroLabel = stringResource(R.string.record_time_reset_now),
        dayUnit = stringResource(R.string.record_time_reset_day_unit),
        hourUnit = stringResource(R.string.record_time_reset_hour_unit),
        minuteUnit = stringResource(R.string.record_time_reset_minute_unit),
        secondUnit = stringResource(R.string.record_time_reset_second_unit),
    )
    val validValue = value.toDoubleOrNull()?.let { UnitConverter.toBase(UnitCategoryType.Glucose.id, it.toFloat(), unitId).toDouble() }?.takeIf(::isValidBloodGlucoseValue)
    val invalidValue = value.isNotBlank() && validValue == null
    val invalidRelativeMinutes = timingRelation != BloodGlucoseTimingRelation.At && (relativeMinutes.toIntOrNull()?.let { it > 0 } != true)
    val glucoseRange = bloodGlucoseInputRange(unitId)
    val glucoseUnit = glucoseUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId
    val glucoseRangeText = rangeText(glucoseRange, glucoseUnit)
    val glucoseReferenceRangeText = rangeText(referenceRange, glucoseUnit)
    val glucoseStep = UnitCategoryType.Glucose.stepSpec(unitId).valueFor(UnitStepMode.Normal).toDouble()
    val glucoseDecimals = 2
    val relativeMinutesValue = when (timingRelation) {
        BloodGlucoseTimingRelation.Before -> relativeMinutes.toIntOrNull()?.let { -it }
        BloodGlucoseTimingRelation.After -> relativeMinutes.toIntOrNull()
        BloodGlucoseTimingRelation.At -> 0
    }
    val editedRecord = validValue?.let {
        BloodGlucoseRecord(record?.id.orEmpty(), timestamp, it, anchor, relativeMinutesValue, note.trim(), sourceId)
    }
    val initialDraft = remember(record?.id) {
        BloodGlucoseEditorDraft(
            timestamp = initialTimestamp,
            valueText = record?.valueMmolPerL?.let {
                UnitConverter.fromBase(UnitCategoryType.Glucose.id, it.toFloat(), initialUnitId).toString()
            }.orEmpty(),
            unitId = initialUnitId,
            timingAnchor = record?.timingAnchor,
            relativeMinutesText = record?.relativeMinutes?.let { kotlin.math.abs(it).toString() }.orEmpty(),
            timingRelation = record?.relativeMinutes?.timingRelation() ?: BloodGlucoseTimingRelation.At,
            note = record?.note.orEmpty(),
            sourceId = record?.sourceId,
        )
    }
    val currentDraft = BloodGlucoseEditorDraft(
        timestamp = timestamp,
        valueText = value,
        unitId = unitId,
        timingAnchor = anchor,
        relativeMinutesText = relativeMinutes,
        timingRelation = timingRelation,
        note = note,
        sourceId = sourceId,
    )
    val hasChanges = bloodGlucoseDraftChanged(
        initial = initialDraft,
        current = currentDraft,
        initialRecord = record,
        currentRecord = editedRecord,
    )
    val saveEnabled = validValue != null && !invalidRelativeMinutes && hasChanges
    var showDiscardDialog by rememberSaveable(record?.id) { mutableStateOf(false) }
    fun save() {
        val saved = editedRecord ?: return
        onSave(saved.copy(id = record?.id ?: UUID.randomUUID().toString()))
    }
    fun requestBack() {
        if (hasChanges) showDiscardDialog = true else onBack()
    }

    BackHandler(enabled = true, onBack = ::requestBack)
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.blood_glucose_add_title), onBack = ::requestBack) { padding ->
        Column(Modifier.fillMaxSize().background(tokens.colors.bg).padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
            ) {
                // 卡 1：时间 + 数值。两者是「这次测的什么时候、多少」，拆开会让用户先填数再想起改时间。
                item {
                    AppSectionGroup {
                        Column(
                            modifier = Modifier.padding(tokens.spacing.s3),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        ) {
                            RecordTimePickerField(
                                title = stringResource(R.string.nutrition_editor_field_required, stringResource(R.string.blood_glucose_time)),
                                valueMillis = timestamp,
                                precision = RecordTimePrecision.SECOND,
                                onClick = { showDateTimePicker = true },
                                resetOffset = timeResetOffset,
                                resetLabel = timeResetLabel,
                                onResetClick = { showTimeResetDialog = true },
                            )
                            // 两个输入框的中心对齐，而不是各自下面那串提示对齐。
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                                    NumericInputField(
                                        label = stringResource(R.string.nutrition_editor_field_required, stringResource(R.string.blood_glucose_value)),
                                        value = value,
                                        onValueChange = { value = it },
                                        spec = NumericInputSpec(
                                            kind = NumericInputKind.Decimal,
                                            range = glucoseRange,
                                            example = if (unitId == "mg_dl") "100.00" else "5.60",
                                            decimalPlaces = glucoseDecimals,
                                            step = glucoseStep,
                                            showSupportingText = false,
                                        ),
                                    )
                                }
                                Box(Modifier.weight(.72f), contentAlignment = Alignment.TopStart) {
                                    AppDropdownField(
                                        label = stringResource(R.string.nutrition_editor_field_required, stringResource(R.string.blood_glucose_value_unit)),
                                        value = glucoseUnitOptions().firstOrNull { it.id == unitId }?.label ?: unitId,
                                        options = glucoseUnitOptions(),
                                        onSelect = { selected ->
                                            value.toDoubleOrNull()?.let { current -> value = UnitConverter.fromBase(UnitCategoryType.Glucose.id, UnitConverter.toBase(UnitCategoryType.Glucose.id, current.toFloat(), unitId), selected.id).toString() }
                                            unitId = selected.id
                                            AppPrefs.setUnit(context, UnitCategoryType.Glucose.id, unitId)
                                        },
                                    )
                                }
                            }
                            // 格式要求与参考范围属于「填的时候要看」的信息，跟输入框同一张卡常显；
                            // 不用 tooltip——填数值的过程中没有长按的余裕。
                            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
                                Text(
                                    text = if (invalidValue) {
                                        stringResource(R.string.blood_glucose_value_invalid)
                                    } else {
                                        stringResource(
                                            R.string.blood_glucose_effective_range_example,
                                            glucoseRangeText,
                                            if (unitId == "mg_dl") "100.00" else "5.60",
                                            glucoseUnit,
                                        )
                                    },
                                    style = tokens.type.caption,
                                    color = if (invalidValue) tokens.colors.danger else tokens.colors.textMuted,
                                )
                                Text(
                                    text = stringResource(R.string.blood_glucose_reference_range, glucoseReferenceRangeText),
                                    style = tokens.type.caption,
                                    color = tokens.colors.textMuted,
                                )
                            }
                        }
                    }
                }
                // 卡 2：时段。锚点 + 前后关系 + 分钟数是一句话的三段，分开摆会读成三个独立设置。
                item {
                    AppSectionGroup {
                        Column(
                            modifier = Modifier.padding(tokens.spacing.s3),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        ) {
                            AppDropdownField(
                                label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.blood_glucose_period_anchor)),
                                value = anchor?.let { stringResource(it.labelRes()) }.orEmpty(),
                                options = BloodGlucoseTimingAnchor.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                                onSelect = { anchor = BloodGlucoseTimingAnchor.valueOf(it.id) },
                            )
                            AppDropdownField(
                                label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.blood_glucose_period_relation)),
                                value = stringResource(timingRelation.labelRes()),
                                options = BloodGlucoseTimingRelation.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                                onSelect = { timingRelation = BloodGlucoseTimingRelation.valueOf(it.id) },
                            )
                            if (timingRelation != BloodGlucoseTimingRelation.At) {
                                NumericInputField(
                                    label = stringResource(R.string.nutrition_editor_field_required, stringResource(R.string.blood_glucose_minutes)),
                                    value = relativeMinutes,
                                    onValueChange = { relativeMinutes = it },
                                    spec = NumericInputSpec(kind = NumericInputKind.Integer),
                                )
                            }
                        }
                    }
                }
                // 卡 3：来源与备注。都是「补充说明」，放在一起，图表和列表里也只读这两项。
                item {
                    AppSectionGroup {
                        Column(
                            modifier = Modifier.padding(tokens.spacing.s3),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        ) {
                            if (sources.isNotEmpty()) {
                                val source = sources.firstOrNull { it.id == sourceId }
                                AppDropdownField(
                                    label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.data_source)),
                                    value = source?.note ?: stringResource(R.string.data_source_none),
                                    options = listOf(AppDropdownOption("__none__", stringResource(R.string.data_source_none))) + sources.map { AppDropdownOption(it.id, it.note) },
                                    onSelect = { sourceId = if (it.id == "__none__") null else it.id },
                                )
                            }
                            TextInputField(
                                label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.blood_glucose_note)),
                                value = note,
                                onValueChange = { note = it },
                                tooltip = stringResource(R.string.blood_glucose_note_hint),
                            )
                        }
                    }
                }
            }
            FormSaveBar(
                text = stringResource(R.string.blood_glucose_save),
                enabled = saveEnabled,
                onSave = ::save,
            )
        }
    }
    if (showDateTimePicker) {
        ComposeDateTimePickerDialog(
            initialMillis = timestamp,
            onDismiss = { showDateTimePicker = false },
            onDateTimePicked = {
                timestamp = it
                showDateTimePicker = false
            },
            precision = RecordTimePrecision.SECOND,
        )
    }
    if (showTimeResetDialog) {
        AlertDialog(
            onDismissRequest = { showTimeResetDialog = false },
            title = { Text(stringResource(R.string.blood_glucose_time_reset_title)) },
            text = { Text(stringResource(R.string.blood_glucose_time_reset_message, timeResetLabel)) },
            confirmButton = {
                TextButton(onClick = {
                    timestamp = normalizeBloodGlucoseTimestamp(System.currentTimeMillis() + timeResetOffset.inWholeMilliseconds)
                    showTimeResetDialog = false
                }) { Text(stringResource(R.string.compose_confirm_dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimeResetDialog = false }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onBack,
            onSave = ::save,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
        )
    }
}

internal data class BloodGlucoseEditorDraft(
    val timestamp: Long,
    val valueText: String,
    val unitId: String,
    val timingAnchor: BloodGlucoseTimingAnchor?,
    val relativeMinutesText: String,
    val timingRelation: BloodGlucoseTimingRelation,
    val note: String,
    val sourceId: String? = null,
)

internal fun bloodGlucoseDraftChanged(
    initial: BloodGlucoseEditorDraft,
    current: BloodGlucoseEditorDraft,
    initialRecord: BloodGlucoseRecord?,
    currentRecord: BloodGlucoseRecord?,
): Boolean = when {
    currentRecord != null && initialRecord != null -> !currentRecord.hasSameContentAs(initialRecord)
    currentRecord != null -> current.valueText.isNotBlank() || current.timingAnchor != null ||
        current.timingRelation != BloodGlucoseTimingRelation.At || current.note.isNotBlank() || current.sourceId != null ||
        current.timestamp != initial.timestamp
    else -> current.copy(unitId = initial.unitId) != initial.copy(unitId = initial.unitId)
}

internal fun BloodGlucoseRecord.hasSameContentAs(other: BloodGlucoseRecord): Boolean =
    id == other.id && timestamp == other.timestamp && abs(valueMmolPerL - other.valueMmolPerL) <= 0.000001 &&
        timingAnchor == other.timingAnchor && (relativeMinutes ?: 0) == (other.relativeMinutes ?: 0) && note == other.note && sourceId == other.sourceId

@Composable
private fun glucoseUnitOptions(): List<AppDropdownOption> = UnitConverter.getRepository()?.getCategory(UnitCategoryType.Glucose.id)?.units
    ?.filterNot { it.hidden }
    ?.map { AppDropdownOption(it.id, it.symbol(Locale.getDefault())) }
    .orEmpty()

@Composable
private fun BloodGlucoseTimingAnchor?.periodLabel(relativeMinutes: Int?): String {
    val anchorText = this?.let { stringResource(it.labelRes()) }.orEmpty()
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

internal enum class BloodGlucoseTimingRelation { Before, After, At }

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
    formatRecordTimestamp(timestamp, RecordTimePrecision.SECOND)


private fun formatBloodGlucoseAxisTime(timestamp: Long, intervalMs: Long): String {
    val pattern = when {
        intervalMs < 60_000L -> "MM-dd HH:mm:ss"
        intervalMs < 86_400_000L -> "MM-dd HH:mm"
        else -> "MM-dd"
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))
}

@Composable
private fun BloodHbA1cEditorScreen(
    record: BloodHbA1cRecord?,
    sources: List<BloodGlucoseSource>,
    timingDefaults: BloodGlucoseTimingDefaults,
    referenceRange: UnitRange<Double>,
    onBack: () -> Unit,
    onSave: (BloodHbA1cRecord) -> Unit,
) {
    val initialTimestamp = rememberSaveable(record?.id) {
        record?.timestamp ?: normalizeBloodGlucoseTimestamp(System.currentTimeMillis())
    }
    var timestamp by rememberSaveable(record?.id) { mutableStateOf(initialTimestamp) }
    var value by rememberSaveable(record?.id) { mutableStateOf(record?.valueHbA1c?.let { "%.2f".format(it) }.orEmpty()) }
    var anchor by rememberSaveable(record?.id) { mutableStateOf(record?.timingAnchor ?: timingDefaults.preferredAnchor()) }
    var relativeMinutes by rememberSaveable(record?.id) { mutableStateOf(record?.relativeMinutes?.let { kotlin.math.abs(it).toString() }.orEmpty()) }
    var timingRelation by rememberSaveable(record?.id) { mutableStateOf(record?.relativeMinutes?.timingRelation() ?: BloodGlucoseTimingRelation.At) }
    var sourceId by rememberSaveable(record?.id) { mutableStateOf(record?.sourceId) }
    var note by rememberSaveable(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    val validValue = value.toDoubleOrNull()?.takeIf(::isValidHbA1cValue)
    val invalidValue = value.isNotBlank() && validValue == null
    val invalidRelativeMinutes = timingRelation != BloodGlucoseTimingRelation.At && (relativeMinutes.toIntOrNull()?.let { it > 0 } != true)
    val hbA1cStep = 0.1
    val hbA1cRangeText = rangeText(hbA1cInputRange, "%")
    val hbA1cReferenceRangeText = stringResource(R.string.hb_a1c_reference_range, rangeText(referenceRange, "%"))
    val relativeMinutesValue = when (timingRelation) {
        BloodGlucoseTimingRelation.Before -> relativeMinutes.toIntOrNull()?.let { -it }
        BloodGlucoseTimingRelation.After -> relativeMinutes.toIntOrNull()
        BloodGlucoseTimingRelation.At -> 0
    }
    val editedRecord = validValue?.let {
        BloodHbA1cRecord(record?.id.orEmpty(), timestamp, it, anchor, relativeMinutesValue, note.trim(), sourceId)
    }
    val initialDraft = remember(record?.id) {
        HbA1cEditorDraft(
            timestamp = initialTimestamp,
            valueText = record?.valueHbA1c?.let { "%.2f".format(it) }.orEmpty(),
            timingAnchor = record?.timingAnchor,
            relativeMinutesText = record?.relativeMinutes?.let { kotlin.math.abs(it).toString() }.orEmpty(),
            timingRelation = record?.relativeMinutes?.timingRelation() ?: BloodGlucoseTimingRelation.At,
            note = record?.note.orEmpty(),
            sourceId = record?.sourceId,
        )
    }
    val currentDraft = HbA1cEditorDraft(
        timestamp = timestamp,
        valueText = value,
        timingAnchor = anchor,
        relativeMinutesText = relativeMinutes,
        timingRelation = timingRelation,
        note = note,
        sourceId = sourceId,
    )
    val hasChanges = hbA1cDraftChanged(
        initial = initialDraft,
        current = currentDraft,
        initialRecord = record,
        currentRecord = editedRecord,
    )
    val saveEnabled = validValue != null && !invalidRelativeMinutes && hasChanges
    var showDiscardDialog by rememberSaveable(record?.id) { mutableStateOf(false) }
    fun save() {
        val saved = editedRecord ?: return
        onSave(saved.copy(id = record?.id ?: UUID.randomUUID().toString()))
    }
    fun requestBack() {
        if (hasChanges) showDiscardDialog = true else onBack()
    }

    BackHandler(enabled = true, onBack = ::requestBack)
    val tokens = LocalAppTokens.current
    BaseScreen(title = stringResource(R.string.blood_glucose_hb_a1c_add_title), onBack = ::requestBack) { padding ->
        Column(Modifier.fillMaxSize().background(tokens.colors.bg).padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
            ) {
                item {
                    AppSectionGroup {
                        Column(
                            modifier = Modifier.padding(tokens.spacing.s3),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        ) {
                            RecordTimePickerField(
                                title = stringResource(R.string.nutrition_editor_field_required, stringResource(R.string.blood_glucose_source_time)),
                                valueMillis = timestamp,
                                precision = RecordTimePrecision.SECOND,
                                onClick = { showDateTimePicker = true },
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                NumericInputField(
                                    label = stringResource(R.string.nutrition_editor_field_required, stringResource(R.string.hb_a1c_value_label)),
                                    value = value,
                                    onValueChange = { value = it },
                                    spec = NumericInputSpec(
                                        kind = NumericInputKind.Decimal,
                                        range = hbA1cInputRange,
                                        example = "5.50",
                                        decimalPlaces = 2,
                                        step = hbA1cStep,
                                        tooltip = hbA1cRangeText,
                                        showSupportingText = false,
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                // 16 sp：与输入框里的数字同高，读起来是「5.50 %」一个整体。
                                Text(
                                    text = "%",
                                    style = tokens.type.body,
                                    color = tokens.colors.textMuted,
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1)) {
                                Text(
                                    text = if (invalidValue) stringResource(R.string.hb_a1c_value_invalid) else stringResource(
                                        R.string.blood_glucose_effective_range_example,
                                        hbA1cRangeText,
                                        "5.50",
                                        "%",
                                    ),
                                    style = tokens.type.caption,
                                    color = if (invalidValue) tokens.colors.danger else tokens.colors.textMuted,
                                )
                                Text(
                                    text = hbA1cReferenceRangeText,
                                    style = tokens.type.caption,
                                    color = tokens.colors.textMuted,
                                )
                            }
                        }
                    }
                }
                item {
                    AppSectionGroup {
                        Column(
                            modifier = Modifier.padding(tokens.spacing.s3),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        ) {
                            AppDropdownField(
                                label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.blood_glucose_period_anchor)),
                                value = anchor?.let { stringResource(it.labelRes()) }.orEmpty(),
                                options = BloodGlucoseTimingAnchor.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                                onSelect = { anchor = BloodGlucoseTimingAnchor.valueOf(it.id) },
                            )
                            AppDropdownField(
                                label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.blood_glucose_period_relation)),
                                value = stringResource(timingRelation.labelRes()),
                                options = BloodGlucoseTimingRelation.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                                onSelect = { timingRelation = BloodGlucoseTimingRelation.valueOf(it.id) },
                            )
                            if (timingRelation != BloodGlucoseTimingRelation.At) {
                                NumericInputField(
                                    label = stringResource(R.string.nutrition_editor_field_required, stringResource(R.string.blood_glucose_minutes)),
                                    value = relativeMinutes,
                                    onValueChange = { relativeMinutes = it },
                                    spec = NumericInputSpec(kind = NumericInputKind.Integer),
                                )
                            }
                        }
                    }
                }
                item {
                    AppSectionGroup {
                        Column(
                            modifier = Modifier.padding(tokens.spacing.s3),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
                        ) {
                            if (sources.isNotEmpty()) {
                                val source = sources.firstOrNull { it.id == sourceId }
                                AppDropdownField(
                                    label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.data_source)),
                                    value = source?.note ?: stringResource(R.string.data_source_none),
                                    options = listOf(AppDropdownOption("__none__", stringResource(R.string.data_source_none))) + sources.map { AppDropdownOption(it.id, it.note) },
                                    onSelect = { sourceId = if (it.id == "__none__") null else it.id },
                                )
                            }
                            TextInputField(
                                label = stringResource(R.string.nutrition_editor_field_optional, stringResource(R.string.blood_glucose_note)),
                                value = note,
                                onValueChange = { note = it },
                                tooltip = stringResource(R.string.blood_glucose_note_hint),
                            )
                        }
                    }
                }
            }
            FormSaveBar(
                text = stringResource(R.string.blood_glucose_save),
                enabled = saveEnabled,
                onSave = ::save,
            )
        }
    }
    if (showDateTimePicker) {
        ComposeDateTimePickerDialog(
            initialMillis = timestamp,
            onDismiss = { showDateTimePicker = false },
            onDateTimePicked = {
                timestamp = it
                showDateTimePicker = false
            },
            precision = RecordTimePrecision.SECOND,
        )
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onBack,
            onSave = ::save,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
        )
    }
}

internal data class HbA1cEditorDraft(
    val timestamp: Long,
    val valueText: String,
    val timingAnchor: BloodGlucoseTimingAnchor?,
    val relativeMinutesText: String,
    val timingRelation: BloodGlucoseTimingRelation,
    val note: String,
    val sourceId: String? = null,
)

internal fun hbA1cDraftChanged(
    initial: HbA1cEditorDraft,
    current: HbA1cEditorDraft,
    initialRecord: BloodHbA1cRecord?,
    currentRecord: BloodHbA1cRecord?,
): Boolean = when {
    currentRecord != null && initialRecord != null -> !currentRecord.hasSameContentAs(initialRecord)
    currentRecord != null -> current.valueText.isNotBlank() || current.timingAnchor != null ||
        current.timingRelation != BloodGlucoseTimingRelation.At || current.note.isNotBlank() || current.sourceId != null ||
        current.timestamp != initial.timestamp
    else -> current != initial
}

internal fun BloodHbA1cRecord.hasSameContentAs(other: BloodHbA1cRecord): Boolean =
    id == other.id && timestamp == other.timestamp && abs(valueHbA1c - other.valueHbA1c) <= 0.000001 &&
        timingAnchor == other.timingAnchor && (relativeMinutes ?: 0) == (other.relativeMinutes ?: 0) && note == other.note && sourceId == other.sourceId

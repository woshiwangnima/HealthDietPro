package com.woshiwangnima.healthdietpro.ui.event

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangePreset
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection
import com.woshiwangnima.healthdietpro.common.time.resolve
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.ActionGridItem
import com.woshiwangnima.healthdietpro.common.ui.RecordTimeRangeFilter
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.model.diet.DietRecord
import com.woshiwangnima.healthdietpro.model.diet.DietRepository
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.disease.curatedId
import com.woshiwangnima.healthdietpro.model.disease.diabetesReferenceIds
import com.woshiwangnima.healthdietpro.model.disease.hasCurrentUserDiabetesRisk
import com.woshiwangnima.healthdietpro.model.sleep.SleepRecord
import com.woshiwangnima.healthdietpro.model.sleep.SleepRepository
import com.woshiwangnima.healthdietpro.ui.diet.DietCard
import com.woshiwangnima.healthdietpro.ui.record.MedicationRecordsPage
import com.woshiwangnima.healthdietpro.ui.record.RecordActionId
import com.woshiwangnima.healthdietpro.ui.sleep.SleepCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class EventTab(val titleRes: Int, val iconRes: Int) {
    Medication(R.string.event_tab_medication, R.drawable.ic_medication),
    Diet(R.string.event_tab_diet, R.drawable.ic_diet),
    Exercise(R.string.event_tab_exercise, R.drawable.ic_exercise),
    Sleep(R.string.event_tab_sleep, R.drawable.ic_sleep),
}

internal data class EventUiState(
    val selectedTab: EventTab = EventTab.Medication,
    val medicationRecords: List<MedicationRecord> = emptyList(),
    val dietRecords: List<DietRecord> = emptyList(),
    val sleepRecords: List<SleepRecord> = emptyList(),
    val medicationTimeRangeSelection: RecordTimeRangeSelection = RecordTimeRangeSelection.Preset(RecordTimeRangePreset.TODAY),
    val dietTimeRangeSelection: RecordTimeRangeSelection = RecordTimeRangeSelection.Preset(RecordTimeRangePreset.TODAY),
    val sleepTimeRangeSelection: RecordTimeRangeSelection = RecordTimeRangeSelection.Preset(RecordTimeRangePreset.LAST_24_HOURS),
)

internal class EventViewModel(application: Application) : ViewModel() {
    private val app = application
    private val dietRepository = DietRepository.fromContext(application)
    private val sleepRepository = SleepRepository.fromContext(application)
    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: EventTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setMedicationTimeRangeSelection(selection: RecordTimeRangeSelection) { _uiState.value = _uiState.value.copy(medicationTimeRangeSelection = selection) }

    fun setDietTimeRangeSelection(selection: RecordTimeRangeSelection) { _uiState.value = _uiState.value.copy(dietTimeRangeSelection = selection) }

    fun setSleepTimeRangeSelection(selection: RecordTimeRangeSelection) { _uiState.value = _uiState.value.copy(sleepTimeRangeSelection = selection) }

    fun refresh() {
        viewModelScope.launch {
            val medicationRecords = withContext(Dispatchers.IO) {
                val diabetesRisk = hasCurrentUserDiabetesRisk(app)
                val diabetesReferences = DiseaseRepository.fromContext(app).diabetesReferenceIds()
                MedicationPrefs.getRecords(app)
                    .filter { record ->
                        diabetesRisk && record.indicationReferences.any { it.curatedId() in diabetesReferences }
                    }
                    .sortedByDescending { it.timestamp }
            }
            val dietRecords = withContext(Dispatchers.IO) {
                dietRepository.load().records.sortedByDescending(DietRecord::mealStartAt)
            }
            val sleepRecords = withContext(Dispatchers.IO) {
                sleepRepository.load().records.sortedByDescending(SleepRecord::sleepStartAt)
            }
            _uiState.value = _uiState.value.copy(
                medicationRecords = medicationRecords,
                dietRecords = dietRecords,
                sleepRecords = sleepRecords,
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EventViewModel(application) as T
    }
}

@Composable
internal fun EventScreen(
    viewModel: EventViewModel,
    onOpenDietAnalysis: (DietRecord) -> Unit,
    onOpenSleepAnalysis: (SleepRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // 事件页挂在 MainActivity 的页签里，全局主题仍是旧的 HealthDietProTheme，因此在屏内自套一层令牌主题。
    AppTheme(darkTheme = rememberAppDarkTheme()) {
        EventContent(
            uiState = uiState,
            onSelectTab = viewModel::selectTab,
            onMedicationRange = viewModel::setMedicationTimeRangeSelection,
            onDietRange = viewModel::setDietTimeRangeSelection,
            onSleepRange = viewModel::setSleepTimeRangeSelection,
            onOpenDietAnalysis = onOpenDietAnalysis,
            onOpenSleepAnalysis = onOpenSleepAnalysis,
            modifier = modifier,
        )
    }
}

@Composable
private fun EventContent(
    uiState: EventUiState,
    onSelectTab: (EventTab) -> Unit,
    onMedicationRange: (RecordTimeRangeSelection) -> Unit,
    onDietRange: (RecordTimeRangeSelection) -> Unit,
    onSleepRange: (RecordTimeRangeSelection) -> Unit,
    onOpenDietAnalysis: (DietRecord) -> Unit,
    onOpenSleepAnalysis: (SleepRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = modifier.fillMaxSize().background(tokens.colors.bg),
    ) {
        EqualWidthSegmentedTabs(
            tabs = EventTab.entries.map { tab -> EqualWidthTab(tab.titleRes, tab.iconRes) },
            selectedIndex = uiState.selectedTab.ordinal,
            onSelected = { onSelectTab(EventTab.entries[it]) },
            modifier = Modifier.padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s2),
        )
        when (uiState.selectedTab) {
            EventTab.Medication -> MedicationRecordsPage(
                records = uiState.medicationRecords,
                timeRangeSelection = uiState.medicationTimeRangeSelection,
                onTimeRangeSelectionChanged = onMedicationRange,
                editable = false,
            )
            EventTab.Diet -> EventReadOnlyRecordsPage(
                records = uiState.dietRecords,
                timeRangeSelection = uiState.dietTimeRangeSelection,
                onTimeRangeSelectionChanged = onDietRange,
                timestamp = DietRecord::mealStartAt,
                key = DietRecord::id,
            ) { record ->
                DietCard(record = record, onOpen = { onOpenDietAnalysis(record) })
            }
            EventTab.Sleep -> EventReadOnlyRecordsPage(
                records = uiState.sleepRecords,
                timeRangeSelection = uiState.sleepTimeRangeSelection,
                onTimeRangeSelectionChanged = onSleepRange,
                timestamp = SleepRecord::sleepStartAt,
                key = SleepRecord::id,
            ) { record ->
                SleepCard(record = record, timer = null, onOpen = { onOpenSleepAnalysis(record) }, highlighted = false)
            }
            EventTab.Exercise -> EventUnavailablePage()
        }
    }
}

@Composable
private fun <T> EventReadOnlyRecordsPage(
    records: List<T>,
    timeRangeSelection: RecordTimeRangeSelection,
    onTimeRangeSelectionChanged: (RecordTimeRangeSelection) -> Unit,
    timestamp: (T) -> Long,
    key: (T) -> String,
    card: @Composable (T) -> Unit,
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timeRangeSelection is RecordTimeRangeSelection.Preset) {
        if (timeRangeSelection !is RecordTimeRangeSelection.Preset) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L - nowMillis % 60_000L)
        }
    }
    val range = timeRangeSelection.resolve(nowMillis)
    val filtered = remember(records, range) { records.filter { range.contains(timestamp(it)) } }
    val grouped = remember(filtered) {
        filtered.groupBy { record -> Instant.ofEpochMilli(timestamp(record)).atZone(ZoneId.systemDefault()).toLocalDate() }
    }
    val tokens = LocalAppTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s3),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
    ) {
        RecordTimeRangeFilter(selection = timeRangeSelection, onSelectionChanged = onTimeRangeSelectionChanged)
        if (filtered.isEmpty()) {
            // 空态紧跟时间范围筛选，用一行居中说明即可；再套一张插图卡片会比上面的筛选条还重。
            Text(
                text = stringResource(R.string.common_empty_records),
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
                modifier = Modifier.fillMaxWidth().padding(vertical = tokens.spacing.s5),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            ) {
                grouped.forEach { (date, dateRecords) ->
                    item(key = "date_$date") { EventDateHeader(date) }
                    items(dateRecords, key = key) { record -> card(record) }
                }
            }
        }
    }
}

@Composable
private fun EventDateHeader(date: LocalDate) {
    val tokens = LocalAppTokens.current
    Text(
        text = date.toString(),
        style = tokens.type.bodyS,
        color = tokens.colors.textMuted,
        modifier = Modifier.padding(top = tokens.spacing.s2, start = tokens.spacing.s1),
    )
}

@Composable
internal fun EventGlucoseAnalysisScreen(
    title: String,
    onBack: () -> Unit,
) {
    com.woshiwangnima.healthdietpro.common.ui.BaseScreen(title = title, onBack = onBack) { _ -> }
}

@Composable
internal fun EventInfoScreen(
    onBack: () -> Unit,
    onOpenRecordAction: (RecordActionId) -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    AppTheme(darkTheme = rememberAppDarkTheme()) {
        baseScreenEventInfo(onBack, onOpenRecordAction)
    }
}

@Composable
private fun baseScreenEventInfo(
    onBack: () -> Unit,
    onOpenRecordAction: (RecordActionId) -> Unit,
) {
    val tokens = LocalAppTokens.current
    com.woshiwangnima.healthdietpro.common.ui.BaseScreen(
        title = stringResource(R.string.blood_glucose_events_info_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding)
                .padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
        ) {
            Text(
                text = stringResource(R.string.blood_glucose_events_info_description),
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
            )
            EventTab.entries.forEach { tab ->
                val action = when (tab) {
                    EventTab.Medication -> RecordActionId.Medication
                    EventTab.Diet -> RecordActionId.Diet
                    EventTab.Exercise -> RecordActionId.Exercise
                    EventTab.Sleep -> RecordActionId.Sleep
                }
                val enabled = tab != EventTab.Exercise
                ActionGridItem(
                    title = stringResource(tab.titleRes),
                    iconRes = tab.iconRes,
                    enabled = enabled,
                    summary = stringResource(if (enabled) R.string.blood_glucose_events_info_entry else R.string.event_unavailable),
                    onClick = { if (enabled) onOpenRecordAction(action) },
                )
            }
        }
    }
}

@Composable
private fun EventUnavailablePage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AppEmptyState(
            title = stringResource(R.string.event_tab_exercise),
            description = stringResource(R.string.event_unavailable),
            iconRes = R.drawable.ic_exercise,
        )
    }
}

package com.woshiwangnima.healthdietpro.ui.event

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.ActionGridItem
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.ui.record.MedicationRecordsPage
import com.woshiwangnima.healthdietpro.ui.record.RecordActionId
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
    val timeRangeSelection: RecordTimeRangeSelection = RecordTimeRangeSelection.Preset(RecordTimeRangePreset.TODAY),
)

internal class EventViewModel(application: Application) : ViewModel() {
    private val app = application
    private val _uiState = MutableStateFlow(EventUiState())
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: EventTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setTimeRangeSelection(selection: RecordTimeRangeSelection) {
        _uiState.value = _uiState.value.copy(timeRangeSelection = selection)
    }

    fun refresh() {
        viewModelScope.launch {
            val records = withContext(Dispatchers.IO) {
                MedicationPrefs.getRecords(app).sortedByDescending { it.timestamp }
            }
            _uiState.value = _uiState.value.copy(medicationRecords = records)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EventViewModel(application) as T
    }
}

@Composable
internal fun EventScreen(viewModel: EventViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        EqualWidthSegmentedTabs(
            tabs = EventTab.entries.map { tab -> EqualWidthTab(tab.titleRes, tab.iconRes) },
            selectedIndex = uiState.selectedTab.ordinal,
            onSelected = { viewModel.selectTab(EventTab.entries[it]) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        when (uiState.selectedTab) {
            EventTab.Medication -> MedicationRecordsPage(
                records = uiState.medicationRecords,
                timeRangeSelection = uiState.timeRangeSelection,
                onTimeRangeSelectionChanged = viewModel::setTimeRangeSelection,
                editable = false,
            )
            else -> EventUnavailablePage()
        }
    }
}

@Composable
internal fun EventInfoScreen(
    onBack: () -> Unit,
    onOpenRecordAction: (RecordActionId) -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    com.woshiwangnima.healthdietpro.common.ui.BaseScreen(
        title = stringResource(R.string.blood_glucose_events_info_title),
        onBack = onBack,
        includeNavigationBarPadding = false,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.blood_glucose_events_info_description),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EventTab.entries.forEach { tab ->
                val action = when (tab) {
                    EventTab.Medication -> RecordActionId.Medication
                    EventTab.Diet -> RecordActionId.Diet
                    EventTab.Exercise -> RecordActionId.Exercise
                    EventTab.Sleep -> RecordActionId.Sleep
                }
                ActionGridItem(
                    title = stringResource(tab.titleRes),
                    iconRes = tab.iconRes,
                    enabled = true,
                    summary = stringResource(R.string.blood_glucose_events_info_entry),
                    onClick = { onOpenRecordAction(action) },
                )
            }
        }
    }
}

@Composable
private fun EventUnavailablePage() {
    Column(
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.event_unavailable),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

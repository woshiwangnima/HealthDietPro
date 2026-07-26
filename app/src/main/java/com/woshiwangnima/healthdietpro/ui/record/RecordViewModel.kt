package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRepository
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun setQuery(query: String) { _uiState.value = _uiState.value.copy(query = query) }

    fun submitQuery(query: String = _uiState.value.query) {
        val normalized = query.trim()
        val history = if (normalized.isBlank()) _uiState.value.searchHistory else (listOf(normalized) + _uiState.value.searchHistory.filterNot { it.equals(normalized, true) }).take(12)
        _uiState.value = _uiState.value.copy(query = query, submittedQuery = normalized, searchHistory = history)
        if (normalized.isNotBlank()) UserPrefs.current(getApplication()).putString(SEARCH_HISTORY_KEY, history.joinToString(HISTORY_SEPARATOR))
    }

    fun removeSearchHistory(value: String) {
        val history = _uiState.value.searchHistory - value
        _uiState.value = _uiState.value.copy(searchHistory = history)
        UserPrefs.current(getApplication()).putString(SEARCH_HISTORY_KEY, history.joinToString(HISTORY_SEPARATOR))
    }

    fun clearSearchHistory() {
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        UserPrefs.current(getApplication()).putString(SEARCH_HISTORY_KEY, "")
    }

    fun recordActionOpened(id: RecordActionId) {
        _uiState.value = _uiState.value.let { state -> state.copy(recentActionIds = (listOf(id) + state.recentActionIds.filterNot { it == id }).take(6)) }
    }
    fun removeRecentAction(id: RecordActionId) {
        _uiState.value = _uiState.value.let { it.copy(recentActionIds = it.recentActionIds - id) }
    }
    fun clearRecentActions() { _uiState.value = _uiState.value.copy(recentActionIds = emptyList()) }

    fun refresh() {
        val context = getApplication<Application>()
        val profile = ProfilePrefs.load(context)
        val glucose = BloodGlucoseRepository.fromContext(context).load().maxByOrNull { it.timestamp }
        val medication = MedicationPrefs.getRecords(context).maxByOrNull { it.timestamp }
        val circumference = profile.circumferenceRecords.values.flatten().maxByOrNull { it.recordedAtMillis ?: it.date.toEpochMillis() }
        val latest = mapOf(
            RecordActionId.Height to profile.heightRecords.maxByOrNull { it.recordedAtMillis ?: it.date.toEpochMillis() }?.let { RecordLatest(it.recordedAtMillis ?: it.date.toEpochMillis(), "${it.value} cm") },
            RecordActionId.Weight to profile.weightRecords.maxByOrNull { it.recordedAtMillis ?: it.date.toEpochMillis() }?.let { RecordLatest(it.recordedAtMillis ?: it.date.toEpochMillis(), "${it.value} kg") },
            RecordActionId.BloodGlucose to glucose?.let { RecordLatest(it.timestamp, "${it.valueMmolPerL} mmol/L") },
            RecordActionId.Medication to medication?.let { RecordLatest(it.timestamp, "${it.medicationName} ${it.doseValue} ${it.doseUnit}") },
            RecordActionId.Waist to circumference?.let { RecordLatest(it.recordedAtMillis ?: it.date.toEpochMillis(), "${it.value} cm") },
        )
        val history = UserPrefs.current(context).getString(SEARCH_HISTORY_KEY, "").split(HISTORY_SEPARATOR).filter { it.isNotBlank() }
        _uiState.value = _uiState.value.copy(
            sections = defaultRecordSections().map { section ->
                section.copy(items = section.items.map { item ->
                    latest[item.id]?.let { item.copy(latestTimestamp = it.timestamp, latestValue = it.value) } ?: item
                })
            },
            searchHistory = history,
        )
    }

    private data class RecordLatest(val timestamp: Long, val value: String)
    private fun String.toEpochMillis(): Long = runCatching {
        java.time.LocalDate.parse(this).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrDefault(0L)

    private companion object {
        const val SEARCH_HISTORY_KEY = "record_search_history_v1"
        const val HISTORY_SEPARATOR = "\u001F"
    }
}

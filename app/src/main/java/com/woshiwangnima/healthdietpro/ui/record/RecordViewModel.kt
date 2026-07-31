package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRepository
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureRepository
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecordRepository
import com.woshiwangnima.healthdietpro.model.disease.UserCustomDisease
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.prefs.deserializeSearchHistory
import com.woshiwangnima.healthdietpro.model.prefs.serializeSearchHistory
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Locale
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
        val history = if (normalized.isBlank()) _uiState.value.searchHistory else {
            (listOf(normalized) + _uiState.value.searchHistory.filterNot { it.equals(normalized, true) })
        }
        _uiState.value = _uiState.value.copy(query = query, submittedQuery = normalized, searchHistory = history)
        if (normalized.isNotBlank()) saveSearchHistory(history)
    }

    fun removeSearchHistory(value: String) {
        val history = _uiState.value.searchHistory - value
        _uiState.value = _uiState.value.copy(searchHistory = history)
        saveSearchHistory(history)
    }

    fun clearSearchHistory() {
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        saveSearchHistory(emptyList())
    }

    fun recordActionOpened(id: RecordActionId) {
        val recent = listOf(id) + _uiState.value.recentActionIds.filterNot { it == id }
        _uiState.value = _uiState.value.copy(recentActionIds = recent.take(6))
        saveRecentActions(recent)
    }
    fun removeRecentAction(id: RecordActionId) {
        val recent = _uiState.value.recentActionIds - id
        _uiState.value = _uiState.value.copy(recentActionIds = recent)
        saveRecentActions(recent)
    }
    fun clearRecentActions() {
        _uiState.value = _uiState.value.copy(recentActionIds = emptyList())
        saveRecentActions(emptyList())
    }

    fun refresh() {
        val context = getApplication<Application>()
        val profile = ProfilePrefs.load(context)
        val glucose = BloodGlucoseRepository.fromContext(context).load().maxByOrNull { it.timestamp }
        val bloodPressure = BloodPressureRepository.fromContext(context).load().maxByOrNull { it.timestamp }
        val latestDisease = UserDiseaseRecordRepository.fromContext(context).load().maxByOrNull { it.updatedAt }
        val customDiseases = UserDiseaseRecordRepository.fromContext(context).loadCustomDiseases().associateBy(UserCustomDisease::id)
        val diseaseCatalog = DiseaseRepository.fromContext(context)
        val pressureUnitId = AppPrefs.getUnit(context, UnitCategoryType.Pressure.id, UnitCategoryType.Pressure.defaultUnitId)
        val pressureUnit = UnitConverter.getRepository()?.getUnit(UnitCategoryType.Pressure.id, pressureUnitId)
            ?.symbol(Locale.getDefault()) ?: pressureUnitId
        val medication = MedicationPrefs.getRecords(context).maxByOrNull { it.timestamp }
        val circumference = profile.circumferenceRecords.values.flatten().maxByOrNull { it.recordedAtMillis }
        val latest = mapOf(
            RecordActionId.Height to profile.heightRecords.maxByOrNull { it.recordedAtMillis }?.let { RecordLatest(it.recordedAtMillis, "${it.value} cm") },
            RecordActionId.Weight to profile.weightRecords.maxByOrNull { it.recordedAtMillis }?.let { RecordLatest(it.recordedAtMillis, "${it.value} kg") },
            RecordActionId.BloodGlucose to glucose?.let { RecordLatest(it.timestamp, "${it.valueMmolPerL} mmol/L") },
            RecordActionId.BloodPressure to bloodPressure?.let {
                val systolic = UnitConverter.fromBase(UnitCategoryType.Pressure.id, it.systolicMmhg, pressureUnitId)
                val diastolic = UnitConverter.fromBase(UnitCategoryType.Pressure.id, it.diastolicMmhg, pressureUnitId)
                val precision = if (pressureUnitId == "kpa") "%.1f/%.1f %s" else "%.0f/%.0f %s"
                RecordLatest(it.timestamp, String.format(Locale.getDefault(), precision, systolic, diastolic, pressureUnit))
            },
            RecordActionId.Disease to latestDisease?.let { record ->
                val name = record.disease.curatedDiseaseId?.let { diseaseCatalog.findById(it)?.displayName(Locale.getDefault()) }
                    ?: record.disease.customDiseaseId?.let { customDiseases[it]?.name }.orEmpty()
                RecordLatest(record.updatedAt, name)
            },
            RecordActionId.Medication to medication?.let { RecordLatest(it.timestamp, "${it.medicationName} ${it.doseValue} ${it.doseUnit}") },
            RecordActionId.Waist to circumference?.let { RecordLatest(it.recordedAtMillis, "${it.value} cm") },
        )
        val prefs = UserPrefs.current(context)
        val history = deserializeSearchHistory(prefs.getString(SEARCH_HISTORY_KEY, "[]"))
        val recentActions = deserializeSearchHistory(prefs.getString(RECENT_ACTIONS_KEY, "[]"))
            .mapNotNull { name -> RecordActionId.entries.firstOrNull { it.name == name } }
        _uiState.value = _uiState.value.copy(
            sections = defaultRecordSections().map { section ->
                section.copy(items = section.items.map { item ->
                    latest[item.id]?.let { item.copy(latestTimestamp = it.timestamp, latestValue = it.value) } ?: item
                })
            },
            searchHistory = history,
            recentActionIds = recentActions,
        )
    }

    private data class RecordLatest(val timestamp: Long, val value: String)
    private fun saveSearchHistory(history: List<String>) {
        UserPrefs.current(getApplication()).putString(SEARCH_HISTORY_KEY, serializeSearchHistory(history))
    }

    private fun saveRecentActions(actions: List<RecordActionId>) {
        UserPrefs.current(getApplication()).putString(RECENT_ACTIONS_KEY, serializeSearchHistory(actions.map { it.name }))
    }

    private companion object {
        const val SEARCH_HISTORY_KEY = "record_search_history_v1"
        const val RECENT_ACTIONS_KEY = "record_recent_actions_v1"
    }
}

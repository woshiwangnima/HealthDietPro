package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.disease.Disease
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRecordStatus
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecord
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecordRepository
import com.woshiwangnima.healthdietpro.model.disease.UserCustomDisease
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.disease.curatedId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

internal data class DiseaseRecordUiState(
    val records: List<UserDiseaseRecord> = emptyList(),
    val customDiseases: List<UserCustomDisease> = emptyList(),
    val userGender: Gender = Gender.MALE,
    val diseases: List<Disease> = emptyList(),
    val categoryLabels: Map<String, String> = emptyMap(),
    val departmentLabels: Map<String, String> = emptyMap(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedDepartmentIds: Set<String> = emptySet(),
    val selectedStatuses: Set<DiseaseRecordStatus> = emptySet(),
    val customOnly: Boolean = false,
    val query: String = "",
) {
    fun filtered(diseaseById: Map<String, Disease>): List<UserDiseaseRecord> = records.filter { record ->
        val disease = record.disease.curatedId()?.let(diseaseById::get)
        (selectedCategoryIds.isEmpty() || disease?.categoryIds?.any(selectedCategoryIds::contains) == true) &&
            (selectedDepartmentIds.isEmpty() || disease?.careDepartmentIds?.any(selectedDepartmentIds::contains) == true) &&
            (selectedStatuses.isEmpty() || record.status in selectedStatuses) &&
            (query.isBlank() || disease?.let { matchesDisease(it, query) } == true)
    }

    fun filteredCatalog(): List<Disease> = diseases.filter { disease ->
        !customOnly &&
        (selectedCategoryIds.isEmpty() || disease.categoryIds.any(selectedCategoryIds::contains)) &&
            (selectedDepartmentIds.isEmpty() || disease.careDepartmentIds.any(selectedDepartmentIds::contains)) &&
            (selectedStatuses.isEmpty() || records.any { record ->
                record.disease.curatedId() == disease.id && record.status in selectedStatuses
            }) &&
            (query.isBlank() || matchesDisease(disease, query))
    }

    fun customDiseaseById(): Map<String, UserCustomDisease> = customDiseases.associateBy { it.id }
}

internal class DiseaseRecordViewModel(application: Application) : AndroidViewModel(application) {
    private val diseaseRepository = DiseaseRepository.fromContext(application)
    private val recordRepository = UserDiseaseRecordRepository.fromContext(application)
    private val _uiState = MutableStateFlow(DiseaseRecordUiState())
    val uiState: StateFlow<DiseaseRecordUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val catalog = withContext(Dispatchers.IO) { diseaseRepository.loadCatalog() }
        val diseases = catalog.diseases
        val records = withContext(Dispatchers.IO) { recordRepository.load() }
        val customDiseases = withContext(Dispatchers.IO) { recordRepository.loadCustomDiseases() }
        val locale = Locale.getDefault()
        _uiState.value = _uiState.value.copy(
            diseases = diseases,
            records = records,
            customDiseases = customDiseases,
            userGender = ProfilePrefs.load(getApplication()).gender,
            categoryLabels = catalog.categories.associate { it.id to it.displayName(locale) },
            departmentLabels = catalog.departments.associate { it.id to it.displayName(locale) },
        )
    }

    fun setQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }
    fun toggleCategory(id: String) = update { copy(selectedCategoryIds = selectedCategoryIds.toggle(id), customOnly = false) }
    fun toggleCustomOnly() = update { copy(customOnly = !customOnly, selectedCategoryIds = emptySet()) }
    fun toggleDepartment(id: String) = update { copy(selectedDepartmentIds = selectedDepartmentIds.toggle(id)) }
    fun toggleStatus(status: DiseaseRecordStatus) = update { copy(selectedStatuses = selectedStatuses.toggle(status)) }

    fun upsert(record: UserDiseaseRecord) {
        val records = (_uiState.value.records.filterNot { it.id == record.id } + record).sortedByDescending { it.updatedAt }
        _uiState.value = _uiState.value.copy(records = records)
        viewModelScope.launch(Dispatchers.IO) { recordRepository.save(records) }
    }

    fun delete(id: String) {
        val records = _uiState.value.records.filterNot { it.id == id }
        _uiState.value = _uiState.value.copy(records = records)
        viewModelScope.launch(Dispatchers.IO) { recordRepository.save(records) }
    }

    fun upsertCustomDisease(disease: UserCustomDisease) {
        require(_uiState.value.customDiseases.none { it.id != disease.id && it.code.equals(disease.code, ignoreCase = true) }) {
            "Custom disease code must be unique"
        }
        val diseases = (_uiState.value.customDiseases.filterNot { it.id == disease.id } + disease)
            .sortedByDescending { it.updatedAt }
        _uiState.value = _uiState.value.copy(customDiseases = diseases)
        viewModelScope.launch(Dispatchers.IO) { recordRepository.saveCustomDiseases(diseases) }
    }

    private fun update(transform: DiseaseRecordUiState.() -> DiseaseRecordUiState) { _uiState.value = _uiState.value.transform() }
    private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
}

private fun matchesDisease(disease: Disease, query: String): Boolean {
    val normalized = query.lowercase(Locale.ROOT).filterNot(Char::isWhitespace)
    return buildList {
        add(disease.id)
        disease.icd11References.forEach { add(it.code) }
        disease.i18n.values.forEach { localized -> add(localized.label); addAll(localized.aliases) }
    }.any { it.lowercase(Locale.ROOT).filterNot(Char::isWhitespace).contains(normalized) }
}

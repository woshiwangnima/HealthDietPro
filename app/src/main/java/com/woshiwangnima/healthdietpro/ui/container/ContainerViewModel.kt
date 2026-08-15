package com.woshiwangnima.healthdietpro.ui.container

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.container.ContainerArchive
import com.woshiwangnima.healthdietpro.model.container.ContainerCategory
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.ContainerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 记容器 UI 状态：容器列表、场景标签注册表与筛选条件。 */
internal data class ContainerUiState(
    val containers: List<ContainerRecord> = emptyList(),
    val scenarioTags: List<String> = emptyList(),
    val selectedCategories: Set<ContainerCategory> = emptySet(),
    val selectedScenarioTags: Set<String> = emptySet(),
    val loading: Boolean = false,
)

internal class ContainerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContainerRepository.fromContext(application)
    private val _uiState = MutableStateFlow(ContainerUiState())
    val uiState: StateFlow<ContainerUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val archive: ContainerArchive = withContext(Dispatchers.IO) { repository.load() }
            _uiState.value = ContainerUiState(
                containers = archive.containers,
                scenarioTags = archive.scenarioTags,
                selectedCategories = _uiState.value.selectedCategories,
                selectedScenarioTags = _uiState.value.selectedScenarioTags,
            )
        }
    }

    fun upsert(record: ContainerRecord) {
        val updated = (_uiState.value.containers.filterNot { it.id == record.id } + record)
            .sortedByDescending(ContainerRecord::updatedAtMillis)
        _uiState.value = _uiState.value.copy(containers = updated)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.upsert(record) }
            refresh()
        }
    }

    fun delete(id: String) {
        _uiState.value = _uiState.value.copy(containers = _uiState.value.containers.filterNot { it.id == id })
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(id) }
            refresh()
        }
    }

    fun toggleCategoryFilter(category: ContainerCategory) {
        _uiState.value = _uiState.value.copy(
            selectedCategories = _uiState.value.selectedCategories.toggle(category),
        )
    }

    fun toggleScenarioTagFilter(tag: String) {
        _uiState.value = _uiState.value.copy(
            selectedScenarioTags = _uiState.value.selectedScenarioTags.toggle(tag),
        )
    }

    fun saveScenarioTags(tags: List<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateScenarioTags(tags) }
            refresh()
        }
    }

    /** Saves a picked image on the IO dispatcher and returns its relative path, or null on failure. */
    suspend fun saveImage(bitmap: android.graphics.Bitmap): String? =
        runCatching { withContext(Dispatchers.IO) { repository.saveImage(bitmap) } }.getOrNull()

    /** Loads an image bitmap for display on the IO dispatcher, or null. */
    suspend fun loadImage(relativePath: String): android.graphics.Bitmap? =
        runCatching { withContext(Dispatchers.IO) { repository.loadImage(relativePath) } }.getOrNull()

    /** Deletes an orphaned attachment (e.g. after removing an image in the editor). */
    fun deleteImage(relativePath: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repository.deleteImage(relativePath) } }
    }

    private fun Set<ContainerCategory>.toggle(category: ContainerCategory): Set<ContainerCategory> =
        if (category in this) this - category else this + category

    private fun Set<String>.toggle(tag: String): Set<String> =
        if (tag in this) this - tag else this + tag
}

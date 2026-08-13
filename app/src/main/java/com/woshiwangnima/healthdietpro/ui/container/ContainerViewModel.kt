package com.woshiwangnima.healthdietpro.ui.container

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.container.ContainerArchive
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.ContainerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 记容器 UI 状态：容器列表。 */
internal data class ContainerUiState(
    val containers: List<ContainerRecord> = emptyList(),
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
            _uiState.value = ContainerUiState(containers = archive.containers)
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
}

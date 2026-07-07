package com.woshiwangnima.healthdietpro.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    sealed interface Toast {
        data class Mb(val value: Float) : Toast
        data class Kb(val value: Int) : Toast
    }

    private val _toastMessage = MutableSharedFlow<Toast>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<Toast> = _toastMessage.asSharedFlow()

    init {
        UnitConverter.init(getApplication())
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                formatStorageSize(cacheTotalSize())
            }
            _uiState.value = _uiState.value.copy(cacheSizeText = text)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freedKb = withContext(Dispatchers.IO) {
                val app = getApplication<Application>()
                val before = cacheTotalSize()
                deleteRecursively(app.cacheDir, app)
                deleteRecursively(app.codeCacheDir, app)
                app.externalCacheDir?.let { deleteRecursively(it, app) }
                (before - cacheTotalSize()) / 1024
            }
            val toast = if (freedKb >= 1024) Toast.Mb(freedKb / 1024f) else Toast.Kb(freedKb.toInt())
            _toastMessage.emit(toast)
            refreshCacheSize()
        }
    }

    private fun cacheTotalSize(): Long {
        val app = getApplication<Application>()
        var total = 0L
        total += sizeOf(app.cacheDir)
        total += sizeOf(app.codeCacheDir)
        app.externalCacheDir?.let { total += sizeOf(it) }
        return total
    }

    private fun sizeOf(file: File): Long {
        if (file.isFile) return file.length()
        if (file.isDirectory) {
            var s = 0L
            file.listFiles()?.forEach { s += sizeOf(it) }
            return s
        }
        return 0
    }

    private fun deleteRecursively(file: File, app: Application) {
        if (file.isDirectory) file.listFiles()?.forEach { deleteRecursively(it, app) }
        if (file != app.cacheDir && file != app.codeCacheDir && file != app.externalCacheDir) {
            file.delete()
        }
    }

    private fun formatStorageSize(bytes: Long): String {
        val repo = UnitConverter.getRepository() ?: return formatLegacy(bytes)
        val storage = repo.getCategory(UnitCategoryType.Storage.id) ?: return formatLegacy(bytes)
        val best = storage.units.lastOrNull { bytes >= it.toBase }
            ?: storage.units.firstOrNull()
            ?: return formatLegacy(bytes)
        val converted = UnitConverter.fromBase(UnitCategoryType.Storage.id, bytes.toFloat(), best.id)
        val symbol = best.symbolCn
        return if (best.id == "b" || best.id == "kb") {
            "%.0f %s".format(converted, symbol)
        } else {
            "%.1f %s".format(converted, symbol)
        }
    }

    private fun formatLegacy(bytes: Long): String {
        val kb = bytes / 1024
        return if (kb >= 1024) "%.1f MB".format(kb / 1024f) else "$kb KB"
    }
}

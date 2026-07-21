package com.woshiwangnima.healthdietpro.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.HealthDietProApplication
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
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

internal class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val cacheRegistry: AppCacheRegistry = (application as HealthDietProApplication).cacheRegistry

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
            val entries = withContext(Dispatchers.IO) {
                cacheRegistry.snapshot()
            }
            _uiState.value = AppSettingsUiState(
                cacheSizeText = formatStorageSize(entries.sumOf { it.byteCount }),
                cacheEntries = entries.map { entry ->
                    CacheEntryUiState(
                        kind = entry.kind,
                        sizeText = formatStorageSize(entry.byteCount),
                        itemCount = entry.itemCount,
                    )
                },
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freedKb = withContext(Dispatchers.IO) {
                cacheRegistry.clearAll().releasedDiskBytes / 1024
            }
            val toast = if (freedKb >= 1024) Toast.Mb(freedKb / 1024f) else Toast.Kb(freedKb.toInt())
            _toastMessage.emit(toast)
            refreshCacheSize()
        }
    }

    private fun formatStorageSize(bytes: Long): String {
        val repo = UnitConverter.getRepository() ?: return formatLegacy(bytes)
        val storage = repo.getCategory(UnitCategoryType.Storage.id) ?: return formatLegacy(bytes)
        val best = storage.units.lastOrNull { bytes >= it.toBase }
            ?: storage.units.firstOrNull()
            ?: return formatLegacy(bytes)
        val converted = UnitConverter.fromBase(UnitCategoryType.Storage.id, bytes.toFloat(), best.id)
        val symbol = best.symbol()
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

package com.woshiwangnima.healthdietpro.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.HealthDietProApplication
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val cacheRegistry: AppCacheRegistry = (application as HealthDietProApplication).cacheRegistry

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    sealed interface Toast {
        data class Cleared(val sizeText: String, val partial: Boolean) : Toast
        data class Empty(val partial: Boolean) : Toast
    }

    private val _toastMessage = MutableSharedFlow<Toast>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<Toast> = _toastMessage.asSharedFlow()

    private var refreshJob: Job? = null

    init {
        UnitConverter.init(getApplication())
        refreshCacheSize()
    }

    /**
     * 重新统计缓存。扫描在 IO 线程执行，旧任务先取消，避免连续打开对话框时两次扫描互相覆盖。
     * 统计期间保留上一次的条目，只把 [AppSettingsUiState.cacheLoading] 置 true，
     * 界面因此可以先显示旧构成、再原位更新，而不是闪成空白。
     */
    fun refreshCacheSize() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(cacheLoading = true) }
            val snapshots = withContext(Dispatchers.IO) { cacheRegistry.snapshot() }
            val totalBytes = snapshots.sumOf { it.stats.byteCount }
            _uiState.value = AppSettingsUiState(
                cacheSizeText = formatStorageSize(totalBytes),
                cacheEntries = snapshots.map { snapshot ->
                    CacheEntryUiState(
                        id = snapshot.spec.id,
                        labelRes = snapshot.spec.labelRes,
                        byteCount = snapshot.stats.byteCount,
                        itemCount = snapshot.stats.itemCount,
                        sizeText = formatStorageSize(snapshot.stats.byteCount),
                        percent = if (totalBytes > 0L) {
                            (snapshot.stats.byteCount * 100.0 / totalBytes).roundToInt()
                        } else {
                            0
                        },
                        readFailed = snapshot.readFailed,
                    )
                },
                cacheLoading = false,
            )
        }
    }

    fun clearCache() {
        refreshJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(cacheLoading = true) }
            val result = withContext(Dispatchers.IO) { cacheRegistry.clearAll() }
            val partial = result.failedCacheIds.isNotEmpty()
            val toast = if (result.releasedBytes <= 0L && result.clearedItems <= 0) {
                Toast.Empty(partial = partial)
            } else {
                Toast.Cleared(sizeText = formatStorageSize(result.releasedBytes), partial = partial)
            }
            _toastMessage.emit(toast)
            refreshCacheSize()
        }
    }

    private fun formatStorageSize(bytes: Long): String {
        val repo = UnitConverter.getRepository() ?: return formatLegacy(bytes)
        val storage = repo.getCategory(UnitCategoryType.Storage.id) ?: return formatLegacy(bytes)
        // 缓存通常只有 KB～MB 级；沿用「首选存储单位」会在用户选 GB 时显示成 0.0 GB，
        // 所以这里按实际字节数量级挑最大的可用单位，0 字节回落到最小单位。
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

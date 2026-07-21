package com.woshiwangnima.healthdietpro.ui.settings

import com.woshiwangnima.healthdietpro.common.cache.AppCacheKind

internal data class AppSettingsUiState(
    val cacheSizeText: String = "",
    val cacheEntries: List<CacheEntryUiState> = emptyList(),
)

internal data class CacheEntryUiState(
    val kind: AppCacheKind,
    val sizeText: String,
    val itemCount: Int,
)

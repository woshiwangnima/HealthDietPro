package com.woshiwangnima.healthdietpro.ui.settings

import androidx.annotation.StringRes

internal data class AppSettingsUiState(
    val cacheSizeText: String = "",
    val cacheEntries: List<CacheEntryUiState> = emptyList(),
    val cacheLoading: Boolean = false,
)

/**
 * 设置页「清除缓存」的一行：文案资源由缓存的 [com.woshiwangnima.healthdietpro.common.cache.AppCacheSpec]
 * 自带，设置页不再维护 kind → 文案的映射表；新模块注册后自动出现在构成条与图例里。
 *
 * [percent] 按字节占比四舍五入；[readFailed] 为 true 时本次读不到占用，界面显示「无法读取」而不是 0 B。
 */
internal data class CacheEntryUiState(
    val id: String,
    @param:StringRes val labelRes: Int,
    val byteCount: Long,
    val itemCount: Int,
    val sizeText: String,
    val percent: Int,
    val readFailed: Boolean = false,
)

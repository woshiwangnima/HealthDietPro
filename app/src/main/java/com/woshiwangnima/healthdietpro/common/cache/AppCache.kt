package com.woshiwangnima.healthdietpro.common.cache

import androidx.annotation.StringRes

/** 缓存所在层：磁盘缓存可跨进程保留，内存缓存随进程 / 持有者生命周期消失。 */
internal enum class AppCacheStorage { DISK, MEMORY }

/**
 * 一个可清理缓存的注册信息。
 *
 * [id] 是稳定且全局唯一的键（小写字母开头，只含小写字母 / 数字 / 下划线），用于重复注册检查与
 * 日志定位；[labelRes] 是设置页「清除缓存」里显示的文案，**必须由缓存自己声明**——
 * 设置页不再维护 kind → 文案的映射表，新模块注册后自动出现在构成条与图例里。
 * [storage] 只描述层级，不参与清理决策：所有注册进 [AppCacheRegistry] 的缓存都必须是可再生的。
 */
internal data class AppCacheSpec(
    val id: String,
    @param:StringRes val labelRes: Int,
    val storage: AppCacheStorage,
)

internal data class AppCacheStats(
    val byteCount: Long,
    val itemCount: Int,
) {
    operator fun plus(other: AppCacheStats) = AppCacheStats(
        byteCount = byteCount + other.byteCount,
        itemCount = itemCount + other.itemCount,
    )
}

/**
 * 一次清理的账目：释放了多少、清掉了多少、还剩多少。
 *
 * `remaining*` 用于识别「文件被占用 / 清理失败」：只要它不为 0，[AppCacheRegistry] 就把该缓存
 * 记进失败清单，设置页可以提示「部分文件被占用」，而不是假装全部清干净。
 */
internal data class AppCacheClearOutcome(
    val releasedBytes: Long,
    val clearedItems: Int,
    val remainingBytes: Long,
    val remainingItems: Int,
)

/**
 * 可清理缓存的统一契约。
 *
 * 新增缓存时实现本接口，并向 [AppCacheRegistry] 注册；设置页从 `snapshot()` 自动派生
 * 构成条、图例、百分比与清理动作，**不需要改设置页代码**。注册由
 * [AppCacheRegistry.register] 返回的 [AppCacheRegistration] 管理生命周期：
 * 持有者（ViewModel / Activity）在 `onCleared()` / `onDestroy()` 里 `close()`，避免
 * 应用级注册表长期持有已废弃的缓存。
 *
 * [clear] 默认按「清理前后各统计一次」记账；实现方只需在 [clearStorage] 里真正删除内容。
 * 任何异常都由注册表捕获并计入失败清单，一个缓存坏掉不会阻断其它缓存。
 */
internal interface AppCache {
    val spec: AppCacheSpec

    fun stats(): AppCacheStats

    fun clearStorage()

    fun clear(): AppCacheClearOutcome {
        val before = stats()
        clearStorage()
        val after = stats()
        return AppCacheClearOutcome(
            releasedBytes = (before.byteCount - after.byteCount).coerceAtLeast(0L),
            clearedItems = (before.itemCount - after.itemCount).coerceAtLeast(0),
            remainingBytes = after.byteCount,
            remainingItems = after.itemCount,
        )
    }
}

/** 单次快照：读取失败的缓存仍保留在列表里，用 [readFailed] 区分「0 B」与「读不出来」。 */
internal data class AppCacheSnapshot(
    val spec: AppCacheSpec,
    val stats: AppCacheStats,
    val readFailed: Boolean = false,
)

/** 整次清理的结果；[failedCacheIds] 非空表示至少一个缓存没清干净或被占用。 */
internal data class AppCacheClearResult(
    val releasedBytes: Long,
    val clearedItems: Int,
    val failedCacheIds: List<String>,
)

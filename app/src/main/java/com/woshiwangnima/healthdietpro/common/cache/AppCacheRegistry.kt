package com.woshiwangnima.healthdietpro.common.cache

import android.app.Application
import java.io.File

internal enum class AppCacheKind {
    AppFiles,
    CodeFiles,
    ExternalFiles,
    FoodImages,
    ProfileAvatars,
}

internal data class AppCacheEntry(
    val kind: AppCacheKind,
    val byteCount: Long,
    val itemCount: Int,
)

internal data class AppCacheClearResult(
    val releasedDiskBytes: Long,
    val clearedMemoryItems: Int,
)

internal interface ClearableMemoryCache {
    val cacheKind: AppCacheKind

    fun cacheEntry(): AppCacheEntry

    fun clearCache()
}

internal class AppCacheRegistry(private val application: Application) {
    private val memoryCaches = LinkedHashSet<ClearableMemoryCache>()

    @Synchronized
    fun register(memoryCache: ClearableMemoryCache) {
        memoryCaches += memoryCache
    }

    fun snapshot(): List<AppCacheEntry> {
        val diskEntries = listOfNotNull(
            directoryEntry(AppCacheKind.AppFiles, application.cacheDir),
            directoryEntry(AppCacheKind.CodeFiles, application.codeCacheDir),
            application.externalCacheDir?.let { directoryEntry(AppCacheKind.ExternalFiles, it) },
        )
        val memoryEntries = synchronized(this) { memoryCaches.toList() }.map { it.cacheEntry() }
        return diskEntries + memoryEntries
    }

    fun clearAll(): AppCacheClearResult {
        val beforeDiskBytes = snapshot().filter { it.kind.isDiskCache() }.sumOf { it.byteCount }
        clearDirectoryContents(application.cacheDir)
        clearDirectoryContents(application.codeCacheDir)
        application.externalCacheDir?.let(::clearDirectoryContents)

        val memoryEntries = synchronized(this) { memoryCaches.toList() }.map { it.cacheEntry() }
        synchronized(this) { memoryCaches.toList() }.forEach { it.clearCache() }

        val afterDiskBytes = snapshot().filter { it.kind.isDiskCache() }.sumOf { it.byteCount }
        return AppCacheClearResult(
            releasedDiskBytes = (beforeDiskBytes - afterDiskBytes).coerceAtLeast(0L),
            clearedMemoryItems = memoryEntries.sumOf { it.itemCount },
        )
    }

    private fun directoryEntry(kind: AppCacheKind, directory: File): AppCacheEntry {
        val stats = directory.stats()
        return AppCacheEntry(kind, stats.byteCount, stats.itemCount)
    }

    private fun clearDirectoryContents(directory: File) {
        directory.listFiles()?.forEach(::deleteRecursively)
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) file.listFiles()?.forEach(::deleteRecursively)
        file.delete()
    }

    private fun File.stats(): CacheFileStats {
        if (isFile) return CacheFileStats(length(), 1)
        if (!isDirectory) return CacheFileStats(0L, 0)
        return listFiles()?.fold(CacheFileStats(0L, 0)) { total, child -> total + child.stats() }
            ?: CacheFileStats(0L, 0)
    }
}

private data class CacheFileStats(
    val byteCount: Long,
    val itemCount: Int,
) {
    operator fun plus(other: CacheFileStats) = CacheFileStats(
        byteCount = byteCount + other.byteCount,
        itemCount = itemCount + other.itemCount,
    )
}

private fun AppCacheKind.isDiskCache(): Boolean = when (this) {
    AppCacheKind.AppFiles,
    AppCacheKind.CodeFiles,
    AppCacheKind.ExternalFiles -> true
    AppCacheKind.FoodImages,
    AppCacheKind.ProfileAvatars -> false
}

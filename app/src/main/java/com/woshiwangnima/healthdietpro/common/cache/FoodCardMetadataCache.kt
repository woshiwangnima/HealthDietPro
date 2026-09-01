package com.woshiwangnima.healthdietpro.common.cache

import com.woshiwangnima.healthdietpro.model.food.FoodCardMetadata
import android.util.Log

internal class FoodCardMetadataCache(
    private val maxEntries: Int = 512,
) : ClearableMemoryCache {
    private val entries = object : LinkedHashMap<String, FoodCardMetadata>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FoodCardMetadata>?): Boolean = size > maxEntries
    }

    override val cacheKind = AppCacheKind.FoodCardMetadata

    @Synchronized
    fun get(key: String): FoodCardMetadata? = entries[key]

    @Synchronized
    fun put(key: String, value: FoodCardMetadata) {
        entries[key] = value
    }

    @Synchronized
    override fun clearCache() {
        Log.d(TAG, "clear metadata cache: ${entries.size} entries")
        entries.clear()
    }

    fun invalidate() = clearCache()

    @Synchronized
    override fun cacheEntry(): AppCacheEntry = AppCacheEntry(cacheKind, entries.size * 512L, entries.size)

    private companion object {
        const val TAG = "FoodCardCache"
    }
}

package com.woshiwangnima.healthdietpro.common.cache

import android.util.Log
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.food.FoodCardMetadata

internal class FoodCardMetadataCache(
    private val maxEntries: Int = 512,
) : AppCache {
    private val entries = object : LinkedHashMap<String, FoodCardMetadata>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FoodCardMetadata>?): Boolean = size > maxEntries
    }

    override val spec = AppCacheSpec(
        id = "food_card_metadata",
        labelRes = R.string.settings_cache_entry_food_card_metadata,
        storage = AppCacheStorage.MEMORY,
    )

    @Synchronized
    fun get(key: String): FoodCardMetadata? = entries[key]

    @Synchronized
    fun put(key: String, value: FoodCardMetadata) {
        entries[key] = value
    }

    @Synchronized
    override fun clearStorage() {
        Log.d(TAG, "clear metadata cache: ${entries.size} entries")
        entries.clear()
    }

    fun invalidate() = clearStorage()

    @Synchronized
    override fun stats(): AppCacheStats = AppCacheStats(
        byteCount = entries.size * EstimatedEntryBytes,
        itemCount = entries.size,
    )

    private companion object {
        const val TAG = "FoodCardCache"

        /** 卡片元数据是几条字符串 + 列表，512 B 只是用于展示量级的保守估计，不作为内存上限。 */
        const val EstimatedEntryBytes = 512L
    }
}

package com.woshiwangnima.healthdietpro.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.cache.AppCache
import com.woshiwangnima.healthdietpro.common.cache.AppCacheSpec
import com.woshiwangnima.healthdietpro.common.cache.AppCacheStats
import com.woshiwangnima.healthdietpro.common.cache.AppCacheStorage
import java.util.LinkedHashMap

internal class ProfileAvatarBitmapCache(
    private val maxEntries: Int = 12,
) : AppCache {
    private val bitmaps = LinkedHashMap<String, Bitmap>(maxEntries, 0.75f, true)

    override val spec = AppCacheSpec(
        id = "profile_avatars",
        labelRes = R.string.settings_cache_entry_profile_avatars,
        storage = AppCacheStorage.MEMORY,
    )

    init {
        require(maxEntries > 0)
    }

    @Synchronized
    fun load(path: String): Bitmap? {
        bitmaps[path]?.let { return it }
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        bitmaps[path] = bitmap
        if (bitmaps.size > maxEntries) {
            bitmaps.entries.iterator().run {
                next()
                remove()
            }
        }
        return bitmap
    }

    @Synchronized
    override fun stats(): AppCacheStats = AppCacheStats(
        byteCount = bitmaps.values.sumOf { it.allocationByteCount.toLong() },
        itemCount = bitmaps.size,
    )

    @Synchronized
    override fun clearStorage() {
        bitmaps.clear()
    }
}

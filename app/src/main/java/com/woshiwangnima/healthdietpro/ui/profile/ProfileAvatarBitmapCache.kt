package com.woshiwangnima.healthdietpro.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.woshiwangnima.healthdietpro.common.cache.AppCacheEntry
import com.woshiwangnima.healthdietpro.common.cache.AppCacheKind
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
import com.woshiwangnima.healthdietpro.common.cache.ClearableMemoryCache
import java.util.LinkedHashMap

internal class ProfileAvatarBitmapCache(
    private val maxEntries: Int = 12,
    cacheRegistry: AppCacheRegistry? = null,
) : ClearableMemoryCache {
    private val bitmaps = LinkedHashMap<String, Bitmap>(maxEntries, 0.75f, true)

    override val cacheKind = AppCacheKind.ProfileAvatars

    init {
        require(maxEntries > 0)
        cacheRegistry?.register(this)
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
    override fun cacheEntry(): AppCacheEntry = AppCacheEntry(
        kind = cacheKind,
        byteCount = bitmaps.values.sumOf { it.allocationByteCount.toLong() },
        itemCount = bitmaps.size,
    )

    @Synchronized
    override fun clearCache() {
        bitmaps.clear()
    }
}

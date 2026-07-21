package com.woshiwangnima.healthdietpro.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.cache.AppCacheEntry
import com.woshiwangnima.healthdietpro.common.cache.AppCacheKind
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
import com.woshiwangnima.healthdietpro.common.cache.ClearableMemoryCache

internal class FoodImageStore(
    private val context: Context,
    cacheRegistry: AppCacheRegistry? = null,
) : ClearableMemoryCache {
    private val cache = LruCache<String, ImageBitmap>(24)

    override val cacheKind = AppCacheKind.FoodImages

    init {
        cacheRegistry?.register(this)
    }

    fun clear() {
        cache.evictAll()
    }

    override fun clearCache() = clear()

    override fun cacheEntry(): AppCacheEntry {
        val images = cache.snapshot().values
        val byteCount = images.sumOf { it.width.toLong() * it.height * 4L }
        return AppCacheEntry(cacheKind, byteCount, images.size)
    }

    fun image(key: String?): ImageBitmap = load(key ?: DEFAULT_KEY)

    private fun load(key: String): ImageBitmap = cache.get(key) ?: render(resourceFor(key)).also {
        cache.put(key, it)
    }

    private fun render(@DrawableRes resourceId: Int): ImageBitmap {
        val drawable = requireNotNull(ContextCompat.getDrawable(context, resourceId))
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        return bitmap.asImageBitmap()
    }

    private fun resourceFor(key: String): Int = when (key) {
        DEFAULT_KEY -> R.drawable.ic_food_illustration
        else -> R.drawable.ic_food_illustration
    }

    companion object {
        const val DEFAULT_KEY = "food.illustration.default"
    }
}

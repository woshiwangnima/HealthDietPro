package com.woshiwangnima.healthdietpro.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.cache.AppCacheEntry
import com.woshiwangnima.healthdietpro.common.cache.AppCacheKind
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
import com.woshiwangnima.healthdietpro.common.cache.ClearableMemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class FoodImageStore(
    private val context: Context,
    cacheRegistry: AppCacheRegistry? = null,
) : ClearableMemoryCache {
    private val cache = object : LruCache<String, ImageBitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width.toLong() * value.height.toLong() * 4L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
    private val imagePaths by lazy { loadImageManifest() }

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

    fun image(key: String?): ImageBitmap = load(key ?: DEFAULT_KEY, ImageVariant.THUMB)

    @Composable
    fun rememberImage(key: String?, variant: ImageVariant): ImageBitmap {
        val placeholder = remember { fallback() }
        val value by produceState(placeholder, key, variant) {
            value = withContext(Dispatchers.IO) { load(key ?: DEFAULT_KEY, variant) }
        }
        return value
    }

    private fun load(key: String, variant: ImageVariant): ImageBitmap {
        val cacheKey = "$key#${variant.name}"
        return cache.get(cacheKey) ?: render(key, variant).also { cache.put(cacheKey, it) }
    }

    private fun render(key: String, variant: ImageVariant): ImageBitmap {
        if (key.startsWith(USER_KEY_PREFIX)) {
            userImage(key, variant)?.let { return it.asImageBitmap() }
        }
        imagePaths[key]?.get(variant)?.let { path ->
            context.assets.open(path).use { input ->
                decodeSampled(input, variant.maxPixels)?.let { return it.asImageBitmap() }
            }
        }
        return renderResource(resourceFor(key))
    }

    private fun fallback(): ImageBitmap = renderResource(R.drawable.ic_food_illustration)

    private fun userImage(key: String, variant: ImageVariant): Bitmap? {
        val relativePath = key.removePrefix(USER_KEY_PREFIX)
        val root = context.filesDir.canonicalFile
        val imageFile = java.io.File(root, relativePath).canonicalFile
        return imageFile.takeIf { it.path.startsWith(root.path + java.io.File.separator) && it.isFile }
            ?.inputStream()?.use { decodeSampled(it, variant.maxPixels) }
    }

    private fun loadImageManifest(): Map<String, Map<ImageVariant, String>> = runCatching {
        val raw = context.assets.open("food_catalog/images/manifest.json").bufferedReader().use { it.readText() }
        val manifest = imageJson.decodeFromString<FoodImageManifest>(raw)
        manifest.images.mapValues { (_, paths) ->
            buildMap {
                paths.thumb?.let { put(ImageVariant.THUMB, "food_catalog/$it") }
                paths.detail?.let { put(ImageVariant.DETAIL, "food_catalog/$it") }
            }
        }
    }.getOrDefault(emptyMap())

    private fun decodeSampled(input: java.io.InputStream, maxPixels: Int): Bitmap? {
        val bytes = input.readBytes()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxPixels)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int, maxPixels: Int): Int {
        var sample = 1
        while (width / sample > maxPixels || height / sample > maxPixels) sample *= 2
        return sample
    }

    private fun renderResource(@DrawableRes resourceId: Int): ImageBitmap {
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
        private const val USER_KEY_PREFIX = "user:"
    }
}

internal enum class ImageVariant(val maxPixels: Int) { THUMB(160), DETAIL(640) }

@Serializable
private data class FoodImageManifest(val images: Map<String, FoodImagePaths> = emptyMap())

@Serializable
private data class FoodImagePaths(val thumb: String? = null, val detail: String? = null)

private val imageJson = Json { ignoreUnknownKeys = true }

package com.woshiwangnima.healthdietpro.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.LinkedHashMap

internal class ProfileAvatarBitmapCache(
    private val maxEntries: Int = 12,
) {
    private val bitmaps = LinkedHashMap<String, Bitmap>(maxEntries, 0.75f, true)

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
}

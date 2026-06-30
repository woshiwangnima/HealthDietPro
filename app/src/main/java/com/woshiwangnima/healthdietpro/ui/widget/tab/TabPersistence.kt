package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context

object TabPersistence {
    private const val PREFS_NAME = "app_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveIndex(context: Context, key: String, index: Int) {
        prefs(context).edit().putInt(key, index).apply()
    }

    fun loadIndex(context: Context, key: String, default: Int = -1): Int =
        prefs(context).getInt(key, default)

    fun saveSet(context: Context, key: String, set: Set<Int>) {
        val stringSet = set.map { it.toString() }.toSet()
        prefs(context).edit().putStringSet(key, stringSet).apply()
    }

    fun loadSet(context: Context, key: String): Set<Int> =
        prefs(context).getStringSet(key, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
}
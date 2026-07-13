package com.woshiwangnima.healthdietpro.model.chart

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class ComposeChartStateRepository private constructor(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(chartStateKey: String): ComposeChartState? {
        if (chartStateKey.isEmpty()) return null
        val encoded = UserPrefs.current(context).getString(storageKey(chartStateKey), String(CharArray(0)))
        if (encoded.isEmpty()) return null
        return runCatching { json.decodeFromString<ComposeChartState>(encoded) }.getOrNull()
    }

    fun save(chartStateKey: String, state: ComposeChartState) {
        if (chartStateKey.isEmpty()) return
        UserPrefs.current(context).putString(storageKey(chartStateKey), json.encodeToString(state))
    }

    private fun storageKey(chartStateKey: String): String =
        PREFIX + chartStateKey

    companion object {
        val PREFIX = charArrayOf(
            'c', 'o', 'm', 'p', 'o', 's', 'e', '_', 'c', 'h', 'a', 'r', 't', '_',
            's', 't', 'a', 't', 'e', '_',
        ).concatToString()

        fun fromContext(context: Context): ComposeChartStateRepository =
            ComposeChartStateRepository(context.applicationContext)
    }
}

package com.woshiwangnima.healthdietpro.model.prefs

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val searchHistoryJson = Json { ignoreUnknownKeys = true }

internal fun serializeSearchHistory(entries: List<String>): String =
    searchHistoryJson.encodeToString(entries)

internal fun deserializeSearchHistory(value: String): List<String> = runCatching {
    searchHistoryJson.decodeFromString<List<String>>(value)
}.getOrElse {
    value.split(LEGACY_HISTORY_SEPARATOR).filter { it.isNotBlank() }
}

private const val LEGACY_HISTORY_SEPARATOR = "\u001F"

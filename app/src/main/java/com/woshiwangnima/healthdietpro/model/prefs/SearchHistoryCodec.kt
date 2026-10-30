package com.woshiwangnima.healthdietpro.model.prefs

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val searchHistoryJson = Json { ignoreUnknownKeys = true }

internal fun serializeSearchHistory(entries: List<String>): String =
    searchHistoryJson.encodeToString(entries)

internal fun deserializeSearchHistory(value: String): List<String> = runCatching {
    searchHistoryJson.decodeFromString<List<String>>(value)
}.getOrElse {
    if (LEGACY_HISTORY_SEPARATOR in value) {
        value.split(LEGACY_HISTORY_SEPARATOR).filter { it.isNotBlank() }
    } else {
        emptyList()
    }
}

/**
 * 搜索历史是个定长栈：新词进栈顶、同名（忽略大小写）的旧条目被顶掉，超出 [limit] 的最旧条目出栈。
 *
 * 记录中心与营养表共用这一条规则，避免两边各写一份 `take(n)` 后容量又走散。
 */
internal fun pushSearchHistory(
    history: List<String>,
    term: String,
    limit: Int = SEARCH_HISTORY_LIMIT,
): List<String> {
    val normalized = term.trim()
    require(limit > 0) { "limit must be positive" }
    if (normalized.isEmpty()) return history.take(limit)
    return (listOf(normalized) + history.filterNot { it.equals(normalized, ignoreCase = true) }).take(limit)
}

/** 搜索历史栈的默认容量：10 条（搜索面板按同一上限展示）。 */
internal const val SEARCH_HISTORY_LIMIT = 10

private const val LEGACY_HISTORY_SEPARATOR = "\u001F"

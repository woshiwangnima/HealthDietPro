package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class UserItemCollectionState(
    val favoriteIds: List<String> = emptyList(),
    val recentIds: List<String> = emptyList(),
)

/** Per-user favorites and click history for one domain, persisted atomically in UserPrefs. */
internal class UserItemCollectionRepository private constructor(
    private val context: Context,
    private val storageKey: String,
    private val maxRecentItems: Int,
) {
    fun load(validIds: Set<String>, legacyRecentIds: List<String> = emptyList()): UserItemCollectionState {
        val prefs = UserPrefs.current(context)
        val stored = if (prefs.contains(storageKey)) read(prefs) else UserItemCollectionState(recentIds = legacyRecentIds)
        val normalized = normalize(stored, validIds)
        if (!prefs.contains(storageKey) || normalized != stored) write(normalized)
        return normalized
    }

    fun toggleFavorite(id: String, validIds: Set<String>): UserItemCollectionState {
        val current = load(validIds)
        val favorites = if (id in current.favoriteIds) current.favoriteIds - id else current.favoriteIds + id
        return save(current.copy(favoriteIds = favorites), validIds)
    }

    fun recordRecent(id: String, validIds: Set<String>): UserItemCollectionState = save(
        load(validIds).let { current ->
            current.copy(recentIds = (listOf(id) + current.recentIds.filterNot { it == id }).take(maxRecentItems))
        },
        validIds,
    )

    fun removeRecent(id: String, validIds: Set<String>): UserItemCollectionState {
        val current = load(validIds)
        return save(current.copy(recentIds = current.recentIds - id), validIds)
    }

    fun clearRecents(validIds: Set<String>): UserItemCollectionState =
        save(load(validIds).copy(recentIds = emptyList()), validIds)

    private fun save(state: UserItemCollectionState, validIds: Set<String>): UserItemCollectionState =
        normalize(state, validIds).also(::write)

    private fun normalize(state: UserItemCollectionState, validIds: Set<String>) = state.copy(
        favoriteIds = state.favoriteIds.distinct().filter { it in validIds },
        recentIds = state.recentIds.distinct().filter { it in validIds }.take(maxRecentItems),
    )

    private fun read(prefs: UserPrefsScope = UserPrefs.current(context)): UserItemCollectionState = runCatching {
        json.decodeFromString(UserItemCollectionState.serializer(), prefs.getString(storageKey, "{}"))
    }.getOrDefault(UserItemCollectionState())

    private fun write(state: UserItemCollectionState) {
        UserPrefs.current(context).putString(storageKey, json.encodeToString(UserItemCollectionState.serializer(), state))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromContext(context: Context, storageKey: String, maxRecentItems: Int = 6): UserItemCollectionRepository {
            require(storageKey.isNotBlank())
            require(maxRecentItems > 0)
            return UserItemCollectionRepository(context.applicationContext, storageKey, maxRecentItems)
        }
    }
}

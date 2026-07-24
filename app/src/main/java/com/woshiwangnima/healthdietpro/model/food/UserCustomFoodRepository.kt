package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Per-user custom foods (食材 / 食物 / 菜肴). Stored as serializable [FoodDto] list in the
 * user archive so they map to the same domain hierarchy as built-in foods. Custom ids use the
 * [CUSTOM_ID_PREFIX] so the browser can pin/filter them per kind.
 */
internal class UserCustomFoodRepository private constructor(
    private val context: Context,
    private val userId: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadDtos(): List<FoodDto> = runCatching {
        json.decodeFromString<List<FoodDto>>(UserPrefs.forUser(context, userId).getString(KEY, "[]"))
    }.getOrDefault(emptyList())

    fun load(): List<FoodItem> = loadDtos().map { it.toDomain() }

    fun save(dtos: List<FoodDto>) {
        UserPrefs.forUser(context, userId).putString(KEY, json.encodeToString(dtos))
    }

    /** Insert or replace by id, then persist. Returns the updated list. */
    fun upsert(dto: FoodDto): List<FoodDto> {
        val current = loadDtos().filterNot { it.id == dto.id }
        val updated = current + dto
        save(updated)
        return updated
    }

    fun delete(id: String): List<FoodDto> {
        val updated = loadDtos().filterNot { it.id == id }
        save(updated)
        return updated
    }

    companion object {
        internal const val CUSTOM_ID_PREFIX = "custom:"
        private const val KEY = "nutrition_custom_foods_v1"

        fun fromContext(context: Context) = UserCustomFoodRepository(
            context = context.applicationContext,
            userId = ProfilePrefs.getCurrentUserId(context),
        )

        fun isCustom(id: String): Boolean = id.startsWith(CUSTOM_ID_PREFIX)
    }
}

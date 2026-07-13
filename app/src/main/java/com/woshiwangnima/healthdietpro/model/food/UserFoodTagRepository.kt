package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class UserFoodTag(val id: String, val label: String)

internal class UserFoodTagRepository private constructor(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    fun load(userId: String): List<UserFoodTag> = runCatching {
        json.decodeFromString<List<UserFoodTag>>(UserPrefs.forUser(context, userId).getString(KEY, "[]"))
    }.getOrDefault(emptyList())
    fun save(userId: String, tags: List<UserFoodTag>) {
        UserPrefs.forUser(context, userId).putString(KEY, json.encodeToString(tags))
    }
    companion object {
        private const val KEY = "nutrition_user_food_tags_v1"
        fun fromContext(context: Context) = UserFoodTagRepository(context.applicationContext)
    }
}

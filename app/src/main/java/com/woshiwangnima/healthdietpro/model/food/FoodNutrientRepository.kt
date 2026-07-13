package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class FoodAsset(val foods: List<Food>)

@Serializable
internal data class Food(
    val id: String,
    val name: Map<String, String>,
    val aliases: List<String> = emptyList(),
    val categoryTag: String,
    val nutrientsPer100g: Map<String, Double>,
) {
    fun displayName(language: String): String = name[language] ?: name["en"] ?: name.values.firstOrNull().orEmpty()
}

internal class FoodNutrientRepository private constructor(private val source: () -> String) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<Food>? = null
    fun foods(): List<Food> = cache ?: json.decodeFromString<FoodAsset>(source()).foods.also { cache = it }
    companion object {
        fun fromContext(context: Context) = FoodNutrientRepository {
            context.assets.open("food_nutrition.json").bufferedReader().use { it.readText() }
        }
        fun fromAsset(path: String) = FoodNutrientRepository { java.io.File(path).readText() }
    }
}

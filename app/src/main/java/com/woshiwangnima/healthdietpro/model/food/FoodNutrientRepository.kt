package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.json.Json

internal class FoodNutrientRepository private constructor(private val source: () -> String) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<FoodItem>? = null
    private var index: Map<String, FoodItem>? = null

    fun foods(): List<FoodItem> = cache ?: json.decodeFromString<FoodAsset>(source()).foods
        .map { it.toDomain() }
        .also { cache = it }

    fun byId(): Map<String, FoodItem> = index ?: foods().associateBy { it.id }.also { index = it }

    fun find(id: String): FoodItem? = byId()[id]

    companion object {
        fun fromContext(context: Context) = FoodNutrientRepository {
            context.assets.open("food_nutrition.json").bufferedReader().use { it.readText() }
        }
        fun fromAsset(path: String) = FoodNutrientRepository { java.io.File(path).readText() }
    }
}

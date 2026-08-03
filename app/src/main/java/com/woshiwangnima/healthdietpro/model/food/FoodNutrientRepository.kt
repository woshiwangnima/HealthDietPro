package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.json.Json

internal class FoodNutrientRepository private constructor(
    private val source: () -> String,
    private val nutrientCodes: () -> Set<String> = { emptySet() },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<FoodItem>? = null
    private var index: Map<String, FoodItem>? = null

    fun foods(): List<FoodItem> = cache ?: json.decodeFromString<FoodAsset>(source()).foods
        .map { it.toDomain() }
        .also { foods ->
            val codes = nutrientCodes()
            if (codes.isNotEmpty()) require(foods.all { food ->
                val tables = when (food) {
                    is Ingredient -> food.nutritionTables.values
                    else -> emptyList()
                }
                tables.all { table -> table.nutrients.keys.all(codes::contains) }
            }) { "Food nutrient code is absent from nutrients_meta.json" }
        }
        .also { cache = it }

    fun byId(): Map<String, FoodItem> = index ?: foods().associateBy { it.id }.also { index = it }

    fun find(id: String): FoodItem? = byId()[id]

    companion object {
        fun fromContext(context: Context) = FoodNutrientRepository(
            source = { context.assets.open("food_nutrition.json").bufferedReader().use { it.readText() } },
            nutrientCodes = { NutrientMetaRepository.fromContext(context).nutrients().mapTo(mutableSetOf(), NutrientMeta::code) },
        )
        fun fromAsset(path: String) = FoodNutrientRepository(source = { java.io.File(path).readText() })
    }
}

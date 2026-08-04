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

    fun categoryRoots(): List<FoodCategory> = FoodCategories.roots

    fun categoryChildren(parentTag: String): List<FoodCategory> = FoodCategories.childrenOf(parentTag)

    fun categoryTree(): List<FoodCategory> = FoodCategories.roots + FoodCategories.children

    fun categoryDisplayPath(tag: String): List<Int> = FoodCategories.displayTagPath(tag)

    fun hasCategory(tags: List<String>, ancestor: String): Boolean = FoodCategories.hasTagWithin(tags, ancestor)

    fun hasAnyCategory(tags: List<String>, ancestors: Set<String>): Boolean =
        FoodCategories.hasTagWithinAny(tags, ancestors)

    fun retainCategoryChildren(selected: Set<String>, roots: Set<String>): Set<String> =
        FoodCategories.retainChildrenForRoots(selected, roots)

    fun isSeasoning(food: FoodItem): Boolean =
        hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), "food.seasoning")

    fun isAuxiliary(food: FoodItem): Boolean =
        hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), "food.seasoning") ||
            hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), "food.oil")

    fun foodsWithin(categoryTag: String): List<FoodItem> = foods().filter { food ->
        hasCategory((food as? CategorizedFood)?.categoryTags.orEmpty(), categoryTag)
    }

    companion object {
        fun fromContext(context: Context) = FoodNutrientRepository(
            source = { context.assets.open("food_nutrition.json").bufferedReader().use { it.readText() } },
            nutrientCodes = { NutrientMetaRepository.fromContext(context).nutrients().mapTo(mutableSetOf(), NutrientMeta::code) },
        )
        fun fromAsset(path: String) = FoodNutrientRepository(source = { java.io.File(path).readText() })
    }
}

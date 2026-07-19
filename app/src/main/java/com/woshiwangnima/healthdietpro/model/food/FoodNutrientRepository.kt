package com.woshiwangnima.healthdietpro.model.food

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class FoodAsset(val foods: List<Food>)

@Serializable
internal data class Food(
    val id: String,
    val names: Map<String, List<String>>,
    val categoryTags: List<String>,
    val nutritionTables: Map<String, FoodNutrientTable> = emptyMap(),
    val nutrients: Map<String, FoodAmount> = emptyMap(),
    val healthMetrics: FoodHealthMetrics = FoodHealthMetrics(),
    val servings: List<FoodServing> = emptyList(),
    val densityGramsPerMl: Double? = null,
    val description: Map<String, String> = emptyMap(),
    val image: FoodImage? = null,
    val sources: List<FoodSource> = emptyList(),
) {
    fun displayName(language: String): String = names[language]?.firstOrNull()
        ?: names["en"]?.firstOrNull()
        ?: names.values.firstOrNull()?.firstOrNull().orEmpty()

    fun allNames(language: String): List<String> = names[language]
        ?: names["en"]
        ?: names.values.firstOrNull().orEmpty()

    fun searchableNames(): List<String> = names.values.flatten()

    fun displayDescription(language: String): String = description[language]
        ?: description["en"]
        ?: description.values.firstOrNull().orEmpty()

    fun servingsOrDefault(): List<FoodServing> = servings.ifEmpty {
        listOf(FoodServing("per_100g", "standard.100g", 1.0, mapOf("zh" to "100 克", "en" to "100 g")))
    }

    fun nutrientTable(key: String): FoodNutrientTable = nutritionTables[key]
        ?: FoodNutrientTable(
            basis = FoodQuantity(100.0, "weight", "g"),
            nutrients = nutrients,
        )
}

@Serializable
internal data class FoodNutrientTable(
    val basis: FoodQuantity,
    val nutrients: Map<String, FoodAmount>,
)

@Serializable
internal data class FoodQuantity(
    val value: Double,
    val unitCategory: String,
    val unitId: String,
)

@Serializable
internal data class FoodServing(
    val id: String,
    val nutritionTableKey: String,
    val ratioToTable: Double,
    val labels: Map<String, String>,
) {
    fun displayLabel(language: String): String = labels[language] ?: labels["en"] ?: labels.values.firstOrNull().orEmpty()
}

@Serializable
internal data class FoodAmount(
    val value: Double,
    val unitCategory: String,
    val unitId: String,
)

@Serializable
internal data class FoodHealthMetrics(
    val glycemicIndex: FoodMetric? = null,
    val glycemicLoadPer100g: FoodMetric? = null,
    val inflammatoryPotential: FoodMetric? = null,
)

@Serializable
internal data class FoodMetric(
    val value: Double,
    val unit: String,
    val basis: String = "per_100g_edible_portion",
    val note: String? = null,
)

@Serializable
internal data class FoodImage(
    val localKey: String = "food.illustration.default",
    val attribution: String,
)

@Serializable
internal data class FoodSource(
    val dataset: String,
    val reference: String,
)

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

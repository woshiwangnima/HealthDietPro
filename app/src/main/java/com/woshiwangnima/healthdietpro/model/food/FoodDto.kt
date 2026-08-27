package com.woshiwangnima.healthdietpro.model.food

import kotlinx.serialization.Serializable

@Serializable
internal data class FoodAsset(val foods: List<FoodDto> = emptyList())

@Serializable
internal data class FoodDto(
    val id: String,
    val kind: String = "ingredient",
    val names: Map<String, List<String>> = emptyMap(),
    val categoryTags: List<String> = emptyList(),
    val flavorTags: List<String> = emptyList(),
    val nutritionTables: Map<String, FoodNutrientTableDto> = emptyMap(),
    val nutrients: Map<String, FoodAmountDto> = emptyMap(),
    val edibleRatio: Double? = null,
    val derivedFrom: FoodDerivationDto? = null,
    val components: List<DishComponentDto> = emptyList(),
    val healthMetrics: FoodHealthMetricsDto = FoodHealthMetricsDto(),
    val hydrationMlPer100g: Double? = null,
    val commonness: Int = 0,
    val systemTags: List<String> = emptyList(),
    val servings: List<FoodServingDto> = emptyList(),
    val densityGramsPerMl: Double? = null,
    val description: Map<String, String> = emptyMap(),
    val image: FoodImageDto? = null,
    val sources: List<FoodSourceDto> = emptyList(),
    val botanicalTaxonomy: BotanicalTaxonomyDto? = null,
    // 菜肴扩展字段
    val cuisine: String? = null,
    val dishCategories: List<String> = emptyList(),
    val recipeSteps: List<RecipeStepDto> = emptyList(),
    val difficulty: Int? = null,
    val servesPeople: Int? = null,
    val tastes: List<String> = emptyList(),
    val techniqueId: String? = null,
    val seasons: List<String> = emptyList(),
) {
    fun toDomain(): FoodItem = when (kind.lowercase()) {
        "food" -> PreparedFood(
            id = id,
            names = names,
            categoryTags = FoodCategories.normalizeTags(categoryTags),
            flavorTags = flavorTags,
            derivedFrom = derivedFrom?.toDomain(),
            components = components.map { it.toDomain() },
            densityGramsPerMl = densityGramsPerMl,
            servings = servings.map { it.toDomain() },
            healthMetrics = healthMetrics.toDomain(),
            hydrationMlPer100g = hydrationMlPer100g,
            commonness = commonness,
            systemTags = systemTags,
            description = description,
            image = image?.toDomain(),
            sources = sources.map { it.toDomain() },
            botanicalTaxonomy = botanicalTaxonomy?.toDomain(),
            recipeSteps = recipeSteps.map { it.toDomain() },
            techniqueId = techniqueId,
            servesPeople = servesPeople,
        )
        "dish" -> Dish(
            id = id,
            names = names,
            components = components.map { it.toDomain() },
            servings = servings.map { it.toDomain() },
            healthMetrics = healthMetrics.toDomain(),
            hydrationMlPer100g = hydrationMlPer100g,
            commonness = commonness,
            systemTags = systemTags,
            description = description,
            image = image?.toDomain(),
            sources = sources.map { it.toDomain() },
            botanicalTaxonomy = botanicalTaxonomy?.toDomain(),
            cuisine = cuisine,
            dishCategories = dishCategories,
            recipeSteps = recipeSteps.map { it.toDomain() },
            difficulty = difficulty,
            servesPeople = servesPeople,
            tastes = tastes,
            techniqueId = techniqueId,
            seasons = seasons,
        )
        else -> Ingredient(
            id = id,
            names = names,
            categoryTags = FoodCategories.normalizeTags(categoryTags),
            flavorTags = flavorTags,
            nutritionTables = resolveTables(),
            edibleRatio = edibleRatio,
            densityGramsPerMl = densityGramsPerMl,
            servings = servings.map { it.toDomain() },
            healthMetrics = healthMetrics.toDomain(),
            hydrationMlPer100g = hydrationMlPer100g,
            commonness = commonness,
            systemTags = systemTags,
            description = description,
            image = image?.toDomain(),
            sources = sources.map { it.toDomain() },
            botanicalTaxonomy = botanicalTaxonomy?.toDomain(),
        )
    }

    private fun resolveTables(): Map<String, FoodNutrientTable> {
        val tables = nutritionTables.mapValues { (_, dto) -> dto.toDomain() }
        if (tables.isNotEmpty() || nutrients.isEmpty()) return tables
        return mapOf(
            "standard.100g" to FoodNutrientTable(
                basis = FoodQuantity(100.0, "weight", "g"),
                nutrients = nutrients.mapValues { (_, amount) -> amount.toDomain() },
            ),
        )
    }
}

@Serializable
internal data class FoodNutrientTableDto(
    val basis: FoodQuantityDto,
    val nutrients: Map<String, FoodAmountDto> = emptyMap(),
) {
    fun toDomain() = FoodNutrientTable(basis.toDomain(), nutrients.mapValues { it.value.toDomain() })
}

@Serializable
internal data class FoodQuantityDto(
    val value: Double,
    val unitCategory: String,
    val unitId: String,
) {
    fun toDomain() = FoodQuantity(value, unitCategory, unitId)
}

@Serializable
internal data class FoodAmountDto(
    val value: Double,
    val unitCategory: String,
    val unitId: String,
) {
    fun toDomain() = FoodAmount(value, unitCategory, unitId)
}

@Serializable
internal data class FoodDerivationDto(
    val ingredientId: String,
    val cookingMethodId: String,
    val nutrientOverrides: Map<String, FoodAmountDto> = emptyMap(),
) {
    fun toDomain() = FoodDerivation(
        ingredientId = ingredientId,
        cookingMethodId = cookingMethodId,
        nutrientOverrides = nutrientOverrides.mapValues { it.value.toDomain() },
    )
}

@Serializable
internal data class DishComponentDto(
    val foodId: String,
    val quantity: FoodQuantityDto,
) {
    fun toDomain() = DishComponent(foodId, quantity.toDomain())
}

@Serializable
internal data class RecipeStepDto(
    val text: String = "",
    val minutes: Int? = null,
    val heatLevel: String? = null,
    val waterTemperatureC: Double? = null,
    val oilTemperatureC: Double? = null,
) {
    fun toDomain() = RecipeStep(text, minutes, heatLevel, waterTemperatureC, oilTemperatureC)
}

@Serializable
internal data class FoodServingDto(
    val id: String,
    val nutritionTableKey: String = "standard.100g",
    val ratioToTable: Double = 1.0,
    val labels: Map<String, String> = emptyMap(),
    val containerId: String? = null,
    val fillRatio: Double? = null,
) {
    fun toDomain() = FoodServing(id, nutritionTableKey, ratioToTable, labels, containerId, fillRatio)
}

@Serializable
internal data class FoodHealthMetricsDto(
    val glycemicIndex: FoodMetricDto? = null,
    val glycemicLoadPer100g: FoodMetricDto? = null,
    val inflammatoryPotential: FoodMetricDto? = null,
) {
    fun toDomain() = FoodHealthMetrics(
        glycemicIndex = glycemicIndex?.toDomain(),
        glycemicLoadPer100g = glycemicLoadPer100g?.toDomain(),
        inflammatoryPotential = inflammatoryPotential?.toDomain(),
    )
}

@Serializable
internal data class FoodMetricDto(
    val value: Double,
    val unit: String,
    val basis: String = "per_100g_edible_portion",
    val note: String? = null,
) {
    fun toDomain() = FoodMetric(value, unit, basis, note)
}

@Serializable
internal data class FoodImageDto(
    val localKey: String = "food.illustration.default",
    val attribution: String,
) {
    fun toDomain() = FoodImage(localKey, attribution)
}

@Serializable
internal data class FoodSourceDto(
    val dataset: String,
    val reference: String,
) {
    fun toDomain() = FoodSource(dataset, reference)
}

@Serializable
internal data class BotanicalTaxonomyDto(
    val family: String,
    val genus: String,
    val species: String? = null,
) {
    fun toDomain() = BotanicalTaxonomy(family, genus, species)
}

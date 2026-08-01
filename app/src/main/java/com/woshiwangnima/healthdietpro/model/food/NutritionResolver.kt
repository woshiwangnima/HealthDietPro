package com.woshiwangnima.healthdietpro.model.food

internal const val TABLE_STANDARD_100G = "standard.100g"
internal const val TABLE_STANDARD_100G_EDIBLE = "standard.100g_edible"
internal const val TABLE_STANDARD_100ML = "standard.100ml"

internal data class ResolvedNutrition(
    val basis: FoodQuantity,
    val nutrients: Map<String, FoodAmount>,
    val healthMetrics: FoodHealthMetrics = FoodHealthMetrics(),
)

/**
 * Pure, Android-free resolver for the three-tier food model.
 *
 * - [Ingredient]: returns its stored per-100 g (edible) table.
 * - [PreparedFood]: derives per-100 g cooked values from the source ingredient and cooking method.
 * - [Dish]: sums resolved components converted to grams.
 */
internal class NutritionResolver(
    private val foodsById: Map<String, FoodItem>,
    private val cookingMethodsById: Map<String, CookingMethod>,
) {
    fun resolvePer100g(item: FoodItem): ResolvedNutrition = resolve(item, mutableSetOf())

    /** Hydration contribution for a consumed edible mass, for future drink-record aggregation. */
    fun hydrationMl(item: FoodItem, consumedGrams: Double): Double? =
        item.hydrationMlPer100g?.let { per100g -> consumedGrams * per100g / 100.0 }

    private fun resolve(item: FoodItem, visiting: MutableSet<String>): ResolvedNutrition {
        if (!visiting.add(item.id)) {
            throw IllegalStateException("Cyclic food composition detected at '${item.id}'")
        }
        return try {
            when (item) {
                is Ingredient -> resolveIngredient(item)
                is PreparedFood -> resolvePrepared(item, visiting)
                is Dish -> resolveDish(item, visiting)
            }
        } finally {
            visiting.remove(item.id)
        }
    }

    private fun resolveIngredient(ingredient: Ingredient): ResolvedNutrition {
        val table = ingredient.nutritionTables[TABLE_STANDARD_100G_EDIBLE]
            ?: ingredient.nutritionTables[TABLE_STANDARD_100G]
            ?: ingredient.nutritionTables.values.firstOrNull()
            ?: FoodNutrientTable(FoodQuantity(100.0, "weight", "g"), emptyMap())
        return ResolvedNutrition(table.basis, table.nutrients, ingredient.healthMetrics.completeFor(table.nutrients))
    }

    private fun resolvePrepared(food: PreparedFood, visiting: MutableSet<String>): ResolvedNutrition {
        if (food.components.isNotEmpty()) {
            return resolveComponents(food.components, food.healthMetrics, visiting)
        }
        val derivation = requireNotNull(food.derivedFrom) { "PreparedFood '${food.id}' requires derivedFrom or components" }
        val source = foodsById[derivation.ingredientId]
            ?: throw IllegalStateException("PreparedFood '${food.id}' references missing ingredient '${derivation.ingredientId}'")
        val method = cookingMethodsById[derivation.cookingMethodId]
            ?: throw IllegalStateException("PreparedFood '${food.id}' references missing cooking method '${derivation.cookingMethodId}'")
        val base = resolve(source, visiting)
        val yieldFactor = if (method.yieldFactor > 0.0) method.yieldFactor else 1.0
        val codes = base.nutrients.keys + method.addedPer100gRaw.keys
        val derived = codes.associateWith { code ->
            val raw = base.nutrients[code]
            val added = method.addedPer100gRaw[code]
            val rawValue = (raw?.value ?: 0.0) * method.retentionFor(code)
            val addedValue = added?.value ?: 0.0
            val unitCategory = raw?.unitCategory ?: added?.unitCategory ?: "weight"
            val unitId = raw?.unitId ?: added?.unitId ?: "g"
            FoodAmount((rawValue + addedValue) / yieldFactor, unitCategory, unitId)
        }
        val overridden = derived + derivation.nutrientOverrides
        return ResolvedNutrition(
            basis = FoodQuantity(100.0, "weight", "g"),
            nutrients = overridden,
            healthMetrics = food.healthMetrics
                .withFallback(base.healthMetrics)
                .completeFor(overridden),
        )
    }

    private fun resolveDish(dish: Dish, visiting: MutableSet<String>): ResolvedNutrition {
        return resolveComponents(dish.components, dish.healthMetrics, visiting)
    }

    private fun resolveComponents(
        components: List<DishComponent>,
        healthMetrics: FoodHealthMetrics,
        visiting: MutableSet<String>,
    ): ResolvedNutrition {
        val totals = LinkedHashMap<String, FoodAmount>()
        val resolvedComponents = mutableListOf<Pair<ResolvedNutrition, Double>>()
        for (component in components) {
            val componentItem = foodsById[component.foodId]
                ?: throw IllegalStateException("Food composition references missing component '${component.foodId}'")
            val resolved = resolve(componentItem, visiting)
            val grams = componentGrams(component.quantity, componentItem)
            resolvedComponents += resolved to grams
            val factor = grams / 100.0
            for ((code, amount) in resolved.nutrients) {
                val existing = totals[code]
                val addedValue = amount.value * factor
                totals[code] = if (existing == null) {
                    FoodAmount(addedValue, amount.unitCategory, amount.unitId)
                } else {
                    existing.copy(value = existing.value + addedValue)
                }
            }
        }
        return ResolvedNutrition(
            basis = FoodQuantity(resolvedComponents.sumOf { it.second }, "weight", "g"),
            nutrients = totals,
            healthMetrics = healthMetrics
                .withFallback(resolvedComponents.weightedGlycemicMetrics())
                .completeFor(totals),
        )
    }

    private fun componentGrams(quantity: FoodQuantity, item: FoodItem): Double = when (quantity.unitCategory) {
        "weight" -> quantity.value
        "volume" -> {
            val density = densityOf(item)
                ?: throw IllegalStateException("Component '${item.id}' has volume quantity but no density")
            quantity.value * density
        }
        else -> quantity.value
    }

    private fun densityOf(item: FoodItem): Double? = when (item) {
        is Ingredient -> item.densityGramsPerMl
        is PreparedFood -> item.densityGramsPerMl
        is Dish -> null
    }

    companion object {
        /** Grams for a container-based serving: capacityMl * fillRatio * density. */
        fun containerGrams(container: ServingContainer, densityGramsPerMl: Double, fillRatio: Double? = null): Double =
            container.capacityMl * (fillRatio ?: container.defaultFillRatio) * densityGramsPerMl

        /** Edible mass from purchased mass via edible ratio. */
        fun edibleGrams(purchasedGrams: Double, edibleRatio: Double): Double = purchasedGrams * edibleRatio

        /** Purchased mass required to yield a target edible mass. */
        fun purchasedGrams(edibleGrams: Double, edibleRatio: Double): Double =
            if (edibleRatio > 0.0) edibleGrams / edibleRatio else edibleGrams
    }
}

private fun FoodHealthMetrics.withFallback(fallback: FoodHealthMetrics): FoodHealthMetrics = FoodHealthMetrics(
    glycemicIndex = glycemicIndex ?: fallback.glycemicIndex,
    glycemicLoadPer100g = glycemicLoadPer100g ?: fallback.glycemicLoadPer100g,
    inflammatoryPotential = inflammatoryPotential ?: fallback.inflammatoryPotential,
)

private fun FoodHealthMetrics.completeFor(nutrients: Map<String, FoodAmount>): FoodHealthMetrics {
    val gi = glycemicIndex
    val carbohydrates = nutrients["CHO"]?.value ?: 0.0
    return copy(
        glycemicLoadPer100g = glycemicLoadPer100g ?: gi?.let {
            FoodMetric(it.value * carbohydrates / 100.0, "GL")
        },
    )
}

private fun List<Pair<ResolvedNutrition, Double>>.weightedGlycemicMetrics(): FoodHealthMetrics {
    val carbohydrateContributions = map { (nutrition, grams) ->
        nutrition to ((nutrition.nutrients["CHO"]?.value ?: 0.0) * grams / 100.0)
    }
    val totalCarbohydrates = carbohydrateContributions.sumOf { it.second }
    if (totalCarbohydrates <= 0.0) return FoodHealthMetrics()
    if (carbohydrateContributions.any { (nutrition, carbohydrates) ->
            carbohydrates > 0.0 && nutrition.healthMetrics.glycemicIndex == null
        }
    ) return FoodHealthMetrics()
    val gi = carbohydrateContributions.filter { (_, carbohydrates) -> carbohydrates > 0.0 }.sumOf { (nutrition, carbohydrates) ->
        requireNotNull(nutrition.healthMetrics.glycemicIndex).value * carbohydrates
    } / totalCarbohydrates
    return FoodHealthMetrics(
        glycemicIndex = FoodMetric(gi, "GI", basis = "whole_dish"),
        glycemicLoadPer100g = FoodMetric(gi * totalCarbohydrates / 100.0, "GL", basis = "whole_dish"),
    )
}

package com.woshiwangnima.healthdietpro.model.food

internal const val TABLE_STANDARD_100G = "standard.100g"
internal const val TABLE_STANDARD_100G_EDIBLE = "standard.100g_edible"
internal const val TABLE_STANDARD_100ML = "standard.100ml"

internal data class ResolvedNutrition(
    val basis: FoodQuantity,
    val nutrients: Map<String, FoodAmount>,
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
        return ResolvedNutrition(table.basis, table.nutrients)
    }

    private fun resolvePrepared(food: PreparedFood, visiting: MutableSet<String>): ResolvedNutrition {
        val source = foodsById[food.derivedFrom.ingredientId]
            ?: throw IllegalStateException("PreparedFood '${food.id}' references missing ingredient '${food.derivedFrom.ingredientId}'")
        val method = cookingMethodsById[food.derivedFrom.cookingMethodId]
            ?: throw IllegalStateException("PreparedFood '${food.id}' references missing cooking method '${food.derivedFrom.cookingMethodId}'")
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
        val overridden = derived + food.derivedFrom.nutrientOverrides
        return ResolvedNutrition(FoodQuantity(100.0, "weight", "g"), overridden)
    }

    private fun resolveDish(dish: Dish, visiting: MutableSet<String>): ResolvedNutrition {
        val totals = LinkedHashMap<String, FoodAmount>()
        for (component in dish.components) {
            val componentItem = foodsById[component.foodId]
                ?: throw IllegalStateException("Dish '${dish.id}' references missing component '${component.foodId}'")
            val resolved = resolve(componentItem, visiting)
            val grams = componentGrams(component.quantity, componentItem)
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
        return ResolvedNutrition(FoodQuantity(100.0, "weight", "g"), totals)
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

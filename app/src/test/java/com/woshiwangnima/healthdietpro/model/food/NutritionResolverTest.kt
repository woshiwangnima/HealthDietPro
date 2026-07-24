package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionResolverTest {
    private val riceRaw = Ingredient(
        id = "ing:rice",
        names = mapOf("zh" to listOf("大米")),
        categoryTags = listOf("food.staple.grain"),
        nutritionTables = mapOf(
            TABLE_STANDARD_100G_EDIBLE to FoodNutrientTable(
                basis = FoodQuantity(100.0, "weight", "g"),
                nutrients = mapOf(
                    "ENERGY" to FoodAmount(348.0, "energy", "kcal"),
                    "PROTEIN" to FoodAmount(7.8, "weight", "g"),
                    "CHO" to FoodAmount(77.7, "weight", "g"),
                ),
            ),
        ),
        densityGramsPerMl = 0.85,
    )

    private val steamGrain = CookingMethod(
        id = "steamed_grain",
        labels = mapOf("en" to "Grain steaming"),
        yieldFactor = 3.0,
    )

    private val stirFry = CookingMethod(
        id = "stir_fried",
        labels = mapOf("en" to "Stir-fried"),
        yieldFactor = 0.9,
        nutrientRetention = mapOf("PROTEIN" to 0.9),
        addedPer100gRaw = mapOf(
            "ENERGY" to FoodAmount(90.0, "energy", "kcal"),
            "FAT" to FoodAmount(10.0, "weight", "g"),
        ),
    )

    @Test
    fun ingredientResolvesToItsEdibleTable() {
        val resolver = NutritionResolver(mapOf(riceRaw.id to riceRaw), emptyMap())
        val resolved = resolver.resolvePer100g(riceRaw)
        assertEquals(348.0, resolved.nutrients.getValue("ENERGY").value, 0.0001)
    }

    @Test
    fun preparedFoodDividesByYieldFactor() {
        val cooked = PreparedFood(
            id = "food:rice:steamed",
            names = mapOf("zh" to listOf("米饭")),
            categoryTags = listOf("food.staple.grain"),
            derivedFrom = FoodDerivation(riceRaw.id, steamGrain.id),
        )
        val resolver = NutritionResolver(
            mapOf(riceRaw.id to riceRaw, cooked.id to cooked),
            mapOf(steamGrain.id to steamGrain),
        )
        val resolved = resolver.resolvePer100g(cooked)
        assertEquals(116.0, resolved.nutrients.getValue("ENERGY").value, 0.0001)
        assertEquals(2.6, resolved.nutrients.getValue("PROTEIN").value, 0.0001)
        assertEquals(25.9, resolved.nutrients.getValue("CHO").value, 0.0001)
    }

    @Test
    fun stirFryAppliesRetentionAndAddedFatBeforeYield() {
        val cooked = PreparedFood(
            id = "food:rice:fried",
            names = mapOf("en" to listOf("Fried rice base")),
            categoryTags = listOf("food.staple.grain"),
            derivedFrom = FoodDerivation(riceRaw.id, stirFry.id),
        )
        val resolver = NutritionResolver(
            mapOf(riceRaw.id to riceRaw, cooked.id to cooked),
            mapOf(stirFry.id to stirFry),
        )
        val resolved = resolver.resolvePer100g(cooked)
        // ENERGY: (348*1 + 90) / 0.9
        assertEquals((348.0 + 90.0) / 0.9, resolved.nutrients.getValue("ENERGY").value, 0.0001)
        // PROTEIN: (7.8*0.9 + 0) / 0.9 = 7.8
        assertEquals(7.8, resolved.nutrients.getValue("PROTEIN").value, 0.0001)
        // FAT only from added: (0 + 10) / 0.9
        assertEquals(10.0 / 0.9, resolved.nutrients.getValue("FAT").value, 0.0001)
    }

    @Test
    fun nutrientOverrideReplacesDerivedValue() {
        val cooked = PreparedFood(
            id = "food:rice:override",
            names = mapOf("en" to listOf("Override")),
            categoryTags = listOf("food.staple.grain"),
            derivedFrom = FoodDerivation(
                riceRaw.id,
                steamGrain.id,
                nutrientOverrides = mapOf("ENERGY" to FoodAmount(130.0, "energy", "kcal")),
            ),
        )
        val resolver = NutritionResolver(
            mapOf(riceRaw.id to riceRaw, cooked.id to cooked),
            mapOf(steamGrain.id to steamGrain),
        )
        assertEquals(130.0, resolver.resolvePer100g(cooked).nutrients.getValue("ENERGY").value, 0.0001)
    }

    @Test
    fun dishSumsComponentsByGrams() {
        val cooked = PreparedFood(
            id = "food:rice:steamed",
            names = mapOf("zh" to listOf("米饭")),
            categoryTags = listOf("food.staple.grain"),
            derivedFrom = FoodDerivation(riceRaw.id, steamGrain.id),
        )
        val veg = Ingredient(
            id = "ing:veg",
            names = mapOf("zh" to listOf("青菜")),
            categoryTags = listOf("food.vegetable"),
            nutritionTables = mapOf(
                TABLE_STANDARD_100G_EDIBLE to FoodNutrientTable(
                    basis = FoodQuantity(100.0, "weight", "g"),
                    nutrients = mapOf("ENERGY" to FoodAmount(20.0, "energy", "kcal")),
                ),
            ),
        )
        val dish = Dish(
            id = "dish:rice_veg",
            names = mapOf("zh" to listOf("青菜盖饭")),
            components = listOf(
                DishComponent(cooked.id, FoodQuantity(200.0, "weight", "g")),
                DishComponent(veg.id, FoodQuantity(100.0, "weight", "g")),
            ),
        )
        val resolver = NutritionResolver(
            listOf(riceRaw, cooked, veg, dish).associateBy { it.id },
            mapOf(steamGrain.id to steamGrain),
        )
        val resolved = resolver.resolvePer100g(dish)
        // rice 116*2 + veg 20*1 = 252
        assertEquals(252.0, resolved.nutrients.getValue("ENERGY").value, 0.0001)
    }

    @Test
    fun dishVolumeComponentUsesDensity() {
        val dish = Dish(
            id = "dish:rice_scoop",
            names = mapOf("en" to listOf("Rice scoop")),
            components = listOf(
                DishComponent(riceRaw.id, FoodQuantity(100.0, "volume", "ml")),
            ),
        )
        val resolver = NutritionResolver(
            listOf(riceRaw, dish).associateBy { it.id },
            emptyMap(),
        )
        // 100 ml * 0.85 = 85 g -> ENERGY 348 * 85/100 = 295.8
        assertEquals(295.8, resolver.resolvePer100g(dish).nutrients.getValue("ENERGY").value, 0.0001)
    }

    @Test
    fun cyclicDishThrows() {
        val a = Dish(
            id = "dish:a",
            names = mapOf("en" to listOf("A")),
            components = listOf(DishComponent("dish:b", FoodQuantity(100.0, "weight", "g"))),
        )
        val b = Dish(
            id = "dish:b",
            names = mapOf("en" to listOf("B")),
            components = listOf(DishComponent("dish:a", FoodQuantity(100.0, "weight", "g"))),
        )
        val resolver = NutritionResolver(listOf(a, b).associateBy { it.id }, emptyMap())
        assertThrows(IllegalStateException::class.java) { resolver.resolvePer100g(a) }
    }

    @Test
    fun containerGramsUsesCapacityFillAndDensity() {
        val cup = ServingContainer("cup_medium", "cup", "medium", 250.0, 0.9, mapOf("en" to "Medium cup"))
        assertEquals(250.0 * 0.9 * 1.03, NutritionResolver.containerGrams(cup, 1.03), 0.0001)
        assertEquals(250.0 * 0.5 * 1.03, NutritionResolver.containerGrams(cup, 1.03, 0.5), 0.0001)
    }

    @Test
    fun edibleAndPurchasedConversionsAreInverse() {
        val purchased = 100.0
        val edible = NutritionResolver.edibleGrams(purchased, 0.68)
        assertEquals(68.0, edible, 0.0001)
        assertEquals(purchased, NutritionResolver.purchasedGrams(edible, 0.68), 0.0001)
    }
}

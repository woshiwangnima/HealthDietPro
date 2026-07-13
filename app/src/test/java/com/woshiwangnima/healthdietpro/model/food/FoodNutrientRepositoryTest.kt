package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodNutrientRepositoryTest {
    @Test
    fun foodAssetLoadsAndUsesKnownNutrientCodes() {
        val foods = FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json").foods()

        assertTrue(foods.isNotEmpty())
        assertTrue(foods.all { it.nutrientsPer100g.keys.all { code -> code in setOf("ENERGY", "PROTEIN", "FAT", "CHO") } })
        assertEquals("米饭", foods.first { it.id == "builtin.rice.cooked" }.displayName("zh"))
    }

    @Test
    fun categoryMatchingIncludesDescendantsOnly() {
        assertTrue(FoodCategories.isWithin("food.staple.grain", "food.staple"))
        assertTrue(FoodCategories.isWithin("food.aquatic.fish", "food.aquatic"))
        assertTrue(!FoodCategories.isWithin("food.fruit", "food.staple"))
    }
}

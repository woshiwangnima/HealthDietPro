package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookingMethodRepositoryTest {
    private fun repo() = CookingMethodRepository.fromAsset("src/main/assets/cooking_methods.json")

    @Test
    fun loadsMethodsWithUniqueIdsAndPositiveYield() {
        val methods = repo().methods()
        assertTrue(methods.size >= 5)
        assertEquals(methods.size, methods.map { it.id }.toSet().size)
        assertTrue(methods.all { it.yieldFactor > 0.0 })
        assertTrue(methods.all { it.nutrientRetention.values.all { r -> r in 0.0..1.0 } })
    }

    @Test
    fun grainSteamingYieldsThree() {
        val method = repo().find("steamed_grain")
        assertEquals(3.0, requireNotNull(method).yieldFactor, 0.0001)
    }

    @Test
    fun steamedRiceDerivesToStoredCookedValues() {
        val foods = FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json")
        val methods = repo()
        val resolver = NutritionResolver(foods.byId(), methods.byId())
        val cooked = foods.find("food:taxon:oryza_sativa:polished:steamed")
        assertTrue(cooked is PreparedFood)
        val resolved = resolver.resolvePer100g(requireNotNull(cooked))
        assertEquals(116.0, resolved.nutrients.getValue("ENERGY").value, 0.5)
        assertEquals(25.9, resolved.nutrients.getValue("CHO").value, 0.1)
    }
}

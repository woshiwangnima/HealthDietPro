package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodSearchTest {
    private val foods = FoodNutrientRepository
        .fromCatalogAsset("src/main/assets/food_catalog")
        .foods()
        .associateBy { it.id }

    @Test
    fun exactNameMatchesBeforeSubstringAndPartialMatches() {
        val query = "猪肉"
        val exact = foods.getValue("food:taxon:sus_scrofa:lean_raw")
        val substring = foods.getValue("food:taxon:sus_scrofa:fat_raw")
        val partial = foods.getValue("food:taxon:sus_scrofa:ribs_raw")

        assertEquals(0, exact.searchMatchRank(query))
        assertEquals(0, substring.searchMatchRank(query))
        assertEquals(2, partial.searchMatchRank(query))
        assertEquals(0, exact.searchMatchRank("猪肉-瘦"))
    }

    @Test
    fun aliasesAreIncludedInExactMatches() {
        val food = foods.getValue("food:taxon:sus_scrofa:lean_raw")

        assertEquals(0, food.searchMatchRank("瘦猪肉"))
    }
}

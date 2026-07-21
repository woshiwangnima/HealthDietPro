package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodNutrientRepositoryTest {
    @Test
    fun foodAssetLoadsNamesTagsAmountsAndCoreHealthMetrics() {
        val foods = FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json").foods()

        assertTrue(foods.size >= 15)
        assertTrue(foods.all { it.categoryTags.isNotEmpty() && it.nutrientTable("standard.100g").nutrients.keys.containsAll(setOf("ENERGY", "PROTEIN", "FAT", "CHO")) })
        assertTrue(foods.all { food -> food.nutritionTables.values.all { table -> table.basis.unitCategory.isNotBlank() && table.basis.unitId.isNotBlank() && table.nutrients.values.all { it.unitCategory.isNotBlank() && it.unitId.isNotBlank() } } })
        assertTrue(foods.all { food ->
            val hasGi = food.healthMetrics.glycemicIndex != null
            val hasGl = food.healthMetrics.glycemicLoadPer100g != null
            val carbohydrate = food.nutrientTable("standard.100g").nutrients.getValue("CHO").value
            hasGi == hasGl && (carbohydrate > 0 || !hasGi)
        })
        assertTrue(foods.all { it.commonness in 1..5 })
        assertEquals("米饭", foods.first { it.id == "food:taxon:oryza_sativa:polished:steamed" }.displayName("zh"))
    }

    @Test
    fun cucumberVariantsHaveDistinctStableIdsAndChineseAliases() {
        val foods = FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json").foods()

        val commercial = foods.first { it.id == "food:taxon:cucumis_sativus:commercial:raw" }
        val landrace = foods.first { it.id == "food:taxon:cucumis_sativus:landrace:raw" }
        assertEquals("黄瓜", commercial.displayName("zh"))
        assertEquals("本地黄瓜", landrace.displayName("zh"))
        assertTrue(landrace.allNames("zh").contains("土黄瓜"))
    }

    @Test
    fun milkServingsReferenceTheVolumeNutritionTable() {
        val milk = FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json").foods()
            .first { it.id == "food:taxon:bos_taurus:milk:whole" }

        val volumeTable = milk.nutrientTable("standard.100ml")
        assertEquals("volume", volumeTable.basis.unitCategory)
        assertEquals("ml", volumeTable.basis.unitId)
        assertTrue(milk.servingsOrDefault().all { it.nutritionTableKey == "standard.100ml" })
    }

    @Test
    fun categoryMatchingIncludesDescendantsOnly() {
        assertTrue(FoodCategories.isWithin("food.staple.grain", "food.staple"))
        assertTrue(FoodCategories.isWithin("food.aquatic.fish", "food.aquatic"))
        assertTrue(!FoodCategories.isWithin("food.fruit", "food.staple"))
        assertTrue(
            FoodCategories.hasTagWithinAny(
                listOf("food.vegetable"),
                setOf("food.staple", "food.vegetable"),
            ),
        )
        assertTrue(
            !FoodCategories.hasTagWithinAny(
                listOf("food.fruit"),
                setOf("food.staple", "food.vegetable"),
            ),
        )
    }

    @Test
    fun selectedRootsExposeTheUnionOfTheirChildren() {
        val children = FoodCategories.childrenForRoots(setOf("food.staple", "food.meat_egg"))
        assertTrue(children.any { it.tag == "food.staple.grain" })
        assertTrue(children.any { it.tag == "food.meat_egg.livestock" })
        assertEquals(
            setOf("food.staple.grain"),
            FoodCategories.retainChildrenForRoots(
                setOf("food.staple.grain", "food.meat_egg.livestock"),
                setOf("food.staple"),
            ),
        )
    }
}

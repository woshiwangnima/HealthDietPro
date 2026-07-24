package com.woshiwangnima.healthdietpro.model.food

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodNutrientRepositoryTest {
    private fun foods() = FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json").foods()

    @Test
    fun foodAssetLoadsNamesTagsAmountsAndCoreHealthMetrics() {
        val foods = foods()
        val resolver = resolverFor(foods)

        assertTrue(foods.size >= 15)
        assertTrue(
            foods.filterIsInstance<CategorizedFood>().all { it.categoryTags.isNotEmpty() },
        )
        val ingredients = foods.filterIsInstance<Ingredient>()
        assertTrue(
            ingredients.all { ingredient ->
                resolver.resolvePer100g(ingredient).nutrients.keys
                    .containsAll(setOf("ENERGY", "PROTEIN", "FAT", "CHO"))
            },
        )
        assertTrue(
            ingredients.all { ingredient ->
                ingredient.nutritionTables.values.all { table ->
                    table.basis.unitCategory.isNotBlank() && table.basis.unitId.isNotBlank() &&
                        table.nutrients.values.all { it.unitCategory.isNotBlank() && it.unitId.isNotBlank() }
                }
            },
        )
        assertTrue(
            foods.all { food ->
                val hasGi = food.healthMetrics.glycemicIndex != null
                val hasGl = food.healthMetrics.glycemicLoadPer100g != null
                hasGi == hasGl
            },
        )
        assertTrue(foods.all { it.commonness in 1..5 })
        assertEquals("米饭", foods.first { it.id == "food:taxon:oryza_sativa:polished:steamed" }.displayName("zh"))
    }

    @Test
    fun cucumberVariantsHaveDistinctStableIdsAndChineseAliases() {
        val foods = foods()

        val commercial = foods.first { it.id == "food:taxon:cucumis_sativus:commercial:raw" }
        val landrace = foods.first { it.id == "food:taxon:cucumis_sativus:landrace:raw" }
        assertEquals("黄瓜", commercial.displayName("zh"))
        assertEquals("本地黄瓜", landrace.displayName("zh"))
        assertTrue(landrace.allNames("zh").contains("土黄瓜"))
    }

    @Test
    fun milkExposesTheVolumeNutritionTable() {
        val milk = foods().first { it.id == "food:taxon:bos_taurus:milk:whole" } as Ingredient

        val volumeTable = milk.nutritionTables.getValue("standard.100ml")
        assertEquals("volume", volumeTable.basis.unitCategory)
        assertEquals("ml", volumeTable.basis.unitId)
        assertTrue(milk.servings.all { it.nutritionTableKey == "standard.100ml" })
    }

    @Test
    fun cookedRiceIsDerivedFromRawRiceAndMatchesLegacyValues() {
        val foods = foods()
        val resolver = resolverFor(foods)
        val cooked = foods.first { it.id == "food:taxon:oryza_sativa:polished:steamed" }
        assertTrue(cooked is PreparedFood)

        val resolved = resolver.resolvePer100g(cooked)
        assertEquals(116.0, resolved.nutrients.getValue("ENERGY").value, 0.01)
        assertEquals(2.6, resolved.nutrients.getValue("PROTEIN").value, 0.01)
        assertEquals(0.3, resolved.nutrients.getValue("FAT").value, 0.01)
        assertEquals(25.9, resolved.nutrients.getValue("CHO").value, 0.01)
    }

    @Test
    fun everyDerivedFoodAndDishComponentResolvesToAKnownId() {
        val foods = foods()
        val byId = foods.associateBy { it.id }
        foods.filterIsInstance<PreparedFood>().forEach {
            assertTrue(
                "missing ingredient ${it.derivedFrom.ingredientId}",
                byId.containsKey(it.derivedFrom.ingredientId),
            )
        }
        foods.filterIsInstance<Dish>().forEach { dish ->
            dish.components.forEach { component ->
                assertTrue(
                    "missing component ${component.foodId}",
                    byId.containsKey(component.foodId),
                )
            }
        }
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

    private fun resolverFor(foods: List<FoodItem>): NutritionResolver {
        val methods = CookingMethodRepository
            .fromAsset("src/main/assets/cooking_methods.json")
            .byId()
        return NutritionResolver(foods.associateBy { it.id }, methods)
    }
}

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
            foods.all { food ->
                resolver.resolvePer100g(food).nutrients.keys
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
                val metrics = resolver.resolvePer100g(food).healthMetrics
                val hasGi = metrics.glycemicIndex != null
                val hasGl = metrics.glycemicLoadPer100g != null
                hasGi == hasGl
            },
        )
        assertTrue(foods.all { it.commonness in 1..5 })
        assertTrue(foods.filterIsInstance<PreparedFood>().all { it.categoryTags.isNotEmpty() })
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
        foods.filterIsInstance<PreparedFood>().forEach { food ->
            food.derivedFrom?.let { derivation ->
                assertTrue(
                    "missing ingredient ${derivation.ingredientId}",
                    byId.containsKey(derivation.ingredientId),
                )
            }
            assertTrue("food ${food.id} has neither derivation nor components", food.derivedFrom != null || food.components.isNotEmpty())
            food.components.forEach { component ->
                assertTrue("missing component ${component.foodId}", byId.containsKey(component.foodId))
            }
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
    fun multicomponentProductsAreDishesAndResolveNutritionFromTheirIngredients() {
        val foods = foods()
        val resolver = resolverFor(foods)
        val meatBun = foods.first { it.id == "food:taxon:triticum_aestivum:meat_bun" }
        val youtiao = foods.first { it.id == "food:taxon:triticum_aestivum:youtiao" }

        assertTrue(meatBun is PreparedFood)
        assertTrue(youtiao is PreparedFood)
        assertTrue((meatBun as PreparedFood).components.size >= 3)
        assertTrue((youtiao as PreparedFood).components.size >= 3)
        assertTrue(resolver.resolvePer100g(meatBun).nutrients.getValue("ENERGY").value > 0.0)
        assertTrue(resolver.resolvePer100g(youtiao).nutrients.getValue("FAT").value > 0.0)
    }

    @Test
    fun animalFatsAreOilsWithResolvedNutrition() {
        val foods = foods().associateBy { it.id }
        listOf("food:animal_fat:lard", "food:animal_fat:beef_tallow", "food:animal_fat:chicken_fat").forEach { id ->
            val fat = foods[id] as? Ingredient
            assertTrue("missing animal fat $id", fat != null)
            assertTrue("$id is not tagged as oil", fat?.categoryTags?.contains("food.oil") == true)
            assertTrue("$id does not contain fat", fat?.nutritionTables?.values?.first()?.nutrients?.get("FAT")?.value ?: 0.0 > 99.0)
        }
    }

    @Test
    fun marketCornAndPackagedKernelsUseDifferentEdiblePortionRules() {
        val foods = foods()
        val marketCorn = foods.first { it.id == "food:taxon:zea_mays:raw" } as Ingredient
        val packagedKernels = foods.first { it.id == "food:taxon:zea_mays:kernels:raw" } as Ingredient

        assertEquals(0.46, marketCorn.edibleRatio ?: 0.0, 0.0001)
        assertEquals(1.0, packagedKernels.edibleRatio ?: 0.0, 0.0001)
        assertEquals(92.0, NutritionResolver.edibleGrams(200.0, requireNotNull(marketCorn.edibleRatio)), 0.0001)
        assertEquals(200.0, NutritionResolver.edibleGrams(200.0, requireNotNull(packagedKernels.edibleRatio)), 0.0001)
        assertEquals(0.46, marketCorn.servings.first { it.id == "purchased_100g" }.ratioToTable, 0.0001)
        assertEquals(1.0, packagedKernels.servings.single().ratioToTable, 0.0001)
    }

    @Test
    fun aromaticsAndCoreCondimentsAreSeasonings() {
        val foods = foods().associateBy { it.id }
        val seasoningIds = setOf(
            "food:taxon:zingiber_officinale:raw",
            "food:taxon:allium_sativum:raw",
            "food:taxon:allium_fistulosum:raw",
            "food:taxon:allium_fistulosum:scallion",
            "food:taxon:houttuynia_cordata:raw",
            "food:taxon:coriandrum_sativum:raw",
            "food:taxon:capsicum_annuum:chili",
            "food:seasoning:salt",
            "food:seasoning:soy_sauce",
            "food:seasoning:vinegar",
            "food:seasoning:sugar",
            "food:seasoning:cooking_wine",
            "food:seasoning:black_pepper",
        )

        seasoningIds.forEach { id ->
            val ingredient = foods[id] as? Ingredient
            assertTrue("missing seasoning $id", ingredient != null)
            assertTrue("$id is not tagged as seasoning", ingredient?.categoryTags?.contains("food.seasoning") == true)
        }
    }

    @Test
    fun beverageFoodsExposeExplicitHydrationForFutureDrinkRecords() {
        val foods = foods()
        val resolver = resolverFor(foods)
        val byId = foods.associateBy { it.id }
        val expectedHydration = mapOf(
            "food:water:drinking" to 100.0,
            "food:beverage:tea" to 99.0,
            "food:beverage:coffee:black" to 98.0,
            "food:beverage:lemon_water" to 95.0,
            "food:beverage:brown_sugar_water" to 92.0,
            "food:taxon:glycine_max:soy_milk" to 92.0,
            "food:beverage:soy_milk:commercial" to 91.0,
            "food:beverage:cola" to 89.3,
            "food:taxon:bos_taurus:milk:whole" to 87.0,
            "food:beverage:fruit_juice" to 88.5,
            "food:beverage:yogurt:plain" to 82.0,
            "food:beverage:milk_tea" to 85.0,
        )

        expectedHydration.forEach { (id, per100g) ->
            val beverage = requireNotNull(byId[id])
            assertEquals(per100g, beverage.hydrationMlPer100g ?: 0.0, 0.0001)
            assertEquals(per100g * 2.5, resolver.hydrationMl(beverage, 250.0) ?: 0.0, 0.0001)
        }
    }

    @Test
    fun bakingIngredientsStarchesButterAndSugarFreeColaAreDistinct() {
        val foods = foods().associateBy { it.id }

        val cakeFlour = foods.getValue("food:taxon:triticum_aestivum:flour:cake") as Ingredient
        val allPurposeFlour = foods.getValue("food:taxon:triticum_aestivum:flour:refined") as Ingredient
        val breadFlour = foods.getValue("food:taxon:triticum_aestivum:flour:bread") as Ingredient
        assertTrue(cakeFlour.nutritionTables.values.single().nutrients.getValue("PROTEIN").value < allPurposeFlour.nutritionTables.values.single().nutrients.getValue("PROTEIN").value)
        assertTrue(breadFlour.nutritionTables.values.single().nutrients.getValue("PROTEIN").value > allPurposeFlour.nutritionTables.values.single().nutrients.getValue("PROTEIN").value)
        assertTrue(setOf("food:starch:corn", "food:starch:potato", "food:starch:tapioca").all(foods::containsKey))
        assertEquals(81.1, (foods.getValue("food:dairy:butter") as Ingredient).nutritionTables.values.single().nutrients.getValue("FAT").value, 0.0001)
        assertEquals(0.0, (foods.getValue("food:beverage:cola:sugar_free") as Ingredient).nutritionTables.values.single().nutrients.getValue("CHO").value, 0.0001)
    }

    @Test
    fun vegetablesExposeBotanicalFamilyAndGenus() {
        val foods = foods().associateBy { it.id }

        val broccoli = foods.getValue("food:taxon:brassica_oleracea:broccoli:raw").botanicalTaxonomy
        val cucumber = foods.getValue("food:taxon:cucumis_sativus:commercial:raw").botanicalTaxonomy
        val carrot = foods.getValue("food:taxon:daucus_carota:raw").botanicalTaxonomy
        assertEquals("Brassicaceae", broccoli?.family)
        assertEquals("Cucumis", cucumber?.genus)
        assertEquals("Apiaceae", carrot?.family)
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

    @Test
    fun categoryCatalogRegistersEveryAssetTagAndProvidesSubcategories() {
        val repository = FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json")
        val catalogTags = repository.categoryTree().mapTo(mutableSetOf()) { it.tag }
        val assetTags = repository.foods().filterIsInstance<CategorizedFood>()
            .flatMapTo(mutableSetOf()) { it.categoryTags }

        assertTrue(assetTags.all(catalogTags::contains))
        assertEquals(
            setOf("food.oil.plant", "food.oil.animal_fat"),
            repository.categoryChildren("food.oil").mapTo(mutableSetOf()) { it.tag },
        )
        assertTrue(repository.categoryChildren("food.seasoning").map { it.tag }.containsAll(
            setOf(
                "food.seasoning.fresh_aromatic",
                "food.seasoning.dry_spice",
                "food.seasoning.salt",
                "food.seasoning.savory_sauce",
                "food.seasoning.acid",
                "food.seasoning.sweetener",
                "food.seasoning.cooking_alcohol",
            ),
        ))
        assertTrue(repository.categoryChildren("food.beverage").map { it.tag }.containsAll(
            setOf(
                "food.beverage.water",
                "food.beverage.carbonated",
                "food.beverage.tea",
                "food.beverage.coffee",
                "food.beverage.juice",
                "food.beverage.soy",
                "food.beverage.mixed",
            ),
        ))
        assertTrue(repository.categoryChildren("food.nut").map { it.tag }.containsAll(
            setOf("food.nut.peanut", "food.nut.tree_nut", "food.nut.seed"),
        ))
    }

    @Test
    fun refinedCategoriesKeepParentCompatibilityAndCorrectCrossClassification() {
        val foods = foods().associateBy { it.id }

        assertTrue((foods.getValue("food:taxon:arachis_hypogaea:raw") as CategorizedFood).categoryTags.containsAll(
            listOf("food.nut", "food.nut.peanut"),
        ))
        assertTrue("food.soy" !in (foods.getValue("food:taxon:arachis_hypogaea:raw") as CategorizedFood).categoryTags)
        assertTrue((foods.getValue("food:dairy:butter") as CategorizedFood).categoryTags.containsAll(
            listOf("food.dairy", "food.oil", "food.oil.animal_fat"),
        ))
        assertEquals(
            listOf("food.dairy", "food.dairy.fermented"),
            (foods.getValue("food:beverage:yogurt:plain") as CategorizedFood).categoryTags,
        )
        assertTrue(
            FoodNutrientRepository.fromAsset("src/main/assets/food_nutrition.json")
                .hasCategory((foods.getValue("food:beverage:coffee:black") as CategorizedFood).categoryTags, "food.beverage"),
        )
    }

    @Test
    fun roastedSnackCatalogSeparatesKindsAndFlavorTags() {
        val foods = foods().associateBy { it.id }
        val requiredIds = setOf(
            "food:snack:peanut:plain",
            "food:snack:peanut:five_spice",
            "food:snack:peanut:garlic",
            "food:snack:peanut:brined",
            "food:snack:sunflower_seed:plain",
            "food:snack:sunflower_seed:caramel",
            "food:snack:sunflower_seed:five_spice",
            "food:snack:sunflower_seed:hickory",
            "food:snack:chestnut:plain",
            "food:snack:chestnut:sugar_roasted",
            "food:snack:fava_bean:plain",
            "food:snack:black_soybean:plain",
            "food:snack:soybean:plain",
            "food:snack:green_pea:plain",
        )
        assertTrue(requiredIds.all(foods::containsKey))
        foods.filterKeys { it in requiredIds }.values.forEach { food ->
            val categorized = food as? CategorizedFood
            assertTrue(categorized != null)
            assertTrue(categorized!!.categoryTags.contains("food.nut"))
            assertTrue(categorized.flavorTags.isNotEmpty())
        }
        assertEquals(setOf("plain", "five_spice", "garlic", "brined"), setOf(
            "food:snack:peanut:plain",
            "food:snack:peanut:five_spice",
            "food:snack:peanut:garlic",
            "food:snack:peanut:brined",
        ).map { (foods.getValue(it) as CategorizedFood).flavorTags.single() }.toSet())
        assertTrue(foods.getValue("food:snack:peanut:five_spice") is PreparedFood)
        assertTrue((foods.getValue("food:snack:peanut:five_spice") as PreparedFood).components.sumOf { it.quantity.value } == 100.0)
    }

    private fun resolverFor(foods: List<FoodItem>): NutritionResolver {
        val methods = CookingMethodRepository
            .fromAsset("src/main/assets/cooking_methods.json")
            .byId()
        return NutritionResolver(foods.associateBy { it.id }, methods)
    }
}

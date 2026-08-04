package com.woshiwangnima.healthdietpro.model.food

import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R

internal data class FoodCategory(
    val tag: String,
    @param:StringRes val labelRes: Int,
    val parentTag: String? = null,
)

internal object FoodCategories {
    val roots = listOf(
        FoodCategory("food.staple", R.string.food_category_staple),
        FoodCategory("food.vegetable", R.string.food_category_vegetable),
        FoodCategory("food.fruit", R.string.food_category_fruit),
        FoodCategory("food.meat_egg", R.string.food_category_meat_egg),
        FoodCategory("food.aquatic", R.string.food_category_aquatic),
        FoodCategory("food.soy", R.string.food_category_soy),
        FoodCategory("food.dairy", R.string.food_category_dairy),
        FoodCategory("food.nut", R.string.food_category_nut),
        FoodCategory("food.oil", R.string.food_category_oil),
        FoodCategory("food.beverage", R.string.food_category_beverage),
        FoodCategory("food.seasoning", R.string.food_category_seasoning),
    )
    val children = listOf(
        FoodCategory("food.staple.grain", R.string.food_category_grain, "food.staple"),
        FoodCategory("food.staple.whole_grain", R.string.food_category_whole_grain, "food.staple"),
        FoodCategory("food.staple.mixed_bean", R.string.food_category_mixed_bean, "food.staple"),
        FoodCategory("food.staple.tuber", R.string.food_category_tuber, "food.staple"),
        FoodCategory("food.staple.processed", R.string.food_category_staple_processed, "food.staple"),
        FoodCategory("food.meat_egg.livestock", R.string.food_category_livestock, "food.meat_egg"),
        FoodCategory("food.meat_egg.poultry", R.string.food_category_poultry, "food.meat_egg"),
        FoodCategory("food.meat_egg.egg", R.string.food_category_egg, "food.meat_egg"),
        FoodCategory("food.meat_egg.processed", R.string.food_category_meat_processed, "food.meat_egg"),
        FoodCategory("food.aquatic.fish", R.string.food_category_fish, "food.aquatic"),
        FoodCategory("food.aquatic.shrimp_crab", R.string.food_category_shrimp_crab, "food.aquatic"),
        FoodCategory("food.aquatic.shellfish", R.string.food_category_shellfish, "food.aquatic"),
        FoodCategory("food.aquatic.mollusk", R.string.food_category_mollusk, "food.aquatic"),
        FoodCategory("food.nut.peanut", R.string.food_category_peanut, "food.nut"),
        FoodCategory("food.nut.tree_nut", R.string.food_category_tree_nut, "food.nut"),
        FoodCategory("food.nut.seed", R.string.food_category_edible_seed, "food.nut"),
        FoodCategory("food.nut.legume_snack", R.string.food_category_legume_snack, "food.nut"),
        FoodCategory("food.oil.plant", R.string.food_category_plant_oil, "food.oil"),
        FoodCategory("food.oil.animal_fat", R.string.food_category_animal_fat, "food.oil"),
        FoodCategory("food.beverage.water", R.string.food_category_drinking_water, "food.beverage"),
        FoodCategory("food.beverage.carbonated", R.string.food_category_carbonated, "food.beverage"),
        FoodCategory("food.beverage.tea", R.string.food_category_tea, "food.beverage"),
        FoodCategory("food.beverage.coffee", R.string.food_category_coffee, "food.beverage"),
        FoodCategory("food.beverage.juice", R.string.food_category_juice, "food.beverage"),
        FoodCategory("food.beverage.soy", R.string.food_category_soy_beverage, "food.beverage"),
        FoodCategory("food.beverage.mixed", R.string.food_category_mixed_beverage, "food.beverage"),
        FoodCategory("food.dairy.fermented", R.string.food_category_fermented_dairy, "food.dairy"),
        FoodCategory("food.seasoning.fresh_aromatic", R.string.food_category_fresh_aromatic, "food.seasoning"),
        FoodCategory("food.seasoning.dry_spice", R.string.food_category_dry_spice, "food.seasoning"),
        FoodCategory("food.seasoning.salt", R.string.food_category_salt, "food.seasoning"),
        FoodCategory("food.seasoning.savory_sauce", R.string.food_category_savory_sauce, "food.seasoning"),
        FoodCategory("food.seasoning.acid", R.string.food_category_acid, "food.seasoning"),
        FoodCategory("food.seasoning.sweetener", R.string.food_category_sweetener, "food.seasoning"),
        FoodCategory("food.seasoning.cooking_alcohol", R.string.food_category_cooking_alcohol, "food.seasoning"),
    )
    private val byTag = (roots + children).associateBy { it.tag }
    fun isWithin(tag: String, ancestor: String): Boolean = tag == ancestor || tag.startsWith("$ancestor.")
    fun hasTagWithin(tags: List<String>, ancestor: String): Boolean = tags.any { isWithin(it, ancestor) }
    fun hasTagWithinAny(tags: List<String>, ancestors: Set<String>): Boolean =
        ancestors.isEmpty() || ancestors.any { ancestor -> hasTagWithin(tags, ancestor) }
    fun childrenForRoots(roots: Set<String>): List<FoodCategory> = children.filter { category ->
        category.parentTag?.let(roots::contains) == true
    }
    fun childrenOf(parentTag: String): List<FoodCategory> = children.filter { it.parentTag == parentTag }
    fun descendantsOf(ancestor: String): List<FoodCategory> = children.filter { isWithin(it.tag, ancestor) }
    fun retainChildrenForRoots(selectedChildren: Set<String>, roots: Set<String>): Set<String> =
        selectedChildren.filterTo(mutableSetOf()) { child -> roots.any { isWithin(child, it) } }
    fun labelRes(tag: String): Int? = byTag[tag]?.labelRes
    fun displayTagPath(tag: String): List<Int> = buildList {
        val path = generateSequence(tag) { current -> byTag[current]?.parentTag }.toList().asReversed()
        path.mapNotNullTo(this) { byTag[it]?.labelRes }
    }
    fun displayTags(tags: List<String>): List<Int> = tags.flatMap { tag ->
        displayTagPath(tag)
    }.distinct()
}

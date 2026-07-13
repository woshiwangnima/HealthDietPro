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
    )
    fun isWithin(tag: String, ancestor: String): Boolean = tag == ancestor || tag.startsWith("$ancestor.")
}

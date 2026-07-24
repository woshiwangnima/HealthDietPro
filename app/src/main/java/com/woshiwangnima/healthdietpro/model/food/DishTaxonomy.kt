package com.woshiwangnima.healthdietpro.model.food

import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R

/** 一个预定义分类项：稳定 id + 本地化标签资源。 */
internal data class DishTaxon(
    val id: String,
    @param:StringRes val labelRes: Int,
)

/** 多级分类节点：稳定 id + 本地化标签 + 子节点。 */
internal data class DishTaxonNode(
    val id: String,
    @param:StringRes val labelRes: Int,
    val children: List<DishTaxonNode> = emptyList(),
)

/**
 * 菜肴相关的预定义枚举集合：菜系 / 菜品分类 / 口味 / 季节 / 难度。
 * 工艺（technique）复用 [CookingMethodRepository] 的烹饪方式，不在此重复定义。
 */
internal object DishTaxonomy {
    /** 多级菜系：中餐（可只选一级）下分八大菜系 + 其他地方菜；另有西餐/日料/韩餐等。 */
    val cuisineTree = listOf(
        DishTaxonNode(
            "cuisine.chinese", R.string.dish_cuisine_chinese,
            children = listOf(
                DishTaxonNode("cuisine.sichuan", R.string.dish_cuisine_sichuan),
                DishTaxonNode("cuisine.cantonese", R.string.dish_cuisine_cantonese),
                DishTaxonNode("cuisine.shandong", R.string.dish_cuisine_shandong),
                DishTaxonNode("cuisine.jiangsu", R.string.dish_cuisine_jiangsu),
                DishTaxonNode("cuisine.zhejiang", R.string.dish_cuisine_zhejiang),
                DishTaxonNode("cuisine.fujian", R.string.dish_cuisine_fujian),
                DishTaxonNode("cuisine.hunan", R.string.dish_cuisine_hunan),
                DishTaxonNode("cuisine.anhui", R.string.dish_cuisine_anhui),
                DishTaxonNode("cuisine.northeast", R.string.dish_cuisine_northeast),
                DishTaxonNode("cuisine.xinjiang", R.string.dish_cuisine_xinjiang),
            ),
        ),
        DishTaxonNode("cuisine.western", R.string.dish_cuisine_western),
        DishTaxonNode("cuisine.japanese", R.string.dish_cuisine_japanese),
        DishTaxonNode("cuisine.korean", R.string.dish_cuisine_korean),
        DishTaxonNode("cuisine.other", R.string.dish_cuisine_other),
    )

    private fun flatten(nodes: List<DishTaxonNode>): List<DishTaxon> =
        nodes.flatMap { listOf(DishTaxon(it.id, it.labelRes)) + flatten(it.children) }

    val cuisines = flatten(cuisineTree)

    val categories = listOf(
        DishTaxon("dishcat.home", R.string.dish_category_home),
        DishTaxon("dishcat.banquet", R.string.dish_category_banquet),
        DishTaxon("dishcat.dessert", R.string.dish_category_dessert),
        DishTaxon("dishcat.staple", R.string.dish_category_staple),
        DishTaxon("dishcat.soup", R.string.dish_category_soup),
        DishTaxon("dishcat.snack", R.string.dish_category_snack),
        DishTaxon("dishcat.breakfast", R.string.dish_category_breakfast),
        DishTaxon("dishcat.drink", R.string.dish_category_drink),
    )

    val tastes = listOf(
        DishTaxon("taste.sour", R.string.dish_taste_sour),
        DishTaxon("taste.sweet", R.string.dish_taste_sweet),
        DishTaxon("taste.bitter", R.string.dish_taste_bitter),
        DishTaxon("taste.spicy", R.string.dish_taste_spicy),
        DishTaxon("taste.salty", R.string.dish_taste_salty),
        DishTaxon("taste.umami", R.string.dish_taste_umami),
        DishTaxon("taste.numbing", R.string.dish_taste_numbing),
        DishTaxon("taste.light", R.string.dish_taste_light),
    )

    val seasons = listOf(
        DishTaxon("season.spring", R.string.dish_season_spring),
        DishTaxon("season.summer", R.string.dish_season_summer),
        DishTaxon("season.autumn", R.string.dish_season_autumn),
        DishTaxon("season.winter", R.string.dish_season_winter),
    )

    private val labelById = (cuisines + categories + tastes + seasons).associate { it.id to it.labelRes }

    /** 菜系多级树转成通用 TagNode 供公共选择器使用。 */
    fun cuisineTagNodes(resolve: (Int) -> String): List<com.woshiwangnima.healthdietpro.common.ui.TagNode> {
        fun map(node: DishTaxonNode): com.woshiwangnima.healthdietpro.common.ui.TagNode =
            com.woshiwangnima.healthdietpro.common.ui.TagNode(node.id, resolve(node.labelRes), node.children.map(::map))
        return cuisineTree.map(::map)
    }

    @StringRes
    fun labelRes(id: String): Int? = labelById[id]
}

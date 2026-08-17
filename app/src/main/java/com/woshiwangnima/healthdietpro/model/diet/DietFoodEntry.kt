package com.woshiwangnima.healthdietpro.model.diet

import com.woshiwangnima.healthdietpro.model.food.FoodKind
import kotlinx.serialization.Serializable

/** Serializable per-nutrient snapshot stored inside a diet entry (value is already scaled to net grams). */
@Serializable
internal data class DietNutrientAmount(
    val value: Double,
    val unitCategory: String,
    val unitId: String,
)

/**
 * 一条食物信息：已有食物（内置或自定义）或自由名字条目。
 *
 * - [foodId] 已有食物 id（`custom:` 前缀代表自定义）；自由名字条目为 null。
 * - [foodName] 冗余展示名（防食物被删后空白）；自由名字条目即用户输入的名字。
 * - [foodKind] INGREDIENT / FOOD / DISH；自由名字条目为 null。
 * - [weightValue] / [weightUnitId] 用户输入的毛重（显示单位数值 + 重量类目单位 id）。
 * - [containerId] 可选去皮容器 id（仅「记容器」中已记录空重的容器）。
 * - [netWeightGrams] 净重（基准 g）= 毛重换算基准 g − 容器空重；落盘快照。
 * - [resolvedNutrients] 按净重缩放的营养素快照；自由名字条目为空（统计按 0 计）。
 */
@Serializable
internal data class DietFoodEntry(
    val foodId: String? = null,
    val foodName: String,
    val foodKind: FoodKind? = null,
    val weightValue: Double,
    val weightUnitId: String,
    val containerId: String? = null,
    val netWeightGrams: Double,
    val resolvedNutrients: Map<String, DietNutrientAmount> = emptyMap(),
) {
    val isFreeName: Boolean get() = foodId == null
}
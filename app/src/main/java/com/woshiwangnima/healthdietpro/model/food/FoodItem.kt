package com.woshiwangnima.healthdietpro.model.food

internal enum class FoodKind { INGREDIENT, FOOD, DISH }

internal sealed interface FoodItem {
    val id: String
    val kind: FoodKind
    val names: Map<String, List<String>>
    val healthMetrics: FoodHealthMetrics
    val commonness: Int
    val systemTags: List<String>
    val description: Map<String, String>
    val image: FoodImage?
    val sources: List<FoodSource>
    val servings: List<FoodServing>

    fun displayName(language: String): String = names[language]?.firstOrNull()
        ?: names["en"]?.firstOrNull()
        ?: names.values.firstOrNull()?.firstOrNull().orEmpty()

    fun allNames(language: String): List<String> = names[language]
        ?: names["en"]
        ?: names.values.firstOrNull().orEmpty()

    fun searchableNames(): List<String> = names.values.flatten()

    fun displayDescription(language: String): String = description[language]
        ?: description["en"]
        ?: description.values.firstOrNull().orEmpty()
}

internal sealed interface CategorizedFood : FoodItem {
    val categoryTags: List<String>
}

internal data class Ingredient(
    override val id: String,
    override val names: Map<String, List<String>>,
    override val categoryTags: List<String>,
    val nutritionTables: Map<String, FoodNutrientTable>,
    val edibleRatio: Double? = null,
    val densityGramsPerMl: Double? = null,
    override val servings: List<FoodServing> = emptyList(),
    override val healthMetrics: FoodHealthMetrics = FoodHealthMetrics(),
    override val commonness: Int = 0,
    override val systemTags: List<String> = emptyList(),
    override val description: Map<String, String> = emptyMap(),
    override val image: FoodImage? = null,
    override val sources: List<FoodSource> = emptyList(),
) : CategorizedFood {
    override val kind: FoodKind get() = FoodKind.INGREDIENT
}

internal data class PreparedFood(
    override val id: String,
    override val names: Map<String, List<String>>,
    override val categoryTags: List<String>,
    val derivedFrom: FoodDerivation,
    val densityGramsPerMl: Double? = null,
    override val servings: List<FoodServing> = emptyList(),
    override val healthMetrics: FoodHealthMetrics = FoodHealthMetrics(),
    override val commonness: Int = 0,
    override val systemTags: List<String> = emptyList(),
    override val description: Map<String, String> = emptyMap(),
    override val image: FoodImage? = null,
    override val sources: List<FoodSource> = emptyList(),
) : CategorizedFood {
    override val kind: FoodKind get() = FoodKind.FOOD
}

internal data class Dish(
    override val id: String,
    override val names: Map<String, List<String>>,
    val components: List<DishComponent>,
    override val servings: List<FoodServing> = emptyList(),
    override val healthMetrics: FoodHealthMetrics = FoodHealthMetrics(),
    override val commonness: Int = 0,
    override val systemTags: List<String> = emptyList(),
    override val description: Map<String, String> = emptyMap(),
    override val image: FoodImage? = null,
    override val sources: List<FoodSource> = emptyList(),
    // 菜肴扩展元数据（全部可选，缺省不影响既有数据）。
    val cuisine: String? = null,                       // 菜系 id（见 DishTaxonomy.cuisines）
    val dishCategories: List<String> = emptyList(),     // 菜品分类：家常/宴客/甜品…
    val recipeSteps: List<RecipeStep> = emptyList(),    // 制作教程/菜谱，逐步
    val difficulty: Int? = null,                        // 难度 1..5
    val servesPeople: Int? = null,                      // 几人份
    val tastes: List<String> = emptyList(),             // 口味：酸/辣/甜/咸… 多选
    val techniqueId: String? = null,                    // 工艺（复用 CookingMethod 枚举）
    val seasons: List<String> = emptyList(),            // 季节：春/夏/秋/冬 多选
) : FoodItem {
    override val kind: FoodKind get() = FoodKind.DISH
}

/** 一条制作步骤：文本 + 可选计时（分钟）。 */
internal data class RecipeStep(
    val text: String,
    val minutes: Int? = null,
)

internal data class FoodDerivation(
    val ingredientId: String,
    val cookingMethodId: String,
    val nutrientOverrides: Map<String, FoodAmount> = emptyMap(),
)

internal data class DishComponent(
    val foodId: String,
    val quantity: FoodQuantity,
)

internal data class FoodNutrientTable(
    val basis: FoodQuantity,
    val nutrients: Map<String, FoodAmount>,
)

internal data class FoodQuantity(
    val value: Double,
    val unitCategory: String,
    val unitId: String,
)

internal data class FoodServing(
    val id: String,
    val nutritionTableKey: String,
    val ratioToTable: Double,
    val labels: Map<String, String>,
    val containerId: String? = null,
    val fillRatio: Double? = null,
) {
    fun displayLabel(language: String): String =
        labels[language] ?: labels["en"] ?: labels.values.firstOrNull().orEmpty()
}

internal data class FoodAmount(
    val value: Double,
    val unitCategory: String,
    val unitId: String,
)

internal data class FoodHealthMetrics(
    val glycemicIndex: FoodMetric? = null,
    val glycemicLoadPer100g: FoodMetric? = null,
    val inflammatoryPotential: FoodMetric? = null,
)

internal data class FoodMetric(
    val value: Double,
    val unit: String,
    val basis: String = "per_100g_edible_portion",
    val note: String? = null,
)

internal data class FoodImage(
    val localKey: String = "food.illustration.default",
    val attribution: String,
)

internal data class FoodSource(
    val dataset: String,
    val reference: String,
)

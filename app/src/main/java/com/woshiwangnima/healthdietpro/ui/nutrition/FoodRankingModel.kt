package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.Dish
import com.woshiwangnima.healthdietpro.model.food.DishTaxonomy
import com.woshiwangnima.healthdietpro.model.food.FoodCategories
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import kotlin.math.abs

/**
 * 详情页「排行」页签的可比指标。
 *
 * 只列营养档案里**确实有数据**的字段：维生素 / 矿物质目前 263 条里只有一条完整（Q-11），
 * 把它们放进来只会得到一堆「暂无数据」的空榜。GI / GL 取自健康指标，与营养素分开取值。
 */
internal enum class RankMetric(@param:StringRes val labelRes: Int, val code: String) {
    ENERGY(R.string.rank_metric_energy, "ENERGY"),
    PROTEIN(R.string.rank_metric_protein, "PROTEIN"),
    FAT(R.string.rank_metric_fat, "FAT"),
    CHO(R.string.rank_metric_carbohydrate, "CHO"),
    FIBER(R.string.rank_metric_fiber, "FIBER"),
    GI(R.string.nutrition_metric_gi, "GI"),
    GL(R.string.nutrition_metric_gl, "GL"),
}

/** 榜单范围：同类别（食材/食物的根分类、菜肴的菜系）或同一 FoodKind 的全部条目。 */
internal enum class RankScope(@param:StringRes val labelRes: Int) {
    ALL(R.string.rank_scope_all),
    SAME_CATEGORY(R.string.rank_scope_same_category),
}

/**
 * 一条已经算好全部指标值的候选。
 *
 * 解析一次 `resolve` 就把七个指标全取出来，切指标时只重新排序，不再重跑 195 次营养解析；
 * [groupTag] 是「同类别」范围用的分组键，取不到就是 null（此时 UI 不提供该范围）。
 */
internal data class RankSample(
    val food: FoodItem,
    val groupTag: String?,
    val values: Map<RankMetric, Double?>,
)

internal data class RankEntry(val food: FoodItem, val value: Double, val rank: Int)

internal data class FoodRanking(
    val metric: RankMetric,
    val scope: RankScope,
    val descending: Boolean,
    val entries: List<RankEntry>,
    val total: Int,
    val currentRank: Int?,
    val currentValue: Double?,
    val median: Double?,
    val average: Double?,
    /** 单位来自 nutrients_meta.json 的 baseUnit；GI / GL 无单位。 */
    val unit: String,
    /** 「超过 X% 同类」的百分位；只有一个可比条目时记 100。 */
    val percentile: Int,
    @param:StringRes val groupLabelRes: Int?,
)

/** 默认一次渲染 10 行，其余按「再看 10 项」增量展开，避免 195 行一次性组合。 */
internal const val RankPageSize = 10

/** 分组键：食材/食物取根分类 tag，菜肴取菜系 id。 */
internal fun FoodItem.rankGroupTag(): String? = when (this) {
    is CategorizedFood -> categoryTags.firstNotNullOfOrNull { tag ->
        FoodCategories.roots.firstOrNull { root -> FoodCategories.isWithin(tag, root.tag) }?.tag
    }
    is Dish -> cuisine
}

@StringRes
internal fun rankGroupLabelRes(tag: String): Int? = FoodCategories.labelRes(tag) ?: DishTaxonomy.labelRes(tag)

/** GI / GL 不占营养素表，单位留空；其余用元数据声明的显示单位（kcal / g / mg）。 */
internal fun rankUnit(metric: RankMetric, nutrientMetas: List<NutrientMeta>): String =
    if (metric == RankMetric.GI || metric == RankMetric.GL) ""
    else nutrientMetas.firstOrNull { it.code == metric.code }?.baseUnit.orEmpty()

/**
 * 把一份解析结果折成「每 100 g」口径。
 *
 * Ingredient / PreparedFood 的表本来就是 100 g，factor = 1；Dish 的 `resolvePer100g`
 * 实际返回**整道菜合计**（basis = 各组分克数之和），不折算的话一道 800 g 的菜会
 * 因为分量大而占据所有榜单首位，那就不是营养密度排行而是菜量排行了。
 * 非克重 basis（如 100 ml 饮品）保持原值，不做假折算。
 */
private fun ResolvedNutrition.per100gFactor(): Double {
    val grams = basis.value
    return if (basis.unitId.equals("g", ignoreCase = true) && grams > 0.0) 100.0 / grams else 1.0
}

private fun RankMetric.valueFrom(
    resolved: ResolvedNutrition,
    meta: NutrientMeta?,
    factor: Double,
): Double? = when (code) {
    // GI 是比值，与份量无关，不折算。
    "GI" -> resolved.healthMetrics.glycemicIndex?.value
    // GL 语义是「每 100 g 的升糖负荷」，整份口径的菜肴同样折回 100 g。
    "GL" -> resolved.healthMetrics.glycemicLoadPer100g?.value?.times(factor)
    else -> {
        val amount = resolved.nutrients[code] ?: return null
        val base = meta?.let { amount.toDisplayValue(it) } ?: amount.value
        base * factor
    }
}

/** 一次解析、七个指标全取；解析失败的条目（缺组分、循环引用等）只留一个空 values，不进任何榜单。 */
internal fun collectRankSamples(
    foods: List<FoodItem>,
    kind: FoodKind,
    resolve: (FoodItem) -> ResolvedNutrition,
    nutrientMetas: List<NutrientMeta>,
): List<RankSample> {
    val metaByCode = nutrientMetas.associateBy(NutrientMeta::code)
    return foods.filter { it.kind == kind }.map { food ->
        val resolved = runCatching { resolve(food) }.getOrNull()
        val factor = resolved?.per100gFactor() ?: 1.0
        val values = RankMetric.values().associateWith { metric ->
            resolved?.let { metric.valueFrom(it, metaByCode[metric.code], factor) }
        }
        RankSample(food, food.rankGroupTag(), values)
    }
}

private fun List<RankSample>.scoped(scope: RankScope, groupTag: String?): List<RankSample> =
    if (scope == RankScope.SAME_CATEGORY && groupTag != null) filter { it.groupTag == groupTag } else this

/** 只有至少两项有值的指标才出现在切换栏里，避免用户点进一个必然为空的榜。 */
internal fun availableRankMetrics(
    samples: List<RankSample>,
    scope: RankScope,
    groupTag: String?,
): List<RankMetric> {
    val scoped = samples.scoped(scope, groupTag)
    return RankMetric.values().filter { metric ->
        scoped.count { it.values[metric] != null } >= 2
    }
}

/** 中位数：偶数项取中间两值的平均；榜单为空返回 null。 */
private fun medianOf(sorted: List<Double>): Double? = when {
    sorted.isEmpty() -> null
    sorted.size % 2 == 1 -> sorted[sorted.size / 2]
    else -> (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
}

internal fun buildFoodRanking(
    current: FoodItem,
    samples: List<RankSample>,
    metric: RankMetric,
    scope: RankScope,
    descending: Boolean,
    unit: String,
    language: String,
): FoodRanking {
    val groupTag = current.rankGroupTag()
    val scoped = samples.scoped(scope, groupTag)
    val pairs = scoped.mapNotNull { sample ->
        sample.values[metric]?.let { value -> sample.food to value }
    }
    val comparator = if (descending) {
        compareByDescending<Pair<FoodItem, Double>> { it.second }.thenBy { it.first.displayName(language) }
    } else {
        compareBy<Pair<FoodItem, Double>> { it.second }.thenBy { it.first.displayName(language) }
    }
    val entries = pairs.sortedWith(comparator).mapIndexed { index, (foodItem, value) ->
        RankEntry(foodItem, value, index + 1)
    }
    val currentEntry = entries.firstOrNull { it.food.id == current.id }
    val sortedValues = entries.map(RankEntry::value).sorted()
    val total = entries.size
    val rank = currentEntry?.rank
    return FoodRanking(
        metric = metric,
        scope = scope,
        descending = descending,
        entries = entries,
        total = total,
        currentRank = rank,
        currentValue = currentEntry?.value,
        median = medianOf(sortedValues),
        average = sortedValues.takeIf { it.isNotEmpty() }?.average(),
        unit = unit,
        // 末名 = 0%，榜首 = 100%；只有一项时「超过 100%」没有意义，统一记 100。
        percentile = if (total > 1 && rank != null) ((total - rank) * 100.0 / (total - 1)).toInt() else 100,
        groupLabelRes = if (scope == RankScope.SAME_CATEGORY) groupTag?.let { rankGroupLabelRes(it) } else null,
    )
}

/** 与中位数的差距，带符号；中位数为 0 或当前项无值时不给结论，避免除以接近 0 的数放大误差。 */
internal fun FoodRanking.medianDeltaPercent(): Int? {
    val current = currentValue ?: return null
    val base = median ?: return null
    if (abs(base) < 0.0001) return null
    return ((current - base) * 100.0 / abs(base)).toInt()
}

/**
 * 金字塔分档的**唯一清单**（与 `ArchiveDomains` 同一套「只改一处」约定）。
 *
 * 档位按「超过同类的百分比」切，语义与 [FoodRanking.percentile] 完全一致：榜首 100、末名 0。
 * [minPercentile] 含、[maxPercentile] 不含，顶层给 101 才能把 percentile = 100 收进来。
 * [weight] 是该层底边宽度占可用宽度的比例，从上到下递增才有金字塔轮廓（越往上越稀有）。
 *
 * 加一档、改阈值、改塔形都只改这里：UI 层数、单测断言、点击联动全部从清单派生。
 */
internal data class RankTier(
    val id: String,
    @param:StringRes val labelRes: Int,
    val minPercentile: Int,
    val maxPercentile: Int,
    val weight: Float,
)

internal val RankTiers = listOf(
    RankTier("top", R.string.rank_tier_top, 90, 101, 0.52f),
    RankTier("upper", R.string.rank_tier_upper, 70, 90, 0.64f),
    RankTier("middle", R.string.rank_tier_middle, 30, 70, 0.76f),
    RankTier("lower", R.string.rank_tier_lower, 10, 30, 0.88f),
    RankTier("bottom", R.string.rank_tier_bottom, 0, 10, 1.0f),
)

/**
 * 金字塔的一档：只给数据，不掺文案与颜色，方便 JVM 单测直接断言。
 *
 * [fromRank] / [toRank] 是 1-based 且闭区间；空档两值记 0，UI 据此显示「—」。
 * [minValue] / [maxValue] 是该档条目数值的极值，与排序方向无关——无论「高→低」还是「低→高」，
 * 区间都写成 min–max，读者结合顶部当前值自己能对上方向。
 */
internal data class RankPyramidBand(
    val id: String,
    @param:StringRes val labelRes: Int,
    val weight: Float,
    val fromRank: Int,
    val toRank: Int,
    val count: Int,
    val minValue: Double?,
    val maxValue: Double?,
    val isCurrent: Boolean,
)

/** 把一份排行拆成 [RankTiers] 各档；空榜返回空列表，调用方直接走空态。 */
internal fun FoodRanking.pyramidBands(): List<RankPyramidBand> {
    if (total <= 0 || entries.isEmpty()) return emptyList()
    // 与 FoodRanking.percentile 用同一把尺子，别让两个地方对「百分位」各有解释。
    fun percentileOf(rank: Int): Int =
        if (total > 1) ((total - rank) * 100.0 / (total - 1)).toInt() else 100
    val currentPercentile = currentRank?.let(::percentileOf)
    return RankTiers.map { tier ->
        fun inTier(percentile: Int) = percentile >= tier.minPercentile && percentile < tier.maxPercentile
        val inBand = entries.filter { inTier(percentileOf(it.rank)) }
        val ranks = inBand.map(RankEntry::rank)
        val values = inBand.map(RankEntry::value)
        RankPyramidBand(
            id = tier.id,
            labelRes = tier.labelRes,
            weight = tier.weight,
            fromRank = ranks.minOrNull() ?: 0,
            toRank = ranks.maxOrNull() ?: 0,
            count = inBand.size,
            minValue = values.minOrNull(),
            maxValue = values.maxOrNull(),
            isCurrent = currentPercentile != null && inTier(currentPercentile),
        )
    }
}

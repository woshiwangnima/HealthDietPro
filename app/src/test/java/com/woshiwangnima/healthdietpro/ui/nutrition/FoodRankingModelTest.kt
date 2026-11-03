package com.woshiwangnima.healthdietpro.ui.nutrition

import com.woshiwangnima.healthdietpro.model.food.Dish
import com.woshiwangnima.healthdietpro.model.food.FoodAmount
import com.woshiwangnima.healthdietpro.model.food.FoodHealthMetrics
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.FoodMetric
import com.woshiwangnima.healthdietpro.model.food.FoodQuantity
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 排行数据层：口径折算、排序方向、无值过滤、范围过滤、名次与百分位。
 *
 * 重点是「整份菜肴折回每 100 g」和「只有有值的条目才参与」两条：前者防止大份菜靠分量霸榜，
 * 后者防止营养数据不齐（Q-11）时把一堆「—」排进榜单。
 */
class FoodRankingModelTest {

    private fun ingredient(
        id: String,
        name: String,
        categoryTags: List<String> = emptyList(),
    ) = Ingredient(
        id = id,
        names = mapOf("zh" to listOf(name), "en" to listOf(name)),
        categoryTags = categoryTags,
        nutritionTables = emptyMap(),
    )

    private fun dish(id: String, name: String, cuisine: String? = null) = Dish(
        id = id,
        names = mapOf("zh" to listOf(name), "en" to listOf(name)),
        components = emptyList(),
        cuisine = cuisine,
    )

    private fun amount(value: Double, unitId: String, category: String = "weight") =
        FoodAmount(value = value, unitCategory = category, unitId = unitId)

    /** 营养素单位与 meta.baseUnit 一致，toDisplayValue 会直接采信原值，不依赖 UnitConverter。 */
    private fun resolved(
        basisGrams: Double = 100.0,
        vararg nutrients: Pair<String, FoodAmount>,
        glycemicIndex: Double? = null,
        glycemicLoad: Double? = null,
    ) = ResolvedNutrition(
        basis = FoodQuantity(value = basisGrams, unitCategory = "weight", unitId = "g"),
        nutrients = nutrients.toMap(),
        healthMetrics = FoodHealthMetrics(
            glycemicIndex = glycemicIndex?.let { FoodMetric(it, "GI") },
            glycemicLoadPer100g = glycemicLoad?.let { FoodMetric(it, "GL") },
        ),
    )

    private val metas = listOf(
        NutrientMeta("ENERGY", "energy", "", "kcal", mapOf("zh" to "能量", "en" to "Energy")),
        NutrientMeta("PROTEIN", "protein", "", "g", mapOf("zh" to "蛋白质", "en" to "Protein")),
        NutrientMeta("FAT", "fat", "", "g", mapOf("zh" to "脂肪", "en" to "Fat")),
        NutrientMeta("CHO", "carbohydrate", "", "g", mapOf("zh" to "碳水", "en" to "Carbs")),
        NutrientMeta("FIBER", "fiber", "", "g", mapOf("zh" to "膳食纤维", "en" to "Fiber")),
    )

    private fun resolverOf(vararg pairs: Pair<String, ResolvedNutrition>): (FoodItem) -> ResolvedNutrition {
        val byId = pairs.toMap()
        return { food -> byId[food.id] ?: throw IllegalStateException("no nutrition for ${food.id}") }
    }

    @Test
    fun collectsOnlySameKindAndSkipsUnresolvableItems() {
        val a = ingredient("a", "苹果", listOf("food.fruit"))
        val b = ingredient("b", "菠菜", listOf("food.vegetable.leafy"))
        val d = dish("d", "炒菠菜")
        val resolve = resolverOf(
            "a" to resolved(100.0, "ENERGY" to amount(50.0, "kcal", "energy")),
            "b" to resolved(100.0, "ENERGY" to amount(20.0, "kcal", "energy")),
            // d 故意缺解析结果，collect 应留一个全 null 的 sample。
        )
        val samples = collectRankSamples(listOf(a, b, d), FoodKind.INGREDIENT, resolve, metas)
        assertEquals(2, samples.size)
        assertTrue(samples.all { it.food.kind == FoodKind.INGREDIENT })
        assertEquals(50.0, samples.first { it.food.id == "a" }.values[RankMetric.ENERGY]!!, 0.0001)
        val dishSamples = collectRankSamples(listOf(a, b, d), FoodKind.DISH, resolve, metas)
        assertEquals(1, dishSamples.size)
        assertTrue(dishSamples.single().values.values.all { it == null })
    }

    @Test
    fun descendingAndAscendingProduceReversedRanks() {
        val a = ingredient("a", "A")
        val b = ingredient("b", "B")
        val c = ingredient("c", "C")
        val resolve = resolverOf(
            "a" to resolved(100.0, "PROTEIN" to amount(10.0, "g")),
            "b" to resolved(100.0, "PROTEIN" to amount(30.0, "g")),
            "c" to resolved(100.0, "PROTEIN" to amount(20.0, "g")),
        )
        val samples = collectRankSamples(listOf(a, b, c), FoodKind.INGREDIENT, resolve, metas)
        val desc = buildFoodRanking(a, samples, RankMetric.PROTEIN, RankScope.ALL, true, "g", "en")
        assertEquals(listOf("b", "c", "a"), desc.entries.map { it.food.id })
        assertEquals(listOf(1, 2, 3), desc.entries.map { it.rank })
        assertEquals(3, desc.currentRank)
        val asc = buildFoodRanking(a, samples, RankMetric.PROTEIN, RankScope.ALL, false, "g", "en")
        assertEquals(listOf("a", "c", "b"), asc.entries.map { it.food.id })
        assertEquals(1, asc.currentRank)
    }

    @Test
    fun itemsWithoutMetricAreExcludedAndMetricAvailabilityNeedsTwoItems() {
        val a = ingredient("a", "A")
        val b = ingredient("b", "B")
        val c = ingredient("c", "C")
        val resolve = resolverOf(
            "a" to resolved(100.0, "ENERGY" to amount(50.0, "kcal", "energy")),
            "b" to resolved(100.0, "ENERGY" to amount(20.0, "kcal", "energy")),
            // 只有 c 有 FIBER，不足两项，不应出现在切换栏。
            "c" to resolved(100.0, "FIBER" to amount(3.0, "g")),
        )
        val samples = collectRankSamples(listOf(a, b, c), FoodKind.INGREDIENT, resolve, metas)
        val ranking = buildFoodRanking(a, samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        assertEquals(2, ranking.total)
        assertFalse(ranking.entries.any { it.food.id == "c" })
        val available = availableRankMetrics(samples, RankScope.ALL, null)
        assertTrue(available.contains(RankMetric.ENERGY))
        assertFalse(available.contains(RankMetric.FIBER))
        assertFalse(available.contains(RankMetric.GI))
    }

    @Test
    fun wholeDishIsNormalisedBackToPer100g() {
        // 800 g 的整份菜合计 800 kcal，折回每 100 g 应为 100 kcal，
        // 与一份本来就是 100 g / 100 kcal 的菜并列，而不是排到它前面。
        val big = dish("big", "大份菜")
        val small = dish("small", "小份菜")
        val resolve = resolverOf(
            "big" to resolved(800.0, "ENERGY" to amount(800.0, "kcal", "energy"), glycemicIndex = 55.0, glycemicLoad = 12.0),
            "small" to resolved(100.0, "ENERGY" to amount(100.0, "kcal", "energy")),
        )
        val samples = collectRankSamples(listOf(big, small), FoodKind.DISH, resolve, metas)
        assertEquals(100.0, samples.first { it.food.id == "big" }.values[RankMetric.ENERGY]!!, 0.0001)
        assertEquals(100.0, samples.first { it.food.id == "small" }.values[RankMetric.ENERGY]!!, 0.0001)
        // GI 是比值，不随份量折算；GL 按每 100 g 口径折算。
        assertEquals(55.0, samples.first { it.food.id == "big" }.values[RankMetric.GI]!!, 0.0001)
        assertEquals(1.5, samples.first { it.food.id == "big" }.values[RankMetric.GL]!!, 0.0001)
    }

    @Test
    fun currentRankPercentileMedianAndAverage() {
        val a = ingredient("a", "A")
        val b = ingredient("b", "B")
        val c = ingredient("c", "C")
        val d = ingredient("d", "D")
        val resolve = resolverOf(
            "a" to resolved(100.0, "ENERGY" to amount(10.0, "kcal", "energy")),
            "b" to resolved(100.0, "ENERGY" to amount(20.0, "kcal", "energy")),
            "c" to resolved(100.0, "ENERGY" to amount(30.0, "kcal", "energy")),
            "d" to resolved(100.0, "ENERGY" to amount(40.0, "kcal", "energy")),
        )
        val samples = collectRankSamples(listOf(a, b, c, d), FoodKind.INGREDIENT, resolve, metas)
        val ranking = buildFoodRanking(c, samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        assertEquals(2, ranking.currentRank)
        assertEquals(30.0, ranking.currentValue!!, 0.0001)
        assertEquals(25.0, ranking.median!!, 0.0001)
        assertEquals(25.0, ranking.average!!, 0.0001)
        // 4 项里第 2 名 → 超过 (4-2)/(4-1) = 66%
        assertEquals(66, ranking.percentile)
        assertEquals(20, ranking.medianDeltaPercent())
        val last = buildFoodRanking(a, samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        assertEquals(0, last.percentile)
        assertEquals(-60, last.medianDeltaPercent())
    }

    @Test
    fun sameCategoryScopeFiltersByRootTag() {
        val apple = ingredient("apple", "苹果", listOf("food.fruit"))
        val spinach = ingredient("spinach", "菠菜", listOf("food.vegetable.leafy"))
        val rice = ingredient("rice", "米饭", listOf("food.staple.grain"))
        val resolve = resolverOf(
            "apple" to resolved(100.0, "ENERGY" to amount(50.0, "kcal", "energy")),
            "spinach" to resolved(100.0, "ENERGY" to amount(20.0, "kcal", "energy")),
            "rice" to resolved(100.0, "ENERGY" to amount(120.0, "kcal", "energy")),
        )
        assertEquals("food.fruit", apple.rankGroupTag())
        assertEquals("food.staple", rice.rankGroupTag())
        val samples = collectRankSamples(listOf(apple, spinach, rice), FoodKind.INGREDIENT, resolve, metas)
        val all = buildFoodRanking(rice, samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        assertEquals(3, all.total)
        assertNull(all.groupLabelRes)
        val scoped = buildFoodRanking(rice, samples, RankMetric.ENERGY, RankScope.SAME_CATEGORY, true, "kcal", "en")
        assertEquals(1, scoped.total)
        assertEquals(1, scoped.currentRank)
        assertNotNull(scoped.groupLabelRes)
        // 只有一项时不该出现「超过 100% 同类」这种误导结论。
        assertEquals(100, scoped.percentile)
    }

    @Test
    fun dishGroupTagUsesCuisineAndUnitIsEmptyForGlycemicMetrics() {
        val d = dish("d", "宫保鸡丁", "cuisine.sichuan")
        assertEquals("cuisine.sichuan", d.rankGroupTag())
        assertNotNull(rankGroupLabelRes("cuisine.sichuan"))
        assertEquals("", rankUnit(RankMetric.GI, metas))
        assertEquals("", rankUnit(RankMetric.GL, metas))
        assertEquals("kcal", rankUnit(RankMetric.ENERGY, metas))
        assertEquals("g", rankUnit(RankMetric.FIBER, metas))
    }

    /** 造 count 条线性递增的能量值，供金字塔分档断言用。 */
    private fun linearFoods(count: Int): Pair<List<FoodItem>, (FoodItem) -> ResolvedNutrition> {
        val items: List<FoodItem> = (1..count).map { ingredient("i$it", "I$it") }
        val pairs = items.mapIndexed { index, item ->
            item.id to resolved(100.0, "ENERGY" to amount((index + 1) * 10.0, "kcal", "energy"))
        }
        return items to resolverOf(*pairs.toTypedArray())
    }

    @Test
    fun pyramidBandsPartitionEveryRankExactlyOnce() {
        val (items, resolve) = linearFoods(20)
        val samples = collectRankSamples(items, FoodKind.INGREDIENT, resolve, metas)
        val ranking = buildFoodRanking(items[4], samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        val bands = ranking.pyramidBands()
        assertEquals(RankTiers.size, bands.size)
        assertEquals(ranking.total, bands.sumOf { it.count })
        assertTrue(bands.all { it.count > 0 })
        // 名次区间按层递增且互不重叠：同一个 rank 不能同时落在两层里。
        bands.zipWithNext().forEach { (upper, lower) ->
            assertTrue(upper.toRank < lower.fromRank)
        }
        assertEquals(1, bands.first().fromRank)
        assertEquals(ranking.total, bands.last().toRank)
    }

    @Test
    fun pyramidBandsMarkExactlyOneTierForCurrentItem() {
        val (items, resolve) = linearFoods(20)
        val samples = collectRankSamples(items, FoodKind.INGREDIENT, resolve, metas)
        // 降序下数值最大的是榜首、最小的是末名，两者应落在不同层且各只有一层被标亮。
        val leader = buildFoodRanking(items.last(), samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        assertEquals("top", leader.pyramidBands().single { it.isCurrent }.id)
        val last = buildFoodRanking(items[0], samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        assertEquals("bottom", last.pyramidBands().single { it.isCurrent }.id)
        assertEquals(1, last.pyramidBands().count { it.isCurrent })
    }

    @Test
    fun pyramidBandsValueRangeFollowsSortDirection() {
        val (items, resolve) = linearFoods(10)
        val samples = collectRankSamples(items, FoodKind.INGREDIENT, resolve, metas)
        val desc = buildFoodRanking(items[0], samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        val topDesc = desc.pyramidBands().first()
        assertEquals(100.0, topDesc.minValue!!, 0.0001)
        assertEquals(100.0, topDesc.maxValue!!, 0.0001)
        val asc = buildFoodRanking(items[0], samples, RankMetric.ENERGY, RankScope.ALL, false, "kcal", "en")
        val topAsc = asc.pyramidBands().first()
        assertEquals(10.0, topAsc.minValue!!, 0.0001)
        assertEquals(10.0, topAsc.maxValue!!, 0.0001)
    }

    @Test
    fun emptyRankingHasNoPyramidBands() {
        val (items, _) = linearFoods(3)
        // 所有条目都没有能量值 → total = 0，金字塔直接走空态，不能画出五层空档。
        val resolve: (FoodItem) -> ResolvedNutrition = { resolved(100.0) }
        val samples = collectRankSamples(items, FoodKind.INGREDIENT, resolve, metas)
        val ranking = buildFoodRanking(items[0], samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        assertEquals(0, ranking.total)
        assertTrue(ranking.pyramidBands().isEmpty())
    }

    @Test
    fun singleItemRankingLandsInTopTierOnly() {
        val (items, resolve) = linearFoods(1)
        val samples = collectRankSamples(items, FoodKind.INGREDIENT, resolve, metas)
        val ranking = buildFoodRanking(items[0], samples, RankMetric.ENERGY, RankScope.ALL, true, "kcal", "en")
        val bands = ranking.pyramidBands()
        assertEquals(100, ranking.percentile)
        assertEquals(1, bands.first().count)
        assertTrue(bands.first().isCurrent)
        assertTrue(bands.drop(1).all { it.count == 0 && !it.isCurrent })
        assertNull(bands.last().minValue)
    }
}

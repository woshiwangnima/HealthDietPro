package com.woshiwangnima.healthdietpro.ui.nutrition

import com.woshiwangnima.healthdietpro.model.food.FoodAmount
import com.woshiwangnima.healthdietpro.model.food.FoodQuantity
import com.woshiwangnima.healthdietpro.model.food.Ingredient
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.util.UnitConverter
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * 对比页的数据层：分组、单位归一、结论计数。
 *
 * 单位归一这几条是防回归的重点——同一项营养素在两份数据里可能一个写 5 mg、一个写 5000 mcg，
 * 按原始数字比会得出「差 1000 倍」的假结论，而旧实现正是直接比原始数字。
 */
class FoodCompareModelTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            UnitConverter.initFromAsset("src/main/assets/units.json")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            UnitConverter.reset()
        }
    }

    private fun food(id: String, name: String) = Ingredient(
        id = id,
        names = mapOf("zh" to listOf(name), "en" to listOf(name)),
        categoryTags = emptyList(),
        nutritionTables = emptyMap(),
    )

    private fun meta(code: String, category: String, baseUnit: String, name: String) = NutrientMeta(
        code = code,
        category = category,
        needType = "",
        baseUnit = baseUnit,
        names = mapOf("zh" to name, "en" to name),
    )

    private fun amount(value: Double, unitId: String, category: String = "weight") =
        FoodAmount(value = value, unitCategory = category, unitId = unitId)

    private fun resolved(vararg nutrients: Pair<String, FoodAmount>) = ResolvedNutrition(
        basis = FoodQuantity(value = 100.0, unitCategory = "weight", unitId = "g"),
        nutrients = nutrients.toMap(),
    )

    private val energyMeta = meta("ENERGY", "energy", "kcal", "能量")
    private val proteinMeta = meta("PROTEIN", "protein", "g", "蛋白质")
    private val fatMeta = meta("FAT", "fat", "g", "脂肪")
    private val vitaminCMeta = meta("VC", "vitamin.water_soluble", "mg", "维生素 C")
    private val sodiumMeta = meta("NA", "mineral.major", "mg", "钠")
    private val lysineMeta = meta("LYS", "protein.amino_acid", "mg", "赖氨酸")

    @Test
    fun metadataCategoryMapsToDisplayGroup() {
        assertEquals(CompareGroup.MACRO, compareGroupOf(energyMeta))
        assertEquals(CompareGroup.MACRO, compareGroupOf(proteinMeta))
        assertEquals(CompareGroup.MACRO, compareGroupOf(fatMeta))
        assertEquals(CompareGroup.VITAMIN_WATER, compareGroupOf(vitaminCMeta))
        assertEquals(CompareGroup.MINERAL_MAJOR, compareGroupOf(sodiumMeta))
        assertEquals(CompareGroup.AMINO_ACID, compareGroupOf(lysineMeta))
        assertEquals(
            CompareGroup.OTHER,
            compareGroupOf(meta("X", "something.new", "g", "未知")),
        )
    }

    @Test
    fun sectionsSkipNutrientsMissingOnBothSides() {
        val a = food("a", "煮玉米")
        val b = food("b", "红烧肉")
        val sections = buildCompareSections(
            food = a,
            opponent = b,
            nutrientMetas = listOf(energyMeta, proteinMeta, fatMeta),
            resolve = { item ->
                when (item.id) {
                    "a" -> resolved("ENERGY" to amount(43.0, "kcal", "energy"), "PROTEIN" to amount(1.6, "g"))
                    else -> resolved("ENERGY" to amount(472.0, "kcal", "energy"))
                }
            },
            language = "zh",
        )
        val rows = sections.flatMap(CompareSection::rows)
        // 脂肪两边都没有值，不该占一行空白。
        assertEquals(listOf("ENERGY", "PROTEIN"), rows.map(CompareRow::code))
        // 只有一侧有值时仍保留该行，另一侧为 null。
        assertNull(rows.first { it.code == "PROTEIN" }.right)
        assertFalse(rows.first { it.code == "PROTEIN" }.comparable)
        assertTrue(rows.first { it.code == "ENERGY" }.comparable)
    }

    @Test
    fun sectionsFollowEnumOrderNotMetadataOrder() {
        val sections = buildCompareSections(
            food = food("a", "A"),
            opponent = food("b", "B"),
            // 元数据故意按「矿物质 → 氨基酸 → 维生素 → 宏量」给，页面仍应按枚举顺序排。
            nutrientMetas = listOf(sodiumMeta, lysineMeta, vitaminCMeta, energyMeta),
            resolve = { resolved("ENERGY" to amount(1.0, "kcal", "energy"), "NA" to amount(1.0, "mg"), "VC" to amount(1.0, "mg"), "LYS" to amount(1.0, "mg")) },
            language = "zh",
        )
        assertEquals(
            listOf(CompareGroup.MACRO, CompareGroup.AMINO_ACID, CompareGroup.VITAMIN_WATER, CompareGroup.MINERAL_MAJOR),
            sections.map(CompareSection::group),
        )
    }

    @Test
    fun microgramValuesAreConvertedToMetadataUnit() {
        // 5000 mcg == 5 mg：按原始数字比会得到「高 1000 倍」的假结论。
        val converted = amount(5000.0, "mcg").toDisplayValue(meta("VC", "vitamin.water_soluble", "mg", "维生素 C"))
        assertEquals(5.0, converted, 0.001)
    }

    @Test
    fun kilojouleEnergyIsConvertedToKilocalories() {
        val converted = amount(180.0, "kj", "energy").toDisplayValue(energyMeta)
        assertEquals(43.02, converted, 0.1)
    }

    @Test
    fun sameUnitValueIsReturnedUntouched() {
        assertEquals(1.6, amount(1.6, "g").toDisplayValue(proteinMeta), 1e-9)
    }

    @Test
    fun summaryCountsWithDisplayPrecision() {
        val rows = listOf(
            CompareRow("A", "A", "g", 1.0, 2.0),
            CompareRow("B", "B", "g", 2.0, 1.0),
            // 显示精度下都是 0.1，算「相同」，否则结论卡会出现「0 项相同」但每一行看着一样。
            CompareRow("C", "C", "g", 0.1, 0.10004),
            // 只有一侧有值：不计入任何一栏。
            CompareRow("D", "D", "g", 3.0, null),
        )
        val summary = compareSummary(listOf(CompareSection(CompareGroup.MACRO, rows)))
        assertEquals(1, summary.leftHigher)
        assertEquals(1, summary.rightHigher)
        assertEquals(1, summary.same)
        assertEquals(3, summary.comparable)
    }

    @Test
    fun valueFormattingFollowsMagnitude() {
        assertEquals("43", formatCompareValue(43.0))
        assertEquals("1.6", formatCompareValue(1.63))
        assertEquals("0.046", formatCompareValue(0.0456))
        assertEquals("0", formatCompareValue(0.0))
    }

    @Test
    fun duelBarScaleUsesLargerSide() {
        val row = CompareRow("A", "A", "g", 1.6, 2.8)
        assertEquals(2.8, row.maxValue, 1e-9)
        // 短的一侧按 1.6 / 2.8 取长度，而不是各自拉满。
        assertEquals(1.6 / 2.8, (row.left ?: 0.0) / row.maxValue, 1e-9)
    }
}

package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.food.FoodAmount
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Locale
import kotlin.math.abs

/**
 * 对比页的营养分组。
 *
 * 按 `nutrients_meta.json` 的 `category` 归并：49 项营养素平铺成一张表没人扫得动，
 * 分组后每一段都短到可以一眼看完，找「蛋白质」「钠」这种目标也变成先找组再找行。
 * 枚举顺序即页面顺序：能量与宏量 → 氨基酸 → 维生素 → 矿物质 → 其他。
 */
internal enum class CompareGroup(@param:StringRes val titleRes: Int) {
    MACRO(R.string.nutrition_compare_group_macro),
    AMINO_ACID(R.string.nutrition_compare_group_amino),
    VITAMIN_WATER(R.string.nutrition_compare_group_vitamin_water),
    VITAMIN_FAT(R.string.nutrition_compare_group_vitamin_fat),
    MINERAL_MAJOR(R.string.nutrition_compare_group_mineral_major),
    MINERAL_TRACE(R.string.nutrition_compare_group_mineral_trace),
    OTHER(R.string.nutrition_compare_group_other),
}

internal fun compareGroupOf(meta: NutrientMeta): CompareGroup = when (meta.category) {
    "energy", "protein", "fat", "fat.essential", "carbohydrate", "fiber", "water" -> CompareGroup.MACRO
    "protein.amino_acid" -> CompareGroup.AMINO_ACID
    "vitamin.water_soluble" -> CompareGroup.VITAMIN_WATER
    "vitamin.fat_soluble" -> CompareGroup.VITAMIN_FAT
    "mineral.major" -> CompareGroup.MINERAL_MAJOR
    "mineral.trace" -> CompareGroup.MINERAL_TRACE
    else -> CompareGroup.OTHER
}

/**
 * 一行营养素对比。
 *
 * [left] / [right] 已按 [unit] 归一：两份数据可能一个写 5 mg、一个写 0.005 g，
 * 直接比原始数字会得出「一边高 1000 倍」的假结论。归一目标取营养素元数据里的 `baseUnit`，
 * 也就是这一行右边显示的单位。
 */
internal data class CompareRow(
    val code: String,
    val name: String,
    val unit: String,
    val left: Double?,
    val right: Double?,
) {
    /** 两边都有值才谈得上「谁更高」；只有一边有值时只能显示「—」。 */
    val comparable: Boolean get() = left != null && right != null

    val leftHigher: Boolean get() = comparable && !sameAtDisplayPrecision() && left!! > right!!

    val rightHigher: Boolean get() = comparable && !sameAtDisplayPrecision() && left!! < right!!

    /** 双柱的满量程：取两侧较大值，这样较短的一柱长度就是真实比例，而不是「都拉满」。 */
    val maxValue: Double get() = maxOf(left ?: 0.0, right ?: 0.0)
}

internal data class CompareSection(val group: CompareGroup, val rows: List<CompareRow>)

/** 结论卡的三个数字，只在两侧都有值的行上统计。 */
internal data class CompareSummary(
    val leftHigher: Int,
    val rightHigher: Int,
    val same: Int,
) {
    val comparable: Int get() = leftHigher + rightHigher + same
}

/**
 * 把两份营养数据摊成「分组 + 行」。
 *
 * - 两侧都没有值的营养素不出现（会出现一整段空白行）；
 * - 只有一侧有值的保留，另一侧显示「—」，因为「这个食物没有这项数据」本身就是信息；
 * - 分组按 [CompareGroup] 的声明顺序排，不按元数据里的出现顺序，否则氨基酸会插到矿物质后面。
 */
internal fun buildCompareSections(
    food: FoodItem,
    opponent: FoodItem?,
    nutrientMetas: List<NutrientMeta>,
    resolve: (FoodItem) -> ResolvedNutrition,
    language: String,
): List<CompareSection> {
    val left = runCatching { resolve(food) }.getOrNull()
    val right = opponent?.let { runCatching { resolve(it) }.getOrNull() }
    val grouped = LinkedHashMap<CompareGroup, MutableList<CompareRow>>()
    nutrientMetas.forEach { meta ->
        val leftValue = left?.nutrients?.get(meta.code)?.toDisplayValue(meta)
        val rightValue = right?.nutrients?.get(meta.code)?.toDisplayValue(meta)
        if (leftValue == null && rightValue == null) return@forEach
        grouped.getOrPut(compareGroupOf(meta)) { mutableListOf() }.add(
            CompareRow(
                code = meta.code,
                name = meta.displayName(language),
                unit = meta.baseUnit,
                left = leftValue,
                right = rightValue,
            ),
        )
    }
    return grouped.entries
        .sortedBy { it.key.ordinal }
        .map { (group, rows) -> CompareSection(group, rows) }
}

internal fun compareSummary(sections: List<CompareSection>): CompareSummary {
    val rows = sections.flatMap(CompareSection::rows)
    return CompareSummary(
        leftHigher = rows.count(CompareRow::leftHigher),
        rightHigher = rows.count(CompareRow::rightHigher),
        same = rows.count { it.comparable && it.sameAtDisplayPrecision() },
    )
}

/**
 * 换到营养素元数据声明的单位再比较。
 *
 * 单位表没初始化时 `toBase` / `fromBase` 都会原样返回，此时换算结果等于原值，直接采信也无害；
 * 只有换算出 0 而原值不是 0（单位 id 不认识）时才退回原值，避免把一行数字显示成 0。
 */
internal fun FoodAmount.toDisplayValue(meta: NutrientMeta): Double {
    val target = meta.baseUnit.ifBlank { unitId }
    if (unitId.equals(target, ignoreCase = true)) return value
    val category = if (meta.code.uppercase(Locale.ROOT) == "ENERGY") "energy" else "weight"
    val base = UnitConverter.toBase(category, value.toFloat(), unitId.lowercase(Locale.ROOT))
    val converted = UnitConverter.fromBase(category, base, target.lowercase(Locale.ROOT)).toDouble()
    return if (converted == 0.0 && value != 0.0) value else converted
}

/**
 * 对比行的显示格式：不固定小数位，否则「能量 43」会写成 43.0、「维生素 B12 0.0004」会写成 0.0。
 * 量级越大越粗、越小越细；整数不拖 `.0`（43 千卡的格子写 43 比 43.0 干净）。
 */
internal fun formatCompareValue(value: Double): String {
    val decimals = when {
        value == 0.0 -> 0
        value >= 100 -> 0
        value >= 1 -> 1
        value >= 0.1 -> 2
        else -> 3
    }
    val text = String.format(Locale.ROOT, "%.${decimals}f", value)
    return if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
}

/** 两侧数值在「显示精度」上相同就算相同，避免 0.10001 与 0.1 被算成一高一低。 */
internal fun CompareRow.sameAtDisplayPrecision(): Boolean {
    val l = left ?: return false
    val r = right ?: return false
    return abs(l - r) <= maxOf(0.0005, maxOf(l, r) * 0.001)
}

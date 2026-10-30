package com.woshiwangnima.healthdietpro.model.price

import java.util.Locale

/**
 * 同一物品在一个「物价单位 + 售卖单位」下的价格区间。
 *
 * 为什么不是「一个物品一个区间」：土豆 3.2 元/斤 和 1.5 元/个 是两个不可比的数，
 * 硬合成 1.5–3.2 会得出一个既不是斤价也不是个价的假数字。所以统计卡里按单位分行，
 * 每行各自是一个可信区间；不做任何基准单位换算（缺密度、缺规格，换算必然是猜的）。
 */
internal data class PriceUnitRange(
    val priceUnit: String,
    val sellUnit: String,
    val low: Double,
    val high: Double,
    /** 这个单位下累计记了多少条原始记录。 */
    val recordCount: Int,
) {
    val rangeText: String get() = formatPriceRange(low, high)
    val unitText: String get() = priceUnit + "/" + sellUnit
    val display: String get() = rangeText + " " + unitText
}

/**
 * 【统计】页的一行：**只按物品聚合**，跨地区、跨日期、跨单位全部收在同一个物品下。
 *
 * 与【记录】页的关系：记录页的卡片按「物品 + 地区 + 日期 + 单位」分开，
 * 是原始记录；这里把它们的区间再按物品收口，回答「这东西我买过多少钱」。
 * 两边用的是同一个合并函数 [groupByPrice]，所以记录页看到的区间与统计页的分行能一一对上。
 */
internal data class PriceItemStats(
    /** 物品名归一化后的键（小写去空格）。同一个名字的手输入和库内物品算同一种物品。 */
    val itemKey: String,
    val itemName: String,
    /** 按记录条数倒序；第一条是主单位，常见情况只有一条。 */
    val ranges: List<PriceUnitRange>,
    val recordCount: Int,
    /** 出现过的不重复地区数；没填地区的记录不计入。 */
    val regionCount: Int,
    /** 最近一次记录的日期，用于排序与展示。 */
    val latestAt: Long,
) {
    val primary: PriceUnitRange get() = ranges.first()
    val extraRangeCount: Int get() = (ranges.size - 1).coerceAtLeast(0)
}

/**
 * 按物品聚合价格区间。
 *
 * 物品键取**名称**而不是 id：用户可能一次从食材库选「土豆」、一次手输「土豆」，
 * 在统计里它们是同一种东西；反过来，不同来源却同名的东西在实际使用中几乎不存在，
 * 用名称归并更贴近用户对「同种物品」的理解。
 */
internal fun List<PriceRecord>.statsByItem(): List<PriceItemStats> {
    if (isEmpty()) return emptyList()
    return groupByPrice()
        .groupBy { it.itemName.trim().lowercase(Locale.ROOT) }
        .map { (itemKey, itemGroups) ->
            val ranges = itemGroups
                .groupBy { it.priceUnit.trim() to it.sellUnit.trim() }
                .map { (unit, groups) ->
                    PriceUnitRange(
                        priceUnit = unit.first,
                        sellUnit = unit.second,
                        low = groups.minOf { it.priceLow },
                        high = groups.maxOf { it.priceHigh },
                        recordCount = groups.sumOf { it.count },
                    )
                }
                .sortedByDescending { it.recordCount }
            PriceItemStats(
                itemKey = itemKey,
                itemName = itemGroups.first().itemName.trim(),
                ranges = ranges,
                recordCount = itemGroups.sumOf { it.count },
                regionCount = itemGroups.map { it.regionDisplay }
                    .filter(String::isNotBlank)
                    .distinct()
                    .size,
                latestAt = itemGroups.maxOf { it.recordedAt },
            )
        }
        // 最近记过的物品排前面：统计页的常见用法是「我昨天刚买的那东西大概多少钱」。
        .sortedWith(
            compareByDescending<PriceItemStats> { it.latestAt }
                .thenByDescending { it.recordCount },
        )
}

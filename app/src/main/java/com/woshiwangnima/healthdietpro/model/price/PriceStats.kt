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
    /** 按记录条数倒序：常用的卖法排前面，单位多的物品不用先找一圈。全部单位都会渲染，不折叠。 */
    val ranges: List<PriceUnitRange>,
    val recordCount: Int,
    /** 出现过的不重复地区数；没填地区的记录不计入。 */
    val regionCount: Int,
    /** 最近一次记录的日期，用于排序与展示。 */
    val latestAt: Long,
)

/**
 * 物品键：名称去首尾空格转小写。
 *
 * 取**名称**而不是 id：用户可能一次从食材库选「土豆」、一次手输「土豆」，
 * 在统计里它们是同一种东西；反过来，不同来源却同名的东西在实际使用中几乎不存在，
 * 用名称归并更贴近用户对「同种物品」的理解。
 *
 * 抽成函数是因为【统计】页与【物品历史】页要**用同一把尺子**认物品：
 * 统计行拿这个键往后传，历史页拿这个键往前筛，两边各写一遍大小写规则迟早会漏一处。
 */
internal fun priceItemKey(name: String): String = name.trim().lowercase(Locale.ROOT)

/**
 * 按物品聚合价格区间。
 *
 * 物品键见 [priceItemKey]。
 */
internal fun List<PriceRecord>.statsByItem(): List<PriceItemStats> {
    if (isEmpty()) return emptyList()
    return groupByPrice()
        .groupBy { priceItemKey(it.itemName) }
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

/** 走势图上的一个点：某一天、某个售卖单位下的价格区间。 */
internal data class PriceHistoryPoint(
    /** 记录日期（当天 00:00 的时间戳），同时是横轴刻度。 */
    val recordedAt: Long,
    val low: Double,
    val high: Double,
    /** 这一天、这个单位下合并了几条原始记录（同一天多地各记一次时 > 1）。 */
    val sampleCount: Int = 1,
    /** 这一天出现过的不重复地区（保持记录顺序，空白已剔除），供点按浮动窗展示。 */
    val regions: List<String> = emptyList(),
)

/**
 * 走势图上的一条线：一个「物价单位 + 售卖单位」下的价格随日期变化。
 *
 * 单位不进同一个系列：元/斤和元/个的点放在一条线上会把两个不可比的数连起来，
 * 看上去像「涨了」，实际只是换了个卖法（与统计行分行的理由相同）。
 */
internal data class PriceHistorySeries(
    val priceUnit: String,
    val sellUnit: String,
    val points: List<PriceHistoryPoint>,
    /** 汇成这条线的原始记录条数，用来排顺序，也让图例能说清样本量。 */
    val recordCount: Int,
) {
    val label: String get() = priceUnit + "/" + sellUnit
}

/**
 * 把同一物品的记录收口成走势系列。
 *
 * 同一天同一单位可能有多地记录（北京 6.0、上海 6.5）：这一天画一个区间 6.0–6.5，
 * 而不是两条各自成线——横轴是日期，同一天出现两个点会让折线来回折。
 * 收口只取包络，**不做任何单位换算**。
 */
internal fun List<PriceRecord>.historySeries(): List<PriceHistorySeries> =
    groupBy { it.priceUnit.trim() to it.sellUnit.trim() }
        .map { (unit, records) ->
            PriceHistorySeries(
                priceUnit = unit.first,
                sellUnit = unit.second,
                points = records.groupBy(PriceRecord::recordedAt)
                    .map { (day, sameDay) ->
                        PriceHistoryPoint(
                            recordedAt = day,
                            low = sameDay.minOf(PriceRecord::priceLow),
                            high = sameDay.maxOf(PriceRecord::priceHigh),
                            sampleCount = sameDay.size,
                            // 点被多地记录合并过，浮动窗就要说清「这个点是哪几个地方的价」，
                            // 否则用户点开只看到一个数，会以为图上漏掉了另一条记录。
                            regions = sameDay.map(PriceRecord::regionDisplay)
                                .filter(String::isNotBlank)
                                .distinct(),
                        )
                    }
                    .sortedBy(PriceHistoryPoint::recordedAt),
                recordCount = records.size,
            )
        }
        // 与统计行的单位顺序一致：记录多的卖法排前面。
        .sortedWith(
            compareByDescending<PriceHistorySeries> { it.recordCount }
                .thenBy { it.label },
        )

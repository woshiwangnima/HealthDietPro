package com.woshiwangnima.healthdietpro.model.price

import kotlinx.serialization.Serializable

/**
 * 物价记录里的物品来源。
 *
 * 用户既可以挂到库内已有食材/菜肴上（[INGREDIENT] / [FOOD] / [DISH]，保存 `id` 便于以后关联查询），
 * 也可以只写一个名字（[CUSTOM]）。无论哪种，都冗余保存当时的显示名：
 * 食材库更新或自定义食材被删掉后，历史物价不应跟着消失或改名。
 */
@Serializable
internal enum class PriceItemSource {
    INGREDIENT,
    FOOD,
    DISH,
    CUSTOM,
}

/**
 * 一条物价记录（记物价）。
 *
 * **为什么不是「物品 + 地区 + 日期」唯一**
 * 同一件物品在不同地区、不同时间当然价格不同，这在同一个键里已经区分开了；
 * 但**同一天、同一地区、同一物品完全可能同时存在「元/斤」和「元/个」两条**——
 * 比如土豆按斤卖 3.2 元、也按个卖 1.5 元。所以 [sellUnit]（和 [priceUnit]）也是身份的一部分。
 * 组合起来仍然撞车时（同天同地同物品同单位记了两次），两条不会被覆盖，而是合并成一条价格区间：
 * 一天之内价格波动本身就是要记录的，用户看卡片时只需要知道「大概多少钱」。
 *
 * 数值约定：
 * - [price] 是**该售卖单位的单价下限**，不再做任何基准单位换算（币种由 [priceUnit] 决定）；
 * - [priceMax] 只在用户填了上限时存在，表示同一条件下见过的最高价；
 * - [recordedAt] 精确到日（当天 00:00），因为物价没有分钟级意义。
 *
 * **为什么价格有下限和上限**：菜场同一摊位的同一种菜，早上和傍晚报价就可能差三成；
 * 只记一个数会丢掉这个信息。用户既可以只填一个数（[priceMax] 为空，等价于单值），
 * 也可以填「3.2–4.5」。列表页再把**同物品 + 同地区 + 同日期 + 同单位**的多条记录
 * 合并成一条区间卡片（见 [groupByPrice]）。
 */
@Serializable
internal data class PriceRecord(
    val id: String,
    val itemSource: PriceItemSource = PriceItemSource.CUSTOM,
    /** 库内物品 id；[PriceItemSource.CUSTOM] 时为空。 */
    val itemId: String = "",
    /** 记录当时展示给用户的物品名，**始终有值**。 */
    val itemName: String,
    /** 单价下限；单值记录时即唯一价格。 */
    val price: Double,
    /** 单价上限；为空表示单值。老归档没有该字段，反序列化后即为 null。 */
    val priceMax: Double? = null,
    /** 物价单位（币种），如 元 / 角 / 美元。 */
    val priceUnit: String = "元",
    /** 售卖单位，如 斤 / 公斤 / 个 / 盒 / 500 克。 */
    val sellUnit: String,
    val provinceCode: String = "",
    val provinceName: String = "",
    val cityCode: String = "",
    val cityName: String = "",
    val districtCode: String = "",
    val districtName: String = "",
    val lng: Double = 0.0,
    val lat: Double = 0.0,
    /** 记录日期，当天 00:00 的时间戳。 */
    val recordedAt: Long,
    val note: String = "",
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    /** 省 - 市 - 区，空段省略；用于列表与卡片展示。 */
    val regionDisplay: String
        get() = listOf(provinceName, cityName, districtName)
            .filter(String::isNotBlank)
            .joinToString(" - ")

    /** 下限，单值记录时等于 [priceHigh]。 */
    val priceLow: Double
        get() = price

    /** 上限；只填了一个数时与下限相同。 */
    val priceHigh: Double
        get() = priceMax?.takeIf { it >= price } ?: price

    /** 是否真的是一段区间（上下限不同）。 */
    val isRange: Boolean
        get() = priceHigh > priceLow

    /** 「3.2 元/斤」或「3.2–4.5 元/斤」。 */
    val priceDisplay: String
        get() = priceRangeText + " " + priceUnit + "/" + sellUnit

    /** 只有数值区间，用于把单位单独排版时。 */
    val priceRangeText: String
        get() = formatPriceRange(price, priceMax)

    /**
     * 身份键：物品 + 地区 + 日期 + 售卖单位 + 物价单位。
     * 键相同的多条记录在列表里合并为一条价格区间（见 [groupByPrice]），
     * 所以键里刻意不包含价格本身。
     */
    val identityKey: String
        get() = priceIdentityKey(
            itemSource = itemSource,
            itemId = itemId,
            itemName = itemName,
            provinceCode = provinceCode,
            cityCode = cityCode,
            districtCode = districtCode,
            recordedAt = recordedAt,
            sellUnit = sellUnit,
            priceUnit = priceUnit,
        )
}

/**
 * 构造身份键。编辑页在记录还没填完（价格为空、无法生成 [PriceRecord]）时，
 * 也要按当前已填的地区 / 日期 / 单位去查同组记录，所以键的拼装单独抽出来。
 */
internal fun priceIdentityKey(
    itemSource: PriceItemSource,
    itemId: String,
    itemName: String,
    provinceCode: String,
    cityCode: String,
    districtCode: String,
    recordedAt: Long,
    sellUnit: String,
    priceUnit: String,
): String = listOf(
    itemSource.name,
    itemId.ifBlank { itemName.trim() },
    provinceCode,
    cityCode,
    districtCode,
    recordedAt.toString(),
    sellUnit.trim(),
    priceUnit.trim(),
).joinToString("\u0000")

/**
 * 同身份键的多条记录合成一个区间：下限取所有记录下限的最小值，上限取所有记录上限的最大值。
 *
 * 例：同一天同一地区同一摊位的土豆，早上记 3.2、傍晚记 4.0，列表里是一条「3.2–4.0 元/斤」，
 * 而不是两行互相矛盾的数字——用户要看的本来就是「这天这儿大概多少钱」。
 */
internal data class PriceGroup(
    val key: String,
    /** 组内记录，按最近更新倒序；[latest] 是代表记录（编辑 / 删除入口）。 */
    val records: List<PriceRecord>,
) {
    init {
        require(records.isNotEmpty()) { "PriceGroup requires at least one record" }
    }

    val latest: PriceRecord
        get() = records.first()

    val count: Int
        get() = records.size

    val itemName: String get() = latest.itemName
    val regionDisplay: String get() = latest.regionDisplay
    val recordedAt: Long get() = latest.recordedAt
    val priceUnit: String get() = latest.priceUnit
    val sellUnit: String get() = latest.sellUnit
    val note: String get() = records.firstOrNull { it.note.isNotBlank() }?.note.orEmpty()

    val priceLow: Double get() = records.minOf { it.priceLow }
    val priceHigh: Double get() = records.maxOf { it.priceHigh }
    val isRange: Boolean get() = priceHigh > priceLow

    /** 合并后的价格文本，如「3.2–4.5 元/斤」。 */
    val priceDisplay: String get() = priceRangeText + " " + priceUnit + "/" + sellUnit

    val priceRangeText: String get() = formatPriceRange(priceLow, priceHigh)
}

/** 按身份键合并成区间卡片；越新的日期排在前面。 */
internal fun List<PriceRecord>.groupByPrice(): List<PriceGroup> =
    groupBy(PriceRecord::identityKey)
        .map { (key, group) -> PriceGroup(key, group.sortedByDescending(PriceRecord::updatedAtMillis)) }
        .sortedWith(
            compareByDescending<PriceGroup> { it.recordedAt }
                .thenByDescending { it.latest.updatedAtMillis },
        )

/** 「3.2」或「3.2–4.5」；上限不大于下限时退化为单值。 */
internal fun formatPriceRange(low: Double, high: Double?): String {
    val upper = high?.takeIf { it > low } ?: return formatPriceValue(low)
    return formatPriceValue(low) + "–" + formatPriceValue(upper)
}

/** 去掉多余的 0：3.20 → 3.2，12.00 → 12。 */
internal fun formatPriceValue(value: Double): String {
    val rounded = Math.round(value * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format("%.2f", rounded).trimEnd('0').trimEnd('.')
}

@Serializable
internal data class PriceArchive(
    val schemaVersion: Int = PRICE_ARCHIVE_SCHEMA_VERSION,
    val records: List<PriceRecord> = emptyList(),
)

internal const val PRICE_ARCHIVE_SCHEMA_VERSION = 1

internal fun validatePriceArchive(archive: PriceArchive) {
    require(archive.schemaVersion == PRICE_ARCHIVE_SCHEMA_VERSION) { "Unsupported price archive schema" }
    val ids = archive.records.map(PriceRecord::id)
    require(ids.all(String::isNotBlank) && ids.distinct().size == ids.size) { "Invalid price record ids" }
    require(archive.records.all {
        it.itemName.isNotBlank() && it.price > 0.0 && it.sellUnit.isNotBlank() && it.priceUnit.isNotBlank() &&
            (it.priceMax == null || it.priceMax >= it.price)
    }) { "Invalid price record" }
}

/**
 * 同组记录：身份键相同、但不是自己。
 *
 * 以前这里叫「重复记录」并弹窗拦截，现在同键多条会被合并成一条价格区间，
 * 属于正常录入（早上一个价、傍晚一个价），所以只用来在编辑页提示「会合并成什么范围」，不阻断保存。
 */
internal fun PriceArchive.findPeers(candidate: PriceRecord): List<PriceRecord> =
    records.filter { it.id != candidate.id && it.identityKey == candidate.identityKey }

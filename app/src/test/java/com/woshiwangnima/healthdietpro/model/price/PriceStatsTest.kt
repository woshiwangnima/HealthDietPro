package com.woshiwangnima.healthdietpro.model.price

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 【统计】页的聚合口径：
 * 只按物品收口，跨地区 / 跨日期全部合并；不同售卖单位的价格不可比，必须分行。
 */
class PriceStatsTest {

    private fun record(
        id: String = "1",
        itemName: String = "土豆",
        sellUnit: String = "斤",
        priceUnit: String = "元",
        province: String = "11",
        city: String = "1101",
        district: String = "110101",
        provinceName: String = "北京",
        cityName: String = "北京市",
        districtName: String = "朝阳区",
        day: Long = 1_760_000_000_000L,
        price: Double = 3.2,
        priceMax: Double? = null,
    ) = PriceRecord(
        id = id,
        itemSource = PriceItemSource.CUSTOM,
        itemName = itemName,
        price = price,
        priceMax = priceMax,
        priceUnit = priceUnit,
        sellUnit = sellUnit,
        provinceCode = province,
        provinceName = provinceName,
        cityCode = city,
        cityName = cityName,
        districtCode = district,
        districtName = districtName,
        recordedAt = day,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun sameItemAcrossRegionsAndDaysCollapsesIntoOneStat() {
        val beijing = record(id = "a", district = "110101", districtName = "朝阳区", day = 1_760_000_000_000L, price = 3.2)
        val shanghai = record(
            id = "b",
            province = "31",
            city = "3101",
            district = "310101",
            provinceName = "上海",
            cityName = "上海市",
            districtName = "浦东新区",
            day = 1_760_100_000_000L,
            price = 4.5,
            priceMax = 5.0,
        )

        val stats = listOf(beijing, shanghai).statsByItem()

        assertEquals(1, stats.size)
        assertEquals(2, stats.single().recordCount)
        assertEquals(2, stats.single().regionCount)
        assertEquals(3.2, stats.single().primary.low, 0.0001)
        assertEquals(5.0, stats.single().primary.high, 0.0001)
        assertEquals("3.2–5 元/斤", stats.single().primary.display)
    }

    @Test
    fun differentSellingUnitsStayOnSeparateLines() {
        // 土豆 3.2 元/斤 + 1.5 元/个：不能合成 1.5–3.2 这种不存在的数。
        val byJin = record(id = "a", sellUnit = "斤", price = 3.2)
        val byPiece = record(id = "b", sellUnit = "个", price = 1.5)

        val stat = listOf(byJin, byPiece).statsByItem().single()

        assertEquals(2, stat.ranges.size)
        assertEquals(setOf("斤", "个"), stat.ranges.map { it.sellUnit }.toSet())
        assertTrue(stat.ranges.all { it.low == it.high })
    }

    @Test
    fun differentPriceUnitsAreAlsoSeparate() {
        val yuan = record(id = "a", priceUnit = "元", price = 3.2)
        val usd = record(id = "b", priceUnit = "USD", price = 0.5)

        val stat = listOf(yuan, usd).statsByItem().single()

        assertEquals(2, stat.ranges.size)
    }

    @Test
    fun sameConditionEntriesCountAsMultipleRecordsButOneRange() {
        val morning = record(id = "a", price = 3.2)
        val evening = record(id = "b", price = 4.0)

        val stat = listOf(morning, evening).statsByItem().single()

        assertEquals(2, stat.recordCount)
        assertEquals(1, stat.ranges.size)
        assertEquals("3.2–4 元/斤", stat.primary.display)
    }

    @Test
    fun nameVariantsMergeIntoOneItem() {
        // 手输「土豆 」和库内「土豆」在用户眼里是同一个东西。
        val manual = record(id = "a", itemName = " 土豆 ")
        val fromLibrary = record(id = "b", itemName = "土豆")

        assertEquals(1, listOf(manual, fromLibrary).statsByItem().size)
    }

    @Test
    fun differentItemsStaySeparate() {
        val potato = record(id = "a", itemName = "土豆")
        val tomato = record(id = "b", itemName = "西红柿")

        assertEquals(2, listOf(potato, tomato).statsByItem().size)
    }

    @Test
    fun mostRecentlyRecordedItemComesFirst() {
        val old = record(id = "a", itemName = "土豆", day = 1_760_000_000_000L)
        val fresh = record(id = "b", itemName = "西红柿", day = 1_760_900_000_000L)

        val stats = listOf(old, fresh).statsByItem()

        assertEquals("西红柿", stats.first().itemName)
    }

    @Test
    fun blankRegionIsNotCounted() {
        val noRegion = record(
            id = "a",
            province = "",
            city = "",
            district = "",
            provinceName = "",
            cityName = "",
            districtName = "",
        )
        val withRegion = record(id = "b")

        assertEquals(1, listOf(noRegion, withRegion).statsByItem().single().regionCount)
    }

    @Test
    fun emptyInputProducesNoStats() {
        assertTrue(emptyList<PriceRecord>().statsByItem().isEmpty())
    }
}

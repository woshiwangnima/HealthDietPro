package com.woshiwangnima.healthdietpro.model.price

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 【物品历史】的走势口径：
 * 同一售卖单位一条线、同一天多地记录取包络、点按日期升序；不同单位绝不连线。
 */
class PriceHistoryTest {

    private fun record(
        id: String = "1",
        itemName: String = "土豆",
        sellUnit: String = "斤",
        priceUnit: String = "元",
        district: String = "110101",
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
        provinceCode = "11",
        provinceName = "北京",
        cityCode = "1101",
        cityName = "北京市",
        districtCode = district,
        districtName = districtName,
        recordedAt = day,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun sameUnitSameDayAcrossRegionsBecomesOneRange() {
        // 同一天北京 6.0、上海 6.5：图上这一天只能有一个点，否则折线会在原地来回折。
        val beijing = record(id = "a", price = 6.0)
        val shanghai = record(id = "b", district = "310101", districtName = "浦东新区", price = 6.5)

        val series = listOf(beijing, shanghai).historySeries().single()

        assertEquals(1, series.points.size)
        assertEquals(6.0, series.points.single().low, 0.0001)
        assertEquals(6.5, series.points.single().high, 0.0001)
        assertEquals(2, series.recordCount)
    }

    @Test
    fun sameDayPointKeepsTheRegionsItMerged() {
        // 点按浮动窗要说清「这个点是哪几个地方的价」：合并过就要留痕，否则用户以为漏了一条。
        val beijing = record(id = "a", price = 6.0)
        val shanghai = record(id = "b", district = "310101", districtName = "浦东新区", price = 6.5)

        val point = listOf(beijing, shanghai).historySeries().single().points.single()

        assertEquals(2, point.sampleCount)
        assertEquals(
            listOf("北京 - 北京市 - 朝阳区", "北京 - 北京市 - 浦东新区"),
            point.regions,
        )
    }

    @Test
    fun singleRecordPointHasNoExtraRegions() {
        val point = listOf(record()).historySeries().single().points.single()

        assertEquals(1, point.sampleCount)
        assertEquals(1, point.regions.size)
    }

    @Test
    fun pointsAreSortedByDateAscending() {
        val newer = record(id = "a", day = 1_760_900_000_000L, price = 5.0)
        val older = record(id = "b", district = "310101", districtName = "浦东新区", day = 1_760_000_000_000L, price = 3.2)

        val points = listOf(newer, older).historySeries().single().points

        assertEquals(listOf(1_760_000_000_000L, 1_760_900_000_000L), points.map { it.recordedAt })
    }

    @Test
    fun differentSellingUnitsNeverShareALine() {
        // 元/斤和元/个没有换算依据，画成一条线等于编造趋势。
        val byJin = record(id = "a", sellUnit = "斤", price = 3.2)
        val byPiece = record(id = "b", district = "310101", districtName = "浦东新区", sellUnit = "个", price = 1.5)

        val series = listOf(byJin, byPiece).historySeries()

        assertEquals(2, series.size)
        assertEquals(setOf("元/斤", "元/个"), series.map { it.label }.toSet())
        assertTrue(series.all { it.points.size == 1 })
    }

    @Test
    fun busiestUnitComesFirst() {
        val jin = listOf(
            record(id = "a", sellUnit = "斤", day = 1_760_000_000_000L),
            record(id = "b", sellUnit = "斤", district = "310101", districtName = "浦东新区", day = 1_760_900_000_000L),
        )
        val piece = record(id = "c", sellUnit = "个", price = 1.5)

        assertEquals("元/斤", (jin + piece).historySeries().first().label)
    }

    @Test
    fun emptyInputProducesNoSeries() {
        assertTrue(emptyList<PriceRecord>().historySeries().isEmpty())
    }

    @Test
    fun itemKeyIgnoresCaseAndSpaces() {
        // 统计页与历史页必须用同一把尺子认物品，否则点进去会是空的。
        assertEquals(priceItemKey("土豆"), priceItemKey(" 土豆 "))
        assertEquals(priceItemKey("Apple"), priceItemKey("apple "))
        assertTrue(priceItemKey("  ") == "")
    }
}

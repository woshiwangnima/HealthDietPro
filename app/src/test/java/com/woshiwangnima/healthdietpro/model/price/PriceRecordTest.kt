package com.woshiwangnima.healthdietpro.model.price

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 记物价的身份判定与区间合并：
 * 同一物品在不同地区、不同日期必然是不同记录；同一天同一地区也必须能按不同售卖单位各记一条；
 * 而「同物品 + 同地区 + 同日期 + 同单位」的多条会被合并成一条价格区间，不再互相覆盖。
 */
class PriceRecordTest {

    private fun record(
        id: String = "1",
        itemName: String = "土豆",
        sellUnit: String = "斤",
        priceUnit: String = "元",
        province: String = "11",
        city: String = "1101",
        district: String = "110101",
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
        cityCode = city,
        districtCode = district,
        recordedAt = day,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    @Test
    fun sameItemDifferentSellUnitIsNotPeer() {
        val byJin = record(id = "a", sellUnit = "斤")
        val byPiece = record(id = "b", sellUnit = "个")
        assertTrue(byJin.identityKey != byPiece.identityKey)
        assertTrue(PriceArchive(records = listOf(byJin)).findPeers(byPiece).isEmpty())
    }

    @Test
    fun sameItemSameUnitButDifferentDayIsNotPeer() {
        val today = record(id = "a", day = 1_760_000_000_000L)
        val tomorrow = record(id = "b", day = 1_760_000_000_000L + 86_400_000L)
        assertTrue(PriceArchive(records = listOf(today)).findPeers(tomorrow).isEmpty())
    }

    @Test
    fun sameItemSameUnitButDifferentRegionIsNotPeer() {
        val beijing = record(id = "a", district = "110101")
        val shanghai = record(id = "b", district = "310101")
        assertTrue(PriceArchive(records = listOf(beijing)).findPeers(shanghai).isEmpty())
    }

    @Test
    fun identicalConditionsShareIdentityKey() {
        val first = record(id = "a")
        val second = record(id = "b", price = 4.0)
        assertEquals(first.identityKey, second.identityKey)
        assertEquals(listOf(first), PriceArchive(records = listOf(first)).findPeers(second))
    }

    @Test
    fun editingSameRecordIsNotPeerOfItself() {
        val only = record(id = "a")
        assertTrue(PriceArchive(records = listOf(only)).findPeers(only.copy(price = 4.5)).isEmpty())
    }

    @Test
    fun priceDisplayJoinsPriceUnitAndSellUnit() {
        assertEquals("3.2 元/斤", record(sellUnit = "斤").priceDisplay)
    }

    @Test
    fun priceDisplayShowsRangeWhenMaxIsSet() {
        assertEquals("3.2–4.5 元/斤", record(price = 3.2, priceMax = 4.5).priceDisplay)
        assertTrue(record(price = 3.2, priceMax = 4.5).isRange)
        assertFalse(record(price = 3.2).isRange)
    }

    @Test
    fun priceRangeFallsBackToSingleValueWhenMaxIsNotGreater() {
        // 上下限相等、或上限被写反，都不该显示成「3.2–3.2」或「4.5–3.2」。
        assertEquals("3.2", formatPriceRange(3.2, 3.2))
        assertEquals("3.2", formatPriceRange(3.2, 2.0))
        assertEquals("3.2", formatPriceRange(3.2, null))
        assertEquals("3.2–4.5", formatPriceRange(3.2, 4.5))
    }

    @Test
    fun priceValueDropsTrailingZeros() {
        assertEquals("12", formatPriceValue(12.0))
        assertEquals("3.2", formatPriceValue(3.20))
        assertEquals("3.25", formatPriceValue(3.25))
    }

    @Test
    fun archiveRejectsBlankItemName() {
        val invalid = PriceArchive(records = listOf(record().copy(itemName = " ")))
        val failure = runCatching { validatePriceArchive(invalid) }
        assertTrue(failure.isFailure)
    }

    @Test
    fun archiveRejectsMaxBelowMin() {
        val invalid = PriceArchive(records = listOf(record(price = 4.0, priceMax = 3.0)))
        assertTrue(runCatching { validatePriceArchive(invalid) }.isFailure)
        assertTrue(runCatching { validatePriceArchive(PriceArchive(records = listOf(record(price = 4.0, priceMax = 5.0)))) }.isSuccess)
    }

    @Test
    fun sameConditionRecordsMergeIntoOneRange() {
        val morning = record(id = "a", price = 4.0)
        val evening = record(id = "b", price = 3.2)
        val groups = listOf(morning, evening).groupByPrice()

        assertEquals(1, groups.size)
        assertEquals(2, groups.single().count)
        // 4.0 会去掉尾随 0，与单价的格式化规则保持一致。
        assertEquals("3.2–4 元/斤", groups.single().priceDisplay)
    }

    @Test
    fun mergeKeepsSeparateGroupsForDifferentDayOrUnit() {
        val today = record(id = "a", day = 1_760_000_000_000L)
        val tomorrow = record(id = "b", day = 1_760_000_000_000L + 86_400_000L)
        val byPiece = record(id = "c", sellUnit = "个")

        assertEquals(3, listOf(today, tomorrow, byPiece).groupByPrice().size)
    }

    @Test
    fun rangeRecordsMergeByOuterBounds() {
        // 早上 3.2–4.0、傍晚 3.8–4.5：合并后应是 3.2–4.5，而不是把 4 条数值拼起来。
        val morning = record(id = "a", price = 3.2, priceMax = 4.0)
        val evening = record(id = "b", price = 3.8, priceMax = 4.5)
        val group = listOf(morning, evening).groupByPrice().single()

        assertEquals(3.2, group.priceLow, 0.0001)
        assertEquals(4.5, group.priceHigh, 0.0001)
        assertEquals("3.2–4.5 元/斤", group.priceDisplay)
    }
}

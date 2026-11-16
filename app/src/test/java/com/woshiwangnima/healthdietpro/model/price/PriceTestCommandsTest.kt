package com.woshiwangnima.healthdietpro.model.price

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceTestCommandsTest {

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val now = 1_700_000_000_000L

    @Test
    fun recordsRespectCountItemsRegionsAndUnits() {
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(
                count = 40,
                itemCount = 3,
                regionCount = 2,
                sellUnits = listOf("斤", "个"),
                recentDays = 10,
                rangePercent = 100,
                peersPerKey = 1,
                notePercent = 0,
            ),
            libraryItems = emptyList(),
            now = now,
            zone = zone,
            random = Random(3),
        )
        assertEquals(40, records.size)
        assertEquals(3, records.map(PriceRecord::itemName).distinct().size)
        assertTrue(records.all { it.itemSource == PriceItemSource.CUSTOM && it.itemId.isEmpty() })
        // 只用前两个地区：第三个地区不该在这批数据里出现。
        val usedDistricts = records.map(PriceRecord::districtCode).distinct()
        assertEquals(PRICE_REGION_POOL.take(2).map { it.districtCode }.toSet(), usedDistricts.toSet())
        assertTrue(records.all { it.sellUnit in setOf("斤", "个") })
        // 100% 区间 → 每条都带上限，且上限不小于下限。
        assertTrue(records.all { it.priceMax != null && it.priceMax!! >= it.price })
        assertTrue(records.all { it.note.isEmpty() })
        // 日期都落在最近 10 天的日界上。
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val earliest = today.minusDays(9).atStartOfDay(zone).toInstant().toEpochMilli()
        assertTrue(records.all { it.recordedAt in earliest..now })
        assertTrue(
            records.all { record ->
                Instant.ofEpochMilli(record.recordedAt).atZone(zone).toLocalTime().toSecondOfDay() == 0
            },
        )
    }

    @Test
    fun recordsAreValidArchivePayload() {
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(count = 50, itemCount = 4, regionCount = 3, peersPerKey = 3),
            libraryItems = emptyList(),
            now = now,
            zone = zone,
            random = Random(11),
        )
        assertEquals(records.size, records.map(PriceRecord::id).distinct().size)
        // 直接过归档校验：造出来的数据必须能落盘，否则测试指令会把存档写坏。
        validatePriceArchive(PriceArchive(records = records))
    }

    @Test
    fun peerGroupsMergeIntoRangeCards() {
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(
                count = 18,
                itemCount = 1,
                regionCount = 1,
                sellUnits = listOf("斤"),
                recentDays = 20,
                rangePercent = 0,
                peersPerKey = 3,
                notePercent = 0,
            ),
            libraryItems = emptyList(),
            now = now,
            zone = zone,
            random = Random(7),
        )
        assertEquals(18, records.size)
        val groups = records.groupByPrice()
        // 合并成卡之后条数一定变少：同键的多条被收成一条区间卡。
        assertTrue("expected merged groups, got ${groups.size}", groups.size < records.size)
        assertEquals(records.size, groups.sumOf(PriceGroup::count))
        assertTrue(groups.any { it.count > 1 })
        assertTrue(groups.all { it.priceHigh >= it.priceLow })
        // 同组内地区 / 物品 / 日期 / 单位一致（身份键一致才是合并的前提）。
        assertTrue(groups.all { group -> group.records.map(PriceRecord::identityKey).distinct().size == 1 })
    }

    @Test
    fun zeroRegionKeepsRecordsWithoutRegion() {
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(count = 6, itemCount = 2, regionCount = 0),
            libraryItems = emptyList(),
            now = now,
            zone = zone,
            random = Random(5),
        )
        assertTrue(records.all { it.regionDisplay.isEmpty() && it.lng == 0.0 && it.lat == 0.0 })
    }

    @Test
    fun attachToLibraryUsesLibraryNamesAndSources() {
        val library = listOf(
            PriceFoodRef("food:apple", "苹果", PriceItemSource.INGREDIENT),
            PriceFoodRef("food:rice", "米饭", PriceItemSource.FOOD),
            PriceFoodRef("food:salad", "沙拉", PriceItemSource.DISH),
        )
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(count = 9, itemCount = 3, regionCount = 1, attachToLibrary = true),
            libraryItems = library,
            now = now,
            zone = zone,
            random = Random(13),
        )
        assertEquals(library.map(PriceFoodRef::name).toSet(), records.map(PriceRecord::itemName).toSet())
        assertTrue(records.all { it.itemId.isNotBlank() && it.itemSource != PriceItemSource.CUSTOM })
    }

    @Test
    fun attachToLibraryDegradesWhenLibraryIsEmpty() {
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(count = 5, itemCount = 2, attachToLibrary = true),
            libraryItems = emptyList(),
            now = now,
            zone = zone,
            random = Random(17),
        )
        assertEquals(5, records.size)
        assertTrue(records.all { it.itemSource == PriceItemSource.CUSTOM && it.itemId.isEmpty() })
    }

    @Test
    fun trendIsOneRecordPerDayAndContinuous() {
        val records = buildPriceTrend(
            spec = PriceTrendSpec(itemName = "测试走势物品", days = 15, basePrice = 6.5, regionCount = 1),
            now = now,
            zone = zone,
            random = Random(19),
        )
        assertEquals(15, records.size)
        assertTrue(records.all { it.itemName == "测试走势物品" && it.sellUnit == "斤" })
        assertTrue(records.all { it.price > 0.0 })
        val days = records.map(PriceRecord::recordedAt).distinct()
        assertEquals(15, days.size)
        // 逐日一条：相邻两天的间隔正好是一天。
        val sorted = days.sorted()
        sorted.zipWithNext().forEach { (earlier, later) ->
            assertEquals(24L * 60 * 60 * 1000, later - earlier)
        }
    }

    @Test
    fun trendWithMultipleRegionsCollapsesToOnePointPerDay() {
        val records = buildPriceTrend(
            spec = PriceTrendSpec(itemName = "多地物品", days = 10, regionCount = 2),
            now = now,
            zone = zone,
            random = Random(23),
        )
        assertEquals(20, records.size)
        val series = records.historySeries()
        assertEquals(1, series.size)
        assertEquals(10, series.single().points.size)
        assertTrue(series.single().points.all { it.sampleCount == 2 && it.regions.size == 2 })
    }

    @Test
    fun zeroFluctuationKeepsTrendFlat() {
        val records = buildPriceTrend(
            spec = PriceTrendSpec(itemName = "平价物品", days = 6, basePrice = 4.0, fluctuationPercent = 0, regionCount = 0),
            now = now,
            zone = zone,
            random = Random(29),
        )
        // 波动率为 0 且不填地区时价格完全不动；`nextDouble(0.0, 0.0)` 的非法空区间已被挡掉。
        assertEquals(1, records.map(PriceRecord::priceLow).distinct().size)
        assertEquals(4.0, records.first().priceLow, 0.001)
        assertTrue(records.all { it.regionDisplay.isEmpty() })
    }

    @Test
    fun sellUnitInputIsSplitAndDeduped() {
        assertEquals(listOf("斤", "公斤", "个"), parseSellUnits("斤, 公斤,个"))
        assertEquals(listOf("斤", "个"), parseSellUnits("斤，个，斤"))
        // 空输入退回单个默认单位，不产生空字符串单位（归档校验会拒）。
        assertEquals(listOf("斤"), parseSellUnits("  "))
        // 最多四种：再多就超出价格的语义，统计行也会糊。
        assertEquals(4, parseSellUnits("斤,个,盒,袋,公斤").size)
    }

    @Test
    fun itemPoolExpandsBeyondKnownNames() {
        // 400 条才够让 20 个物品都被抽到：这里验的是「物品池不够时靠后缀补足」，不是抽样分布。
        val records = buildRandomPriceRecords(
            spec = RandomPriceSpec(count = 400, itemCount = 20, regionCount = 1),
            libraryItems = emptyList(),
            now = now,
            zone = zone,
            random = Random(31),
        )
        val names = records.map(PriceRecord::itemName).distinct()
        assertEquals(20, names.size)
        // 超出词池的部分靠序号后缀区分，不能出现重名（否则统计会把两种物品并成一种）。
        assertTrue(names.all { it.isNotBlank() })
        assertTrue(names.any { it.endsWith("2") })
    }
}

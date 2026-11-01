package com.woshiwangnima.healthdietpro.ui.price

import com.woshiwangnima.healthdietpro.model.price.PriceItemSource
import com.woshiwangnima.healthdietpro.model.price.PriceRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 【统计】页筛选的**点到即筛**：没有草稿层，也没有【开始查找】，
 * 四个格子改哪一个 [PriceUiState.filteredRecords] 立刻跟着变。
 */
class PriceFilterRealtimeTest {

    private fun at(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun record(
        id: String,
        itemName: String,
        provinceName: String,
        cityName: String,
        districtName: String,
        day: Long,
        price: Double = 3.2,
    ) = PriceRecord(
        id = id,
        itemSource = PriceItemSource.CUSTOM,
        itemName = itemName,
        price = price,
        priceMax = null,
        priceUnit = "元",
        sellUnit = "斤",
        provinceCode = "11",
        provinceName = provinceName,
        cityCode = "1101",
        cityName = cityName,
        districtCode = "110101",
        districtName = districtName,
        recordedAt = day,
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )

    private val records = listOf(
        record("a", "土豆", "北京市", "北京市", "朝阳区", at(2026, 3, 8)),
        record("b", "土豆", "上海市", "上海市", "浦东新区", at(2026, 3, 9)),
        record("c", "鸡蛋", "北京市", "北京市", "朝阳区", at(2025, 6, 1)),
    )

    private val beijing = "北京市 - 北京市 - 朝阳区"
    private val shanghai = "上海市 - 上海市 - 浦东新区"

    @Test
    fun noConditionMeansEveryRecord() {
        val state = PriceUiState(records = records)

        assertEquals(3, state.filteredRecords.size)
        assertFalse(state.hasFilters)
    }

    @Test
    fun pickingOneOptionFiltersImmediately() {
        // 只选商品，不点任何「应用」按钮：列表必须已经收窄。
        val state = PriceUiState(records = records, selectedItem = "土豆")

        assertEquals(listOf("b", "a"), state.filteredRecords.map { it.id })
        assertTrue(state.hasFilters)
    }

    @Test
    fun allFourConditionsStack() {
        val state = PriceUiState(records = records).copy(
            selectedItem = "土豆",
            selectedRegions = setOf(beijing),
            selectedYear = 2026,
            selectedMonth = 3,
        )

        assertEquals(listOf("a"), state.filteredRecords.map { it.id })
        assertTrue(state.hasFilters)
    }

    @Test
    fun togglingOffRegionFreesTheRecordAgain() {
        val filtered = PriceUiState(records = records, selectedRegions = setOf(beijing))
        val reverted = filtered.copy(selectedRegions = emptySet())

        // 多选勾掉又勾回来，不该留下任何「条件还在」的残留：列表必须回到全量。
        // 北京两条（土豆 a、鸡蛋 c），勾掉地区后回到全量三条。
        assertEquals(2, filtered.filteredRecords.size)
        assertEquals(3, reverted.filteredRecords.size)
        assertFalse(reverted.hasFilters)
    }

    @Test
    fun withoutFiltersResetsEveryCondition() {
        val cleared = PriceUiState(records = records)
            .copy(selectedItem = "土豆", selectedRegions = setOf(beijing))
            .copy(selectedYear = 2026)
            .withoutFilters()

        assertEquals(3, cleared.filteredRecords.size)
        assertFalse(cleared.hasFilters)
        assertNull(cleared.selectedYear)
    }

    @Test
    fun regionAndMonthFiltersStack() {
        val state = PriceUiState(records = records).copy(
            selectedRegions = setOf(beijing, shanghai),
            selectedMonth = 3,
        )

        // 两个地区 + 3 月 → 两条土豆，鸡蛋（2025-06）被月份挡掉。
        assertEquals(listOf("b", "a"), state.filteredRecords.map { it.id })
        assertEquals(1, state.visibleStats.size)
        assertEquals(2, state.visibleStats.single().recordCount)
    }

    @Test
    fun itemMatchingGoesThroughTheItemKey() {
        // 「土豆」和带空格的写法在统计里是同一个物品，筛选也必须用同一把尺子。
        val loose = records + record("d", "土豆 ", "北京市", "北京市", "朝阳区", at(2026, 3, 10))

        assertEquals(
            listOf("d", "b", "a"),
            PriceUiState(records = loose, selectedItem = "土豆").filteredRecords.map { it.id },
        )
    }
}

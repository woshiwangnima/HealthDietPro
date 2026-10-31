package com.woshiwangnima.healthdietpro.ui.price

import com.woshiwangnima.healthdietpro.model.price.PriceItemSource
import com.woshiwangnima.healthdietpro.model.price.PriceRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * 【统计】页筛选的「草稿 → 生效」两步走：
 * 改格子只动草稿（列表不动），[PriceUiState.applyDraft] 才把四个条件一起落到 [PriceUiState.filteredRecords]。
 */
class PriceFilterApplyTest {

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
    fun changingDraftDoesNotTouchTheList() {
        val applied = PriceUiState(records = records)
        // 用户在四个格子里挑了一圈，但还没点【开始查找】。
        val draft = applied.copy(
            selectedItem = "土豆",
            selectedRegions = setOf(beijing),
            selectedYear = 2026,
            selectedMonth = 3,
        )

        assertEquals(3, draft.filteredRecords.size)
        assertTrue(draft.hasPendingFilters)
        assertFalse(draft.hasFilters)
    }

    @Test
    fun applyDraftAppliesAllFourConditionsAtOnce() {
        val draft = PriceUiState(records = records).copy(
            selectedItem = "土豆",
            selectedRegions = setOf(beijing),
            selectedYear = 2026,
            selectedMonth = 3,
        )

        val applied = draft.applyDraft()

        assertEquals(listOf("a"), applied.filteredRecords.map { it.id })
        assertFalse(applied.hasPendingFilters)
        assertTrue(applied.hasFilters)
    }

    @Test
    fun applyDraftIsNoOpWhenNothingChanged() {
        val applied = PriceUiState(records = records, selectedItem = "土豆").applyDraft()

        // 没改动就别造新对象：`copy` 一次等于让整页重组一次。
        assertSame(applied, applied.applyDraft())
    }

    @Test
    fun revertingTheDraftClearsPendingState() {
        val applied = PriceUiState(records = records, selectedItem = "土豆").applyDraft()
        val reverted = applied
            .copy(selectedRegions = setOf(shanghai))
            .copy(selectedRegions = emptySet())

        // 只改一个多选再改回来，不该留着「有改动未应用」的假信号。
        assertFalse(reverted.hasPendingFilters)
    }

    @Test
    fun withoutFiltersResetsDraftAndAppliedTogether() {
        val applied = PriceUiState(records = records)
            .copy(selectedItem = "土豆", selectedRegions = setOf(beijing))
            .applyDraft()
            .copy(selectedYear = 2026)

        val cleared = applied.withoutFilters()

        assertEquals(3, cleared.filteredRecords.size)
        assertFalse(cleared.hasFilters)
        assertFalse(cleared.hasPendingFilters)
        assertNull(cleared.selectedYear)
    }

    @Test
    fun regionAndMonthFiltersStackOnTheAppliedConditions() {
        val applied = PriceUiState(records = records)
            .copy(selectedRegions = setOf(beijing, shanghai), selectedMonth = 3)
            .applyDraft()

        // 两个地区 + 3 月 → 两条土豆，鸡蛋（2025-06）被月份挡掉。
        assertEquals(listOf("b", "a"), applied.filteredRecords.map { it.id })
        assertEquals(1, applied.visibleStats.size)
        assertEquals(2, applied.visibleStats.single().recordCount)
    }
}

package com.woshiwangnima.healthdietpro.ui.price

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientRepository
import com.woshiwangnima.healthdietpro.model.food.searchMatchRank
import com.woshiwangnima.healthdietpro.model.price.PriceGroup
import com.woshiwangnima.healthdietpro.model.price.PriceItemStats
import com.woshiwangnima.healthdietpro.model.price.PriceRecord
import com.woshiwangnima.healthdietpro.model.price.PriceRepository
import com.woshiwangnima.healthdietpro.model.price.groupByPrice
import com.woshiwangnima.healthdietpro.model.price.priceItemKey
import com.woshiwangnima.healthdietpro.model.price.statsByItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** 记物价的界面状态：全部记录、筛选条件与可选物品库。 */
internal data class PriceUiState(
    val records: List<PriceRecord> = emptyList(),
    val foods: List<FoodItem> = emptyList(),
    /**
     * 商品筛选的**草稿值**：记录里出现过的某个物品名；null 表示全部。
     *
     * 只存显示名，比对时一律过 [priceItemKey]：记录里同一个物品可能写成「红富士苹果」「红富士苹果 」，
     * 与统计聚合用的是同一把尺子，否则筛出来的行数和那一行的「N 条」对不上。
     *
     * 这只是界面里正在编的那一份，点【开始查找】后才写进 [appliedItem]。
     */
    val selectedItem: String? = null,
    /** 地区筛选的草稿值：按「省-市-区」显示名；空集表示不限地区。 */
    val selectedRegions: Set<String> = emptySet(),
    /** 年份筛选的草稿值；null 表示不限年份。 */
    val selectedYear: Int? = null,
    /** 月份筛选的草稿值（1–12）；null 表示不限月份。 */
    val selectedMonth: Int? = null,
    /**
     * 已生效的商品筛选。[filteredRecords] 只认这一组，不认上面四个草稿值。
     *
     * 四个筛选项是「填完一起查」：改格子只动草稿，点【开始查找】才落到这里。少了这层，
     * 每点一个地区就重算一次统计——连选三个地区列表要跳三次，还没选完结果就把人带跑了。
     */
    val appliedItem: String? = null,
    /** 已生效的地区筛选。 */
    val appliedRegions: Set<String> = emptySet(),
    /** 已生效的年份筛选。 */
    val appliedYear: Int? = null,
    /** 已生效的月份筛选。 */
    val appliedMonth: Int? = null,
    val loading: Boolean = false,
) {
    /** 记录里出现过的地区（去重，保持出现顺序），用于地区下拉。 */
    val regionOptions: List<String>
        get() = records.map(PriceRecord::regionDisplay)
            .filter(String::isNotBlank)
            .distinct()

    /**
     * 记录里出现过的物品名（按物品键去重，最近的在前），用于商品下拉。
     *
     * 顺序按记录时间倒序而不是按字面排序：下拉里最想找的通常是刚买过的那几样，
     * 而中文按 Unicode 排出来的顺序没有任何语义。名字长了还有下拉内的搜索框兜着。
     */
    val itemOptions: List<String>
        get() = records.sortedByDescending(PriceRecord::recordedAt)
            .map(PriceRecord::itemName)
            .distinctBy(::priceItemKey)

    /** 记录里出现过的年份（倒序）。只列真实存在的年份，不给一个筛不出东西的空年。 */
    val yearOptions: List<Int>
        get() {
            val zone = ZoneId.systemDefault()
            return records.map { Instant.ofEpochMilli(it.recordedAt).atZone(zone).year }
                .distinct()
                .sortedDescending()
        }

    /** 草稿与已生效的不一致：说明有改动还没查，卡片上的【开始查找】要亮起来。 */
    val hasPendingFilters: Boolean
        get() = selectedItem != appliedItem ||
            selectedRegions != appliedRegions ||
            selectedYear != appliedYear ||
            selectedMonth != appliedMonth

    /**
     * 把四个草稿值一次性提交成生效条件。
     *
     * 没有改动时**返回自己**而不是 `copy`：`copy` 会造一个新对象，等于让整页重组一次，
     * 而这一刻什么也没变。
     */
    fun applyDraft(): PriceUiState = if (!hasPendingFilters) {
        this
    } else {
        copy(
            appliedItem = selectedItem,
            appliedRegions = selectedRegions,
            appliedYear = selectedYear,
            appliedMonth = selectedMonth,
        )
    }

    /** 草稿与生效值一起归零：格子回「全部」，列表立刻回全量，不留一个「待应用」的中间态。 */
    fun withoutFilters(): PriceUiState = copy(
        selectedItem = null,
        selectedRegions = emptySet(),
        selectedYear = null,
        selectedMonth = null,
        appliedItem = null,
        appliedRegions = emptySet(),
        appliedYear = null,
        appliedMonth = null,
    )

    /** 只要有一项筛选**已生效**，统计页才需要露出「清除筛选」。 */
    val hasFilters: Boolean
        get() = appliedItem != null || appliedRegions.isNotEmpty() ||
            appliedYear != null || appliedMonth != null

    /**
     * 筛选之后的记录，只给【统计】页用。
     *
     * 【记录】页搬到本页的筛选控件是「看汇总时的收窄手段」：记录页要保持「全部原始记录」的语义，
     * 所以它读 [allGroups]，不读这里。两边分开之后，记录页不会因为统计页筛过而少几条而让人以为丢数据。
     */
    val filteredRecords: List<PriceRecord>
        get() {
            val itemKey = appliedItem?.let(::priceItemKey)
            val needsDate = appliedYear != null || appliedMonth != null
            val zone = ZoneId.systemDefault()
            return records
                .filter { record ->
                    if (appliedRegions.isNotEmpty() && record.regionDisplay !in appliedRegions) {
                        return@filter false
                    }
                    if (itemKey != null && priceItemKey(record.itemName) != itemKey) {
                        return@filter false
                    }
                    if (needsDate) {
                        val date = Instant.ofEpochMilli(record.recordedAt).atZone(zone)
                        if (appliedYear != null && date.year != appliedYear) return@filter false
                        if (appliedMonth != null && date.monthValue != appliedMonth) return@filter false
                    }
                    true
                }
                .sortedByDescending(PriceRecord::recordedAt)
        }

    /**
     * 【记录】页实际渲染的卡片：同物品 + 同地区 + 同日期 + 同单位的多条记录合并成一条价格区间。
     * 恒基于全部记录，与统计页的筛选互不影响。
     */
    val allGroups: List<PriceGroup>
        get() = records.groupByPrice()

    /** 【统计】页：筛选之后的记录按物品收口成价格区间。 */
    val visibleStats: List<PriceItemStats>
        get() = filteredRecords.statsByItem()

    /**
     * 【物品历史】页要展示的记录：**统计页当前筛选结果之内**、属于该物品的那些。
     *
     * 刻意不读全量 `records`：统计行上写着「N 个地区 · N 条」，点进去看到的却是另一批记录，
     * 会让人以为统计数错了。筛选条件（地区 / 年份 / 月份 / 搜索）在历史页里继承，页面上也不再重复摆一份。
     */
    fun historyRecords(itemKey: String): List<PriceRecord> =
        filteredRecords.filter { priceItemKey(it.itemName) == itemKey }

    /**
     * 【物品历史】列表右上角「X 条 / 共 M 条」的分母：这个物品在**不受统计页筛选影响**时的条目数。
     *
     * 分母与分子用同一把尺子（都按「地区 + 日期 + 单位」合并），比的才是「筛选藏掉了多少条」；
     * 拿原始记录条数当分母会和列表行数对不上——一行可能由多条记录合成。
     */
    fun historyItemRowTotal(itemKey: String): Int =
        records.filter { priceItemKey(it.itemName) == itemKey }.groupByPrice().size
}

internal class PriceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PriceRepository.fromContext(application)
    private val foodRepository = FoodNutrientRepository.fromContext(application)
    private val _uiState = MutableStateFlow(PriceUiState())
    val uiState: StateFlow<PriceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val archive = withContext(Dispatchers.IO) { repository.load() }
            _uiState.value = _uiState.value.copy(
                records = archive.records.sortedByDescending(PriceRecord::recordedAt),
                loading = false,
            )
            ensureFoodsLoaded()
        }
    }

    /** 物品库较大（数千条 JSON），只在首次需要时加载一次。 */
    fun ensureFoodsLoaded() {
        if (_uiState.value.foods.isNotEmpty()) return
        viewModelScope.launch {
            val foods = withContext(Dispatchers.IO) { runCatching { foodRepository.foods() }.getOrDefault(emptyList()) }
            _uiState.value = _uiState.value.copy(foods = foods)
        }
    }

    // 下面四个 setter 都只改**草稿**：界面立刻回显，但统计不动，等【开始查找】一起提交。
    fun selectItem(item: String?) {
        _uiState.value = _uiState.value.copy(selectedItem = item)
    }

    fun toggleRegion(region: String) {
        val selected = _uiState.value.selectedRegions
        _uiState.value = _uiState.value.copy(
            selectedRegions = if (region in selected) selected - region else selected + region,
        )
    }

    fun clearRegionFilters() {
        _uiState.value = _uiState.value.copy(selectedRegions = emptySet())
    }

    /** 年份单选：点已选中的年份等于取消该条件（「全部」走同一个入口传 null）。 */
    fun selectYear(year: Int?) {
        _uiState.value = _uiState.value.copy(selectedYear = year)
    }

    fun selectMonth(month: Int?) {
        _uiState.value = _uiState.value.copy(selectedMonth = month)
    }

    /**
     * 把四个草稿值一次性提交成生效条件。
     *
     * 必须一次 copy 写四个字段：分四次改状态会触发四次重组，中间那几帧是「只应用了一半」的
     * 条件组合，列表会闪一下不存在的中间结果。
     */
    fun applyFilters() {
        _uiState.value = _uiState.value.applyDraft()
    }

    /** 四项一起清：草稿与生效值同时归零，格子回到「全部」，列表立刻回全量。 */
    fun clearFilters() {
        _uiState.value = _uiState.value.withoutFilters()
    }

    fun upsert(record: PriceRecord) {
        _uiState.value = _uiState.value.copy(
            records = (_uiState.value.records.filterNot { it.id == record.id } + record)
                .sortedByDescending(PriceRecord::recordedAt),
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.upsert(record) }
            refresh()
        }
    }

    /** 删除一张合并卡片：卡片里可能是同一个条件下的多条记录，一起删掉。 */
    fun deleteGroup(group: PriceGroup) {
        deleteAll(group.records.map(PriceRecord::id).toSet())
    }

    private fun deleteAll(ids: Set<String>) {
        if (ids.isEmpty()) return
        _uiState.value = _uiState.value.copy(records = _uiState.value.records.filterNot { it.id in ids })
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (ids.size == 1) repository.delete(ids.first()) else repository.deleteAll(ids)
            }
            refresh()
        }
    }

    /** 供物品选择器搜索：先按名称排名，名字全不中时再退到搜索索引。 */
    fun searchFoods(query: String, limit: Int = 40): List<FoodItem> {
        val token = query.trim()
        if (token.isBlank()) {
            return _uiState.value.foods.sortedByDescending(FoodItem::commonness).take(limit)
        }
        val ranked = _uiState.value.foods
            .mapNotNull { food -> food.searchMatchRank(token)?.let { rank -> rank to food } }
            .sortedWith(compareBy({ it.first }, { -it.second.commonness }))
            .map { it.second }
        if (ranked.isNotEmpty()) return ranked.take(limit)
        val indexed = runCatching { foodRepository.searchIds(token) }.getOrDefault(emptyList()).toSet()
        return _uiState.value.foods.filter { it.id in indexed }.take(limit)
    }

    fun foodById(id: String): FoodItem? = _uiState.value.foods.firstOrNull { it.id == id }
}

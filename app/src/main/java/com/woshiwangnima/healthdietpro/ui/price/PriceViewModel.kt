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
import com.woshiwangnima.healthdietpro.model.price.statsByItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 记物价的界面状态：全部记录、筛选条件与可选物品库。 */
internal data class PriceUiState(
    val records: List<PriceRecord> = emptyList(),
    val foods: List<FoodItem> = emptyList(),
    val query: String = "",
    /** 按「省-市-区」显示名筛选；空集表示不限地区。 */
    val selectedRegions: Set<String> = emptySet(),
    val loading: Boolean = false,
) {
    /** 记录里出现过的地区（去重，保持出现顺序），用于筛选 chip。 */
    val regionOptions: List<String>
        get() = records.map(PriceRecord::regionDisplay)
            .filter(String::isNotBlank)
            .distinct()

    val visibleRecords: List<PriceRecord>
        get() {
            val token = query.trim()
            return records
                .filter { record ->
                    (selectedRegions.isEmpty() || record.regionDisplay in selectedRegions) &&
                        (token.isBlank() || record.itemName.contains(token, ignoreCase = true))
                }
                .sortedByDescending(PriceRecord::recordedAt)
        }

    /**
     * 列表实际渲染的卡片：同物品 + 同地区 + 同日期 + 同单位的多条记录合并成一条价格区间。
     * 合并发生在筛选之后，避免「筛掉的记录还被算进区间」。
     */
    val visibleGroups: List<PriceGroup>
        get() = visibleRecords.groupByPrice()

    /** 【统计】页：同样是筛选之后的记录，再按物品收口成价格区间。 */
    val visibleStats: List<PriceItemStats>
        get() = visibleRecords.statsByItem()
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

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
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

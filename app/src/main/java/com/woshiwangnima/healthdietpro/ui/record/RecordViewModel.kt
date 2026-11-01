package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import com.woshiwangnima.healthdietpro.R
import androidx.lifecycle.AndroidViewModel
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRepository
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureRepository
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecordRepository
import com.woshiwangnima.healthdietpro.model.disease.UserCustomDisease
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.disease.curatedId
import com.woshiwangnima.healthdietpro.model.disease.customId
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.diet.DietRepository
import com.woshiwangnima.healthdietpro.model.price.PriceRepository
import com.woshiwangnima.healthdietpro.model.water.WaterRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.prefs.RecordFeaturePrefs
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.prefs.deserializeSearchHistory
import com.woshiwangnima.healthdietpro.model.prefs.pushSearchHistory
import com.woshiwangnima.healthdietpro.model.prefs.serializeSearchHistory
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.model.unit.formatGlucoseValue
import com.woshiwangnima.healthdietpro.model.sleep.durationMinutes
import com.woshiwangnima.healthdietpro.ui.diet.displayRes
import com.woshiwangnima.healthdietpro.ui.diet.formatCalories
import com.woshiwangnima.healthdietpro.ui.profile.CircumferenceMetric
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun setQuery(query: String) { _uiState.value = _uiState.value.copy(query = query) }

    /**
     * 把当前输入记进搜索历史。过滤是随输入实时发生的，这里只负责「这次搜索值得记住」，
     * 所以只在回车、或点开某条搜索结果时调用。
     */
    fun commitSearch(query: String = _uiState.value.query) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        // 历史是个定长栈：最新一条进栈，超出 10 条的最旧一条出栈（与搜索面板展示上限同一个数）。
        val history = pushSearchHistory(_uiState.value.searchHistory, normalized)
        _uiState.value = _uiState.value.copy(searchHistory = history)
        saveSearchHistory(history)
    }

    fun removeSearchHistory(value: String) {
        val history = _uiState.value.searchHistory - value
        _uiState.value = _uiState.value.copy(searchHistory = history)
        saveSearchHistory(history)
    }

    fun clearSearchHistory() {
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        saveSearchHistory(emptyList())
    }

    fun recordActionOpened(id: RecordActionId) {
        val recent = listOf(id) + _uiState.value.recentActionIds.filterNot { it == id }
        _uiState.value = _uiState.value.copy(recentActionIds = recent.take(6))
        saveRecentActions(recent)
    }
    fun removeRecentAction(id: RecordActionId) {
        val recent = _uiState.value.recentActionIds - id
        _uiState.value = _uiState.value.copy(recentActionIds = recent)
        saveRecentActions(recent)
    }
    fun clearRecentActions() {
        _uiState.value = _uiState.value.copy(recentActionIds = emptyList())
        saveRecentActions(emptyList())
    }

    fun refresh() {
        val context = getApplication<Application>()
        val profile = ProfilePrefs.load(context)
        val glucose = BloodGlucoseRepository.fromContext(context).load().maxByOrNull { it.timestamp }
        val bloodPressure = BloodPressureRepository.fromContext(context).load().maxByOrNull { it.timestamp }
        val latestDisease = UserDiseaseRecordRepository.fromContext(context).load().maxByOrNull { it.updatedAt }
        val customDiseases = UserDiseaseRecordRepository.fromContext(context).loadCustomDiseases().associateBy(UserCustomDisease::id)
        val diseaseCatalog = DiseaseRepository.fromContext(context)
        val pressureUnitId = AppPrefs.getUnit(context, UnitCategoryType.Pressure.id, UnitCategoryType.Pressure.defaultUnitId)
        val pressureUnit = UnitConverter.getRepository()?.getUnit(UnitCategoryType.Pressure.id, pressureUnitId)
            ?.symbol(Locale.getDefault()) ?: pressureUnitId
        val medication = MedicationPrefs.getRecords(context).maxByOrNull { it.timestamp }
        val water = WaterRepository.fromContext(context).load().records.maxByOrNull { it.timestamp }
        val glucoseUnitId = AppPrefs.getUnit(context, UnitCategoryType.Glucose.id, UnitCategoryType.Glucose.defaultUnitId)
        val glucoseUnit = UnitConverter.getRepository()?.getUnit(UnitCategoryType.Glucose.id, glucoseUnitId)
            ?.symbol(Locale.getDefault()) ?: glucoseUnitId
        val circumference = profile.circumferenceRecords
            .flatMap { (metricId, records) -> records.map { metricId to it } }
            .maxByOrNull { (_, record) -> record.recordedAtMillis }
        val sleep = com.woshiwangnima.healthdietpro.model.sleep.SleepRepository.fromContext(context)
            .load().records.maxByOrNull { it.sleepStartAt }
        val diet = DietRepository.fromContext(context).load().records.maxByOrNull { it.mealStartAt }
        val price = PriceRepository.fromContext(context).load().records.maxByOrNull { it.recordedAt }
        val latest = mapOf(
            RecordActionId.Height to profile.heightRecords.maxByOrNull { it.recordedAtMillis }?.let { RecordLatest(it.recordedAtMillis, "${it.value} cm") },
            RecordActionId.Weight to profile.weightRecords.maxByOrNull { it.recordedAtMillis }?.let { RecordLatest(it.recordedAtMillis, String.format(Locale.getDefault(), "%.2f kg", it.value)) },
            RecordActionId.BloodGlucose to glucose?.let {
                RecordLatest(it.timestamp, "${formatGlucoseValue(it.valueMmolPerL, glucoseUnitId)} $glucoseUnit")
            },
            RecordActionId.BloodPressure to bloodPressure?.let {
                val systolic = UnitConverter.fromBase(UnitCategoryType.Pressure.id, it.systolicMmhg, pressureUnitId)
                val diastolic = UnitConverter.fromBase(UnitCategoryType.Pressure.id, it.diastolicMmhg, pressureUnitId)
                val precision = if (pressureUnitId == "kpa") "%.1f/%.1f %s" else "%.0f/%.0f %s"
                RecordLatest(it.timestamp, String.format(Locale.getDefault(), precision, systolic, diastolic, pressureUnit))
            },
            RecordActionId.Disease to latestDisease?.let { record ->
                val name = record.disease.curatedId()?.let { id -> diseaseCatalog.findByReferenceId(id)?.displayName(Locale.getDefault()) }
                    ?: record.disease.customId()?.let { customDiseases[it]?.name }.orEmpty()
                RecordLatest(record.updatedAt, name)
            },
            RecordActionId.Medication to medication?.let { RecordLatest(it.timestamp, "${it.medicationName} ${it.doseValue} ${it.doseUnit}") },
            RecordActionId.Water to water?.let { RecordLatest(it.timestamp, "${it.beverageName} ${it.volumeMl.toInt()} ml") },
            RecordActionId.Waist to circumference?.let { (metricId, record) ->
                val metricName = CircumferenceMetric.entries.firstOrNull { it.id == metricId }
                    ?.let { context.getString(it.titleRes) }
                    ?: metricId
                val value = UnitConverter.fromBase(UnitCategoryType.Length.id, record.value, "cm")
                RecordLatest(record.recordedAtMillis, "$metricName ${String.format(Locale.getDefault(), "%.1f cm", value)}")
            },
            RecordActionId.Sleep to sleep?.let { record ->
                val minutes = record.durationMinutes()
                // 与睡眠页同一套写法（「8 小时 30 分」而不是「8h 30m」）：
                // 一张列表里两种时长记法，读者要先换算一次才敢比较。
                val duration = minutes?.let { value ->
                    val hours = value / 60
                    val rest = value % 60
                    when {
                        hours > 0 && rest == 0L -> context.getString(R.string.sleep_duration_h, hours)
                        hours > 0 -> context.getString(R.string.sleep_duration_hm, hours, rest)
                        else -> context.getString(R.string.sleep_duration_m, rest)
                    }
                } ?: ""
                RecordLatest(record.sleepStartAt, duration)
            },
            RecordActionId.Diet to diet?.let { record ->
                val totalKcal = record.entries.sumOf { it.resolvedNutrients["ENERGY"]?.value ?: 0.0 }
                val period = context.getString(record.mealPeriod.displayRes())
                RecordLatest(record.mealStartAt, "$period ${formatCalories(totalKcal)} kcal")
            },
            RecordActionId.Price to price?.let { record ->
                RecordLatest(record.recordedAt, "${record.itemName} ${record.priceDisplay}")
            },
        )
        val prefs = UserPrefs.current(context)
        val history = deserializeSearchHistory(prefs.getString(SEARCH_HISTORY_KEY, "[]"))
        val recentActions = deserializeSearchHistory(prefs.getString(RECENT_ACTIONS_KEY, "[]"))
            .mapNotNull { name -> RecordActionId.entries.firstOrNull { it.name == name } }
        // 用户在「用户设置 - 记录设置 - 功能启用」里关掉的功能直接不进列表：
        // 关掉是为了精简界面，不是留个灰块；最近使用里也要一并剔掉，否则搜索面板还能点进去。
        val disabledActions = RecordFeaturePrefs.disabledFeatureIds(context)
            .mapNotNull { name -> RecordActionId.entries.firstOrNull { it.name == name } }
            .toSet()
        _uiState.value = _uiState.value.copy(
            sections = defaultRecordSections().withoutDisabledActions(disabledActions).map { section ->
                section.copy(items = section.items.map { item ->
                    latest[item.id]?.let { item.copy(latestTimestamp = it.timestamp, latestValue = it.value) } ?: item
                })
            },
            searchHistory = history,
            recentActionIds = recentActions.filterNot { it in disabledActions },
        )
    }

    private data class RecordLatest(val timestamp: Long, val value: String)
    private fun saveSearchHistory(history: List<String>) {
        UserPrefs.current(getApplication()).putString(SEARCH_HISTORY_KEY, serializeSearchHistory(history))
    }

    private fun saveRecentActions(actions: List<RecordActionId>) {
        UserPrefs.current(getApplication()).putString(RECENT_ACTIONS_KEY, serializeSearchHistory(actions.map { it.name }))
    }

    private companion object {
        const val SEARCH_HISTORY_KEY = "record_search_history_v1"
        const val RECENT_ACTIONS_KEY = "record_recent_actions_v1"
    }
}

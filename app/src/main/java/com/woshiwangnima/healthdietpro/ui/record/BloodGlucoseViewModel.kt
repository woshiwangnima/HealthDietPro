package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodHbA1cRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseSource
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseDiabetesType
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTargetRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseReminderRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseReminderSettings
import com.woshiwangnima.healthdietpro.model.bloodglucose.evaluateBloodGlucoseAlerts
import com.woshiwangnima.healthdietpro.common.notification.BloodGlucoseAlertNotifier
import com.woshiwangnima.healthdietpro.model.bloodglucose.normalizeBloodGlucoseTimestamp
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartWindow
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartStylePrefs
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseChartStyleRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionPoint
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionConfidence
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionArchiveStore
import com.woshiwangnima.healthdietpro.model.bloodglucose.predictBloodGlucose
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionGenerationStatus
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionEligibility
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionFormula
import com.woshiwangnima.healthdietpro.model.bloodglucose.evaluatePredictionEligibility
import com.woshiwangnima.healthdietpro.model.diet.DietRepository
import com.woshiwangnima.healthdietpro.model.sleep.SleepRepository
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.disease.diabetesReferenceIds
import com.woshiwangnima.healthdietpro.model.disease.hasCurrentUserDiabetesRisk
import com.woshiwangnima.healthdietpro.model.disease.curatedId
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRange
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangePreset
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class BloodGlucoseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BloodGlucoseRepository.fromContext(application)
    private val targetRepository = BloodGlucoseTargetRepository.fromContext(application)
    private val reminderRepository = BloodGlucoseReminderRepository.fromContext(application)
    private val chartStyleRepository = BloodGlucoseChartStyleRepository.fromContext(application)
    private val predictionArchive = BloodGlucosePredictionArchiveStore.current(application)
    private val alertNotifier = BloodGlucoseAlertNotifier(application)
    private val _records = MutableStateFlow<List<BloodGlucoseRecord>>(emptyList())
    val records: StateFlow<List<BloodGlucoseRecord>> = _records.asStateFlow()
    private val _hbA1cRecords = MutableStateFlow<List<BloodHbA1cRecord>>(emptyList())
    val hbA1cRecords: StateFlow<List<BloodHbA1cRecord>> = _hbA1cRecords.asStateFlow()
    private val _sources = MutableStateFlow<List<BloodGlucoseSource>>(emptyList())
    val sources: StateFlow<List<BloodGlucoseSource>> = _sources.asStateFlow()
    private val _diabetesType = MutableStateFlow(BloodGlucoseDiabetesType.Normal)
    val diabetesType: StateFlow<BloodGlucoseDiabetesType> = _diabetesType.asStateFlow()
    private val _reminderSettings = MutableStateFlow(BloodGlucoseReminderSettings())
    val reminderSettings: StateFlow<BloodGlucoseReminderSettings> = _reminderSettings.asStateFlow()
    private val preferences = UserPrefs.current(application)
    private val _chartWindow = MutableStateFlow(loadChartWindow())
    val chartWindow: StateFlow<BloodGlucoseChartWindow> = _chartWindow.asStateFlow()
    private val _chartScope = MutableStateFlow(loadChartScope())
    val chartScope: StateFlow<RecordTimeRangeSelection> = _chartScope.asStateFlow()
    private val _chartWindowEnd = MutableStateFlow<Long?>(null)
    val chartWindowEnd: StateFlow<Long?> = _chartWindowEnd.asStateFlow()
    private val _chartStyle = MutableStateFlow(BloodGlucoseChartStylePrefs())
    val chartStyle: StateFlow<BloodGlucoseChartStylePrefs> = _chartStyle.asStateFlow()
    private val _predictionPoints = MutableStateFlow<List<BloodGlucosePredictionPoint>>(emptyList())
    val predictionPoints: StateFlow<List<BloodGlucosePredictionPoint>> = _predictionPoints.asStateFlow()
    private val _predictionConfidence = MutableStateFlow<BloodGlucosePredictionConfidence?>(null)
    val predictionConfidence: StateFlow<BloodGlucosePredictionConfidence?> = _predictionConfidence.asStateFlow()
    private val _predictionGenerating = MutableStateFlow(false)
    val predictionGenerating: StateFlow<Boolean> = _predictionGenerating.asStateFlow()
    private val _predictionGenerationStatus = MutableStateFlow(BloodGlucosePredictionGenerationStatus.IDLE)
    val predictionGenerationStatus: StateFlow<BloodGlucosePredictionGenerationStatus> = _predictionGenerationStatus.asStateFlow()
    private val _predictionEligibility = MutableStateFlow(BloodGlucosePredictionEligibility(0, 0, 0, 0))
    val predictionEligibility: StateFlow<BloodGlucosePredictionEligibility> = _predictionEligibility.asStateFlow()
    private val _predictionFormula = MutableStateFlow<BloodGlucosePredictionFormula?>(null)
    val predictionFormula: StateFlow<BloodGlucosePredictionFormula?> = _predictionFormula.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            _diabetesType.value = withContext(Dispatchers.IO) { targetRepository.loadDiabetesType() }
        }
        viewModelScope.launch {
            _reminderSettings.value = withContext(Dispatchers.IO) { reminderRepository.load() }
        }
        viewModelScope.launch {
            _chartStyle.value = withContext(Dispatchers.IO) { chartStyleRepository.load() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val archive = withContext(Dispatchers.IO) { repository.loadArchive() }
            _records.value = archive.records
            _hbA1cRecords.value = archive.hbA1cRecords
            _sources.value = archive.sources
            val predictions = withContext(Dispatchers.IO) { predictionArchive.load() }
            _predictionPoints.value = predictions.points
            _predictionConfidence.value = predictions.confidence
            refreshPredictionEligibility()
        }
    }

    fun upsert(record: BloodGlucoseRecord) {
        val normalizedRecord = record.copy(timestamp = normalizeBloodGlucoseTimestamp(record.timestamp))
        val updated = (_records.value.filterNot { it.id == normalizedRecord.id } + normalizedRecord).sortedByDescending { it.timestamp }
        _records.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.save(updated)
            val settings = reminderRepository.load()
            evaluateBloodGlucoseAlerts(normalizedRecord, updated, settings).forEach { alert ->
                val intervalMinutes = when (alert.kind) {
                    com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseAlertKind.RisingFast -> settings.risingReminderIntervalSeconds / 60
                    com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseAlertKind.FallingFast -> settings.fallingReminderIntervalSeconds / 60
                    else -> 0
                }
                if (normalizedRecord.timestamp - reminderRepository.lastAlertAt(alert.kind) >= intervalMinutes * 60_000L) {
                    reminderRepository.saveLastAlertAt(alert.kind, normalizedRecord.timestamp)
                    alertNotifier.notify(alert)
                }
            }
        }
    }

    fun upsertHbA1c(record: BloodHbA1cRecord) {
        val updated = (_hbA1cRecords.value.filterNot { it.id == record.id } + record).sortedByDescending { it.timestamp }
        _hbA1cRecords.value = updated
        viewModelScope.launch(Dispatchers.IO) { repository.saveHbA1cRecords(updated) }
    }

    fun deleteGlucose(id: String) {
        val updated = _records.value.filterNot { it.id == id }
        _records.value = updated
        viewModelScope.launch(Dispatchers.IO) { repository.save(updated) }
    }

    fun delete(id: String) = deleteGlucose(id)

    fun deleteHbA1c(id: String) {
        val updated = _hbA1cRecords.value.filterNot { it.id == id }
        _hbA1cRecords.value = updated
        viewModelScope.launch(Dispatchers.IO) { repository.saveHbA1cRecords(updated) }
    }

    fun saveSources(sources: List<BloodGlucoseSource>) {
        _sources.value = sources
        viewModelScope.launch(Dispatchers.IO) { repository.saveSources(sources) }
    }

    fun deleteDataForSource(sourceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDataForSource(sourceId)
            refresh()
        }
    }

    fun reorderSources(orderedIds: List<String>) {
        val sources = com.woshiwangnima.healthdietpro.model.bloodglucose.reorderBloodGlucoseSources(_sources.value, orderedIds)
        _sources.value = sources
        viewModelScope.launch(Dispatchers.IO) { repository.reorderSources(orderedIds) }
    }

    fun setDiabetesType(type: BloodGlucoseDiabetesType) {
        if (!type.available) return
        _diabetesType.value = type
        viewModelScope.launch(Dispatchers.IO) { targetRepository.saveDiabetesType(type) }
    }

    fun setReminderSettings(settings: BloodGlucoseReminderSettings) {
        _reminderSettings.value = settings
        viewModelScope.launch(Dispatchers.IO) { reminderRepository.save(settings) }
    }

    fun setChartWindow(window: BloodGlucoseChartWindow) {
        _chartWindow.value = window
        preferences.putString(CHART_WINDOW_KEY, window.name)
    }

    fun setChartScope(scope: RecordTimeRangeSelection) {
        _chartScope.value = scope
        _chartWindowEnd.value = null
        preferences.putString(CHART_SCOPE_KEY, scope.encode())
    }

    fun setChartWindowEnd(timestamp: Long?) {
        _chartWindowEnd.value = timestamp
    }

    fun setChartStyle(style: BloodGlucoseChartStylePrefs) {
        _chartStyle.value = style
        viewModelScope.launch(Dispatchers.IO) { chartStyleRepository.save(style) }
    }

    fun generatePrediction() {
        if (_predictionGenerating.value) return
        _predictionGenerating.value = true
        _predictionGenerationStatus.value = BloodGlucosePredictionGenerationStatus.GENERATING
        viewModelScope.launch {
            try {
                val generatedAt = System.currentTimeMillis()
                val result = withContext(Dispatchers.Default) {
                    val diabetesIds = withContext(Dispatchers.IO) { DiseaseRepository.fromContext(getApplication()).diabetesReferenceIds() }
                    val medications = withContext(Dispatchers.IO) {
                        if (!hasCurrentUserDiabetesRisk(getApplication())) emptyList()
                        else MedicationPrefs.getRecords(getApplication()).filter { record -> record.indicationReferences.any { it.curatedId() in diabetesIds } }
                    }
                    val meals = withContext(Dispatchers.IO) { DietRepository.fromContext(getApplication()).load().records }
                    val sleep = withContext(Dispatchers.IO) { SleepRepository.fromContext(getApplication()).load().records }
                    predictBloodGlucose(_records.value, medications, meals, sleep, generatedAt)
                }
                if (result != null) {
                    withContext(Dispatchers.IO) { predictionArchive.replaceOverlapping(result, generatedAt) }
                    _predictionPoints.value = withContext(Dispatchers.IO) { predictionArchive.load().points }
                    _predictionConfidence.value = result.confidence
                    _chartWindowEnd.value = result.points.lastOrNull()?.timestamp
                    _predictionGenerationStatus.value = BloodGlucosePredictionGenerationStatus.SUCCESS
                    _predictionFormula.value = result.formula
                } else {
                    _predictionGenerationStatus.value = BloodGlucosePredictionGenerationStatus.INSUFFICIENT_DATA
                }
            } catch (_: Throwable) {
                _predictionGenerationStatus.value = BloodGlucosePredictionGenerationStatus.FAILED
            } finally {
                _predictionGenerating.value = false
            }
        }
    }

    fun refreshPredictionEligibility() {
        viewModelScope.launch(Dispatchers.IO) {
            val diabetesIds = DiseaseRepository.fromContext(getApplication()).diabetesReferenceIds()
            val medications = if (!hasCurrentUserDiabetesRisk(getApplication())) emptyList()
            else MedicationPrefs.getRecords(getApplication()).filter { record -> record.indicationReferences.any { it.curatedId() in diabetesIds } }
            val meals = DietRepository.fromContext(getApplication()).load().records
            val sleep = SleepRepository.fromContext(getApplication()).load().records
            _predictionEligibility.value = evaluatePredictionEligibility(_records.value, medications, meals, sleep)
        }
    }

    fun consumePredictionFormula() {
        _predictionFormula.value = null
    }

    private fun loadChartWindow(): BloodGlucoseChartWindow =
        runCatching { BloodGlucoseChartWindow.valueOf(preferences.getString(CHART_WINDOW_KEY, BloodGlucoseChartWindow.Hours24.name)) }
            .getOrDefault(BloodGlucoseChartWindow.Hours24)

    private fun loadChartScope(): RecordTimeRangeSelection {
        val value = preferences.getString(CHART_SCOPE_KEY, "preset:${RecordTimeRangePreset.ALL.name}")
        return value.decodeScope()
    }

    private fun RecordTimeRangeSelection.encode(): String = when (this) {
        is RecordTimeRangeSelection.Preset -> "preset:${preset.name}"
        is RecordTimeRangeSelection.Custom -> "custom:${range.startMillis}:${range.endMillis}"
    }

    private fun String.decodeScope(): RecordTimeRangeSelection = when {
        startsWith("preset:") -> runCatching {
            RecordTimeRangeSelection.Preset(RecordTimeRangePreset.valueOf(removePrefix("preset:")))
        }.getOrDefault(RecordTimeRangeSelection.Preset(RecordTimeRangePreset.ALL))
        startsWith("custom:") -> removePrefix("custom:").split(':').let { parts ->
            val start = parts.getOrNull(0)?.toLongOrNull()
            val end = parts.getOrNull(1)?.toLongOrNull()
            if (start != null && end != null && start <= end) RecordTimeRangeSelection.Custom(RecordTimeRange(start, end))
            else RecordTimeRangeSelection.Preset(RecordTimeRangePreset.ALL)
        }
        else -> RecordTimeRangeSelection.Preset(RecordTimeRangePreset.ALL)
    }

    private companion object {
        const val CHART_WINDOW_KEY = "blood_glucose_chart_window"
        const val CHART_SCOPE_KEY = "blood_glucose_chart_scope"
    }
}

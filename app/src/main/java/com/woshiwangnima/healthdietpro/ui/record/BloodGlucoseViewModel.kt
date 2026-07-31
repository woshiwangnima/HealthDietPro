package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartViewModel
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseDiabetesType
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseTargetRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseReminderRepository
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseReminderSettings
import com.woshiwangnima.healthdietpro.model.bloodglucose.evaluateBloodGlucoseAlerts
import com.woshiwangnima.healthdietpro.common.notification.BloodGlucoseAlertNotifier
import com.woshiwangnima.healthdietpro.model.bloodglucose.normalizeBloodGlucoseTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class BloodGlucoseViewModel(application: Application) : BaseChartViewModel(
    application = application,
    chartBaseKey = "blood_glucose_history",
) {
    private val repository = BloodGlucoseRepository.fromContext(application)
    private val targetRepository = BloodGlucoseTargetRepository.fromContext(application)
    private val reminderRepository = BloodGlucoseReminderRepository.fromContext(application)
    private val alertNotifier = BloodGlucoseAlertNotifier(application)
    private val _records = MutableStateFlow<List<BloodGlucoseRecord>>(emptyList())
    val records: StateFlow<List<BloodGlucoseRecord>> = _records.asStateFlow()
    private val _diabetesType = MutableStateFlow(BloodGlucoseDiabetesType.Normal)
    val diabetesType: StateFlow<BloodGlucoseDiabetesType> = _diabetesType.asStateFlow()
    private val _reminderSettings = MutableStateFlow(BloodGlucoseReminderSettings())
    val reminderSettings: StateFlow<BloodGlucoseReminderSettings> = _reminderSettings.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            _diabetesType.value = withContext(Dispatchers.IO) { targetRepository.loadDiabetesType() }
        }
        viewModelScope.launch {
            _reminderSettings.value = withContext(Dispatchers.IO) { reminderRepository.load() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _records.value = withContext(Dispatchers.IO) { repository.load() }
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

    fun delete(id: String) {
        val updated = _records.value.filterNot { it.id == id }
        _records.value = updated
        viewModelScope.launch(Dispatchers.IO) { repository.save(updated) }
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
}

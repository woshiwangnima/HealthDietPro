package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartViewModel
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRecord
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseRepository
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
    private val _records = MutableStateFlow<List<BloodGlucoseRecord>>(emptyList())
    val records: StateFlow<List<BloodGlucoseRecord>> = _records.asStateFlow()

    init {
        refresh()
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
        viewModelScope.launch(Dispatchers.IO) { repository.save(updated) }
    }

    fun delete(id: String) {
        val updated = _records.value.filterNot { it.id == id }
        _records.value = updated
        viewModelScope.launch(Dispatchers.IO) { repository.save(updated) }
    }
}

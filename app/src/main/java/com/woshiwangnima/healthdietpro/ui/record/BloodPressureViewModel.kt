package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartViewModel
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureRecord
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class BloodPressureViewModel(application: Application) : BaseChartViewModel(
    application = application,
    chartBaseKey = "blood_pressure_history",
) {
    private val repository = BloodPressureRepository.fromContext(application)
    private val _records = MutableStateFlow<List<BloodPressureRecord>>(emptyList())
    val records: StateFlow<List<BloodPressureRecord>> = _records.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _records.value = withContext(Dispatchers.IO) { repository.load() } }
    }

    fun upsert(record: BloodPressureRecord) {
        val normalized = record.copy(timestamp = record.timestamp / 60_000L * 60_000L)
        val updated = (_records.value.filterNot { it.id == normalized.id } + normalized).sortedByDescending { it.timestamp }
        _records.value = updated
        viewModelScope.launch(Dispatchers.IO) { repository.save(updated) }
    }

    fun delete(id: String) {
        val updated = _records.value.filterNot { it.id == id }
        _records.value = updated
        viewModelScope.launch(Dispatchers.IO) { repository.save(updated) }
    }
}

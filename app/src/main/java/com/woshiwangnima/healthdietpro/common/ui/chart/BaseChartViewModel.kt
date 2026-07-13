package com.woshiwangnima.healthdietpro.common.ui.chart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.chart.ComposeChartState
import com.woshiwangnima.healthdietpro.model.chart.ComposeChartStateRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface BaseChartEvent {
    data class StateChanged(val state: ComposeChartState) : BaseChartEvent
}

internal abstract class BaseChartViewModel(
    application: Application,
    chartBaseKey: String,
) : AndroidViewModel(application) {

    private val app = getApplication<Application>()
    internal val chartStateKey = ProfilePrefs.makeChartStateKey(app, chartBaseKey)
    private val chartStateRepository = ComposeChartStateRepository.fromContext(app)
    private var saveChartStateJob: Job? = null
    private var isChartStateLoaded = false

    private val _chartState = MutableStateFlow<ComposeChartState?>(null)
    val chartState: StateFlow<ComposeChartState?> = _chartState.asStateFlow()

    init {
        viewModelScope.launch {
            _chartState.value = withContext(Dispatchers.IO) {
                chartStateRepository.load(chartStateKey)
            }
            isChartStateLoaded = true
        }
    }

    fun onChartEvent(event: BaseChartEvent) {
        when (event) {
            is BaseChartEvent.StateChanged -> persistChartState(event.state)
        }
    }

    private fun persistChartState(chartState: ComposeChartState) {
        if (!isChartStateLoaded) return
        if (_chartState.value == chartState) return
        _chartState.value = chartState
        saveChartStateJob?.cancel()
        saveChartStateJob = viewModelScope.launch(Dispatchers.IO) {
            delay(CHART_STATE_SAVE_DEBOUNCE_MS)
            chartStateRepository.save(chartStateKey, chartState)
        }
    }

    private companion object {
        const val CHART_STATE_SAVE_DEBOUNCE_MS = 250L
    }
}

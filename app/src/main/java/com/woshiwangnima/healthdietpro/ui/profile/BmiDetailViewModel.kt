package com.woshiwangnima.healthdietpro.ui.profile

import android.app.Application
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class BmiDetailUiState(
    val bmiData: List<DataPoint> = emptyList(),
    val selectedTab: Int = 0,
)

internal sealed interface BmiDetailEvent {
    data class TabSelected(val tab: Int) : BmiDetailEvent
}

internal class BmiDetailViewModel(application: Application) : BaseChartViewModel(
    application = application,
    chartBaseKey = BMI_HISTORY_CHART_KEY,
) {

    private val app = getApplication<Application>()

    private val _uiState = MutableStateFlow(
        BmiDetailUiState(selectedTab = AppPrefs.getBmiChartTab(app).coerceIn(0, 1)),
    )
    val uiState: StateFlow<BmiDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val initial = withContext(Dispatchers.IO) {
                val profile = ProfilePrefs.load(app)
                BmiDetailUiState(
                    bmiData = BmiUtil.buildBmiDataPoints(profile.weightRecords, profile.heightRecords),
                    selectedTab = AppPrefs.getBmiChartTab(app).coerceIn(0, 1),
                )
            }
            _uiState.value = initial
        }
    }

    fun onEvent(event: BmiDetailEvent) {
        when (event) {
            is BmiDetailEvent.TabSelected -> selectTab(event.tab)
        }
    }

    private fun selectTab(tab: Int) {
        val selectedTab = tab.coerceIn(0, 1)
        _uiState.value = _uiState.value.copy(selectedTab = selectedTab)
        AppPrefs.setBmiChartTab(app, selectedTab)
    }

    private companion object {
        val BMI_HISTORY_CHART_KEY = charArrayOf(
            'b', 'm', 'i', '_', 'h', 'i', 's', 't', 'o', 'r', 'y',
        ).concatToString()
    }
}

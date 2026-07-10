package com.woshiwangnima.healthdietpro.ui.record

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        RecordUiState(sections = defaultRecordSections()),
    )
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
}

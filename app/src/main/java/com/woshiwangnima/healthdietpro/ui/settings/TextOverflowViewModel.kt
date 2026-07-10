package com.woshiwangnima.healthdietpro.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TextOverflowUiState(
    val overflowMode: String = "ellipsis",
    val autoShrinkEnabled: Boolean = true,
    val autoShrinkMinSize: Int = 8,
    val marqueeSpeed: Int = 200,
)

class TextOverflowViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TextOverflowUiState())
    val uiState: StateFlow<TextOverflowUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                val app = getApplication<Application>()
                TextOverflowUiState(
                    overflowMode = AppPrefs.getTextOverflowMode(app),
                    autoShrinkEnabled = AppPrefs.isAutoShrinkEnabled(app),
                    autoShrinkMinSize = AppPrefs.getAutoShrinkMinSize(app),
                    marqueeSpeed = AppPrefs.getMarqueeSpeed(app),
                )
            }
            _uiState.value = state
        }
    }

    fun setOverflowMode(mode: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppPrefs.setTextOverflowMode(getApplication(), mode)
            }
            _uiState.value = _uiState.value.copy(overflowMode = mode)
        }
    }

    fun setAutoShrinkEnabled(enabled: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppPrefs.setAutoShrinkEnabled(getApplication(), enabled)
            }
            _uiState.value = _uiState.value.copy(autoShrinkEnabled = enabled)
        }
    }

    fun setAutoShrinkMinSize(sp: Int) {
        val clamped = sp.coerceIn(4, 16)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppPrefs.setAutoShrinkMinSize(getApplication(), clamped)
            }
            _uiState.value = _uiState.value.copy(autoShrinkMinSize = clamped)
        }
    }

    fun setMarqueeSpeed(speed: Int) {
        val clamped = speed.coerceIn(50, 2000)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppPrefs.setMarqueeSpeed(getApplication(), clamped)
            }
            _uiState.value = _uiState.value.copy(marqueeSpeed = clamped)
        }
    }
}
package com.woshiwangnima.healthdietpro.common.ui

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppFontScaleState {
    private val _scale = MutableStateFlow(1f)
    val scale: StateFlow<Float> = _scale.asStateFlow()

    fun load(context: Context) {
        _scale.value = AppPrefs.getFontScale(context)
    }

    fun update(context: Context, scale: Float) {
        AppPrefs.setFontScale(context, scale)
        _scale.value = scale
    }
}

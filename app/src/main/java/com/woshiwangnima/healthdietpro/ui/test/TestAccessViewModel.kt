package com.woshiwangnima.healthdietpro.ui.test

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class TestAccessViewModel : ViewModel() {
    private val _isVerified = MutableStateFlow(false)

    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()

    fun verify(password: String): Boolean {
        val accepted = password == TEST_PASSWORD
        if (accepted) _isVerified.value = true
        return accepted
    }

    private companion object {
        const val TEST_PASSWORD = "WSW0923..."
    }
}

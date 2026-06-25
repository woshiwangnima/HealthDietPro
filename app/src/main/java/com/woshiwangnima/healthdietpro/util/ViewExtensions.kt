package com.woshiwangnima.healthdietpro.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.setPadding(0, statusBar.top, 0, navBar.bottom)
        WindowInsetsCompat.CONSUMED
    }
}

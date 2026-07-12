package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.woshiwangnima.healthdietpro.util.needsNavigationBarInset
import com.woshiwangnima.healthdietpro.util.systemNavigationMode

@Composable
internal fun adaptiveNavigationBarWindowInsets(): WindowInsets {
    val context = LocalContext.current
    val needsInset = remember(context) {
        context.systemNavigationMode().needsNavigationBarInset()
    }
    return if (needsInset) {
        WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
    } else {
        WindowInsets(0, 0, 0, 0)
    }
}

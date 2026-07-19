package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton

@Composable
internal fun TestLandingScreen(onOpenCommands: () -> Unit, onOpenCommonUi: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("测试", style = MaterialTheme.typography.headlineLarge)
        AppIconTextButton("测试指令", com.woshiwangnima.healthdietpro.R.drawable.ic_settings, onOpenCommands, Modifier.fillMaxWidth())
        AppIconTextButton("通用UI功能测试", com.woshiwangnima.healthdietpro.R.drawable.ic_nav_test, onOpenCommonUi, Modifier.fillMaxWidth())
    }
}

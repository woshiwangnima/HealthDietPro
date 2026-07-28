package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen

@Composable
internal fun TestLandingScreen(onOpenCommands: () -> Unit, onOpenCommonUi: () -> Unit, modifier: Modifier = Modifier) {
    BaseScreen(title = "测试", includeStatusBarPadding = false) { padding ->
        Column(
            modifier = modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionSectionCard(title = "开发验证", titleIconRes = R.drawable.ic_nav_test) {
                AppIconTextButton("测试指令", R.drawable.ic_settings, onOpenCommands, Modifier.fillMaxWidth())
                AppIconTextButton("通用UI功能测试", R.drawable.ic_nav_test, onOpenCommonUi, Modifier.fillMaxWidth())
            }
        }
    }
}

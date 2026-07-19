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
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen

@Composable
internal fun TestGmScreen(
    onAddHeightRecord: () -> Unit,
    onAddWeightRecord: () -> Unit,
    onAddMedicationRecord: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(title = "测试指令", onBack = onBack) { padding -> Column(
        modifier = modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("用于快速生成本地测试数据。", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppIconTextButton("添加测试身高记录", R.drawable.ic_add, onAddHeightRecord, Modifier.fillMaxWidth())
        AppIconTextButton("添加测试体重记录", R.drawable.ic_add, onAddWeightRecord, Modifier.fillMaxWidth())
        AppIconTextButton("添加测试用药记录", R.drawable.ic_add, onAddMedicationRecord, Modifier.fillMaxWidth())
    } }
}

package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen

@Composable
internal fun TestGmScreen(
    onAddHeightRecord: (Int) -> Unit,
    onAddWeightRecord: (Int) -> Unit,
    onAddMedicationRecord: (Int) -> Unit,
    onAddNutritionFoods: (Int) -> Unit,
    onAddYesterdayGlucose: (Int) -> Unit,
    onAddTodayGlucose: (Int) -> Unit,
    onAddTodayWater: (Int) -> Unit,
    onAddSearchHistories: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var countText by remember { mutableStateOf("10") }
    val count = countText.toIntOrNull()?.coerceIn(1, 100) ?: 1
    BaseScreen(title = "测试指令", onBack = onBack, includeStatusBarPadding = false) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "用于为当前用户生成随机的本地测试数据。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    value = countText,
                    onValueChange = { value -> if (value.all(Char::isDigit)) countText = value },
                    label = { Text("每次添加条目数量（1 至 100）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ActionSectionCard(title = "体征记录", titleIconRes = R.drawable.ic_chart) {
                    AppIconTextButton("添加随机身高记录", R.drawable.ic_add, { onAddHeightRecord(count) }, Modifier.fillMaxWidth())
                    AppIconTextButton("添加随机体重记录", R.drawable.ic_add, { onAddWeightRecord(count) }, Modifier.fillMaxWidth())
                }
            }
            item {
                ActionSectionCard(title = "健康记录", titleIconRes = R.drawable.ic_blood_glucose) {
                    AppIconTextButton("添加随机药品和用药记录", R.drawable.ic_add, { onAddMedicationRecord(count) }, Modifier.fillMaxWidth())
                    AppIconTextButton("添加随机昨日血糖记录", R.drawable.ic_add, { onAddYesterdayGlucose(count) }, Modifier.fillMaxWidth())
                    AppIconTextButton("添加随机今日血糖记录", R.drawable.ic_add, { onAddTodayGlucose(count) }, Modifier.fillMaxWidth())
                    AppIconTextButton("添加随机今日饮水记录", R.drawable.ic_add, { onAddTodayWater(count) }, Modifier.fillMaxWidth())
                }
            }
            item {
                ActionSectionCard(title = "营养与搜索数据", titleIconRes = R.drawable.ic_nav_nutrition) {
                    AppIconTextButton("添加随机食材、食物和菜肴", R.drawable.ic_add, { onAddNutritionFoods(count) }, Modifier.fillMaxWidth())
                    AppIconTextButton("添加随机搜索历史", R.drawable.ic_add, { onAddSearchHistories(count) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

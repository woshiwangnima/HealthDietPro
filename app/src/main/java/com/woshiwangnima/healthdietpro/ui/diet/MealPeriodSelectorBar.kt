package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod

/** 用餐时段单选切换栏（与「设置-默认饮食习惯-用餐时段」同款分段按钮样式）。选中 null 表示「全部时段」。 */
@Composable
internal fun MealPeriodSelectorBar(
    selected: MealPeriod?,
    onPeriodSelected: (MealPeriod?) -> Unit,
    includeAllPeriod: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val options: List<MealPeriod?> = if (includeAllPeriod) {
        listOf(null) + MealPeriod.entries.toList()
    } else {
        MealPeriod.entries.toList()
    }
    val labels = options.associateWith { option -> if (option == null) stringResource(R.string.diet_filter_all) else stringResource(option.displayRes()) }
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))) {
        Row(Modifier.fillMaxWidth().padding(3.dp)) {
            options.forEachIndexed { index, option ->
                val label = labels.getValue(option)
                val selectedColor = if (selected == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(if (selected == option) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent).clickable { onPeriodSelected(option) }.padding(horizontal = 4.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (option == null) Icon(painterResource(R.drawable.ic_diet), null, tint = selectedColor, modifier = Modifier.size(18.dp)) else MealPeriodIcon(option, Modifier.size(20.dp), selectedColor)
                    TextOverflowText(label, Modifier.fillMaxWidth(), MaterialTheme.typography.labelSmall, selectedColor, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

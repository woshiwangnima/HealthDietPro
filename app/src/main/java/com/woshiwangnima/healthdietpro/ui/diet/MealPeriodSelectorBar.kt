package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.R
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
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onPeriodSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = {
                    Text(
                        text = if (option == null) stringResource(R.string.diet_filter_all) else stringResource(option.displayRes()),
                    )
                },
            )
        }
    }
}
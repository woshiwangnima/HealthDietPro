package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.model.diet.MealPeriod

@Composable
internal fun MealPeriodIcon(
    period: MealPeriod,
    modifier: Modifier = Modifier.size(18.dp),
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    when (period) {
        MealPeriod.BREAKFAST -> MealIcon(Icons.Filled.RamenDining, tint, modifier)
        MealPeriod.LUNCH -> MealIcon(Icons.Filled.LunchDining, tint, modifier)
        MealPeriod.DINNER -> MealIcon(Icons.Filled.DinnerDining, tint, modifier)
        MealPeriod.PRE_BREAKFAST_SNACK -> MealIconGroup(
            modifier = modifier,
            icons = listOf(Icons.Filled.Add, Icons.Filled.RamenDining),
            tint = tint,
        )
        MealPeriod.MID_MORNING_SNACK -> MealIconGroup(
            modifier = modifier,
            icons = listOf(Icons.Filled.RamenDining, Icons.Filled.Add, Icons.Filled.LunchDining),
            tint = tint,
        )
        MealPeriod.MID_AFTERNOON_SNACK -> MealIconGroup(
            modifier = modifier,
            icons = listOf(Icons.Filled.LunchDining, Icons.Filled.Add, Icons.Filled.DinnerDining),
            tint = tint,
        )
        MealPeriod.POST_DINNER_SNACK -> MealIconGroup(
            modifier = modifier,
            icons = listOf(Icons.Filled.DinnerDining, Icons.Filled.Add),
            tint = tint,
        )
    }
}

@Composable
private fun MealIcon(image: ImageVector, tint: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Icon(
        imageVector = image,
        contentDescription = null,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
private fun MealIconGroup(
    icons: List<ImageVector>,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        icons.forEach { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(if (icon == Icons.Filled.Add) 7.dp else 11.dp),
            )
        }
    }
}

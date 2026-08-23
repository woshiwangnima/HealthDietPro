package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

data class DetailTabItem(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int? = null,
)

@Composable
fun DetailTabBar(
    items: List<DetailTabItem>,
    selectedId: String,
    onSelected: (DetailTabItem) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = adaptiveNavigationBarWindowInsets(),
    ) {
        AnimatedNavigationRow(
            itemCount = items.size,
            selectedIndex = items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0),
            modifier = Modifier.height(80.dp),
            indicator = { indicatorModifier, selectedIndex ->
                MagneticFluidSliderIndicator(
                    selectedIndex = selectedIndex,
                    itemCount = items.size,
                    color = navigationIndicatorColor(),
                    cornerRadius = 16.dp,
                    horizontalInset = 8.dp,
                    verticalInset = 8.dp,
                    modifier = indicatorModifier,
                )
            },
        ) { index ->
            val item = items[index]
            DetailTabButton(
                item = item,
                selected = item.id == selectedId,
                onClick = { onSelected(item) },
            )
        }
    }
}

@Composable
private fun RowScope.DetailTabButton(
    item: DetailTabItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(item.titleRes)

    AnimatedNavigationItem(
        selected = selected,
        onClick = onClick,
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        color -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            item.iconRes?.let { iconRes ->
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = color,
                )
            }
            Text(
                text = title,
                color = color,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppBottomNavigationBar(
    items: List<AppBottomNavItem>,
    selectedRoute: String,
    onItemClick: (AppBottomNavItem) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        AnimatedNavigationRow(
            itemCount = items.size,
            selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0),
            modifier = Modifier.height(80.dp),
            indicator = { indicatorModifier ->
                Box(
                    modifier = indicatorModifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .background(navigationIndicatorColor(), RoundedCornerShape(16.dp)),
                )
            },
        ) { index ->
            val item = items[index]
            AppBottomNavigationButton(
                item = item,
                selected = item.route == selectedRoute,
                enabled = enabled,
                onClick = { onItemClick(item) },
            )
        }
    }
}

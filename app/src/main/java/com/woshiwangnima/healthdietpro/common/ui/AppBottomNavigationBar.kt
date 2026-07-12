package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable

@Composable
fun AppBottomNavigationBar(
    items: List<AppBottomNavItem>,
    selectedRoute: String,
    onItemClick: (AppBottomNavItem) -> Unit,
) {
    NavigationBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        items.forEach { item ->
            AppBottomNavigationButton(
                item = item,
                selected = item.route == selectedRoute,
                onClick = { onItemClick(item) },
            )
        }
    }
}

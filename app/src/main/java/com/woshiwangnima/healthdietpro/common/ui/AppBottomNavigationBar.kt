package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable

@Composable
fun AppBottomNavigationBar(
    items: List<AppBottomNavItem>,
    selectedRoute: String,
    onItemClick: (AppBottomNavItem) -> Unit,
) {
    NavigationBar {
        items.forEach { item ->
            AppBottomNavigationButton(
                item = item,
                selected = item.route == selectedRoute,
                onClick = { onItemClick(item) },
            )
        }
    }
}

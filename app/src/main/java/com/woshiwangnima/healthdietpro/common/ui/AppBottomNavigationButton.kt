package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

@Composable
fun RowScope.AppBottomNavigationButton(
    item: AppBottomNavItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val title = stringResource(item.titleRes)

    AnimatedNavigationItem(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        selectedContentColor = MaterialTheme.colorScheme.primary,
        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        color -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = title,
                tint = color,
            )
            Text(
                text = title,
                color = color,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

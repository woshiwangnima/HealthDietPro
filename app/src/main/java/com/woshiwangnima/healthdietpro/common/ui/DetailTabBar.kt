package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

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
    ) {
        items.forEach { item ->
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
    val color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val title = stringResource(item.titleRes)

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
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

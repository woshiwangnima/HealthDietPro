package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class EqualWidthTab(@param:StringRes val titleRes: Int, @param:DrawableRes val iconRes: Int? = null)

@Composable
fun EqualWidthSegmentedTabs(
    tabs: List<EqualWidthTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(44.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        AnimatedNavigationRow(
            itemCount = tabs.size,
            selectedIndex = selectedIndex,
            indicator = { indicatorModifier ->
                Box(
                    modifier = indicatorModifier
                        .padding(3.dp)
                        .background(navigationIndicatorColor(), RoundedCornerShape(9.dp)),
                )
            },
        ) { index ->
            val tab = tabs[index]
            AnimatedNavigationItem(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) { contentColor ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    tab.iconRes?.let { Icon(painterResource(it), null, modifier = Modifier.size(16.dp), tint = contentColor) }
                    Text(text = stringResource(tab.titleRes), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = contentColor)
                }
            }
        }
    }
}

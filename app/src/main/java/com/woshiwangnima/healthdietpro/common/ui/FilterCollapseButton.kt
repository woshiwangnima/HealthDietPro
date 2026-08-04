package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal enum class FilterCollapseAxis { Vertical, Horizontal }

/** Shared stateful-looking control for filter panels that collapse on different axes. */
@Composable
internal fun FilterCollapseButton(
    expanded: Boolean,
    axis: FilterCollapseAxis,
    expandedLabel: String,
    collapsedLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = when (axis) {
        FilterCollapseAxis.Vertical -> if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown
        FilterCollapseAxis.Horizontal -> if (expanded) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight
    }
    Surface(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                TextOverflowText(
                    text = if (expanded) expandedLabel else collapsedLabel,
                    style = androidx.compose.ui.text.TextStyle(fontSize = FontTokens.caption),
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

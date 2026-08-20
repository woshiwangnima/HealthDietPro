package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val VisibleChipsFraction = 4.5f

enum class SortOrder { NONE, ASCENDING, DESCENDING }

data class SortOption(val id: String, val label: String)

@Composable
fun SortBar(
    options: List<SortOption>,
    modifier: Modifier = Modifier,
    onSortChange: (List<Pair<String, SortOrder>>) -> Unit = {},
) {
    var activeEncoded by rememberSaveable { mutableStateOf("") }
    val active = remember(activeEncoded) {
        activeEncoded.split(',').filter { it.isNotBlank() }.map { part ->
            val (id, order) = part.split(':')
            id to SortOrder.valueOf(order)
        }
    }
    val onChipClick = { id: String ->
        val cur = active.firstOrNull { it.first == id }
        val next = when {
            cur == null -> active + (id to SortOrder.ASCENDING)
            cur.second == SortOrder.ASCENDING -> active.map { if (it.first == id) id to SortOrder.DESCENDING else it }
            else -> active.filterNot { it.first == id }
        }
        activeEncoded = next.joinToString(",") { "${it.first}:${it.second.name}" }
        onSortChange(next)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val chipWidth: Dp = if (maxWidth == Dp.Infinity) 96.dp else maxWidth / VisibleChipsFraction - 8.dp
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                SortChip(
                    option = option,
                    active = active,
                    chipWidth = chipWidth,
                    onClick = onChipClick,
                )
            }
        }
    }
}

@Composable
private fun SortChip(
    option: SortOption,
    active: List<Pair<String, SortOrder>>,
    chipWidth: Dp,
    onClick: (String) -> Unit,
) {
    val selection = active.firstOrNull { it.first == option.id }
    val isActive = selection != null
    val shape = RoundedCornerShape(50)
    val fg = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val priority = active.indexOf(selection)
    Surface(
        onClick = { onClick(option.id) },
        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = fg,
        shape = shape,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.width(chipWidth),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextOverflowText(
                text = option.label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (isActive) {
                Text(
                    text = if (selection.second == SortOrder.ASCENDING) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                )
                if (active.size > 1) {
                    Text(
                        text = "${priority + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = fg.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
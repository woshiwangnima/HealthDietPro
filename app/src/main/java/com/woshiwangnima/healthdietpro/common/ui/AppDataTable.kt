package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDataTableColumn<T>(
    val title: String,
    val width: Dp = 120.dp,
    val cell: @Composable RowScope.(T) -> Unit,
)

data class AppDataTableAction<T>(
    val title: String,
    val onClick: (T) -> Unit,
)

@Composable
fun <T> AppDataTable(
    rows: List<T>,
    columns: List<AppDataTableColumn<T>>,
    modifier: Modifier = Modifier,
    actions: List<AppDataTableAction<T>> = emptyList(),
    onRowClick: (T) -> Unit = {},
) {
    val horizontalScroll = rememberScrollState()
    val rowEven = MaterialTheme.colorScheme.surface
    val rowOdd = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    val border = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            columns.forEach { column ->
                HeaderCell(title = column.title, width = column.width)
            }
            if (actions.isNotEmpty()) {
                HeaderCell(title = "", width = actionWidth(actions.size))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 0.dp)
                .weight(1f),
        ) {
            itemsIndexed(rows) { index, row ->
                val bg = if (index % 2 == 0) rowEven else rowOdd
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll)
                        .background(bg)
                        .clickable { onRowClick(row) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    columns.forEach { column ->
                        Box(
                            modifier = Modifier
                                .width(column.width)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row {
                                column.cell.invoke(this, row)
                            }
                        }
                    }
                    if (actions.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .width(actionWidth(actions.size))
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        ) {
                            actions.forEach { action ->
                                Text(
                                    text = action.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { action.onClick(row) },
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(border)
                )
            }
        }
    }
}

@Composable
fun AppDataTableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
    )
}

@Composable
private fun HeaderCell(
    title: String,
    width: Dp,
) {
    Text(
        text = title,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 12.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun actionWidth(actionCount: Int): Dp =
    (88 * actionCount.coerceAtLeast(1)).dp

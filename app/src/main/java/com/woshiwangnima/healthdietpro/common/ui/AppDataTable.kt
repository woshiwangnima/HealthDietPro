package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDataTableColumn<T>(
    val key: String,
    val header: @Composable () -> Unit,
    val width: ColumnWidth,
    val alignment: Alignment = Alignment.CenterStart,
    val priority: Int = 0,
    val overflow: ColumnOverflow = ColumnOverflow.Ellipsis,
    val cell: @Composable AppDataTableCellScope<T>.(T) -> Unit,
)

@Immutable
sealed interface ColumnWidth {
    @Immutable
    data class Fixed(val width: Dp) : ColumnWidth

    @Immutable
    data class Flex(
        val weight: Float,
        val min: Dp,
        val max: Dp = Dp.Infinity,
    ) : ColumnWidth
}

enum class ColumnOverflow {
    Clip,
    Ellipsis,
    Wrap,
}

sealed interface AppDataTableLayoutPolicy<T> {
    data class HorizontalScroll<T>(
        val minTableWidth: Dp = 0.dp,
    ) : AppDataTableLayoutPolicy<T>

    data class Responsive<T>(
        val compactAt: Dp,
        val compactHeader: (@Composable () -> Unit)? = null,
        val compactRow: @Composable AppDataTableRowScope<T>.(T) -> Unit,
    ) : AppDataTableLayoutPolicy<T>
}

@Immutable
data class AppDataTableStyle(
    val headerVerticalPadding: Dp = 10.dp,
    val rowVerticalPadding: Dp = 10.dp,
    val cellHorizontalPadding: Dp = 12.dp,
    val actionHorizontalPadding: Dp = 8.dp,
    val dividerHeight: Dp = 1.dp,
)

@Stable
class AppDataTableCellScope<T> internal constructor(
    val column: AppDataTableColumn<T>,
    val rowIndex: Int,
)

@Stable
class AppDataTableRowScope<T> internal constructor(
    val row: T,
    val rowIndex: Int,
)

@Composable
fun <T> AppDataTable(
    rows: List<T>,
    columns: List<AppDataTableColumn<T>>,
    modifier: Modifier = Modifier,
    rowKey: ((Int, T) -> Any)? = null,
    layoutPolicy: AppDataTableLayoutPolicy<T> = AppDataTableLayoutPolicy.HorizontalScroll(),
    actionsWidth: Dp = 88.dp,
    actionsHeader: @Composable (() -> Unit)? = null,
    rowActions: (@Composable AppDataTableRowScope<T>.(T) -> Unit)? = null,
    onRowClick: ((T) -> Unit)? = null,
    style: AppDataTableStyle = AppDataTableStyle(),
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (
            layoutPolicy is AppDataTableLayoutPolicy.Responsive &&
            maxWidth < layoutPolicy.compactAt
        ) {
            CompactDataTable(
                rows = rows,
                rowKey = rowKey,
                onRowClick = onRowClick,
                compactHeader = layoutPolicy.compactHeader,
                compactRow = layoutPolicy.compactRow,
                style = style,
            )
        } else {
            HorizontalDataTable(
                rows = rows,
                columns = columns,
                rowKey = rowKey,
                maxWidth = maxWidth,
                minTableWidth = (layoutPolicy as? AppDataTableLayoutPolicy.HorizontalScroll<*>)?.minTableWidth ?: 0.dp,
                actionsWidth = actionsWidth,
                actionsHeader = actionsHeader,
                rowActions = rowActions,
                onRowClick = onRowClick,
                style = style,
            )
        }
    }
}

@Composable
fun AppDataTableHeaderText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun AppDataTableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    overflow: ColumnOverflow = ColumnOverflow.Ellipsis,
    maxLines: Int = 1,
) {
    when (overflow) {
        ColumnOverflow.Clip -> Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            softWrap = false,
        )

        ColumnOverflow.Ellipsis -> TextOverflowText(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = maxLines,
        )

        ColumnOverflow.Wrap -> Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            overflow = TextOverflow.Clip,
            softWrap = true,
        )
    }
}

@Composable
private fun <T> HorizontalDataTable(
    rows: List<T>,
    columns: List<AppDataTableColumn<T>>,
    rowKey: ((Int, T) -> Any)?,
    maxWidth: Dp,
    minTableWidth: Dp,
    actionsWidth: Dp,
    actionsHeader: @Composable (() -> Unit)?,
    rowActions: (@Composable AppDataTableRowScope<T>.(T) -> Unit)?,
    onRowClick: ((T) -> Unit)?,
    style: AppDataTableStyle,
) {
    val horizontalScroll = rememberScrollState()
    val hasActions = rowActions != null
    val widths = calculateColumnWidths(columns, maxWidth, if (hasActions) actionsWidth else 0.dp)
    val contentWidth = (widths.fold(0.dp) { acc, width -> acc + width } + if (hasActions) actionsWidth else 0.dp)
        .coerceAtLeast(maxWidth)
        .coerceAtLeast(minTableWidth)
    val rowEven = MaterialTheme.colorScheme.surface
    val rowOdd = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    val border = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
        ) {
            Row(
                modifier = Modifier
                    .width(contentWidth)
                    .padding(vertical = style.headerVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                columns.forEachIndexed { index, column ->
                    HeaderCell(column = column, width = widths[index], style = style)
                }
                if (hasActions) {
                    Box(
                        modifier = Modifier
                            .width(actionsWidth)
                            .padding(horizontal = style.actionHorizontalPadding),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        actionsHeader?.invoke()
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 0.dp)
                .weight(1f),
        ) {
            itemsIndexed(
                items = rows,
                key = if (rowKey == null) {
                    null
                } else {
                    { index, row -> rowKey(index, row) }
                },
            ) { index, row ->
                val bg = if (index % 2 == 0) rowEven else rowOdd
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll)
                        .background(bg),
                ) {
                    Row(
                        modifier = Modifier
                            .width(contentWidth)
                            .then(if (onRowClick == null) Modifier else Modifier.clickable { onRowClick(row) })
                            .padding(vertical = style.rowVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        columns.forEachIndexed { columnIndex, column ->
                            Cell(
                                column = column,
                                width = widths[columnIndex],
                                row = row,
                                rowIndex = index,
                                style = style,
                            )
                        }
                        if (hasActions) {
                            Row(
                                modifier = Modifier
                                    .width(actionsWidth)
                                    .padding(horizontal = style.actionHorizontalPadding),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val scope = AppDataTableRowScope(row, index)
                                rowActions?.invoke(scope, row)
                            }
                        }
                    }
                }
                Divider(border = border, style = style)
            }
        }
    }
}

@Composable
private fun <T> CompactDataTable(
    rows: List<T>,
    rowKey: ((Int, T) -> Any)?,
    onRowClick: ((T) -> Unit)?,
    compactHeader: (@Composable () -> Unit)?,
    compactRow: @Composable AppDataTableRowScope<T>.(T) -> Unit,
    style: AppDataTableStyle,
) {
    val rowEven = MaterialTheme.colorScheme.surface
    val rowOdd = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
    val border = MaterialTheme.colorScheme.outlineVariant
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (compactHeader != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .padding(horizontal = style.cellHorizontalPadding, vertical = style.headerVerticalPadding),
                ) {
                    compactHeader()
                }
            }
        }
        itemsIndexed(
            items = rows,
            key = if (rowKey == null) {
                null
            } else {
                { index, row -> rowKey(index, row) }
            },
        ) { index, row ->
            val bg = if (index % 2 == 0) rowEven else rowOdd
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .then(if (onRowClick == null) Modifier else Modifier.clickable { onRowClick(row) })
                    .padding(horizontal = style.cellHorizontalPadding, vertical = style.rowVerticalPadding),
            ) {
                val scope = AppDataTableRowScope(row, index)
                compactRow.invoke(scope, row)
            }
            Divider(border = border, style = style)
        }
    }
}

@Composable
private fun <T> HeaderCell(
    column: AppDataTableColumn<T>,
    width: Dp,
    style: AppDataTableStyle,
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = style.cellHorizontalPadding),
        contentAlignment = column.alignment,
    ) {
        column.header()
    }
}

@Composable
private fun <T> Cell(
    column: AppDataTableColumn<T>,
    width: Dp,
    row: T,
    rowIndex: Int,
    style: AppDataTableStyle,
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = style.cellHorizontalPadding),
        contentAlignment = column.alignment,
    ) {
        val scope = AppDataTableCellScope(column, rowIndex)
        column.cell.invoke(scope, row)
    }
}

@Composable
private fun Divider(
    border: Color,
    style: AppDataTableStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(style.dividerHeight)
            .background(border),
    )
}

private fun <T> calculateColumnWidths(
    columns: List<AppDataTableColumn<T>>,
    availableWidth: Dp,
    actionsWidth: Dp,
): List<Dp> {
    val fixedWidth = columns.fold(0.dp) { acc, column ->
        acc + when (val width = column.width) {
            is ColumnWidth.Fixed -> width.width
            is ColumnWidth.Flex -> 0.dp
        }
    }
    val flexColumns = columns.mapNotNull { it.width as? ColumnWidth.Flex }
    val flexMinWidth = flexColumns.fold(0.dp) { acc, width -> acc + width.min }
    val remainingForFlex = (availableWidth - fixedWidth - actionsWidth).coerceAtLeast(flexMinWidth)
    val extra = (remainingForFlex - flexMinWidth).coerceAtLeast(0.dp)
    val totalWeight = flexColumns.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)

    return columns.map { column ->
        when (val width = column.width) {
            is ColumnWidth.Fixed -> width.width
            is ColumnWidth.Flex -> {
                val weightedExtra = extra * (width.weight / totalWeight)
                (width.min + weightedExtra).coerceAtMost(width.max)
            }
        }
    }
}

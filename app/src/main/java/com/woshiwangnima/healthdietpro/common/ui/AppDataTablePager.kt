package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.woshiwangnima.healthdietpro.R

@Composable
internal fun DataTablePager(
    totalRows: Int,
    rowsPerPageText: String,
    currentPage: Int,
    pageCount: Int,
    selectedCount: Int,
    onRowsPerPageChange: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onEditSelected: (() -> Unit)?,
    onClearSelection: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.data_table_total, totalRows), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.data_table_rows_per_page), style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = rowsPerPageText,
                onValueChange = onRowsPerPageChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(56.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (selectedCount > 0) {
                onEditSelected?.let { edit ->
                    TextButton(onClick = edit) { Text(stringResource(R.string.data_table_edit_selected, selectedCount)) }
                }
                TextButton(onClick = onClearSelection) { Text(stringResource(R.string.data_table_clear_selection)) }
            }
            CompactPageButton("|<", currentPage > 0) { onPageChange(0) }
            CompactPageButton("<", currentPage > 0) { onPageChange(currentPage - 1) }
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                visiblePageNumbers(currentPage, pageCount).forEach { page ->
                    CompactPageButton((page + 1).toString(), page != currentPage) { onPageChange(page) }
                }
            }
            CompactPageButton(">", currentPage < pageCount - 1) { onPageChange(currentPage + 1) }
            CompactPageButton(">|", currentPage < pageCount - 1) { onPageChange(pageCount - 1) }
        }
    }
}

@Composable
private fun CompactPageButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.width(24.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        TextOverflowText(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

private fun visiblePageNumbers(currentPage: Int, pageCount: Int): List<Int> {
    val visibleCount = minOf(pageCount, 3)
    val firstPage = (currentPage - 1).coerceIn(0, pageCount - visibleCount)
    return (firstPage until firstPage + visibleCount).toList()
}

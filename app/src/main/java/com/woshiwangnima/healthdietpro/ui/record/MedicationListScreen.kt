package com.woshiwangnima.healthdietpro.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableLayoutPolicy
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnOverflow
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.medication.MedicationCatalogItem
import com.woshiwangnima.healthdietpro.common.ui.formatDateTime

@Composable
internal fun MedicationListScreen(
    uiState: MedicationListUiState,
    title: String,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (MedicationRecord) -> Unit,
    onDeleteRecord: (MedicationRecord) -> Unit,
    onAddCatalogItem: () -> Unit,
    onEditCatalogItem: (MedicationCatalogItem) -> Unit,
) {
    val tabs = remember {
        listOf(
            DetailTabItem("0", R.string.detail_tab_reminder, R.drawable.ic_notification),
            DetailTabItem("1", R.string.detail_tab_log_medication, R.drawable.ic_list),
            DetailTabItem("2", R.string.detail_tab_manage_medicines, R.drawable.ic_medication),
        )
    }

    BaseScreen(title = title, onBack = onBack, includeNavigationBarPadding = false) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    0 -> MedicationReminderPage()
                    1 -> MedicationRecordsPage(
                        records = uiState.records,
                        onAdd = onAddRecord,
                        onEdit = onEditRecord,
                        onDelete = onDeleteRecord,
                    )
                    2 -> MedicationCatalogPage(uiState.catalog, onAddCatalogItem, onEditCatalogItem)
                }
            }
            DetailTabBar(
                items = tabs,
                selectedId = uiState.selectedTab.toString(),
                onSelected = { onTabSelected(it.id.toInt()) },
            )
        }
    }
}

@Composable
private fun MedicationCatalogPage(
    catalog: List<MedicationCatalogItem>,
    onAdd: () -> Unit,
    onEdit: (MedicationCatalogItem) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.medication_catalog_heading), style = MaterialTheme.typography.titleMedium)
                AppIconTextButton(stringResource(R.string.medication_catalog_add), R.drawable.ic_add, onAdd)
            }
        }
        if (catalog.isEmpty()) {
            item { Text(stringResource(R.string.medication_catalog_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(catalog, key = { it.id }) { item ->
            androidx.compose.material3.ListItem(
                headlineContent = { Text(item.name) },
                supportingContent = {
                    Text(listOf(formatCatalogSpec(item), item.manufacturer, item.defaultMethod).filter { it.isNotBlank() }.joinToString(" / "))
                },
                trailingContent = { Text(if (item.archived) stringResource(R.string.medication_catalog_archived) else "") },
                modifier = Modifier.clickable { onEdit(item) },
            )
        }
    }
}

private fun formatCatalogSpec(item: MedicationCatalogItem): String =
    if (item.specValue > 0f || item.specUnitId.isNotBlank()) "${item.specValue}${item.specUnitId}" else ""

@Composable
private fun MedicationReminderPage() {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.medication_reminder_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.medication_reminder_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MedicationRecordsPage(
    records: List<MedicationRecord>,
    onAdd: () -> Unit,
    onEdit: (MedicationRecord) -> Unit,
    onDelete: (MedicationRecord) -> Unit,
) {
    var pendingDeletion by remember { mutableStateOf<MedicationRecord?>(null) }
    pendingDeletion?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text(stringResource(R.string.medication_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.medication_record_delete_confirm_message, record.medicationName)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeletion = null
                    onDelete(record)
                }) {
                    Text(stringResource(R.string.body_record_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletion = null }) {
                    Text(stringResource(R.string.body_record_cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.body_record_count, records.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AppIconTextButton(
                text = stringResource(R.string.medication_record_add),
                iconRes = R.drawable.ic_add,
                onClick = onAdd,
            )
        }
        if (records.isEmpty()) {
            Text(
                text = stringResource(R.string.medication_record_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            MedicationRecordTable(
                records = records,
                onEdit = onEdit,
                onDelete = { pendingDeletion = it },
            )
        }
    }
}

@Composable
private fun MedicationRecordTable(
    records: List<MedicationRecord>,
    onEdit: (MedicationRecord) -> Unit,
    onDelete: (MedicationRecord) -> Unit,
) {
    AppDataTable(
        rows = records,
        rowKey = { _, record -> record.id },
        columns = listOf(
            AppDataTableColumn<MedicationRecord>(
                key = "time",
                header = { AppDataTableHeaderText(stringResource(R.string.medication_record_time)) },
                width = ColumnWidth.Fixed(136.dp),
            ) { record -> AppDataTableText(formatDateTime(record.timestamp)) },
            AppDataTableColumn<MedicationRecord>(
                key = "name",
                header = { AppDataTableHeaderText(stringResource(R.string.medication_record_name)) },
                width = ColumnWidth.Flex(weight = 1.4f, min = 160.dp, max = 260.dp),
            ) { record -> AppDataTableText(record.medicationName, overflow = column.overflow) },
            AppDataTableColumn<MedicationRecord>(
                key = "dose",
                header = { AppDataTableHeaderText(stringResource(R.string.medication_record_dose)) },
                width = ColumnWidth.Fixed(96.dp),
            ) { record -> AppDataTableText(formatDose(record)) },
            AppDataTableColumn<MedicationRecord>(
                key = "method",
                header = { AppDataTableHeaderText(stringResource(R.string.medication_record_method)) },
                width = ColumnWidth.Fixed(96.dp),
            ) { record -> AppDataTableText(record.method) },
            AppDataTableColumn<MedicationRecord>(
                key = "feeling",
                header = { AppDataTableHeaderText(stringResource(R.string.medication_record_feeling)) },
                width = ColumnWidth.Flex(weight = 1f, min = 168.dp, max = 320.dp),
                overflow = ColumnOverflow.Wrap,
            ) { record -> AppDataTableText(formatFeeling(record), overflow = column.overflow) },
        ),
        layoutPolicy = AppDataTableLayoutPolicy.Responsive(
            compactAt = 600.dp,
            compactHeader = {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppDataTableHeaderText(stringResource(R.string.medication_record_time), Modifier.width(132.dp))
                    AppDataTableHeaderText(stringResource(R.string.medication_record_name), Modifier.weight(1f))
                    AppDataTableHeaderText(stringResource(R.string.medication_record_actions))
                }
            },
            compactRow = { record -> CompactMedicationRow(record, onDelete) },
        ),
        actionsWidth = 104.dp,
        actionsHeader = { AppDataTableHeaderText(stringResource(R.string.medication_record_actions)) },
        rowActions = { record ->
            AppDataTableDeleteAction(
                text = stringResource(R.string.body_record_delete),
                onClick = { onDelete(record) },
            )
        },
        onRowClick = onEdit,
    )
}

@Composable
private fun CompactMedicationRow(record: MedicationRecord, onDelete: (MedicationRecord) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppDataTableText(
                text = formatDateTime(record.timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(132.dp),
            )
            AppDataTableText(record.medicationName, modifier = Modifier.weight(1f))
            AppDataTableDeleteAction(
                text = stringResource(R.string.body_record_delete),
                onClick = { onDelete(record) },
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(132.dp))
            AppDataTableText(
                text = listOf(formatDose(record), record.method).filter { it.isNotBlank() }.joinToString(" / "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        formatFeeling(record).takeIf { it.isNotBlank() }?.let { feeling ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(132.dp))
                AppDataTableText(
                    text = feeling,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = ColumnOverflow.Wrap,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun formatDose(record: MedicationRecord): String =
    if (record.doseValue > 0f || record.doseUnit.isNotBlank()) "${record.doseValue}${record.doseUnit}" else ""

private fun formatFeeling(record: MedicationRecord): String =
    listOf(record.feelings.joinToString(", "), record.feelingNote)
        .filter { it.isNotBlank() }
        .joinToString(" / ")

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
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
import com.woshiwangnima.healthdietpro.model.medication.formatDefaultDose
import com.woshiwangnima.healthdietpro.model.medication.formatDose
import com.woshiwangnima.healthdietpro.model.medication.formatSpecification
import com.woshiwangnima.healthdietpro.model.medication.format
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecordRepository
import com.woshiwangnima.healthdietpro.common.ui.formatDateTime
import com.woshiwangnima.healthdietpro.common.ui.RecordTimeRangeFilter
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRange
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection
import com.woshiwangnima.healthdietpro.common.time.resolve
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
internal fun MedicationListScreen(
    uiState: MedicationListUiState,
    title: String,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onAddRecord: () -> Unit,
    canAddRecord: Boolean,
    onEditRecord: (MedicationRecord) -> Unit,
    onDeleteRecord: (MedicationRecord) -> Unit,
    onAddCatalogItem: () -> Unit,
    onEditCatalogItem: (MedicationCatalogItem) -> Unit,
    onDeleteCatalogItem: (MedicationCatalogItem) -> Unit,
    timeRangeSelection: RecordTimeRangeSelection,
    onTimeRangeSelectionChanged: (RecordTimeRangeSelection) -> Unit,
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
                        canAdd = canAddRecord,
                        onEdit = onEditRecord,
                        onDelete = onDeleteRecord,
                        timeRangeSelection = timeRangeSelection,
                        onTimeRangeSelectionChanged = onTimeRangeSelectionChanged,
                        editable = true,
                    )
                    2 -> MedicationCatalogPage(uiState.catalog, onAddCatalogItem, onEditCatalogItem, onDeleteCatalogItem)
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
    onDelete: (MedicationCatalogItem) -> Unit,
) {
    var pendingDeletion by remember { mutableStateOf<MedicationCatalogItem?>(null) }
    pendingDeletion?.let { item ->
        AlertDialog(onDismissRequest = { pendingDeletion = null }, title = { Text(stringResource(R.string.medication_catalog_delete_confirm_title)) }, text = { Text(stringResource(R.string.medication_catalog_delete_confirm_message, item.name)) }, confirmButton = { TextButton(onClick = { pendingDeletion = null; onDelete(item) }) { Text(stringResource(R.string.body_record_delete)) } }, dismissButton = { TextButton(onClick = { pendingDeletion = null }) { Text(stringResource(R.string.body_record_cancel)) } })
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.medication_catalog_heading), style = MaterialTheme.typography.titleMedium)
            AppIconTextButton(stringResource(R.string.medication_catalog_add), R.drawable.ic_add, onAdd)
        }
        if (catalog.isEmpty()) {
            Text(stringResource(R.string.medication_catalog_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            CatalogTable(catalog, onEdit, { pendingDeletion = it }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CatalogTable(catalog: List<MedicationCatalogItem>, onEdit: (MedicationCatalogItem) -> Unit, onDelete: (MedicationCatalogItem) -> Unit, modifier: Modifier = Modifier) {
    val locale = Locale.getDefault()
    val units = UnitConverter.getRepository()
    val context = LocalContext.current
    val diseasesById = remember { DiseaseRepository.fromContext(context).loadAll().associateBy { it.id } }
    val customDiseasesById = remember { UserDiseaseRecordRepository.fromContext(context).loadCustomDiseases().associateBy { it.id } }
    AppDataTable(
        rows = catalog, modifier = modifier, rowKey = { _, item -> item.id },
        columns = listOf(
            AppDataTableColumn<MedicationCatalogItem>("name", { AppDataTableHeaderText(stringResource(R.string.medication_catalog_name)) }, ColumnWidth.Flex(1.2f, 130.dp)) { AppDataTableText(it.name) },
            AppDataTableColumn<MedicationCatalogItem>("specDose", { AppDataTableHeaderText(stringResource(R.string.medication_catalog_spec_dose)) }, ColumnWidth.Fixed(136.dp)) { AppDataTableText(listOf(it.formatSpecification(units, locale), it.formatDefaultDose(locale)).filter(String::isNotBlank).joinToString(" / ")) },
            AppDataTableColumn<MedicationCatalogItem>("frequency", { AppDataTableHeaderText(stringResource(R.string.medication_catalog_frequency)) }, ColumnWidth.Fixed(112.dp)) { AppDataTableText(it.frequency.format(locale)) },
            AppDataTableColumn<MedicationCatalogItem>("timing", { AppDataTableHeaderText(stringResource(R.string.medication_catalog_timing)) }, ColumnWidth.Fixed(128.dp)) { AppDataTableText(it.intakeRules.format(locale)) },
            AppDataTableColumn<MedicationCatalogItem>("purpose", { AppDataTableHeaderText(stringResource(R.string.medication_catalog_indications)) }, ColumnWidth.Flex(1f, 130.dp)) { item -> AppDataTableText(item.indications.joinToString(", ") { it.displayName(diseasesById, customDiseasesById) }) },
            AppDataTableColumn<MedicationCatalogItem>("manufacturer", { AppDataTableHeaderText(stringResource(R.string.medication_catalog_manufacturer)) }, ColumnWidth.Flex(1f, 120.dp)) { AppDataTableText(it.manufacturer) },
        ),
        layoutPolicy = AppDataTableLayoutPolicy.HorizontalScroll(minTableWidth = 900.dp), actionsHeader = { AppDataTableHeaderText(stringResource(R.string.medication_record_actions)) },
        rowActions = { item -> AppDataTableDeleteAction(stringResource(R.string.body_record_delete), onClick = { onDelete(item) }, enabled = item.archived) }, onRowClick = onEdit,
    )
}

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
internal fun MedicationRecordsPage(
    records: List<MedicationRecord>,
    timeRangeSelection: RecordTimeRangeSelection,
    onTimeRangeSelectionChanged: (RecordTimeRangeSelection) -> Unit,
    editable: Boolean,
    onAdd: (() -> Unit)? = null,
    canAdd: Boolean = false,
    onEdit: ((MedicationRecord) -> Unit)? = null,
    onDelete: ((MedicationRecord) -> Unit)? = null,
) {
    var pendingDeletion by remember { mutableStateOf<MedicationRecord?>(null) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timeRangeSelection is RecordTimeRangeSelection.Preset) {
        if (timeRangeSelection !is RecordTimeRangeSelection.Preset) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000L - nowMillis % 60_000L)
        }
    }
    pendingDeletion?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text(stringResource(R.string.medication_record_delete_confirm_title)) },
            text = { Text(stringResource(R.string.medication_record_delete_confirm_message, record.medicationName)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeletion = null
                    onDelete?.invoke(record)
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
        val timeRange = timeRangeSelection.resolve(nowMillis)
        val filteredRecords = remember(records, timeRange) { records.filter { timeRange.contains(it.timestamp) } }
        if (editable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.width(1.dp))
                AppIconTextButton(
                    text = stringResource(R.string.medication_record_add),
                    iconRes = R.drawable.ic_add,
                    onClick = { onAdd?.invoke() },
                    modifier = Modifier.alpha(if (canAdd) 1f else 0.45f),
                )
            }
        }
        RecordTimeRangeFilter(selection = timeRangeSelection, onSelectionChanged = onTimeRangeSelectionChanged)
        if (filteredRecords.isEmpty()) {
            Text(
                text = if (records.isEmpty()) {
                    stringResource(R.string.common_empty_records)
                } else {
                    stringResource(R.string.medication_record_empty_filtered, records.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            MedicationRecordTable(
                records = filteredRecords,
                onEdit = onEdit,
                onDelete = if (editable) ({ pendingDeletion = it }) else null,
            )
        }
    }
}

@Composable
private fun MedicationRecordTable(
    records: List<MedicationRecord>,
    onEdit: ((MedicationRecord) -> Unit)?,
    onDelete: ((MedicationRecord) -> Unit)?,
) {
    val readOnly = onDelete == null
    AppDataTable(
        rows = records,
        rowKey = { _, record -> record.id },
        showRowNumber = !readOnly,
        columns = listOf(
            AppDataTableColumn<MedicationRecord>(
                key = "time",
                header = { MedicationTableHeader(stringResource(R.string.medication_record_time), readOnly) },
                width = ColumnWidth.Fixed(if (readOnly) 96.dp else 112.dp),
            ) { record ->
                Text(
                    text = formatDateTime(record.timestamp),
                    style = if (readOnly) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                )
            },
            AppDataTableColumn<MedicationRecord>(
                key = "name",
                header = { MedicationTableHeader(stringResource(R.string.medication_record_name), readOnly) },
                width = ColumnWidth.Flex(weight = 1.6f, min = if (readOnly) 128.dp else 184.dp, max = 300.dp),
            ) { record -> MedicationTableText(record.medicationName, column.overflow, readOnly) },
            AppDataTableColumn<MedicationRecord>(
                key = "dose",
                header = { MedicationTableHeader(stringResource(R.string.medication_record_dose), readOnly) },
                width = ColumnWidth.Fixed(if (readOnly) 72.dp else 96.dp),
            ) { record -> MedicationTableText(formatDose(record), ColumnOverflow.Ellipsis, readOnly) },
            AppDataTableColumn<MedicationRecord>(
                key = "method",
                header = { MedicationTableHeader(stringResource(R.string.medication_record_method), readOnly) },
                width = ColumnWidth.Fixed(if (readOnly) 72.dp else 96.dp),
            ) { record -> MedicationTableText(record.method, ColumnOverflow.Ellipsis, readOnly) },
            AppDataTableColumn<MedicationRecord>(
                key = "feeling",
                header = { MedicationTableHeader(stringResource(R.string.medication_record_feeling), readOnly) },
                width = ColumnWidth.Flex(weight = 1f, min = if (readOnly) 112.dp else 168.dp, max = 320.dp),
                overflow = ColumnOverflow.Wrap,
            ) { record -> MedicationTableText(formatFeeling(record), column.overflow, readOnly) },
        ),
        layoutPolicy = AppDataTableLayoutPolicy.Responsive(
            compactAt = 600.dp,
            compactHeader = if (onDelete != null) {
                {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppDataTableHeaderText(stringResource(R.string.medication_record_time), Modifier.width(108.dp))
                    AppDataTableHeaderText(stringResource(R.string.medication_record_name), Modifier.weight(1f))
                    AppDataTableHeaderText(stringResource(R.string.medication_record_actions))
                }
                }
            } else {
                {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AppDataTableHeaderText(stringResource(R.string.medication_record_time), Modifier.width(108.dp))
                        AppDataTableHeaderText(stringResource(R.string.medication_record_name), Modifier.weight(1f))
                    }
                }
            },
            compactRow = { record -> CompactMedicationRow(record, onDelete) },
        ),
        actionsWidth = if (onDelete != null) 76.dp else 0.dp,
        actionsHeader = onDelete?.let { { AppDataTableHeaderText(stringResource(R.string.medication_record_actions)) } },
        rowActions = onDelete?.let { delete ->
            { record ->
                AppDataTableDeleteAction(
                    text = stringResource(R.string.body_record_delete),
                    onClick = { delete(record) },
                    iconSize = 14.dp,
                    textStyle = MaterialTheme.typography.labelSmall,
                )
            }
        },
        onRowClick = onEdit,
    )
}

@Composable
private fun MedicationTableHeader(text: String, compact: Boolean) {
    if (compact) Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    else AppDataTableHeaderText(text)
}

@Composable
private fun MedicationTableText(text: String, overflow: ColumnOverflow, compact: Boolean) {
    if (!compact) {
        AppDataTableText(text, overflow = overflow)
        return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = if (overflow == ColumnOverflow.Wrap) Int.MAX_VALUE else 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

@Composable
private fun CompactMedicationRow(record: MedicationRecord, onDelete: ((MedicationRecord) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDateTime(record.timestamp),
                modifier = Modifier.width(108.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            AppDataTableText(record.medicationName, modifier = Modifier.weight(1f))
            onDelete?.let { delete ->
                AppDataTableDeleteAction(
                    text = stringResource(R.string.body_record_delete),
                    onClick = { delete(record) },
                    iconSize = 14.dp,
                    textStyle = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(108.dp))
            Text(
                text = listOf(formatDose(record), record.method).filter { it.isNotBlank() }.joinToString(" / "),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        formatFeeling(record).takeIf { it.isNotBlank() }?.let { feeling ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(108.dp))
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

private fun formatDose(record: MedicationRecord): String = record.formatDose(Locale.getDefault())

private fun formatFeeling(record: MedicationRecord): String =
    listOf(record.feelings.joinToString(", "), record.feelingNote)
        .filter { it.isNotBlank() }
        .joinToString(" / ")

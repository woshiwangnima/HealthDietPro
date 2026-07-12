package com.woshiwangnima.healthdietpro.ui.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableLayoutPolicy
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.ColumnOverflow
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.ui.record.MedicationRecordActivity.Companion.EXTRA_RECORD_ID
import com.woshiwangnima.healthdietpro.util.showConfirmDialog
import com.woshiwangnima.healthdietpro.util.time.DateTimePicker

class MedicationListFragment : Fragment() {

    var onRecordsChanged: (() -> Unit)? = null

    private var refreshComposeList: (() -> Unit)? = null

    private val medicationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            refreshList()
            onRecordsChanged?.invoke()
        }
    }

    companion object {
        fun newInstance(): MedicationListFragment {
            return MedicationListFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = createComposeListView()

    fun refreshList() {
        refreshComposeList?.invoke()
    }

    private fun createComposeListView(): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HealthDietProTheme {
                    var refreshVersion by remember { mutableIntStateOf(0) }
                    refreshComposeList = { refreshVersion++ }
                    val records = remember(refreshVersion) {
                        MedicationPrefs.getRecords(requireContext()).sortedByDescending { it.timestamp }
                    }
                    MedicationRecordTable(
                        records = records,
                        onAdd = { openRecord(null) },
                        onEdit = { openRecord(it) },
                        onDelete = { confirmDelete(it) },
                    )
                }
            }
        }
    }

    private fun openRecord(record: MedicationRecord?) {
        val intent = Intent(requireContext(), MedicationRecordActivity::class.java).apply {
            if (record != null) {
                putExtra(EXTRA_RECORD_ID, record.id)
            }
        }
        medicationLauncher.launch(intent)
    }

    private fun confirmDelete(record: MedicationRecord) {
        requireContext().showConfirmDialog(
            getString(R.string.medication_record_delete_confirm_title),
            getString(R.string.medication_record_delete_confirm_message, record.medicationName)
        ) {
            val all = MedicationPrefs.getRecords(requireContext()).toMutableList()
            all.removeAll { it.id == record.id }
            MedicationPrefs.saveRecords(requireContext(), all)
            refreshList()
            onRecordsChanged?.invoke()
        }
    }
}

@Composable
private fun MedicationRecordTable(
    records: List<MedicationRecord>,
    onAdd: () -> Unit,
    onEdit: (MedicationRecord) -> Unit,
    onDelete: (MedicationRecord) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
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
            AppDataTable(
                rows = records,
                rowKey = { _, record -> record.id },
                columns = listOf(
                    AppDataTableColumn<MedicationRecord>(
                        key = "time",
                        header = { AppDataTableHeaderText(stringResource(R.string.medication_record_time)) },
                        width = ColumnWidth.Fixed(136.dp),
                    ) { record ->
                        AppDataTableText(DateTimePicker.format(record.timestamp))
                    },
                    AppDataTableColumn<MedicationRecord>(
                        key = "name",
                        header = { AppDataTableHeaderText(stringResource(R.string.medication_record_name)) },
                        width = ColumnWidth.Flex(weight = 1.4f, min = 160.dp, max = 260.dp),
                    ) { record ->
                        AppDataTableText(record.medicationName, overflow = column.overflow)
                    },
                    AppDataTableColumn<MedicationRecord>(
                        key = "dose",
                        header = { AppDataTableHeaderText(stringResource(R.string.medication_record_dose)) },
                        width = ColumnWidth.Fixed(96.dp),
                    ) { record ->
                        AppDataTableText(formatDose(record))
                    },
                    AppDataTableColumn<MedicationRecord>(
                        key = "method",
                        header = { AppDataTableHeaderText(stringResource(R.string.medication_record_method)) },
                        width = ColumnWidth.Fixed(96.dp),
                    ) { record ->
                        AppDataTableText(record.method)
                    },
                    AppDataTableColumn<MedicationRecord>(
                        key = "feeling",
                        header = { AppDataTableHeaderText(stringResource(R.string.medication_record_feeling)) },
                        width = ColumnWidth.Flex(weight = 1f, min = 168.dp, max = 320.dp),
                        overflow = ColumnOverflow.Wrap,
                    ) { record ->
                        AppDataTableText(formatFeeling(record), overflow = column.overflow)
                    },
                ),
                layoutPolicy = AppDataTableLayoutPolicy.Responsive(
                    compactAt = 600.dp,
                    compactHeader = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppDataTableHeaderText(
                                text = stringResource(R.string.medication_record_time),
                                modifier = Modifier.width(132.dp),
                            )
                            AppDataTableHeaderText(
                                text = stringResource(R.string.medication_record_name),
                                modifier = Modifier.weight(1f),
                            )
                            AppDataTableHeaderText(
                                text = stringResource(R.string.medication_record_actions),
                            )
                        }
                    },
                    compactRow = { record ->
                        CompactMedicationRow(
                            record = record,
                            onDelete = onDelete,
                        )
                    },
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
    }
}

@Composable
private fun CompactMedicationRow(
    record: MedicationRecord,
    onDelete: (MedicationRecord) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppDataTableText(
                text = DateTimePicker.format(record.timestamp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(132.dp),
            )
            AppDataTableText(
                text = record.medicationName,
                modifier = Modifier.weight(1f),
            )
            AppDataTableDeleteAction(
                text = stringResource(R.string.body_record_delete),
                onClick = { onDelete(record) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(132.dp))
            AppDataTableText(
                text = listOf(formatDose(record), record.method)
                    .filter { it.isNotBlank() }
                    .joinToString(" / "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        val feeling = formatFeeling(record)
        if (feeling.isNotBlank()) {
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
    if (record.doseValue > 0f || record.doseUnit.isNotBlank()) {
        "${record.doseValue}${record.doseUnit}"
    } else {
        ""
    }

private fun formatFeeling(record: MedicationRecord): String {
    val tags = record.feelings.joinToString(", ")
    return listOf(tags, record.feelingNote)
        .filter { it.isNotBlank() }
        .joinToString(" / ")
}

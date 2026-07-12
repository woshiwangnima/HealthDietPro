package com.woshiwangnima.healthdietpro.ui.profile.list

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.bodyRecordEpochMillis
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDateTime
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDisplayDateTime
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.showConfirmDialog
import com.woshiwangnima.healthdietpro.util.time.DateTimePicker

class DataListFragment : Fragment() {

    var onRecordsChanged: (() -> Unit)? = null
    var records: MutableList<BodyRecord> = mutableListOf()
    private var unitId: String = UnitCategoryType.Length.defaultUnitId
    private var category: String = UnitCategoryType.Length.id
    private var isHeight: Boolean = true
    private var refreshComposeTable: (() -> Unit)? = null

    companion object {
        private const val ARG_RECORDS = "records"
        private const val ARG_UNIT = "unit"
        private const val ARG_CATEGORY = "category"
        private const val ARG_IS_HEIGHT = "is_height"

        fun newInstance(records: ArrayList<BodyRecord>, unit: String, category: String, isHeight: Boolean): DataListFragment {
            val fragment = DataListFragment()
            fragment.arguments = Bundle().apply {
                putSerializable(ARG_RECORDS, records)
                putString(ARG_UNIT, unit)
                putString(ARG_CATEGORY, category)
                putBoolean(ARG_IS_HEIGHT, isHeight)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            records = (it.getSerializable(ARG_RECORDS, ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList()
                ?: mutableListOf()
            unitId = it.getString(ARG_UNIT, UnitCategoryType.Length.defaultUnitId) ?: UnitCategoryType.Length.defaultUnitId
            category = it.getString(ARG_CATEGORY, UnitCategoryType.Length.id) ?: UnitCategoryType.Length.id
            isHeight = it.getBoolean(ARG_IS_HEIGHT, true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                HealthDietProTheme {
                    var refreshVersion by remember { mutableIntStateOf(0) }
                    var editPosition by remember { mutableStateOf<Int?>(null) }
                    refreshComposeTable = { refreshVersion++ }
                    val rows = remember(refreshVersion) {
                        records.sortedByDescending { bodyRecordEpochMillis(it.date) }
                    }
                    BodyRecordTable(
                        rows = rows,
                        unitId = unitId,
                        category = category,
                        onAdd = { editPosition = -1 },
                        onEdit = { record ->
                            val pos = records.indexOf(record)
                            if (pos >= 0) editPosition = pos
                        },
                        onDelete = { record ->
                            requireContext().showConfirmDialog(
                                getString(R.string.body_record_delete_confirm_title),
                                getString(R.string.body_record_delete_confirm_message),
                            ) {
                                val pos = records.indexOf(record)
                                if (pos >= 0) records.removeAt(pos)
                                refreshList()
                                onRecordsChanged?.invoke()
                            }
                        },
                    )
                    editPosition?.let { position ->
                        BodyRecordEditBottomSheet(
                            editing = records.getOrNull(position),
                            isHeight = isHeight,
                            unitId = unitId,
                            category = category,
                            onPickTime = { current, onPicked ->
                                DateTimePicker.show(requireContext(), bodyRecordEpochMillis(current)) { millis ->
                                    onPicked(DateTimePicker.format(millis))
                                }
                            },
                            onDismiss = { editPosition = null },
                            onSave = { date, valueText ->
                                val saved = saveBodyRecordFromSheet(
                                    context = requireContext(),
                                    editPosition = position,
                                    date = date,
                                    valueText = valueText,
                                )
                                if (saved) editPosition = null
                            },
                        )
                    }
                }
            }
        }
    }

    private fun refreshList() {
        records.sortByDescending { bodyRecordEpochMillis(it.date) }
        refreshComposeTable?.invoke()
    }

    private fun saveBodyRecordFromSheet(
        context: Context,
        editPosition: Int,
        date: String,
        valueText: String,
    ): Boolean {
        if (valueText.isBlank()) {
            Toast.makeText(context, R.string.body_record_value_required, Toast.LENGTH_SHORT).show()
            return false
        }
        val value = valueText.toFloatOrNull()
        if (value == null || value <= 0f) {
            Toast.makeText(context, R.string.body_record_value_invalid, Toast.LENGTH_SHORT).show()
            return false
        }
        val baseValue = UnitConverter.toBase(category, value, unitId)
        val record = BodyRecord(date = date, value = baseValue, unit = unitId)
        if (editPosition >= 0) {
            records[editPosition] = record
        } else {
            records.add(record)
        }
        refreshList()
        onRecordsChanged?.invoke()
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyRecordEditBottomSheet(
    editing: BodyRecord?,
    isHeight: Boolean,
    unitId: String,
    category: String,
    onPickTime: (String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        BodyRecordEditSheet(
            isEdit = editing != null,
            isHeight = isHeight,
            unitId = unitId,
            initialDate = editing?.date ?: formatBodyRecordDateTime(java.time.LocalDateTime.now()),
            initialValue = editing?.let {
                "%.1f".format(UnitConverter.fromBase(category, it.value, unitId))
            }.orEmpty(),
            onPickTime = onPickTime,
            onDismiss = onDismiss,
            onSave = onSave,
        )
    }
}

@Composable
private fun BodyRecordTable(
    rows: List<BodyRecord>,
    unitId: String,
    category: String,
    onAdd: () -> Unit,
    onEdit: (BodyRecord) -> Unit,
    onDelete: (BodyRecord) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.body_record_count, rows.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AppIconTextButton(
                text = stringResource(R.string.body_record_add),
                iconRes = R.drawable.ic_add,
                onClick = onAdd,
            )
        }
        AppDataTable(
            rows = rows,
            rowKey = { index, record -> "${record.date}_${record.value}_${record.unit}_$index" },
            columns = listOf(
                AppDataTableColumn<BodyRecord>(
                    key = "time",
                    header = { AppDataTableHeaderText(stringResource(R.string.body_record_time)) },
                    width = ColumnWidth.Fixed(156.dp),
                ) { record ->
                    AppDataTableText(formatBodyRecordDisplayDateTime(record.date))
                },
                AppDataTableColumn<BodyRecord>(
                    key = "value",
                    header = { AppDataTableHeaderText(stringResource(R.string.body_record_value)) },
                    width = ColumnWidth.Fixed(120.dp),
                ) { record ->
                    val displayValue = UnitConverter.fromBase(category, record.value, unitId)
                    AppDataTableText("%.1f".format(displayValue))
                },
                AppDataTableColumn<BodyRecord>(
                    key = "unit",
                    header = { AppDataTableHeaderText(stringResource(R.string.body_record_unit)) },
                    width = ColumnWidth.Fixed(96.dp),
                ) {
                    AppDataTableText(unitId)
                },
            ),
            actionsWidth = 104.dp,
            actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
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

@Composable
private fun BodyRecordEditSheet(
    isEdit: Boolean,
    isHeight: Boolean,
    unitId: String,
    initialDate: String,
    initialValue: String,
    onPickTime: (String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var date by rememberSaveable(initialDate) { mutableStateOf(initialDate) }
    var value by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    val title = when {
        isEdit && isHeight -> stringResource(R.string.body_record_edit_height_title)
        isEdit -> stringResource(R.string.body_record_edit_weight_title)
        isHeight -> stringResource(R.string.body_record_add_height_title)
        else -> stringResource(R.string.body_record_add_weight_title)
    }
    val help = stringResource(
        if (isHeight) R.string.body_record_height_help else R.string.body_record_weight_help,
        unitId,
    )

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AppFormSubtitle(text = help)
        Text(
            text = stringResource(R.string.body_record_time),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
                .clickable { onPickTime(date) { date = it } }
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { AppInputLabel(stringResource(R.string.body_record_value)) },
            colors = AppInputTextFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.body_record_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
            AppIconTextButton(
                text = stringResource(if (isEdit) R.string.body_record_save else R.string.body_record_add),
                iconRes = if (isEdit) R.drawable.ic_save else R.drawable.ic_add,
                onClick = { onSave(date, value) },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

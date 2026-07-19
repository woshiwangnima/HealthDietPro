package com.woshiwangnima.healthdietpro.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.DirtyFormActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.formatDateTime
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.bodyRecordEpochMillis
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDateTime
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.time.LocalDateTime
import java.util.Locale

class BodyMetricRecordActivity : DirtyFormActivity() {
    private var isHeight = true
    private var unitId = "cm"
    private var category = "length"
    private var position = -1
    private var editing: BodyRecord? = null
    private var form by mutableStateOf(BodyMetricRecordForm())
    private var originalForm = BodyMetricRecordForm()

    override fun getTitleText(): String = getString(titleRes())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isHeight = intent.getBooleanExtra(EXTRA_IS_HEIGHT, true)
        unitId = intent.getStringExtra(EXTRA_UNIT_ID) ?: if (isHeight) "cm" else "kg"
        category = intent.getStringExtra(EXTRA_CATEGORY) ?: if (isHeight) "length" else "weight"
        position = intent.getIntExtra(EXTRA_POSITION, -1)
        @Suppress("DEPRECATION")
        editing = intent.getSerializableExtra(EXTRA_RECORD) as? BodyRecord
        form = initialForm()
        originalForm = form
        setContent {
            HealthDietProTheme {
                BaseScreen(title = stringResource(titleRes()), onBack = ::requestFormExit) { padding ->
                    BodyMetricRecordScreen(form, padding, isHeight, category, ::updateForm, ::saveRecord)
                }
                DiscardChangesConfirmation()
            }
        }
    }

    private fun initialForm(): BodyMetricRecordForm {
        val options = bodyMetricUnitOptions(category, Locale.getDefault().language == "zh")
        val selectedUnit = editing?.getUnit(!isHeight)?.takeIf { selected -> options.any { it.id == selected } } ?: unitId
        return BodyMetricRecordForm(
            date = editing?.date ?: formatBodyRecordDateTime(LocalDateTime.now()),
            unitId = selectedUnit,
            value = editing?.let { "%.1f".format(UnitConverter.fromBase(category, it.value, selectedUnit)) }.orEmpty(),
        )
    }

    private fun updateForm(updated: BodyMetricRecordForm) {
        form = updated
    }

    private fun saveRecord() {
        val value = form.value.toFloatOrNull()
        when {
            form.value.isBlank() -> Toast.makeText(this, R.string.body_record_value_required, Toast.LENGTH_SHORT).show()
            value == null || value <= 0f -> Toast.makeText(this, R.string.body_record_value_invalid, Toast.LENGTH_SHORT).show()
            else -> {
                setResult(
                    Activity.RESULT_OK,
                    Intent()
                        .putExtra(EXTRA_POSITION, position)
                        .putExtra(EXTRA_RECORD, BodyRecord(form.date, UnitConverter.toBase(category, value, form.unitId), form.unitId)),
                )
                finish()
            }
        }
    }

    override fun hasUnsavedChanges(): Boolean = form != originalForm

    override fun saveFormChanges() = saveRecord()

    private fun titleRes(): Int = when {
        editing != null && isHeight -> R.string.body_record_edit_height_title
        editing != null -> R.string.body_record_edit_weight_title
        isHeight -> R.string.body_record_add_height_title
        else -> R.string.body_record_add_weight_title
    }

    companion object {
        const val EXTRA_IS_HEIGHT = "is_height"
        const val EXTRA_UNIT_ID = "unit_id"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_POSITION = "position"
        const val EXTRA_RECORD = "record"
    }
}

private data class BodyMetricRecordForm(
    val date: String = "",
    val value: String = "",
    val unitId: String = "",
)

@Composable
private fun BodyMetricRecordScreen(
    form: BodyMetricRecordForm,
    contentPadding: PaddingValues,
    isHeight: Boolean,
    category: String,
    onFormChange: (BodyMetricRecordForm) -> Unit,
    onSave: () -> Unit,
) {
    val isChineseLocale = LocalConfiguration.current.locales[0]?.language == "zh"
    val options = bodyMetricUnitOptions(category, isChineseLocale)
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppFormSubtitle(stringResource(if (isHeight) R.string.body_record_height_help else R.string.body_record_weight_help, form.unitId))
            }
            item {
                Text(stringResource(R.string.body_record_time), style = MaterialTheme.typography.titleSmall)
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
                        .clickable { showDateTimePicker = true }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                ) {
                    Text(form.date, style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                OutlinedTextField(
                    value = form.value,
                    onValueChange = { onFormChange(form.copy(value = it)) },
                    label = { AppInputLabel(stringResource(R.string.body_record_value)) },
                    colors = AppInputTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                AppDropdownField(
                    label = stringResource(R.string.body_record_unit),
                    value = options.firstOrNull { it.id == form.unitId }?.label ?: form.unitId,
                    options = options,
                    onSelect = { selected ->
                        val converted = form.value.toFloatOrNull()?.let { value ->
                            "%.2f".format(UnitConverter.fromBase(category, UnitConverter.toBase(category, value, form.unitId), selected.id))
                        } ?: form.value
                        onFormChange(form.copy(value = converted, unitId = selected.id))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        FormSaveBar(
            text = stringResource(if (isHeight) R.string.body_record_save_height else R.string.body_record_save_weight),
            enabled = form.value.toFloatOrNull()?.let { it > 0f } == true,
            onSave = onSave,
        )
    }
    if (showDateTimePicker) {
        ComposeDateTimePickerDialog(
            initialMillis = bodyRecordEpochMillis(form.date),
            onDismiss = { showDateTimePicker = false },
            onDateTimePicked = {
                onFormChange(form.copy(date = formatDateTime(it)))
                showDateTimePicker = false
            },
        )
    }
}

private fun bodyMetricUnitOptions(category: String, isChineseLocale: Boolean): List<AppDropdownOption> {
    val ids = when {
        isChineseLocale && category == "length" -> listOf("cm", "m")
        isChineseLocale -> listOf("jin", "kg")
        category == "length" -> listOf("cm", "m", "ft", "in")
        else -> listOf("kg", "lb")
    }
    val units = UnitConverter.getRepository()?.getCategory(category)?.units.orEmpty()
    return ids.mapNotNull { id ->
        units.find { it.id == id && !it.hidden }?.let { AppDropdownOption(it.id, it.symbol(Locale.getDefault())) }
    }.ifEmpty { ids.map { AppDropdownOption(it, it) } }
}

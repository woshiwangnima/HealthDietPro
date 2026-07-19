package com.woshiwangnima.healthdietpro.ui.record

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.DirtyFormActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.HorizontalImageEditor
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.AppInfoDialog
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.model.medication.MedicationCatalogItem
import com.woshiwangnima.healthdietpro.model.medication.MedicationFrequency
import com.woshiwangnima.healthdietpro.model.medication.MedicationFrequencyUnit
import com.woshiwangnima.healthdietpro.model.medication.MedicationFrequencyType
import com.woshiwangnima.healthdietpro.model.medication.MedicationIntakeRule
import com.woshiwangnima.healthdietpro.model.medication.MedicationTimingAnchor
import com.woshiwangnima.healthdietpro.model.medication.defaultIntakeRules
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.io.File
import java.io.FileOutputStream

class MedicationCatalogActivity : DirtyFormActivity() {
    companion object { const val EXTRA_CATALOG_ID = "catalog_id" }
    override fun getTitleText(): String = getString(R.string.medication_catalog_heading)

    private var editingId: String? = null
    private var state by mutableStateOf(CatalogFormState())
    private var showExpiryDatePicker by mutableStateOf(false)
    private var originalState = CatalogFormState()
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            runCatching {
                contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) }
            }.getOrNull()?.let(::saveImage)
        }
    }
    private val categories: List<UnitCategory>
        get() = UnitConverter.getRepository()?.getCategories()
            ?.filter { it.id in setOf(UnitCategoryType.Weight.id, UnitCategoryType.Volume.id) }.orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(this)
        editingId = intent.getStringExtra(EXTRA_CATALOG_ID)
        editingId?.let { id -> MedicationPrefs.getCatalog(this).find { it.id == id } }?.let { item ->
            state = CatalogFormState(
                name = item.name, specValue = item.specValue.takeIf { it > 0f }?.toString().orEmpty(), categoryId = item.specUnitCategory, unitId = item.specUnitId,
                manufacturer = item.manufacturer, method = item.defaultMethod, defaultDoseValue = item.defaultDoseValue.takeIf { it > 0f }?.toString().orEmpty(), defaultDoseUnit = item.defaultDoseUnit,
                imagePaths = item.imagePaths, imageBitmaps = item.imagePaths.mapNotNull(::loadBitmap), frequency = item.frequency, intakeRules = item.intakeRules.ifEmpty { item.frequency.defaultIntakeRules() },
                packageQuantity = item.packageQuantity.takeIf { it > 0f }?.toString().orEmpty(), packageUnit = item.packageUnit, packageDescription = item.packageDescription,
                lotNumber = item.lotNumber, expiryAt = item.expiresAt, indicationTags = item.indicationTags.joinToString(", "), sideEffectWarning = item.sideEffectWarning, archived = item.archived,
            )
        }
        originalState = state
        setContent { HealthDietProTheme { BaseScreen(getTitleText(), ::requestFormExit) { padding ->
            CatalogEditor(state, padding, categories, { state = it }, { galleryLauncher.launch("image/*") }, { showExpiryDatePicker = true }, ::save, state != originalState)
        }; if (showExpiryDatePicker) ComposeDatePickerDialog(state.expiryAt ?: System.currentTimeMillis(), { showExpiryDatePicker = false }) { date -> state = state.copy(expiryAt = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()); showExpiryDatePicker = false }; DiscardChangesConfirmation() } }
    }

    private fun saveImage(bitmap: Bitmap) {
        if (state.imageBitmaps.any { it.sameAs(bitmap) }) return
        val dir = File(filesDir, "medication_catalog_images").apply { mkdirs() }
        val name = "medicine_${System.currentTimeMillis()}.jpg"
        FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        state = state.copy(imagePaths = state.imagePaths + "medication_catalog_images/$name", imageBitmaps = state.imageBitmaps + bitmap)
    }
    private fun formatDate(timestamp: Long): String = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
    private fun loadBitmap(path: String): Bitmap? = File(filesDir, path).takeIf(File::exists)?.let { BitmapFactory.decodeFile(it.path) }
    private fun save() {
        if (state.name.trim().isEmpty()) { Toast.makeText(this, R.string.medication_catalog_name_required, Toast.LENGTH_SHORT).show(); return }
        MedicationPrefs.upsertCatalogItem(this, MedicationCatalogItem(
            id = editingId ?: "med_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}", name = state.name.trim(), specValue = state.specValue.toFloatOrNull() ?: 0f,
            specUnitCategory = state.categoryId, specUnitId = state.unitId, manufacturer = state.manufacturer.trim(), defaultMethod = state.method.trim(),
            defaultDoseValue = state.defaultDoseValue.toFloatOrNull() ?: 0f, defaultDoseUnit = state.defaultDoseUnit.trim(), imagePaths = state.imagePaths,
            frequency = state.frequency, intakeRules = state.intakeRules, packageQuantity = state.packageQuantity.toFloatOrNull() ?: 0f, packageUnit = state.packageUnit.trim(),
            packageDescription = state.packageDescription.trim(), lotNumber = state.lotNumber.trim(), expiresAt = state.expiryAt,
            indicationTags = state.indicationTags.split(',').map(String::trim).filter(String::isNotEmpty), sideEffectWarning = state.sideEffectWarning.trim(), archived = state.archived,
        ))
        setResult(Activity.RESULT_OK); finish()
    }
    override fun hasUnsavedChanges(): Boolean = state != originalState

    override fun saveFormChanges() = save()
}

private data class CatalogFormState(
    val name: String = "", val specValue: String = "", val categoryId: String = "", val unitId: String = "", val manufacturer: String = "", val method: String = "",
    val defaultDoseValue: String = "", val defaultDoseUnit: String = "", val imagePaths: List<String> = emptyList(), val imageBitmaps: List<Bitmap> = emptyList(), val frequency: MedicationFrequency = MedicationFrequency(), val intakeRules: List<MedicationIntakeRule> = MedicationFrequency().defaultIntakeRules(),
    val packageQuantity: String = "", val packageUnit: String = "", val packageDescription: String = "", val lotNumber: String = "", val expiryAt: Long? = null,
    val indicationTags: String = "", val sideEffectWarning: String = "", val archived: Boolean = false,
)

@androidx.compose.runtime.Composable
private fun CatalogEditor(state: CatalogFormState, padding: PaddingValues, categories: List<UnitCategory>, onChange: (CatalogFormState) -> Unit, onPickImage: () -> Unit, onPickExpiryDate: () -> Unit, onSave: () -> Unit, saveEnabled: Boolean) {
    var showArchiveHelp by androidx.compose.runtime.remember { mutableStateOf(false) }
    val category = categories.find { it.id == state.categoryId }
    val units = category?.units?.filter { !it.hidden }.orEmpty()
    Column(Modifier.fillMaxSize().padding(padding)) {
    LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.medication_catalog_basic), style = androidx.compose.material3.MaterialTheme.typography.titleSmall) }
        item { OutlinedTextField(state.name, { onChange(state.copy(name = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_name)) }, colors = AppInputTextFieldColors()) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.specValue, { onChange(state.copy(specValue = it)) }, Modifier.weight(1f), label = { AppInputLabel(stringResource(R.string.medication_catalog_specification)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = AppInputTextFieldColors(), singleLine = true)
            AppDropdownField(stringResource(R.string.medication_record_spec_category_select), category?.displayName().orEmpty(), categories.map { AppDropdownOption(it.id, it.displayName()) }, { onChange(state.copy(categoryId = it.id, unitId = categories.find { c -> c.id == it.id }?.baseUnit.orEmpty())) }, Modifier.weight(1f))
            AppDropdownField(stringResource(R.string.medication_record_spec_unit_select), units.find { it.id == state.unitId }?.symbol().orEmpty(), units.map { AppDropdownOption(it.id, it.symbol()) }, { onChange(state.copy(unitId = it.id)) }, Modifier.weight(1f), enabled = category != null)
        } }
        item { OutlinedTextField(state.manufacturer, { onChange(state.copy(manufacturer = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_manufacturer)) }, colors = AppInputTextFieldColors()) }
        item { Text(stringResource(R.string.medication_catalog_default_medication), style = androidx.compose.material3.MaterialTheme.typography.titleSmall) }
        item { AppDropdownField(stringResource(R.string.medication_catalog_default_method), state.method, stringArrayResource(R.array.medication_record_default_methods).map { AppDropdownOption(it, it) }, { onChange(state.copy(method = it.id)) }) }
        item { Text(stringResource(R.string.medication_catalog_default_dose), style = androidx.compose.material3.MaterialTheme.typography.titleSmall) }
        item { androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.defaultDoseValue, { onChange(state.copy(defaultDoseValue = it)) }, Modifier.weight(1f), label = { AppInputLabel(stringResource(R.string.medication_record_dose_value_hint)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = AppInputTextFieldColors(), singleLine = true)
            OutlinedTextField(state.defaultDoseUnit, { onChange(state.copy(defaultDoseUnit = it)) }, Modifier.weight(1f), label = { AppInputLabel(stringResource(R.string.medication_record_dose_unit_hint)) }, colors = AppInputTextFieldColors(), singleLine = true)
        } }
        item { FrequencyEditor(state.frequency) { frequency -> onChange(state.copy(frequency = frequency, intakeRules = frequency.defaultIntakeRules())) } }
        item { IntakeRulesEditor(state.intakeRules) { onChange(state.copy(intakeRules = it)) } }
        item { Text(stringResource(R.string.medication_catalog_inventory), style = androidx.compose.material3.MaterialTheme.typography.titleSmall) }
        item { androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(state.packageQuantity, { onChange(state.copy(packageQuantity = it)) }, Modifier.weight(1f), label = { AppInputLabel(stringResource(R.string.medication_catalog_package_quantity)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = AppInputTextFieldColors(), singleLine = true)
            OutlinedTextField(state.packageUnit, { onChange(state.copy(packageUnit = it)) }, Modifier.weight(1f), label = { AppInputLabel(stringResource(R.string.medication_catalog_package_unit)) }, colors = AppInputTextFieldColors(), singleLine = true)
        } }
        item { OutlinedTextField(state.packageDescription, { onChange(state.copy(packageDescription = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_package_description)) }, colors = AppInputTextFieldColors()) }
        item { OutlinedTextField(state.lotNumber, { onChange(state.copy(lotNumber = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_lot_number)) }, colors = AppInputTextFieldColors()) }
        item { androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)).clickable(onClick = onPickExpiryDate).padding(12.dp)) { Text(state.expiryAt?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString() } ?: stringResource(R.string.medication_catalog_expiry_date)) } }
        item { Text(stringResource(R.string.medication_catalog_clinical), style = androidx.compose.material3.MaterialTheme.typography.titleSmall) }
        item { OutlinedTextField(state.indicationTags, { onChange(state.copy(indicationTags = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_indications)) }, colors = AppInputTextFieldColors()) }
        item { OutlinedTextField(state.sideEffectWarning, { onChange(state.copy(sideEffectWarning = it)) }, Modifier.fillMaxWidth(), label = { AppInputLabel(stringResource(R.string.medication_catalog_side_effect_warning)) }, colors = AppInputTextFieldColors(), minLines = 2) }
        item { Text(stringResource(R.string.medication_catalog_other), style = androidx.compose.material3.MaterialTheme.typography.titleSmall) }
        item { HorizontalImageEditor(state.imageBitmaps, onPickImage, { index -> onChange(state.copy(imagePaths = state.imagePaths.filterIndexed { i, _ -> i != index }, imageBitmaps = state.imageBitmaps.filterIndexed { i, _ -> i != index })) }) }
        item { androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            FilterChip(state.archived, { onChange(state.copy(archived = !state.archived)) }, label = { Text(stringResource(R.string.medication_catalog_archive)) })
            IconButton(onClick = { showArchiveHelp = true }) { androidx.compose.material3.Icon(painterResource(R.drawable.ic_help), contentDescription = stringResource(R.string.medication_catalog_archive_help)) }
        } }
    }
    FormSaveBar(stringResource(R.string.medication_catalog_save), saveEnabled, onSave)
    }
    if (showArchiveHelp) {
        AppInfoDialog(stringResource(R.string.medication_catalog_archive), { showArchiveHelp = false }) {
            Text(stringResource(R.string.medication_catalog_archive_help))
        }
    }
}

@androidx.compose.runtime.Composable
private fun FrequencyEditor(value: MedicationFrequency, onChange: (MedicationFrequency) -> Unit) {
    val asNeeded = stringResource(R.string.medication_frequency_as_needed)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppDropdownField(
            label = stringResource(R.string.medication_catalog_frequency),
            value = if (value.type == MedicationFrequencyType.AS_NEEDED) asNeeded else stringResource(R.string.medication_frequency_regular),
            options = listOf(AppDropdownOption("regular", stringResource(R.string.medication_frequency_regular)), AppDropdownOption("needed", asNeeded)),
            onSelect = { onChange(value.copy(type = if (it.id == "needed") MedicationFrequencyType.AS_NEEDED else MedicationFrequencyType.SCHEDULED)) },
        )
        if (value.type == MedicationFrequencyType.SCHEDULED) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value.interval.toString(), { input -> input.toIntOrNull()?.takeIf { it > 0 }?.let { onChange(value.copy(interval = it)) } }, Modifier.weight(1f), label = { AppInputLabel(stringResource(R.string.medication_frequency_interval)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = AppInputTextFieldColors())
            AppDropdownField(stringResource(R.string.medication_frequency_unit), when (value.unit) { MedicationFrequencyUnit.MINUTE -> stringResource(R.string.medication_frequency_minutes); MedicationFrequencyUnit.HOUR -> stringResource(R.string.medication_frequency_hours); MedicationFrequencyUnit.DAY -> stringResource(R.string.medication_frequency_days) }, MedicationFrequencyUnit.entries.map { AppDropdownOption(it.name, when (it) { MedicationFrequencyUnit.MINUTE -> stringResource(R.string.medication_frequency_minutes); MedicationFrequencyUnit.HOUR -> stringResource(R.string.medication_frequency_hours); MedicationFrequencyUnit.DAY -> stringResource(R.string.medication_frequency_days) }) }, { onChange(value.copy(unit = MedicationFrequencyUnit.valueOf(it.id))) }, Modifier.weight(1f))
            OutlinedTextField(value.times.toString(), { input -> input.toIntOrNull()?.takeIf { it > 0 }?.let { onChange(value.copy(times = it)) } }, Modifier.weight(1f), label = { AppInputLabel(stringResource(R.string.medication_frequency_times)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = AppInputTextFieldColors())
        }
    }
}

@androidx.compose.runtime.Composable
private fun IntakeRulesEditor(value: List<MedicationIntakeRule>, onChange: (List<MedicationIntakeRule>) -> Unit) {
    val anchorOptions = MedicationTimingAnchor.entries.map { anchor -> AppDropdownOption(anchor.name, when (anchor) { MedicationTimingAnchor.BREAKFAST -> stringResource(R.string.medication_timing_breakfast); MedicationTimingAnchor.LUNCH -> stringResource(R.string.medication_timing_lunch); MedicationTimingAnchor.DINNER -> stringResource(R.string.medication_timing_dinner); MedicationTimingAnchor.WAKE_UP -> stringResource(R.string.medication_timing_wake_up); MedicationTimingAnchor.BEDTIME -> stringResource(R.string.medication_timing_bedtime) }) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.medication_catalog_timing), style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        value.forEachIndexed { index, rule ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppDropdownField(stringResource(R.string.medication_timing_anchor), anchorOptions.first { it.id == rule.anchor.name }.label, anchorOptions, { option -> onChange(value.toMutableList().apply { set(index, rule.copy(anchor = MedicationTimingAnchor.valueOf(option.id))) }) }, Modifier.weight(1f))
                IntakeOffsetMinutesField(rule, { updated -> onChange(value.toMutableList().apply { set(index, updated) }) }, Modifier.weight(1f))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun IntakeOffsetMinutesField(
    rule: MedicationIntakeRule,
    onChange: (MedicationIntakeRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember(rule.anchor) { mutableStateOf(rule.offsetMinutes.toString()) }
    val isValid = input.toIntOrNull() != null
    OutlinedTextField(
        value = input,
        onValueChange = { text ->
            input = text
            text.toIntOrNull()?.let { minutes -> onChange(rule.copy(offsetMinutes = minutes)) }
        },
        modifier = modifier,
        label = { AppInputLabel(stringResource(R.string.medication_timing_offset_minutes)) },
        supportingText = { Text(stringResource(R.string.medication_timing_offset_minutes_help)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        isError = input.isNotEmpty() && !isValid,
        singleLine = true,
        colors = AppInputTextFieldColors(),
    )
}

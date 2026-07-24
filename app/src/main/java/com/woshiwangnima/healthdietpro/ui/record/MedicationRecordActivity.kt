package com.woshiwangnima.healthdietpro.ui.record

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.DirtyFormActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.HorizontalImageEditor
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.formatDateTime
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.medication.MedicationCatalogItem
import com.woshiwangnima.healthdietpro.model.medication.formatSelectionDetails
import com.woshiwangnima.healthdietpro.model.medication.formatSpecification
import com.woshiwangnima.healthdietpro.model.medication.format
import com.woshiwangnima.healthdietpro.model.medication.defaultIntakeRules
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import com.woshiwangnima.healthdietpro.util.image.WatermarkUtil
import com.woshiwangnima.healthdietpro.util.location.CurrentLocationProvider
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.io.File
import java.io.FileOutputStream

class MedicationRecordActivity : DirtyFormActivity() {

    override fun getTitleText(): String = getString(R.string.medication_record_title)

    companion object {
        const val EXTRA_RECORD_ID = "record_id"
    }

    private var editingRecordId: String? = null
    private var formState by mutableStateOf(MedicationRecordFormState())
    private var originalFormState = MedicationRecordFormState()
    private var showDateTimePicker by mutableStateOf(false)
    private lateinit var locationProvider: CurrentLocationProvider
    private lateinit var provinceRepo: ProvinceRepository
    private var pendingCameraUri: Uri? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(
                this,
                getString(R.string.medication_record_camera_permission_required),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) {
            handleCapturedPhoto(uri)
        }
        pendingCameraUri = null
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach(::handlePickedPhoto) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationProvider = CurrentLocationProvider(this)
        provinceRepo = ProvinceRepository.fromContext(this)
        editingRecordId = intent.getStringExtra(EXTRA_RECORD_ID)
        formState = initialFormState(editingRecordId)
        originalFormState = formState

        setContent {
            HealthDietProTheme {
                BaseScreen(
                    title = if (editingRecordId == null) {
                        stringResource(R.string.medication_record_title)
                    } else {
                        stringResource(R.string.medication_record_edit_title)
                    },
                    onBack = ::requestFormExit,
                ) { innerPadding ->
                    MedicationRecordScreen(
                        state = formState,
                        contentPadding = innerPadding,
                        catalog = MedicationPrefs.getCatalog(this),
                        onStateChange = { formState = it },
                        onPickTime = { showDateTimePicker = true },
                        onTakePhoto = ::requestCamera,
                        onPickPhoto = { galleryLauncher.launch("image/*") },
                        onRemovePhoto = ::removePhoto,
                        onSave = ::saveRecord,
                        saveEnabled = formState != originalFormState,
                    )
                }
                if (showDateTimePicker) {
                    ComposeDateTimePickerDialog(
                        initialMillis = formState.timestamp,
                        onDismiss = { showDateTimePicker = false },
                        onDateTimePicked = { timestamp ->
                            formState = formState.copy(timestamp = timestamp)
                            showDateTimePicker = false
                        },
                    )
                }
                DiscardChangesConfirmation()
            }
        }
    }

    private fun initialFormState(recordId: String?): MedicationRecordFormState {
        val record = recordId?.let { id -> MedicationPrefs.getRecords(this).find { it.id == id } }
            ?: return MedicationRecordFormState()
        return MedicationRecordFormState(
            timestamp = record.timestamp,
            medicationName = record.medicationName,
            medicationId = record.medicationId,
            doseValue = record.doseValue.takeIf { it > 0f }?.toString().orEmpty(),
            doseUnit = record.doseUnit,
            specValue = record.specValue.takeIf { it > 0f }?.toString().orEmpty(),
            specCategoryId = record.specUnitCategory,
            specUnitId = record.specUnitId,
            method = record.method,
            manufacturer = record.manufacturer,
            medicationImagePaths = record.medicationImagePaths,
            selectedFeelings = record.feelings.toSet(),
            feelingNote = record.feelingNote,
            photoFileNames = record.recordPhotoPaths,
            photoBitmaps = record.recordPhotoPaths.mapNotNull(::loadBitmap),
            frequency = record.frequency,
            intakeRules = record.intakeRules.ifEmpty { record.frequency.defaultIntakeRules() },
            purposes = record.purposes,
            purposeOptions = record.purposes,
            sideEffectWarning = record.sideEffectWarning,
            lotNumber = record.lotNumber,
            expiresAt = record.expiresAt,
        )
    }

    private fun requestCamera() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val dir = File(filesDir, "medication_photos").apply { if (!exists()) mkdirs() }
        val tmpFile = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        pendingCameraUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            tmpFile,
        )
        pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    private fun handleCapturedPhoto(uri: Uri) {
        try {
            val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return
            locationProvider.getCurrentLocation { result ->
                runOnUiThread {
                    val locationText = when (result) {
                        is CurrentLocationProvider.Result.Ok -> {
                            provinceRepo.findByPoint(result.lng, result.lat)?.name
                                ?: getString(R.string.medication_record_location_unknown)
                        }

                        else -> ""
                    }
                    saveAndPreview(
                        WatermarkUtil.apply(
                            bitmap,
                            formState.timestamp,
                            locationText,
                        )
                    )
                    runCatching { File(uri.path ?: "").delete() }
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.medication_record_photo_load_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handlePickedPhoto(uri: Uri) {
        try {
            val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return
            saveAndPreview(bitmap)
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.medication_record_photo_load_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAndPreview(bitmap: Bitmap) {
        if (formState.photoBitmaps.any { it.sameAs(bitmap) }) return
        val dir = File(filesDir, "medication_photos").apply { if (!exists()) mkdirs() }
        val fileName = "med_${System.currentTimeMillis()}.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        formState = formState.copy(
            photoFileNames = formState.photoFileNames + "medication_photos/$fileName",
            photoBitmaps = formState.photoBitmaps + bitmap,
        )
    }

    private fun loadBitmap(relativePath: String): Bitmap? {
        val file = File(filesDir, relativePath)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private fun removePhoto(index: Int) {
        val path = formState.photoFileNames.getOrNull(index) ?: return
        File(filesDir, path).delete()
        formState = formState.copy(
            photoFileNames = formState.photoFileNames.filterIndexed { i, _ -> i != index },
            photoBitmaps = formState.photoBitmaps.filterIndexed { i, _ -> i != index },
        )
    }

    private fun saveRecord() {
        val state = formState
        val medicationId = state.medicationId
        if (medicationId == null && editingRecordId == null) {
            Toast.makeText(this, getString(R.string.medication_record_select_required), Toast.LENGTH_SHORT).show()
            return
        }

        val recordId = editingRecordId ?: "${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
        val record = MedicationRecord(
            id = recordId,
            timestamp = state.timestamp,
            medicationName = state.medicationName,
            doseValue = state.doseValue.toFloatOrNull() ?: 0f,
            doseUnit = state.doseUnit.trim(),
            specValue = state.specValue.toFloatOrNull() ?: 0f,
            specUnitCategory = state.specCategoryId,
            specUnitId = state.specUnitId,
            method = state.method.trim(),
            feelings = state.selectedFeelings.toList(),
            feelingNote = state.feelingNote.trim(),
            recordPhotoPaths = state.photoFileNames,
            medicationId = medicationId,
            manufacturer = state.manufacturer,
            medicationImagePaths = state.medicationImagePaths,
            frequency = state.frequency,
            intakeRules = state.intakeRules,
            purposes = state.purposes,
            sideEffectWarning = state.sideEffectWarning,
            lotNumber = state.lotNumber,
            expiresAt = state.expiresAt,
        )
        if (editingRecordId != null) {
            val all = MedicationPrefs.getRecords(this).toMutableList()
            val index = all.indexOfFirst { it.id == editingRecordId }
            if (index >= 0) all[index] = record else all.add(record)
            MedicationPrefs.saveRecords(this, all)
        } else {
            MedicationPrefs.addRecord(this, record)
        }
        Toast.makeText(this, getString(R.string.medication_record_saved), Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun hasUnsavedChanges(): Boolean = formState != originalFormState

    override fun saveFormChanges() = saveRecord()
}

private data class MedicationRecordFormState(
    val timestamp: Long = System.currentTimeMillis(),
    val medicationName: String = "",
    val medicationId: String? = null,
    val doseValue: String = "",
    val doseUnit: String = "",
    val specValue: String = "",
    val specCategoryId: String = "",
    val specUnitId: String = "",
    val method: String = "",
    val manufacturer: String = "",
    val selectedFeelings: Set<String> = emptySet(),
    val feelingNote: String = "",
    val medicationImagePaths: List<String> = emptyList(),
    val frequency: com.woshiwangnima.healthdietpro.model.medication.MedicationFrequency = com.woshiwangnima.healthdietpro.model.medication.MedicationFrequency(),
    val intakeRules: List<com.woshiwangnima.healthdietpro.model.medication.MedicationIntakeRule> = emptyList(),
    val purposes: List<String> = emptyList(),
    val purposeOptions: List<String> = emptyList(),
    val sideEffectWarning: String = "",
    val lotNumber: String = "",
    val expiresAt: Long? = null,
    val photoFileNames: List<String> = emptyList(),
    val photoBitmaps: List<Bitmap> = emptyList(),
)

@Composable
private fun MedicationRecordScreen(
    state: MedicationRecordFormState,
    contentPadding: PaddingValues,
    catalog: List<MedicationCatalogItem>,
    onStateChange: (MedicationRecordFormState) -> Unit,
    onPickTime: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onSave: () -> Unit,
    saveEnabled: Boolean,
) {
    var safetyWarning by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    val expiredWarning = stringResource(R.string.medication_record_expired_warning)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TimeField(
                    timestamp = state.timestamp,
                    onClick = onPickTime,
                )
            }
            item {
                AppDropdownField(
                    label = stringResource(R.string.medication_record_name),
                    value = state.medicationName,
                    options = catalog.filter { !it.archived || it.id == state.medicationId }.map { item ->
                        AppDropdownOption(
                            id = item.id,
                            label = item.name,
                            secondaryLabel = item.formatSelectionDetails(UnitConverter.getRepository(), java.util.Locale.getDefault(), " / "),
                        )
                    },
                    showOptionDividers = true,
                    largeOptionText = true,
                    onSelect = { option -> catalog.find { it.id == option.id }?.let { item ->
                        val isExpired = item.expiresAt?.let { it < System.currentTimeMillis() } == true
                        val warning = listOfNotNull(if (isExpired) expiredWarning else null, item.sideEffectWarning.takeIf { it.isNotBlank() }).joinToString("\n")
                        onStateChange(state.copy(medicationId = item.id, medicationName = item.name, doseValue = item.defaultDoseValue.takeIf { value -> value > 0f }?.toString().orEmpty(), doseUnit = item.defaultDoseUnit, specValue = item.specValue.takeIf { value -> value > 0f }?.toString().orEmpty(), specCategoryId = item.specUnitCategory, specUnitId = item.specUnitId, method = item.defaultMethod, manufacturer = item.manufacturer, medicationImagePaths = item.imagePaths, frequency = item.frequency, intakeRules = item.intakeRules.ifEmpty { item.frequency.defaultIntakeRules() }, purposes = item.indicationTags, purposeOptions = item.indicationTags, sideEffectWarning = item.sideEffectWarning, lotNumber = item.lotNumber, expiresAt = item.expiresAt))
                        if (warning.isNotBlank()) safetyWarning = warning
                    } },
                )
            }
            item {
                DoseSection(
                    state = state,
                    onStateChange = onStateChange,
                )
            }
            item {
                CatalogSnapshot(state)
            }
            if (state.purposes.isNotEmpty()) {
                item {
                    PurposeSection(state, onStateChange)
                }
            }
            item {
                FeelingSection(
                    state = state,
                    onStateChange = onStateChange,
                )
            }
            item {
                PhotoSection(
                    bitmaps = state.photoBitmaps,
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                    onRemovePhoto = onRemovePhoto,
                )
            }
        }
        FormSaveBar(stringResource(R.string.medication_record_save), saveEnabled, onSave)
    }
    safetyWarning?.let { warning ->
        AlertDialog(onDismissRequest = { safetyWarning = null }, title = { Text(stringResource(R.string.medication_record_safety_warning)) }, text = { Text(warning) }, confirmButton = { TextButton(onClick = { safetyWarning = null }) { Text(stringResource(R.string.test_access_confirm)) } })
    }
}

@Composable
private fun CatalogSnapshot(state: MedicationRecordFormState) {
    val spec = MedicationCatalogItem(
        id = "", name = "", specValue = state.specValue.toFloatOrNull() ?: 0f,
        specUnitCategory = state.specCategoryId, specUnitId = state.specUnitId,
    ).formatSpecification(UnitConverter.getRepository(), java.util.Locale.getDefault())
    val locale = java.util.Locale.getDefault()
    val content = listOf(spec, state.manufacturer, state.method, state.frequency.format(locale), state.intakeRules.format(locale), state.purposes.joinToString(", "), state.lotNumber).filter { it.isNotBlank() }.joinToString(" / ")
    Text(text = stringResource(R.string.medication_record_catalog_snapshot, content), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PurposeSection(
    state: MedicationRecordFormState,
    onStateChange: (MedicationRecordFormState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.medication_record_purpose), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.purposeOptions.forEach { purpose ->
                FilterChip(
                    selected = purpose in state.purposes,
                    onClick = {
                        val purposes = if (purpose in state.purposes) state.purposes - purpose else state.purposes + purpose
                        onStateChange(state.copy(purposes = purposes))
                    },
                    label = { Text(purpose) },
                )
            }
        }
        AppFormSubtitle(stringResource(R.string.medication_record_purpose_help))
    }
}

@Composable
private fun TimeField(
    timestamp: Long,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.medication_record_time),
            style = MaterialTheme.typography.titleSmall,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Text(
                text = formatDateTime(timestamp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DoseSection(
    state: MedicationRecordFormState,
    onStateChange: (MedicationRecordFormState) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.medication_record_dose_section),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.doseValue,
                onValueChange = { onStateChange(state.copy(doseValue = it)) },
                label = { AppInputLabel(stringResource(R.string.medication_record_dose_value_hint)) },
                colors = AppInputTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.doseUnit,
                onValueChange = {},
                label = { AppInputLabel(stringResource(R.string.medication_record_dose_unit_hint)) },
                colors = AppInputTextFieldColors(), readOnly = true,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        AppFormSubtitle(text = stringResource(R.string.medication_record_dose_help))
    }
}

@Composable
private fun FeelingSection(
    state: MedicationRecordFormState,
    onStateChange: (MedicationRecordFormState) -> Unit,
) {
    val defaultFeelings = stringArrayResource(R.array.medication_record_default_feelings).toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.medication_record_feeling_section),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            defaultFeelings.forEach { label ->
                val selected = label in state.selectedFeelings
                FilterChip(
                    selected = selected,
                    onClick = {
                        val next = if (selected) {
                            state.selectedFeelings - label
                        } else {
                            state.selectedFeelings + label
                        }
                        onStateChange(state.copy(selectedFeelings = next))
                    },
                    label = { Text(label) },
                )
            }
        }
        OutlinedTextField(
            value = state.feelingNote,
            onValueChange = { onStateChange(state.copy(feelingNote = it)) },
            label = { AppInputLabel(stringResource(R.string.medication_record_feeling_note_hint)) },
            colors = AppInputTextFieldColors(),
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PhotoSection(
    bitmaps: List<Bitmap>,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.medication_record_photo_section),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.medication_record_take_photo),
                iconRes = R.drawable.ic_camera,
                onClick = onTakePhoto,
                modifier = Modifier.weight(1f),
            )
            AppOutlinedIconTextButton(
                text = stringResource(R.string.medication_record_pick_photo),
                iconRes = R.drawable.ic_photo,
                onClick = onPickPhoto,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalImageEditor(bitmaps, onPickPhoto, onRemovePhoto)
    }
}

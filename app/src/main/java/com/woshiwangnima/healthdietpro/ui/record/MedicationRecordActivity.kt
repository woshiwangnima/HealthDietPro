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
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppEditableDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppFormSubtitle
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.image.WatermarkUtil
import com.woshiwangnima.healthdietpro.util.location.CurrentLocationProvider
import com.woshiwangnima.healthdietpro.util.time.DateTimePicker
import java.io.File
import java.io.FileOutputStream

class MedicationRecordActivity : BaseBackActivity() {

    override fun getTitleText(): String = getString(R.string.medication_record_title)

    companion object {
        const val EXTRA_RECORD_ID = "record_id"
    }

    private var editingRecordId: String? = null
    private var formState by mutableStateOf(MedicationRecordFormState())
    private lateinit var locationProvider: CurrentLocationProvider
    private lateinit var provinceRepo: ProvinceRepository
    private var pendingCameraUri: Uri? = null

    private val specCategories: List<UnitCategory>
        get() {
            val ids = setOf(UnitCategoryType.Weight.id, UnitCategoryType.Volume.id)
            return UnitConverter.getRepository()
                ?.getCategories()
                ?.filter { it.id in ids }
                .orEmpty()
        }

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
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { handlePickedPhoto(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(this)
        locationProvider = CurrentLocationProvider(this)
        provinceRepo = ProvinceRepository.fromContext(this)
        editingRecordId = intent.getStringExtra(EXTRA_RECORD_ID)
        formState = initialFormState(editingRecordId)

        setContent {
            HealthDietProTheme {
                BaseScreen(
                    title = if (editingRecordId == null) {
                        stringResource(R.string.medication_record_title)
                    } else {
                        stringResource(R.string.medication_record_edit_title)
                    },
                    onBack = { finish() },
                ) { innerPadding ->
                    MedicationRecordScreen(
                        state = formState,
                        contentPadding = innerPadding,
                        specCategories = specCategories,
                        medicationHistory = MedicationPrefs.getMedicationNameHistory(this),
                        methodHistory = MedicationPrefs.getMethodHistory(this),
                        onStateChange = { formState = it },
                        onApplyNameDefaults = ::applyNameDefaults,
                        onPickTime = ::pickTime,
                        onTakePhoto = ::requestCamera,
                        onPickPhoto = { galleryLauncher.launch("image/*") },
                        onSave = ::saveRecord,
                    )
                }
            }
        }
    }

    private fun initialFormState(recordId: String?): MedicationRecordFormState {
        val record = recordId?.let { id -> MedicationPrefs.getRecords(this).find { it.id == id } }
            ?: return MedicationRecordFormState()
        val category = specCategories.find { it.id == record.specUnitCategory }
        val unitId = category?.units?.find { it.id == record.specUnitId }?.id ?: record.specUnitId
        return MedicationRecordFormState(
            timestamp = record.timestamp,
            medicationName = record.medicationName,
            doseValue = record.doseValue.takeIf { it > 0f }?.toString().orEmpty(),
            doseUnit = record.doseUnit,
            specValue = record.specValue.takeIf { it > 0f }?.toString().orEmpty(),
            specCategoryId = category?.id.orEmpty(),
            specUnitId = unitId,
            method = record.method,
            selectedFeelings = record.feelings.toSet(),
            feelingNote = record.feelingNote,
            photoFileName = record.photoPath,
            photoBitmap = record.photoPath?.let(::loadBitmap),
        )
    }

    private fun applyNameDefaults(name: String) {
        if (name.isBlank()) return
        val defaults = MedicationPrefs.findNameDefaults(this, name) ?: return
        val category = specCategories.find { it.id == defaults.specUnitCategory }
        val unitId = category?.units?.find { it.id == defaults.specUnitId }?.id ?: defaults.specUnitId
        formState = formState.copy(
            doseValue = defaults.doseValue.takeIf { it > 0f }?.toString().orEmpty(),
            doseUnit = defaults.doseUnit,
            specValue = defaults.specValue.takeIf { it > 0f }?.toString().orEmpty(),
            specCategoryId = category?.id.orEmpty(),
            specUnitId = unitId,
            method = defaults.method,
        )
    }

    private fun pickTime() {
        DateTimePicker.show(this, formState.timestamp) { timestamp ->
            formState = formState.copy(timestamp = timestamp)
        }
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
        val dir = File(filesDir, "medication_photos").apply { if (!exists()) mkdirs() }
        val fileName = "med_${System.currentTimeMillis()}.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        formState = formState.copy(
            photoFileName = "medication_photos/$fileName",
            photoBitmap = bitmap,
        )
    }

    private fun loadBitmap(relativePath: String): Bitmap? {
        val file = File(filesDir, relativePath)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private fun saveRecord() {
        val state = formState
        val name = state.medicationName.trim()
        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.medication_record_name_required), Toast.LENGTH_SHORT).show()
            return
        }

        val category = specCategories.find { it.id == state.specCategoryId }
        val unitId = category?.units?.find { it.id == state.specUnitId }?.id.orEmpty()
        val recordId = editingRecordId ?: "${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
        val record = MedicationRecord(
            id = recordId,
            timestamp = state.timestamp,
            medicationName = name,
            doseValue = state.doseValue.toFloatOrNull() ?: 0f,
            doseUnit = state.doseUnit.trim(),
            specValue = state.specValue.toFloatOrNull() ?: 0f,
            specUnitCategory = category?.id.orEmpty(),
            specUnitId = unitId,
            method = state.method.trim(),
            feelings = state.selectedFeelings.toList(),
            feelingNote = state.feelingNote.trim(),
            photoPath = state.photoFileName,
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
}

private data class MedicationRecordFormState(
    val timestamp: Long = System.currentTimeMillis(),
    val medicationName: String = "",
    val doseValue: String = "",
    val doseUnit: String = "",
    val specValue: String = "",
    val specCategoryId: String = "",
    val specUnitId: String = "",
    val method: String = "",
    val selectedFeelings: Set<String> = emptySet(),
    val feelingNote: String = "",
    val photoFileName: String? = null,
    val photoBitmap: Bitmap? = null,
)

@Composable
private fun MedicationRecordScreen(
    state: MedicationRecordFormState,
    contentPadding: PaddingValues,
    specCategories: List<UnitCategory>,
    medicationHistory: List<String>,
    methodHistory: List<String>,
    onStateChange: (MedicationRecordFormState) -> Unit,
    onApplyNameDefaults: (String) -> Unit,
    onPickTime: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onSave: () -> Unit,
) {
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
                AppEditableDropdownField(
                    title = stringResource(R.string.medication_record_name),
                    label = stringResource(R.string.medication_record_name_hint),
                    value = state.medicationName,
                    options = medicationHistory,
                    onValueChange = { onStateChange(state.copy(medicationName = it)) },
                    onSelect = {
                        onStateChange(state.copy(medicationName = it))
                        onApplyNameDefaults(it)
                    },
                )
            }
            item {
                DoseSection(
                    state = state,
                    onStateChange = onStateChange,
                )
            }
            item {
                SpecSection(
                    state = state,
                    categories = specCategories,
                    onStateChange = onStateChange,
                )
            }
            item {
                AppEditableDropdownField(
                    title = stringResource(R.string.medication_record_method),
                    label = stringResource(R.string.medication_record_method),
                    value = state.method,
                    options = methodHistory,
                    onValueChange = { onStateChange(state.copy(method = it)) },
                    onSelect = { onStateChange(state.copy(method = it)) },
                )
            }
            item {
                FeelingSection(
                    state = state,
                    onStateChange = onStateChange,
                )
            }
            item {
                PhotoSection(
                    bitmap = state.photoBitmap,
                    onTakePhoto = onTakePhoto,
                    onPickPhoto = onPickPhoto,
                )
            }
        }
        AppIconTextButton(
            text = stringResource(R.string.medication_record_save),
            iconRes = R.drawable.ic_save,
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
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
                text = DateTimePicker.format(timestamp),
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
                onValueChange = { onStateChange(state.copy(doseUnit = it)) },
                label = { AppInputLabel(stringResource(R.string.medication_record_dose_unit_hint)) },
                colors = AppInputTextFieldColors(),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        AppFormSubtitle(text = stringResource(R.string.medication_record_dose_help))
    }
}

@Composable
private fun SpecSection(
    state: MedicationRecordFormState,
    categories: List<UnitCategory>,
    onStateChange: (MedicationRecordFormState) -> Unit,
) {
    val selectedCategory = categories.find { it.id == state.specCategoryId }
    val unitOptions = selectedCategory
        ?.units
        ?.filter { !it.hidden }
        .orEmpty()
    val selectedUnit = unitOptions.find { it.id == state.specUnitId }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.medication_record_spec_section),
            style = MaterialTheme.typography.titleSmall,
        )
        OutlinedTextField(
            value = state.specValue,
            onValueChange = { onStateChange(state.copy(specValue = it)) },
            label = { AppInputLabel(stringResource(R.string.medication_record_spec_value_hint)) },
            colors = AppInputTextFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppDropdownField(
                label = stringResource(R.string.medication_record_spec_category_select),
                value = selectedCategory?.displayName().orEmpty(),
                options = categories.map { AppDropdownOption(id = it.id, label = it.displayName()) },
                onSelect = { option ->
                    val category = categories.find { it.id == option.id }
                    onStateChange(
                        state.copy(
                            specCategoryId = option.id,
                            specUnitId = category?.baseUnit.orEmpty(),
                        )
                    )
                },
                modifier = Modifier.weight(1f),
            )
            AppDropdownField(
                label = stringResource(R.string.medication_record_spec_unit_select),
                value = selectedUnit?.symbol().orEmpty(),
                options = unitOptions.map { AppDropdownOption(id = it.id, label = "${it.symbol()} (${it.id})") },
                onSelect = { option -> onStateChange(state.copy(specUnitId = option.id)) },
                enabled = selectedCategory != null,
                modifier = Modifier.weight(1f),
            )
        }
        AppFormSubtitle(text = stringResource(R.string.medication_record_spec_help))
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
    bitmap: Bitmap?,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
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
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

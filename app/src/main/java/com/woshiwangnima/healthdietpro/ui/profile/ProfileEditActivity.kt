package com.woshiwangnima.healthdietpro.ui.profile

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.DirtyFormActivity
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppInputLabel
import com.woshiwangnima.healthdietpro.common.ui.AppInputTextFieldColors
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.common.ui.AppTextIconButton
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.AppDate
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import com.woshiwangnima.healthdietpro.model.profile.bodyRecordEpochMillis
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import com.woshiwangnima.healthdietpro.model.region.Region
import com.woshiwangnima.healthdietpro.model.region.RegionRepository
import com.woshiwangnima.healthdietpro.model.region.RegionSnapshot
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.util.location.CurrentLocationProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class ProfileEditActivity : DirtyFormActivity() {

    private lateinit var diseaseRepo: DiseaseRepository
    private lateinit var provinceRepo: ProvinceRepository
    private lateinit var regionRepo: RegionRepository
    private lateinit var locationProvider: CurrentLocationProvider

    private var selectedBirthday: AppDate? = null
    private var selectedRegion: RegionSnapshot = RegionSnapshot()
    private var selectedDiseaseIds: MutableList<String> = mutableListOf()
    private var selectedGender: Gender = Gender.MALE
    private var profileName: String = ""
    private var heightRecords: MutableList<BodyRecord> = mutableListOf()
    private var weightRecords: MutableList<BodyRecord> = mutableListOf()
    private var avatarFileName: String = ""
    private var originalProfile: UserProfile? = null
    private var editingUserId: String = ""
    private var isNewUser: Boolean = false
    private var uiState by androidx.compose.runtime.mutableStateOf(ProfileEditUiState())
    private var dialogState by mutableStateOf<ProfileEditDialogState?>(null)

    private val avatarPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { handleAvatarSelected(it) }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startLocationLookup() else showManualProvinceSheet()
    }

    private val heightDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("UNCHECKED_CAST")
            val updated = result.data?.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>
            if (updated != null) {
                heightRecords = updated.toMutableList()
                refreshUiState()
            }
        }
    }

    private val weightDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("UNCHECKED_CAST")
            val updated = result.data?.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>
            if (updated != null) {
                weightRecords = updated.toMutableList()
                refreshUiState()
            }
        }
    }

    override fun getTitleText(): String = getString(R.string.profile_edit_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        diseaseRepo = DiseaseRepository(this)
        diseaseRepo.loadAll()
        provinceRepo = ProvinceRepository.fromContext(this)
        regionRepo = RegionRepository.fromContext(this)
        locationProvider = CurrentLocationProvider(this)
        loadProfile()
        setContent {
            HealthDietProTheme {
                ProfileEditScreen(
                    state = uiState,
                    dialogState = dialogState,
                    onBack = ::requestFormExit,
                    onNameChange = {
                        profileName = it
                        refreshUiState()
                    },
                    onGenderSelect = {
                        selectedGender = it
                        selectedDiseaseIds.removeAll { id ->
                            diseaseRepo.loadAll().none { disease ->
                                disease.id == id && (disease.gender?.contains(selectedGender.name) ?: true)
                            }
                        }
                        refreshUiState()
                    },
                    onAvatarClick = { avatarPickerLauncher.launch("image/*") },
                    onBirthdayClick = { showBirthdayPicker() },
                    onRegionClick = { showRegionChoiceSheet() },
                    onDiseaseClick = { showDiseasePicker() },
                    onHeightClick = { openHeightDetail() },
                    onWeightClick = { openWeightDetail() },
                    onBmiClick = { startActivity(Intent(this, BmiDetailActivity::class.java)) },
                    onSave = { saveProfile() },
                    onDismissDialog = { dismissCurrentDialog() },
                )
                DiscardChangesConfirmation()
            }
        }
    }

    private fun loadProfile() {
        isNewUser = intent.getBooleanExtra("create_new", false)
        val profile = if (isNewUser) UserProfile(id = "") else ProfilePrefs.load(this)
        editingUserId = profile.id
        profileName = profile.name
        selectedGender = profile.gender
        selectedBirthday = profile.birthday
        selectedDiseaseIds = profile.diseaseIds.toMutableList()
        selectedRegion = resolveRegion(profile.region)
        heightRecords = profile.heightRecords.toMutableList()
        weightRecords = profile.weightRecords.toMutableList()
        avatarFileName = profile.avatarFileName
        originalProfile = profile.copy(region = selectedRegion)
        refreshUiState()
    }

    private fun resolveRegion(region: RegionSnapshot): RegionSnapshot {
        return if (region.provinceName.isEmpty() && region.provinceCode.isNotEmpty()) {
            val province = provinceRepo.findByCode(region.provinceCode)
            region.copy(provinceName = province?.name ?: region.provinceCode)
        } else {
            region
        }
    }

    private fun refreshUiState() {
        val latestHeight = heightRecords.maxByOrNull { bodyRecordEpochMillis(it.date) }
        val latestWeight = weightRecords.maxByOrNull { bodyRecordEpochMillis(it.date) }
        val bmi = if (latestHeight != null && latestWeight != null) {
            BmiUtil.computeBmi(latestWeight.value, latestHeight.value)
        } else {
            0f
        }
        uiState = ProfileEditUiState(
            name = profileName,
            selectedGender = selectedGender,
            birthdayText = selectedBirthday?.date ?: getString(R.string.profile_edit_choose),
            regionText = selectedRegion.takeIf { !it.isEmpty() }?.display() ?: getString(R.string.profile_edit_choose),
            diseaseText = diseaseDisplayText(),
            heightText = latestHeight?.let { "%.1f cm".format(it.value) } ?: getString(R.string.profile_edit_no_record),
            weightText = latestWeight?.let { "%.1f kg".format(it.value) } ?: getString(R.string.profile_edit_no_record),
            bmiText = if (bmi > 0f) "%.1f %s".format(bmi, BmiUtil.getBmiLabel(bmi)) else getString(R.string.profile_edit_no_record),
            bmiColor = if (bmi > 0f) androidx.compose.ui.graphics.Color(BmiUtil.getBmiColor(bmi)).copy(alpha = 1f) else null,
            avatarFilePath = avatarFilePath(),
            avatarInitial = profileName.firstOrNull()?.toString() ?: "?",
            saveEnabled = hasChanges(),
        )
    }

    private fun diseaseDisplayText(): String {
        if (selectedDiseaseIds.isEmpty()) return getString(R.string.profile_edit_none)
        val diseases = diseaseRepo.loadAll()
        return selectedDiseaseIds.joinToString("、") { id ->
            diseases.find { it.id == id }?.displayName(Locale.getDefault()) ?: id
        }
    }

    private fun avatarFilePath(): String? =
        avatarFileName.takeIf { it.isNotBlank() }
            ?.let { File(filesDir, "avatars/$it") }
            ?.takeIf { it.exists() }
            ?.absolutePath

    private fun hasChanges(): Boolean {
        val orig = originalProfile ?: return true
        return profileName.trim() != orig.name ||
            selectedGender != orig.gender ||
            selectedBirthday != orig.birthday ||
            selectedRegion != orig.region ||
            selectedDiseaseIds.toList() != orig.diseaseIds ||
            heightRecords.toList() != orig.heightRecords ||
            weightRecords.toList() != orig.weightRecords ||
            avatarFileName != orig.avatarFileName
    }

    private fun openHeightDetail() {
        heightDetailLauncher.launch(
            Intent(this, HeightDetailActivity::class.java).apply {
                putExtra("records", ArrayList(heightRecords))
                putExtra("unit", AppPrefs.getUnit(this@ProfileEditActivity, UnitCategoryType.Length.id, UnitCategoryType.Length.defaultUnitId))
            },
        )
    }

    private fun openWeightDetail() {
        weightDetailLauncher.launch(
            Intent(this, WeightDetailActivity::class.java).apply {
                putExtra("records", ArrayList(weightRecords))
                putExtra("unit", AppPrefs.getUnit(this@ProfileEditActivity, UnitCategoryType.Weight.id, UnitCategoryType.Weight.defaultUnitId))
            },
        )
    }

    private fun dismissCurrentDialog() {
        dialogState = null
    }

    private fun showBirthdayPicker() {
        val initialDate = selectedBirthday?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()
        dialogState = ProfileEditDialogState.BirthdayPicker(
            initialMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            onDatePicked = { date ->
                selectedBirthday = AppDate(date = date.toString())
                dismissCurrentDialog()
                refreshUiState()
            },
        )
    }

    private fun showRegionChoiceSheet() {
        dismissCurrentDialog()
        dialogState = ProfileEditDialogState.RegionChoice(
            onUseLocation = {
                dismissCurrentDialog()
                if (locationProvider.hasPermission()) startLocationLookup()
                else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onManual = {
                dismissCurrentDialog()
                showManualProvinceSheet()
            },
        )
    }

    private fun startLocationLookup() {
        dismissCurrentDialog()
        dialogState = ProfileEditDialogState.LocationProgress(
            onCancel = {
                dismissCurrentDialog()
                showManualProvinceSheet()
            },
        )

        locationProvider.getCurrentLocation { result ->
            runOnUiThread {
                dialogState = null
                when (result) {
                    is CurrentLocationProvider.Result.Ok -> {
                        val snapshot = regionRepo.resolve(result.lng, result.lat, provinceRepo)
                        if (snapshot.provinceCode.isNotEmpty()) {
                            selectedRegion = snapshot
                            refreshUiState()
                            Toast.makeText(this, getString(R.string.profile_edit_location_recognized, snapshot.display()), Toast.LENGTH_SHORT).show()
                        } else {
                            selectedRegion = snapshot
                            refreshUiState()
                            Toast.makeText(this, R.string.profile_edit_location_unknown_province, Toast.LENGTH_SHORT).show()
                            showManualProvinceSheet()
                        }
                    }
                    is CurrentLocationProvider.Result.Err -> {
                        Toast.makeText(this, getString(R.string.profile_edit_location_failed, result.reason), Toast.LENGTH_SHORT).show()
                        showManualProvinceSheet()
                    }
                }
            }
        }
    }

    private fun showManualProvinceSheet() {
        showSelectionSheet(
            title = getString(R.string.profile_edit_select_province),
            items = provinceRepo.all().sortedBy { it.code }.map { it.name to it },
        ) { province ->
            selectedRegion = RegionSnapshot(provinceCode = province.code, provinceName = province.name)
            refreshUiState()
            showManualCitySheet(province.code, province.name)
        }
    }

    private fun showManualCitySheet(provinceCode: String, provinceName: String) {
        val cities = regionRepo.citiesOf(provinceCode)
        if (cities.isEmpty()) {
            Toast.makeText(this, getString(R.string.profile_edit_no_city_data, provinceName), Toast.LENGTH_SHORT).show()
            return
        }
        showSelectionSheet(
            title = "$provinceName / ${getString(R.string.profile_edit_select_city)}",
            items = cities.map { it.name to it },
        ) { city ->
            selectedRegion = selectedRegion.copy(
                provinceCode = provinceCode,
                provinceName = provinceName,
                cityCode = city.code,
                cityName = city.name,
                districtCode = "",
                districtName = "",
                lng = city.lng,
                lat = city.lat,
            )
            refreshUiState()
            showManualDistrictSheet(provinceCode, provinceName, city)
        }
    }

    private fun showManualDistrictSheet(provinceCode: String, provinceName: String, city: Region) {
        val districts = regionRepo.districtsOf(city.code)
        if (districts.isEmpty()) return
        showSelectionSheet(
            title = "${city.name} / ${getString(R.string.profile_edit_select_district)}",
            items = districts.map { it.name to it },
        ) { district ->
            selectedRegion = selectedRegion.copy(
                provinceCode = provinceCode,
                provinceName = provinceName,
                cityCode = city.code,
                cityName = city.name,
                districtCode = district.code,
                districtName = district.name,
                lng = district.lng,
                lat = district.lat,
            )
            refreshUiState()
        }
    }

    private fun <T> showSelectionSheet(
        title: String,
        items: List<Pair<String, T>>,
        onSelected: (T) -> Unit,
    ) {
        dismissCurrentDialog()
        dialogState = ProfileEditDialogState.Selection(
            title = title,
            items = items.map { (label, value) ->
                ProfileSelectionItem(
                    label = label,
                    onClick = {
                        dismissCurrentDialog()
                        onSelected(value)
                    },
                )
            },
        )
    }

    private fun showDiseasePicker() {
        dismissCurrentDialog()
        val diseases = diseaseRepo.getSorted(selectedRegion.provinceCode.ifEmpty { null })
        val enabled = BooleanArray(diseases.size) { i ->
            diseases[i].gender?.contains(selectedGender.name) ?: true
        }
        selectedDiseaseIds.removeAll { id ->
            diseases.none { it.id == id && (it.gender?.contains(selectedGender.name) ?: true) }
        }
        dialogState = ProfileEditDialogState.DiseasePicker(
            items = diseases.mapIndexed { index, disease ->
                ProfileDiseaseChoice(
                    id = disease.id,
                    label = disease.displayName(Locale.getDefault()),
                    enabled = enabled[index],
                    checked = disease.id in selectedDiseaseIds,
                )
            },
            onConfirm = { checkedIds ->
                selectedDiseaseIds = checkedIds.toMutableList()
                dismissCurrentDialog()
                refreshUiState()
            },
        )
    }

    private fun handleAvatarSelected(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) return
            val cropped = cropToSquare(bitmap, 200)
            val dir = File(filesDir, "avatars")
            if (!dir.exists()) dir.mkdirs()
            avatarFileName = "${System.currentTimeMillis()}.jpg"
            FileOutputStream(File(dir, avatarFileName)).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            refreshUiState()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.profile_edit_avatar_load_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropToSquare(bitmap: Bitmap, maxSize: Int): Bitmap {
        val side = minOf(bitmap.width, bitmap.height)
        val square = Bitmap.createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
        if (side <= maxSize) return square
        return Bitmap.createScaledBitmap(square, maxSize, maxSize, true)
    }

    override fun hasUnsavedChanges(): Boolean = hasChanges()

    override fun saveFormChanges() = saveProfile()

    private fun saveProfile(allowBlankName: Boolean = false) {
        val name = profileName.trim()
        if (!allowBlankName && name.isBlank()) {
            Toast.makeText(this, R.string.profile_edit_name_required, Toast.LENGTH_SHORT).show()
            return
        }
        ProfilePrefs.save(
            this,
            UserProfile(
                id = editingUserId,
                name = name,
                gender = selectedGender,
                birthday = selectedBirthday,
                region = selectedRegion,
                diseaseIds = selectedDiseaseIds.toList(),
                heightRecords = heightRecords.toList(),
                weightRecords = weightRecords.toList(),
                avatarFileName = avatarFileName,
            ),
        )
        setResult(RESULT_OK)
        finish()
    }
}

private data class ProfileEditUiState(
    val name: String = "",
    val selectedGender: Gender = Gender.MALE,
    val birthdayText: String = "",
    val regionText: String = "",
    val diseaseText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val bmiText: String = "",
    val bmiColor: Color? = null,
    val avatarFilePath: String? = null,
    val avatarInitial: String = "?",
    val saveEnabled: Boolean = false,
)

private sealed interface ProfileEditDialogState {
    data class RegionChoice(
        val onUseLocation: () -> Unit,
        val onManual: () -> Unit,
    ) : ProfileEditDialogState

    data class Selection(
        val title: String,
        val items: List<ProfileSelectionItem>,
    ) : ProfileEditDialogState

    data class DiseasePicker(
        val items: List<ProfileDiseaseChoice>,
        val onConfirm: (List<String>) -> Unit,
    ) : ProfileEditDialogState

    data class LocationProgress(
        val onCancel: () -> Unit,
    ) : ProfileEditDialogState

    data class BirthdayPicker(
        val initialMillis: Long,
        val onDatePicked: (LocalDate) -> Unit,
    ) : ProfileEditDialogState
}

private data class ProfileSelectionItem(
    val label: String,
    val onClick: () -> Unit,
)

private data class ProfileDiseaseChoice(
    val id: String,
    val label: String,
    val enabled: Boolean,
    val checked: Boolean,
)

@Composable
private fun ProfileEditScreen(
    state: ProfileEditUiState,
    dialogState: ProfileEditDialogState?,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onGenderSelect: (Gender) -> Unit,
    onAvatarClick: () -> Unit,
    onBirthdayClick: () -> Unit,
    onRegionClick: () -> Unit,
    onDiseaseClick: () -> Unit,
    onHeightClick: () -> Unit,
    onWeightClick: () -> Unit,
    onBmiClick: () -> Unit,
    onSave: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    BaseScreen(
        title = stringResource(R.string.profile_edit_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    ProfileAvatarEditor(
                        state = state,
                        onAvatarClick = onAvatarClick,
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = { AppInputLabel(stringResource(R.string.profile_edit_name)) },
                        colors = AppInputTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    AppDropdownField(
                        label = stringResource(R.string.profile_edit_gender_select),
                        value = state.selectedGender.displayText(),
                        options = listOf(
                            AppDropdownOption(Gender.MALE.name, stringResource(R.string.profile_gender_male)),
                            AppDropdownOption(Gender.FEMALE.name, stringResource(R.string.profile_gender_female)),
                        ),
                        onSelect = { option -> onGenderSelect(Gender.valueOf(option.id)) },
                    )
                }
                item { ProfileClickableField(stringResource(R.string.profile_edit_birthday), state.birthdayText, onBirthdayClick) }
                item { ProfileClickableField(stringResource(R.string.profile_edit_region), state.regionText, onRegionClick) }
                item { ProfileClickableField(stringResource(R.string.profile_edit_disease), state.diseaseText, onDiseaseClick) }
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                    ) {
                        Column {
                            SettingRow(
                                title = stringResource(R.string.profile_edit_latest_height),
                                subtitle = "",
                                leadingIconRes = R.drawable.ic_height,
                                trailingValue = state.heightText,
                                onClick = onHeightClick,
                            )
                            SettingRow(
                                title = stringResource(R.string.profile_edit_latest_weight),
                                subtitle = "",
                                leadingIconRes = R.drawable.ic_weight,
                                trailingValue = state.weightText,
                                onClick = onWeightClick,
                            )
                            SettingRow(
                                title = stringResource(R.string.profile_edit_latest_bmi),
                                subtitle = "",
                                leadingIconRes = R.drawable.ic_chart,
                                trailingValue = state.bmiText,
                                trailingValueColor = state.bmiColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = onBmiClick,
                            )
                        }
                    }
                }
            }
            FormSaveBar(stringResource(R.string.profile_edit_save), state.saveEnabled, onSave)
        }
    }
    ProfileEditDialogs(
        dialogState = dialogState,
        onDismiss = onDismissDialog,
    )
}

@Composable
private fun ProfileEditDialogs(
    dialogState: ProfileEditDialogState?,
    onDismiss: () -> Unit,
) {
    when (dialogState) {
        null -> Unit
        is ProfileEditDialogState.RegionChoice -> RegionChoiceDialog(
            state = dialogState,
            onDismiss = onDismiss,
        )
        is ProfileEditDialogState.Selection -> SelectionDialog(
            state = dialogState,
            onDismiss = onDismiss,
        )
        is ProfileEditDialogState.DiseasePicker -> DiseasePickerDialog(
            state = dialogState,
            onDismiss = onDismiss,
        )
        is ProfileEditDialogState.LocationProgress -> LocationProgressDialog(
            state = dialogState,
        )
        is ProfileEditDialogState.BirthdayPicker -> ComposeDatePickerDialog(
            initialMillis = dialogState.initialMillis,
            onDismiss = onDismiss,
            onDatePicked = dialogState.onDatePicked,
        )
    }
}

@Composable
private fun RegionChoiceDialog(
    state: ProfileEditDialogState.RegionChoice,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_select_region)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIconTextButton(
                    text = stringResource(R.string.profile_edit_use_current_location),
                    iconRes = R.drawable.ic_placeholder,
                    onClick = state.onUseLocation,
                    modifier = Modifier.fillMaxWidth(),
                )
                AppIconTextButton(
                    text = stringResource(R.string.profile_edit_select_region_manual),
                    iconRes = R.drawable.ic_list,
                    onClick = state.onManual,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            AppTextIconButton(
                text = stringResource(R.string.compose_confirm_dialog_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun SelectionDialog(
    state: ProfileEditDialogState.Selection,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                items(state.items) { item ->
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = item.onClick)
                            .padding(vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppTextIconButton(
                text = stringResource(R.string.compose_confirm_dialog_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun DiseasePickerDialog(
    state: ProfileEditDialogState.DiseasePicker,
    onDismiss: () -> Unit,
) {
    var checkedIds by remember(state.items) {
        mutableStateOf(state.items.filter { it.checked }.map { it.id }.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_select_disease)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = item.enabled) {
                                checkedIds = if (item.id in checkedIds) {
                                    checkedIds - item.id
                                } else {
                                    checkedIds + item.id
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.id in checkedIds,
                            enabled = item.enabled,
                            onCheckedChange = { checked ->
                                checkedIds = if (checked) checkedIds + item.id else checkedIds - item.id
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (item.enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            },
                            textDecoration = if (item.enabled) null else TextDecoration.LineThrough,
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppTextIconButton(
                text = stringResource(R.string.compose_confirm_dialog_ok),
                iconRes = R.drawable.ic_check,
                onClick = { state.onConfirm(checkedIds.toList()) },
            )
        },
        dismissButton = {
            AppTextIconButton(
                text = stringResource(R.string.compose_confirm_dialog_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun LocationProgressDialog(
    state: ProfileEditDialogState.LocationProgress,
) {
    AlertDialog(
        onDismissRequest = state.onCancel,
        title = { Text(stringResource(R.string.profile_edit_locating_title)) },
        text = { Text(stringResource(R.string.profile_edit_locating_message)) },
        confirmButton = {},
        dismissButton = {
            AppTextIconButton(
                text = stringResource(R.string.compose_confirm_dialog_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = state.onCancel,
            )
        },
    )
}

@Composable
private fun ProfileAvatarEditor(
    state: ProfileEditUiState,
    onAvatarClick: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap = androidx.compose.runtime.remember(state.avatarFilePath) {
        state.avatarFilePath?.let { BitmapFactory.decodeFile(it) }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAvatarClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = state.avatarInitial,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
        Text(
            text = stringResource(R.string.profile_edit_avatar_change),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ProfileClickableField(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Gender.displayText(): String = when (this) {
    Gender.MALE -> stringResource(R.string.profile_gender_male)
    Gender.FEMALE -> stringResource(R.string.profile_gender_female)
}

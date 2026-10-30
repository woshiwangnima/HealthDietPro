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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.DirtyFormActivity
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.AppTextIconButton
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.archive.resolveAvatarFile
import com.woshiwangnima.healthdietpro.model.archive.userArchiveDirectory
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.AppDate
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.BodyMetrics
import com.woshiwangnima.healthdietpro.model.profile.BodyMetricsRepository
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
        diseaseRepo = DiseaseRepository.fromContext(this)
        diseaseRepo.loadAll()
        provinceRepo = ProvinceRepository.fromContext(this)
        regionRepo = RegionRepository.fromContext(this)
        locationProvider = CurrentLocationProvider(this)
        loadProfile()
        setContent {
            // 与详情页一致：局部套 AppTheme，让 MaterialTheme 与设计令牌同源，
            // 顶栏 / 弹窗 / 输入框都拿到同一套配色，不再出现「页面深色、控件浅色」的混搭。
            AppTheme(darkTheme = rememberAppDarkTheme()) {
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
                        refreshUiState()
                    },
                    onAvatarClick = { avatarPickerLauncher.launch("image/*") },
                    onBirthdayClick = { showBirthdayPicker() },
                    onRegionClick = { showRegionChoiceSheet() },
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
            birthdayMillis = selectedBirthday?.date?.let { date ->
                LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            },
            regionText = selectedRegion.takeIf { !it.isEmpty() }?.display() ?: getString(R.string.profile_edit_choose),
            heightText = latestHeight?.let { "%.1f cm".format(it.value) } ?: getString(R.string.profile_edit_no_record),
            weightText = latestWeight?.let { "%.2f kg".format(it.value) } ?: getString(R.string.profile_edit_no_record),
            bmiText = if (bmi > 0f) {
                // 不用 BmiUtil.getBmiLabel：它返回资产里写死的中文标签，英文环境会中英混排。
                "%.1f %s".format(bmi, bmiCategoryLabel(bmi))
            } else {
                getString(R.string.profile_edit_no_record)
            },
            avatarFilePath = avatarFilePath(),
            avatarInitial = profileName.firstOrNull()?.toString() ?: "?",
            saveEnabled = hasChanges(),
        )
    }

    /** BMI 分级标签取自字符串资源，随语言切换（资产里的 label 是写死的中文）。 */
    private fun bmiCategoryLabel(bmi: Float): String {
        val bands = BmiUtil.loadBmiBands()
        val index = bands.indexOfFirst { band ->
            (band.min < 0f || bmi >= band.min) && (band.max == Float.MAX_VALUE || bmi < band.max)
        }
        return when (index) {
            0 -> getString(R.string.bmi_underweight)
            1 -> getString(R.string.bmi_normal)
            2 -> getString(R.string.bmi_overweight)
            3 -> getString(R.string.bmi_obese)
            else -> getString(R.string.bmi_unknown)
        }
    }

    private fun avatarFilePath(): String? =
        avatarFileName.takeIf { it.isNotBlank() }
            ?.let { resolveAvatarFile(this, originalProfile?.id.orEmpty(), it) }
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
        val diseases = diseaseRepo.loadAll()
        val enabled = BooleanArray(diseases.size) { i ->
            diseases[i].applicability.allows(selectedGender)
        }
        selectedDiseaseIds.removeAll { id ->
            diseases.none { it.id == id && it.applicability.allows(selectedGender) }
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
            val userId = originalProfile?.id ?: ProfilePrefs.getCurrentUserId(this)
            val dir = File(userArchiveDirectory(this, userId), "attachments/avatar")
            if (!dir.exists()) dir.mkdirs()
            val fileName = "${System.currentTimeMillis()}.jpg"
            avatarFileName = "attachments/avatar/$fileName"
            FileOutputStream(File(dir, fileName)).use { out ->
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
        val original = originalProfile
        val latest = ProfilePrefs.load(this)
        val updatedMetrics = BodyMetrics(
            heightRecords = if (heightRecords.toList() == original?.heightRecords) latest.heightRecords else heightRecords.toList(),
            weightRecords = if (weightRecords.toList() == original?.weightRecords) latest.weightRecords else weightRecords.toList(),
            circumferenceRecords = latest.circumferenceRecords,
        )
        BodyMetricsRepository.current(this).replace(updatedMetrics)
        ProfilePrefs.save(
            this,
            latest.copy(
                id = editingUserId,
                name = name,
                gender = selectedGender,
                birthday = selectedBirthday,
                region = selectedRegion,
                diseaseIds = selectedDiseaseIds.toList(),
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
    val birthdayMillis: Long? = null,
    val regionText: String = "",
    val diseaseText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val bmiText: String = "",
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
    onHeightClick: () -> Unit,
    onWeightClick: () -> Unit,
    onBmiClick: () -> Unit,
    onSave: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    BaseScreen(
        title = stringResource(R.string.profile_edit_title),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = spacing.page,
                    end = spacing.page,
                    top = spacing.s3,
                    bottom = spacing.s4,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.s4),
            ) {
                item { ProfileAvatarCard(state = state, onAvatarClick = onAvatarClick) }
                item {
                    ProfileBasicInfoCard(
                        state = state,
                        onNameChange = onNameChange,
                        onGenderSelect = onGenderSelect,
                        onBirthdayClick = onBirthdayClick,
                        onRegionClick = onRegionClick,
                    )
                }
                item {
                    ProfileBodyDataCard(
                        state = state,
                        onHeightClick = onHeightClick,
                        onWeightClick = onWeightClick,
                        onBmiClick = onBmiClick,
                    )
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
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_select_region), style = tokens.type.titleS) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                ProfileDialogOption(
                    iconRes = R.drawable.ic_placeholder,
                    label = stringResource(R.string.profile_edit_use_current_location),
                    onClick = state.onUseLocation,
                )
                HorizontalDivider(
                    thickness = tokens.radii.highlight,
                    color = tokens.colors.border,
                )
                ProfileDialogOption(
                    iconRes = R.drawable.ic_list,
                    label = stringResource(R.string.profile_edit_select_region_manual),
                    onClick = state.onManual,
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

/** 弹层里的可选行：图标 + 文案 + 箭头，56 dp 高（§4.6-3）。 */
@Composable
private fun ProfileDialogOption(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = spacing.rowMinHeight)
            .padding(horizontal = spacing.s1, vertical = spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(spacing.s5),
            tint = tokens.colors.primary,
        )
        Spacer(Modifier.width(spacing.s2))
        Text(
            text = label,
            style = tokens.type.body,
            color = tokens.colors.text,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(spacing.s5),
            tint = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun SelectionDialog(
    state: ProfileEditDialogState.Selection,
    onDismiss: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title, style = tokens.type.titleS) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
            ) {
                items(state.items) { item ->
                    Text(
                        text = item.label,
                        style = tokens.type.body,
                        color = tokens.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = item.onClick)
                            .heightIn(min = spacing.rowMinHeight)
                            .padding(vertical = spacing.s2),
                    )
                    HorizontalDivider(
                        thickness = tokens.radii.highlight,
                        color = tokens.colors.border,
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
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_select_disease), style = tokens.type.titleS) },
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
                            .heightIn(min = spacing.rowMinHeight)
                            .padding(vertical = spacing.s1),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.id in checkedIds,
                            enabled = item.enabled,
                            onCheckedChange = { checked ->
                                checkedIds = if (checked) checkedIds + item.id else checkedIds - item.id
                            },
                        )
                        Spacer(Modifier.width(spacing.s2))
                        Text(
                            text = item.label,
                            style = tokens.type.body,
                            color = if (item.enabled) tokens.colors.text else tokens.colors.disabled,
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
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    AlertDialog(
        onDismissRequest = state.onCancel,
        title = { Text(stringResource(R.string.profile_edit_locating_title), style = tokens.type.titleS) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(spacing.s5),
                    strokeWidth = 2.dp,
                    color = tokens.colors.primary,
                )
                Spacer(Modifier.width(spacing.s3))
                Text(
                    text = stringResource(R.string.profile_edit_locating_message),
                    style = tokens.type.bodyS,
                    color = tokens.colors.text,
                )
            }
        },
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

/** 头像卡：整行可点即换图，右侧箭头与左下提示指向同一个动作。 */
@Composable
private fun ProfileAvatarCard(
    state: ProfileEditUiState,
    onAvatarClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val bitmap = remember(state.avatarFilePath) {
        state.avatarFilePath?.let { BitmapFactory.decodeFile(it) }
    }

    AppSectionCard(
        iconRes = R.drawable.ic_photo,
        title = stringResource(R.string.profile_edit_section_avatar),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAvatarClick)
                .heightIn(min = spacing.rowMinHeight)
                .padding(horizontal = spacing.s3, vertical = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.s8)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(spacing.s8)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    // 首字母用 onPrimary 而不是白色：深色主题主色是浅蓝，白字只有 2.4:1。
                    Text(
                        text = state.avatarInitial,
                        style = tokens.type.titleS,
                        color = colors.onPrimary,
                    )
                }
            }
            Spacer(Modifier.width(spacing.s3))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.s1),
            ) {
                Text(
                    text = stringResource(R.string.profile_edit_avatar_change),
                    style = tokens.type.body,
                    color = colors.primary,
                )
                Text(
                    text = stringResource(R.string.profile_edit_avatar_hint),
                    style = tokens.type.caption,
                    color = colors.textMuted,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(spacing.s5),
                tint = colors.textMuted,
            )
        }
    }
}

/** 基本信息卡：姓名是唯一的输入控件（带描边），性别用片选，生日与地区走行内导航。 */
@Composable
private fun ProfileBasicInfoCard(
    state: ProfileEditUiState,
    onNameChange: (String) -> Unit,
    onGenderSelect: (Gender) -> Unit,
    onBirthdayClick: () -> Unit,
    onRegionClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing

    AppSectionCard(
        iconRes = R.drawable.ic_nav_profile,
        title = stringResource(R.string.profile_edit_section_basic),
        contentPadding = PaddingValues(bottom = spacing.s2),
    ) {
        EditorTextField(
            label = stringResource(R.string.profile_edit_name),
            value = state.name,
            onValueChange = onNameChange,
            required = true,
            modifier = Modifier.padding(horizontal = spacing.s3),
        )
        Spacer(Modifier.height(spacing.s3))
        Text(
            text = stringResource(R.string.profile_edit_gender),
            style = tokens.type.caption,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = spacing.s3),
        )
        Spacer(Modifier.height(spacing.s1))
        Row(
            modifier = Modifier.padding(horizontal = spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            ProfileGenderChip(
                label = stringResource(R.string.profile_gender_male),
                selected = state.selectedGender == Gender.MALE,
                onClick = { onGenderSelect(Gender.MALE) },
            )
            ProfileGenderChip(
                label = stringResource(R.string.profile_gender_female),
                selected = state.selectedGender == Gender.FEMALE,
                onClick = { onGenderSelect(Gender.FEMALE) },
            )
        }
        Spacer(Modifier.height(spacing.s2))
        ProfileDivider()
        // 未填写时值显示为次要色：「点击选择」是提示，不是内容。
        val chooseText = stringResource(R.string.profile_edit_choose)
        ProfileNavRow(
            label = stringResource(R.string.profile_edit_birthday),
            value = state.birthdayText,
            valueColor = if (state.birthdayText == chooseText) colors.textMuted else null,
            onClick = onBirthdayClick,
        )
        ProfileDivider()
        ProfileNavRow(
            label = stringResource(R.string.profile_edit_region),
            value = state.regionText,
            valueColor = if (state.regionText == chooseText) colors.textMuted else null,
            onClick = onRegionClick,
        )
    }
}

/** 身体数据卡：三项都是「点进去看历史」，所以统一成行内导航，不再套一层灰底输入框。 */
@Composable
private fun ProfileBodyDataCard(
    state: ProfileEditUiState,
    onHeightClick: () -> Unit,
    onWeightClick: () -> Unit,
    onBmiClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing

    AppSectionCard(
        iconRes = R.drawable.ic_health_metrics,
        title = stringResource(R.string.profile_edit_section_body),
        contentPadding = PaddingValues(bottom = spacing.s2),
    ) {
        ProfileNavRow(
            label = stringResource(R.string.profile_edit_latest_height),
            value = state.heightText,
            iconRes = R.drawable.ic_height,
            onClick = onHeightClick,
        )
        ProfileDivider()
        ProfileNavRow(
            label = stringResource(R.string.profile_edit_latest_weight),
            value = state.weightText,
            iconRes = R.drawable.ic_weight,
            onClick = onWeightClick,
        )
        ProfileDivider()
        ProfileNavRow(
            label = stringResource(R.string.profile_edit_latest_bmi),
            value = state.bmiText,
            iconRes = R.drawable.ic_chart,
            onClick = onBmiClick,
        )
        Text(
            text = stringResource(R.string.profile_edit_body_hint),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            modifier = Modifier.padding(horizontal = spacing.s3, vertical = spacing.s2),
        )
    }
}

@Composable
private fun ProfileDivider() {
    val tokens = LocalAppTokens.current
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = tokens.spacing.s3),
        thickness = tokens.radii.highlight,
        color = tokens.colors.border,
    )
}

/**
 * 卡内可点行：标签 + 当前值 + 右箭头，最小 56 dp（§4.6-3 触屏行高）。
 * 卡底已经是 `panelRaised`，行里不再铺灰底，避免出现第二层浮起（§2.7）。
 */
@Composable
private fun ProfileNavRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    @DrawableRes iconRes: Int? = null,
    valueColor: Color? = null,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = spacing.rowMinHeight)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(spacing.s5),
                tint = colors.textMuted,
            )
            Spacer(Modifier.width(spacing.s2))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            Text(
                text = label,
                style = tokens.type.caption,
                color = colors.textMuted,
            )
            Text(
                text = value,
                style = tokens.type.body,
                color = valueColor ?: colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(spacing.s5),
            tint = colors.textMuted,
        )
    }
}

/** 性别片选：视觉高度 30 dp 左右，外层补到 48 dp 热区。 */
@Composable
private fun ProfileGenderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing

    Box(
        modifier = Modifier
            .heightIn(min = spacing.hitMin)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(tokens.radii.label),
            color = if (selected) colors.primaryContainer else Color.Transparent,
            border = BorderStroke(
                width = if (selected) tokens.radii.stroke else tokens.radii.highlight,
                color = if (selected) colors.primary else colors.border,
            ),
        ) {
            Text(
                text = label,
                style = tokens.type.bodyS,
                color = if (selected) colors.onPrimaryContainer else colors.textMuted,
                maxLines = 1,
                // 片选宽度不跟着字数走：两三个字太窄时点按会发抖，统一留出 24 dp 横向内边距。
                modifier = Modifier.padding(horizontal = spacing.s5, vertical = spacing.s1),
            )
        }
    }
}

@Composable
private fun Gender.displayText(): String = when (this) {
    Gender.MALE -> stringResource(R.string.profile_gender_male)
    Gender.FEMALE -> stringResource(R.string.profile_gender_female)
}


package com.woshiwangnima.healthdietpro.ui.profile

import android.Manifest
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityProfileEditBinding
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.model.profile.AppDate
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import com.woshiwangnima.healthdietpro.model.region.RegionRepository
import com.woshiwangnima.healthdietpro.model.region.RegionSnapshot
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import com.woshiwangnima.healthdietpro.util.location.CurrentLocationProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class ProfileEditActivity : BaseBackActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var diseaseRepo: DiseaseRepository

    private var selectedBirthday: AppDate? = null
    private var selectedRegion: RegionSnapshot = RegionSnapshot()
    private var selectedDiseaseIds: MutableList<String> = mutableListOf()
    private var selectedGender: Gender = Gender.MALE

    private var heightRecords: MutableList<BodyRecord> = mutableListOf()
    private var weightRecords: MutableList<BodyRecord> = mutableListOf()
    private var originalProfile: UserProfile? = null
    private var editingUserId: String = ""
    private var isNewUser: Boolean = false
    private val genderOptions = listOf("男", "女")
    private var avatarFileName: String = ""
    private var currentDialog: Dialog? = null

    private val avatarPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleAvatarSelected(it) }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startLocationLookup()
        else showManualProvinceSheet()
    }

    private lateinit var provinceRepo: ProvinceRepository
    private lateinit var regionRepo: RegionRepository
    private lateinit var locationProvider: CurrentLocationProvider

    private val heightDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("UNCHECKED_CAST")
            val updated = result.data?.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>
            if (updated != null) {
                heightRecords = updated.toMutableList()
                updateHeightDisplay()
                checkSaveEnabled()
            }
        }
    }

    private val weightDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            @Suppress("UNCHECKED_CAST")
            val updated = result.data?.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>
            if (updated != null) {
                weightRecords = updated.toMutableList()
                updateWeightDisplay()
                checkSaveEnabled()
            }
        }
    }

    override fun getTitleText(): String = "个人信息"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        diseaseRepo = DiseaseRepository(this)
        diseaseRepo.loadAll()
        provinceRepo = ProvinceRepository.fromContext(this)
        regionRepo = RegionRepository.fromContext(this)
        locationProvider = CurrentLocationProvider(this)

        loadProfile()
        setupClickListeners()
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val name = binding.nameInput.text.toString().trim()
                val gender = selectedGender
                val profile = UserProfile(
                    id = editingUserId,
                    name = name,
                    gender = gender,
                    birthday = selectedBirthday,
                    region = selectedRegion,
                    diseaseIds = selectedDiseaseIds.toList(),
                    heightRecords = heightRecords.toList(),
                    weightRecords = weightRecords.toList(),
                    avatarFileName = avatarFileName
                )
                ProfilePrefs.save(this@ProfileEditActivity, profile)
                setResult(RESULT_OK)
                finish()
            }
        })
    }

    private fun loadProfile() {
        isNewUser = intent.getBooleanExtra("create_new", false)
        val profile = if (isNewUser) UserProfile(id = "") else ProfilePrefs.load(this)
        editingUserId = profile.id
        binding.nameInput.setText(profile.name)
        selectedGender = profile.gender
        setupGenderDropdown()

        profile.birthday?.let {
            selectedBirthday = it
            binding.birthdayDisplay.text = it.date
        }

        selectedDiseaseIds = profile.diseaseIds.toMutableList()
        updateDiseaseDisplay()

        selectedRegion = profile.region
        updateProvinceDisplay()

        heightRecords = profile.heightRecords.toMutableList()
        updateHeightDisplay()
        weightRecords = profile.weightRecords.toMutableList()
        updateWeightDisplay()

        avatarFileName = profile.avatarFileName
        refreshAvatarDisplay()

        originalProfile = profile
        checkSaveEnabled()
    }

    private fun setupGenderDropdown() {
        val adapter = android.widget.ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, genderOptions)
        binding.genderInput.setAdapter(adapter)
        binding.genderInput.setText(
            if (selectedGender == Gender.MALE) "男" else "女", false
        )
    }

    private fun updateProvinceDisplay() {
        // 兜底：若新 region 仅有省代码但无省名，从 ProvinceRepository 补。
        val display = if (selectedRegion.provinceName.isEmpty() && selectedRegion.provinceCode.isNotEmpty()) {
            val p = provinceRepo.findByCode(selectedRegion.provinceCode)
            selectedRegion.copy(provinceName = p?.name ?: selectedRegion.provinceCode)
        } else selectedRegion
        binding.provinceDisplay.text = display.display()
    }

    private fun setupClickListeners() {
        binding.avatarEditText.setOnClickListener { avatarPickerLauncher.launch("image/*") }
        binding.birthdayDisplay.setOnClickListener { showDatePicker() }
        binding.provinceDisplay.setOnClickListener { showRegionChoiceSheet() }
        binding.diseaseDisplay.setOnClickListener { showDiseasePicker() }

        binding.genderInput.setOnItemClickListener { parent, _, pos, _ ->
            selectedGender = if (pos == 0) Gender.MALE else Gender.FEMALE
            checkSaveEnabled()
        }

        binding.heightRow.setOnClickListener {
            val context = this@ProfileEditActivity
            val intent = Intent(context, HeightDetailActivity::class.java).apply {
                putExtra("records", ArrayList(heightRecords))
                putExtra("unit", AppPrefs.getUnit(context, UnitCategory.ID_LENGTH, UnitCategory.DEFAULT_UNIT_LENGTH))
            }
            heightDetailLauncher.launch(intent)
        }
        binding.weightRow.setOnClickListener {
            val context = this@ProfileEditActivity
            val intent = Intent(context, WeightDetailActivity::class.java).apply {
                putExtra("records", ArrayList(weightRecords))
                putExtra("unit", AppPrefs.getUnit(context, UnitCategory.ID_WEIGHT, UnitCategory.DEFAULT_UNIT_WEIGHT))
            }
            weightDetailLauncher.launch(intent)
        }

        binding.bmiRow.setOnClickListener {
            startActivity(Intent(this@ProfileEditActivity, BmiDetailActivity::class.java))
        }

        binding.nameInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { checkSaveEnabled() }
        })

        binding.saveBtn.setOnClickListener { saveProfile() }
    }

    private fun checkSaveEnabled() {
        val orig = originalProfile ?: return
        val currentName = binding.nameInput.text.toString().trim()
        val currentGender = selectedGender
        val changed = currentName != orig.name ||
            currentGender != orig.gender ||
            selectedBirthday != orig.birthday ||
            selectedRegion != orig.region ||
            selectedDiseaseIds.toList() != orig.diseaseIds ||
            heightRecords.toList() != orig.heightRecords ||
            weightRecords.toList() != orig.weightRecords ||
            avatarFileName != orig.avatarFileName
        binding.saveBtn.isEnabled = changed
    }

    private fun dismissCurrentDialog() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    private fun showDatePicker() {
        dismissCurrentDialog()
        val cal = Calendar.getInstance()
        selectedBirthday?.let {
            try {
                val parts = it.date.split("-")
                if (parts.size == 3) {
                    cal.set(Calendar.YEAR, parts[0].toInt())
                    cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
            } catch (_: Exception) {}
        }
        currentDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val dateStr = "%04d-%02d-%02d".format(year, month + 1, day)
                selectedBirthday = AppDate(date = dateStr)
                binding.birthdayDisplay.text = dateStr
                checkSaveEnabled()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply { show() }
    }

    /** 「选择地区」入口：弹底部 sheet 让用户选「使用当前位置」或「手动选择三级地区」。 */
    private fun showRegionChoiceSheet() {
        dismissCurrentDialog()
        val sheet = BottomSheetDialog(this).also { currentDialog = it }
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(),
                (24 * density).toInt(), (16 * density).toInt())
        }
        root.addView(TextView(this).apply {
            text = "选择地区"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        })

        val useLocation = MaterialButton(this).apply {
            text = "使用当前位置"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * density).toInt() }
            setOnClickListener {
                sheet.dismiss()
                if (locationProvider.hasPermission()) startLocationLookup()
                else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        root.addView(useLocation)

        val manual = MaterialButton(this).apply {
            text = "手动选择省/市/县"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * density).toInt() }
            setOnClickListener {
                sheet.dismiss()
                showManualProvinceSheet()
            }
        }
        root.addView(manual)

        sheet.setContentView(root)
        sheet.show()
    }

    private fun startLocationLookup() {
        dismissCurrentDialog()
        currentDialog = AlertDialog.Builder(this)
            .setTitle("正在定位")
            .setMessage("请稍候，正在获取当前位置…")
            .setCancelable(true)
            .setOnCancelListener { showManualProvinceSheet() }
            .show()

        locationProvider.getCurrentLocation { result ->
            runOnUiThread {
                currentDialog?.dismiss()
                currentDialog = null
                when (result) {
                    is CurrentLocationProvider.Result.Ok -> {
                        // 省级走射线法、市/县级走质心最近法——一次反查得到三级完整 snapshot。
                        val snapshot = regionRepo.resolve(result.lng, result.lat, provinceRepo)
                        if (snapshot.provinceCode.isNotEmpty()) {
                            selectedRegion = snapshot
                            updateProvinceDisplay()
                            checkSaveEnabled()
                            Toast.makeText(this,
                                "已识别：${snapshot.display()}", Toast.LENGTH_SHORT).show()
                        } else {
                            // 坐标落到包外多边形外，省都没中——保留坐标，回退到手选省
                            selectedRegion = snapshot
                            updateProvinceDisplay()
                            Toast.makeText(this, "未能识别当前位置所属省份",
                                Toast.LENGTH_SHORT).show()
                            showManualProvinceSheet()
                        }
                    }
                    is CurrentLocationProvider.Result.Err -> {
                        Toast.makeText(this, "定位失败 (${result.reason})",
                            Toast.LENGTH_SHORT).show()
                        showManualProvinceSheet()
                    }
                }
            }
        }
    }

    /** 手选三级地区：先用底部 sheet 选省 → 选市 → 选县。每选一层进入下一层。 */
    private fun showManualProvinceSheet() {
        dismissCurrentDialog()
        val sheet = BottomSheetDialog(this).also { currentDialog = it }
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(),
                (24 * density).toInt(), (16 * density).toInt())
        }
        root.addView(TextView(this).apply {
            text = "选择省份"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        })
        val scroll = android.widget.ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (p in provinceRepo.all().sortedBy { it.code }) {
            list.addView(TextView(this).apply {
                text = "  ${p.name}"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setPadding(0, (14 * density).toInt(), 0, (14 * density).toInt())
                setOnClickListener {
                    sheet.dismiss()
                    selectedRegion = RegionSnapshot(
                        provinceCode = p.code, provinceName = p.name
                    )
                    updateProvinceDisplay()
                    checkSaveEnabled()
                    showManualCitySheet(p.code, p.name)
                }
            })
            list.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setPadding(0, 0, 0, 0) }
                setBackgroundColor(0xFFEEEEEE.toInt())
            })
        }
        scroll.addView(list)
        root.addView(scroll)
        sheet.setContentView(root)
        sheet.show()
    }

    private fun showManualCitySheet(provinceCode: String, provinceName: String) {
        val cities = regionRepo.citiesOf(provinceCode)
        if (cities.isEmpty()) {
            // 该省没有市级质心数据（assets/regions.json 还没补全）
            Toast.makeText(this, "暂无 $provinceName 市级数据", Toast.LENGTH_SHORT).show()
            return
        }
        dismissCurrentDialog()
        val sheet = BottomSheetDialog(this).also { currentDialog = it }
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(),
                (24 * density).toInt(), (16 * density).toInt())
        }
        root.addView(TextView(this).apply {
            text = "$provinceName／选择市"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        })
        val scroll = android.widget.ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (c in cities) {
            list.addView(TextView(this).apply {
                text = "  ${c.name}"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setPadding(0, (14 * density).toInt(), 0, (14 * density).toInt())
                setOnClickListener {
                    sheet.dismiss()
                    selectedRegion = selectedRegion.copy(
                        provinceCode = provinceCode, provinceName = provinceName,
                        cityCode = c.code, cityName = c.name,
                        lng = c.lng, lat = c.lat,
                        districtCode = "", districtName = ""
                    )
                    updateProvinceDisplay()
                    checkSaveEnabled()
                    showManualDistrictSheet(provinceCode, provinceName, c)
                }
            })
            list.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setPadding(0, 0, 0, 0) }
                setBackgroundColor(0xFFEEEEEE.toInt())
            })
        }
        scroll.addView(list)
        root.addView(scroll)
        sheet.setContentView(root)
        sheet.show()
    }

    private fun showManualDistrictSheet(provinceCode: String, provinceName: String, city: com.woshiwangnima.healthdietpro.model.region.Region) {
        val districts = regionRepo.districtsOf(city.code)
        if (districts.isEmpty()) {
            // 该市没有县级质心数据，保留市一级
            return
        }
        dismissCurrentDialog()
        val sheet = BottomSheetDialog(this).also { currentDialog = it }
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (20 * density).toInt(),
                (24 * density).toInt(), (16 * density).toInt())
        }
        root.addView(TextView(this).apply {
            text = "${city.name}／选择县/区"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
        })
        val scroll = android.widget.ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (d in districts) {
            list.addView(TextView(this).apply {
                text = "  ${d.name}"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setPadding(0, (14 * density).toInt(), 0, (14 * density).toInt())
                setOnClickListener {
                    selectedRegion = selectedRegion.copy(
                        provinceCode = provinceCode, provinceName = provinceName,
                        cityCode = city.code, cityName = city.name,
                        districtCode = d.code, districtName = d.name,
                        lng = d.lng, lat = d.lat
                    )
                    updateProvinceDisplay()
                    checkSaveEnabled()
                    sheet.dismiss()
                }
            })
            list.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).apply { setPadding(0, 0, 0, 0) }
                setBackgroundColor(0xFFEEEEEE.toInt())
            })
        }
        scroll.addView(list)
        root.addView(scroll)
        sheet.setContentView(root)
        sheet.show()
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
        val names = diseases.map { it.name }.toTypedArray()
        val checked = BooleanArray(diseases.size) { i ->
            diseases[i].id in selectedDiseaseIds
        }

        currentDialog = AlertDialog.Builder(this)
            .setTitle("选择特殊病史")
            .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                if (!enabled[which]) return@setMultiChoiceItems
                if (isChecked) {
                    selectedDiseaseIds.add(diseases[which].id)
                } else {
                    selectedDiseaseIds.remove(diseases[which].id)
                }
            }
            .setPositiveButton("确定") { _, _ ->
                currentDialog = null
                updateDiseaseDisplay()
                checkSaveEnabled()
            }
            .setOnDismissListener { currentDialog = null }
            .show()
            .also { dialog ->
                dialog.listView.post {
                    for (i in diseases.indices) {
                        if (!enabled[i]) {
                            (dialog.listView.getChildAt(i) as? android.widget.TextView)?.let { child ->
                                child.isEnabled = false
                                child.alpha = 0.4f
                                child.paint.flags = child.paint.flags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                            }
                        }
                    }
                }
            }
    }

    private fun updateDiseaseDisplay() {
        if (selectedDiseaseIds.isEmpty()) {
            binding.diseaseDisplay.text = "无"
        } else {
            val diseases = diseaseRepo.loadAll()
            val names = selectedDiseaseIds.map { id ->
                diseases.find { it.id == id }?.name ?: id
            }
            binding.diseaseDisplay.text = names.joinToString("、")
        }
    }

    private fun updateHeightDisplay() {
        val latest = heightRecords.maxByOrNull { it.date }
        binding.heightDisplay.text = if (latest != null) "${latest.value} cm" else "无记录"
    }

    private fun updateWeightDisplay() {
        val latest = weightRecords.maxByOrNull { it.date }
        binding.weightDisplay.text = if (latest != null) "${latest.value} kg" else "无记录"
        updateBmiDisplay()
    }

    private fun updateBmiDisplay() {
        val latestH = heightRecords.maxByOrNull { it.date }
        val latestW = weightRecords.maxByOrNull { it.date }
        if (latestH != null && latestW != null) {
            val bmi = BmiUtil.computeBmi(latestW.value, latestH.value)
            val label = BmiUtil.getBmiLabel(bmi)
            binding.bmiDisplay.text = "%.1f %s".format(bmi, label)
        } else {
            binding.bmiDisplay.text = "无记录"
        }
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
            val file = File(dir, avatarFileName)
            FileOutputStream(file).use { out ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            refreshAvatarDisplay()
            checkSaveEnabled()
        } catch (_: Exception) {
            Toast.makeText(this, "\u65E0\u6CD5\u52A0\u8F7D\u56FE\u7247", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropToSquare(bitmap: Bitmap, maxSize: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val side = minOf(w, h)
        val x = (w - side) / 2
        val y = (h - side) / 2
        val square = Bitmap.createBitmap(bitmap, x, y, side, side)
        if (side <= maxSize) return square
        return Bitmap.createScaledBitmap(square, maxSize, maxSize, true)
    }

    private fun refreshAvatarDisplay() {
        if (avatarFileName.isNotEmpty()) {
            val file = File(filesDir, "avatars/$avatarFileName")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.avatarEditText.text = ""
                val d = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(resources, bitmap)
                d.isCircular = true
                binding.avatarEditText.background = d
                return
            }
        }
        val profile = ProfilePrefs.load(this)
        val initial = if (profile.name.isNotEmpty()) profile.name.first().toString() else "?"
        binding.avatarEditText.text = initial
        binding.avatarEditText.background = getDrawable(R.drawable.avatar_circle)
    }

    private fun saveProfile() {
        val name = binding.nameInput.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show()
            return
        }
        val gender = selectedGender
        val profile = UserProfile(
            id = editingUserId,
            name = name,
            gender = gender,
            birthday = selectedBirthday,
            region = selectedRegion,
            diseaseIds = selectedDiseaseIds.toList(),
            heightRecords = heightRecords.toList(),
            weightRecords = weightRecords.toList(),
            avatarFileName = avatarFileName
        )
        ProfilePrefs.save(this, profile)
        setResult(RESULT_OK)
        finish()
    }
}

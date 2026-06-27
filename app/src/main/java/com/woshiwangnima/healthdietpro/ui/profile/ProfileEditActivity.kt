package com.woshiwangnima.healthdietpro.ui.profile

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class ProfileEditActivity : BaseBackActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var diseaseRepo: DiseaseRepository

    private var selectedBirthday: AppDate? = null
    private var selectedProvince: String = ""
    private var selectedDiseaseIds: MutableList<String> = mutableListOf()
    private var selectedGender: Gender = Gender.MALE

    private var heightRecords: MutableList<BodyRecord> = mutableListOf()
    private var weightRecords: MutableList<BodyRecord> = mutableListOf()
    private var originalProfile: UserProfile? = null
    private var editingUserId: String = ""
    private var isNewUser: Boolean = false
    private var avatarFileName: String = ""

    private val avatarPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleAvatarSelected(it) }
    }

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

    override fun getTitleText(): String = "编辑个人信息"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        diseaseRepo = DiseaseRepository(this)

        loadProfile()
        setupClickListeners()
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val name = binding.nameInput.text.toString().trim()
                val gender = if (binding.genderMale.isChecked) Gender.MALE else Gender.FEMALE
                val profile = UserProfile(
                    id = editingUserId,
                    name = name,
                    gender = gender,
                    birthday = selectedBirthday,
                    province = selectedProvince,
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
        if (profile.gender == Gender.MALE) binding.genderMale.isChecked = true
        else binding.genderFemale.isChecked = true

        profile.birthday?.let {
            selectedBirthday = it
            binding.birthdayDisplay.text = it.date
        }
        if (profile.province.isNotEmpty()) {
            selectedProvince = profile.province
            binding.provinceDisplay.text = profile.province
        }
        selectedDiseaseIds = profile.diseaseIds.toMutableList()
        updateDiseaseDisplay()

        heightRecords = profile.heightRecords.toMutableList()
        updateHeightDisplay()
        weightRecords = profile.weightRecords.toMutableList()
        updateWeightDisplay()

        avatarFileName = profile.avatarFileName
        refreshAvatarDisplay()

        originalProfile = profile
        checkSaveEnabled()
    }

    private fun setupClickListeners() {
        binding.avatarEditText.setOnClickListener { avatarPickerLauncher.launch("image/*") }
        binding.birthdayDisplay.setOnClickListener { showDatePicker() }
        binding.provinceDisplay.setOnClickListener { showProvincePicker() }
        binding.diseaseDisplay.setOnClickListener { showDiseasePicker() }

        binding.genderGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedGender = if (checkedId == R.id.genderMale) Gender.MALE else Gender.FEMALE
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
        val currentGender = if (binding.genderMale.isChecked) Gender.MALE else Gender.FEMALE
        val changed = currentName != orig.name ||
            currentGender != orig.gender ||
            selectedBirthday != orig.birthday ||
            selectedProvince != orig.province ||
            selectedDiseaseIds.toList() != orig.diseaseIds ||
            heightRecords.toList() != orig.heightRecords ||
            weightRecords.toList() != orig.weightRecords ||
            avatarFileName != orig.avatarFileName
        binding.saveBtn.isEnabled = changed
    }

    private fun showDatePicker() {
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
        DatePickerDialog(
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
        ).show()
    }

    private fun showProvincePicker() {
        val provinces = arrayOf(
            "北京", "天津", "河北省", "山西省", "内蒙古",
            "辽宁省", "吉林省", "黑龙江省", "上海", "江苏省",
            "浙江省", "安徽省", "福建省", "江西省", "山东省",
            "河南省", "湖北省", "湖南省", "广东省", "广西",
            "海南省", "重庆", "四川省", "贵州省", "云南省",
            "西藏", "陕西省", "甘肃省", "青海省", "宁夏",
            "新疆", "台湾省", "香港", "澳门"
        )
        AlertDialog.Builder(this)
            .setTitle("选择地区")
            .setItems(provinces) { _, which ->
                selectedProvince = provinces[which]
                binding.provinceDisplay.text = selectedProvince
                checkSaveEnabled()
            }
            .show()
    }

    private fun showDiseasePicker() {
        val diseases = diseaseRepo.getSorted(selectedProvince.ifEmpty { null })
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

        AlertDialog.Builder(this)
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
                updateDiseaseDisplay()
                checkSaveEnabled()
            }
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
        val gender = if (binding.genderMale.isChecked) Gender.MALE else Gender.FEMALE
        val profile = UserProfile(
            id = editingUserId,
            name = name,
            gender = gender,
            birthday = selectedBirthday,
            province = selectedProvince,
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

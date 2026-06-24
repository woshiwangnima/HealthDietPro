package com.woshiwangnima.healthdietpro.ui.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityProfileEditBinding
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRepository
import com.woshiwangnima.healthdietpro.model.profile.AppDate
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.Gender
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import java.util.Calendar

class ProfileEditActivity : BaseBackActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var diseaseRepo: DiseaseRepository

    private var selectedBirthday: AppDate? = null
    private var selectedProvince: String = ""
    private var selectedDiseaseIds: MutableList<String> = mutableListOf()

    private lateinit var heightAdapter: BodyRecordAdapter
    private lateinit var weightAdapter: BodyRecordAdapter
    private var heightRecords: MutableList<BodyRecord> = mutableListOf()
    private var weightRecords: MutableList<BodyRecord> = mutableListOf()

    override fun getTitleText(): String = "编辑个人信息"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar(binding.toolbar)

        diseaseRepo = DiseaseRepository(this)

        setupAdapters()
        loadProfile()
        setupClickListeners()
    }

    private fun setupAdapters() {
        heightAdapter = BodyRecordAdapter("cm")
        weightAdapter = BodyRecordAdapter("kg")

        binding.heightList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.heightList.adapter = heightAdapter

        binding.weightList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.weightList.adapter = weightAdapter

        heightAdapter.onDelete = { pos ->
            heightRecords.removeAt(pos)
            heightAdapter.records = heightRecords
        }
        weightAdapter.onDelete = { pos ->
            weightRecords.removeAt(pos)
            weightAdapter.records = weightRecords
        }
    }

    private fun loadProfile() {
        val profile = ProfilePrefs.load(this)
        binding.nameInput.setText(profile.name)
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
        heightAdapter.records = heightRecords
        weightRecords = profile.weightRecords.toMutableList()
        weightAdapter.records = weightRecords
    }

    private fun setupClickListeners() {
        binding.birthdayDisplay.setOnClickListener { showDatePicker() }
        binding.provinceDisplay.setOnClickListener { showProvincePicker() }
        binding.diseaseDisplay.setOnClickListener { showDiseasePicker() }
        binding.addHeightBtn.setOnClickListener { showAddRecordDialog(true) }
        binding.addWeightBtn.setOnClickListener { showAddRecordDialog(false) }

        binding.saveBtn.setOnClickListener { saveProfile() }
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
            }
            .show()
    }

    private fun showDiseasePicker() {
        val diseases = diseaseRepo.getSorted(selectedProvince.ifEmpty { null })
        val names = diseases.map { it.name }.toTypedArray()
        val checked = BooleanArray(diseases.size) { i ->
            diseases[i].id in selectedDiseaseIds
        }

        val allNames = names + listOf("无")
        val allChecked = checked + booleanArrayOf(selectedDiseaseIds.isEmpty())

        AlertDialog.Builder(this)
            .setTitle("选择特殊病史")
            .setMultiChoiceItems(allNames, allChecked) { _, which, isChecked ->
                if (which == allNames.lastIndex) {
                    if (isChecked) {
                        for (i in allChecked.indices) allChecked[i] = false
                        allChecked[allNames.lastIndex] = true
                        selectedDiseaseIds.clear()
                    }
                } else {
                    allChecked[allNames.lastIndex] = false
                    if (isChecked) {
                        selectedDiseaseIds.add(diseases[which].id)
                    } else {
                        selectedDiseaseIds.remove(diseases[which].id)
                    }
                }
            }
            .setPositiveButton("确定") { _, _ ->
                updateDiseaseDisplay()
            }
            .show()
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

    private fun showAddRecordDialog(isHeight: Boolean) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_body_record, null)
        val dateInput = view.findViewById<TextView>(R.id.dialogRecordDate)
        val valueInput = view.findViewById<TextInputEditText>(R.id.dialogRecordValue)
        val unitText = view.findViewById<TextView>(R.id.dialogRecordUnit)
        unitText.text = if (isHeight) "cm" else "kg"

        val cal = Calendar.getInstance()
        val today = "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dateInput.text = today
        dateInput.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dateInput.text = "%04d-%02d-%02d".format(year, month + 1, day)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle(if (isHeight) "添加身高记录" else "添加体重记录")
            .setView(view)
            .setPositiveButton("添加") { _, _ ->
                val dateStr = dateInput.text.toString()
                val valueStr = valueInput.text.toString()
                if (valueStr.isBlank()) {
                    Toast.makeText(this, "请输入数值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val value = valueStr.toFloatOrNull()
                if (value == null || value <= 0) {
                    Toast.makeText(this, "请输入有效数值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val record = BodyRecord(date = dateStr, value = value)
                if (isHeight) {
                    heightRecords.add(record)
                    heightRecords.sortByDescending { it.date }
                    heightAdapter.records = heightRecords
                } else {
                    weightRecords.add(record)
                    weightRecords.sortByDescending { it.date }
                    weightAdapter.records = weightRecords
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveProfile() {
        val name = binding.nameInput.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show()
            return
        }
        val gender = if (binding.genderMale.isChecked) Gender.MALE else Gender.FEMALE
        val profile = UserProfile(
            name = name,
            gender = gender,
            birthday = selectedBirthday,
            province = selectedProvince,
            diseaseIds = selectedDiseaseIds.toList(),
            heightRecords = heightRecords.toList(),
            weightRecords = weightRecords.toList()
        )
        ProfilePrefs.save(this, profile)
        setResult(RESULT_OK)
        finish()
    }
}

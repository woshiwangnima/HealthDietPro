package com.woshiwangnima.healthdietpro.ui.record

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityMedicationRecordBinding
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.region.ProvinceRepository
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.ui.widget.tab.FilterBar
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabItem
import com.woshiwangnima.healthdietpro.util.TextOverflowUtil
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import com.woshiwangnima.healthdietpro.util.image.WatermarkUtil
import com.woshiwangnima.healthdietpro.util.location.CurrentLocationProvider
import com.woshiwangnima.healthdietpro.util.time.DateTimePicker
import java.io.File
import java.io.FileOutputStream

class MedicationRecordActivity : BaseBackActivity() {

    override fun getTitleText(): String = "记用药"

    companion object {
        const val EXTRA_RECORD_ID = "record_id"
    }

    private lateinit var binding: ActivityMedicationRecordBinding

    // 编辑模式：非空时表示修改已有记录
    private var editingRecordId: String? = null

    // 默认感受标签
    private val defaultFeelings = listOf(
        "恶心", "便秘", "腹胀", "腹泻", "头痛", "头晕",
        "失眠", "嗜睡", "皮疹", "心悸"
    )

    // 时间
    private var selectedTimestamp: Long = System.currentTimeMillis()

    // 单位转换
    private var selectedSpecCategory: UnitCategory? = null
    private var selectedSpecUnitId: String = ""

    // 单位分类下拉所用 unit categories（药品规格常用质量/体积/时间）
    private val specCategories: List<UnitCategory> by lazy {
        val ids = listOf(
            UnitCategory.ID_WEIGHT, UnitCategory.ID_VOLUME, UnitCategory.ID_TIME
        )
        UnitConverter.getRepository()?.getCategories()
            ?.filter { it.id in ids }
            .orEmpty()
    }

    // 图片路径（保存后的本地文件相对路径，相对 filesDir）
    private var photoFileName: String? = null

    // 定位（用于拍照水印）
    private lateinit var locationProvider: CurrentLocationProvider
    private lateinit var provinceRepo: ProvinceRepository

    // 拍照临时 Uri
    private var pendingCameraUri: Uri? = null

    // 拍照权限申请之后再启动相机
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
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
        binding = ActivityMedicationRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        UnitConverter.init(this)
        locationProvider = CurrentLocationProvider(this)
        provinceRepo = ProvinceRepository.fromContext(this)

        // 检查是否为编辑模式
        editingRecordId = intent.getStringExtra(EXTRA_RECORD_ID)
        if (editingRecordId != null) {
            loadRecordForEdit(editingRecordId!!)
        } else {
            // 时间默认显示当前时间
            binding.timeRow.text = DateTimePicker.format(selectedTimestamp)
        }
        binding.timeRow.setOnClickListener {
            DateTimePicker.show(this, selectedTimestamp) { ts ->
                selectedTimestamp = ts
                binding.timeRow.text = DateTimePicker.format(ts)
            }
        }

        setupMedNameInput()
        setupSpecSpinners()
        setupMethodInput()
        setupFeelingsBar()
        setupPhotoButtons()
        setupSaveButton()
        applyOverflowAndHintStyle()

        // 预览尚未加载的图片时占位隐藏
        binding.photoPreview.visibility = View.GONE
    }

    /** 编辑模式：从已有记录回填所有字段。 */
    private fun loadRecordForEdit(recordId: String) {
        val record = MedicationPrefs.getRecords(this).find { it.id == recordId } ?: return
        title = "编辑用药"
        supportActionBar?.title = "编辑用药"
        editingRecordId = record.id

        selectedTimestamp = record.timestamp
        binding.timeRow.text = DateTimePicker.format(record.timestamp)

        binding.medNameInput.setText(record.medicationName)
        binding.doseValueInput.setText(
            if (record.doseValue > 0f) record.doseValue.toString() else ""
        )
        binding.doseUnitInput.setText(record.doseUnit)
        binding.specValueInput.setText(
            if (record.specValue > 0f) record.specValue.toString() else ""
        )
        binding.methodInput.setText(record.method)

        // 规格分类和单位
        val catIdx = specCategories.indexOfFirst { it.id == record.specUnitCategory }
        if (catIdx >= 0) {
            binding.specCategorySpinner.setSelection(catIdx + 1)
            populateUnitSpinner(specCategories[catIdx])
            // +1 补占位偏移；specUnitId 找不到（旧 bug 错位 / 脏数据）则留空不选，不崩
            val unitIdx = specCategories[catIdx].units.indexOfFirst { it.id == record.specUnitId }
            if (unitIdx >= 0) binding.specUnitSpinner.setSelection(unitIdx + 1)
        }

        // 感受
        selectedFeelingLabels = record.feelings
        val feelingIndices = record.feelings.mapNotNull { f ->
            defaultFeelings.indexOf(f).takeIf { it >= 0 }
        }.toSet()
        binding.feelingsBar.setSelected(feelingIndices)
        if (record.feelingNote.isNotEmpty()) {
            val prefix = if (record.feelings.isEmpty()) "" else
                record.feelings.joinToString("；") + "；"
            binding.feelingNoteInput.setText(prefix + record.feelingNote)
        }

        // 照片
        if (!record.photoPath.isNullOrEmpty()) {
            val file = File(filesDir, record.photoPath)
            if (file.exists()) {
                photoFileName = record.photoPath
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.photoPreview.visibility = View.VISIBLE
                    binding.photoPreview.setImageBitmap(bitmap)
                }
            }
        }
    }

    /** 根据偏好设置统一应用文字溢出模式和深色模式提示文本颜色。 */
    private fun applyOverflowAndHintStyle() {
        val overflowMode = com.woshiwangnima.healthdietpro.model.prefs.AppPrefs.getTextOverflowMode(this)
        TextOverflowUtil.apply(binding.feelingNoteInput, overflowMode)

        // 深色模式提示文本颜色：降低透明度（~38%）
        val isDark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val hintAlpha = if (isDark) 0.45f else 0.38f
        val hintColor = androidx.core.content.ContextCompat.getColor(this, R.color.on_surface_variant)
        val alphaColor = (hintColor and 0x00FFFFFF) or ((0xFF * hintAlpha).toInt() shl 24)

        binding.doseValueLayout.defaultHintTextColor =
            android.content.res.ColorStateList.valueOf(alphaColor)
        binding.doseUnitLayout.defaultHintTextColor =
            android.content.res.ColorStateList.valueOf(alphaColor)
        binding.specValueLayout.defaultHintTextColor =
            android.content.res.ColorStateList.valueOf(alphaColor)
        val feelingLayout = binding.feelingNoteInput.parent as? com.google.android.material.textfield.TextInputLayout
        feelingLayout?.defaultHintTextColor =
            android.content.res.ColorStateList.valueOf(alphaColor)
    }

    /** 药品名称：AutoCompleteTextView + 历史。焦点离开或选择时联动预填剂量/规格/方式。 */
    private fun setupMedNameInput() {
        val history = MedicationPrefs.getMedicationNameHistory(this)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            history
        )
        binding.medNameInput.setAdapter(adapter)
        binding.medNameInput.setOnItemClickListener { _, _, _, _ ->
            applyNameDefaults(binding.medNameInput.text.toString().trim())
        }
        // 当用户手输新名后焦点离开也尝试预填（若该名已有历史）
        binding.medNameInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) applyNameDefaults(binding.medNameInput.text.toString().trim())
        }
    }

    /** 名称选定后预填剂量/规格/方式（取该名最近一条历史记录）。 */
    private fun applyNameDefaults(name: String) {
        if (name.isEmpty()) return
        val defaults = MedicationPrefs.findNameDefaults(this, name) ?: return

        binding.doseValueInput.setText(defaults.doseValue.toString())
        binding.doseUnitInput.setText(defaults.doseUnit)

        // 规格要从 spinners 中先选到对应 category 再选 unit
        // 注意：spinner adapter 在 index 0 处插入了 "选择分类"/"选择单位" 占位项
        val catIdx = specCategories.indexOfFirst { it.id == defaults.specUnitCategory }
        if (catIdx >= 0) {
            binding.specCategorySpinner.setSelection(catIdx + 1)
            // spinners 已 setAdapter，再触发 onItemSelected 来填充 unit spinner
            populateUnitSpinner(specCategories[catIdx])
            // +1 补占位偏移；找不到则留空不崩
            val unitIdx = specCategories[catIdx].units.indexOfFirst { it.id == defaults.specUnitId }
            if (unitIdx >= 0) binding.specUnitSpinner.setSelection(unitIdx + 1)
        }
        binding.specValueInput.setText(defaults.specValue.toString())

        binding.methodInput.setText(defaults.method)
    }

    /** 用药规格：先选单位分类下拉，再选具体单位下拉。 */
    private fun setupSpecSpinners() {
        val categoryNames = specCategories.map { it.categoryCn }.toMutableList().apply {
            add(0, "选择分类")
        }
        binding.specCategorySpinner.adapter = TextOverflowUtil.createSpinnerAdapter(
            this, categoryNames
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.specCategorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, v: View?, pos: Int, id: Long
                ) {
                    if (pos == 0) { selectedSpecCategory = null; populateUnitSpinner(null) }
                    else {
                        val cat = specCategories[pos - 1]
                        selectedSpecCategory = cat
                        populateUnitSpinner(cat)
                    }
                }

                override fun onNothingSelected(p: AdapterView<*>?) {
                    selectedSpecCategory = null
                    populateUnitSpinner(null)
                }
            }

        populateUnitSpinner(null)
        binding.specUnitSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, v: View?, pos: Int, id: Long
                ) {
                    val cat = selectedSpecCategory
                    // adapter 在 index 0 插入了"选择单位"占位项，真实单位在 pos-1
                    if (cat == null || pos <= 0 || pos > cat.units.size) selectedSpecUnitId = ""
                    else selectedSpecUnitId = cat.units[pos - 1].id
                }

                override fun onNothingSelected(p: AdapterView<*>?) {
                    selectedSpecUnitId = ""
                }
            }
    }

    private fun populateUnitSpinner(cat: UnitCategory?) {
        val units = cat?.units.orEmpty()
        val adapter = TextOverflowUtil.createSpinnerAdapter(
            this,
            units.map { "${it.symbolCn} (${it.id})" }.toMutableList().apply {
                add(0, "选择单位")
            }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.specUnitSpinner.adapter = adapter
    }

    /** 用药方式：内置默认 + 历史，AutoCompleteTextView。 */
    private fun setupMethodInput() {
        val history = MedicationPrefs.getMethodHistory(this)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            history
        )
        binding.methodInput.setAdapter(adapter)
        binding.methodInput.threshold = 1
    }

    /** 用药感受：FilterBar 多选 + 自由输入框。 */
    private var selectedFeelingLabels: List<String> = emptyList()

    private fun setupFeelingsBar() {
        binding.feelingsBar.setTabs(defaultFeelings.map { TabItem(label = it) })
        binding.feelingsBar.listener = { selected: Set<Int> ->
            selectedFeelingLabels = selected.map { defaultFeelings[it] }
            syncFeelingInput()
        }
    }

    /** 把选中标签作为前缀写入输入框，分号分隔；保留用户原有正文。 */
    private fun syncFeelingInput() {
        val userNote = stripExistingPrefix(binding.feelingNoteInput.text.toString())
        val prefix = if (selectedFeelingLabels.isEmpty()) "" else
            selectedFeelingLabels.joinToString("；") + "；"
        binding.feelingNoteInput.setText(prefix + userNote)
        // 光标移到末尾
        binding.feelingNoteInput.setSelection(binding.feelingNoteInput.text.toString().length)
    }

    private fun stripExistingPrefix(text: String): String {
        // 已有前缀形如「恶心；腹泻；」→ 把这些分号分隔的标签全删掉，保留正文。
        var rest = text.trimStart()
        for (label in defaultFeelings) {
            if (rest.startsWith(label + "；")) {
                rest = rest.removePrefix(label + "；")
            } else if (rest.startsWith(label + ";")) {
                rest = rest.removePrefix(label + ";")
            }
        }
        return rest
    }

    /** 图片记录：拍照 / 选相册。 */
    private fun setupPhotoButtons() {
        binding.takePhotoBtn.setOnClickListener {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) launchCamera() else
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        binding.pickPhotoBtn.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun launchCamera() {
        // 准备临时文件 + 通过 FileProvider 暴露给相机
        val dir = File(filesDir, "medication_photos").apply { if (!exists()) mkdirs() }
        val tmpName = "camera_${System.currentTimeMillis()}.jpg"
        val tmpFile = File(dir, tmpName)
        pendingCameraUri = FileProvider.getUriForFile(
            this, "${packageName}.fileprovider", tmpFile
        )
        // 临时文件名先记下，成功保存到位后改正式名
        pendingCameraUri?.let { cameraLauncher.launch(it) }
    }

    /**
     * 拍照完成：解码后立即追加时间+地点水印，再写入正式文件。
     */
    private fun handleCapturedPhoto(uri: Uri) {
        try {
            val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return
            // 异步反查当前位置（不阻塞 UI 但水印合并在主线程")
            locationProvider.getCurrentLocation { result ->
                runOnUiThread {
                    val locText = when (result) {
                        is CurrentLocationProvider.Result.Ok -> {
                            val p = provinceRepo.findByPoint(result.lng, result.lat)
                            p?.name ?: "(未识别省份)"
                        }
                        else -> ""
                    }
                    saveAndPreview(WatermarkUtil.apply(
                        bitmap,
                        selectedTimestamp,
                        locText
                    ))
                    // 临时 input stream 已读完，可删 camera temp
                    runCatching { File(uri.path ?: "").delete() }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法加载照片", Toast.LENGTH_SHORT).show()
        }
    }

    /** 选相册：不加水印（已存在的图片默认按原样存储），仅预览与保存。 */
    private fun handlePickedPhoto(uri: Uri) {
        try {
            val raw = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
            val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return
            saveAndPreview(bitmap)
        } catch (_: Exception) {
            Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAndPreview(bitmap: Bitmap) {
        val dir = File(filesDir, "medication_photos").apply { if (!exists()) mkdirs() }
        val fileName = "med_${System.currentTimeMillis()}.jpg"
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        photoFileName = "medication_photos/$fileName"
        binding.photoPreview.visibility = View.VISIBLE
        binding.photoPreview.setImageBitmap(bitmap)
    }

    /** 保存记录：组装 MedicationRecord 并存盘。 */
    private fun setupSaveButton() {
        binding.saveBtn.setOnClickListener {
            val name = binding.medNameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "请填写药品名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dose = binding.doseValueInput.text.toString().toFloatOrNull()
            val doseUnit = binding.doseUnitInput.text.toString().trim()
            val spec = binding.specValueInput.text.toString().toFloatOrNull()
            val method = binding.methodInput.text.toString().trim()

            // 规范化感受：从输入框解析出选中的标签 + 正文
            val noteText = binding.feelingNoteInput.text.toString()
            val feelingsInNote = defaultFeelings.filter { noteText.contains(it) }
            val noteBody = stripExistingPrefix(noteText)
            // 此外用户可能在多选 tab 上又选了标签，统一以多选为准
            val feelings = (feelingsInNote + selectedFeelingLabels).distinct()

            val recordId = editingRecordId
                ?: "${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
            val record = MedicationRecord(
                id = recordId,
                timestamp = selectedTimestamp,
                medicationName = name,
                doseValue = dose ?: 0f,
                doseUnit = doseUnit,
                specValue = spec ?: 0f,
                specUnitCategory = selectedSpecCategory?.id ?: "",
                specUnitId = selectedSpecUnitId,
                method = method,
                feelings = feelings,
                feelingNote = noteBody,
                photoPath = photoFileName
            )
            if (editingRecordId != null) {
                val all = MedicationPrefs.getRecords(this).toMutableList()
                val idx = all.indexOfFirst { it.id == editingRecordId }
                if (idx >= 0) all[idx] = record else all.add(record)
                MedicationPrefs.saveRecords(this, all)
            } else {
                MedicationPrefs.addRecord(this, record)
            }
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
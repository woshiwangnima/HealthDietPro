package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityPreferencesBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.ui.theme.FontStyle
import com.woshiwangnima.healthdietpro.ui.theme.applyFontStyle
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class PreferencesActivity : BaseBackActivity() {

    private lateinit var binding: ActivityPreferencesBinding

    override fun getTitleText(): String = "偏好设置"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        UnitConverter.init(this)
        applyFontStyles()
        buildFontPreviews()
        buildUnitRows()
        refreshDisplay()
        setupClickListeners()
        setupFontScaleBar()
    }

    private fun applyFontStyles() {
        binding.fontSizeHeader.applyFontStyle(FontStyle.SUBTITLE)
        binding.unitHeader.applyFontStyle(FontStyle.SUBTITLE)
        binding.otherHeader.applyFontStyle(FontStyle.SUBTITLE)
        binding.fontScaleValue.applyFontStyle(FontStyle.CAPTION)

        findLabelInRow(binding.firstDayRow)?.applyFontStyle(FontStyle.BODY)
        findLabelInRow(binding.darkModeRow)?.applyFontStyle(FontStyle.BODY)
        findLabelInRow(binding.textOverflowRow)?.applyFontStyle(FontStyle.BODY)
        findLabelInRow(binding.marqueeSpeedRow)?.applyFontStyle(FontStyle.BODY)
    }

    private fun findLabelInRow(row: android.view.ViewGroup): TextView? {
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i)
            if (child is TextView && child.id != R.id.firstDayValue &&
                child.id != R.id.darkModeValue && child.id != R.id.textOverflowValue &&
                child.id != R.id.marqueeSpeedValue && child.id != R.id.rowValue
            ) return child
        }
        return null
    }

    private fun buildFontPreviews() {
        val container = binding.fontPreviewContainer
        container.removeAllViews()
        val styles = listOf(FontStyle.CAPTION, FontStyle.LABEL, FontStyle.BODY,
            FontStyle.SUBTITLE, FontStyle.TITLE, FontStyle.HEADLINE, FontStyle.DISPLAY)
        for (style in styles) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val label = TextView(this).apply {
                text = "${style.cnName}"
                applyFontStyle(FontStyle.CAPTION)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
            }
            val sizeInfo = TextView(this).apply {
                text = "%.0fsp".format(FontStyle.sp(this@PreferencesActivity, style))
                applyFontStyle(FontStyle.CAPTION)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.15f)
            }
            val preview = TextView(this).apply {
                text = "预览ABcd123"
                applyFontStyle(style)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.65f)
            }
            row.addView(label)
            row.addView(sizeInfo)
            row.addView(preview)
            container.addView(row)
        }
    }

    private fun buildUnitRows() {
        val repo = UnitConverter.getRepository() ?: return
        val container = binding.unitSettingsContainer
        container.removeAllViews()

        for (category in repo.getCategories()) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_preference_row, null)
            val label = row.findViewById<TextView>(R.id.rowLabel)
            val value = row.findViewById<TextView>(R.id.rowValue)

            label.applyFontStyle(FontStyle.BODY)
            label.text = category.categoryCn
            val currentId = AppPrefs.getUnit(this, category.id, category.baseUnit)
            val currentUnit = category.units.find { it.id == currentId }
            value.text = currentUnit?.let { "${it.symbolCn} ${it.symbolEn}" } ?: currentId

            row.setOnClickListener {
                val items = category.units.map { "${it.symbolCn}  ${it.symbolEn}" }.toTypedArray()
                val checkedIndex = category.units.indexOfFirst { u ->
                    u.id == AppPrefs.getUnit(this@PreferencesActivity, category.id, category.baseUnit)
                }.coerceAtLeast(0)
                showPicker(category.categoryCn, items, checkedIndex) { which ->
                    AppPrefs.setUnit(this@PreferencesActivity, category.id, category.units[which].id)
                    buildUnitRows()
                }
            }
            container.addView(row)
        }
    }

    private fun refreshDisplay() {
        binding.firstDayValue.text =
            if (AppPrefs.getFirstDayOfWeek(this) == "MONDAY") "周一" else "周日"
        binding.darkModeValue.text = when (AppPrefs.getDarkMode(this)) {
            "FOLLOW_SYSTEM" -> "跟随系统"
            "YES" -> "深色模式"
            else -> "浅色模式"
        }
        refreshOverflowDisplay()
    }

    private fun refreshOverflowDisplay() {
        val mode = AppPrefs.getTextOverflowMode(this)
        binding.textOverflowValue.text = if (mode == "marquee") "左右轮播" else "自适应缩小"
        binding.marqueeSpeedRow.visibility = if (mode == "marquee") android.view.View.VISIBLE else android.view.View.GONE
        binding.marqueeSpeedValue.text = "${AppPrefs.getMarqueeSpeed(this)}"
    }

    private fun setupClickListeners() {
        binding.firstDayRow.setOnClickListener {
            val items = arrayOf("周一", "周日")
            val checkedIndex = if (AppPrefs.getFirstDayOfWeek(this) == "MONDAY") 0 else 1
            showPicker("每周第一天", items, checkedIndex) { which ->
                AppPrefs.setFirstDayOfWeek(this, if (which == 0) "MONDAY" else "SUNDAY")
                refreshDisplay()
            }
        }

        binding.darkModeRow.setOnClickListener {
            val items = arrayOf("跟随系统", "深色模式", "浅色模式")
            val modes = arrayOf("FOLLOW_SYSTEM", "YES", "NO")
            val currentMode = AppPrefs.getDarkMode(this)
            val checkedIndex = modes.indexOf(currentMode).coerceAtLeast(0)
            showPicker("深色模式", items, checkedIndex) { which ->
                AppPrefs.setDarkMode(this, modes[which])
                refreshDisplay()
                AppCompatDelegate.setDefaultNightMode(when (modes[which]) {
                    "YES" -> AppCompatDelegate.MODE_NIGHT_YES
                    "NO" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                })
            }
        }

        binding.textOverflowRow.setOnClickListener {
            val items = arrayOf("自适应缩小", "左右轮播")
            val modes = arrayOf("shrink", "marquee")
            val checkedIndex = modes.indexOf(AppPrefs.getTextOverflowMode(this)).coerceAtLeast(0)
            showPicker("文字溢出处理", items, checkedIndex) { which ->
                AppPrefs.setTextOverflowMode(this, modes[which])
                refreshOverflowDisplay()
            }
        }

        binding.marqueeSpeedRow.setOnClickListener {
            val input = EditText(this).apply {
                setText("${AppPrefs.getMarqueeSpeed(this@PreferencesActivity)}")
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            AlertDialog.Builder(this)
                .setTitle("轮播速度 (ms/字)")
                .setView(input)
                .setPositiveButton("确定") { _, _ ->
                    val speed = input.text.toString().toIntOrNull()?.coerceIn(50, 2000) ?: 200
                    AppPrefs.setMarqueeSpeed(this@PreferencesActivity, speed)
                    refreshOverflowDisplay()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun setupFontScaleBar() {
        val currentScale = AppPrefs.getFontScale(this)
        val progress = ((currentScale - 0.8f) / 0.7f * 70f).toInt().coerceIn(0, 70)
        binding.fontScaleSeekBar.progress = progress
        binding.fontScaleValue.text = "${(currentScale * 100).toInt()}%"

        binding.fontScaleSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                val scale = 0.8f + p / 70f * 0.7f
                binding.fontScaleValue.text = "${(scale * 100).toInt()}%"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                val p = seekBar?.progress ?: 22
                val scale = 0.8f + p / 70f * 0.7f
                AppPrefs.setFontScale(this@PreferencesActivity, scale)
                buildFontPreviews()
                applyFontStyles()
            }
        })
    }

    private fun showPicker(title: String, items: Array<String>, checkedIndex: Int, onSelected: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(items, checkedIndex) { dialog, which ->
                onSelected(which)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

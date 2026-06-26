package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
        buildUnitRows()
        refreshDisplay()
        setupClickListeners()
        setupFontScaleBar()
        addSectionDividers()
    }

    private fun addSectionDividers() {
        val divider1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(16, 0, 16, 0)
            }
            setBackgroundColor(0x33000000)
        }
        val divider2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(16, 0, 16, 0)
            }
            setBackgroundColor(0x33000000)
        }
        val main = binding.root as LinearLayout
        // Find unitSettingsContainer index and insert divider before/after
        val unitIdx = main.indexOfChild(binding.unitSettingsContainer)
        if (unitIdx >= 0) {
            // Insert divider before 默认单位偏好 section
            val fontHeaderIdx = unitIdx - 1 // The "默认字体大小" header is just before
            if (fontHeaderIdx >= 0) main.addView(divider1, fontHeaderIdx)
            // Insert divider after unit container
            main.addView(divider2, unitIdx + 2)
        }
    }

    private fun applyFontStyles() {
        // Section headers: find TextViews with specific text
        findSectionHeader("默认字体大小")?.applyFontStyle(FontStyle.SUBTITLE)
        findSectionHeader("默认单位偏好")?.applyFontStyle(FontStyle.SUBTITLE)

        binding.firstDayRow?.let { row ->
            findLabelInRow(row)?.applyFontStyle(FontStyle.BODY)
        }
        binding.darkModeRow?.let { row ->
            findLabelInRow(row)?.applyFontStyle(FontStyle.BODY)
        }
    }

    private fun findSectionHeader(text: String): TextView? {
        val main = binding.root as LinearLayout
        for (i in 0 until main.childCount) {
            val child = main.getChildAt(i)
            if (child is TextView && child.text == text) return child
        }
        return null
    }

    private fun findLabelInRow(row: android.view.ViewGroup): TextView? {
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i)
            if (child is TextView && child.id != R.id.firstDayValue && child.id != R.id.darkModeValue && child.id != R.id.rowValue) return child
        }
        return null
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
                val checkedIndex = category.units.indexOfFirst { u -> u.id == AppPrefs.getUnit(this@PreferencesActivity, category.id, category.baseUnit) }.coerceAtLeast(0)
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
                val nightMode = when (modes[which]) {
                    "YES" -> AppCompatDelegate.MODE_NIGHT_YES
                    "NO" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
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

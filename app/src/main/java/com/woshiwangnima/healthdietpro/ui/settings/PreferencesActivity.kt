package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityPreferencesBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
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

        refreshDisplay()
        setupClickListeners()
    }

    private fun refreshDisplay() {
        binding.heightUnitValue.text = AppPrefs.getHeightUnit(this)
        binding.weightUnitValue.text = AppPrefs.getWeightUnit(this)
        binding.energyUnitValue.text = AppPrefs.getEnergyUnit(this)
        binding.firstDayValue.text =
            if (AppPrefs.getFirstDayOfWeek(this) == "MONDAY") "周一" else "周日"
        binding.darkModeValue.text = when (AppPrefs.getDarkMode(this)) {
            "FOLLOW_SYSTEM" -> "跟随系统"
            "YES" -> "深色模式"
            else -> "浅色模式"
        }
    }

    private fun setupClickListeners() {
        binding.heightUnitRow.setOnClickListener {
            showPicker("身高单位", arrayOf("cm", "m", "mm", "in", "ft"),
                AppPrefs.getHeightUnit(this)) { selected ->
                AppPrefs.setHeightUnit(this, selected)
                refreshDisplay()
            }
        }
        binding.weightUnitRow.setOnClickListener {
            showPicker("体重单位", arrayOf("kg", "g", "lb"),
                AppPrefs.getWeightUnit(this)) { selected ->
                AppPrefs.setWeightUnit(this, selected)
                refreshDisplay()
            }
        }
        binding.energyUnitRow.setOnClickListener {
            showPicker("热量单位", arrayOf("kcal", "kJ"),
                AppPrefs.getEnergyUnit(this)) { selected ->
                AppPrefs.setEnergyUnit(this, selected)
                refreshDisplay()
            }
        }
        binding.firstDayRow.setOnClickListener {
            showPicker("每周第一天", arrayOf("周一", "周日"),
                if (AppPrefs.getFirstDayOfWeek(this) == "MONDAY") "周一" else "周日") { selected ->
                AppPrefs.setFirstDayOfWeek(this, if (selected == "周一") "MONDAY" else "SUNDAY")
                refreshDisplay()
            }
        }
        binding.darkModeRow.setOnClickListener {
            showPicker("深色模式", arrayOf("跟随系统", "深色模式", "浅色模式"),
                when (AppPrefs.getDarkMode(this)) {
                    "FOLLOW_SYSTEM" -> "跟随系统"
                    "YES" -> "深色模式"
                    else -> "浅色模式"
                }) { selected ->
                val mode = when (selected) {
                    "深色模式" -> "YES"
                    "浅色模式" -> "NO"
                    else -> "FOLLOW_SYSTEM"
                }
                AppPrefs.setDarkMode(this, mode)
                refreshDisplay()
                val nightMode = when (mode) {
                    "YES" -> AppCompatDelegate.MODE_NIGHT_YES
                    "NO" -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        }
    }

    private fun showPicker(title: String, items: Array<String>, current: String, onSelected: (String) -> Unit) {
        val checkedIndex = items.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(items, checkedIndex) { dialog, which ->
                onSelected(items[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

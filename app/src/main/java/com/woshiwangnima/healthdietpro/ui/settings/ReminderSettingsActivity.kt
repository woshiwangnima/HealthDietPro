package com.woshiwangnima.healthdietpro.ui.settings

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityReminderSettingsBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class ReminderSettingsActivity : BaseBackActivity() {

    private lateinit var binding: ActivityReminderSettingsBinding

    override fun getTitleText(): String = "提醒设置"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        setupSwitches()
    }

    private fun setupSwitches() {
        initSwitch(binding.drinkReminderSwitch, binding.drinkReminderDetail, binding.drinkReminderArrow,
            { AppPrefs.getReminderDrinkWater(this) },
            { v -> AppPrefs.setReminderDrinkWater(this, v) })
        initSwitch(binding.medReminderSwitch, binding.medReminderDetail, binding.medReminderArrow,
            { AppPrefs.getReminderMedication(this) },
            { v -> AppPrefs.setReminderMedication(this, v) })
        initSwitch(binding.periodReminderSwitch, binding.periodReminderDetail, binding.periodReminderArrow,
            { AppPrefs.getReminderPeriod(this) },
            { v -> AppPrefs.setReminderPeriod(this, v) })
        initSwitch(binding.fastingReminderSwitch, binding.fastingReminderDetail, binding.fastingReminderArrow,
            { AppPrefs.getReminderFasting(this) },
            { v -> AppPrefs.setReminderFasting(this, v) })
    }

    private fun initSwitch(
        switch: MaterialSwitch, detail: LinearLayout, arrow: TextView,
        getter: () -> Boolean, setter: (Boolean) -> Unit
    ) {
        switch.isChecked = getter()
        updateDetailState(arrow, getter())
        switch.setOnCheckedChangeListener { _, isChecked ->
            setter(isChecked)
            updateDetailState(arrow, isChecked)
        }
        detail.setOnClickListener { /* TODO */ }
    }

    private fun updateDetailState(arrow: TextView, enabled: Boolean) {
        arrow.alpha = if (enabled) 1f else 0.3f
    }
}

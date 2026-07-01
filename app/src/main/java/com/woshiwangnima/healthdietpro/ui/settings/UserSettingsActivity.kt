package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.os.Bundle
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityUserSettingsBinding
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class UserSettingsActivity : BaseBackActivity() {

    private lateinit var binding: ActivityUserSettingsBinding

    override fun getTitleText(): String = "用户设置"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        binding.reminderSettingsRow.setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }
        binding.prefSettingsRow.setOnClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java))
        }
    }
}
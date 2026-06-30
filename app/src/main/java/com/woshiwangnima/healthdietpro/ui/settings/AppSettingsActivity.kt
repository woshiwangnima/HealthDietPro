package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityAppSettingsBinding
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class AppSettingsActivity : BaseBackActivity() {

    private lateinit var binding: ActivityAppSettingsBinding

    override fun getTitleText(): String = "设置"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        setupListeners()
    }

    private fun setupListeners() {
        binding.messageSettingsRow.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            })
        }
        binding.reminderSettingsRow.setOnClickListener {
            startActivity(Intent(this, ReminderSettingsActivity::class.java))
        }
        binding.prefSettingsBtn.setOnClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java))
        }
    }
}

package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityAppSettingsBinding
import com.woshiwangnima.healthdietpro.ui.theme.FontStyle
import com.woshiwangnima.healthdietpro.ui.theme.applyFontStyle
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

        applyFontStyles()
        setupListeners()
    }

    private fun applyFontStyles() {
        findLabelInRow(binding.messageSettingsRow)?.applyFontStyle(FontStyle.SUBTITLE)
        findLabelInRow(binding.reminderSettingsRow)?.applyFontStyle(FontStyle.SUBTITLE)
        findLabelInRow(binding.prefSettingsBtn)?.applyFontStyle(FontStyle.SUBTITLE)
    }

    private fun findLabelInRow(row: android.view.ViewGroup): TextView? {
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i)
            if (child is TextView) return child
            if (child is android.view.ViewGroup) {
                val found = findLabelInRow(child)
                if (found != null) return found
            }
        }
        return null
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

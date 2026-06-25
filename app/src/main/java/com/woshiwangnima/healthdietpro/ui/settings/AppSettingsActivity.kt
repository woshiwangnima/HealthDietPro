package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.os.Bundle
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

        binding.prefSettingsBtn.setOnClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java))
        }
    }
}

package com.woshiwangnima.healthdietpro.base

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

abstract class BaseActivity : AppCompatActivity() {

    private var lastAppliedFontScale: Float = 1f

    override fun attachBaseContext(newBase: Context) {
        val scale = AppPrefs.getFontScale(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = scale
        lastAppliedFontScale = scale
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onResume() {
        super.onResume()
        val current = AppPrefs.getFontScale(this)
        if (current != lastAppliedFontScale) {
            lastAppliedFontScale = current
            recreate()
        }
    }
}
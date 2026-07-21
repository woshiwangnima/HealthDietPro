package com.woshiwangnima.healthdietpro

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

class HealthDietProApplication : Application() {
    internal val cacheRegistry by lazy { AppCacheRegistry(this) }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setApplicationLocales(
            when (AppPrefs.getAppLanguage(this)) {
                "ZH" -> LocaleListCompat.forLanguageTags("zh-CN")
                "EN" -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            },
        )
    }
}

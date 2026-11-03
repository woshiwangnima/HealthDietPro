package com.woshiwangnima.healthdietpro

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.woshiwangnima.healthdietpro.common.cache.AppCacheRegistry
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientRepository
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.ui.profile.ProfileAvatarBitmapCache

class HealthDietProApplication : Application() {
    internal val cacheRegistry by lazy { AppCacheRegistry.from(this) }

    /**
     * 头像位图缓存归应用所有，而不是归 MainActivity：ViewModel 在旋转屏幕后会保留，
     * 若缓存跟着 Activity 重建，保留的 ViewModel 会继续拿着已注销的旧缓存，
     * 「清除缓存」就统计不到它。放在 Application 只注册一次，生命周期与进程一致。
     */
    internal val profileAvatarBitmapCache by lazy {
        ProfileAvatarBitmapCache().also { cacheRegistry.register(it) }
    }

    internal val foodNutrientRepository by lazy { FoodNutrientRepository.fromContext(this) }

    override fun onCreate() {
        super.onCreate()
        // 进程启动时先恢复已保存的深浅色：系统启动窗口的 values-night 资源、状态栏图标与
        // Compose 启动页的 AppTheme 因此从第一帧起就是同一套，不会出现「系统底色亮、页面变暗」。
        AppCompatDelegate.setDefaultNightMode(
            when (AppPrefs.getDarkMode(this)) {
                "YES" -> AppCompatDelegate.MODE_NIGHT_YES
                "NO" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            },
        )
        AppCompatDelegate.setApplicationLocales(
            when (AppPrefs.getAppLanguage(this)) {
                "ZH" -> LocaleListCompat.forLanguageTags("zh-CN")
                "EN" -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            },
        )
    }
}

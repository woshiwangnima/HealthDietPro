package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context

/**
 * 记录中心的功能启用开关，用户级存档（`files/user_archives/<uid>/user_preferences.json`）。
 *
 * 存「被关闭的功能」而不是「被开启的功能」：默认全部可用，新增功能不必给存量用户补写开启记录，
 * 换用户后各读各的，互不影响。
 */
object RecordFeaturePrefs {
    private const val DISABLED_FEATURES_KEY = "record_disabled_features_v1"

    fun disabledFeatureIds(context: Context): Set<String> =
        UserPrefs.current(context).getStringSet(DISABLED_FEATURES_KEY)

    fun setDisabledFeatureIds(context: Context, ids: Set<String>) {
        UserPrefs.current(context).putStringSet(DISABLED_FEATURES_KEY, ids)
    }

    fun isEnabled(context: Context, featureId: String): Boolean =
        featureId !in disabledFeatureIds(context)
}

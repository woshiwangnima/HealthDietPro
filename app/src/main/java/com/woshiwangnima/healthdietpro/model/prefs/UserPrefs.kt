package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context
import android.content.SharedPreferences
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs

/**
 * 按用户存储的设置统一入口。每个用户一个独立 prefs 文件 `user_prefs_<uid>`，
 * uid 为空时回退 `user_prefs_default`。删用户即删文件，隔离清晰。
 *
 * 通过 [current] 获取当前登录用户的 scope；通过 [forUser] 获取指定用户的 scope。
 * 一次性数据迁移见 [ensureMigrated]。
 */
object UserPrefs {
    private const val APP_PREFS_NAME = "app_prefs"
    private const val APP_KEY_MIGRATED = "user_prefs_migrated"

    /** 迁移到用户级的精确键名（app_prefs → user_prefs_<uid>）。 */
    private val MIGRATE_KEYS_EXACT = listOf(
        "reminder_drink_water", "reminder_medication", "reminder_period", "reminder_fasting",
        "tab_weight_chart", "tab_height_chart", "tab_bmi_chart"
    )

    /** 迁移到用户级的前缀键名（匹配任意后缀）。 */
    private val MIGRATE_KEY_PREFIXES = listOf(
        "pref_unit_",
        "chart_style_", "chart_timerange_", "chart_ymin_", "chart_ymax_", "chart_label_"
    )

    fun current(context: Context): UserPrefsScope {
        ensureMigrated(context)
        val uid = ProfilePrefs.getCurrentUserId(context)
        return UserPrefsScope.create(context, uid)
    }

    fun forUser(context: Context, uid: String): UserPrefsScope =
        UserPrefsScope.create(context, uid)

    /** 删除指定用户的 user_prefs_<uid> 文件（删用户时清理）。 */
    fun deleteUserFile(context: Context, uid: String) {
        // API 24+ 支持 deleteSharedPreferences；本项目 minSdk=35，直接用
        context.deleteSharedPreferences(UserPrefsScope.fileName(uid))
    }

    /**
     * 一次性、幂等迁移：把旧 app_prefs 中的用户级键拷贝到当前用户的 user_prefs_<uid>，
     * 然后从 app_prefs 删除这些键。以 app_prefs 中的 `user_prefs_migrated` 标记防重复。
     */
    @Synchronized
    fun ensureMigrated(context: Context) {
        val appPrefs = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        if (appPrefs.getBoolean(APP_KEY_MIGRATED, false)) return
        val uid = ProfilePrefs.getCurrentUserId(context)
        val target = context.getSharedPreferences(UserPrefsScope.fileName(uid), Context.MODE_PRIVATE)
        val allKeys = appPrefs.all.keys.toList()

        val dst = target.edit()
        for (key in MIGRATE_KEYS_EXACT) {
            if (appPrefs.contains(key)) copyAny(appPrefs, key, dst)
        }
        for (key in allKeys) {
            if (MIGRATE_KEY_PREFIXES.any { key.startsWith(it) }) copyAny(appPrefs, key, dst)
        }
        dst.apply()

        val clean = appPrefs.edit()
        for (key in MIGRATE_KEYS_EXACT) clean.remove(key)
        for (key in allKeys) {
            if (MIGRATE_KEY_PREFIXES.any { key.startsWith(it) }) clean.remove(key)
        }
        clean.putBoolean(APP_KEY_MIGRATED, true).apply()
    }

    private fun copyAny(src: SharedPreferences, key: String, dst: SharedPreferences.Editor) {
        val v = src.all[key] ?: return
        when (v) {
            is Boolean -> dst.putBoolean(key, v)
            is Int -> dst.putInt(key, v)
            is Long -> dst.putLong(key, v)
            is Float -> dst.putFloat(key, v)
            is String -> dst.putString(key, v)
            is Set<*> -> @Suppress("UNCHECKED_CAST") dst.putStringSet(key, v as Set<String>)
            else -> null
        }
    }
}

/**
 * 单个用户的设置作用域。持有 uid 与对应的 [SharedPreferences]，方法签名无需再传 uid。
 */
class UserPrefsScope private constructor(
    val context: Context,
    val uid: String,
    private val sp: SharedPreferences
) {
    companion object {
        private const val FILE_PREFIX = "user_prefs_"
        private const val FALLBACK_UID = "default"

        internal fun fileName(uid: String): String =
            if (uid.isEmpty()) "${FILE_PREFIX}${FALLBACK_UID}" else "${FILE_PREFIX}${uid}"

        internal fun create(context: Context, uid: String): UserPrefsScope {
            val sp = context.getSharedPreferences(fileName(uid), Context.MODE_PRIVATE)
            return UserPrefsScope(context, uid, sp)
        }
    }

    fun getBoolean(key: String, default: Boolean) = sp.getBoolean(key, default)
    fun putBoolean(key: String, v: Boolean) { sp.edit().putBoolean(key, v).apply() }

    fun getString(key: String, default: String) = sp.getString(key, default) ?: default
    fun putString(key: String, v: String) { sp.edit().putString(key, v).apply() }

    fun getInt(key: String, default: Int) = sp.getInt(key, default)
    fun putInt(key: String, v: Int) { sp.edit().putInt(key, v).apply() }

    fun getFloat(key: String, default: Float) = sp.getFloat(key, default)
    fun putFloat(key: String, v: Float) { sp.edit().putFloat(key, v).apply() }

    fun getLong(key: String, default: Long) = sp.getLong(key, default)
    fun putLong(key: String, v: Long) { sp.edit().putLong(key, v).apply() }

    fun contains(key: String) = sp.contains(key)
}

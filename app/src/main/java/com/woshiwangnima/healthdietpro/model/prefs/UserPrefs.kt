package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context
import android.content.SharedPreferences
import com.woshiwangnima.healthdietpro.model.archive.ArchiveSchemaVersion
import com.woshiwangnima.healthdietpro.model.archive.appVersion
import com.woshiwangnima.healthdietpro.model.archive.archiveSchemaVersionFromLegacy
import com.woshiwangnima.healthdietpro.model.archive.migrateArchiveSchemaVersion
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

    /** 读取指定用户的全部偏好键值，供归档模块编码为明文 JSON。 */
    internal fun snapshot(context: Context, uid: String): Map<String, Any> =
        context.getSharedPreferences(UserPrefsScope.fileName(uid), Context.MODE_PRIVATE).all
            .mapNotNull { (key, value) -> value?.let { key to it } }
            .toMap()

    /**
     * 以一个 SharedPreferences 事务替换指定用户的全部偏好。
     * 归档格式在替换前已经完成校验，写入完成后立即补齐当前归档版本信息。
     */
    internal fun replaceAll(context: Context, uid: String, values: Map<String, Any>): Boolean {
        val scope = UserPrefsScope.create(context, uid)
        val target = context.getSharedPreferences(UserPrefsScope.fileName(scope.uid), Context.MODE_PRIVATE)
        val editor = target.edit().clear()
        values.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
            }
        }
        return editor.commit().also { committed ->
            if (committed) UserPrefsScope.create(context, uid)
        }
    }

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
        private const val KEY_LEGACY_ARCHIVE_SCHEMA_VERSION = "archive_schema_version"
        private const val KEY_ARCHIVE_SCHEMA_MAJOR = "archive_schema_major"
        private const val KEY_ARCHIVE_SCHEMA_MINOR = "archive_schema_minor"
        private const val KEY_ARCHIVE_SCHEMA_PATCH = "archive_schema_patch"
        private const val KEY_ARCHIVE_APP_VERSION = "archive_app_version"

        internal fun fileName(uid: String): String =
            if (uid.isEmpty()) "${FILE_PREFIX}${FALLBACK_UID}" else "${FILE_PREFIX}${uid}"

        internal fun create(context: Context, uid: String): UserPrefsScope {
            val sp = context.getSharedPreferences(fileName(uid), Context.MODE_PRIVATE)
            return UserPrefsScope(context, uid, sp).also { it.migrateArchiveChain() }
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

    private fun migrateArchiveChain() {
        val editor = sp.edit()
        val storedSchemaVersion = if (
            sp.contains(KEY_ARCHIVE_SCHEMA_MAJOR) ||
                sp.contains(KEY_ARCHIVE_SCHEMA_MINOR) ||
                sp.contains(KEY_ARCHIVE_SCHEMA_PATCH)
        ) {
            ArchiveSchemaVersion(
                major = sp.getInt(KEY_ARCHIVE_SCHEMA_MAJOR, 0),
                minor = sp.getInt(KEY_ARCHIVE_SCHEMA_MINOR, 0),
                patch = sp.getInt(KEY_ARCHIVE_SCHEMA_PATCH, 0),
            )
        } else {
            archiveSchemaVersionFromLegacy(
                if (sp.contains(KEY_LEGACY_ARCHIVE_SCHEMA_VERSION)) {
                    sp.getInt(KEY_LEGACY_ARCHIVE_SCHEMA_VERSION, 0)
                } else {
                    null
                },
            )
        }
        val schemaVersion = migrateArchiveSchemaVersion(storedSchemaVersion)
        editor
            .putInt(KEY_ARCHIVE_SCHEMA_MAJOR, schemaVersion.major)
            .putInt(KEY_ARCHIVE_SCHEMA_MINOR, schemaVersion.minor)
            .putInt(KEY_ARCHIVE_SCHEMA_PATCH, schemaVersion.patch)
            .remove(KEY_LEGACY_ARCHIVE_SCHEMA_VERSION)
        val installedVersion = appVersion(context)
        if (sp.getString(KEY_ARCHIVE_APP_VERSION, "") != installedVersion) {
            editor.putString(KEY_ARCHIVE_APP_VERSION, installedVersion)
        }
        editor.apply()
    }
}

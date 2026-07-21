package com.woshiwangnima.healthdietpro.model.profile

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.woshiwangnima.healthdietpro.model.archive.ArchiveSchemaVersion
import com.woshiwangnima.healthdietpro.model.archive.appVersion
import com.woshiwangnima.healthdietpro.model.archive.archiveSchemaVersionFromLegacy
import com.woshiwangnima.healthdietpro.model.archive.migrateArchiveSchemaVersion
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import java.io.File
import java.lang.reflect.Type

object ProfilePrefs {
    private const val PREFS_NAME = "health_diet_prefs"
    private const val KEY_LEGACY_PROFILE = "user_profile"
    private const val KEY_ALL_USERS = "all_users"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private val profileGson = GsonBuilder()
        .registerTypeAdapter(
            ArchiveSchemaVersion::class.java,
            object : JsonSerializer<ArchiveSchemaVersion>, JsonDeserializer<ArchiveSchemaVersion> {
                override fun serialize(
                    source: ArchiveSchemaVersion,
                    typeOfSource: Type,
                    context: JsonSerializationContext,
                ): JsonElement = com.google.gson.JsonObject().apply {
                    addProperty("major", source.major)
                    addProperty("minor", source.minor)
                    addProperty("patch", source.patch)
                }

                override fun deserialize(
                    source: JsonElement,
                    typeOfTarget: Type,
                    context: JsonDeserializationContext,
                ): ArchiveSchemaVersion = when {
                    source.isJsonPrimitive -> archiveSchemaVersionFromLegacy(source.asInt)
                    source.isJsonObject -> ArchiveSchemaVersion(
                        major = source.asJsonObject.get("major")?.asInt ?: 0,
                        minor = source.asJsonObject.get("minor")?.asInt ?: 0,
                        patch = source.asJsonObject.get("patch")?.asInt ?: 0,
                    )
                    else -> ArchiveSchemaVersion.Unversioned
                }
            },
        )
        .create()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureMigrated(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_ALL_USERS)) {
            // 二次迁移：把老版本里仅存的 `province: String`（省名或 2 位代码）
            // 在 load 时封装成 RegionSnapshot。这里只触发一次：探测首条用户
            // 数据中是否还有遗留的 'province' 字段。
            migrateLegacyProvinceField(context)
            migrateArchiveSchema(context)
            return
        }
        val legacyJson = p.getString(KEY_LEGACY_PROFILE, null) ?: return
        try {
            val legacy: UserProfile = profileGson.fromJson(legacyJson, UserProfile::class.java)
            val migrated = legacy.copy(id = "default")
            saveUserMap(context, listOf(migrated))
            setCurrentUserId(context, "default")
            p.edit().remove(KEY_LEGACY_PROFILE).apply()
        } catch (_: Exception) {}
    }

    private fun migrateArchiveSchema(context: Context) {
        val users = readUsersWithoutMigration(context)
        if (users.isEmpty()) return
        val migratedUsers = users.map { migrateArchiveChain(it, context) }
        if (migratedUsers != users) saveUserMap(context, migratedUsers)
    }

    private fun migrateArchiveChain(user: UserProfile, context: Context): UserProfile {
        var migrated = user
        val schemaVersion = migrateArchiveSchemaVersion(migrated.archiveSchemaVersion)
        if (migrated.archiveSchemaVersion != schemaVersion) {
            migrated = migrated.copy(archiveSchemaVersion = schemaVersion)
        }
        val installedVersion = appVersion(context)
        if (migrated.archiveAppVersion != installedVersion) {
            migrated = migrated.copy(archiveAppVersion = installedVersion)
        }
        return migrated
    }

    /**
     * 二次：旧版本 UserProfile 在 SharedPreferences 里以 `province: String`
     * 存储省名/省代码；新版本直接用 RegionSnapshot。这里是迁移的进入点：
     * 读原始 JSON 把 `province` 字段抽出，构造 RegionSnapshot，再写回。
     */
    private var legacyMigrationDone = false
    private fun migrateLegacyProvinceField(context: Context) {
        if (legacyMigrationDone) return
        val raw = prefs(context).getString(KEY_ALL_USERS, null) ?: run {
            legacyMigrationDone = true; return
        }
        if (!raw.contains("\"province\"")) {
            legacyMigrationDone = true; return
        }
        try {
            // 把每条 user JSON 中的 province 字段拼到 region.provinceCode 上，
            // 同时移除老旧 province 顶层键。
            val arr = com.google.gson.JsonParser.parseString(raw).asJsonArray
            for (i in 0 until arr.size()) {
                val obj = arr[i].asJsonObject
                if (obj.has("province")) {
                    val prov = obj.get("province").asString ?: ""
                    obj.remove("province")
                    val region = obj.getAsJsonObject("region") ?: com.google.gson.JsonObject().also { obj.add("region", it) }
                    val code = normalizeProvinceCode(prov)
                    if (code.isNotEmpty()) {
                        region.addProperty("provinceCode", code)
                    }
                }
            }
            prefs(context).edit().putString(KEY_ALL_USERS, arr.toString()).apply()
        } catch (_: Exception) {}
        legacyMigrationDone = true
    }

    /** 把任意旧 province 值（中文省名 / 2 位码 / 空）统一成 2 位码。 */
    private fun normalizeProvinceCode(stored: String?): String {
        if (stored.isNullOrEmpty()) return ""
        if (stored.length == 2 && stored.all { it.isDigit() }) return stored
        val map = mapOf(
            "北京" to "11", "天津" to "12", "河北省" to "13", "山西省" to "14", "内蒙古" to "15",
            "辽宁省" to "21", "吉林省" to "22", "黑龙江省" to "23", "上海" to "31", "江苏省" to "32",
            "浙江省" to "33", "安徽省" to "34", "福建省" to "35", "江西省" to "36", "山东省" to "37",
            "河南省" to "41", "湖北省" to "42", "湖南省" to "43", "广东省" to "44", "广西" to "45",
            "海南省" to "46", "重庆" to "50", "四川省" to "51", "贵州省" to "52", "云南省" to "53",
            "西藏" to "54", "陕西省" to "61", "甘肃省" to "62", "青海省" to "63", "宁夏" to "64",
            "新疆" to "65", "台湾省" to "71", "香港" to "81", "澳门" to "82"
        )
        return map[stored] ?: ""
    }

    private fun loadUserMap(context: Context): List<UserProfile> {
        ensureMigrated(context)
        return readUsersWithoutMigration(context)
    }

    private fun readUsersWithoutMigration(context: Context): List<UserProfile> {
        val json = prefs(context).getString(KEY_ALL_USERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<UserProfile>>() {}.type
            profileGson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveUserMap(context: Context, users: List<UserProfile>) {
        prefs(context).edit().putString(KEY_ALL_USERS, profileGson.toJson(users)).apply()
    }

    fun getAllUsers(context: Context): List<UserProfile> = loadUserMap(context)

    fun getCurrentUserId(context: Context): String {
        ensureMigrated(context)
        val id = prefs(context).getString(KEY_CURRENT_USER_ID, null)
        if (!id.isNullOrEmpty()) return id
        val users = loadUserMap(context)
        if (users.isNotEmpty()) {
            setCurrentUserId(context, users.first().id)
            return users.first().id
        }
        return ""
    }

    fun makeChartStateKey(context: Context, baseKey: String): String {
        val uid = getCurrentUserId(context)
        return if (uid.isNotEmpty()) "${baseKey}_${uid}" else baseKey
    }

    fun setCurrentUserId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_CURRENT_USER_ID, id).apply()
    }

    fun getProfile(context: Context, userId: String): UserProfile? {
        return loadUserMap(context).find { it.id == userId }
    }

    fun save(context: Context, profile: UserProfile) {
        val upgraded = migrateArchiveChain(profile, context)
        val withId = if (upgraded.id.isEmpty()) upgraded.copy(id = genId()) else upgraded
        val users = loadUserMap(context).toMutableList()
        val idx = users.indexOfFirst { it.id == withId.id }
        if (idx >= 0) users[idx] = withId else users.add(withId)
        saveUserMap(context, users)
        setCurrentUserId(context, withId.id)
    }

    /**
     * 为归档导出提供当前用户资料的既有编码结果。
     * Gson 仅保留在这个历史存储边界内，新归档模块不直接依赖 Gson。
     */
    internal fun exportCurrentUserJson(context: Context): String =
        profileGson.toJson(migrateArchiveChain(load(context), context))

    /** 解析并升级归档中的用户资料，供完整校验通过后再替换当前用户使用。 */
    internal fun parseArchiveProfile(context: Context, rawJson: String): UserProfile? = try {
        profileGson.fromJson(rawJson, UserProfile::class.java)
            ?.let { migrateArchiveChain(it, context) }
    } catch (_: Exception) {
        null
    }

    /**
     * 覆盖当前用户的资料，但始终保留当前用户 id，避免导入其他设备的归档后破坏本机用户隔离。
     * 返回实际承载导入数据的用户 id。
     */
    internal fun replaceCurrentUserFromArchive(context: Context, profile: UserProfile): String {
        val currentUser = getProfile(context, getCurrentUserId(context))
        val targetUserId = currentUser?.id ?: profile.id.ifEmpty { genId() }
        val replacement = migrateArchiveChain(profile.copy(id = targetUserId), context)
        save(context, replacement)
        if (
            currentUser != null &&
            currentUser.avatarFileName.isNotEmpty() &&
            currentUser.avatarFileName != replacement.avatarFileName
        ) {
            File(context.filesDir, "avatars/${currentUser.avatarFileName}").delete()
        }
        return targetUserId
    }

    internal fun snapshotLegacyUserPreferences(context: Context, userId: String): Map<String, Map<String, Any>> {
        val suffix = "_${userId}"
        return legacyPreferenceFiles.mapNotNull { fileName ->
            val values = context.getSharedPreferences(fileName, Context.MODE_PRIVATE).all
                .mapNotNull { (key, value) ->
                    key.takeIf { it.endsWith(suffix) }
                        ?.removeSuffix(suffix)
                        ?.let { baseKey -> value?.let { baseKey to it } }
                }
                .toMap()
            fileName.takeIf { values.isNotEmpty() }?.let { it to values }
        }.toMap()
    }

    internal fun replaceLegacyUserPreferences(
        context: Context,
        userId: String,
        valuesByFile: Map<String, Map<String, Any>>,
    ): Boolean {
        val suffix = "_${userId}"
        return legacyPreferenceFiles.all { fileName ->
            val preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            val editor = preferences.edit()
            preferences.all.keys.filter { it.endsWith(suffix) }.forEach(editor::remove)
            valuesByFile[fileName].orEmpty().forEach { (baseKey, value) ->
                when (value) {
                    is Boolean -> editor.putBoolean(baseKey + suffix, value)
                    is Int -> editor.putInt(baseKey + suffix, value)
                    is Long -> editor.putLong(baseKey + suffix, value)
                    is Float -> editor.putFloat(baseKey + suffix, value)
                    is String -> editor.putString(baseKey + suffix, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(baseKey + suffix, value as Set<String>)
                }
            }
            editor.commit()
        }
    }

fun load(context: Context): UserProfile {
        val current = getProfile(context, getCurrentUserId(context))
        return UserProfile(
            id = current?.id ?: "",
            archiveSchemaVersion = migrateArchiveSchemaVersion(current?.archiveSchemaVersion),
            archiveAppVersion = current?.archiveAppVersion ?: appVersion(context),
            name = current?.name.orEmpty(),
            gender = current?.gender ?: Gender.MALE,
            birthday = current?.birthday,
            region = current?.region ?: com.woshiwangnima.healthdietpro.model.region.RegionSnapshot(),
            diseaseIds = current?.diseaseIds.orEmpty(),
            heightRecords = current?.heightRecords.orEmpty().map { fixUnit(it, false) },
            weightRecords = current?.weightRecords.orEmpty().map { fixUnit(it, true) },
            avatarFileName = current?.avatarFileName ?: ""
        )
    }

    fun deleteUser(context: Context, id: String) {
        // Clean up per-user data before removing from list
        val user = getProfile(context, id)
        val users = loadUserMap(context).filter { it.id != id }
        saveUserMap(context, users)
        if (getCurrentUserId(context) == id) {
            val next = users.firstOrNull()
            setCurrentUserId(context, next?.id ?: "")
        }
        if (user != null) {
            cleanupPerUserData(context, user)
        }
    }

    fun cleanupPerUserData(context: Context, user: UserProfile) {
        // Delete avatar file
        if (user.avatarFileName.isNotEmpty()) {
            File(context.filesDir, "avatars/${user.avatarFileName}").delete()
        }
        // Delete per-user settings file (user_prefs_<uid>)
        com.woshiwangnima.healthdietpro.model.prefs.UserPrefs.deleteUserFile(context, user.id)
        // Delete per-user chart/medication prefs (keys ending _${userId}) in both legacy files
        val suffix = "_${user.id}"
        for (file in listOf("health_diet_prefs", "app_prefs")) {
            val sp = context.getSharedPreferences(file, Context.MODE_PRIVATE)
            val editor = sp.edit()
            for (key in sp.all.keys) {
                if (key.endsWith(suffix)) editor.remove(key)
            }
            editor.apply()
        }
    }

    fun createDefaultIfEmpty(context: Context): UserProfile {
        val users = loadUserMap(context)
        if (users.isNotEmpty()) return users.first()
        val profile = load(context).copy(id = genId())
        save(context, profile)
        return profile
    }

    private fun genId(): String = (System.currentTimeMillis() xor Math.random().toLong().and(0xFFFF)).toString()

    private fun fixUnit(record: BodyRecord, isWeight: Boolean): BodyRecord {
        val u = record.unit
        if (u != null && u.isNotEmpty()) return record
        return record.copy(unit = if (isWeight) UnitCategoryType.Weight.defaultUnitId else UnitCategoryType.Length.defaultUnitId)
    }

    private val legacyPreferenceFiles = listOf("health_diet_prefs", "app_prefs")

}

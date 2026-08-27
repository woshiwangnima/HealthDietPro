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
import com.woshiwangnima.healthdietpro.model.archive.UserProfileArchiveStore
import com.woshiwangnima.healthdietpro.model.archive.appVersion
import com.woshiwangnima.healthdietpro.model.archive.migrateAvatarReference
import com.woshiwangnima.healthdietpro.model.archive.deleteAvatarReference
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import java.io.File
import java.lang.reflect.Type

object ProfilePrefs {
    private const val PREFS_NAME = "health_diet_prefs"
    private const val KEY_LEGACY_PROFILE = "user_profile"
    private const val KEY_LEGACY_ALL_USERS = "all_users"
    private const val KEY_USER_ARCHIVE_IDS = "user_archive_ids_v1"
    private const val KEY_USER_METADATA_INDEX = "user_metadata_index_v1"
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
                    source.isJsonPrimitive -> error("Archive schema version must be an object")
                    source.isJsonObject -> ArchiveSchemaVersion(
                        major = source.asJsonObject.get("major")?.asInt ?: 0,
                        minor = source.asJsonObject.get("minor")?.asInt ?: 0,
                        patch = source.asJsonObject.get("patch")?.asInt ?: 0,
                    )
                    else -> error("Invalid archive schema version")
                }
            },
        )
        .create()
    private val legacyProfileGson = GsonBuilder()
        .registerTypeAdapter(
            ArchiveSchemaVersion::class.java,
            JsonDeserializer<ArchiveSchemaVersion> { source, _, _ ->
                when {
                    source.isJsonNull -> ArchiveSchemaVersion(0, 0, 0)
                    source.isJsonPrimitive -> ArchiveSchemaVersion(
                        major = 0,
                        minor = 0,
                        patch = source.asInt,
                    )
                    source.isJsonObject -> ArchiveSchemaVersion(
                        major = source.asJsonObject.get("major")?.asInt ?: 0,
                        minor = source.asJsonObject.get("minor")?.asInt ?: 0,
                        patch = source.asJsonObject.get("patch")?.asInt ?: 0,
                    )
                    else -> null
                }
            },
        )
        .create()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun archiveStore(context: Context) = UserProfileArchiveStore(context.applicationContext)

    private fun loadUserMap(context: Context): List<UserProfile> {
        return readUsersWithoutMigration(context)
    }

    private fun readUserMetadata(context: Context): List<UserMetadata> {
        val raw = prefs(context).getString(KEY_USER_METADATA_INDEX, null) ?: return emptyList()
        val type = object : TypeToken<List<UserMetadata>>() {}.type
        val parsed = runCatching { profileGson.fromJson<List<UserMetadata>>(raw, type) }
            .getOrNull()
            .orEmpty()
            .filter { it.id.isNotBlank() }
        val normalized = parsed.distinctBy(UserMetadata::id).map { metadata ->
            val fallbackTime = metadata.updatedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
            metadata.copy(
                createdAtMillis = metadata.createdAtMillis.takeIf { it > 0L } ?: fallbackTime,
                lastActiveAtMillis = metadata.lastActiveAtMillis.takeIf { it > 0L } ?: fallbackTime,
                updatedAtMillis = fallbackTime,
            )
        }
        if (normalized != parsed) saveUserMetadata(context, normalized)
        return normalized
    }

    /** Migrates the pre-6b8 SharedPreferences profile list before the new index is read. */
    private fun ensureLegacyUsersMigrated(context: Context) {
        val preferences = prefs(context)
        val type = object : TypeToken<List<UserProfile>>() {}.type
        val legacyUsers = mutableListOf<UserProfile>()
        preferences.getString(KEY_LEGACY_ALL_USERS, null)?.let { raw ->
            runCatching { legacyProfileGson.fromJson<List<UserProfile>>(raw, type).orEmpty() }
                .onSuccess { legacyUsers += it }
                .onFailure {
                    // Keep valid profiles when one legacy record has an incompatible field.
                    runCatching {
                        com.google.gson.JsonParser.parseString(raw).asJsonArray.forEach { element ->
                            runCatching {
                                legacyProfileGson.fromJson(element, UserProfile::class.java)
                            }.getOrNull()?.let { legacyUsers += it }
                        }
                    }
                }
        }
        preferences.getString(KEY_LEGACY_PROFILE, null)?.let { raw ->
            runCatching { legacyProfileGson.fromJson<UserProfile>(raw, UserProfile::class.java) }
                .getOrNull()
                ?.let { legacyUsers += it }
        }
        if (legacyUsers.isEmpty()) return

        val existingIds = preferences.getStringSet(KEY_USER_ARCHIVE_IDS, emptySet()).orEmpty()
        val existingUsers = existingIds.mapNotNull { readArchiveUser(context, it) }
        val knownIds = (existingIds + existingUsers.map(UserProfile::id)).toMutableSet()

        val migratedLegacy = legacyUsers.mapIndexedNotNull { index, profile ->
            val baseId = profile.id.ifBlank { "legacy_$index" }
            var id = baseId
            var suffix = 1
            while (id in knownIds) {
                if (existingUsers.any { it.id == id && it.name == profile.name }) return@mapIndexedNotNull null
                id = "${baseId}_$suffix"
                suffix++
            }
            knownIds += id
            val normalized = profile.copy(
                id = id,
                archiveSchemaVersion = ArchiveSchemaVersion.Current,
                archiveAppVersion = appVersion(context),
            )
            writeArchiveUser(context, normalized)
            BodyMetricsRepository.forUser(context, id).replace(
                BodyMetrics(
                    heightRecords = normalized.heightRecords,
                    weightRecords = normalized.weightRecords,
                    circumferenceRecords = normalized.circumferenceRecords,
                ),
            )
            normalized
        }
        val migrated = (existingUsers + migratedLegacy).distinctBy(UserProfile::id)
        val current = preferences.getString(KEY_CURRENT_USER_ID, null)
        preferences.edit()
            .putStringSet(KEY_USER_ARCHIVE_IDS, migrated.map { it.id }.toSet())
            .remove(KEY_LEGACY_PROFILE)
            .remove(KEY_LEGACY_ALL_USERS)
            .apply()
        val now = System.currentTimeMillis()
        saveUserMetadata(context, migrated.map { it.toMetadata(now, now, now) })
        if (current.isNullOrBlank() || migrated.none { it.id == current }) {
            preferences.edit().putString(KEY_CURRENT_USER_ID, migrated.first().id).apply()
        }
    }

    private fun saveUserMetadata(context: Context, users: List<UserMetadata>) {
        prefs(context).edit().putString(KEY_USER_METADATA_INDEX, profileGson.toJson(users)).apply()
    }

    private fun readUsersWithoutMigration(context: Context): List<UserProfile> {
        return prefs(context).getStringSet(KEY_USER_ARCHIVE_IDS, emptySet())
            .orEmpty()
            .sorted()
            .mapNotNull { id -> readArchiveUser(context, id) }
    }

    private fun readArchiveUser(context: Context, userId: String): UserProfile? = runCatching {
        archiveStore(context).load(userId)
            ?.let { profileGson.fromJson(it, UserProfile::class.java) }
            ?.takeIf { it.id == userId }
    }.getOrNull()

    private fun writeArchiveUser(context: Context, user: UserProfile): UserProfile {
        val normalized = user.copy(
            avatarFileName = migrateAvatarReference(context, user.id, user.avatarFileName),
            heightRecords = emptyList(),
            weightRecords = emptyList(),
            circumferenceRecords = emptyMap(),
        )
        archiveStore(context).save(normalized.id, profileGson.toJson(normalized))
        return normalized
    }

    /** Rebuilds missing public index entries from profile archives without reading any domain data. */
    private fun repairUserMetadataIndex(context: Context) {
        val storedIds = prefs(context).getStringSet(KEY_USER_ARCHIVE_IDS, emptySet()).orEmpty()
        val archiveRoot = File(context.filesDir, "user_archives")
        val discoveredIds = archiveRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory && File(it, "profile.json").isFile }
            .map { it.name }
        val archiveIds = (storedIds + discoveredIds).toSet()
        val existing = readUserMetadata(context).associateBy(UserMetadata::id)
        val repaired = archiveIds.mapNotNull { userId ->
            val profile = readArchiveUser(context, userId) ?: return@mapNotNull existing[userId]
            val prior = existing[userId]
            val fallbackTime = prior?.updatedAtMillis ?: System.currentTimeMillis()
            profile.toMetadata(
                createdAtMillis = prior?.createdAtMillis ?: fallbackTime,
                lastActiveAtMillis = prior?.lastActiveAtMillis ?: fallbackTime,
                updatedAtMillis = prior?.updatedAtMillis ?: fallbackTime,
            )
        }
        if (archiveIds != storedIds) {
            prefs(context).edit().putStringSet(KEY_USER_ARCHIVE_IDS, archiveIds).apply()
        }
        if (repaired.associateBy(UserMetadata::id) != existing) saveUserMetadata(context, repaired)
    }

    private fun saveUserMap(context: Context, users: List<UserProfile>) {
        val normalizedUsers = users.map { writeArchiveUser(context, it) }
        prefs(context).edit().putStringSet(KEY_USER_ARCHIVE_IDS, normalizedUsers.map { it.id }.toSet()).apply()
        val existing = readUserMetadata(context).associateBy(UserMetadata::id)
        val now = System.currentTimeMillis()
        saveUserMetadata(context, normalizedUsers.map { user ->
            val prior = existing[user.id]
            user.toMetadata(
                createdAtMillis = prior?.createdAtMillis ?: now,
                lastActiveAtMillis = prior?.lastActiveAtMillis ?: now,
                updatedAtMillis = prior?.updatedAtMillis ?: now,
            )
        })
    }

    fun getAllUserMetadata(context: Context): List<UserMetadata> {
        ensureLegacyUsersMigrated(context)
        repairUserMetadataIndex(context)
        return readUserMetadata(context).sortedByDescending { it.updatedAtMillis }
    }

    internal fun getUserMetadata(context: Context, userId: String): UserMetadata? {
        return readUserMetadata(context).firstOrNull { it.id == userId }
    }

    fun getCurrentUserId(context: Context): String {
        ensureLegacyUsersMigrated(context)
        val id = prefs(context).getString(KEY_CURRENT_USER_ID, null)
        if (!id.isNullOrEmpty()) return id
        val users = getAllUserMetadata(context)
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
        ensureLegacyUsersMigrated(context)
        val users = readUserMetadata(context)
        require(id.isEmpty() || users.any { it.id == id }) { "Unknown user id" }
        updateUserActivity(context, id, users, force = true)
        prefs(context).edit().putString(KEY_CURRENT_USER_ID, id).apply()
    }

    /** Records user activity without loading a profile or any domain archive. */
    fun noteCurrentUserActivity(context: Context) {
        val userId = prefs(context).getString(KEY_CURRENT_USER_ID, null).orEmpty()
        if (userId.isNotEmpty()) noteUserActivity(context, userId)
    }

    /** Records a cold application open even when the prior activity was within the write window. */
    fun noteApplicationOpened(context: Context) {
        val userId = prefs(context).getString(KEY_CURRENT_USER_ID, null).orEmpty()
        if (userId.isNotEmpty()) {
            updateUserActivity(context, userId, readUserMetadata(context), force = true)
        }
    }

    /** Records activity only when the saved domain belongs to the current user. */
    fun noteUserActivity(context: Context, userId: String) {
        if (userId.isBlank() || prefs(context).getString(KEY_CURRENT_USER_ID, null) != userId) return
        updateUserActivity(context, userId, readUserMetadata(context), force = false)
    }

    private fun updateUserActivity(
        context: Context,
        userId: String,
        users: List<UserMetadata>,
        force: Boolean,
    ) {
        val now = System.currentTimeMillis()
        val updated = users.map { metadata ->
            if (metadata.id == userId && (force || now - metadata.lastActiveAtMillis >= ACTIVITY_WRITE_INTERVAL_MILLIS)) {
                metadata.copy(lastActiveAtMillis = now)
            } else {
                metadata
            }
        }
        if (updated != users) saveUserMetadata(context, updated)
    }

    fun getProfile(context: Context, userId: String): UserProfile? {
        ensureLegacyUsersMigrated(context)
        return readArchiveUser(context, userId)
    }

    fun save(context: Context, profile: UserProfile) {
        val withId = if (profile.id.isEmpty()) profile.copy(id = genId()) else profile
        val profileOnly = withId.copy(
            heightRecords = emptyList(),
            weightRecords = emptyList(),
            circumferenceRecords = emptyMap(),
        )
        val savedProfile = writeArchiveUser(context, profileOnly)
        val existingMetadata = readUserMetadata(context).firstOrNull { it.id == savedProfile.id }
        val now = System.currentTimeMillis()
        val publicProfileChanged = existingMetadata?.let { existing ->
            existing.name != savedProfile.name ||
                existing.gender != savedProfile.gender ||
                existing.avatarFileName != savedProfile.avatarFileName
        } ?: true
        val metadata = readUserMetadata(context).filterNot { it.id == savedProfile.id } + savedProfile.toMetadata(
            createdAtMillis = existingMetadata?.createdAtMillis ?: now,
            lastActiveAtMillis = existingMetadata?.lastActiveAtMillis ?: now,
            updatedAtMillis = if (publicProfileChanged) now else existingMetadata?.updatedAtMillis ?: now,
        )
        saveUserMetadata(context, metadata)
        val archiveIds = prefs(context).getStringSet(KEY_USER_ARCHIVE_IDS, emptySet()).orEmpty() + savedProfile.id
        prefs(context).edit().putStringSet(KEY_USER_ARCHIVE_IDS, archiveIds).apply()
        setCurrentUserId(context, savedProfile.id)
    }

    /**
     * 为归档导出提供当前用户资料的既有编码结果。
     * Gson 仅保留在这个历史存储边界内，新归档模块不直接依赖 Gson。
     */
    internal fun exportCurrentUserJson(context: Context): String =
        profileGson.toJson(load(context))

    /** 解析并升级归档中的用户资料，供完整校验通过后再替换当前用户使用。 */
    internal fun parseArchiveProfile(context: Context, rawJson: String): UserProfile? = try {
        profileGson.fromJson(rawJson, UserProfile::class.java)
            ?.copy(archiveSchemaVersion = ArchiveSchemaVersion.Current)
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
        val replacement = profile.copy(id = targetUserId, archiveSchemaVersion = ArchiveSchemaVersion.Current)
        save(context, replacement)
        if (
            currentUser != null &&
            currentUser.avatarFileName.isNotEmpty() &&
            currentUser.avatarFileName != replacement.avatarFileName
        ) {
            deleteAvatarReference(context, targetUserId, currentUser.avatarFileName)
        }
        return targetUserId
    }

fun load(context: Context): UserProfile {
        val current = getProfile(context, getCurrentUserId(context))
        val metrics = BodyMetricsRepository.current(context).load()
        return UserProfile(
            id = current?.id ?: "",
            archiveSchemaVersion = ArchiveSchemaVersion.Current,
            archiveAppVersion = current?.archiveAppVersion ?: appVersion(context),
            name = current?.name.orEmpty(),
            gender = current?.gender ?: Gender.MALE,
            birthday = current?.birthday,
            region = current?.region ?: com.woshiwangnima.healthdietpro.model.region.RegionSnapshot(),
            diseaseIds = current?.diseaseIds.orEmpty(),
            heightRecords = metrics.heightRecords.map { fixUnit(it, false) },
            weightRecords = metrics.weightRecords.map { fixUnit(it, true) },
            circumferenceRecords = metrics.circumferenceRecords.mapValues { (_, records) -> records.map { fixUnit(it, false) } },
            avatarFileName = current?.avatarFileName ?: ""
        )
    }

    fun deleteUser(context: Context, id: String) {
        require(id.isNotBlank())
        val user = getProfile(context, id)
        val metadata = readUserMetadata(context).filterNot { it.id == id }
        saveUserMetadata(context, metadata)
        val archiveIds = prefs(context).getStringSet(KEY_USER_ARCHIVE_IDS, emptySet()).orEmpty() - id
        prefs(context).edit().putStringSet(KEY_USER_ARCHIVE_IDS, archiveIds).apply()
        if (getCurrentUserId(context) == id) {
            val next = metadata.maxByOrNull { it.lastActiveAtMillis }
            setCurrentUserId(context, next?.id ?: "")
        }
        removeUserStorage(context, id, user?.avatarFileName.orEmpty())
    }

    private fun removeUserStorage(context: Context, userId: String, avatarFileName: String) {
        archiveStore(context).delete(userId)
        File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}").deleteRecursively()
        cleanupPerUserData(context, userId, avatarFileName)
    }


    private fun cleanupPerUserData(context: Context, userId: String, avatarFileName: String) {
        if (avatarFileName.isNotEmpty()) {
            deleteAvatarReference(context, userId, avatarFileName)
        }
        // Custom food cover images are stored in a user-scoped private directory.
        File(context.filesDir, "food_images/$userId").deleteRecursively()
        // Delete per-user settings file (user_prefs_<uid>)
        com.woshiwangnima.healthdietpro.model.prefs.UserPrefs.deleteUserFile(context, userId)
        deleteLegacyUserPreferenceBackups(context, userId)
        // Delete per-user chart/medication prefs (keys ending _${userId}) in both legacy files
        val suffix = "_$userId"
        for (file in listOf("health_diet_prefs", "app_prefs")) {
            val sp = context.getSharedPreferences(file, Context.MODE_PRIVATE)
            val editor = sp.edit()
            for (key in sp.all.keys) {
                if (key.endsWith(suffix)) editor.remove(key)
            }
            editor.apply()
        }
    }

    private fun deleteLegacyUserPreferenceBackups(context: Context, userId: String) {
        val baseName = "user_prefs_${userId}.xml"
        val sharedPrefsDirectory = File(context.applicationInfo.dataDir, "shared_prefs")
        sharedPrefsDirectory.listFiles()
            .orEmpty()
            .filter { file -> file.name == baseName || file.name.startsWith("$baseName.") }
            .forEach(File::delete)
    }

    fun createDefaultIfEmpty(context: Context): UserProfile {
        val users = getAllUserMetadata(context)
        if (users.isNotEmpty()) {
            val currentUserId = getCurrentUserId(context)
            return getProfile(context, currentUserId)
                ?: getProfile(context, users.first().id)
                ?: error("User metadata has no matching archive")
        }
        val profile = load(context).copy(id = genId())
        save(context, profile)
        return profile
    }

    private fun genId(): String = (System.currentTimeMillis() xor Math.random().toLong().and(0xFFFF)).toString()

    private const val ACTIVITY_WRITE_INTERVAL_MILLIS = 5 * 60 * 1000L

    private fun fixUnit(record: BodyRecord, isWeight: Boolean): BodyRecord {
        val u = record.unit
        if (u != null && u.isNotEmpty()) return record
        return record.copy(unit = if (isWeight) UnitCategoryType.Weight.defaultUnitId else UnitCategoryType.Length.defaultUnitId)
    }

}

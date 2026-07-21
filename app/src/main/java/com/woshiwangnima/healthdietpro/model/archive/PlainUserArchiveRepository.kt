package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import android.util.Base64
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

internal class PlainUserArchiveRepository(
    private val context: Context,
) {
    fun exportCurrentUser(): Result<String> = runCatching {
        val profile = ProfilePrefs.load(context).let { loaded ->
            loaded.takeIf { it.id.isNotBlank() } ?: ProfilePrefs.createDefaultIfEmpty(context)
        }
        val preferences = UserPrefs.snapshot(context, profile.id)
            .mapValues { (_, value) -> encodePreference(value) ?: error("Unsupported preference value") }
        val legacyPreferences = ProfilePrefs.snapshotLegacyUserPreferences(context, profile.id)
            .mapValues { (_, values) ->
                values.mapValues { (_, value) -> encodePreference(value) ?: error("Unsupported preference value") }
            }
        val archive = PlainUserArchive(
            formatVersion = ArchiveSchemaVersion.Current,
            appVersion = appVersion(context),
            exportedAt = Instant.now().toString(),
            sourceUserId = profile.id,
            profile = json.parseToJsonElement(ProfilePrefs.exportCurrentUserJson(context)),
            preferences = preferences,
            legacyPreferences = legacyPreferences,
            avatar = encodeAvatar(profile.avatarFileName),
        )
        json.encodeToString(archive)
    }

    fun importIntoCurrentUser(rawArchive: String): Result<Unit> = runCatching {
        val archive = migrateArchiveChain(json.decodeFromString<PlainUserArchive>(rawArchive))
        require(archive.appVersion.isNotBlank())
        require(archive.exportedAt.isNotBlank())
        require(archive.sourceUserId.isNotBlank())
        require(archive.profile is JsonObject)

        val importedProfile = ProfilePrefs.parseArchiveProfile(context, archive.profile.toString())
            ?: error("Invalid profile")
        require(importedProfile.id == archive.sourceUserId)
        val preferences = archive.preferences.mapValues { (key, value) ->
            require(key.isNotBlank())
            decodePreference(value)
        }
        val legacyPreferences = archive.legacyPreferences.mapValues { (fileName, values) ->
            require(fileName in LEGACY_PREFERENCE_FILES)
            values.mapValues { (key, value) ->
                require(key.isNotBlank())
                decodePreference(value)
            }
        }
        val targetUserId = ProfilePrefs.getCurrentUserId(context).ifEmpty { importedProfile.id }
        val avatarFileName = decodeAndStoreAvatar(archive.avatar, targetUserId)

        check(UserPrefs.replaceAll(context, targetUserId, preferences))
        check(ProfilePrefs.replaceLegacyUserPreferences(context, targetUserId, legacyPreferences))
        ProfilePrefs.replaceCurrentUserFromArchive(
            context = context,
            profile = importedProfile.copy(avatarFileName = avatarFileName),
        )
    }

    private fun encodeAvatar(fileName: String): PlainUserArchiveAvatar? {
        if (fileName.isBlank()) return null
        val file = File(context.filesDir, "avatars/$fileName")
        if (!file.isFile) return null
        return PlainUserArchiveAvatar(
            fileName = fileName,
            contentBase64 = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP),
        )
    }

    private fun decodeAndStoreAvatar(
        avatar: PlainUserArchiveAvatar?,
        targetUserId: String,
    ): String {
        if (avatar == null) return ""
        val bytes = Base64.decode(avatar.contentBase64, Base64.NO_WRAP)
        require(bytes.isNotEmpty() && bytes.size <= MAX_AVATAR_BYTES)
        val extension = avatar.fileName.substringAfterLast('.', "jpg")
            .lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,5}")) }
            ?: "jpg"
        val safeUserId = targetUserId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "archive_${safeUserId}_${System.currentTimeMillis()}.$extension"
        val target = File(context.filesDir, "avatars/$fileName")
        target.parentFile?.mkdirs()
        target.writeBytes(bytes)
        return fileName
    }

    private fun encodePreference(value: Any): PlainUserArchivePreference? = when (value) {
        is Boolean -> PlainUserArchivePreference(PlainUserArchivePreferenceType.BOOLEAN, JsonPrimitive(value))
        is Int -> PlainUserArchivePreference(PlainUserArchivePreferenceType.INT, JsonPrimitive(value))
        is Long -> PlainUserArchivePreference(PlainUserArchivePreferenceType.LONG, JsonPrimitive(value))
        is Float -> value.takeIf { it.isFinite() }?.let {
            PlainUserArchivePreference(PlainUserArchivePreferenceType.FLOAT, JsonPrimitive(it))
        }
        is String -> PlainUserArchivePreference(PlainUserArchivePreferenceType.STRING, JsonPrimitive(value))
        is Set<*> -> value.filterIsInstance<String>().takeIf { it.size == value.size }?.let { strings ->
            PlainUserArchivePreference(
                PlainUserArchivePreferenceType.STRING_SET,
                JsonArray(strings.sorted().map(::JsonPrimitive)),
            )
        }
        else -> null
    }

    private fun decodePreference(preference: PlainUserArchivePreference): Any = when (preference.type) {
        PlainUserArchivePreferenceType.BOOLEAN -> preference.value.jsonPrimitive.booleanOrNull
        PlainUserArchivePreferenceType.INT -> preference.value.jsonPrimitive.intOrNull
        PlainUserArchivePreferenceType.LONG -> preference.value.jsonPrimitive.longOrNull
        PlainUserArchivePreferenceType.FLOAT -> preference.value.jsonPrimitive.floatOrNull?.takeIf { it.isFinite() }
        PlainUserArchivePreferenceType.STRING -> preference.value.jsonPrimitive.content
        PlainUserArchivePreferenceType.STRING_SET -> preference.value.jsonArray.map {
            it.jsonPrimitive.content
        }.toSet().takeIf { it.size == preference.value.jsonArray.size }
    } ?: error("Invalid preference value")

    private fun migrateArchiveChain(archive: PlainUserArchive): PlainUserArchive {
        val storedVersion = archive.formatVersion ?: ArchiveSchemaVersion.Unversioned
        require(storedVersion <= ArchiveSchemaVersion.Current)
        var migrated = archive
        if (storedVersion < ArchiveSchemaVersion.LegacyV2) {
            migrated = migrated.copy(
                legacyPreferences = emptyMap(),
            )
        }
        return migrated.copy(formatVersion = migrateArchiveSchemaVersion(storedVersion))
    }

    private companion object {
        const val MAX_AVATAR_BYTES = 15 * 1024 * 1024
        val LEGACY_PREFERENCE_FILES = setOf("health_diet_prefs", "app_prefs")

        val json = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = false
        }
    }
}

@Serializable
internal data class PlainUserArchive(
    val formatVersion: ArchiveSchemaVersion? = null,
    val appVersion: String,
    val exportedAt: String,
    val sourceUserId: String,
    val profile: JsonElement,
    val preferences: Map<String, PlainUserArchivePreference>,
    val legacyPreferences: Map<String, Map<String, PlainUserArchivePreference>> = emptyMap(),
    val avatar: PlainUserArchiveAvatar? = null,
)

@Serializable
internal data class PlainUserArchiveAvatar(
    val fileName: String,
    val contentBase64: String,
)

@Serializable
internal data class PlainUserArchivePreference(
    val type: PlainUserArchivePreferenceType,
    val value: JsonElement,
)

@Serializable
internal enum class PlainUserArchivePreferenceType {
    BOOLEAN,
    INT,
    LONG,
    FLOAT,
    STRING,
    STRING_SET,
}

package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Per-user profile archive. The legacy all_users preference is intentionally not deleted yet;
 * it remains the migration source and a recovery fallback until all test devices are upgraded.
 */
internal class UserProfileArchiveStore(
    private val context: Context,
) {
    private val directoryName = "user_archives"
    private val profileFileName = "profile.json"

    fun exists(userId: String): Boolean = archiveFile(userId).isFile

    fun load(userId: String): String? {
        if (userId.isBlank()) return null
        verifyOrRepairUserArchiveManifest(context, userId)
        val archive = archiveFile(userId)
        val current = read(archive)
        return current
    }

    fun save(userId: String, archiveJson: String) {
        require(userId.isNotBlank())
        val payload = json.parseToJsonElement(archiveJson).jsonObject
        require(payload["id"]?.jsonPrimitive?.content == userId) { "Profile archive user id mismatch" }
        val profilePayload = JsonObject(payload.filterKeys { it !in BODY_METRIC_KEYS })
        val target = archiveFile(userId)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(
            json.encodeToString(
                DomainEnvelope.serializer(JsonObject.serializer()),
                DomainEnvelope(
                    domainId = DOMAIN_ID,
                    schemaVersion = ArchiveSchemaVersion.Current,
                    appVersion = appVersion(context),
                    updatedAtMillis = System.currentTimeMillis(),
                    payload = profilePayload,
                ),
            ),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace user archive: ${target.name}" }
        writeUserArchiveManifest(context, userId)
    }

    fun delete(userId: String) {
        archiveFile(userId).delete()
    }

    private fun read(file: File): String? = runCatching {
        if (!file.isFile) return null
        val raw = file.readText(Charsets.UTF_8).takeIf { it.isNotBlank() } ?: return null
        val root = json.parseToJsonElement(raw).jsonObject
        if ("domainId" !in root) return raw
        val envelope = json.decodeFromString(DomainEnvelope.serializer(JsonObject.serializer()), raw)
        require(envelope.domainId == DOMAIN_ID) { "Unexpected archive domain: ${envelope.domainId}" }
        require(envelope.schemaVersion <= ArchiveSchemaVersion.Current) { "Unsupported profile archive schema" }
        json.encodeToString(JsonObject.serializer(), envelope.payload)
    }.getOrNull()

    private fun archiveFile(userId: String): File =
        File(userDirectory(userId), profileFileName)

    private fun directory(): File = File(context.filesDir, directoryName)

    private fun userDirectory(userId: String): File = File(directory(), safeId(userId))

    private fun safeId(userId: String): String =
        userId.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private companion object {
        const val DOMAIN_ID = "profile"
        val BODY_METRIC_KEYS = setOf("heightRecords", "weightRecords", "circumferenceRecords")
        val json = Json { encodeDefaults = true; explicitNulls = false; ignoreUnknownKeys = false }
    }
}

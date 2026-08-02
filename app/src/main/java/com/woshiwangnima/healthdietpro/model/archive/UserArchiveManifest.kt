package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
internal data class UserArchiveManifest(
    val userId: String,
    val archiveSchemaVersion: ArchiveSchemaVersion,
    val archiveAppVersion: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val domains: Map<String, DomainManifest>,
)

@Serializable
internal data class DomainManifest(
    val schemaVersion: ArchiveSchemaVersion,
    val updatedAtMillis: Long,
    val byteSize: Long,
    val checksumSha256: String,
)

internal enum class UserArchiveIntegrity {
    HEALTHY,
    MANIFEST_REBUILT,
    INCONSISTENT,
    MISSING,
}

internal data class UserArchiveIntegrityReport(
    val integrity: UserArchiveIntegrity,
    val invalidDomainIds: List<String> = emptyList(),
)

internal fun writeUserArchiveManifest(context: Context, userId: String, directory: File = userArchiveDirectory(context, userId)) {
    val now = System.currentTimeMillis()
    val existing = File(directory, "manifest.json").takeIf(File::isFile)?.let { file ->
        runCatching { manifestJson.decodeFromString(UserArchiveManifest.serializer(), file.readText(Charsets.UTF_8)) }.getOrNull()
    }
    val domains = DOMAIN_FILE_NAMES.mapNotNull { (domainId, fileName) ->
        File(directory, fileName).takeIf(File::isFile)?.let { file ->
            val envelope = readDomainFile(file, domainId)?.envelope
            domainId to DomainManifest(
                schemaVersion = envelope?.schemaVersion ?: ArchiveSchemaVersion.Current,
                updatedAtMillis = envelope?.updatedAtMillis ?: file.lastModified(),
                byteSize = file.length(),
                checksumSha256 = sha256(file.readBytes()),
            )
        }
    }.toMap()
    val manifest = UserArchiveManifest(
        userId = userId,
        archiveSchemaVersion = ArchiveSchemaVersion.Current,
        archiveAppVersion = appVersion(context),
        createdAtMillis = existing?.createdAtMillis ?: now,
        updatedAtMillis = now,
        domains = domains,
    )
    directory.mkdirs()
    val target = File(directory, "manifest.json")
    val temporary = File(directory, "${target.name}.tmp")
    temporary.writeText(manifestJson.encodeToString(manifest), Charsets.UTF_8)
    check(temporary.renameTo(target)) { "Unable to replace user archive manifest" }
}

internal fun verifyOrRepairUserArchiveManifest(context: Context, userId: String): UserArchiveIntegrity =
    inspectUserArchive(context, userId).integrity

internal fun inspectUserArchive(context: Context, userId: String): UserArchiveIntegrityReport {
    val directory = userArchiveDirectory(context, userId)
    if (!directory.isDirectory) return UserArchiveIntegrityReport(UserArchiveIntegrity.MISSING)
    val manifestFile = File(directory, "manifest.json")
    val manifest = runCatching {
        manifestJson.decodeFromString(UserArchiveManifest.serializer(), manifestFile.readText(Charsets.UTF_8))
    }.getOrNull()
    val invalidDomainIds = DOMAIN_FILE_NAMES.mapNotNull { (domainId, fileName) ->
        val file = File(directory, fileName)
        domainId.takeIf { file.isFile && readDomainFile(file, domainId) == null }
    }
    if (invalidDomainIds.isNotEmpty()) {
        return UserArchiveIntegrityReport(UserArchiveIntegrity.INCONSISTENT, invalidDomainIds)
    }
    val valid = manifest?.takeIf { it.userId == userId }?.let { stored ->
        val expectedDomainIds = DOMAIN_FILE_NAMES.filterValues { fileName ->
            File(directory, fileName).isFile
        }.keys
        stored.domains.keys == expectedDomainIds && stored.domains.all { (domainId, domain) ->
            val fileName = DOMAIN_FILE_NAMES[domainId] ?: return@all false
            val file = File(directory, fileName)
            val envelope = readDomainFile(file, domainId)?.envelope
            file.isFile &&
                file.length() == domain.byteSize &&
                sha256(file.readBytes()) == domain.checksumSha256 &&
                domain.schemaVersion == (envelope?.schemaVersion ?: ArchiveSchemaVersion.Current) &&
                domain.updatedAtMillis == (envelope?.updatedAtMillis ?: file.lastModified())
        }
    } == true
    if (valid) return UserArchiveIntegrityReport(UserArchiveIntegrity.HEALTHY)
    if (manifest == null) {
        writeUserArchiveManifest(context, userId, directory)
        return UserArchiveIntegrityReport(UserArchiveIntegrity.MANIFEST_REBUILT)
    }
    return UserArchiveIntegrityReport(UserArchiveIntegrity.INCONSISTENT)
}

private data class DomainFileRead(val envelope: DomainEnvelope<JsonElement>?)

private fun readDomainFile(file: File, expectedDomainId: String): DomainFileRead? = runCatching {
    if (!file.isFile) return null
    val raw = file.readText(Charsets.UTF_8)
    if ("domainId" !in manifestJson.parseToJsonElement(raw).jsonObject) return DomainFileRead(null)
    val envelope = manifestJson.decodeFromString(DomainEnvelope.serializer(JsonElement.serializer()), raw).also { envelope ->
        require(envelope.domainId == expectedDomainId) { "Unexpected archive domain: ${envelope.domainId}" }
        require(envelope.schemaVersion == ArchiveSchemaVersion.Current) { "Unsupported archive schema" }
    }
    DomainFileRead(envelope)
}.getOrNull()

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { "%02x".format(it) }

private val DOMAIN_FILE_NAMES = linkedMapOf(
    "profile" to "profile.json",
    "body_metrics" to "body_metrics.json",
    "medications" to "medications.json",
    "blood_glucose" to "blood_glucose.json",
    "blood_pressure" to "blood_pressure.json",
    "disease_records" to "disease_records.json",
    "custom_foods" to "custom_foods.json",
    "user_preferences" to "user_preferences.json",
)

private val manifestJson = Json { encodeDefaults = true; explicitNulls = false }

internal fun readUserArchiveManifest(context: Context, userId: String): UserArchiveManifest? = runCatching {
    val file = File(userArchiveDirectory(context, userId), "manifest.json")
    manifestJson.decodeFromString(UserArchiveManifest.serializer(), file.readText(Charsets.UTF_8))
}.getOrNull()

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
    val existing = File(directory, MANIFEST_FILE_NAME).takeIf(File::isFile)?.let { file ->
        runCatching { manifestJson.decodeFromString(UserArchiveManifest.serializer(), file.readText(Charsets.UTF_8)) }.getOrNull()
    }
    // 清单也走注册表：新增领域自动进 manifest，不用再抄一份文件名。
    val domains = ArchiveDomains.mapNotNull { domain ->
        File(directory, domain.fileName).takeIf(File::isFile)?.let { file ->
            val envelope = readDomainFile(file, domain.id)?.envelope
            domain.id to DomainManifest(
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
    val target = File(directory, MANIFEST_FILE_NAME)
    val temporary = File(directory, "${target.name}.tmp")
    temporary.writeText(manifestJson.encodeToString(manifest), Charsets.UTF_8)
    check(temporary.renameTo(target)) { "Unable to replace user archive manifest" }
}

internal fun verifyOrRepairUserArchiveManifest(context: Context, userId: String): UserArchiveIntegrity =
    inspectUserArchive(context, userId).integrity

internal fun inspectUserArchive(context: Context, userId: String): UserArchiveIntegrityReport {
    val directory = userArchiveDirectory(context, userId)
    if (!directory.isDirectory) return UserArchiveIntegrityReport(UserArchiveIntegrity.MISSING)
    val manifestFile = File(directory, MANIFEST_FILE_NAME)
    val manifest = runCatching {
        manifestJson.decodeFromString(UserArchiveManifest.serializer(), manifestFile.readText(Charsets.UTF_8))
    }.getOrNull()
    val invalidDomainIds = ArchiveDomains.mapNotNull { domain ->
        val file = File(directory, domain.fileName)
        domain.id.takeIf { file.isFile && readDomainFile(file, domain.id) == null }
    }
    if (invalidDomainIds.isNotEmpty()) {
        return UserArchiveIntegrityReport(UserArchiveIntegrity.INCONSISTENT, invalidDomainIds)
    }
    val valid = manifest?.takeIf { it.userId == userId }?.let { stored ->
        val expectedDomainIds = ArchiveDomains.filter { domain ->
            File(directory, domain.fileName).isFile
        }.map(ArchiveDomain::id).toSet()
        stored.domains.keys == expectedDomainIds && stored.domains.all { (domainId, domain) ->
            val fileName = ArchiveDomains.firstOrNull { it.id == domainId }?.fileName ?: return@all false
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
        // 与 decodeDomain 保持一致：旧版本存档视为可读（由领域 migrate 补齐），
        // 只有高于当前版本的存档才算不可解读。这里若用相等会让校验层先把旧存档判成损坏。
        require(envelope.schemaVersion <= ArchiveSchemaVersion.Current) { "Unsupported archive schema" }
    }
    DomainFileRead(envelope)
}.getOrNull()

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { "%02x".format(it) }

private val manifestJson = Json { encodeDefaults = true; explicitNulls = false }

internal fun readUserArchiveManifest(context: Context, userId: String): UserArchiveManifest? = runCatching {
    val file = File(userArchiveDirectory(context, userId), MANIFEST_FILE_NAME)
    manifestJson.decodeFromString(UserArchiveManifest.serializer(), file.readText(Charsets.UTF_8))
}.getOrNull()

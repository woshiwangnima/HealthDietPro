package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

internal data class UserArchiveDiagnostics(
    val userId: String,
    val archiveSchemaVersion: ArchiveSchemaVersion?,
    val integrity: UserArchiveIntegrity,
    val invalidDomainIds: List<String>,
    val domains: List<UserArchiveDomainDiagnostics>,
    /** 本机目录里有、注册表却没认领的领域文件——新领域漏登记时这里非空。 */
    val unregisteredFiles: List<String> = emptyList(),
)

internal data class UserArchiveDomainDiagnostics(
    val domainId: String,
    val byteSize: Long,
    val recordCount: Int?,
    val checksumSha256: String,
    val schemaVersion: ArchiveSchemaVersion?,
    val error: String? = null,
)

/** Read-only diagnostics used by device-upgrade verification tooling. */
internal fun inspectUserArchiveDiagnostics(context: Context, userId: String): UserArchiveDiagnostics {
    val report = inspectUserArchive(context, userId)
    val manifest = readUserArchiveManifest(context, userId)
    val directory = userArchiveDirectory(context, userId)
    // 领域清单来自注册表，不再维护第二份文件名表。
    val domains = ArchiveDomains.mapNotNull { domain ->
        val file = File(directory, domain.fileName).takeIf(File::isFile) ?: return@mapNotNull null
        val manifestDomain = manifest?.domains?.get(domain.id)
        val digest = sha256(file.readBytes())
        UserArchiveDomainDiagnostics(
            domainId = domain.id,
            byteSize = file.length(),
            recordCount = diagnosticRecordCount(file),
            checksumSha256 = digest,
            schemaVersion = manifestDomain?.schemaVersion,
            error = when {
                domain.id in report.invalidDomainIds -> "Invalid or unsupported domain envelope"
                manifestDomain == null -> "Missing manifest entry"
                manifestDomain.byteSize != file.length() -> "Manifest byte size mismatch"
                manifestDomain.checksumSha256 != digest -> "Manifest checksum mismatch"
                else -> null
            },
        )
    }
    return UserArchiveDiagnostics(
        userId = userId,
        archiveSchemaVersion = manifest?.archiveSchemaVersion,
        integrity = report.integrity,
        invalidDomainIds = report.invalidDomainIds,
        domains = domains,
        unregisteredFiles = unregisteredArchiveFiles(context, userId),
    )
}

private fun diagnosticRecordCount(file: File): Int? = runCatching {
    val root = diagnosticJson.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
    val payload = root["payload"]?.jsonObject ?: root
    when {
        payload["records"] is JsonArray -> (payload["records"] as JsonArray).size
        payload["foods"] is JsonArray -> (payload["foods"] as JsonArray).size
        payload["heightRecords"] is JsonArray -> (payload["heightRecords"] as JsonArray).size
        else -> null
    }
}.getOrNull()

private val diagnosticJson = Json { ignoreUnknownKeys = true }

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { "%02x".format(it) }
